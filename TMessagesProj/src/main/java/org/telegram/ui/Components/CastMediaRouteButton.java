package org.telegram.ui.Components;

import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;

public class CastMediaRouteButton extends View {
    public CastMediaRouteButton(@NonNull Context context) {
        super(context);
        setVisibility(View.GONE);
    }

    public boolean isConnected() {
        return false;
    }

    public void stateUpdated(boolean connected) {
    }
}
