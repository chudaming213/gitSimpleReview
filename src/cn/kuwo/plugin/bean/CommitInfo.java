package cn.kuwo.plugin.bean;

import com.google.gson.annotations.SerializedName;

public class CommitInfo {
    @SerializedName("user")
    public String submitter = "";
    @SerializedName("versionHash")
    public String version_hash = "";
    @SerializedName("time")
    public String commit_time = "";
    @SerializedName("msg")
    public String commit_msg = "";

    @SerializedName("reviewer")
    public String reviewer = "";
    @SerializedName("review_state")
    public int review_state = 0;
    @SerializedName("review_time")
    public String review_time = "";
    @SerializedName("review_comment")
    public String review_comment = "";
}
