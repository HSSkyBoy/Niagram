package org.telegram.messenger.chromecast;

import android.net.Uri;

public class ChromecastMedia {
    public static final String IMAGE_JPEG = "image/jpeg";
    public static final String IMAGE_PNG = "image/png";
    public static final String VIDEO_MP4 = "video/mp4";
    public static final String APPLICATION_X_MPEG_URL = "application/x-mpegURL";

    public final String mimeType;
    public final Uri internalUri;
    public final String externalPath;
    public final int width;
    public final int height;

    private ChromecastMedia(Builder b) {
        this.mimeType = b.mimeType;
        this.internalUri = b.internalUri;
        this.externalPath = b.externalPath;
        this.width = b.width;
        this.height = b.height;
    }

    public String getExternalUri(String host) {
        return ChromecastFileServer.getUrlToSource(host, externalPath);
    }

    public static class Builder {
        private final String mimeType;
        private final Uri internalUri;
        private final String externalPath;
        private int width;
        private int height;

        private Builder(String mime, Uri internalUri, String externalPath) {
            this.mimeType = mime;
            this.internalUri = internalUri;
            this.externalPath = externalPath;
        }

        public static Builder fromUri(Uri internalUri, String externalPath, String mimeType) {
            return new Builder(mimeType, internalUri, externalPath);
        }

        public Builder setTitle(String title) {
            return this;
        }

        public Builder setSubtitle(String subtitle) {
            return this;
        }

        public Builder setSize(int width, int height) {
            this.width = width;
            this.height = height;
            return this;
        }

        public ChromecastMedia build() {
            return new ChromecastMedia(this);
        }
    }
}
