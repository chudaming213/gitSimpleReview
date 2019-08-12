package cn.kuwo.plugin;

import cn.kuwo.plugin.bean.CommitInfo;
import cn.kuwo.plugin.help.CommitFilter;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsException;
import git4idea.branch.GitBranchUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Observable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommitListObservable extends Observable {
    private Project project;

    private CommitListObservable(Project project) {
        this.project = project;
    }

    private static CommitListObservable instance;

    public static CommitListObservable getInstance(Project project) {
        if (instance == null) {
            synchronized (CommitListObservable.class) {
                if (instance == null) {
                    instance = new CommitListObservable(project);
                }
            }
        }
        return instance;
    }

    ArrayList<CommitInfo> commitInfos = new ArrayList<>();

    public void setCommitInfos(ArrayList<CommitInfo> commitInfos) {
        if (commitInfos != null) {
//            Collections.reverse(commitInfos);
            this.commitInfos = commitInfos;
            filt();
        } else {
            setChanged();
            notifyObservers(null);
        }
    }

    public ProgressIndicator mProgressIndicator;

    public void filt() {
        if (mProgressIndicator != null) {
            mProgressIndicator.cancel();
        }
        Task.Backgroundable backgroundable = new Task.Backgroundable(project, "Filting...", true) {
            @Override
            public void onCancel() {
                super.onCancel();
            }

            @Override
            public void run(@NotNull ProgressIndicator progressIndicator) {
                mProgressIndicator = progressIndicator;
                ArrayList<CommitInfo> resultList = new ArrayList<>();
                if (progressIndicator.isCanceled()) {
                    Thread.interrupted();
                }
                for (CommitInfo commitInfo : commitInfos) {
                    if (progressIndicator.isCanceled()) {
                        Thread.interrupted();
                    }
                    if (CommitFilter.getInstance().searchKey != null && !CommitFilter.getInstance().searchKey.isEmpty()) {
                        Pattern pattern = Pattern.compile(CommitFilter.getInstance().searchKey);

                        Matcher matcher = pattern.matcher(commitInfo.version_hash);
                        if (!matcher.find() && !commitInfo.version_hash.contains(CommitFilter.getInstance().searchKey)) {
                            if (commitInfo.commit_msg == null || commitInfo.commit_msg.isEmpty()) {
                                continue;
                            }
                            matcher = pattern.matcher(commitInfo.commit_msg);
                            if (!matcher.find() && !commitInfo.commit_msg.contains(CommitFilter.getInstance().searchKey)) {
                                continue;
                            }
                        }
                    }
                    if (progressIndicator.isCanceled()) {
                        Thread.interrupted();
                    }
                    if (CommitFilter.getInstance().user != null && !CommitFilter.getInstance().user.isEmpty() && !commitInfo.submitter.equals(CommitFilter.getInstance().user)) {
                        continue;
                    }
                    if (progressIndicator.isCanceled()) {
                        Thread.interrupted();
                    }
                    if (CommitFilter.getInstance().commitStartTime != null && !CommitFilter.getInstance().commitStartTime.isEmpty() && compareTime(commitInfo.commit_time, CommitFilter.getInstance().commitStartTime) < 0) {
                        continue;
                    }
                    if (progressIndicator.isCanceled()) {
                        Thread.interrupted();
                    }
                    if (CommitFilter.getInstance().commitEndTime != null && !CommitFilter.getInstance().commitEndTime.isEmpty() && compareTime(commitInfo.commit_time, CommitFilter.getInstance().commitEndTime) > 0) {
                        continue;
                    }
                    if (progressIndicator.isCanceled()) {
                        Thread.interrupted();
                    }
                    if (CommitFilter.getInstance().reviewer != null && !CommitFilter.getInstance().reviewer.isEmpty() && !CommitFilter.getInstance().reviewer.equals(commitInfo.reviewer)) {
                        continue;
                    }
                    if (progressIndicator.isCanceled()) {
                        Thread.interrupted();
                    }
                    if (CommitFilter.getInstance().state != null && !CommitFilter.getInstance().state.isEmpty()) {
                        if (CommitFilter.getInstance().state.equals("unreview") && commitInfo.review_state != 0) {
                            continue;
                        }
                        if (CommitFilter.getInstance().state.equals("reviewed") && commitInfo.review_state != 1) {
                            continue;
                        }
                    }
                    if (progressIndicator.isCanceled()) {
                        Thread.interrupted();
                    }
                    if (CommitFilter.getInstance().branch != null && !CommitFilter.getInstance().branch.isEmpty()) {
                        Collection<String> branches = null;
                        try {
                            branches = GitBranchUtil.getBranches(project, project.getBaseDir(), false, true, commitInfo.version_hash);
                            if (!branches.contains(CommitFilter.getInstance().branch)) {
                                continue;
                            }
                        } catch (VcsException e) {
                            e.printStackTrace();
                        }
                    }
                    if (progressIndicator.isCanceled()) {
                        Thread.interrupted();
                    }
                    resultList.add(commitInfo);
                }
                ApplicationManager.getApplication().invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        setChanged();
                        notifyObservers(resultList);
                    }
                }, ModalityState.any());
            }
        };

        backgroundable.queue();

    }

    /**
     * @param time1
     * @param time2
     * @return time1 > time2 <=> >0
     * time1 = time2  <=>  =0
     * time1 < time2  <=> <0
     */
    private int compareTime(String time1, String time2) {
        return time1.compareTo(time2);
    }

    public void refreshSub() {
        setChanged();
        notifyObservers("refreshSub");
    }
}
