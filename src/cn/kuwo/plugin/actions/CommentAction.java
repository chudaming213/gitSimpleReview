package cn.kuwo.plugin.actions;

import cn.kuwo.plugin.bean.CommitInfo;
import cn.kuwo.plugin.view.TextBorderUtlis;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogBuilder;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.IconLoader;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public final class CommentAction extends AnAction {
    private CommitInfo commitInfo;

    public CommentAction() {
        super("", "Add comments on the commit.", IconLoader.getIcon("/icons/comment.png"));
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        showCommentDialog(e.getProject(), commitInfo);
    }

    public static void showCommentDialog(Project project, CommitInfo commit) {
        DialogBuilder builder = new DialogBuilder(project);
        JTextPane jTextField = new JTextPane();
        jTextField.setPreferredSize(new Dimension(300, 150));
        jTextField.setText(commit.review_comment == null ? "" : commit.review_comment);
        builder.centerPanel(jTextField);
        builder.title("Comment");
        if (commit.review_state != 0) {
            jTextField.setEnabled(false);
            jTextField.setEditable(false);
            builder.okActionEnabled(false);
            builder.show();
            return;
        }
        jTextField.setBorder(new TextBorderUtlis(Color.decode("#808080"), 6, false));
        builder.getWindow().addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                jTextField.setEnabled(true);
                jTextField.setEditable(true);
                ApplicationManager.getApplication().invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        jTextField.requestFocus();
                    }
                }, ModalityState.any());
            }
        });
        int show = builder.show();
        String comment = jTextField.getText();
        if (comment != null && !comment.isEmpty()) {
            if (comment.length() > 200) {
                Messages.showMessageDialog(project,
                        "The length of the message must be less than  than 200.", "Message", AllIcons.Ide.Error);
                return;
            }
        }
        if (commit.review_state == 0) {
            commit.review_comment = comment;
            if (show == DialogWrapper.OK_EXIT_CODE) {
                AcceptAction.acceptCommit(project, commit);
            }
        }
    }

    @Override
    public void update(AnActionEvent e) {
        if (commitInfo == null || (commitInfo.review_state == 1 && (commitInfo.review_comment == null || commitInfo.review_comment.trim().isEmpty()))) {
            e.getPresentation().setEnabled(false);
        } else {
            e.getPresentation().setEnabled(true);
        }
    }

    public void setData(CommitInfo selected) {
        this.commitInfo = selected;
    }
}