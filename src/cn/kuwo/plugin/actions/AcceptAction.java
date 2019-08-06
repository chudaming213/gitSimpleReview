package cn.kuwo.plugin.actions;

import cn.kuwo.plugin.CommitListObservable;
import cn.kuwo.plugin.bean.CommitInfo;
import cn.kuwo.plugin.help.CommitHelper;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.wm.StatusBar;
import git4idea.GitUserRegistry;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;

public final class AcceptAction extends AnAction {

    private CommitInfo commitInfo;

    public AcceptAction() {
        super("Reviewed the commit", "Reviewed the commit", IconLoader.getIcon("/icons/accept.png"));
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        acceptCommit(e.getProject(), commitInfo);
    }

    public static void acceptCommit(Project project, CommitInfo commitInfo) {
        new com.intellij.openapi.progress.Task.Backgroundable(project, "Update the Commit") {

            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                commitInfo.reviewer = GitUserRegistry.getInstance(project).getOrReadUser(project.getBaseDir()).getName();
                String result = CommitHelper.getInstance().reviewedCommit(commitInfo);
                Gson gson = new Gson();
                Type type = new TypeToken<CommitHelper.NetData>() {
                }.getType();
                CommitHelper.NetData netData = gson.fromJson(result, type);
                if (netData.code == 0) {
                    StatusBar.Info.set("Review the commit success.", project);
                    CommitInfo commitInfo1 = gson.fromJson(netData.data, new TypeToken<CommitInfo>() {
                    }.getType());
                    commitInfo.review_time = commitInfo1.review_time;
                    commitInfo.review_state = 1;
                    CommitListObservable.getInstance().refreshSub();
                } else {
                    StatusBar.Info.set("Review the commit fail." + netData.msg, project);
                }
            }
        }.queue();
    }

    @Override
    public void update(AnActionEvent e) {
        if (commitInfo == null || commitInfo.review_state == 1) {
            e.getPresentation().setEnabled(false);
        } else {
            e.getPresentation().setEnabled(true);
        }
    }

    public void setData(CommitInfo selected) {
        this.commitInfo = selected;
    }
}