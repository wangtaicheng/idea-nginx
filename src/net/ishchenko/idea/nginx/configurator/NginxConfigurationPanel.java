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
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.ui.DialogBuilder;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.MultiLineLabelUI;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import net.ishchenko.idea.nginx.NginxBundle;
import net.ishchenko.idea.nginx.platform.PlatformDependentTools;

import javax.swing.*;
import java.awt.*;

/**
 * Created by IntelliJ IDEA.
 * User: Max
 * Date: 27.07.2009
 * Time: 17:55:06
 */
public class NginxConfigurationPanel {

    private NginxServersConfiguration config;

    private JSplitPane panel;
    private JBList<NginxServerDescriptor> serverList;

    private TrickyMediator mediator = new TrickyMediator();

    public NginxConfigurationPanel(NginxServersConfiguration config) {

        this.config = config;

        JPanel leftComponent = new JPanel(new BorderLayout());
        JPanel rightComponent = new ServerFieldsForm(this.mediator).getPanel();

        JPanel buttonsPanel = new JPanel();
        buttonsPanel.add(this.createAddButtom());
        buttonsPanel.add(this.createRemoveButton());

        this.serverList = this.createServerList();
        this.mediator.serverList = this.serverList;

        JBScrollPane scrollPane = new JBScrollPane();
        scrollPane.setViewportView(this.serverList);

        leftComponent.add(buttonsPanel, BorderLayout.NORTH);
        leftComponent.add(scrollPane, BorderLayout.CENTER);

        this.panel = new JSplitPane();
        this.panel.setLeftComponent(leftComponent);
        this.panel.setRightComponent(rightComponent);
        this.panel.setDividerLocation(300);

    }

    private JBList<NginxServerDescriptor> createServerList() {
        JBList<NginxServerDescriptor> result = new JBList<>();
        result.setCellRenderer(new DefaultListCellRenderer() {
        });
        result.getSelectionModel()
                .setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        result.addListSelectionListener(e -> {
            if(!e.getValueIsAdjusting()) {
                final NginxServerDescriptor selectedDescriptor = this.serverList.getSelectedValue();
                if(selectedDescriptor != null) {
                    SwingUtilities.invokeLater(() -> this.mediator.showDescriptor(selectedDescriptor));
                }
            }
        });
        return result;
    }

    private JButton createAddButtom() {
        JButton button = new JButton(NginxBundle.message("run.newserver"));
        button.addActionListener(e -> SwingUtilities.invokeLater(() -> this.mediator.addNewServerClicked()));
        button.setMnemonic('n');
        this.mediator.addButton = button;
        return button;
    }

    private JButton createRemoveButton() {
        JButton button = new JButton(NginxBundle.message("run.removeserver"));
        button.addActionListener(e -> {
            final NginxServerDescriptor selectedDescriptor = this.serverList.getSelectedValue();
            if(selectedDescriptor != null) {
                SwingUtilities.invokeLater(() -> this.mediator.removeDescriptor(selectedDescriptor));
            }
        });
        this.mediator.removeButton = button;
        button.setMnemonic('r');
        return button;
    }

    public synchronized void reset() {
        DefaultListModel<NginxServerDescriptor> model = new DefaultListModel<>();
        for(NginxServerDescriptor descriptor : this.config.getServersDescriptors()) {
            model.addElement(descriptor.clone());
        }
        this.serverList.setModel(model);
    }

    public synchronized void apply() throws ConfigurationException {

        PlatformDependentTools pdt = ApplicationManager.getApplication()
                .getService(PlatformDependentTools.class);
        for(int i = 0; i < this.serverList.getModel()
                .getSize(); i++) {
            NginxServerDescriptor descriptor = this.serverList.getModel()
                    .getElementAt(i);
            if(!pdt.checkExecutable(descriptor.getExecutablePath())) {
                this.serverList.setSelectedIndex(i);
                throw new ConfigurationException(NginxBundle.message("run.error.badpath"));
            }
        }

        this.config.removeAllServerDescriptors();
        for(int i = 0; i < this.serverList.getModel()
                .getSize(); i++) {
            NginxServerDescriptor descriptor = this.serverList.getModel()
                    .getElementAt(i);
            this.config.addServerDescriptor(descriptor.clone());
        }
    }

    public JComponent getPanel() {
        return this.panel;
    }

    public boolean isModified() {

        if(this.serverList.getModel()
                   .getSize() != this.config.getServersDescriptors().length) {
            return true;
        }

        for(int i = 0; i < this.serverList.getModel()
                .getSize(); i++) {
            NginxServerDescriptor descriptor = this.serverList.getModel()
                    .getElementAt(i);
            if(!descriptor.equals(this.config.getServersDescriptors()[i])) {
                return true;
            }
        }

        return false;
    }

    static class TrickyMediator {

        JBList serverList;
        JButton addButton;
        JButton removeButton;
        JTextField nameField;
        JTextField executableField;
        JTextField configField;
        JTextField pidField;
        JTextField globalsField;

        PlatformDependentTools pdt;

        public TrickyMediator() {
            this.pdt = ApplicationManager.getApplication()
                    .getService(PlatformDependentTools.class);
        }

