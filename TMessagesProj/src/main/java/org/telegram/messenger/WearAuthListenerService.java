package org.telegram.messenger;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;

/**
 * Stub service replacing WearableListenerService to eliminate play-services-wearable dependency.
 */
public class WearAuthListenerService extends Service {

    public static final String PATH_OFFER = "/tg-wear-auth/offer";
    public static final String PATH_CANCEL = "/tg-wear-auth/cancel";

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
