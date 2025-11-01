package example.utils;


public class StringUtils {

    private StringUtils() {
    }

    public static boolean isNullOrEmpty(String str) {
        return null == str || str.isEmpty();
    }
}