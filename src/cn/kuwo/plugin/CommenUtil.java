package cn.kuwo.plugin;

import java.util.regex.Pattern;

public class CommenUtil {
    public final static String REQUEST_COUNT = "cn.kuwo.plugin.pull.commitcount";
    public final static String USERLIST_JSON = "cn.kuwo.plugin.userlist";

    public static boolean isInteger(String str) {
        Pattern pattern = Pattern.compile("^[-\\+]?[\\d]*$");
        return pattern.matcher(str).matches();
    }
}
