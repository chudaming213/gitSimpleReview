package cn.kuwo.plugin;

import cn.kuwo.plugin.bean.CommitInfo;
import cn.kuwo.plugin.help.CommitFilter;

import java.util.ArrayList;
import java.util.Observable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommitListObservable extends Observable {
    private CommitListObservable() {

    }

    private static CommitListObservable instance = new CommitListObservable();

    public static CommitListObservable getInstance() {
        return instance;
    }

    ArrayList<CommitInfo> commitInfos = new ArrayList<>();

    public void setCommitInfos(ArrayList<CommitInfo> commitInfos) {
        if (commitInfos != null) {
//            Collections.reverse(commitInfos);
            this.commitInfos = commitInfos;
            filt();
        }else {
            setChanged();
            notifyObservers(null);
        }
    }

    public void filt() {
        ArrayList<CommitInfo> resultList = new ArrayList<>();
        for (CommitInfo commitInfo : commitInfos) {
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
            if (CommitFilter.getInstance().user != null && !CommitFilter.getInstance().user.isEmpty() && !commitInfo.submitter.equals(CommitFilter.getInstance().user)) {
                continue;
            }
            if (CommitFilter.getInstance().commitStartTime != null && !CommitFilter.getInstance().commitStartTime.isEmpty() && compareTime(commitInfo.commit_time, CommitFilter.getInstance().commitStartTime) < 0) {
                continue;
            }
            if (CommitFilter.getInstance().commitEndTime != null && !CommitFilter.getInstance().commitEndTime.isEmpty() && compareTime(commitInfo.commit_time, CommitFilter.getInstance().commitEndTime) > 0) {
                continue;
            }
            if (CommitFilter.getInstance().reviewer != null && !CommitFilter.getInstance().reviewer.isEmpty() && !CommitFilter.getInstance().reviewer.equals(commitInfo.reviewer)) {
                continue;
            }
            if (CommitFilter.getInstance().state != null && !CommitFilter.getInstance().state.isEmpty()) {
                if (CommitFilter.getInstance().state.equals("unreview") && commitInfo.review_state != 0) {
                    continue;
                }
                if (CommitFilter.getInstance().state.equals("reviewed") && commitInfo.review_state != 1) {
                    continue;
                }
            }
            resultList.add(commitInfo);
        }
        setChanged();
        notifyObservers(resultList);
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
