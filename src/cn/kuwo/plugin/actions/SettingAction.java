package cn.kuwo.plugin.actions;

import cn.kuwo.plugin.CommenUtil;
import com.intellij.icons.AllIcons;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;

public final class SettingAction extends AnAction {

    public SettingAction() {
        super("Setting", "Set the number of commits per time.", AllIcons.General.Settings);
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        showRequestCountCountDialog(e.getProject());
    }

    private void showRequestCountCountDialog(Project project) {
        PropertiesComponent propertiesComponent = PropertiesComponent.getInstance(project);
        String value = propertiesComponent.getValue(CommenUtil.REQUEST_COUNT);
        int currentValue = 100;
        if (value != null && !value.isEmpty()) {
            currentValue = Integer.parseInt(value);
        }
        String s = Messages.showInputDialog(project, "Set the number of commits per time.(Current=" + currentValue + ")", "Setting", AllIcons.General.Settings);
        if (s != null && !s.isEmpty()) {
            s = s.trim();
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