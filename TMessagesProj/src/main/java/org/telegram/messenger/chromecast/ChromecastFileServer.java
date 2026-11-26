package org.telegram.messenger.chromecast;

public class ChromecastFileServer {
    public static final ChromecastMedia ASSET_FALLBACK_FILE = null;

    public static String getHost() {
        return "127.0.0.1";
    }

    public static String getUrlToSource(String host, String source) {
        return "";
    }
}
