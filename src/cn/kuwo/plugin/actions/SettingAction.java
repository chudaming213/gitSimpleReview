package cn.kuwo.plugin.actions;

import cn.kuwo.plugin.CommenUtil;
import com.intellij.icons.AllIcons;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;

import java.awt.*;

public final class SettingAction extends AnAction {

    public SettingAction() {
        super("Setting", "Set the number of commits per time.", AllIcons.General.Settings);
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        showCommentDialog(e.getProject());
    }

    private void showCommentDialog(Project project) {
        PropertiesComponent propertiesComponent = PropertiesComponent.getInstance(project);
        TextField textField = new TextField();
        String value = propertiesComponent.getValue(CommenUtil.REQUEST_COUNT);
        if (value == null || value.isEmpty()) {
            textField.setText(String.valueOf(100));
        } else {
            Integer integer = Integer.parseInt(value);
            textField.setText(String.valueOf(integer));
        }
        String s = Messages.showInputDialog(textField, "Set the number of commits per time.", "Set the Number of Commits per Time.", AllIcons.General.Settings);
        if (s != null) {
            if (CommenUtil.isInteger(s)) {
                Integer integer = Integer.parseInt(s);
                if (integer < 10) {
                    Messages.showErrorDialog("The input must be an integer greater than 10.", "Error");
                } else {
                    propertiesComponent.setValue(CommenUtil.REQUEST_COUNT, String.valueOf(integer.intValue()));
                }
            } else {
                Messages.showErrorDialog("The input must be an integer.", "Error");
            }
        }
    }

}