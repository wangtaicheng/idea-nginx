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

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.options.BaseConfigurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.util.IconLoader;
import net.ishchenko.idea.nginx.NginxBundle;
import org.jetbrains.annotations.Nls;

import javax.swing.*;

/**
 * Created by IntelliJ IDEA.
 * User: Max
 * Date: 21.07.2009
 * Time: 15:10:16
 */
@Service(Service.Level.APP)
public final class NginxConfigurationManager extends BaseConfigurable {

    private NginxServersConfiguration configuration;
    private NginxConfigurationPanel panel;

    public NginxConfigurationManager() {

        this.configuration = NginxServersConfiguration.getInstance();
    }

    public NginxServersConfiguration getConfiguration() {
        return this.configuration;
    }

    public static NginxConfigurationManager getInstance() {
        return ApplicationManager.getApplication()
                .getService(NginxConfigurationManager.class);
    }


    @Nls
    @Override
    public String getDisplayName() {
        return NginxBundle.message("config.title");
    }

    @Override
    public String getHelpTopic() {
        return null;
    }

    public Icon getIcon() {
        return IconLoader.getIcon("/nginx.png");
    }

    @Override
    public void reset() {
        this.panel.reset();
    }

    @Override
    public void apply() throws ConfigurationException {
        this.panel.apply();
    }

    @Override
    public boolean isModified() {
        return this.panel.isModified();
    }

    @Override
    public JComponent createComponent() {
        if(this.panel == null) {
            this.panel = new NginxConfigurationPanel(this.configuration);
        }
        return this.panel.getPanel();
    }

    @Override
    public void disposeUIResources() {
        this.panel = null;
    }

}
