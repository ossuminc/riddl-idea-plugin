package com.ossuminc.riddl.plugins.idea.configuration;

import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.project.Project;
import com.ossuminc.riddl.plugins.idea.RiddlIdeaPluginBundle;

import javax.swing.*;

class RiddlRunConfigurationForm {

    private JPanel confFilePanel;
    private TextFieldWithBrowseButton confFilePath;
    private JLabel confFileLabel;

    public RiddlRunConfigurationForm(final Project project, final RiddlRunConfiguration configuration) {
        confFilePath.setText(configuration.getWorkingDir());
        FileChooserDescriptor folderDescriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor();
        confFilePath.addBrowseFolderListener(RiddlIdeaPluginBundle.message("riddl.plugins.idea.choose.conf.path"), null, project, folderDescriptor);
    }

    public void apply(RiddlRunConfiguration configuration) {
        return confFilePath.setText(configuration.getWorkingDir());
    }

    public String getConfPath() {
        return confFilePath.getText();
    }

    public JPanel getConfFilePanel() {
        return confFilePanel;
    }
}
