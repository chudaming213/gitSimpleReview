package cn.kuwo.plugin.actions;

import cn.kuwo.plugin.CommitListObservable;
import cn.kuwo.plugin.help.CommitFilter;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.util.Consumer;
import git4idea.GitRemoteBranch;
import git4idea.GitUtil;
import git4idea.repo.GitRepository;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collection;

public final class BranchPopupAction extends BasePopupAction {
    private Project project;

    public BranchPopupAction(Project project, String filterName) {
        super(filterName);
        this.project = project;
        updateFilterValueLabel("All");
    }

    @Override
    protected void createActions(Consumer<AnAction> actionConsumer) {
        actionConsumer.consume(new DumbAwareAction("All") {
            @Override
            public void actionPerformed(AnActionEvent e) {
                updateFilterValueLabel("All");
                CommitFilter.getInstance().branch = "";
                CommitListObservable.getInstance(e.getProject()).filt();
            }
        });
        ArrayList<String> branches = new ArrayList<>();
        Collection<GitRepository> repositories = GitUtil.getRepositories(project);
        if (repositories.size() == 1) {
            ArrayList<GitRepository> gitRepositories = new ArrayList<>(repositories);
            for (GitRemoteBranch gitRemoteBranch : gitRepositories.get(0).getBranches().getRemoteBranches()) {
                branches.add(gitRemoteBranch.getNameForLocalOperations());
            }
        } else {
            for (GitRepository repository : repositories) {
                for (GitRemoteBranch gitRemoteBranch : repository.getBranches().getRemoteBranches()) {
                    branches.add(repository.getRoot().getName() + ":" + gitRemoteBranch.getNameForLocalOperations());
                }
            }
        }
        for (String branch : branches) {
            actionConsumer.consume(new DumbAwareAction(branch) {
                @Override
                public void actionPerformed(AnActionEvent e) {
                    updateFilterValueLabel(branch);
                    CommitFilter.getInstance().branch = branch;
                    CommitListObservable.getInstance(e.getProject()).filt();
                }
            });
        }
        selectUserTextArea = new JTextArea();
        selectOkAction = buildOkAction();
        addSelectItem(actionConsumer);
    }


    protected AnAction buildOkAction() {
        return new AnAction() {
            public void actionPerformed(AnActionEvent e) {
                popup.closeOk(e.getInputEvent());
                String newText = selectUserTextArea.getText().trim();
                if (newText.isEmpty()) {
                    return;
                }
                if (!Comparing.equal(newText, getFilterValueLabel().getText())) {
                    updateFilterValueLabel(newText);
                    CommitFilter.getInstance().branch = newText;
                    CommitListObservable.getInstance(e.getProject()).filt();
                }
            }
        };
    }
}