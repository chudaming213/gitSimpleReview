package cn.kuwo.plugin.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ide.CopyPasteManager;

import java.awt.datatransfer.StringSelection;

public class CopyVersionNumAction extends AnAction {
    private String reversionNumber;

    public CopyVersionNumAction() {
        super("Copy Reversion Number", "Copy reversion number of selected commit to the clipboard", AllIcons.Actions.Copy);
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        CopyPasteManager.getInstance().setContents(new StringSelection(reversionNumber));
    }

    public void setReversionNumber(String reversionNumber) {
        this.reversionNumber=reversionNumber;
    }

}
