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

package net.ishchenko.idea.nginx.run;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.*;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.DefaultJDOMExternalizer;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import net.ishchenko.idea.nginx.NginxBundle;
import net.ishchenko.idea.nginx.configurator.NginxServerDescriptor;
import net.ishchenko.idea.nginx.configurator.NginxServersConfiguration;
import net.ishchenko.idea.nginx.platform.PlatformDependentTools;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.Serial;

/**
 * Created by IntelliJ IDEA.
 * User: Max
 * Date: 14.07.2009
 * Time: 19:16:26
 */
@SuppressWarnings({"deprecation"})
public class NginxRunConfiguration extends RunConfigurationBase {

    @Serial
    private static final long serialVersionUID = 1945808023214725526L;
    public String serverDescriptorId;
    public boolean showHttpLog;
    public boolean showErrorLog;
    public String httpLogPath;
    public String errorLogPath;

    public NginxRunConfiguration(Project project, ConfigurationFactory nginxConfigurationFactory, String name) {
        super(project, nginxConfigurationFactory, name);
    }

    @Override
    public RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment env)
            throws ExecutionException {
        return new NginxRunProfileState(env, this.getProject());
    }

    @Override
    public void checkConfiguration() throws RuntimeConfigurationException {

        NginxServersConfiguration config = NginxServersConfiguration.getInstance();
        NginxServerDescriptor descriptor = config.getDescriptorById(this.serverDescriptorId);
        if(descriptor == null) {
            throw new RuntimeConfigurationException(NginxBundle.message("run.error.noserver"));
        }


        if(this.showHttpLog) {
            File accessLogFile = new File(this.httpLogPath);
            if(accessLogFile.isDirectory()) {
                throw new RuntimeConfigurationException("accesslog is directory");
            }
        }

        if(this.showErrorLog) {
            File errorLogFile = new File(this.errorLogPath);
            if(errorLogFile.isDirectory()) {
                throw new RuntimeConfigurationException("errorlog is directory");
            }
        }


        VirtualFile vfile = LocalFileSystem.getInstance()
                .findFileByPath(descriptor.getExecutablePath());
        if(vfile == null) {
            throw new RuntimeConfigurationException(NginxBundle.message("run.error.badpath"));
        } else {

            PlatformDependentTools pdt = ApplicationManager.getApplication()
                    .getService(PlatformDependentTools.class);
            if(!pdt.checkExecutable(vfile)) {
                throw new RuntimeConfigurationException(NginxBundle.message("run.error.notexecutable"));
            }

        }

    }

    @Override
    public void createAdditionalTabComponents(AdditionalTabComponentManager manager,
            final ProcessHandler startedProcess) {

        if(this.showHttpLog) {

            final NginxLogTab httpLogTab = new NginxLogTab(this.getProject(), new File(this.httpLogPath));

            manager.addAdditionalTabComponent(httpLogTab, "errorlogtab");
            startedProcess.addProcessListener(new ProcessAdapter() {
                @Override
                public void startNotified(ProcessEvent event) {
                    httpLogTab.poke();
                }

                @Override
                public void processTerminated(ProcessEvent event) {
                    httpLogTab.poke();
                    startedProcess.removeProcessListener(this);
                }
            });

        }

        if(this.showErrorLog) {

            final NginxLogTab errorLogTab = new NginxLogTab(this.getProject(), new File(this.errorLogPath));

            manager.addAdditionalTabComponent(errorLogTab, "accesslogtab");
            startedProcess.addProcessListener(new ProcessAdapter() {
                @Override
                public void startNotified(ProcessEvent event) {
                    errorLogTab.poke();
                }

                @Override
                public void processTerminated(ProcessEvent event) {
                    errorLogTab.poke();
                    startedProcess.removeProcessListener(this);
                }
            });

        }

    }

    @Override
    public void readExternal(Element element) throws InvalidDataException {
        DefaultJDOMExternalizer.readExternal(this, element);
        super.readExternal(element);
    }

    @Override
    public void writeExternal(Element element) throws WriteExternalException {
        DefaultJDOMExternalizer.writeExternal(this, element);
        super.writeExternal(element);
    }

    @Override
    public SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
        return new NginxRunSettingsEditor(this);
    }

    @Override
    public ConfigurationPerRunnerSettings createRunnerSettings(ConfigurationInfoProvider provider) {
        return null;  // To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public SettingsEditor<ConfigurationPerRunnerSettings> getRunnerSettingsEditor(ProgramRunner runner) {
        return null;  // To change body of implemented methods use File | Settings | File Templates.
    }

    public String getServerDescriptorId() {
        return this.serverDescriptorId;
    }

    public void setServerDescriptorId(String serverDescriptorId) {
        this.serverDescriptorId = serverDescriptorId;
    }

    public boolean isShowHttpLog() {
        return this.showHttpLog;
    }

    public void setShowHttpLog(boolean showHttpLog) {
        this.showHttpLog = showHttpLog;
    }

    public boolean isShowErrorLog() {
        return this.showErrorLog;
    }

    public void setShowErrorLog(boolean showErrorLog) {
        this.showErrorLog = showErrorLog;
    }

    public String getHttpLogPath() {
        return this.httpLogPath;
    }

    public void setHttpLogPath(String httpLogPath) {
        this.httpLogPath = httpLogPath;
    }

    public String getErrorLogPath() {
        return this.errorLogPath;
    }

    public void setErrorLogPath(String errorLogPath) {
        this.errorLogPath = errorLogPath;
    }
}
