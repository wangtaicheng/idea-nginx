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

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.options.ex.SingleConfigurableEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.util.DimensionService;
import net.ishchenko.idea.nginx.configurator.NginxConfigurationManager;
import net.ishchenko.idea.nginx.configurator.NginxServerDescriptor;
import net.ishchenko.idea.nginx.configurator.NginxServersConfiguration;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

/**
 * Created by IntelliJ IDEA.
 * User: Max
 * Date: 14.07.2009
 * Time: 19:29:20
 */
public class NginxRunSettingsEditor extends SettingsEditor<NginxRunConfiguration> {

    private final NginxRunConfiguration config;
    private Mediator mediator;

    public NginxRunSettingsEditor(NginxRunConfiguration config) {
        this.config = config;
        this.mediator = new Mediator();
    }

    @Override
    protected void applyEditorTo(NginxRunConfiguration s) throws ConfigurationException {
        this.mediator.applyEditorTo(s);
    }

    @Override
    protected void resetEditorFrom(NginxRunConfiguration s) {
        this.mediator.resetEditorFrom(s);
    }

    @Override
    @NotNull
    protected JComponent createEditor() {
        return new NginxRunSettingsForm(this.mediator).getPanel();
    }

    @Override
    protected void disposeEditor() {
        this.mediator = null;
    }

    class Mediator {

        NginxRunSettingsForm form;

        void showServerManagerDialog() {
            // This is a little hack.
            // I could have used ShowSettingsUtil.editConfigurable(), but it gives me
            // little control over opening window. The main problem is dimensions.
            // The window will be too small on first open. So, i'm fixing dimensions on first open.
            // The dimension key generation logic is hidden in ShowSettingsUtil and I had to
            // copy key generation code here
            NginxConfigurationManager configManager = NginxConfigurationManager.getInstance();
            String dimensionServiceKey = "#" + configManager.getDisplayName()
                    .replaceAll("\n", "_")
                    .replaceAll(" ", "_");
            DimensionService dimensionService = DimensionService.getInstance();
            Project[] openProjects = ProjectManager.getInstance()
                    .getOpenProjects();
            Project project = openProjects.length == 1 ? openProjects[0] : null;
            if(dimensionService.getSize(dimensionServiceKey, project) == null) {
                dimensionService.setSize(dimensionServiceKey, new Dimension(750, 500), project);
            }

            SingleConfigurableEditor editor = new SingleConfigurableEditor(this.form.panel, configManager, dimensionServiceKey);
            editor.show();

            this.resetEditorFrom(NginxRunSettingsEditor.this.config);

        }

        public void applyEditorTo(NginxRunConfiguration s) {
            if(this.form.serverCombo.getSelectedItem() != null) {
                NginxServerDescriptor descriptor = (NginxServerDescriptor) this.form.serverCombo.getSelectedItem();
                s.setServerDescriptorId(descriptor.getId());
                s.setShowHttpLog(NginxRunSettingsEditor.this.mediator.form.showHttpLogCheckBox.isSelected());
                s.setHttpLogPath(NginxRunSettingsEditor.this.mediator.form.httpLogPathField.getText());
                s.setShowErrorLog(NginxRunSettingsEditor.this.mediator.form.showErrorLogCheckBox.isSelected());
                s.setErrorLogPath(NginxRunSettingsEditor.this.mediator.form.errorLogPathField.getText());
            }
        }

        public void onChooseDescriptor(NginxServerDescriptor descriptor) {
            if(descriptor != null) {
                this.form.executableField.setText(descriptor.getExecutablePath());
                this.form.configurationField.setText(descriptor.getConfigPath());
                this.form.pidField.setText(descriptor.getPidPath());
                this.form.globalsField.setText(descriptor.getGlobals());
                this.form.httpLogPathField.setText(descriptor.getHttpLogPath());
                this.form.errorLogPathField.setText(descriptor.getErrorLogPath());
            } else {
                this.form.executableField.setText("");
                this.form.configurationField.setText("");
                this.form.pidField.setText("");
                this.form.globalsField.setText("");
                this.form.httpLogPathField.setText("");
                this.form.errorLogPathField.setText("");
            }
        }

        public void onHttpLogCheckboxAction() {
            this.form.httpLogPathField.setEnabled(this.form.showHttpLogCheckBox.isSelected());
        }

        public void onErrorLogCheckboxAction() {
            this.form.errorLogPathField.setEnabled(this.form.showErrorLogCheckBox.isSelected());
        }

        public void resetEditorFrom(NginxRunConfiguration configuration) {
            DefaultComboBoxModel model = (DefaultComboBoxModel) this.form.serverCombo.getModel();
            model.removeAllElements();
            NginxServersConfiguration servers = NginxServersConfiguration.getInstance();
            for(NginxServerDescriptor descriptor : servers.getServersDescriptors()) {
                model.addElement(descriptor);
            }
            String chosenDescriptorId = configuration.getServerDescriptorId();
            if(chosenDescriptorId != null) {
                NginxServersConfiguration serversConfig = NginxServersConfiguration.getInstance();
                NginxServerDescriptor descriptor = serversConfig.getDescriptorById(chosenDescriptorId);
                if(descriptor != null) {
                    model.setSelectedItem(descriptor);
                } else {
                    model.setSelectedItem(null);
                }
            } else {
                model.setSelectedItem(null);
            }
            this.form.showHttpLogCheckBox.setSelected(configuration.isShowHttpLog());
            this.form.httpLogPathField.setEnabled(configuration.isShowHttpLog());
            this.form.showErrorLogCheckBox.setSelected(configuration.isShowErrorLog());
            this.form.errorLogPathField.setEnabled(configuration.isShowErrorLog());
        }

    }

}
