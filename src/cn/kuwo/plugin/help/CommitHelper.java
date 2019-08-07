package cn.kuwo.plugin.help;

import cn.kuwo.plugin.CommenUtil;
import cn.kuwo.plugin.bean.CommitInfo;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.project.Project;
import git4idea.GitUtil;
import git4idea.repo.GitRemote;
import git4idea.repo.GitRepository;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class CommitHelper {
    private final String baseUrl = "http://git.simplereview.com/githook";
    private static CommitHelper ourInstance;
    private Project project;
    private Type gsonTypeList;

    public static CommitHelper getInstance(Project project) {
        if (ourInstance == null) {
            synchronized (CommitHelper.class) {
                if (ourInstance == null) {
                    ourInstance = new CommitHelper(project);
                }
            }
        }
        return ourInstance;
    }

    private CommitHelper(Project project) {
        this.project = project;
        gsonTypeList = new TypeToken<List<CommitInfo>>() {
        }.getType();
    }

    public ArrayList<CommitInfo> getCommits(Project project) {
        HashMap<String, String> param = new HashMap<>();
        StringBuffer project_names = new StringBuffer();
        HashSet<String> projectSet = new HashSet<>();
        for (GitRepository gitRepository : GitUtil.getRepositories(project)) {
            for (GitRemote gitRemote : gitRepository.getRemotes()) {
                String project_name = gitRemote.getFirstUrl();
                int endIndex = project_name.lastIndexOf('.');
                int beginIndex = project_name.lastIndexOf('/');
                beginIndex = beginIndex == project_name.length() - 1 ? beginIndex : beginIndex + 1;
                project_name = project_name.substring(beginIndex, endIndex > 0 ? endIndex : project_name.length());
                projectSet.add(project_name);
            }
        }
        for (String project_name : projectSet) {
            project_names.append(project_name);
            project_names.append(",");
        }
        String str_project_names = project_names.toString();
        if (str_project_names.endsWith(",")) {
            str_project_names = str_project_names.substring(0, str_project_names.length() - 1);
        }
        PropertiesComponent propertiesComponent = PropertiesComponent.getInstance(project);
        String value = propertiesComponent.getValue(CommenUtil.REQUEST_COUNT);
        int requestCount = 100;
        if (value == null || value.isEmpty()) {
            propertiesComponent.setValue(CommenUtil.REQUEST_COUNT, String.valueOf(100));
        } else if (CommenUtil.isInteger(value)) {
            requestCount = Integer.parseInt(value);
        }
        param.put("requestCount", String.valueOf(requestCount));
        if (str_project_names != null && !str_project_names.isEmpty()) {
            param.put("project_names", str_project_names);
        }
        param.put("cmd", "query");
        CommitFilter instance = CommitFilter.getInstance();
        if (instance.commitEndTime != null && !instance.commitEndTime.isEmpty()) {
            param.put("commit_end_time", instance.commitEndTime);
        }
        if (instance.commitStartTime != null && !instance.commitStartTime.isEmpty()) {
            param.put("commit_start_time", instance.commitStartTime);
        }
        if (instance.user != null && !instance.user.isEmpty()) {
            param.put("user", instance.user);
        }
        if (instance.state != null && !instance.state.isEmpty()) {
            if (instance.state.equals("reviewed")) {
                param.put("review_state", "1");
            } else if (instance.state.equals("unreview")) {
                param.put("review_state", "0");
            }
        }
        String result = request(param, "get");
        Gson gson = new Gson();
        Type type = new TypeToken<NetDataList>() {
        }.getType();
        NetDataList netData = gson.fromJson(result, type);
        for (CommitInfo datum : netData.data) {
            checkString(datum);
        }
        if (netData.code == 0) {
            return netData.data;
        }
        return null;
    }

    /**
     * 引号会导致问题，所以编码
     *
     * @param commitInfo
     */
    private void checkString(CommitInfo commitInfo) {
        commitInfo.commit_msg = URLDecoder.decode(commitInfo.commit_msg);
        commitInfo.submitter = URLDecoder.decode(commitInfo.submitter);
        commitInfo.review_comment = URLDecoder.decode(commitInfo.review_comment);
        commitInfo.reviewer = URLDecoder.decode(commitInfo.reviewer);
    }
    private String request(HashMap<String, String> param, String methord) {
        int responseCode = -1;
        try {
            StringBuffer stringBuffer = new StringBuffer();
            for (String key : param.keySet()) {
                String value = param.get(key);
                stringBuffer.append(key);
                stringBuffer.append("=");
                stringBuffer.append(value);
                stringBuffer.append("&");
            }
            String paramStr = stringBuffer.toString();
            if (paramStr.endsWith("&")) {
                paramStr = paramStr.substring(0, paramStr.length() - 1);
            }
            String url = baseUrl;
            if (methord == null || methord.isEmpty() || methord.toLowerCase().equals("get")) {
                if (!paramStr.isEmpty()) {
                    url = url + "?" + paramStr;
                }
            }
            HttpURLConnection httpURLConnection = (HttpURLConnection) new URL(url).openConnection();
            if (methord == null || methord.isEmpty() || methord.toLowerCase().equals("get")) {
                httpURLConnection.setRequestMethod("GET");
            } else {
                httpURLConnection.setRequestMethod("POST");
                httpURLConnection.setDoOutput(true);
            }
            httpURLConnection.connect();
            if (methord == null || methord.isEmpty() || methord.toLowerCase().equals("get")) {
            } else {
                httpURLConnection.getOutputStream().write(paramStr.getBytes("utf-8"));
            }
            //得到响应码
            responseCode = httpURLConnection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                //得到响应流
                InputStream inputStream = httpURLConnection.getInputStream();
                //将响应流转换成字符串
                byte[] bytes = new byte[1024];
                int len = 0;
                StringBuffer result = new StringBuffer();
                while ((len = inputStream.read(bytes)) != -1) {
                    String tem = new String(bytes, 0, len, "utf-8");
                    result.append(tem);
                }
                inputStream.close();
                return result.toString();
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("code", responseCode);
        jsonObject.addProperty("msg", "request fail");
        return jsonObject.toString();
    }

    public String reviewedCommit(CommitInfo commitInfo) {
        HashMap<String, String> params = new HashMap<>();
        params.put("cmd", "update");
        params.put("versionHash", commitInfo.version_hash);
        params.put("reviewer", commitInfo.reviewer);
        params.put("review_comment", commitInfo.review_comment.trim());
//        params.put("review_comment", URLEncoder.encode(commitInfo.review_comment));
        params.put("review_state", "1");
        String post = request(params, "post");
        return post;
    }

    public class NetDataList {
        @SerializedName("code")
        public int code;
        @SerializedName("data")
        public ArrayList<CommitInfo> data;
        @SerializedName("msg")
        public String msg;
    }

    public class NetData {
        @SerializedName("code")
        public int code;
        @SerializedName("data")
        public Object data;
        @SerializedName("msg")
        public String msg;
    }
}
