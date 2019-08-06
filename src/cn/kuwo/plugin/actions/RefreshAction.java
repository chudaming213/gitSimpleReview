package cn.kuwo.plugin.actions;

import cn.kuwo.plugin.CommitListObservable;
import cn.kuwo.plugin.bean.CommitInfo;
import cn.kuwo.plugin.help.CommitHelper;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.wm.StatusBar;
import git4idea.GitUtil;
import git4idea.repo.GitRemote;
import git4idea.repo.GitRepository;
import git4idea.update.GitFetchResult;
import git4idea.update.GitFetcher;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;

public class RefreshAction extends AnAction implements DumbAware {

    public RefreshAction() {
        super("Refresh", "Refresh commit list", AllIcons.Actions.Refresh);
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        new Task.Backgroundable(e.getProject(), "Get Review Commits...") {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                Collection<GitRepository> repositories = GitUtil.getRepositories(e.getProject());
                for (GitRepository repository : repositories) {
                    for (GitRemote gitRemote : repository.getRemotes()) {
                        GitFetchResult result = new GitFetcher(e.getProject(), indicator, false).fetch(repository.getRoot(), gitRemote.getName(), null);
                        if (!result.isSuccess()) {
                            GitFetcher.displayFetchResult(e.getProject(), result, null, result.getErrors());
                            return;
                        }
                    }
                }
                ArrayList<CommitInfo> commits = CommitHelper.getInstance().getCommits(e.getProject());
                indicator.setText("Success to get " + commits.size() + " commits.");
                ApplicationManager.getApplication().invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        CommitListObservable.getInstance().setCommitInfos(commits);
                    }
                }, ModalityState.any());
                StatusBar.Info.set("Success to get " + commits.size() + " commits.", e.getProject());
            }
        }.queue();
    }
}
