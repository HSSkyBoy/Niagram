package org.telegram.messenger.chromecast;

import java.io.File;

public class ChromecastController {
    private static volatile ChromecastController Instance;

    private ChromecastController() {
    }

    public static ChromecastController getInstance() {
        ChromecastController localInstance = Instance;
        if (localInstance == null) {
            synchronized (ChromecastController.class) {
                localInstance = Instance;
                if (localInstance == null) {
                    Instance = localInstance = new ChromecastController();
                }
            }
        }
        return localInstance;
    }

    public boolean isCasting() {
        return false;
    }

    public void setCurrentMediaAndCastIfNeeded(ChromecastMediaVariations newMedia) {
    }

    public String setCover(File file) {
        return null;
    }

    public boolean isPlaying(ChromecastMediaVariations media) {
        return false;
    }

    public static boolean eq(ChromecastMediaVariations a, ChromecastMediaVariations b) {
        return false;
    }

    public static boolean eq(ChromecastMedia a, ChromecastMedia b) {
        return false;
    }
}
