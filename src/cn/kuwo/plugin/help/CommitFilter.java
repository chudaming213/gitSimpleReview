package cn.kuwo.plugin.help;

public class CommitFilter {
    private static CommitFilter ourInstance = new CommitFilter();

    public static CommitFilter getInstance() {
        return ourInstance;
    }

    private CommitFilter() {
    }
    public String searchKey;
    public String commitStartTime;
    public String commitEndTime;
    public String reviewStartTime;
    public String reviewEndTime;
    public String state;//reviewed //unreview
    public String reviewer;
    public String user;
}
