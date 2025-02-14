/*
 * Copyright 2009 Max Ishchenko
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.ishchenko.idea.nginx.configurator;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.xmlb.XmlSerializer;
import org.jdom.Element;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: Max
 * Date: 22.07.2009
 * Time: 15:33:19
 */

@State(name = NginxServersConfiguration.COMPONENT_NAME,
        storages = {
                @Storage(value = "$APP_CONFIG$/nginx.xml")
        }
)
@Service(Service.Level.APP)
public final class NginxServersConfiguration implements PersistentStateComponent<Element>, Disposable {

    public final static String COMPONENT_NAME = "nginxServers";

    private Set<NginxServerDescriptor> descriptors = new LinkedHashSet<>();
    private NginxServerDescriptor[] cachedDescriptors = null;
    private Map<String, Set<String>> cachedNameToPathsMapping;
    private Set<String> cachedFilepaths;

    public static NginxServersConfiguration getInstance() {
        return ApplicationManager.getApplication()
                .getService(NginxServersConfiguration.class);
    }

    public synchronized NginxServerDescriptor[] getServersDescriptors() {
        if(this.cachedDescriptors == null) {
            this.cachedDescriptors = this.descriptors.toArray(new NginxServerDescriptor[this.descriptors.size()]);
            this.cachedNameToPathsMapping = this.extractNameToPaths();
            this.cachedFilepaths = this.extractFilepaths();
        }
        return this.cachedDescriptors;
    }

    public synchronized void addServerDescriptor(NginxServerDescriptor descriptor) {
        this.descriptors.add(descriptor);
        this.cachedDescriptors = null;
        this.cachedNameToPathsMapping = null;
        this.cachedFilepaths = null;
    }

    public synchronized void removeAllServerDescriptors() {
        this.descriptors.clear();
        this.cachedDescriptors = null;
        this.cachedNameToPathsMapping = null;
        this.cachedFilepaths = null;
    }

    @Override
    public void dispose() {

    }

    @Override
    public synchronized Element getState() {
        Element element = new Element("servers");
        for(final NginxServerDescriptor description : this.getServersDescriptors()) {
            element.addContent(XmlSerializer.serialize(description));
        }
        return element;
    }

    @Override
    public synchronized void loadState(final Element state) {
        this.removeAllServerDescriptors();
        for(final Object o : state.getChildren()) {
            Element element = (Element) o;
            this.addServerDescriptor(XmlSerializer.deserialize(element, NginxServerDescriptor.class));
        }
    }

    @Nullable
    public synchronized NginxServerDescriptor getDescriptorById(String id) {
        for(NginxServerDescriptor descriptor : this.descriptors) {
            if(descriptor.getId()
                    .equals(id)) {
                return descriptor;
            }
        }
        return null;
    }

    public synchronized Map<String, Set<String>> getNameToPathsMapping() {
        if(this.cachedNameToPathsMapping == null) {
            this.cachedNameToPathsMapping = this.extractNameToPaths();
        }
        return this.cachedNameToPathsMapping;
    }

    public synchronized Set<String> getFilepaths() {
        if(this.cachedFilepaths == null) {
            this.cachedFilepaths = this.extractFilepaths();
        }
        return this.cachedFilepaths;
    }

    public synchronized void rebuildFilepaths() {

        // very ugly, indeed

        this.cachedDescriptors = null;
        this.getServersDescriptors();

        this.cachedNameToPathsMapping = null;
        this.getNameToPathsMapping();

        this.cachedFilepaths = null;
        this.getFilepaths();

    }

    private Map<String, Set<String>> extractNameToPaths() {
        Map<String, Set<String>> result = new HashMap<>();
        for(NginxServerDescriptor descriptor : this.getServersDescriptors()) {
            VirtualFile vfile = LocalFileSystem.getInstance()
                    .findFileByPath(descriptor.getConfigPath());
            if(vfile != null) {
                VirtualFile parent = vfile.getParent();
                if(parent != null && parent.isDirectory()) {
                    this.recursiveAdd(result, parent, 0);
                }
            }
        }
        return result;
    }

    private Set<String> extractFilepaths() {
        Set<String> result = new HashSet<>();
        Map<String, Set<String>> nameToPathsMapping = this.getNameToPathsMapping();
        for(Map.Entry<String, Set<String>> entry : nameToPathsMapping.entrySet()) {
            result.addAll(entry.getValue());
        }
        return result;
    }

    private void recursiveAdd(Map<String, Set<String>> result, VirtualFile file, int level) {

        if(level > 2) {
            // only 2 inner folders, dude. that should be enough for most cases.
            // and it should be done the other way at all (parsing includes in conf)
            return;
        }
        if(file.isDirectory()) {
            for(VirtualFile child : file.getChildren()) {
                this.recursiveAdd(result, child, level + 1);
            }
        } else {
            String path = file.getPath();
            String name = path.substring(path.lastIndexOf('/') + 1);
            if(!result.containsKey(name)) {
                result.put(name, new HashSet<>());
            }
            result.get(name)
                    .add(path);
        }

    }


}
