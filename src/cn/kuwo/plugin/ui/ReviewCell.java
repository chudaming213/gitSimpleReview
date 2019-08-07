package cn.kuwo.plugin.ui;

import cn.kuwo.plugin.bean.CommitInfo;
import com.intellij.openapi.util.IconLoader;

import javax.swing.*;

public class ReviewCell {
    private JLabel msg;
    private JLabel user;
    private JPanel base;
    private JLabel time;
    private JLabel reviewinfo;
    private JLabel comment;

    public JPanel getBaseCell(CommitInfo commitInfo) {
        user.setText(commitInfo.submitter);
        time.setText(commitInfo.commit_time);
        msg.setText("   "+commitInfo.commit_msg);
        if (commitInfo.review_state != 0) {
            reviewinfo.setText(commitInfo.reviewer + "  " + commitInfo.review_time);
            if (commitInfo.review_comment != null && !commitInfo.review_comment.isEmpty()) {
                comment.setIcon(IconLoader.getIcon("/icons/comment.png"));
            } else {
                comment.setIcon(null);
            }
        } else {
            reviewinfo.setText("");
            reviewinfo.setIcon(null);
        }
        return base;
    }
}
