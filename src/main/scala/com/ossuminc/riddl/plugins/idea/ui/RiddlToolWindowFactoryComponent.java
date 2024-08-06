package com.ossuminc.riddl.plugins.idea.ui;

import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.ide.DataManager;
import java.awt.BorderLayout;

import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.ossuminc.riddl.plugins.idea.actions.RiddlToolWindowCompileAction;
import com.ossuminc.riddl.plugins.idea.actions.RiddlToolWindowSettingsOpenAction;
import javax.swing.*;
import java.awt.event.ActionEvent;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;

class RiddlToolWindowFactoryComponent {
    public JPanel toolWindowPanel;
    public JScrollPane outputPane;
    public JLabel outputLabel;
    public JToolBar toolbar;
    private SimpleToolWindowPanel topBar;

    private ActionGroup actionGroup;
    private RiddlToolWindowCompileAction compileAction;
    private RiddlToolWindowSettingsOpenAction settingsAction;

    public RiddlToolWindowFactoryComponent() {
        toolWindowPanel = new JBPanel();
        outputPane = new JScrollPane();
        outputLabel = new JBLabel();
        toolbar = new JToolBar();
        topBar = new SimpleToolWindowPanel(false, false);

        toolbar.setOrientation(SwingConstants.HORIZONTAL);
        topBar.setToolbar(toolbar);

        toolWindowPanel.setLayout(new BorderLayout(0, 20));
        toolWindowPanel.setBorder(BorderFactory.createEmptyBorder(40, 0, 0, 0));
        toolWindowPanel.add(topBar, BorderLayout.NORTH);

        outputPane.add(outputLabel);
        toolWindowPanel.add(outputPane);

        // converts from AnAction to SwingAction for form
        AnAction compileAnAction = ActionManager.getInstance().getAction("compile_riddl_action");
        AnAction settingsAnAction = ActionManager.getInstance().getAction("riddl_settings_open_window_action");

        Action compileSwingAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                DataContext dataContext = DataManager.getInstance().getDataContext(toolbar);
                AnActionEvent event = AnActionEvent.createFromDataContext(ActionPlaces.UNKNOWN, null, dataContext);
                compileAnAction.actionPerformed(event);
            }
        };
        Action settingsSwingAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                DataContext dataContext = DataManager.getInstance().getDataContext(toolbar);
                AnActionEvent event = AnActionEvent.createFromDataContext(ActionPlaces.UNKNOWN, null, dataContext);
                settingsAnAction.actionPerformed(event);
            }
        };
        compileSwingAction.putValue(Action.SMALL_ICON, compileAnAction.getTemplatePresentation().getIcon());
        settingsSwingAction.putValue(Action.SMALL_ICON, settingsAnAction.getTemplatePresentation().getIcon());
        toolbar.add(compileSwingAction);
        toolbar.add(settingsSwingAction);
    }

    void setLabel(String text) {
        outputLabel.setText(text);
    }
}
