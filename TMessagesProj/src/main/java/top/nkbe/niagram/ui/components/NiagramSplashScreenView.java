package top.nkbe.niagram.ui.components;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.SystemClock;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.CubicBezierInterpolator;
import org.telegram.ui.Components.LayoutHelper;

public class NiagramSplashScreenView extends FrameLayout {

    private static final int ICON_CARD_SIZE_DP = 88;
    private static final int ICON_SIZE_DP = 76;
    private static final int CARD_RADIUS_DP = 24;
    private static final int ICON_RADIUS_DP = 20;
    private static final long MIN_DISPLAY_DURATION_MS = 800L;
    private static final long EXIT_DURATION_MS = 360L;

    private final FrameLayout iconCard;
    private final ImageView iconView;
    private final LinearLayout centerLayout;
    private final LinearLayout textLayout;
    private final TextView titleView;
    private final TextView subtitleView;
    private final TextView footerView;

    private final long startTime;
    private boolean dismissed = false;

    public NiagramSplashScreenView(Context context) {
        super(context);
        startTime = SystemClock.elapsedRealtime();

        setClickable(true);
        setFocusable(true);

        int backgroundColor = Theme.getColor(Theme.key_windowBackgroundWhite);
        setBackgroundColor(backgroundColor);

        boolean isDark = Theme.isCurrentThemeDark();

        centerLayout = new LinearLayout(context);
        centerLayout.setOrientation(LinearLayout.VERTICAL);
        centerLayout.setGravity(Gravity.CENTER_HORIZONTAL);

        iconCard = new FrameLayout(context);
        int cardBgColor = isDark ? 0xFF242C38 : 0xFFFFFFFF;
        iconCard.setBackgroundColor(cardBgColor);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            iconCard.setElevation(AndroidUtilities.dp(10));
            iconCard.setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), AndroidUtilities.dp(CARD_RADIUS_DP));
                }
            });
            iconCard.setClipToOutline(true);
        }

        iconView = new ImageView(context);
        iconView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        Drawable appIcon = loadAppIcon(context);
        if (appIcon != null) {
            iconView.setImageDrawable(appIcon);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            iconView.setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), AndroidUtilities.dp(ICON_RADIUS_DP));
                }
            });
            iconView.setClipToOutline(true);
        }

        iconCard.addView(iconView, LayoutHelper.createFrame(ICON_SIZE_DP, ICON_SIZE_DP, Gravity.CENTER));
        centerLayout.addView(iconCard, LayoutHelper.createLinear(ICON_CARD_SIZE_DP, ICON_CARD_SIZE_DP, Gravity.CENTER_HORIZONTAL));

        textLayout = new LinearLayout(context);
        textLayout.setOrientation(LinearLayout.VERTICAL);
        textLayout.setGravity(Gravity.CENTER_HORIZONTAL);
        textLayout.setAlpha(0f);

        titleView = new TextView(context);
        titleView.setText("Niagram");
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        titleView.setTypeface(AndroidUtilities.bold());
        titleView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        titleView.setGravity(Gravity.CENTER);
        textLayout.addView(titleView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 0, 18, 0, 0));

        subtitleView = new TextView(context);
        subtitleView.setText("An enhanced, high-performance Telegram client");
        subtitleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        subtitleView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        subtitleView.setAlpha(0.7f);
        subtitleView.setGravity(Gravity.CENTER);
        textLayout.addView(subtitleView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 0, 4, 0, 0));

        centerLayout.addView(textLayout, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL));

        addView(centerLayout, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER));

        footerView = new TextView(context);
        footerView.setText("Crafted for freedom, tuned for speed.");
        footerView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        footerView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        footerView.setAlpha(0f);
        footerView.setGravity(Gravity.CENTER);
        addView(footerView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 16, 0, 16, 36));

        iconCard.setScaleX(0.72f);
        iconCard.setScaleY(0.72f);
        iconCard.setAlpha(0f);

        post(this::startEntranceAnimation);
    }

    private Drawable loadAppIcon(Context context) {
        Drawable drawable = ContextCompat.getDrawable(context, R.mipmap.icon_9_launcher);
        if (drawable == null) {
            drawable = ContextCompat.getDrawable(context, R.mipmap.ic_launcher);
        }
        return drawable;
    }

    private void startEntranceAnimation() {
        if (dismissed) return;

        AnimatorSet cardSet = new AnimatorSet();
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(iconCard, View.SCALE_X, 0.72f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(iconCard, View.SCALE_Y, 0.72f, 1f);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(iconCard, View.ALPHA, 0f, 1f);

        cardSet.playTogether(scaleX, scaleY, alpha);
        cardSet.setDuration(420);
        cardSet.setInterpolator(new OvershootInterpolator(1.35f));

        ObjectAnimator textAlpha = ObjectAnimator.ofFloat(textLayout, View.ALPHA, 0f, 1f);
        textAlpha.setDuration(380);
        textAlpha.setStartDelay(180);
        textAlpha.setInterpolator(CubicBezierInterpolator.EASE_OUT);

        ObjectAnimator footerAlpha = ObjectAnimator.ofFloat(footerView, View.ALPHA, 0f, 0.55f);
        footerAlpha.setDuration(420);
        footerAlpha.setStartDelay(260);
        footerAlpha.setInterpolator(CubicBezierInterpolator.EASE_OUT);

        cardSet.start();
        textAlpha.start();
        footerAlpha.start();
    }

    public void dismiss(Runnable onDismissed) {
        if (dismissed) return;
        dismissed = true;

        long elapsed = SystemClock.elapsedRealtime() - startTime;
        long remaining = Math.max(0, MIN_DISPLAY_DURATION_MS - elapsed);

        postDelayed(() -> {
            animate()
                .scaleX(1.05f)
                .scaleY(1.05f)
                .alpha(0f)
                .setDuration(EXIT_DURATION_MS)
                .setInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (getParent() instanceof ViewGroup) {
                            ((ViewGroup) getParent()).removeView(NiagramSplashScreenView.this);
                        }
                        if (onDismissed != null) {
                            onDismissed.run();
                        }
                    }
                })
                .start();
        }, remaining);
    }
}