        public void addNewServerClicked() {

            VirtualFile[] file = FileChooser.chooseFiles(new NginxExecutableFileChooserDescriptor(), this.serverList, null, null);
            if(file.length > 0) {

                NginxServerDescriptor newDescriptor = this.getDescriptorFromFile(file[0]);

                if(newDescriptor != null) {
                    newDescriptor.setName(this.getUniqueName(newDescriptor.getName()));
                    DefaultListModel model = (DefaultListModel) this.serverList.getModel();
                    model.addElement(newDescriptor);
                    this.serverList.setSelectedIndex(model.getSize() - 1);
                }

            }
        }

        public void removeDescriptor(NginxServerDescriptor descriptor) {
            ((DefaultListModel) this.serverList.getModel()).removeElement(descriptor);
            this.nameField.setText("");
            this.executableField.setText("");
            this.configField.setText("");
            this.pidField.setText("");
            this.globalsField.setText("");
            this.removeButton.setEnabled(false);
        }

        public void showDescriptor(NginxServerDescriptor descriptor) {
            this.nameField.setText(descriptor.getName());
            this.executableField.setText(descriptor.getExecutablePath());
            this.configField.setText(descriptor.getConfigPath());
            this.pidField.setText(descriptor.getPidPath());
            this.globalsField.setText(descriptor.getGlobals());
            this.removeButton.setEnabled(true);
        }

        public void chooseExecutableClicked() {

            VirtualFile oldFile = LocalFileSystem.getInstance()
                    .findFileByPath(this.executableField.getText());
            VirtualFile[] chosen = FileChooser.chooseFiles(new NginxExecutableFileChooserDescriptor(), this.serverList, null, oldFile);

            if(chosen.length > 0) {

                NginxServerDescriptor descriptor = this.getDescriptorFromFile(chosen[0]);

                if(descriptor != null) {
                    this.executableField.setText(descriptor.getExecutablePath());
                    this.configField.setText(descriptor.getConfigPath());
                    this.pidField.setText(descriptor.getPidPath());
                    this.sync();
                }
            }

        }

        private NginxServerDescriptor getDescriptorFromFile(VirtualFile chosen) {

            boolean useDefaultDescriptor = false;
            NginxServerDescriptor descriptor = null;

            if(!this.userAgreesToRunExecutable()) {
                useDefaultDescriptor = true;
            } else {
                try {
                    descriptor = this.pdt.createDescriptorFromFile(chosen);
                } catch(PlatformDependentTools.ThisIsNotNginxExecutableException e) {
                    useDefaultDescriptor = this.userAgreesThatItIsNotNginx();
                }
            }

            if(useDefaultDescriptor) {
                descriptor = this.pdt.getDefaultDescriptorFromFile(chosen);
            }
            return descriptor;
        }

        public void chooseConfigurationClicked() {
            VirtualFile oldFile = LocalFileSystem.getInstance()
                    .findFileByPath(this.configField.getText());
            VirtualFile[] file = FileChooser.chooseFiles(FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor(), this.serverList, null, oldFile);
            if(file.length > 0) {
                this.configField.setText(file[0].getPath());
                this.sync();
            }
        }

        public void choosePidClicked() {
            VirtualFile oldFile = LocalFileSystem.getInstance()
                    .findFileByPath(this.pidField.getText());
            VirtualFile[] file = FileChooser.chooseFiles(FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor(), this.serverList, null, oldFile);
            if(file.length > 0) {
                this.pidField.setText(file[0].getPath());
                this.sync();
            }
        }

        public void sync() {
            if(this.serverList.getSelectedValue() != null) {
                NginxServerDescriptor descriptor = (NginxServerDescriptor) this.serverList.getSelectedValue();
                descriptor.setName(this.nameField.getText());
                descriptor.setExecutablePath(this.executableField.getText());
                descriptor.setConfigPath(this.configField.getText());
                descriptor.setPidPath(this.pidField.getText());
                descriptor.setGlobals(this.globalsField.getText());
                this.serverList.updateUI();
            }
        }

        private boolean userAgreesThatItIsNotNginx() {
            final DialogBuilder builder = new DialogBuilder(this.serverList);

            JLabel label = new JLabel(NginxBundle.message("run.notnginx"), IconLoader.getIcon("/notnginx.png"), SwingConstants.LEFT);
            label.setUI(new MultiLineLabelUI());

            builder.setTitle(NginxBundle.message("run.notnginx.warning"));
            builder.setCenterPanel(label);

            return builder.show() == DialogWrapper.OK_EXIT_CODE;
        }

        private boolean userAgreesToRunExecutable() {

            final DialogBuilder builder = new DialogBuilder(this.serverList);

            JLabel label = new JLabel(NginxBundle.message("run.doyouwanttorun"));
            builder.setTitle(NginxBundle.message("run.notnginx.warning"));
            builder.setCenterPanel(label);

            return builder.show() == DialogWrapper.OK_EXIT_CODE;

        }

        private String getUniqueName(String name) {
            while(this.notUnique(name)) {
                name = name + "_";
            }
            return name;
        }

        private boolean notUnique(String name) {
            for(int i = 0; i < this.serverList.getModel()
                    .getSize(); i++) {
                NginxServerDescriptor descriptor = (NginxServerDescriptor) this.serverList.getModel()
                        .getElementAt(i);
                if(descriptor.getName()
                        .equals(name)) {
                    return true;
                }
            }
            return false;
        }
    }
}
