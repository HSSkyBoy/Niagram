package org.telegram.ui;

import java.util.concurrent.atomic.AtomicInteger;

public class CastSync {
    public static final int TYPE_PHOTOVIEWER = 0;
    public static final int TYPE_MUSIC = 1;

    public static int type;
    public static AtomicInteger pending;

    public static void check(int type) {
        CastSync.type = type;
    }

    public static void stop() {
    }

    public static boolean isActive() {
        return false;
    }

    public static long getPosition() {
        return 0;
    }

    public static void seekTo(long position) {
    }

    public static void syncPosition(long position) {
    }

    public static void setVolume(float volume) {
    }

    public static float getVolume() {
        return 0f;
    }

    public static boolean isPlaying() {
        return false;
    }

    public static void setPlaying(boolean play) {
    }

    public static void setSpeed(float speed) {
    }

    public static boolean isUpdatePending() {
        return false;
    }

    public static float getSpeed() {
        return 1.0f;
    }

    public static void doSyncVolume(boolean sync) {
    }

    public static void syncInterface() {
    }

    public static float getDeviceVolume() {
        return 0f;
    }
}
