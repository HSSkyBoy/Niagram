/*
 * This is the source code of Telegram for Android v. 5.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2018.
 */

package org.telegram.ui.Components;

import static top.nkbe.niagram.helpers.MessageHelper.canSendAsDice;
import static top.nkbe.niagram.helpers.MessageHelper.containsMarkdown;
import static org.telegram.messenger.AndroidUtilities.dp;
import static org.telegram.messenger.AndroidUtilities.dpf2;
import static org.telegram.messenger.AndroidUtilities.lerp;
import static org.telegram.messenger.LocaleController.formatString;
import static org.telegram.messenger.LocaleController.getString;
import static org.telegram.ui.LaunchActivity.getLastFragment;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.Editable;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.ImageSpan;
import android.text.style.RelativeSizeSpan;
import android.util.Property;
import android.util.TypedValue;
import android.view.ActionMode;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.view.ViewTreeObserver;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.webkit.MimeTypeMap;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.text.InputFilter;
import java.util.Arrays;
import android.widget.Toast;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.LongSparseArray;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.core.math.MathUtils;
import androidx.core.os.BuildCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.core.view.inputmethod.EditorInfoCompat;
import androidx.core.view.inputmethod.InputConnectionCompat;
import androidx.core.view.inputmethod.InputContentInfoCompat;
import androidx.customview.widget.ExploreByTouchHelper;
import androidx.dynamicanimation.animation.DynamicAnimation;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import org.telegram.ui.recyclerview.ChatListItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;

import org.jetbrains.annotations.NotNull;
import org.telegram.messenger.AccountInstance;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.AnimationNotificationsLocker;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.BirthdayController;
import org.telegram.messenger.BotForumHelper;
import org.telegram.messenger.BotWebViewVibrationEffect;
import org.telegram.messenger.BuildVars;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.ContactsController;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.Emoji;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.ImageReceiver;
import org.telegram.messenger.LiteMode;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MediaController;
import org.telegram.messenger.MediaDataController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.RichMessageLayout;
import org.telegram.messenger.MessageSuggestionParams;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.NotificationsController;
import org.telegram.messenger.R;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.SharedPrefsHelper;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.UserObject;
import org.telegram.messenger.Utilities;
import org.telegram.messenger.VideoEditedInfo;
import org.telegram.messenger.browser.Browser;
import org.telegram.messenger.camera.CameraController;
import org.telegram.messenger.utils.DrawableUtils;
import org.telegram.messenger.utils.EphemeralMessagesHelper;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.SerializedData;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.tgnet.tl.TL_account;
import org.telegram.tgnet.tl.TL_bots;
import org.telegram.tgnet.tl.TL_iv;
import org.telegram.messenger.utils.tlutils.TLKeyboardHelper;
import org.telegram.tgnet.tl.TL_keyboard;
import org.telegram.tgnet.tl.TL_stories;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenuSubItem;
import org.telegram.ui.ActionBar.ActionBarPopupWindow;
import org.telegram.ui.ActionBar.AdjustPanLayoutHelper;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.SimpleTextView;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeBind;
import org.telegram.ui.BasePermissionsActivity;
import org.telegram.ui.Business.BusinessLinksController;
import org.telegram.ui.Business.QuickRepliesController;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.Components.Forum.ForumUtilities;
import org.telegram.ui.Components.Premium.PremiumFeatureBottomSheet;
import org.telegram.ui.Components.Premium.boosts.BoostRepository;
import org.telegram.ui.Components.blur3.BlurredBackgroundDrawableViewFactory;
import org.telegram.ui.Components.blur3.drawable.BlurredBackgroundDrawable;
import org.telegram.ui.Components.blur3.drawable.color.BlurredBackgroundColorProviderThemed;
import org.telegram.ui.Components.chat.SendButtonBlockedByTypingView;
import org.telegram.ui.Components.chat.layouts.ChatActivitySideControlsButtonsLayout;
import org.telegram.ui.Components.inset.WindowInsetsInAppController;
import org.telegram.ui.ContentPreviewViewer;
import org.telegram.ui.DialogsActivity;
import org.telegram.ui.Gifts.GiftSheet;
import org.telegram.ui.GroupStickersActivity;
import org.telegram.ui.LaunchActivity;
import org.telegram.ui.LinkManager;
import org.telegram.ui.MessageSendPreview;
import org.telegram.ui.MultiContactsSelectorBottomSheet;
import org.telegram.ui.PhotoViewer;
import org.telegram.ui.PremiumPreviewFragment;
import org.telegram.ui.ProfileActivity;
import org.telegram.ui.Stars.StarsController;
import org.telegram.ui.Stars.StarsIntroActivity;
import org.telegram.ui.StickersActivity;
import org.telegram.ui.Stories.HighlightMessageSheet;
import org.telegram.ui.Stories.recorder.CaptionContainerView;
import org.telegram.ui.Stories.recorder.HintView2;
import org.telegram.ui.bots.BotCommandsMenuContainer;
import org.telegram.ui.bots.BotCommandsMenuView;
import org.telegram.ui.bots.BotKeyboardView;
import org.telegram.ui.bots.BotWebViewAttachedSheet;
import org.telegram.ui.bots.BotWebViewSheet;
import org.telegram.ui.bots.ChatActivityBotWebViewButton;
import org.telegram.ui.bots.WebViewRequestProps;
import org.telegram.ui.iv.BlockRow;
import org.telegram.ui.iv.RichEditor;
import org.telegram.ui.iv.RichHtml;
import org.telegram.ui.iv.RichMessageConvert;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import kotlin.Unit;
import top.nkbe.niagram.helpers.ChatsHelper;
import top.nkbe.niagram.llm.LlmConfig;
import top.nkbe.niagram.utils.AndroidUtil;
import top.nkbe.niagram.utils.StringUtils;
import top.nkbe.niagram.translate.Translator;
import top.nkbe.niagram.translate.TranslatorKt;
import top.nkbe.niagram.ui.BottomBuilder;
import top.nkbe.niagram.utils.AlertUtil;
import top.nkbe.niagram.config.NyaConfig;

import me.vkryl.android.animator.BoolAnimator;
import me.vkryl.android.animator.FactorAnimator;

public class ChatActivityEnterView extends FrameLayout implements
    NotificationCenter.NotificationCenterDelegate,
    SizeNotifierFrameLayout.SizeNotifierFrameLayoutDelegate,
    StickersAlert.StickersAlertDelegate,
    SuggestEmojiView.AnchorViewDelegate,
    FactorAnimator.Target, Theme.Colorable
{

    private int commonInputType;
    private boolean stickersEnabled;
    private ActionBarMenuSubItem sendWhenOnlineButton;
    private LinearLayout recordTimeContainer;
    private CharSequence overrideHint;
    private CharSequence overrideHint2;
    float emojiButtonScale = 1f;
    float emojiButtonAlpha = 1f;
    float emojiButtonPaddingScale = 1f;
    float emojiButtonPaddingAlpha = 1f;
    private boolean overrideKeyboardAnimation;
    private Runnable animationEndRunnable;
    private float attachLayoutTranslationX;
    protected float attachLayoutPaddingTranslationX;
    private float attachLayoutAlpha = 1f;
    private float attachLayoutPaddingAlpha = 1f;
    private int attachGravity;
    private int fieldLeftDp;
    private int attachLayoutRightDp;
    private int emojiGravity;
    private int fieldRightDp;
    private int emojiLeftDp;
    private int emojiRightDp;
    private int iosGapDp;
    private int aiButtonGravity;
    private int aiButtonRightMarginDp;
    private static final int IOS_LEFT_EDGE_MARGIN_DP = 0;
    private static final int CAPSULE_INSET_DP = 4;
    private static final int NO_ICON_TEXT_INSET_DP = 8;
    private static final int COMPACT_TEXT_INSET_DP = 4;
    private static final int RIGHT_CLUSTER_GAP_DP = 4;
    private static final int IOS_BUBBLE_RADIUS_DP = 22;
    private static final int SENDER_SELECT_WIDTH_DP = 36;
    private static final int BOT_BUTTON_WIDTH_DP = 36;
    private static final int BOT_COMMANDS_MIN_WIDTH_DP = 40;
    private static final int AI_RICH_LINE_THRESHOLD = 3;
    private float messageTextTranslationX;
    private float messageTextPaddingTranslationX;
    private float horizontalPadding = 0;
    private boolean sendButtonEnabled = true;
    private TLRPC.UserFull userInfo;
    public HintView2 birthdayHint;
    public HintView2 aiHint;
    private HintView2 sendSuggestHintView;

    public boolean voiceOnce;
    public boolean onceVisible;

    public void drawRecordedPannel(Canvas canvas) {
        if (getAlpha() == 0 || recordedAudioPanel == null || recordedAudioPanel.getParent() == null || recordedAudioPanel.getVisibility() != View.VISIBLE) {
            return;
        }
        int restoreCount = canvas.save();
        canvas.translate(getX() + textFieldContainer.getX() + messageEditTextContainer.getX() + recordedAudioPanel.getX(), getY() + textFieldContainer.getY() + messageEditTextContainer.getY() + recordedAudioPanel.getY());
        if (getAlpha() != 1f) {
            canvas.saveLayerAlpha(0, 0, getMeasuredWidth(), getMeasuredHeight(), (int) (255 * getAlpha()), Canvas.ALL_SAVE_FLAG);
        }
        recordedAudioPanel.draw(canvas);
        canvas.restoreToCount(restoreCount);
    }

    public void setAnimatedTop(int animatedTop) {
        this.animatedTop = animatedTop;
    }

    public float getTopViewEnterProgress() {
        return animatorTopViewVisibility.getFloatValue();
    }

    public interface ChatActivityEnterViewDelegate {
        default void beforeMessageSend(CharSequence message, boolean notify, int scheduleDate, long payStars) {}

        default void onEditTextScroll() {}

        default void onContextMenuOpen() {}

        default void onContextMenuClose() {}

        void onMessageSend(CharSequence message, boolean notify, int scheduleDate, int scheduleRepeatPeriod, long payStars);

        void needSendTyping();

        void onTextChanged(CharSequence text, boolean bigChange, boolean fromDraft);

        void onTextSelectionChanged(int start, int end);

        void onTextSpansChanged(CharSequence text);

        void onAttachButtonHidden();

        void onAttachButtonShow();

        void onWindowSizeChanged(int size);

        default void onEmojiViewTabChanged() {

        }

        void onStickersTab(boolean opened);

        void onMessageEditEnd(boolean loading);

        void didPressAttachButton();

        default void didPressStreamingStop() {}

        default void didPressSuggestionButton() {}

        void needStartRecordVideo(int state, boolean notify, int scheduleDate, int scheduleRepeatPeriod, int ttl, long effectId, long stars);

        void toggleVideoRecordingPause();

        boolean isVideoRecordingPaused();

        void needChangeVideoPreviewState(int state, float seekProgress);

        void onSwitchRecordMode(boolean video);

        void onPreAudioVideoRecord();

        void needStartRecordAudio(int state);

        void needShowMediaBanHint();

        void onStickersExpandedChange();

        void onUpdateSlowModeButton(View button, boolean show, CharSequence time);

        default boolean checkCanRemoveRestrictionsByBoosts() {
            return false;
        }

        default void scrollToSendingMessage() {

        }

        default void openScheduledMessages() {

        }

        default boolean hasScheduledMessages() {
            return true;
        }

        void onSendLongClick();

        void onAudioVideoInterfaceUpdated();

        default void bottomPanelTranslationYChanged(float translation) {

        }

        default void prepareMessageSending() {

        }

        default void onTrendingStickersShowed(boolean show) {

        }

        default int getDisableLinkPreviewStatus() {
            return 0;
        }

        default void toggleDisableLinkPreview() {
        }

        default boolean hasForwardingMessages() {
            return false;
        }

        /**
         * @return Height of the content view
         */
        default int getContentViewHeight() {
            return 0;
        }

        /**
         * @return Measured keyboard height
         */
        default int measureKeyboardHeight() {
            return 0;
        }

        /**
         * @return A list of available peers to send messages as
         */
        @Nullable
        default TLRPC.TL_channels_sendAsPeers getSendAsPeers() {
            return null;
        }

        default TLRPC.Peer getDefaultSendAs() {
            return null;
        }

        default boolean setDefaultSendAs(long dialogId, long newPeer) {
            return false;
        }

        default TL_stories.StoryItem getReplyToStory() {
            return null;
        }

        default ChatActivity.ReplyQuote getReplyQuote() {
            return null;
        }

        default void onKeyboardRequested() {

        }

        default boolean onceVoiceAvailable() {
            return false;
        }
        default void setVideoRecordingCameraFront(boolean front) { // nax
        }
    }

    public final static int RECORD_STATE_ENTER = 0;
    public final static int RECORD_STATE_SENDING = 1;
    public final static int RECORD_STATE_CANCEL = 2;
    public final static int RECORD_STATE_PREPARING = 3;
    public final static int RECORD_STATE_CANCEL_BY_TIME = 4;
    public final static int RECORD_STATE_CANCEL_BY_GESTURE = 5;

    private final static int POPUP_CONTENT_EMOJI_KEYBOARD = 0;
    private final static int POPUP_CONTENT_BOT_KEYBOARD = 1;

    private int currentAccount = UserConfig.selectedAccount;
    private AccountInstance accountInstance = AccountInstance.getInstance(UserConfig.selectedAccount);

    private boolean isInitLineCount;
    private int lineCount = 1;
    private AdjustPanLayoutHelper adjustPanLayoutHelper;
    private Runnable showTopViewRunnable;
    private Runnable setTextFieldRunnable;
    public boolean preventInput;
    @Nullable
    public NumberTextView captionLimitView;
    private int currentLimit = -1;
    private int codePointCount;
    private CrossOutDrawable notifySilentDrawable;

    private Runnable moveToSendStateRunnable;
    boolean messageTransitionIsRunning;
    boolean textTransitionIsRunning;
    private float sendButtonLastDrawAlpha;
    private boolean isSendBubbleDismissed;

    private BotMenuButtonType botMenuButtonType = BotMenuButtonType.NO_BUTTON;
    private String botMenuWebViewTitle;
    private String botMenuWebViewUrl;

    private ChatActivityBotWebViewButton botWebViewButton;

    @Nullable
    private BotCommandsMenuView botCommandsMenuButton;
    public BotCommandsMenuContainer botCommandsMenuContainer;
    private BotCommandsMenuView.BotCommandsAdapter botCommandsAdapter;

    private boolean captionLimitBulletinShown;

    // Send as... stuff
    @Nullable
    public SenderSelectView senderSelectView;
    private SenderSelectPopup senderSelectPopupWindow;
    private Runnable onEmojiSearchClosed;
    private int popupX, popupY;
    private Runnable onKeyboardClosed;

    private ValueAnimator searchAnimator;
    private float searchToOpenProgress;

    private boolean sendRoundEnabled = true;
    private boolean sendVoiceEnabled = true;
    public boolean sendPlainEnabled = true;
    private boolean emojiButtonRestricted;


    private HashMap<View, Float> animationParamsX = new HashMap<>();

    private static class SlowModeBtn extends FrameLayout {
        private final SimpleTextView textView;
        private final RectF bgRect = new RectF();
        private final Paint gradientPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Drawable closeDrawable;
        private boolean isPremiumMode = false;

        public SlowModeBtn(@NonNull Context context) {
            super(context);
            textView = new SimpleTextView(context);
            addView(textView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
            setWillNotDraw(false);
            closeDrawable = ContextCompat.getDrawable(context, R.drawable.msg_mini_close_tooltip);
            closeDrawable.setBounds(0, 0, closeDrawable.getIntrinsicWidth(), closeDrawable.getIntrinsicHeight());
            setClipToPadding(false);
            setClipChildren(false);
            ScaleStateListAnimator.apply(this);
        }

        @Override
        protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
            super.onLayout(changed, left, top, right, bottom);
            gradientPaint.setShader(new LinearGradient(0, 0, getMeasuredWidth(), 0, new int[]{0xff7593ff, 0xffa472ff}, new float[]{0f, 1f}, Shader.TileMode.CLAMP));
        }

        public void setTextSize(int size) {
            textView.setTextSize(size);
            invalidate();
        }

        public void setTextColor(int color) {
            textView.setTextColor(color);
            invalidate();
        }

        public void setGravity(int gravity) {
            textView.setGravity(gravity);
            invalidate();
        }

        public void setText(CharSequence text) {
            textView.setText(text);
            invalidate();
        }

        public void setPremiumMode(boolean premiumMode) {
            this.isPremiumMode = premiumMode;
            invalidate();
        }

        public CharSequence getText() {
            return textView.getText();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            if (isPremiumMode) {
                canvas.save();
                int heightRect = dp(26);
                canvas.translate(dp(5), ((getMeasuredHeight() - heightRect) / 2f));
                bgRect.set(-dp(5), 0f, (float) getMeasuredWidth() - getPaddingEnd(), (float) heightRect);
                canvas.drawRoundRect(bgRect, heightRect / 2f, heightRect / 2f, gradientPaint);
                canvas.translate(getMeasuredWidth() - getPaddingEnd() - dp(6) - closeDrawable.getIntrinsicWidth(), dp(5));
                closeDrawable.draw(canvas);
                canvas.restore();
            }
        }

        @Override
        protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
            if (child instanceof SimpleTextView && isPremiumMode) {
                SimpleTextView simpleTextView = (SimpleTextView) child;
                canvas.save();
                canvas.scale(0.8f, 0.8f);
                canvas.translate(-dp(12), dp(6));
                int oldColor = simpleTextView.getTextPaint().getColor();
                simpleTextView.getTextPaint().setColor(Color.WHITE);
                boolean result = super.drawChild(canvas, child, drawingTime);
                simpleTextView.getTextPaint().setColor(oldColor);
                canvas.restore();
                return result;
            }
            return super.drawChild(canvas, child, drawingTime);
        }
    }

    @SuppressWarnings("FieldCanBeLocal")
    private View.AccessibilityDelegate mediaMessageButtonsDelegate = new View.AccessibilityDelegate() {
        @Override
        public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfo info) {
            super.onInitializeAccessibilityNodeInfo(host, info);
            info.setClassName("android.widget.ImageButton");
            info.setClickable(true);
            info.setLongClickable(true);
        }
    };

    boolean ctrlPressed = false;
    boolean shiftPressed = false;

    @Nullable
    public EditTextCaption messageEditText;
    private SlowModeBtn slowModeButton;
    private int slowModeTimer;
    private Runnable updateSlowModeRunnable;
    private SendButtonBlockedByTypingView sendButtonBlockedByTypingView;
    private SendButton sendButton;
    private int sendButtonBackgroundColor;
    public MessageSendPreview messageSendPreview;
    private long sentFromPreview;
    private ActionBarPopupWindow sendPopupWindow;
    private ActionBarPopupWindow.ActionBarPopupWindowLayout sendPopupLayout;
    private ActionBarPopupWindow cameraSelectionPopup; // nax
    private ImageView cancelBotButton;
    private ChatActivityEnterViewAnimatedIconView emojiButton;
    private ImageView deleteRichDraftButton;
    @Nullable
    private ImageView expandStickersButton;
    private boolean emojiViewFrozen;
    private EmojiView emojiView;
    private AnimatorSet panelAnimation;
    public boolean emojiViewVisible;
    private boolean botKeyboardViewVisible;
    private TimerView recordTimerView;
    private FrameLayout audioVideoButtonContainer;
    private boolean audioVideoButtonContainerForbidden;
    private ChatActivityEnterViewAnimatedIconView audioVideoSendButton;
    private boolean isInVideoMode;
    @Nullable
    private FrameLayout recordPanel;
    @Nullable
    public FrameLayout recordedAudioPanel;
    @Nullable
    protected VideoTimelineView videoTimelineView;
    @SuppressWarnings("FieldCanBeLocal")
    private RLottieImageView recordDeleteImageView;
    protected RecordedAudioPlayerView audioTimelineView;
    private long millisecondsRecorded;
    private boolean roundVideoUiFrameClockActive;
    @Nullable
    private SlideTextView slideText;
    @Nullable
    private RecordDot recordDot;
    private SizeNotifierFrameLayout sizeNotifierLayout;
    private ViewGroup viewParentForEmojiView;

    private int originalViewHeight;
    private LinearLayout attachLayout;
    private ViewPropertyAnimator attachButtonAnimator;
    private ImageView attachButton;
    private AiButtonDrawable aiButtonIcon;
    private ImageView aiButton;
    private ImageView richButton;
    private float attachButtonAlpha = 1.0f;
    private ImageView suggestButton;
    @Nullable
    private ImageView botButton;
    @Nullable
    private ImageView reactionsButton;
    public FrameLayout messageEditTextContainer;
    public FrameLayout textFieldContainer;
    public FrameLayout sendButtonContainer;
    private ImageView sendOutlineView;
    public RichMessageLayout.PreviewView richDraftPreview;
    private boolean richDraftActive;
    private TL_iv.RichMessage richDraftMessage;
    @Nullable
    private SendButton doneButton;
    private AnimatorSet doneButtonAnimation;
    protected View topView;
    private BotKeyboardView botKeyboardView;
    private ImageView notifyButton;
    @Nullable
    private ImageView scheduledButton;
    @Nullable
    private ImageView giftButton;
    private boolean scheduleButtonHidden;
    private AnimatorSet scheduledButtonAnimation;
    @Nullable
    private RecordCircle recordCircle;
    public ControlsView controlsView;
    private CloseProgressDrawable2 progressDrawable;
    private Paint dotPaint;
    private int searchingType;
    private Runnable focusRunnable;
    protected int animatedTop;
    @Nullable
    private ReplaceableIconDrawable botButtonDrawable;

    private CharSequence draftMessage;
    private boolean draftSearchWebpage;

    private boolean isPaste;

    private boolean destroyed;

    private MessageObject editingMessageObject;
    private boolean editingCaption;

    private TL_account.TL_businessChatLink editingBusinessLink;

    private BusinessLinkPresetMessage lastSavedBusinessLinkMessage;

    private TLRPC.ChatFull info;

    private boolean hasRecordVideo;

    private int currentPopupContentType = -1;

    private boolean silent;
    private boolean canWriteToChannel;

    private boolean smoothKeyboard;

    private boolean isPaused = true;
    private boolean recordIsCanceled;
    private boolean showKeyboardOnResume;

    private MessageObject botButtonsMessageObject;
    private TLRPC.TL_replyKeyboardMarkup botReplyMarkup;
    private int botCount;
    private boolean hasBotCommands;
    private boolean hasQuickReplies;

    private PowerManager.WakeLock wakeLock;
    private AnimatorSet runningAnimation;
    private AnimatorSet runningAnimation2;
    private AnimatorSet runningAnimationAudio;
    private AnimatorSet recordPannelAnimation;
    private boolean isAttachRestorePending;
    private boolean isSenderSelectSlotReserved;
    private int runningAnimationType;
    private int recordInterfaceState;

    private int keyboardHeight;
    private int keyboardHeightLand;
    private boolean keyboardVisible;
    private int emojiPadding;
    private boolean sendByEnter;
    private long lastTypingTimeSend;
    private float startedDraggingX = -1;
    private float distCanMove = dp(80);
    private boolean recordingAudioVideo;
    public int recordingGuid;
    public boolean forceShowSendButton;
    private boolean allowAnimatedEmoji;
    private boolean allowStickers;
    private boolean allowGifs;

    private boolean skipDotAtEnd = false;
    private String voiceCaption = null;

    private int lastSizeChangeValue1;
    private boolean lastSizeChangeValue2;

    private int[] location = new int[2];

    private Activity parentActivity;
    private ChatActivity parentFragment;
    private long dialog_id;
    private boolean ignoreTextChange;
    private int innerTextChange;
    private MessageObject replyingMessageObject;
    private MessageObject replyingTopMessage;
    private ChatActivity.ReplyQuote replyingQuote;
    private MessageObject botMessageObject;
    private TLRPC.WebPage messageWebPage;
    private boolean messageWebPageSearch = !NyaConfig.disableLinkPreviewByDefault.Bool();
    private ChatActivityEnterViewDelegate delegate;
    private TrendingStickersAlert trendingStickersAlert;

    private TLRPC.TL_document audioToSend;
    private String audioToSendPath;
    private MessageObject audioToSendMessageObject;
    private VideoEditedInfo videoToSendMessageObject;

    protected boolean topViewShowed;

    private boolean needShowTopView;
    private boolean allowShowTopView;

    private MessageObject pendingMessageObject;
    private TL_keyboard.KeyboardButtonProto pendingLocationButton;

    private boolean waitingForKeyboardOpen;
    private boolean waitingForKeyboardOpenAfterAnimation;
    private boolean wasSendTyping;
    protected boolean shouldAnimateEditTextWithBounds;
    private int animatingContentType = -1;

    private boolean clearBotButtonsOnKeyboardOpen;
    private boolean expandStickersWithKeyboard;
    private float doneButtonEnabledProgress = 1f;
    @Nullable
    private Drawable doneCheckDrawable;
    boolean doneButtonEnabled = true;
    private ValueAnimator doneButtonColorAnimator;

    private Runnable openKeyboardRunnable = new Runnable() {
        @Override
        public void run() {
            if (hasBotWebView() && botCommandsMenuIsShowing() || BaseFragment.hasSheets(parentFragment)) {
                return;
            }

            if (!destroyed && messageEditText != null && waitingForKeyboardOpen && !keyboardVisible && !AndroidUtilities.usingHardwareInput && !AndroidUtilities.isInMultiwindow) {
                if (delegate != null) {
                    delegate.onKeyboardRequested();
                }
                messageEditText.requestFocus();
                AndroidUtilities.showKeyboard(messageEditText);
                AndroidUtilities.cancelRunOnUIThread(openKeyboardRunnable);
                AndroidUtilities.runOnUIThread(openKeyboardRunnable, 100);
            }
        }
    };
    private Runnable updateExpandabilityRunnable = new Runnable() {

        private int lastKnownPage = -1;

        @Override
        public void run() {
            if (emojiView != null) {
                int curPage = emojiView.getCurrentPage();
                if (curPage != lastKnownPage) {
                    lastKnownPage = curPage;
                    boolean prevOpen = stickersTabOpen;
                    stickersTabOpen = curPage == 1 || curPage == 2;
                    boolean prevOpen2 = emojiTabOpen;
                    emojiTabOpen = curPage == 0;
                    if (stickersExpanded) {
                        if (searchingType != 0) {
                            setSearchingTypeInternal(curPage == 0 ? 2 : 1, true);
                            checkStickresExpandHeight();
                        } else if (!stickersTabOpen) {
                            setStickersExpanded(false, true, false);
                        }
                    }
                    if (prevOpen != stickersTabOpen || prevOpen2 != emojiTabOpen) {
                        checkSendButton(true);
                    }
                }
            }
        }
    };

    private Property<View, Integer> roundedTranslationYProperty = new Property<View, Integer>(Integer.class, "translationY") {
        @Override
        public Integer get(View object) {
            return Math.round(object.getTranslationY());
        }

        @Override
        public void set(View object, Integer value) {
            object.setTranslationY(value);
        }
    };

    private Property<RecordCircle, Float> recordCircleScale = new Property<RecordCircle, Float>(Float.class, "scale") {
        @Override
        public Float get(RecordCircle object) {
            return object.getScale();
        }

        @Override
        public void set(RecordCircle object, Float value) {
            object.setScale(value);
        }
    };

    private Property<RecordCircle, Float> recordControlsCircleScale = new Property<RecordCircle, Float>(Float.class, "controlsScale") {
        @Override
        public Float get(RecordCircle object) {
            return object.getControlsScale();
        }

        @Override
        public void set(RecordCircle object, Float value) {
            object.setControlsScale(value);
        }
    };

    private Paint redDotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private boolean stickersTabOpen;
    private boolean emojiTabOpen;
    private boolean gifsTabOpen;
    private boolean stickersExpanded;
    private boolean closeAnimationInProgress;
    private Animator stickersExpansionAnim;
    private Animator currentResizeAnimation;
    private float stickersExpansionProgress;
    private int stickersExpandedHeight;
    private boolean stickersDragging;
    private AnimatedArrowDrawable stickersArrow;
    private boolean removeEmojiViewAfterAnimation;

    private Boolean pendingCameraFront = null; // nax

    private Runnable onFinishInitCameraRunnable = new Runnable() {
        @Override
        public void run() {
            if (delegate != null) {
                if (pendingCameraFront != null) {
                    delegate.setVideoRecordingCameraFront(pendingCameraFront);
                    pendingCameraFront = null;
                }
                delegate.needStartRecordVideo(0, true, 0, 0, 0, 0, 0);
            }
        }
    };

    private boolean recordAudioVideoRunnableStarted;
    private boolean calledRecordRunnable;
    private Runnable recordAudioVideoRunnable = new Runnable() {
        @Override
        public void run() {
            if (delegate == null || parentActivity == null) {
                return;
            }
            /*delegate.onPreAudioVideoRecord();
            calledRecordRunnable = true;
            recordAudioVideoRunnableStarted = false;
            if (slideText != null) {
                slideText.setAlpha(1.0f);
                slideText.setTranslationY(0);
            }
            audioToSendPath = null;
            audioToSend = null;*/
            if (isInVideoMode()) {
                if (Build.VERSION.SDK_INT >= 23) {
                    boolean hasAudio = parentActivity.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
                    boolean hasVideo = parentActivity.checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
                    if (!hasAudio || !hasVideo) {
                        String[] permissions = new String[!hasAudio && !hasVideo ? 2 : 1];
                        if (!hasAudio && !hasVideo) {
                            permissions[0] = Manifest.permission.RECORD_AUDIO;
                            permissions[1] = Manifest.permission.CAMERA;
                        } else if (!hasAudio) {
                            permissions[0] = Manifest.permission.RECORD_AUDIO;
                        } else {
                            permissions[0] = Manifest.permission.CAMERA;
                        }
                        parentActivity.requestPermissions(permissions, BasePermissionsActivity.REQUEST_CODE_VIDEO_MESSAGE);
                        return;
                    }
                }
                // 0 = front, 1 = rear, 2 = ask
                int cameraMode = NyaConfig.INSTANCE.getCameraInVideoMessages().Int();
                if (cameraMode == 2 && pendingCameraFront == null) {
                    recordAudioVideoRunnableStarted = false;
                    calledRecordRunnable = false;
                    showCameraSelectionPopup(audioVideoButtonContainer, () -> {
                        pendingCameraFront = true;
                        proceedWithVideoRecording();
                    }, () -> {
                        pendingCameraFront = false;
                        proceedWithVideoRecording();
                    });
                    return;
                } else if (cameraMode != 2) {
                    pendingCameraFront = (cameraMode == 0);
                }
            }

            delegate.onPreAudioVideoRecord();
            calledRecordRunnable = true;
            recordAudioVideoRunnableStarted = false;
            if (slideText != null) {
                slideText.setAlpha(1.0f);
                slideText.setTranslationY(0);
            }
            audioToSendPath = null;
            audioToSend = null;

            if (isInVideoMode()) {
                if (!CameraController.getInstance().isCameraInitied()) {
                    CameraController.getInstance().initCamera(onFinishInitCameraRunnable);
                } else {
                    onFinishInitCameraRunnable.run();
                }
                if (!recordingAudioVideo) {
                    recordingAudioVideo = true;
                    updateRecordInterface(RECORD_STATE_ENTER, true);
                    if (recordCircle != null) {
                        recordCircle.showWaves(false, false);
                    }
                    if (recordTimerView != null) {
                        recordTimerView.reset();
                    }
                }
            } else {
                if (Build.VERSION.SDK_INT >= 23 && parentActivity.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                    parentActivity.requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 3);
                    return;
                }

                delegate.needStartRecordAudio(1);
                startedDraggingX = -1;
                TL_stories.StoryItem storyItem = delegate != null ? delegate.getReplyToStory() : null;
                MediaController.getInstance().startRecording(currentAccount, dialog_id, replyingMessageObject, getThreadMessage(), storyItem, recordingGuid, true, parentFragment != null ? parentFragment.getMessageChatSendParams() : null, getSendMonoForumPeerId(), getSendMessageSuggestionParams());
                recordingAudioVideo = true;
                updateRecordInterface(RECORD_STATE_ENTER, true);
                if (recordTimerView != null) {
                    recordTimerView.start(0);
                }
                if (recordDot != null) {
                    recordDot.enterAnimation = false;
                }
                audioVideoButtonContainer.getParent().requestDisallowInterceptTouchEvent(true);
                if (recordCircle != null) {
                    recordCircle.showWaves(true, false);
                }
            }
        }
    };

    private AnimationNotificationsLocker notificationsLocker = new AnimationNotificationsLocker();

    private class RecordDot extends View {

        private float alpha;
        private long lastUpdateTime;
        private boolean isIncr;
        boolean attachedToWindow;
        boolean playing;
        RLottieDrawable drawable;
        private boolean enterAnimation;
        private boolean externalFrameClock;
        private long externalBlinkStartMs = -1L;

        @Override
        protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            attachedToWindow = true;
            if (playing) {
                drawable.start();
            }
            drawable.setMasterParent(this);
        }

        @Override
        protected void onDetachedFromWindow() {
            super.onDetachedFromWindow();
            attachedToWindow = false;
            drawable.stop();
            drawable.setMasterParent(null);
        }

        public RecordDot(Context context) {
            super(context);
            int resId = R.raw.chat_audio_record_delete_2;
            drawable = new RLottieDrawable(resId, dp(28), dp(28), false, null);
            drawable.setInvalidateOnProgressSet(true);
            updateColors();
        }

        public void updateColors() {
            int dotColor = getThemedColor(Theme.key_chat_recordedVoiceDot);
            int background = getThemedColor(Theme.key_chat_messagePanelBackground);
            int greyColor = getThemedColor(Theme.key_chat_messagePanelVoiceDelete);
            redDotPaint.setColor(dotColor);
            drawable.beginApplyLayerColors();
            drawable.setLayerColor("Cup Red", dotColor);
            drawable.setLayerColor("Box Red", dotColor);
            drawable.setLayerColor("Cup Grey", greyColor);
            drawable.setLayerColor("Box Grey", greyColor);
            drawable.setLayerColor("Box_Grey 2", greyColor);
            drawable.setLayerColor("Line 1", greyColor);
            drawable.setLayerColor("Line 2", greyColor);
            drawable.setLayerColor("Line 3", greyColor);
            drawable.setLayerColor("Line 1 Dup", background);
            drawable.setLayerColor("Line 2 Dup", background);
            drawable.setLayerColor("Line 3 Dup", background);
            drawable.commitApplyLayerColors();
        }

        public void resetAlpha() {
            alpha = 1.0f;
            lastUpdateTime = System.currentTimeMillis();
            externalBlinkStartMs = -1L;
            isIncr = false;
            playing = false;
            drawable.stop();
            invalidate();
        }

        void setExternalFrameClock(boolean enabled) {
            externalFrameClock = enabled;
            lastUpdateTime = System.currentTimeMillis();
            externalBlinkStartMs = -1L;
            invalidate();
        }

        void onExternalFrame(long durationMs) {
            if (!externalFrameClock) return;
            if (enterAnimation || externalBlinkStartMs < 0L) {
                externalBlinkStartMs = durationMs;
                alpha = 1f;
            } else if (!playing) {
                long phaseMs = Math.max(0L, durationMs - externalBlinkStartMs) % 1200L;
                alpha = phaseMs < 600L
                        ? 1f - phaseMs / 600f
                        : (phaseMs - 600L) / 600f;
            }
            invalidate();
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            drawable.setBounds(0, 0, getMeasuredWidth(), getMeasuredHeight());
        }

        @Override
        protected void onDraw(Canvas canvas) {
            if (playing) {
                drawable.setAlpha((int) (255 * alpha));
            }
            redDotPaint.setAlpha((int) (255 * alpha));

            if (!externalFrameClock) {
                long now = System.currentTimeMillis();
                long dt = now - lastUpdateTime;
                if (enterAnimation) {
                    alpha = 1;
                } else {
                    if (!isIncr && !playing) {
                        alpha -= dt / 600.0f;
                        if (alpha <= 0) {
                            alpha = 0;
                            isIncr = true;
                        }
                    } else {
                        alpha += dt / 600.0f;
                        if (alpha >= 1) {
                            alpha = 1;
                            isIncr = false;
                        }
                    }
                }
                lastUpdateTime = now;
            }
            if (playing) {
                drawable.draw(canvas);
            }
            if (!playing || !drawable.hasBitmap()) {
                canvas.drawCircle(this.getMeasuredWidth() >> 1, this.getMeasuredHeight() >> 1, dp(5), redDotPaint);
            }
            if (!externalFrameClock) invalidate();
        }

        public void playDeleteAnimation() {
            playing = true;
            drawable.setProgress(0);
            if (attachedToWindow) {
                drawable.start();
            }
        }
    }

    private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Drawable micOutline;
    private Drawable cameraOutline;
    private Drawable micDrawable;
    private Drawable cameraDrawable;
    private Drawable sendDrawable;
    private RectF pauseRect = new RectF();
    private Rect sendRect = new Rect();
    private Rect rect = new Rect();

    private Drawable lockShadowDrawable;
    private final Theme.ResourcesProvider resourcesProvider;

    private final boolean isChat;

    private Runnable runEmojiPanelAnimation = new Runnable() {
        @Override
        public void run() {
            if (panelAnimation != null && !panelAnimation.isRunning()) {
                panelAnimation.start();
            }
        }
    };

    private final Property<? super View, Float> EMOJI_BUTTON_SCALE = new Property<View, Float>(Float.class, "emoji_button_scale") {
        @Override
        public Float get(View object) {
            return emojiButtonScale;
        }

        @Override
        public void set(View object, Float value) {
            emojiButtonScale = value;
            updateEmojiButtonParams();
        }
    };

    private final Property<? super View, Float> ATTACH_LAYOUT_ALPHA = new Property<View, Float>(Float.class, "attach_scale") {
        @Override
        public Float get(View object) {
            return attachLayoutAlpha;
        }

        @Override
        public void set(View object, Float value) {
            attachLayoutAlpha = value;
            updateAttachLayoutParams();
        }
    };

    private final Property<? super View, Float> EMOJI_BUTTON_ALPHA = new Property<View, Float>(Float.class, "emoji_button_alpha") {
        @Override
        public Float get(View object) {
            return emojiButtonAlpha;
        }

        @Override
        public void set(View object, Float value) {
            emojiButtonAlpha = value;
            updateEmojiButtonParams();
        }
    };

    private final Property<? super View, Float> ATTACH_LAYOUT_TRANSLATION_X = new Property<View, Float>(Float.class, "attach_layout_translation_x") {
        @Override
        public Float get(View object) {
            return attachLayoutTranslationX;
        }

        @Override
        public void set(View object, Float value) {
            attachLayoutTranslationX = value;
            updateAttachLayoutParams();
        }
    };

    private final Property<? super View, Float> MESSAGE_TEXT_TRANSLATION_X = new Property<View, Float>(Float.class, "message_text_translation_x") {
        @Override
        public Float get(View object) {
            return messageTextTranslationX;
        }

        @Override
        public void set(View object, Float value) {
            messageTextTranslationX = value;
            updateMessageTextParams();
        }
    };

    private boolean showTooltip;
    private long showTooltipStartTime;
    private float tooltipAlpha;

    public class ControlsView extends FrameLayout implements Theme.Colorable {

        private HintView2 pauseHint;
        private HintView2 onceHint;

        private Drawable tooltipBackground;
        private Drawable tooltipBackgroundArrow;
        private String tooltipMessage;
        private StaticLayout tooltipLayout;
        private float tooltipWidth;
        private TextPaint tooltipPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        Paint lockBackgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Paint lockPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Paint lockOutlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        Path path = new Path();
        private Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

        private CaptionContainerView.PeriodDrawable periodDrawable;

        private Drawable micDrawable;
        private Drawable vidDrawable;

        @Override
        public void setAlpha(float alpha) {
            super.setAlpha(alpha);
        }

        @Override
        protected boolean onSetAlpha(int alpha) {
            return super.onSetAlpha(alpha);
        }

        private VirtualViewHelper virtualViewHelper;

        public ControlsView(Context context) {
            super(context);

            virtualViewHelper = new VirtualViewHelper(this);
            ViewCompat.setAccessibilityDelegate(this, virtualViewHelper);

            periodDrawable = new CaptionContainerView.PeriodDrawable();
            periodDrawable.setCallback(this);
            periodDrawable.setValue(1, voiceOnce, false);

            lockOutlinePaint.setStyle(Paint.Style.STROKE);
            lockOutlinePaint.setStrokeCap(Paint.Cap.ROUND);
            lockOutlinePaint.setStrokeWidth(dpf2(1.7f));

            lockShadowDrawable = getResources().getDrawable(R.drawable.lock_round_shadow);
            ThemeBind.drawableColorFilter(this, lockShadowDrawable, Theme.key_chat_messagePanelVoiceLockShadow, ChatActivityEnterView.this.resourcesProvider, PorterDuff.Mode.MULTIPLY);
            tooltipBackground = Theme.createRoundRectDrawable(dp(5), getThemedColor(Theme.key_chat_gifSaveHintBackground));

            tooltipPaint.setTextSize(dp(14));
            tooltipBackgroundArrow = ContextCompat.getDrawable(context, R.drawable.tooltip_arrow);
            tooltipMessage = getString("SlideUpToLock", R.string.SlideUpToLock);

            radiiLeft[0] = radiiLeft[1] = radiiLeft[6] = radiiLeft[7] = dp(3);
            radiiLeft[2] = radiiLeft[3] = radiiLeft[4] = radiiLeft[5] = 0;

            radiiRight[0] = radiiRight[1] = radiiRight[6] = radiiRight[7] = 0;
            radiiRight[2] = radiiRight[3] = radiiRight[4] = radiiRight[5] = dp(3);

            micDrawable = getResources().getDrawable(R.drawable.input_mic).mutate();
            vidDrawable = getResources().getDrawable(R.drawable.input_video).mutate();

            setWillNotDraw(false);
            updateColors();
        }

        public void showTooltipIfNeed() {
            if (SharedConfig.lockRecordAudioVideoHint < 3) {
                showTooltip = true;
                showTooltipStartTime = System.currentTimeMillis();
            }
        }

        public void showPauseHint() {
            if (MessagesController.getGlobalMainSettings().getInt("voicepausehint", 0) > 3) {
                return;
            }
            hideHintView();
            pauseHint = new HintView2(getContext(), HintView2.DIRECTION_RIGHT);
            pauseHint.setJoint(1, 0);
            pauseHint.setMultilineText(true);
            pauseHint.setText(getString(R.string.VoicePauseHint));
            MessagesController.getGlobalMainSettings().edit().putInt("voicepausehint", MessagesController.getGlobalMainSettings().getInt("voicepausehint", 0) + 1).apply();
            addView(pauseHint, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.FILL, 0, 0, 54, 58));
            final HintView2 thisHintView = pauseHint;
            pauseHint.setOnHiddenListener(() -> {
                removeView(thisHintView);
                if (pauseHint == thisHintView) {
                    pauseHint = null;
                }
            });
            pauseHint.show();
        }

        public void showOnceHint() {
            hideHintView();
            onceHint = new HintView2(getContext(), HintView2.DIRECTION_RIGHT);
            onceHint.setJoint(1, 0);
            onceHint.setMultilineText(true);
            int text;
            if (isInVideoMode) {
                text = voiceOnce ? R.string.VideoSetOnceHintEnabled : R.string.VideoSetOnceHint;
            } else {
                text = voiceOnce ? R.string.VoiceSetOnceHintEnabled : R.string.VoiceSetOnceHint;
            }
            onceHint.setText(AndroidUtilities.replaceTags(getString(text)));
            onceHint.setMaxWidthPx(HintView2.cutInFancyHalf(onceHint.getText(), onceHint.getTextPaint()));
            if (voiceOnce) {
                onceHint.setIcon(R.raw.fire_on);
            } else {
                MessagesController.getGlobalMainSettings().edit().putInt("voiceoncehint", MessagesController.getGlobalMainSettings().getInt("voiceoncehint", 0) + 1).apply();
            }
            addView(onceHint, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.FILL, 0, 0, 54, 58));
            final HintView2 thisHintView = onceHint;
            onceHint.setOnHiddenListener(() -> {
                removeView(thisHintView);
                if (onceHint == thisHintView) {
                    onceHint = null;
                }
            });
            onceHint.show();
        }

        public void hideHintView() {
            if (pauseHint != null) {
                HintView2 oldPauseHintView = pauseHint;
                oldPauseHintView.setOnHiddenListener(() -> removeView(oldPauseHintView));
                oldPauseHintView.hide();
                pauseHint = null;
            }
            if (onceHint != null) {
                HintView2 oldHintView = onceHint;
                oldHintView.setOnHiddenListener(() -> removeView(oldHintView));
                oldHintView.hide();
                onceHint = null;
            }
        }

        private int lastSize;

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            int currentSize = MeasureSpec.getSize(widthMeasureSpec);
            int h = dp(194 + DEFAULT_HEIGHT + 12);
            if (lastSize != currentSize) {
                lastSize = currentSize;
                tooltipLayout = new StaticLayout(tooltipMessage, tooltipPaint, dp(220), Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, true);
                int n = tooltipLayout.getLineCount();
                tooltipWidth = 0;
                for (int i = 0; i < n; i++) {
                    float w = tooltipLayout.getLineWidth(i);
                    if (w > tooltipWidth) {
                        tooltipWidth = w;
                    }
                }
            }
//            if (tooltipLayout != null && tooltipLayout.getLineCount() > 1) {
//                h += tooltipLayout.getHeight() - tooltipLayout.getLineBottom(0);
//            }
            super.onMeasure(
                widthMeasureSpec,
                MeasureSpec.makeMeasureSpec(h, MeasureSpec.EXACTLY)
            );
        }

        private final RectF rectF = new RectF();
        public final RectF onceRect = new RectF();

        private long lastUpdateTime;

        private final Path path2 = new Path();
        private final float[] radiiLeft = new float[8], radiiRight = new float[8];

        private AnimatedFloat hidePauseT = new AnimatedFloat(this, 0, 350, CubicBezierInterpolator.EASE_OUT_QUINT);

        @Override
        protected void onDraw(Canvas canvas) {
            float sc;
            if (scale <= 0.5f) {
                sc = scale / 0.5f;
            } else if (scale <= 0.75f) {
                sc = 1.0f - (scale - 0.5f) / 0.25f * 0.1f;
            } else {
                sc = 0.9f + (scale - 0.75f) / 0.25f * 0.1f;
            }
            long dt = System.currentTimeMillis() - lastUpdateTime;
            lastUpdateTime = System.currentTimeMillis();

            float yAdd = 0;
            if (lockAnimatedTranslation != 10000) {
                yAdd = Math.max(0, (int) (startTranslation - lockAnimatedTranslation));
                if (yAdd > dp(57)) {
                    yAdd = dp(57);
                }
            }

            final int cx = getMeasuredWidth() - AndroidUtilities.dp2(26);
            float moveProgress = 1.0f - yAdd / dp(57);
            final float multilinTooltipOffset = getMeasuredHeight() - dp(194);

            float lockSize;
            float lockY;
            float lockTopY;
            float lockMiddleY;

            float lockRotation;
            float transformToPauseProgress = 0;
            if (sendButtonVisible) {
                lockSize = dp(36);
                lockY = dp(60) + multilinTooltipOffset + dpf2(30) * (1.0f - sc) - yAdd + dpf2(14f) * moveProgress;

                lockMiddleY = lockY + lockSize / 2f - dpf2(8) + dpf2(2);
                lockTopY =    lockY + lockSize / 2f - dpf2(16) + dpf2(2);
                float snapRotateBackProgress = moveProgress > 0.4f ? 1f : moveProgress / 0.4f;

                lockRotation = 9 * (1f - moveProgress) * (1f - snapAnimationProgress) - 15 * snapAnimationProgress * (1f - snapRotateBackProgress);

                transformToPauseProgress = moveProgress;
            } else {
                lockSize = dp(36) + (int) (dp(14) * moveProgress);
                lockY = dp(60) + multilinTooltipOffset + (int) (dp(30) * (1.0f - sc)) - (int) yAdd + (moveProgress) * idleProgress * -dp(8);
                lockMiddleY = lockY + lockSize / 2f - dpf2(8) + dpf2(2) + dpf2(2) * moveProgress;
                lockTopY =    lockY + lockSize / 2f - dpf2(16) + dpf2(2) + dpf2(2) * moveProgress;
                lockRotation = 9 * (1f - moveProgress);
                snapAnimationProgress = 0;
            }

            if ((showTooltip && System.currentTimeMillis() - showTooltipStartTime > 200) || tooltipAlpha != 0f) {
                if (moveProgress < 0.8f || sendButtonVisible || exitTransition != 0 || transformToSeekbar != 0) {
                    showTooltip = false;
                }
                if (showTooltip) {
                    if (tooltipAlpha != 1f) {
                        tooltipAlpha += dt / 150f;
                        if (tooltipAlpha >= 1f) {
                            tooltipAlpha = 1f;
                            SharedConfig.increaseLockRecordAudioVideoHintShowed();
                        }
                    }
                } else {
                    tooltipAlpha -= dt / 150f;
                    if (tooltipAlpha < 0) {
                        tooltipAlpha = 0f;
                    }
                }


                int alphaInt = (int) (tooltipAlpha * 255);

                tooltipBackground.setAlpha(alphaInt);
                tooltipBackgroundArrow.setAlpha(alphaInt);
                tooltipPaint.setAlpha(alphaInt);

                if (tooltipLayout != null) {
                    canvas.save();
                    rectF.set(0, 0, getMeasuredWidth(), getMeasuredHeight());
                    canvas.translate(getMeasuredWidth() - tooltipWidth - dp(44), multilinTooltipOffset + dpf2(16));
                    tooltipBackground.setBounds(
                            -dp(8), -dp(2),
                            (int) (tooltipWidth + dp(36)), (int) (tooltipLayout.getHeight() + dpf2(4))
                    );
                    tooltipBackground.draw(canvas);
                    tooltipLayout.draw(canvas);
                    canvas.restore();

                    canvas.save();
                    canvas.translate(getMeasuredWidth() - dp(26), multilinTooltipOffset + dpf2(16 + 1) + tooltipLayout.getHeight() / 2f - idleProgress * dpf2(3f));
                    path.reset();
                    path.setLastPoint(-dpf2(5), dpf2(4));
                    path.lineTo(0, 0);
                    path.lineTo(dpf2(5), dpf2(4));

                    p.setColor(Color.WHITE);
                    p.setAlpha(alphaInt);
                    p.setStyle(Paint.Style.STROKE);
                    p.setStrokeCap(Paint.Cap.ROUND);
                    p.setStrokeJoin(Paint.Join.ROUND);
                    p.setStrokeWidth(dpf2(1.5f));
                    canvas.drawPath(path, p);
                    canvas.restore();

                    canvas.save();
                    tooltipBackgroundArrow.setBounds(
                            cx - tooltipBackgroundArrow.getIntrinsicWidth() / 2, (int) (tooltipLayout.getHeight() + multilinTooltipOffset + dpf2(20)),
                            cx + tooltipBackgroundArrow.getIntrinsicWidth() / 2, (int) (tooltipLayout.getHeight() + multilinTooltipOffset + dpf2(20)) + tooltipBackgroundArrow.getIntrinsicHeight()
                    );
                    tooltipBackgroundArrow.draw(canvas);
                    canvas.restore();
                }
            }

            float progressToSeekbarStep1 = 0f;
            float progressToSeekbarStep2 = 0;
            float exitProgress2 = 0f;
            float hidePause = hidePauseT.set(isInVideoMode && millisecondsRecorded >= 59_000);

            if (transformToSeekbar != 0 && audioTimelineView != null) {
                float step1Time = 0.38f;
                float step2Time = 0.25f;

                progressToSeekbarStep1 = transformToSeekbar > step1Time ? 1f : transformToSeekbar / step1Time;
                progressToSeekbarStep2 = transformToSeekbar > step1Time + step2Time ? 1f : Math.max(0, (transformToSeekbar - step1Time) / step2Time);

                progressToSeekbarStep1 = CubicBezierInterpolator.EASE_BOTH.getInterpolation(progressToSeekbarStep1);
                progressToSeekbarStep2 = CubicBezierInterpolator.EASE_BOTH.getInterpolation(progressToSeekbarStep2);
            } else if (exitTransition != 0) {
                float step1Time = 0.6f;
                float step2Time = 0.4f;

                progressToSeekbarStep1 = exitTransition > step1Time ? 1f : exitTransition / step1Time;
                exitProgress2 = messageTransitionIsRunning ? exitTransition : Math.max(0, (exitTransition - step1Time) / step2Time);

                progressToSeekbarStep1 = CubicBezierInterpolator.EASE_BOTH.getInterpolation(progressToSeekbarStep1);
                exitProgress2 = CubicBezierInterpolator.EASE_BOTH.getInterpolation(exitProgress2);
            }

            canvas.save();
            canvas.clipRect(0, 0, getMeasuredWidth(), getMeasuredHeight() - textFieldContainer.getMeasuredHeight());
            float translation = 0;
            if (1f - controlsScale != 0) {
                translation = 1f - controlsScale;
//            } else if (progressToSeekbarStep2 != 0) {
//                translation = progressToSeekbarStep2;
            } else if (exitProgress2 != 0) {
                translation = exitProgress2;
            }
            if (slideToCancelProgress < 0.7f || canceledByGesture) {
                showTooltip = false;
                if (slideToCancelLockProgress != 0) {
                    slideToCancelLockProgress -= 0.12f;
                    if (slideToCancelLockProgress < 0) {
                        slideToCancelLockProgress = 0;
                    }
                }
            } else {
                if (slideToCancelLockProgress != 1f) {
                    slideToCancelLockProgress += 0.12f;
                    if (slideToCancelLockProgress > 1f) {
                        slideToCancelLockProgress = 1f;
                    }
                }
            }

            float maxTranslationDy = dpf2(72);
            float dy = (
                maxTranslationDy * translation
                + dpf2(24) * (progressToSeekbarStep1) * (1f - translation)
                + maxTranslationDy * (1f - slideToCancelLockProgress)
            );
            if (dy > maxTranslationDy) {
                dy = maxTranslationDy;
            }
            float s = (1f - hidePause) * controlsScale * (1f - exitProgress2) * slideToCancelLockProgress;
            canvas.scale(s, s, cx, lockMiddleY + dy);

            rectF.set(cx - dpf2(18), lockY + dy, cx + dpf2(18), lockY + dy + lockSize);

            if (lockBackgroundDrawable != null){
                lockBackgroundDrawable.setBounds(
                        (int) (rectF.left - dpf2(3)), (int) (rectF.top - dpf2(3)),
                        (int) (rectF.right + dpf2(3)), (int) (rectF.bottom + dpf2(3))
                );
                lockBackgroundDrawable.draw(canvas);
            } else {
                lockShadowDrawable.setBounds(
                        (int) (rectF.left - dpf2(3)), (int) (rectF.top - dpf2(3)),
                        (int) (rectF.right + dpf2(3)), (int) (rectF.bottom + dpf2(3))
                );
                lockShadowDrawable.draw(canvas);
                canvas.drawRoundRect(rectF, dpf2(18), dpf2(18), lockBackgroundPaint);
            }


            pauseRect.set(rectF);
            scale(pauseRect, s);
            if (pauseHint != null) {
                pauseHint.setJointPx(0, rectF.centerY());
                pauseHint.invalidate();
            }

            rectF.set(
                cx - dpf2(6) - dpf2(2) * (1f - transformToPauseProgress),
                lockMiddleY + dy - dpf2(2) * (1f - transformToPauseProgress),
                cx + dp(6) + dpf2(2) * (1f - transformToPauseProgress),
                lockMiddleY + dy + dp(12) + dpf2(2) * (1f - transformToPauseProgress)
            );
            float lockBottom = rectF.bottom;
            float locCx = rectF.centerX();
            float locCy = rectF.centerY();
            canvas.save();

            final float transformToResume = Utilities.clamp(transformToSeekbar * 2f, 1, 0);
            final int wasAlpha = lockPaint.getAlpha();
            final int saveToLayer = canvas.saveLayerAlpha(
                    locCx - dp(24),
                    locCy - dp(24),
                    locCx + dp(24),
                    locCy + dp(24), (int) (wasAlpha * (1f - transformToResume)), Canvas.ALL_SAVE_FLAG);
            lockOutlinePaint.setAlpha(255);
            lockPaint.setAlpha(255);

            canvas.translate(0, dpf2(2) * (1f - moveProgress));
            canvas.rotate(lockRotation, locCx, locCy);

            if (transformToPauseProgress != 1f) {
                AndroidUtilities.rectTmp.set(0, 0, dpf2(8), dpf2(8));
                canvas.save();
                canvas.clipRect(0, 0, getMeasuredWidth(), dy + lockBottom + dpf2(2) * (1f - moveProgress));
                canvas.translate(cx - dpf2(4), rectF.top - dp(6) - lerp(dpf2(2), dpf2(1.5f) * (1f - idleProgress), moveProgress) + dpf2(12) * transformToPauseProgress + dpf2(2) * snapAnimationProgress);
                if (lockRotation > 0) {
                    canvas.rotate(lockRotation, dp(8), dp(8));
                }
                canvas.drawLine(dpf2(8), dpf2(4), dpf2(8), dpf2(6) + dpf2(4) * (1f - transformToPauseProgress), lockOutlinePaint);
                canvas.drawArc(AndroidUtilities.rectTmp, 0, -180, false, lockOutlinePaint);
                canvas.drawLine(
                        0, dpf2(4),
                        0, dpf2(4) + dpf2(4) * idleProgress * (moveProgress) * (sendButtonVisible ? 0 : 1) + dpf2(4) * snapAnimationProgress * (1f - moveProgress),
                        lockOutlinePaint
                );
                canvas.restore();
            }

            Drawable resumeDrawable = null;
            if (transformToResume > 0) {
                resumeDrawable = isInVideoMode ? vidDrawable : micDrawable;
            }

            if (transformToPauseProgress > 0) {
                if (periodBackgroundDrawable == null) {
                    canvas.drawRoundRect(rectF, dpf2(3), dpf2(3), lockBackgroundPaint);
                }

                path2.rewind();
                AndroidUtilities.rectTmp.set(rectF);
                AndroidUtilities.rectTmp.right = rectF.centerX() - dp(1.66f) * transformToPauseProgress;
                radiiLeft[0] = radiiLeft[1] = radiiLeft[6] = radiiLeft[7] = lerp(dp(3), dp(1.5f), transformToPauseProgress);
                radiiLeft[2] = radiiLeft[3] = radiiLeft[4] = radiiLeft[5] = dp(1.5f) * transformToPauseProgress;
                path2.addRoundRect(AndroidUtilities.rectTmp, radiiLeft, Path.Direction.CW);
                AndroidUtilities.rectTmp.set(rectF);
                AndroidUtilities.rectTmp.left = rectF.centerX() + dp(1.66f) * transformToPauseProgress;
                radiiRight[2] = radiiRight[3] = radiiRight[4] = radiiRight[5] = lerp(dp(3), dp(1.5f), transformToPauseProgress);
                radiiRight[0] = radiiRight[1] = radiiRight[6] = radiiRight[7] = dp(1.5f) * transformToPauseProgress;
                path2.addRoundRect(AndroidUtilities.rectTmp, radiiRight, Path.Direction.CW);
                canvas.drawPath(path2, lockPaint);
            } else {
                canvas.drawRoundRect(rectF, dpf2(3), dpf2(3), lockPaint);
            }
            lockPaint.setAlpha(wasAlpha);
            lockOutlinePaint.setAlpha(wasAlpha);
            canvas.restoreToCount(saveToLayer);

            if (resumeDrawable != null) {
                final float _s = 0.9285f;
                AndroidUtilities.rectTmp2.set(
                    (int) (rectF.centerX() - resumeDrawable.getIntrinsicWidth() / 2 * _s),
                    (int) (rectF.centerY() - resumeDrawable.getIntrinsicHeight() / 2 * _s),
                    (int) (rectF.centerX() + resumeDrawable.getIntrinsicWidth() / 2 * _s),
                    (int) (rectF.centerY() + resumeDrawable.getIntrinsicHeight() / 2 * _s)
                );
                resumeDrawable.setBounds(AndroidUtilities.rectTmp2);
                resumeDrawable.setAlpha((int) (0xFF * transformToResume));
                resumeDrawable.draw(canvas);
            }

            if (transformToPauseProgress != 1) {
                canvas.drawCircle(locCx, locCy, dpf2(2) * (1f - transformToPauseProgress), lockBackgroundPaint);
            }
            canvas.restore();
            canvas.restore();

            final float cy = lerp(lockY, getMeasuredHeight() - dp(118), Math.max(exitTransition, Math.min(progressToSeekbarStep1, slideToCancelLockProgress))) + dy + dp(38) * hidePause;
            rectF.set(cx - dpf2(18), cy, cx + dpf2(18), cy + lockSize);
            onceVisible = delegate != null && delegate.onceVoiceAvailable();
            if (onceVisible) {
                final float onceOffset = dpf2(12);
                rectF.set(
                    rectF.left, rectF.top - dpf2(36) - onceOffset, rectF.right, rectF.top - onceOffset
                );
                if (onceHint != null) {
                    onceHint.setJointPx(0, rectF.centerY());
                    onceHint.invalidate();
                }
                onceRect.set(rectF);
                canvas.save();
                final float s2 = controlsScale * (1f - exitTransition) * slideToCancelLockProgress * snapAnimationProgress;
                canvas.scale(s2, s2, rectF.centerX(), rectF.centerY());

                if (periodBackgroundDrawable != null) {
                    periodBackgroundDrawable.setBounds(
                        (int) (rectF.left - dpf2(3)), (int) (rectF.top - dpf2(3)),
                        (int) (rectF.right + dpf2(3)), (int) (rectF.bottom + dpf2(3))
                    );
                    periodBackgroundDrawable.draw(canvas);
                } else {
                    lockShadowDrawable.setBounds(
                            (int) (rectF.left - dpf2(3)), (int) (rectF.top - dpf2(3)),
                            (int) (rectF.right + dpf2(3)), (int) (rectF.bottom + dpf2(3))
                    );
                    lockShadowDrawable.draw(canvas);
                    canvas.drawRoundRect(rectF, dpf2(18), dpf2(18), lockBackgroundPaint);
                }
                periodDrawable.setBounds((int) rectF.left, (int) rectF.top, (int) rectF.right, (int) rectF.bottom);
                periodDrawable.draw(canvas);
                canvas.restore();
            }
        }

        @Override
        protected boolean dispatchHoverEvent(MotionEvent event) {
            return super.dispatchHoverEvent(event) || virtualViewHelper.dispatchHoverEvent(event);
        }

        private BlurredBackgroundColorProviderThemed colorProvider;
        private boolean useGlassDesign;
        private BlurredBackgroundDrawable lockBackgroundDrawable;
        private BlurredBackgroundDrawable periodBackgroundDrawable;

        public void setBlurredBackgroundFactory(BlurredBackgroundDrawableViewFactory factory) {
            useGlassDesign = true;

            if (colorProvider == null) {
                colorProvider = new BlurredBackgroundColorProviderThemed(resourcesProvider, Theme.key_chat_messagePanelVoiceLockBackground);
            }

            lockBackgroundDrawable = factory.create(this, colorProvider);
            lockBackgroundDrawable.setRadius(dp(18));
            lockBackgroundDrawable.setPadding(dp(3));

            periodBackgroundDrawable = factory.create(this, colorProvider);
            periodBackgroundDrawable.setRadius(dp(18));
            periodBackgroundDrawable.setPadding(dp(3));

            updateColors();
        }

        @Override
        public void updateColors() {
            if (periodDrawable == null || tooltipPaint == null || micDrawable == null || vidDrawable == null) {
                return;
            }
            if (colorProvider != null) {
                colorProvider.updateColors();
            }
            if (lockBackgroundDrawable != null) {
                lockBackgroundDrawable.updateColors();
            }
            if (periodBackgroundDrawable != null) {
                periodBackgroundDrawable.updateColors();
            }

            periodDrawable.updateColors(
                getThemedColor(useGlassDesign ? Theme.key_glass_defaultIcon : Theme.key_chat_messagePanelVoiceLock),
                getThemedColor(Theme.key_chat_messagePanelVoiceBackground),
                0xFFFFFFFF
            );

            tooltipPaint.setColor(getThemedColor(Theme.key_chat_gifSaveHintText));
            if (tooltipBackground != null) {
                Theme.setDrawableColor(tooltipBackground, getThemedColor(Theme.key_chat_gifSaveHintBackground));
            } else {
                tooltipBackground = Theme.createRoundRectDrawable(dp(5), getThemedColor(Theme.key_chat_gifSaveHintBackground));
            }
            final int tooltipArrowColor = getThemedColor(Theme.key_chat_gifSaveHintBackground);
            if (tooltipBackgroundArrow != null && tooltipArrowColor != lastTooltipArrowColor) {
                lastTooltipArrowColor = tooltipArrowColor;
                tooltipBackgroundArrow.setColorFilter(new PorterDuffColorFilter(tooltipArrowColor, PorterDuff.Mode.SRC_IN));
            }

            lockBackgroundPaint.setColor(getThemedColor(Theme.key_chat_messagePanelVoiceLockBackground));
            lockPaint.setColor(getThemedColor(useGlassDesign ? Theme.key_glass_defaultIcon : Theme.key_chat_messagePanelVoiceLock));
            lockOutlinePaint.setColor(getThemedColor(useGlassDesign ? Theme.key_glass_defaultIcon : Theme.key_chat_messagePanelVoiceLock));

            final int lockIconColor = getThemedColor(useGlassDesign ? Theme.key_glass_defaultIcon : Theme.key_chat_messagePanelVoiceLock);
            if (!lockIconColorApplied || lockIconColor != lastLockIconColor) {
                lockIconColorApplied = true;
                lastLockIconColor = lockIconColor;
                micDrawable.setColorFilter(new PorterDuffColorFilter(lockIconColor, PorterDuff.Mode.SRC_IN));
                vidDrawable.setColorFilter(new PorterDuffColorFilter(lockIconColor, PorterDuff.Mode.SRC_IN));
            }
        }

        private int lastTooltipArrowColor, lastLockIconColor;
        private boolean lockIconColorApplied;

        private void scale(RectF rect, float s) {
            final float cx = rect.centerX(), cy = rect.centerY();
            rect.left = lerp(cx, rect.left, s);
            rect.right = lerp(cx, rect.right, s);
            rect.top = lerp(cy, rect.top, s);
            rect.bottom = lerp(cy, rect.bottom, s);
        }

        private boolean oncePressed;
        private boolean pausePressed;

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            final int x = (int) event.getX();
            final int y = (int) event.getY();

            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                if (sendButtonVisible) {
                    pausePressed = pauseRect.contains(x, y);
                }
                if (onceVisible && (recordCircle != null && snapAnimationProgress > .1f)) {
                    oncePressed = onceRect.contains(x, y);
                }
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                if (pausePressed && pauseRect.contains(x, y)) {
                    if (isInVideoMode()) {
                        if (slideText != null) {
                            slideText.setEnabled(false);
                        }
                        delegate.toggleVideoRecordingPause();
                    } else {
                        final Runnable resume = () -> {
                            if (!MediaController.getInstance().isRecordingPaused()) {
                                MessagesController.getGlobalMainSettings().edit().putInt("voicepausehint", 3).apply();
                            }
                            if (sendButtonVisible) {
                                calledRecordRunnable = true;
                            }
                            MediaController.getInstance().toggleRecordingPause(voiceOnce);
                            delegate.needStartRecordAudio(0);
                            if (slideText != null) {
                                slideText.setEnabled(false);
                            }
                        };
                        if (pauseHint != null && pauseHint.shown()) {
                            hideHintView();
                        }
                        if (audioTimelineView != null) {
                            audioTimelineView.setPlaying(false);
                        }
                        if (MediaController.getInstance().isRecordingPaused() && (audioTimelineView.getAudioLeft() > 0.01f || audioTimelineView.getAudioRight() < 0.99f)) {
                            final Runnable trim = () -> {
                                millisecondsRecorded = audioTimelineView.getAudioRightMs() - audioTimelineView.getAudioLeftMs();
                                MediaController.getInstance().trimCurrentRecording(audioTimelineView.getAudioLeftMs(), audioTimelineView.getAudioRightMs(), resume);
                            };
                            if (MessagesController.getGlobalMainSettings().getBoolean("trimvoicehint", true)) {
                                new AlertDialog.Builder(getContext(), resourcesProvider)
                                    .setTitle(getString(R.string.RecordingTrimTitle))
                                    .setMessage(getString(R.string.RecordingTrimText))
                                    .setPositiveButton(getString(R.string.OK), (di, w) -> {
                                        trim.run();
                                        MessagesController.getGlobalMainSettings().edit().putBoolean("trimvoicehint", false).apply();
                                    })
                                    .setNegativeButton(getString(R.string.Cancel), null)
                                    .show();
                            } else {
                                trim.run();
                            }
                        } else {
                            resume.run();
                        }
                    }
                    pausePressed = oncePressed = false;
                    return true;
                } else if (oncePressed && onceRect.contains(x, y)) {
                    voiceOnce = !voiceOnce;
                    periodDrawable.setValue(1, voiceOnce, true);
                    MediaDataController.getInstance(currentAccount).toggleDraftVoiceOnce(dialog_id, parentFragment != null && parentFragment.isTopic ? parentFragment.getTopicId() : 0, voiceOnce);
                    if (voiceOnce) {
                        showOnceHint();
                    } else {
                        hideHintView();
                    }
                    invalidate();
                    pausePressed = oncePressed = false;
                    return true;
                }
                pausePressed = oncePressed = false;
            } else if (event.getAction() == MotionEvent.ACTION_CANCEL) {
                pausePressed = oncePressed = false;
            }
            return pausePressed || oncePressed;
        }

        @Override
        protected boolean verifyDrawable(@NonNull Drawable who) {
            return who == periodDrawable || super.verifyDrawable(who);
        }


        private class VirtualViewHelper extends ExploreByTouchHelper {

            public VirtualViewHelper(@NonNull View host) {
                super(host);
            }

            @Override
            protected int getVirtualViewAt(float x, float y) {
                if (sendButtonVisible && recordCircle != null && pauseRect.contains(x, y)) {
                    return 2;
                }
                if (onceVisible && (recordCircle != null && snapAnimationProgress > .1f) && onceRect.contains(x, y)) {
                    return 4;
                }
                return HOST_ID;
            }

            @Override
            protected void getVisibleVirtualViews(List<Integer> list) {
                if (sendButtonVisible) {
                    list.add(2);
                }
                if (onceVisible && (recordCircle != null && snapAnimationProgress > .1f)) {
                    list.add(4);
                }
            }

            @Override
            protected void onPopulateNodeForVirtualView(int id, @NonNull AccessibilityNodeInfoCompat info) {
                if (id == 2) {
                    rect.set((int) pauseRect.left, (int) pauseRect.top, (int) pauseRect.right, (int) pauseRect.bottom);
                    info.setBoundsInParent(rect);
                    info.setText(getString(transformToSeekbar > .5f ? R.string.AccActionResume : R.string.AccActionPause));
                } else if (id == 4) {
                    rect.set((int) onceRect.left, (int) onceRect.top, (int) onceRect.right, (int) onceRect.bottom);
                    info.setBoundsInParent(rect);
                    info.setText(getString(voiceOnce ? R.string.AccActionOnceDeactivate : R.string.AccActionOnceActivate));
                }
            }

            @Override
            protected boolean onPerformActionForVirtualView(int id, int action, @Nullable Bundle args) {
                return true;
            }
        }
    }

    private float scale;
    private float controlsScale;
    private float slideToCancelProgress;
    private float startTranslation;
    private float lockAnimatedTranslation;
    private float exitTransition;
    private float snapAnimationProgress;
    float idleProgress;
    private float transformToSeekbar;
    private float progressToSeekbarStep1, progressToSeekbarStep2;
    private float slideToCancelLockProgress;
    private boolean canceledByGesture;
    private boolean sendButtonVisible;
    private int slideDelta;

    @Keep
    public float getExitTransition() {
        return exitTransition;
    }

    @Keep
    public void setExitTransition(float value) {
        exitTransition = value;
        if (recordCircle != null) {
            recordCircle.invalidate();
        }
    }

    @Keep
    public float getSlideToCancelProgress() {
        return slideToCancelProgress;
    }

    @Keep
    public void setSlideToCancelProgress(float value) {
        slideToCancelProgress = value;
        float distance = getMeasuredWidth() * 0.35f;
        if (distance > dp(140)) {
            distance = dp(140);
        }
        slideDelta = (int) (-distance * (1f - slideToCancelProgress));
        if (recordCircle != null) {
            recordCircle.invalidate();
        }
    }

    @Keep
    public float getLockAnimatedTranslation() {
        return lockAnimatedTranslation;
    }
    @Keep
    public void setLockAnimatedTranslation(float value) {
        lockAnimatedTranslation = value;
        if (recordCircle != null) {
            recordCircle.invalidate();
        }
    }

    @Keep
    public void setSnapAnimationProgress(float value) {
        snapAnimationProgress = value;
        invalidate();
    }

    public boolean seekbarVisible() {
        return !recordIsCanceled && transformToSeekbar > 0;
    }

    public class RecordCircle extends View {

        private float amplitude;
        private float animateToAmplitude;
        private float animateAmplitudeDiff;
        private long lastUpdateTime;
        private boolean pressed;
        public float progressToSeekbarStep3;
        private float progressToSendButton;

        public float iconScale;

        BlobDrawable tinyWaveDrawable = new BlobDrawable(11, LiteMode.FLAGS_CHAT);
        BlobDrawable bigWaveDrawable = new BlobDrawable(12, LiteMode.FLAGS_CHAT);

        private float circleRadius = dpf2(41);
        private float circleRadiusAmplitude = dp(30);
        RectF rectF = new RectF();

        boolean incIdle;

        private VirtualViewHelper virtualViewHelper;

        private int paintAlpha;
        private float touchSlop;

        private float lastMovingX;
        private float lastMovingY;

        private float wavesEnterAnimation = 0f;
        private boolean showWaves = true;

        public float drawingCx, drawingCy, drawingCircleRadius;

        public boolean voiceEnterTransitionInProgress;
        public boolean skipDraw;
        private int lastSize;

        public RecordCircle(Context context) {
            super(context);

            virtualViewHelper = new VirtualViewHelper(this);
            ViewCompat.setAccessibilityDelegate(this, virtualViewHelper);

            tinyWaveDrawable.minRadius = dp(47);
            tinyWaveDrawable.maxRadius = dp(55);
            tinyWaveDrawable.generateBlob();

            bigWaveDrawable.minRadius = dp(47);
            bigWaveDrawable.maxRadius = dp(55);
            bigWaveDrawable.generateBlob();
            iconScale = 1f;

            final ViewConfiguration vc = ViewConfiguration.get(context);
            touchSlop = vc.getScaledTouchSlop();
            touchSlop *= touchSlop;

            updateColors();
        }

        private void checkDrawables() {
            if (micDrawable != null) {
                return;
            }
            micDrawable = getResources().getDrawable(R.drawable.input_mic_pressed).mutate();
            ThemeBind.drawableColorFilter(this, micDrawable, Theme.key_chat_messagePanelVoicePressed, ChatActivityEnterView.this.resourcesProvider, PorterDuff.Mode.MULTIPLY);

            cameraDrawable = getResources().getDrawable(R.drawable.input_video_pressed).mutate();
            ThemeBind.drawableColorFilter(this, cameraDrawable, Theme.key_chat_messagePanelVoicePressed, ChatActivityEnterView.this.resourcesProvider, PorterDuff.Mode.MULTIPLY);

            sendDrawable = getResources().getDrawable(R.drawable.attach_send).mutate();
            ThemeBind.drawableColorFilter(this, sendDrawable, Theme.key_chat_messagePanelVoicePressed, ChatActivityEnterView.this.resourcesProvider, PorterDuff.Mode.MULTIPLY);

            micOutline = getResources().getDrawable(R.drawable.input_mic).mutate();
            ThemeBind.drawableColorFilter(this, micOutline, Theme.key_glass_defaultIcon, ChatActivityEnterView.this.resourcesProvider, PorterDuff.Mode.MULTIPLY);

            cameraOutline = getResources().getDrawable(R.drawable.input_video).mutate();
            ThemeBind.drawableColorFilter(this, cameraOutline, Theme.key_glass_defaultIcon, ChatActivityEnterView.this.resourcesProvider, PorterDuff.Mode.MULTIPLY);
        }

        public void setAmplitude(double value) {
            bigWaveDrawable.setValue((float) (Math.min(WaveDrawable.MAX_AMPLITUDE, value) / WaveDrawable.MAX_AMPLITUDE), true);
            tinyWaveDrawable.setValue((float) (Math.min(WaveDrawable.MAX_AMPLITUDE, value) / WaveDrawable.MAX_AMPLITUDE), false);

            animateToAmplitude = (float) (Math.min(WaveDrawable.MAX_AMPLITUDE, value) / WaveDrawable.MAX_AMPLITUDE);
            animateAmplitudeDiff = (animateToAmplitude - amplitude) / (100 + 500.0f * WaveDrawable.animationSpeedCircle);

            invalidate();
        }

        public float getScale() {
            return scale;
        }

        @Keep
        public void setScale(float value) {
            scale = value;
            invalidate();
        }

        public float getControlsScale() {
            return controlsScale;
        }

        @Keep
        public void setControlsScale(float value) {
            controlsScale = value;
            if (controlsView != null) {
                controlsView.invalidate();
            }
        }

        public boolean isSendButtonVisible() {
            return sendButtonVisible;
        }

        public void setSendButtonInvisible() {
            sendButtonVisible = false;
            invalidate();
            if (controlsView != null) {
                controlsView.invalidate();
            }
        }

        public void resetLockTranslation(boolean toLock) {
            if (!toLock) {
                sendButtonVisible = false;
                lockAnimatedTranslation = -1;
                startTranslation = -1;
                slideToCancelProgress = 1f;
                slideToCancelLockProgress = 1f;
                snapAnimationProgress = 0;
                controlsScale = 0f;
            }
            invalidate();
            transformToSeekbar = 0;
            isRecordingStateChanged();
            exitTransition = 0;
            iconScale = 1f;
            scale = 0f;
            tooltipAlpha = 0f;
            showTooltip = false;
            progressToSendButton = 0f;
            canceledByGesture = false;
            if (controlsView != null) {
                controlsView.invalidate();
            }
        }

        public int setLockTranslation(float value) {
            if (sendButtonVisible) {
                return 2;
            }
            if (lockAnimatedTranslation == -1) {
                startTranslation = value;
            }
            lockAnimatedTranslation = value;
            invalidate();
            if (canceledByGesture || slideToCancelProgress < 0.7f) {
                return 1;
            }
            if (startTranslation - lockAnimatedTranslation >= dp(57)) {
                sendButtonVisible = true;
                if (controlsView != null) {
                    controlsView.showPauseHint();
                }
                return 2;
            }
            return 1;
        }

        @SuppressLint("DrawAllocation")
        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            int currentSize = MeasureSpec.getSize(widthMeasureSpec);
            int h = dp(194);
            super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(h, MeasureSpec.EXACTLY));

            float distance = getMeasuredWidth() * 0.35f;
            if (distance > dp(140)) {
                distance = dp(140);
            }
            slideDelta = (int) (-distance * (1f - slideToCancelProgress));
        }

        @Override
        protected void onDraw(Canvas canvas) {
            if (skipDraw) {
                return;
            }
            float multilinTooltipOffset = 0;
//            if (tooltipLayout != null && tooltipLayout.getLineCount() > 1) {
//                multilinTooltipOffset = tooltipLayout.getHeight() - tooltipLayout.getLineBottom(0);
//            }
            int cx = getMeasuredWidth() - AndroidUtilities.dp2(26);
            int cy = (int) (dp(170) + multilinTooltipOffset);
//            float yAdd = 0;
//            if (lockAnimatedTranslation != 10000) {
//                yAdd = Math.max(0, (int) (startTranslation - lockAnimatedTranslation));
//                if (yAdd > AndroidUtilities.dp(57)) {
//                    yAdd = AndroidUtilities.dp(57);
//                }
//            }

            drawingCx = cx + slideDelta;
            drawingCy = cy;

            float sc;
            float circleAlpha = 1f;
            if (scale <= 0.5f) {
                sc = scale / 0.5f;
            } else if (scale <= 0.75f) {
                sc = 1.0f - (scale - 0.5f) / 0.25f * 0.1f;
            } else {
                sc = 0.9f + (scale - 0.75f) / 0.25f * 0.1f;
            }
            long dt = System.currentTimeMillis() - lastUpdateTime;
            if (animateToAmplitude != amplitude) {
                amplitude += animateAmplitudeDiff * dt;
                if (animateAmplitudeDiff > 0) {
                    if (amplitude > animateToAmplitude) {
                        amplitude = animateToAmplitude;
                    }
                } else {
                    if (amplitude < animateToAmplitude) {
                        amplitude = animateToAmplitude;
                    }
                }
                invalidate();
            }

            float slideToCancelScale;
            if (canceledByGesture) {
                slideToCancelScale = 0.7f * CubicBezierInterpolator.EASE_OUT.getInterpolation(1f - slideToCancelProgress);
            } else {
                slideToCancelScale = (0.7f + slideToCancelProgress * 0.3f);
            }
            float radius = (circleRadius + circleRadiusAmplitude * amplitude) * sc * slideToCancelScale;

            progressToSeekbarStep3 = 0f;
            float progressToSeekbarStep1 = 0f;
            float progressToSeekbarStep2 = 0;
            float exitProgress2 = 0f;
            if (transformToSeekbar != 0 && audioTimelineView != null) {
                float step1Time = 0.38f;
                float step2Time = 0.25f;
                float step3Time = 1f - step1Time - step2Time;

                progressToSeekbarStep1 = transformToSeekbar > step1Time ? 1f : transformToSeekbar / step1Time;
                progressToSeekbarStep2 = transformToSeekbar > step1Time + step2Time ? 1f : Math.max(0, (transformToSeekbar - step1Time) / step2Time);
                progressToSeekbarStep3 = Math.max(0, (transformToSeekbar - step1Time - step2Time) / step3Time);

                progressToSeekbarStep1 = CubicBezierInterpolator.EASE_BOTH.getInterpolation(progressToSeekbarStep1);
                progressToSeekbarStep2 = CubicBezierInterpolator.EASE_BOTH.getInterpolation(progressToSeekbarStep2);
                progressToSeekbarStep3 = CubicBezierInterpolator.EASE_BOTH.getInterpolation(progressToSeekbarStep3);

                radius = radius + dp(16) * progressToSeekbarStep1;

                float toRadius = dp(8);
                radius = toRadius + (radius - toRadius) * (1f - progressToSeekbarStep2);
            } else if (exitTransition != 0) {
                float step1Time = 0.6f;
                float step2Time = 0.4f;

                progressToSeekbarStep1 = exitTransition > step1Time ? 1f : exitTransition / step1Time;
                exitProgress2 = messageTransitionIsRunning ? exitTransition : Math.max(0, (exitTransition - step1Time) / step2Time);

                progressToSeekbarStep1 = CubicBezierInterpolator.EASE_BOTH.getInterpolation(progressToSeekbarStep1);
                exitProgress2 = CubicBezierInterpolator.EASE_BOTH.getInterpolation(exitProgress2);

                radius = radius + dp(16) * progressToSeekbarStep1;
                radius *= (1f - exitProgress2);

                if (LiteMode.isEnabled(LiteMode.FLAGS_CHAT) && exitTransition > 0.6f) {
                    circleAlpha = Math.max(0, 1f - (exitTransition - 0.6f) / 0.4f);
                }
            }

            if (canceledByGesture && slideToCancelProgress > 0.7f) {
                circleAlpha *= (1f - (slideToCancelProgress - 0.7f) / 0.3f);
            }

            if (progressToSeekbarStep3 > 0) {
                paint.setColor(ColorUtils.blendARGB(getThemedColor(Theme.key_chat_messagePanelVoiceBackground), getThemedColor(Theme.key_chat_recordedVoiceBackground), progressToSeekbarStep3));
            } else {
                paint.setColor(getThemedColor(Theme.key_chat_messagePanelVoiceBackground));
            }

            Drawable drawable;
            Drawable replaceDrawable = null;
            checkDrawables();
            if (isSendButtonVisible()) {
                if (progressToSendButton != 1f) {
                    progressToSendButton += dt / 150f;
                    if (progressToSendButton > 1f) {
                        progressToSendButton = 1f;
                    }
                    replaceDrawable = isInVideoMode() ? cameraDrawable : micDrawable;
                }
                drawable = sendDrawable;
                sendRect.set(cx - drawable.getIntrinsicWidth() / 2, cy - drawable.getIntrinsicHeight() / 2, cx + drawable.getIntrinsicWidth() / 2, cy + drawable.getIntrinsicHeight() / 2);
                if (replaceDrawable != null) {
                    replaceDrawable.setBounds(cx - replaceDrawable.getIntrinsicWidth() / 2, cy - replaceDrawable.getIntrinsicHeight() / 2, cx + replaceDrawable.getIntrinsicWidth() / 2, cy + replaceDrawable.getIntrinsicHeight() / 2);
                }
            } else {
                drawable = isInVideoMode() ? cameraDrawable : micDrawable;
                sendRect.set(cx - dp(12), cy - dp(12), cx + dp(12), cy + dp(12));
            }
            drawable.setBounds(sendRect);

            if (incIdle) {
                idleProgress += 0.01f;
                if (idleProgress > 1f) {
                    incIdle = false;
                    idleProgress = 1f;
                }
            } else {
                idleProgress -= 0.01f;
                if (idleProgress < 0) {
                    incIdle = true;
                    idleProgress = 0;
                }
            }

            if (LiteMode.isEnabled(LiteMode.FLAGS_CHAT)) {
                tinyWaveDrawable.minRadius = dp(47);
                tinyWaveDrawable.maxRadius = dp(47) + dp(15) * BlobDrawable.FORM_SMALL_MAX;

                bigWaveDrawable.minRadius = dp(50);
                bigWaveDrawable.maxRadius = dp(50) + dp(12) * BlobDrawable.FORM_BIG_MAX;

                bigWaveDrawable.updateAmplitude(dt);
                bigWaveDrawable.update(bigWaveDrawable.amplitude, 1.01f);
                tinyWaveDrawable.updateAmplitude(dt);
                tinyWaveDrawable.update(tinyWaveDrawable.amplitude, 1.02f);

//                bigWaveDrawable.tick(radius);
//                tinyWaveDrawable.tick(radius);
            }
            lastUpdateTime = System.currentTimeMillis();
            float slideToCancelProgress1 = slideToCancelProgress > 0.7f ? 1f : slideToCancelProgress / 0.7f;

            if (LiteMode.isEnabled(LiteMode.FLAGS_CHAT) && progressToSeekbarStep2 != 1 && exitProgress2 < 0.4f && slideToCancelProgress1 > 0 && !canceledByGesture) {
                if (showWaves && wavesEnterAnimation != 1f) {
                    wavesEnterAnimation += 0.04f;
                    if (wavesEnterAnimation > 1f) {
                        wavesEnterAnimation = 1f;
                    }
                }
                if (!voiceEnterTransitionInProgress) {
                    float enter = CubicBezierInterpolator.EASE_OUT.getInterpolation(wavesEnterAnimation);
                    canvas.save();
                    float s = scale * (1f - progressToSeekbarStep1) * slideToCancelProgress1 * enter * (BlobDrawable.SCALE_BIG_MIN + 1.4f * bigWaveDrawable.amplitude);
                    canvas.scale(s, s, cx + slideDelta, cy);
                    bigWaveDrawable.draw(cx + slideDelta, cy, canvas, bigWaveDrawable.paint);
                    canvas.restore();
                    s = scale * (1f - progressToSeekbarStep1) * slideToCancelProgress1 * enter * (BlobDrawable.SCALE_SMALL_MIN + 1.4f * tinyWaveDrawable.amplitude);
                    canvas.save();
                    canvas.scale(s, s, cx + slideDelta, cy);
                    tinyWaveDrawable.draw(cx + slideDelta, cy, canvas, tinyWaveDrawable.paint);
                    canvas.restore();
                }
            }

            if (canceledByGesture && slideToCancelProgress < 1f) {
                radius = Math.max(radius, dp(19));
            }

            if (!voiceEnterTransitionInProgress) {
                paint.setAlpha((int) (paintAlpha * circleAlpha));
                if (scale == 1f) {
                    if (transformToSeekbar != 0) {
                        if (!isInVideoMode && progressToSeekbarStep3 > 0 && audioTimelineView != null) {
                            float circleB = cy + radius;
                            float circleT = cy - radius;
                            float circleR = cx + slideDelta + radius;
                            float circleL = cx + slideDelta - radius;

                            int topOffset = 0;
                            int leftOffset = 0;

                            View transformToView = audioTimelineView;
                            View v = (View) transformToView.getParent();
                            while (v != getParent()) {
                                topOffset += v.getY();
                                leftOffset += v.getX();
                                v = (View) v.getParent();
                            }

                            float seekbarT = transformToView.getY() + topOffset - getY();
                            float seekbarB = transformToView.getY() + transformToView.getMeasuredHeight() + topOffset - getY();
                            float seekbarR = transformToView.getX() + transformToView.getMeasuredWidth() + leftOffset - getX() - horizontalPadding;
                            float seekbarL = transformToView.getX() + leftOffset - getX() + horizontalPadding;
                            float toRadius = isInVideoMode() ? 0 : transformToView.getMeasuredHeight() / 2f;

                            float top = lerp(circleT, seekbarT, progressToSeekbarStep3); // seekbarT + (circleT - seekbarT) * (1f - progressToSeekbarStep3);
                            float bottom = lerp(circleB, seekbarB, progressToSeekbarStep3); // seekbarB + (circleB - seekbarB) * (1f - progressToSeekbarStep3);
                            float left = lerp(circleL, seekbarL, progressToSeekbarStep3); //seekbarL + (circleL - seekbarL) * (1f - progressToSeekbarStep3);
                            float right = lerp(circleR, seekbarR, progressToSeekbarStep3); //seekbarR + (circleR - seekbarR) * (1f - progressToSeekbarStep3);
                            float transformRadius = lerp(radius, toRadius, progressToSeekbarStep3); // toRadius + (radius - toRadius) * (1f - progressToSeekbarStep3);

                            rectF.set(left, top, right, bottom);
                            audioTimelineView.drawIn(canvas, rectF, progressToSeekbarStep3);
                        } else {
                            canvas.drawCircle(cx + slideDelta, cy, radius * (1f - progressToSeekbarStep3), paint);
                        }
                    } else {
                        canvas.drawCircle(cx + slideDelta, cy, radius, paint);
                    }
                    canvas.save();
                    float a = (1f - exitProgress2);
                    canvas.translate(slideDelta, 0);
                    drawIconInternal(canvas, drawable, replaceDrawable, progressToSendButton, (int) ((1f - progressToSeekbarStep2) * a * 255));
                    canvas.restore();
                }
            }

            if (scale != 1f) {
                canvas.drawCircle(cx + slideDelta, cy, radius, paint);
                float a = (canceledByGesture ? (1f - slideToCancelProgress) : 1);
                canvas.save();
                canvas.translate(slideDelta, 0);
                drawIconInternal(canvas, drawable, replaceDrawable, progressToSendButton, (int) (255 * a));
                canvas.restore();
            }
            drawingCircleRadius = radius;
        }

        @Override
        public void invalidate() {
            super.invalidate();
            if (controlsView != null) {
                controlsView.invalidate();
            }
        }

        public void drawIcon(Canvas canvas, int cx, int cy, float alpha) {
            Drawable drawable;
            Drawable replaceDrawable = null;
            checkDrawables();
            if (isSendButtonVisible()) {
                if (progressToSendButton != 1f) {
                    replaceDrawable = isInVideoMode() ? cameraDrawable : micDrawable;
                }
                drawable = sendDrawable;
                sendRect.set(cx - drawable.getIntrinsicWidth() / 2, cy - drawable.getIntrinsicHeight() / 2, cx + drawable.getIntrinsicWidth() / 2, cy + drawable.getIntrinsicHeight() / 2);
                if (replaceDrawable != null) {
                    replaceDrawable.setBounds(cx - replaceDrawable.getIntrinsicWidth() / 2, cy - replaceDrawable.getIntrinsicHeight() / 2, cx + replaceDrawable.getIntrinsicWidth() / 2, cy + replaceDrawable.getIntrinsicHeight() / 2);
                }
            } else {
                drawable = isInVideoMode() ? cameraDrawable : micDrawable;
                sendRect.set(cx - dp(12), cy - dp(12), cx + dp(12), cy + dp(12));
            }
            drawable.setBounds(sendRect);
            drawIconInternal(canvas, drawable, replaceDrawable, progressToSendButton, (int) (255 * alpha));
        }

        private void drawIconInternal(Canvas canvas, Drawable drawable, Drawable replaceDrawable, float progressToSendButton, int alpha) {
            checkDrawables();
            if (progressToSendButton == 0 || progressToSendButton == 1 || replaceDrawable == null) {
                if (canceledByGesture && slideToCancelProgress == 1f) {
                    View v = audioVideoSendButton;
                    v.setAlpha(1f);
                    setVisibility(View.GONE);
                    return;
                }
                if (canceledByGesture && slideToCancelProgress < 1f) {
                    drawable.setAlpha(255);
                    drawable.draw(canvas);
                } else if (!canceledByGesture) {
                    drawable.setAlpha(alpha);
                    drawable.draw(canvas);
                }
            } else {
                canvas.save();
                canvas.scale(progressToSendButton, progressToSendButton, drawable.getBounds().centerX(), drawable.getBounds().centerY());
                drawable.setAlpha((int) (alpha * progressToSendButton));
                drawable.draw(canvas);
                canvas.restore();

                canvas.save();
                canvas.scale(1f - progressToSendButton, 1f - progressToSendButton, drawable.getBounds().centerX(), drawable.getBounds().centerY());
                replaceDrawable.setAlpha((int) (alpha * (1f - progressToSendButton)));
                replaceDrawable.draw(canvas);
                canvas.restore();
            }
        }

        @Override
        protected boolean dispatchHoverEvent(MotionEvent event) {
            return super.dispatchHoverEvent(event) || virtualViewHelper.dispatchHoverEvent(event);
        }

        public void setTransformToSeekbar(float value) {
            transformToSeekbar = value;
            invalidate();
        }

        public float getTransformToSeekbarProgressStep3() {
            return progressToSeekbarStep3;
        }

        public void updateColors() {
            paint.setColor(getThemedColor(Theme.key_chat_messagePanelVoiceBackground));
            tinyWaveDrawable.paint.setColor(ColorUtils.setAlphaComponent(getThemedColor(Theme.key_chat_messagePanelVoiceBackground), (int) (255 * WaveDrawable.CIRCLE_ALPHA_2)));
            bigWaveDrawable.paint.setColor(ColorUtils.setAlphaComponent(getThemedColor(Theme.key_chat_messagePanelVoiceBackground), (int) (255 * WaveDrawable.CIRCLE_ALPHA_1)));

            paintAlpha = paint.getAlpha();
        }

        public void canceledByGesture() {
            canceledByGesture = true;
        }

        public void setMovingCords(float x, float y) {
            float delta = (x - lastMovingX) * (x - lastMovingX) + (y - lastMovingY) * (y - lastMovingY);
            lastMovingY = y;
            lastMovingX = x;
            if (showTooltip && tooltipAlpha == 0f && delta > touchSlop) {
                showTooltipStartTime = System.currentTimeMillis();
            }
        }

        public void showWaves(boolean b, boolean animated) {
            if (!animated) {
                wavesEnterAnimation = b ? 1f : 0.5f;
            }
            showWaves = b;
        }

        public void drawWaves(Canvas canvas, float cx, float cy, float additionalScale) {
            float enter = CubicBezierInterpolator.EASE_OUT.getInterpolation(wavesEnterAnimation);
            float slideToCancelProgress1 = slideToCancelProgress > 0.7f ? 1f : slideToCancelProgress / 0.7f;
            canvas.save();
            float s = scale * slideToCancelProgress1 * enter * (BlobDrawable.SCALE_BIG_MIN + 1.4f * bigWaveDrawable.amplitude) * additionalScale;
            canvas.scale(s, s, cx, cy);
            bigWaveDrawable.draw(cx, cy, canvas, bigWaveDrawable.paint);
            canvas.restore();
            s = scale * slideToCancelProgress1 * enter * (BlobDrawable.SCALE_SMALL_MIN + 1.4f * tinyWaveDrawable.amplitude) * additionalScale;
            canvas.save();
            canvas.scale(s, s, cx, cy);
            tinyWaveDrawable.draw(cx, cy, canvas, tinyWaveDrawable.paint);
            canvas.restore();
        }

        private class VirtualViewHelper extends ExploreByTouchHelper {

            public VirtualViewHelper(@NonNull View host) {
                super(host);
            }

            private int[] coords = new int[2];

            @Override
            protected int getVirtualViewAt(float x, float y) {
                if (isSendButtonVisible() && recordCircle != null) {
                    if (sendRect.contains((int) x, (int) y)) {
                        return 1;
                    } else if (pauseRect.contains(x, y)) {
                        return 2;
                    } else if (slideText != null && slideText.cancelRect != null) {
                        AndroidUtilities.rectTmp.set(slideText.cancelRect);
                        slideText.getLocationOnScreen(coords);
                        AndroidUtilities.rectTmp.offset(coords[0], coords[1]);
                        recordCircle.getLocationOnScreen(coords);
                        AndroidUtilities.rectTmp.offset(-coords[0], -coords[1]);
                        if (AndroidUtilities.rectTmp.contains(x, y)) {
                            return 3;
                        }
                    }
                }
                return HOST_ID;
            }

            @Override
            protected void getVisibleVirtualViews(List<Integer> list) {
                if (isSendButtonVisible()) {
                    list.add(1);
//                    list.add(2);
                    list.add(3);
                }
            }

            @Override
            protected void onPopulateNodeForVirtualView(int id, @NonNull AccessibilityNodeInfoCompat info) {
                if (id == 1) {
                    info.setBoundsInParent(sendRect);
                    info.setText(getString("Send", R.string.Send));
                } else if (id == 2) {
                    rect.set((int) pauseRect.left, (int) pauseRect.top, (int) pauseRect.right, (int) pauseRect.bottom);
                    info.setBoundsInParent(rect);
                    info.setText(getString(R.string.Stop));
                } else if (id == 3 && recordCircle != null) {
                    if (slideText != null && slideText.cancelRect != null) {
                        AndroidUtilities.rectTmp2.set(slideText.cancelRect);
                        slideText.getLocationOnScreen(coords);
                        AndroidUtilities.rectTmp2.offset(coords[0], coords[1]);
                        recordCircle.getLocationOnScreen(coords);
                        AndroidUtilities.rectTmp2.offset(-coords[0], -coords[1]);
                        info.setBoundsInParent(AndroidUtilities.rectTmp2);
                    }
                    info.setText(getString("Cancel", R.string.Cancel));
                }
            }

            @Override
            protected boolean onPerformActionForVirtualView(int id, int action, @Nullable Bundle args) {
                return true;
            }
        }
    }

    public ChatActivityEnterView(Activity context, SizeNotifierFrameLayout parent, ChatActivity fragment, final boolean isChat) {
        this(context, parent, fragment, isChat, null);
    }

    @SuppressLint("ClickableViewAccessibility")
    public ChatActivityEnterView(Activity context, SizeNotifierFrameLayout parent, ChatActivity fragment, final boolean isChat, Theme.ResourcesProvider resourcesProvider) {
        super(context);
        this.resourcesProvider = resourcesProvider;
        this.isChat = isChat;
        updateIosLayoutSeeds();
        int attachButtonRightDp = isIosButtonPlacement() ? 0 : (isIosInputAppearance() ? iosGapDp : 0);

        smoothKeyboard = isChat && !AndroidUtilities.isInMultiwindow && (fragment == null || !fragment.isInBubbleMode());
        dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dotPaint.setColor(getThemedColor(Theme.key_chat_emojiPanelNewTrending));
        setFocusable(true);
        setFocusableInTouchMode(true);
        setWillNotDraw(false);
        setClipChildren(false);

        NotificationCenter.getInstance(currentAccount).addWeakObserver(this, NotificationCenter.recordStarted);
        NotificationCenter.getInstance(currentAccount).addWeakObserver(this, NotificationCenter.recordPaused);
        NotificationCenter.getInstance(currentAccount).addWeakObserver(this, NotificationCenter.recordResumed);
        NotificationCenter.getInstance(currentAccount).addWeakObserver(this, NotificationCenter.recordStartError);
        NotificationCenter.getInstance(currentAccount).addWeakObserver(this, NotificationCenter.recordStopped);
        NotificationCenter.getInstance(currentAccount).addWeakObserver(this, NotificationCenter.recordProgressChanged);
        NotificationCenter.getInstance(currentAccount).addWeakObserver(this, NotificationCenter.closeChats);
        NotificationCenter.getInstance(currentAccount).addWeakObserver(this, NotificationCenter.audioDidSent);
        NotificationCenter.getInstance(currentAccount).addWeakObserver(this, NotificationCenter.audioRouteChanged);
        NotificationCenter.getInstance(currentAccount).addWeakObserver(this, NotificationCenter.messagePlayingProgressDidChanged);
        NotificationCenter.getInstance(currentAccount).addWeakObserver(this, NotificationCenter.featuredStickersDidLoad);
        NotificationCenter.getInstance(currentAccount).addWeakObserver(this, NotificationCenter.messageReceivedByServer2);
        NotificationCenter.getInstance(currentAccount).addWeakObserver(this, NotificationCenter.sendingMessagesChanged);
        NotificationCenter.getInstance(currentAccount).addWeakObserver(this, NotificationCenter.audioRecordTooShort);
        NotificationCenter.getInstance(currentAccount).addWeakObserver(this, NotificationCenter.updateBotMenuButton);
        NotificationCenter.getInstance(currentAccount).addWeakObserver(this, NotificationCenter.didUpdatePremiumGiftFieldIcon);
        NotificationCenter.getInstance(currentAccount).addWeakObserver(this, NotificationCenter.currentUserPremiumStatusChanged);
        NotificationCenter.getGlobalInstance().addWeakObserver(this, NotificationCenter.emojiLoaded);
        NotificationCenter.getInstance(currentAccount).addWeakObserver(this, NotificationCenter.beforeAudioDidSent);
        NotificationCenter.getGlobalInstance().addWeakObserver(this, NotificationCenter.didSetNewTheme);
        NotificationCenter.getGlobalInstance().addWeakObserver(this, NotificationCenter.reloadInterface);

        parentActivity = context;
        parentFragment = fragment;
        if (fragment != null) {
            recordingGuid = parentFragment.getClassGuid();
        }
        sizeNotifierLayout = parent;
        viewParentForEmojiView = parent;
        sizeNotifierLayout.setDelegate(this);
        SharedPreferences preferences = MessagesController.getGlobalMainSettings();
        sendByEnter = preferences.getBoolean("send_by_enter", false);

        textFieldContainer = new FrameLayout(context) {
            @Override
            public boolean dispatchTouchEvent(MotionEvent ev) {
                if (botWebViewButton != null && botWebViewButton.getVisibility() == VISIBLE) {
                    return botWebViewButton.dispatchTouchEvent(ev);
                }
                return super.dispatchTouchEvent(ev);
            }

            @Override
            protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
                if (isIosInputAppearance()) {
                    if (child == doneButton) {
                        drawIosBubble(canvas, doneBubbleDrawable, child);
                    } else if (!isIosButtonPlacement() && child == aiButton) {
                        drawIosBubble(canvas, aiBubbleDrawable, child);
                    } else if (child == richButton) {
                        drawIosBubble(canvas, richBubbleDrawable, child);
                    }
                }
                return super.drawChild(canvas, child, drawingTime);
            }
        };
        textFieldContainer.setClipChildren(false);
        textFieldContainer.setClipToPadding(false);
        textFieldContainer.setPadding(0, dp(1), 0, 0);
        addView(textFieldContainer, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.BOTTOM, 0, 1, 0, 0));

        FrameLayout frameLayout = messageEditTextContainer = new FrameLayout(context) {
            @Override
            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                super.onMeasure(widthMeasureSpec, heightMeasureSpec);
                final int height = Math.max(dp(44), getMeasuredHeight());
                if (animatorInputFieldHeight.getFactor() > 0) {
                    animatorInputFieldHeight.animateTo(height);
                } else {
                    animatorInputFieldHeight.forceFactor(height);
                }
                checkUi_TopViewVisibility();
            }

            @Override
            protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
                super.onLayout(changed, left, top, right, bottom);
                if (!animationParamsX.isEmpty()) {
                    for (int i = 0; i < getChildCount(); i++) {
                        View child = getChildAt(i);
                        Float fromX = animationParamsX.get(child);
                        if (fromX != null) {
                            child.setTranslationX(fromX - child.getLeft());
                            child.animate().translationX(0).setDuration(150).setInterpolator(CubicBezierInterpolator.DEFAULT).start();
                        }
                    }
                    animationParamsX.clear();
                }
                if (messageEditText != null && doneButton != null && doneButton.getVisibility() == VISIBLE
                        && !isIosButtonPlacement() && isIosInputAppearance()) {
                    FrameLayout.LayoutParams editTextParams = (FrameLayout.LayoutParams) messageEditText.getLayoutParams();
                    int targetRightMargin = editTextParams.leftMargin - dp(DEFAULT_HEIGHT);
                    if (editTextParams.rightMargin != targetRightMargin) {
                        editTextParams.rightMargin = targetRightMargin;
                        messageEditText.setLayoutParams(editTextParams);
                    }
                }
            }

            @Override
            protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
                if (isIosInputAppearance()) {
                    BlurredBackgroundDrawable outsideLeftBubble = isIosButtonPlacement() ? attachBubbleDrawable : emojiBubbleDrawable;
                    View outsideLeftButton = isIosButtonPlacement() ? attachButton : emojiButton;
                    if (child == outsideLeftButton) {
                        drawIosBubble(canvas, outsideLeftBubble, child);
                    }
                }
                if (child != null && child == messageEditText) {
                    return drawMessageEditText(canvas, () -> super.drawChild(canvas, child, drawingTime));
                }
                if (shouldDrawRecordedAudioPanelInParent && child == recordedAudioPanel) {
                    return true;
                }
                return super.drawChild(canvas, child, drawingTime);
            }
        };
        frameLayout.setClipChildren(false);
        textFieldContainer.addView(frameLayout, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.BOTTOM, 0, 0, DEFAULT_HEIGHT, 0));

        emojiButton = new ChatActivityEnterViewAnimatedIconView(context) {
            @Override
            protected void onDraw(Canvas canvas) {
                super.onDraw(canvas);
                if (getTag() != null && attachLayout != null && !emojiViewVisible && !isFormatMode() && !MediaDataController.getInstance(currentAccount).getUnreadStickerSets().isEmpty() && dotPaint != null) {
                    int x = getWidth() / 2 + dp(4 + 5);
                    int y = getHeight() / 2 - dp(13 - 5);
                    canvas.drawCircle(x, y, dp(5), dotPaint);

                }
            }
        };
        emojiButton.setContentDescription(getString(R.string.AccDescrEmojiButton));
        emojiButton.setFocusable(true);
        int padding = dp(7.5f);
        emojiButton.setPadding(padding, padding, padding, padding);
        ThemeBind.colorFilter(emojiButton, Theme.key_glass_defaultIcon, resourcesProvider, PorterDuff.Mode.SRC_IN);
        emojiButton.setBackground(Theme.createInsetRoundRectDrawable(getThemedColor(Theme.key_listSelector), dp(19), dp(1), dp(3)));
        emojiButton.setOnClickListener(v -> {
            if (adjustPanLayoutHelper != null && adjustPanLayoutHelper.animationInProgress()) {
                return;
            }
            if (emojiButton.isFormatMode()) {
                toggleFormatOptions();
                return;
            }
            if (emojiButtonRestricted) {
                showRestrictedHint();
                return;
            }
            if (!isPopupShowing() || currentPopupContentType != 0) {
                showPopup(1, POPUP_CONTENT_EMOJI_KEYBOARD);
                emojiView.onOpen(messageEditText != null && messageEditText.length() > 0, parentFragment != null && parentFragment.groupEmojiPackHintWasVisible());
            } else {
                if (searchingType != 0) {
                    setSearchingTypeInternal(0, true);
                    if (emojiView != null) {
                        emojiView.closeSearch(false);
                    }
                    if (messageEditText != null) {
                        messageEditText.requestFocus();
                    }
                }
                if (stickersExpanded) {
                    setStickersExpanded(false, true, false);
                    waitingForKeyboardOpenAfterAnimation = true;
                    AndroidUtilities.runOnUIThread(() -> {
                        waitingForKeyboardOpenAfterAnimation = false;
                        openKeyboardInternal();
                    }, 200);
                } else {
                    openKeyboardInternal();
                }
            }
        });
        messageEditTextContainer.addView(emojiButton, LayoutHelper.createFrame(DEFAULT_HEIGHT, DEFAULT_HEIGHT, emojiGravity, emojiLeftDp, 0, emojiRightDp, 0));
        setEmojiButtonImage(false, false);

        deleteRichDraftButton = new ImageView(context);
        deleteRichDraftButton.setScaleType(ImageView.ScaleType.CENTER);
        deleteRichDraftButton.setImageResource(R.drawable.menu_delete_old);
        ThemeBind.colorFilter(deleteRichDraftButton, Theme.key_glass_defaultIcon, resourcesProvider, PorterDuff.Mode.SRC_IN);
        deleteRichDraftButton.setBackground(Theme.createInsetRoundRectDrawable(getThemedColor(Theme.key_listSelector), dp(19), dp(1), dp(3)));
        deleteRichDraftButton.setVisibility(View.GONE);
        deleteRichDraftButton.setContentDescription(getString(R.string.ArticleDeleteDraft));
        deleteRichDraftButton.setOnClickListener(v -> {
            new AlertDialog.Builder(getContext(), resourcesProvider)
                .setTitle(getString(R.string.ArticleDeleteDraftTitle))
                .setMessage(getString(R.string.ArticleDeleteDraftMessage))
                .setNegativeButton(getString(R.string.Cancel), null)
                .setPositiveButton(getString(R.string.Delete), (di, w) -> {
                    clearRichDraft();
                    if (messageEditText != null) {
                        messageEditText.setTextWithoutInputAnimation("");
                    }
                })
                .makeRed(AlertDialog.BUTTON_POSITIVE)
                .show();
        });
        messageEditTextContainer.addView(deleteRichDraftButton, LayoutHelper.createFrame(DEFAULT_HEIGHT, DEFAULT_HEIGHT, emojiGravity, emojiLeftDp, 0, emojiRightDp, 0));

        if (isChat) {
            attachLayout = new LinearLayout(context) {
                @Override
                public void setAlpha(float alpha) {
                    super.setAlpha(alpha);
                    if (isIosButtonPlacement() && messageEditTextContainer != null) {
                        messageEditTextContainer.requestLayout();
                    }
                }
                @Override
                protected void onLayout(boolean changed, int l, int t, int r, int b) {
                    super.onLayout(changed, l, t, r, b);
                    setPivotX(getWidth());
                }
            };
            attachLayout.setOrientation(LinearLayout.HORIZONTAL);
            attachLayout.setEnabled(false);
            attachLayout.setClipChildren(false);
            messageEditTextContainer.addView(attachLayout, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, DEFAULT_HEIGHT, attachGravity, 0, 0, attachLayoutRightDp, 0));

            if (chatMode != ChatActivity.MODE_WELCOME_MESSAGES) {
                notifyButton = new ImageView(context);
                notifySilentDrawable = new CrossOutDrawable(context, R.drawable.input_notify_on, Theme.key_glass_defaultIcon);
                notifyButton.setImageDrawable(notifySilentDrawable);
                notifySilentDrawable.setCrossOut(silent, false);
                notifyButton.setContentDescription(silent ? getString("AccDescrChanSilentOn", R.string.AccDescrChanSilentOn) : getString("AccDescrChanSilentOff", R.string.AccDescrChanSilentOff));
                ThemeBind.colorFilter(notifyButton, Theme.key_glass_defaultIcon, resourcesProvider, PorterDuff.Mode.MULTIPLY);
                notifyButton.setScaleType(ImageView.ScaleType.CENTER);
                ThemeBind.selectorBackground(notifyButton, Theme.createSelectorDrawable(getThemedColor(Theme.key_listSelector)), null, ThemeBind.of(Theme.key_listSelector), resourcesProvider);
                notifyButton.setVisibility(canWriteToChannel && (delegate == null || !delegate.hasScheduledMessages()) ? VISIBLE : GONE);
                attachLayout.addView(notifyButton, LayoutHelper.createLinear(DEFAULT_HEIGHT, DEFAULT_HEIGHT));
                notifyButton.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (fragment == null) return;
                        silent = !silent;
                        if (notifySilentDrawable == null) {
                            notifySilentDrawable = new CrossOutDrawable(context, R.drawable.input_notify_on, Theme.key_glass_defaultIcon);
                        }
                        notifySilentDrawable.setCrossOut(silent, true);
                        notifyButton.setImageDrawable(notifySilentDrawable);
                        MessagesController.getNotificationsSettings(currentAccount).edit().putBoolean("silent_" + dialog_id, silent).commit();
                        NotificationsController.getInstance(currentAccount).updateServerNotificationsSettings(dialog_id, fragment == null ? 0 : fragment.getTopicId());
                        UndoView undoView = fragment.getUndoView();
                        if (undoView != null) {
                            undoView.showWithAction(0, !silent ? UndoView.ACTION_NOTIFY_ON : UndoView.ACTION_NOTIFY_OFF, null);
                        }
                        notifyButton.setContentDescription(silent ? getString("AccDescrChanSilentOn", R.string.AccDescrChanSilentOn) : getString("AccDescrChanSilentOff", R.string.AccDescrChanSilentOff));
                        updateFieldHint(true);
                    }
                    notifySilentDrawable.setCrossOut(silent, true);
                    notifyButton.setImageDrawable(notifySilentDrawable);
                    MessagesController.getNotificationsSettings(currentAccount).edit().putBoolean("silent_" + dialog_id, silent).commit();
                    NotificationsController.getInstance(currentAccount).updateServerNotificationsSettings(dialog_id, fragment == null ? 0 : fragment.getTopicId());
                    UndoView undoView = fragment.getUndoView();
                    if (undoView != null) {
                        undoView.showWithAction(0, !silent ? UndoView.ACTION_NOTIFY_ON : UndoView.ACTION_NOTIFY_OFF, null);
                    }
                    notifyButton.setContentDescription(silent ? getString("AccDescrChanSilentOn", R.string.AccDescrChanSilentOn) : getString("AccDescrChanSilentOff", R.string.AccDescrChanSilentOff));
                    updateFieldHint(true);
                }
            });

            attachButton = new ImageView(context) {
                @Override
                public boolean dispatchTouchEvent(MotionEvent event) {
                    if (getAlpha() < 0.5f) return false;
                    return super.dispatchTouchEvent(event);
                }
            };
            attachButton.setScaleType(ImageView.ScaleType.CENTER);
            ThemeBind.colorFilter(attachButton, Theme.key_glass_defaultIcon, resourcesProvider, PorterDuff.Mode.MULTIPLY);
            attachButton.setImageResource(R.drawable.msg_input_attach2);
            ThemeBind.selectorBackground(attachButton, Theme.createSelectorDrawable(getThemedColor(Theme.key_listSelector)), null, ThemeBind.of(Theme.key_listSelector), resourcesProvider);
            messageEditTextContainer.addView(attachButton, LayoutHelper.createFrame(DEFAULT_HEIGHT, DEFAULT_HEIGHT, Gravity.BOTTOM | Gravity.RIGHT));
            attachButton.setOnClickListener(v -> {
                if (adjustPanLayoutHelper != null && adjustPanLayoutHelper.animationInProgress() || attachLayoutPaddingAlpha == 0f) {
                    return;
                }
                delegate.didPressAttachButton();
            });
            attachButton.setContentDescription(getString(R.string.AccDescrAttachButton));
            updateFieldRight(1);
        }

        aiButton = new ImageView(context);
        aiButton.setImageDrawable(aiButtonIcon = new AiButtonDrawable(context));
        aiButton.setScaleType(ImageView.ScaleType.CENTER);
        ThemeBind.colorFilter(aiButton, Theme.key_glass_defaultIcon, resourcesProvider, PorterDuff.Mode.MULTIPLY);
        ThemeBind.selectorBackground(aiButton, Theme.createSelectorDrawable(getThemedColor(Theme.key_listSelector), Theme.RIPPLE_MASK_CIRCLE_20DP, dp(16)), null, ThemeBind.of(Theme.key_listSelector), resourcesProvider);
        textFieldContainer.addView(aiButton, LayoutHelper.createFrame(DEFAULT_HEIGHT, DEFAULT_HEIGHT, Gravity.TOP | Gravity.LEFT, 0, 1, 0, 0));
        aiButton.setContentDescription(getString(R.string.AIEditor));
        ScaleStateListAnimator.apply(aiButton);
        aiButton.setOnClickListener(v -> {
            MessagesController.getGlobalMainSettings().edit().putInt("aihintshown", 3).apply();
            final long dialogId = parentFragment != null ? parentFragment.getDialogId() : dialog_id;
            if (richDraftActive) {
                if (richDraftMessage == null) return;
                new AIEditorAlert(getContext(), resourcesProvider)
                    .setText(richDraftMessage)
                    .setOnUseRich(this::saveRichDraft)
                    .setOnSendRich(dialogId, (rich, scheduleDate, scheduleRepeatPeriod, notify) -> {
                        saveRichDraft(rich);
                        if (isInScheduleMode() && scheduleDate == 0) {
                            AlertsCreator.createScheduleDatePickerDialog(parentActivity, dialogId, new AlertsCreator.ScheduleDatePickerDelegate() {
                                @Override
                                public void didSelectDate(boolean notify2, int scheduleDate2, int scheduleRepeatPeriod2) {
                                    sendMessageInternal(notify2, scheduleDate2, scheduleRepeatPeriod2, 0, true);
                                }
                            }, resourcesProvider);
                        } else {
                            sendMessageInternal(notify, scheduleDate, scheduleRepeatPeriod, 0, true);
                        }
                    })
                    .show();
                return;
            }
            if (messageEditText == null) return;
            new AIEditorAlert(getContext(), resourcesProvider)
                .setText(messageEditText.getText())
                .setOnUse(text -> {
                    messageEditText.setText(text);
                    messageEditText.setSelection(text.length(), text.length());
                })
                .setOnSend(dialogId, editingMessageObject != null, (text, scheduleDate, scheduleRepeatPeriod, notify) -> {
                    messageEditText.setText(text);
                    if (editingMessageObject != null) {
                        doneEditingMessage();
                    } else {
                        if (isInScheduleMode() && scheduleDate == 0) {
                            AlertsCreator.createScheduleDatePickerDialog(parentActivity, dialogId, new AlertsCreator.ScheduleDatePickerDelegate() {
                                @Override
                                public void didSelectDate(boolean notify, int scheduleDate, int scheduleRepeatPeriod) {
                                    final boolean shownDialog = sendMessageInternal(notify, scheduleDate, scheduleRepeatPeriod, 0, true);
                                    if (messageSendPreview != null) {
                                        messageSendPreview.dismiss(!shownDialog);
                                        messageSendPreview = null;
                                    }
                                }
                            }, resourcesProvider);
                        } else {
                            sendMessageInternal(notify, scheduleDate, scheduleRepeatPeriod, 0, true);
                        }
                    }
                })
                .show();
        });
        aiButton.setVisibility(View.GONE);
        aiButton.setAlpha(0.0f);
        aiButton.setScaleX(0.6f);
        aiButton.setScaleY(0.6f);

        richButton = new ImageView(context);
        richButton.setImageResource(R.drawable.iv_fullscreen);
        richButton.setScaleType(ImageView.ScaleType.CENTER);
        ThemeBind.colorFilter(richButton, Theme.key_glass_defaultIcon, resourcesProvider, PorterDuff.Mode.MULTIPLY);
        ThemeBind.selectorBackground(richButton, Theme.createSelectorDrawable(getThemedColor(Theme.key_listSelector), Theme.RIPPLE_MASK_CIRCLE_20DP, dp(16)), null, ThemeBind.of(Theme.key_listSelector), resourcesProvider);
        textFieldContainer.addView(richButton, LayoutHelper.createFrame(DEFAULT_HEIGHT, DEFAULT_HEIGHT, Gravity.TOP | Gravity.RIGHT, 0, 1, 0, 0));
        richButton.setContentDescription(getString(R.string.ArticleEditor));
        ScaleStateListAnimator.apply(richButton);
        richButton.setOnClickListener(v -> openRichEditor());
        richButton.setVisibility(View.GONE);
        richButton.setAlpha(0.0f);
        richButton.setScaleX(0.6f);
        richButton.setScaleY(0.6f);

        if (audioToSend != null) {
            createRecordAudioPanel();
        }

        sendOutlineView = new ImageView(context);
        sendOutlineView.setImageResource(R.drawable.send_outline);
        sendOutlineView.setScaleType(ImageView.ScaleType.CENTER);
        sendOutlineView.setVisibility(View.GONE);
        ThemeBind.colorFilter(sendOutlineView, Theme.key_telegram_color, resourcesProvider, PorterDuff.Mode.SRC_IN);
        textFieldContainer.addView(sendOutlineView, LayoutHelper.createFrame(DEFAULT_HEIGHT, DEFAULT_HEIGHT, Gravity.BOTTOM | Gravity.RIGHT));

        sendButtonContainer = new FrameLayout(context) {
            @Override
            protected void onSizeChanged(int w, int h, int oldw, int oldh) {
                super.onSizeChanged(w, h, oldw, oldh);
                setPivotX(w - dp(IOS_BUBBLE_RADIUS_DP));
                setPivotY(h - dp(IOS_BUBBLE_RADIUS_DP));
            }

            @Override
            protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
                if (child == sendButton && textTransitionIsRunning && !isIosInputAppearance()) {
                    return true;
                }
                if (isIosInputAppearance()) {
                    if (child == sendButton) {
                        float sendButtonAlpha = child.getAlpha();
                        if (textTransitionIsRunning) {
                            isSendBubbleDismissed = true;
                        } else if (sendButtonAlpha > sendButtonLastDrawAlpha || sendButtonAlpha >= 1f) {
                            isSendBubbleDismissed = false;
                        }
                        if (sendBubbleDrawable != null && child.getVisibility() == VISIBLE && !isSendBubbleDismissed) {
                            sendBubbleDrawable.setBounds(child.getRight() - dp(DEFAULT_HEIGHT), child.getBottom() - dp(DEFAULT_HEIGHT), child.getRight(), child.getBottom());
                            sendBubbleDrawable.setAlpha((int) (255 * sendButtonAlpha));
                            DrawableUtils.drawWithScale(canvas, sendBubbleDrawable, child.getScaleX());
                        }
                        sendButtonLastDrawAlpha = sendButtonAlpha;
                    } else if (child == expandStickersButton) {
                        drawIosBubbleSquare(canvas, expandStickersBubbleDrawable, child);
                    } else if (child == cancelBotButton) {
                        drawIosBubbleSquare(canvas, cancelBotBubbleDrawable, child);
                    }
                }
                return super.drawChild(canvas, child, drawingTime);
            }

            @Override
            public boolean dispatchTouchEvent(MotionEvent ev) {
                if (!isSendButtonEnabled()) {
                    return false;
                }
                return super.dispatchTouchEvent(ev);
            }

            @Override
            public boolean onTouchEvent(MotionEvent event) {
                if (!isSendButtonEnabled()) {
                    return false;
                }
                return super.onTouchEvent(event);
            }
        };
        sendButtonContainer.setClipChildren(false);
        sendButtonContainer.setClipToPadding(false);
        textFieldContainer.addView(sendButtonContainer, LayoutHelper.createFrame(100, DEFAULT_HEIGHT, Gravity.BOTTOM | Gravity.RIGHT));

        audioVideoButtonContainer = new FrameLayout(context) {

            @Override
            public void setVisibility(int visibility) {
                super.setVisibility(visibility);
            }

            @Override
            public boolean onInterceptTouchEvent(MotionEvent ev) {
                if (isLiveComment) return false;
                return true;
            }

            @Override
            public boolean onTouchEvent(MotionEvent motionEvent) {
                if (isLiveComment) return false;
                if (NyaConfig.useChatAttachMediaMenu.Bool() && !isStories)
                    return super.onTouchEvent(motionEvent);
                createRecordCircle();
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    if (recordCircle.isSendButtonVisible()) {
                        if (!hasRecordVideo || calledRecordRunnable) {
                            startedDraggingX = -1;
                            if (hasRecordVideo && isInVideoMode()) {
                                if (AlertsCreator.needsPaidMessageAlert(currentAccount, dialog_id)) {
                                    if (slideText != null) {
                                        slideText.setEnabled(false);
                                    }
                                    delegate.toggleVideoRecordingPause();
                                    AlertsCreator.ensurePaidMessageConfirmation(currentAccount, dialog_id, 1, payStars -> {
                                        sendMessageInternal(!NyaConfig.INSTANCE.getSilentMessageByDefault().Bool(), 0, 0, payStars, false);
                                    });
                                    return true;
                                }
                                delegate.needStartRecordVideo(NyaConfig.confirmAVMessage.Bool() ? 3 : 1, true, 0, 0, voiceOnce ? 0x7FFFFFFF : 0, effectId, 0);
                                sendButton.setEffect(effectId = 0);
                            } else if (NyaConfig.confirmAVMessage.Bool()) {
                                MediaController.getInstance().stopRecording(2, true, 0, voiceOnce, 0);
                            } else {
                                if (recordingAudioVideo && isInScheduleMode()) {
                                    AlertsCreator.createScheduleDatePickerDialog(parentActivity, parentFragment.getDialogId(), (notify, scheduleDate, scheduleRepeatPeriod) -> MediaController.getInstance().stopRecording(1, notify, scheduleDate, false, 0), () -> MediaController.getInstance().stopRecording(0, false, 0, false, 0), resourcesProvider);
                                }
                                if (AlertsCreator.needsPaidMessageAlert(currentAccount, dialog_id)) {
                                    if (isInVideoMode()) {
                                        if (slideText != null) {
                                            slideText.setEnabled(false);
                                        }
                                        delegate.toggleVideoRecordingPause();
                                    } else {
                                        if (sendButtonVisible) {
                                            calledRecordRunnable = true;
                                        }
                                        MediaController.getInstance().toggleRecordingPause(voiceOnce);
                                        delegate.needStartRecordAudio(0);
                                        if (slideText != null) {
                                            slideText.setEnabled(false);
                                        }
                                    }
                                    AlertsCreator.ensurePaidMessageConfirmation(currentAccount, dialog_id, 1, payStars -> {
                                        sendMessageInternal(!NyaConfig.INSTANCE.getSilentMessageByDefault().Bool(), 0, 0, payStars, false);
                                    });
                                    return true;
                                }
                                MediaController.getInstance().stopRecording(isInScheduleMode() ? 3 : 1, true, 0, voiceOnce, 0);
                                delegate.needStartRecordAudio(0);
                            }
                            if (!NyaConfig.confirmAVMessage.Bool()) {
                                recordingAudioVideo = false;
                                messageTransitionIsRunning = false;
                                AndroidUtilities.runOnUIThread(moveToSendStateRunnable = () -> {
                                    moveToSendStateRunnable = null;
                                    updateRecordInterface(RECORD_STATE_SENDING, true);
                                }, 200);
                            }
                        }
                        getParent().requestDisallowInterceptTouchEvent(true);
                        return true;
                    }

                    TLRPC.Chat chat = parentFragment == null ? null : parentFragment.getCurrentChat();
                    TLRPC.UserFull userFull = parentFragment == null ? userInfo : parentFragment.getCurrentUserInfo();
                    if (chat != null && !(ChatObject.canSendVoice(chat) || (ChatObject.canSendRoundVideo(chat) && hasRecordVideo)) || userFull != null && userFull.voice_messages_forbidden) {
                        delegate.needShowMediaBanHint();
                        return true;
                    }

                    if (hasRecordVideo) {
                        calledRecordRunnable = false;
                        recordAudioVideoRunnableStarted = true;
                        AndroidUtilities.runOnUIThread(recordAudioVideoRunnable, 150);
                    } else {
                        recordAudioVideoRunnable.run();
                    }
                    return true;
                } else if (motionEvent.getAction() == MotionEvent.ACTION_UP || motionEvent.getAction() == MotionEvent.ACTION_CANCEL) {
                    if (motionEvent.getAction() == MotionEvent.ACTION_CANCEL && recordingAudioVideo) {
                        if (slideToCancelProgress < 0.7f) {
                            if (hasRecordVideo && isInVideoMode()) {
                                CameraController.getInstance().cancelOnInitRunnable(onFinishInitCameraRunnable);
                                pendingCameraFront = null;
                                delegate.needStartRecordVideo(2, true, 0, 0, voiceOnce ? 0x7FFFFFFF : 0, effectId, 0);
                                sendButton.setEffect(effectId = 0);
                            } else {
                                delegate.needStartRecordAudio(0);
                                MediaController.getInstance().stopRecording(0, false, 0, voiceOnce, 0);
                            }
                            millisecondsRecorded = 0;
                            recordingAudioVideo = false;
                            updateRecordInterface(RECORD_STATE_CANCEL_BY_GESTURE, true);
                        } else {
                            sendButtonVisible = true;
                            startLockTransition();
                        }
                        return false;
                    }
                    if (recordCircle != null && recordCircle.isSendButtonVisible() || recordedAudioPanel != null && recordedAudioPanel.getVisibility() == VISIBLE) {
                        if (recordAudioVideoRunnableStarted) {
                            AndroidUtilities.cancelRunOnUIThread(recordAudioVideoRunnable);
                        }
                        return false;
                    }

                    float x = motionEvent.getX() + audioVideoButtonContainer.getX();
                    float dist = (x - startedDraggingX);
                    float alpha = 1.0f + dist / distCanMove;
                    if (alpha < 0.45) {
                        if (hasRecordVideo && isInVideoMode()) {
                            CameraController.getInstance().cancelOnInitRunnable(onFinishInitCameraRunnable);
                            pendingCameraFront = null;
                            delegate.needStartRecordVideo(2, true, 0, 0, voiceOnce ? 0x7FFFFFFF : 0, effectId, 0);
                            sendButton.setEffect(effectId = 0);
                        } else {
                            delegate.needStartRecordAudio(0);
                            MediaController.getInstance().stopRecording(0, false, 0, voiceOnce, 0);
                        }
                        millisecondsRecorded = 0;
                        recordingAudioVideo = false;
                        updateRecordInterface(RECORD_STATE_CANCEL_BY_GESTURE, true);
                    } else {
                        if (recordAudioVideoRunnableStarted) {
                            AndroidUtilities.cancelRunOnUIThread(recordAudioVideoRunnable);
                            if (sendVoiceEnabled && sendRoundEnabled) {
                                delegate.onSwitchRecordMode(!isInVideoMode());
                                setRecordVideoButtonVisible(!isInVideoMode(), true);
                            } else {
                                delegate.needShowMediaBanHint();
                            }
                            if (!NyaConfig.disableVibration.Bool()) performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                            sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_CLICKED);
                        } else if (!hasRecordVideo || calledRecordRunnable) {
                            startedDraggingX = -1;
                            if (hasRecordVideo && isInVideoMode()) {
                                if (AlertsCreator.needsPaidMessageAlert(currentAccount, dialog_id)) {
                                    if (slideText != null) {
                                        slideText.setEnabled(false);
                                    }
                                    delegate.toggleVideoRecordingPause();
                                    AlertsCreator.ensurePaidMessageConfirmation(currentAccount, dialog_id, 1, payStars -> {
                                        sendMessageInternal(!NyaConfig.INSTANCE.getSilentMessageByDefault().Bool(), 0, 0, payStars, false);
                                    });
                                    return true;
                                }
                                CameraController.getInstance().cancelOnInitRunnable(onFinishInitCameraRunnable);
                                pendingCameraFront = null;
                                delegate.needStartRecordVideo(NyaConfig.confirmAVMessage.Bool() ? 3 : 1, true, 0, 0, voiceOnce ? 0x7FFFFFFF : 0, effectId, 0);
                                sendButton.setEffect(effectId = 0);
                            } else if (!sendVoiceEnabled) {
                                delegate.needShowMediaBanHint();
                            } else {
                                if (AlertsCreator.needsPaidMessageAlert(currentAccount, dialog_id)) {
                                    if (sendButtonVisible) {
                                        calledRecordRunnable = true;
                                    }
                                    MediaController.getInstance().toggleRecordingPause(voiceOnce);
                                    delegate.needStartRecordAudio(0);
                                    if (slideText != null) {
                                        slideText.setEnabled(false);
                                    }
                                    AlertsCreator.ensurePaidMessageConfirmation(currentAccount, dialog_id, 1, payStars -> {
                                        sendMessageInternal(!NyaConfig.INSTANCE.getSilentMessageByDefault().Bool(), 0, 0, payStars, false);
                                    });
                                    return true;
                                }
                                if (!NyaConfig.confirmAVMessage.Bool() && recordingAudioVideo && isInScheduleMode()) {
                                    AlertsCreator.createScheduleDatePickerDialog(parentActivity, parentFragment.getDialogId(), (notify, scheduleDate, scheduleRepeatPeriod) -> MediaController.getInstance().stopRecording(1, notify, scheduleDate, voiceOnce, 0), () -> MediaController.getInstance().stopRecording(0, false, 0, voiceOnce, 0), resourcesProvider);
                                }
                                delegate.needStartRecordAudio(0);
                                MediaController.getInstance().stopRecording(NyaConfig.confirmAVMessage.Bool() ? 2 : isInScheduleMode() ? 3 : 1, true, 0, voiceOnce, 0);
                            }
                            if (!NyaConfig.confirmAVMessage.Bool()) {
                                recordingAudioVideo = false;
                                messageTransitionIsRunning = false;
                                AndroidUtilities.runOnUIThread(moveToSendStateRunnable = () -> {
                                    moveToSendStateRunnable = null;
                                    updateRecordInterface(RECORD_STATE_SENDING, true);
                                }, shouldDrawBackground ? 500 : 0);
                            }
                        }
                    }
                    return true;
                } else if (motionEvent.getAction() == MotionEvent.ACTION_MOVE && recordingAudioVideo) {
                    float x = motionEvent.getX();
                    float y = motionEvent.getY();
                    if (recordCircle.isSendButtonVisible()) {
                        return false;
                    }
                    if (recordCircle.setLockTranslation(y) == 2) {
                        startLockTransition();
                        return false;
                    } else {
                        recordCircle.setMovingCords(x, y);
                    }

                    if (startedDraggingX == -1) {
                        startedDraggingX = x;
                        distCanMove = (float) (sizeNotifierLayout.getMeasuredWidth() * 0.35);
                        if (distCanMove > dp(140)) {
                            distCanMove = dp(140);
                        }
                    }

                    x = x + audioVideoButtonContainer.getX();
                    float dist = (x - startedDraggingX);
                    float alpha = 1.0f + dist / distCanMove;
                    if (startedDraggingX != -1) {
                        if (alpha > 1) {
                            alpha = 1;
                        } else if (alpha < 0) {
                            alpha = 0;
                        }
                        if (slideText != null) {
                            slideText.setSlideX(alpha);
                        }
                        setSlideToCancelProgress(alpha);
                    }

                    if (alpha == 0) {
                        if (hasRecordVideo && isInVideoMode()) {
                            CameraController.getInstance().cancelOnInitRunnable(onFinishInitCameraRunnable);
                            pendingCameraFront = null;
                            delegate.needStartRecordVideo(2, true, 0, 0, voiceOnce ? 0x7FFFFFFF : 0, effectId, 0);
                            sendButton.setEffect(effectId = 0);
                        } else {
                            delegate.needStartRecordAudio(0);
                            MediaController.getInstance().stopRecording(0, false, 0, voiceOnce, 0);
                        }
                        recordingAudioVideo = false;
                        updateRecordInterface(RECORD_STATE_CANCEL_BY_GESTURE, true);
                    }
                    return true;
                }
                return true;
            }

            private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            private final RectF backgroundRect = new RectF();

            @Override
            protected void dispatchDraw(@NonNull Canvas canvas) {
                boolean isMenuState = audioVideoSendButton != null
                        && audioVideoSendButton.getCurrentState() == ChatActivityEnterViewAnimatedIconView.State.MENU;
                if ((!audioVideoButtonContainerForbidden || isIosInputAppearance()) && (!isMenuState || isIosInputAppearance())) {
                    float s = 1;
                    if (expandStickersButton != null) {
                        if (expandStickersButton.getVisibility() == View.VISIBLE) {
                            s = 1f - expandStickersButton.getAlpha();
                        }
                    }

                    canvas.save();
                    canvas.scale(s, s, getMeasuredWidth() - dpf2(IOS_BUBBLE_RADIUS_DP), getMeasuredHeight() - dpf2(IOS_BUBBLE_RADIUS_DP));
                    if (isIosInputAppearance() && voiceBubbleDrawable != null) {
                        voiceBubbleDrawable.setBounds(getMeasuredWidth() - dp(DEFAULT_HEIGHT), getMeasuredHeight() - dp(DEFAULT_HEIGHT), getMeasuredWidth(), getMeasuredHeight());
                        voiceBubbleDrawable.setAlpha((int) (255 * s * audioVideoSendButton.getAlpha()));
                        DrawableUtils.drawWithScale(canvas, voiceBubbleDrawable, audioVideoSendButton.getScaleX());
                    } else {
                        final float r = dpf2(19);
                        paint.setColor(ActionButtonStyle.resolveBackgroundColor(resourcesProvider));
                        final float margin = dpf2(3);
                        final float height = dpf2(38);
                        final float width = dpf2(38);
                        backgroundRect.set(
                                getMeasuredWidth() - width - margin,
                                getMeasuredHeight() - height - margin,
                                getMeasuredWidth() - margin,
                                getMeasuredHeight() - margin
                        );
                        canvas.drawRoundRect(backgroundRect, r, r, paint);
                    }
                    canvas.restore();
                }
                super.dispatchDraw(canvas);
            }
        };
        if (NyaConfig.useChatAttachMediaMenu.Bool() && !isStories) {
            audioVideoButtonContainer.setOnClickListener(v -> {
                createRecordAudioPanel();
                createRecordCircle();
                if (recordCircle.isSendButtonVisible()) {
                    if (!hasRecordVideo || calledRecordRunnable) {
                        startedDraggingX = -1;
                        if (hasRecordVideo && isInVideoMode) {
                            delegate.needStartRecordVideo(1, true, 0, 0, voiceOnce ? 0x7FFFFFFF : 0, effectId, 0);
                        } else {
                            if (recordingAudioVideo && isInScheduleMode()) {
                                AlertsCreator.createScheduleDatePickerDialog(parentActivity, parentFragment.getDialogId(), (notify, scheduleDate, scheduleRepeatPeriod) -> MediaController.getInstance().stopRecording(1, notify, scheduleDate, voiceOnce, 0), () -> MediaController.getInstance().stopRecording(0, false, 0, voiceOnce, 0), null);
                            }
                            MediaController.getInstance().stopRecording(isInScheduleMode() ? 3 : 1, true, 0, voiceOnce, 0);
                            delegate.needStartRecordAudio(0);
                        }
                        recordingAudioVideo = false;
                        updateRecordInterface(RECORD_STATE_SENDING, true);
                    }
                    return;
                }
                onMenuClick(v);
            });
        }
        audioVideoButtonContainer.setSoundEffectsEnabled(false);
        sendButtonContainer.addView(audioVideoButtonContainer, LayoutHelper.createFrame(DEFAULT_HEIGHT, DEFAULT_HEIGHT, Gravity.RIGHT | Gravity.BOTTOM));
        audioVideoButtonContainer.setFocusable(true);
        audioVideoButtonContainer.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);

//        audioVideoButtonContainer.setOnTouchListener((view, motionEvent) -> {
//            createRecordCircle();
//            if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
//                if (recordCircle.isSendButtonVisible()) {
//                    if (!hasRecordVideo || calledRecordRunnable) {
//                        startedDraggingX = -1;
//                        if (hasRecordVideo && isInVideoMode()) {
//                            delegate.needStartRecordVideo(1, true, 0);
//                        } else {
//                            if (recordingAudioVideo && isInScheduleMode()) {
//                                AlertsCreator.createScheduleDatePickerDialog(parentActivity, parentFragment.getDialogId(), (notify, scheduleDate) -> MediaController.getInstance().stopRecording(1, notify, scheduleDate), () -> MediaController.getInstance().stopRecording(0, false, 0), resourcesProvider);
//                            }
//                            MediaController.getInstance().stopRecording(isInScheduleMode() ? 3 : 1, true, 0);
//                            delegate.needStartRecordAudio(0);
//                        }
//                        recordingAudioVideo = false;
//                        messageTransitionIsRunning = false;
//                        AndroidUtilities.runOnUIThread(moveToSendStateRunnable = () -> {
//                            moveToSendStateRunnable = null;
//                            updateRecordInterface(RECORD_STATE_SENDING);
//                        }, 200);
//                    }
//                    getParent().requestDisallowInterceptTouchEvent(true);
//                    return true;
//                }
//                if (parentFragment != null) {
//                    TLRPC.Chat chat = parentFragment.getCurrentChat();
//                    TLRPC.UserFull userFull = parentFragment.getCurrentUserInfo();
//                    if (chat != null && !(ChatObject.canSendVoice(chat) || (ChatObject.canSendRoundVideo(chat) && hasRecordVideo)) || userFull != null && userFull.voice_messages_forbidden) {
//                        delegate.needShowMediaBanHint();
//                        return true;
//                    }
//                }
//                if (hasRecordVideo) {
//                    calledRecordRunnable = false;
//                    recordAudioVideoRunnableStarted = true;
//                    AndroidUtilities.runOnUIThread(recordAudioVideoRunnable, 150);
//                } else {
//                    recordAudioVideoRunnable.run();
//                }
//                return true;
//            } else if (motionEvent.getAction() == MotionEvent.ACTION_UP || motionEvent.getAction() == MotionEvent.ACTION_CANCEL) {
//                if (motionEvent.getAction() == MotionEvent.ACTION_CANCEL && recordingAudioVideo) {
//                    if (recordCircle.slideToCancelProgress < 0.7f) {
//                        if (hasRecordVideo && isInVideoMode()) {
//                            CameraController.getInstance().cancelOnInitRunnable(onFinishInitCameraRunnable);
//                            delegate.needStartRecordVideo(2, true, 0);
//                        } else {
//                            delegate.needStartRecordAudio(0);
//                            MediaController.getInstance().stopRecording(0, false, 0);
//                        }
//                        recordingAudioVideo = false;
//                        updateRecordInterface(RECORD_STATE_CANCEL_BY_GESTURE);
//                    } else {
//                        recordCircle.sendButtonVisible = true;
//                        startLockTransition();
//                    }
//                    return false;
//                }
//                if (recordCircle != null && recordCircle.isSendButtonVisible() || recordedAudioPanel != null && recordedAudioPanel.getVisibility() == VISIBLE) {
//                    if (recordAudioVideoRunnableStarted) {
//                        AndroidUtilities.cancelRunOnUIThread(recordAudioVideoRunnable);
//                    }
//                    return false;
//                }
//
//                float x = motionEvent.getX() + audioVideoButtonContainer.getX();
//                float dist = (x - startedDraggingX);
//                float alpha = 1.0f + dist / distCanMove;
//                if (alpha < 0.45) {
//                    if (hasRecordVideo && isInVideoMode()) {
//                        CameraController.getInstance().cancelOnInitRunnable(onFinishInitCameraRunnable);
//                        delegate.needStartRecordVideo(2, true, 0);
//                    } else {
//                        delegate.needStartRecordAudio(0);
//                        MediaController.getInstance().stopRecording(0, false, 0);
//                    }
//                    recordingAudioVideo = false;
//                    updateRecordInterface(RECORD_STATE_CANCEL_BY_GESTURE);
//                } else {
//                    if (recordAudioVideoRunnableStarted) {
//                        AndroidUtilities.cancelRunOnUIThread(recordAudioVideoRunnable);
//                        if (sendVoiceEnabled && sendRoundEnabled) {
//                            delegate.onSwitchRecordMode(!isInVideoMode());
//                            setRecordVideoButtonVisible(!isInVideoMode(), true);
//                        } else {
//                            delegate.needShowMediaBanHint();
//                        }
//                        performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
//                        sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_CLICKED);
//                    } else if (!hasRecordVideo || calledRecordRunnable) {
//                        startedDraggingX = -1;
//                        if (hasRecordVideo && isInVideoMode()) {
//                            CameraController.getInstance().cancelOnInitRunnable(onFinishInitCameraRunnable);
//                            delegate.needStartRecordVideo(1, true, 0);
//                        } else if (!sendVoiceEnabled) {
//                            delegate.needShowMediaBanHint();
//                        } else {
//                            if (recordingAudioVideo && isInScheduleMode()) {
//                                AlertsCreator.createScheduleDatePickerDialog(parentActivity, parentFragment.getDialogId(), (notify, scheduleDate) -> MediaController.getInstance().stopRecording(1, notify, scheduleDate), () -> MediaController.getInstance().stopRecording(0, false, 0), resourcesProvider);
//                            }
//                            delegate.needStartRecordAudio(0);
//                            MediaController.getInstance().stopRecording(isInScheduleMode() ? 3 : 1, true, 0);
//                        }
//                        recordingAudioVideo = false;
//                        messageTransitionIsRunning = false;
//                        AndroidUtilities.runOnUIThread(moveToSendStateRunnable = () -> {
//                            moveToSendStateRunnable = null;
//                            updateRecordInterface(RECORD_STATE_SENDING);
//                        }, 500);
//                    }
//                }
//                return true;
//            } else if (motionEvent.getAction() == MotionEvent.ACTION_MOVE && recordingAudioVideo) {
//                float x = motionEvent.getX();
//                float y = motionEvent.getY();
//                if (recordCircle.isSendButtonVisible()) {
//                    return false;
//                }
//                if (recordCircle.setLockTranslation(y) == 2) {
//                    startLockTransition();
//                    return false;
//                } else {
//                    recordCircle.setMovingCords(x, y);
//                }
//
//                if (startedDraggingX == -1) {
//                    startedDraggingX = x;
//                    distCanMove = (float) (sizeNotifierLayout.getMeasuredWidth() * 0.35);
//                    if (distCanMove > AndroidUtilities.dp(140)) {
//                        distCanMove = AndroidUtilities.dp(140);
//                    }
//                }
//
//                x = x + audioVideoButtonContainer.getX();
//                float dist = (x - startedDraggingX);
//                float alpha = 1.0f + dist / distCanMove;
//                if (startedDraggingX != -1) {
//                    if (alpha > 1) {
//                        alpha = 1;
//                    } else if (alpha < 0) {
//                        alpha = 0;
//                    }
//                    if (slideText != null) {
//                        slideText.setSlideX(alpha);
//                    }
//                    if (recordCircle != null) {
//                        recordCircle.setSlideToCancelProgress(alpha);
//                    }
//                }
//
//                if (alpha == 0) {
//                    if (hasRecordVideo && isInVideoMode()) {
//                        CameraController.getInstance().cancelOnInitRunnable(onFinishInitCameraRunnable);
//                        delegate.needStartRecordVideo(2, true, 0);
//                    } else {
//                        delegate.needStartRecordAudio(0);
//                        MediaController.getInstance().stopRecording(0, false, 0);
//                    }
//                    recordingAudioVideo = false;
//                    updateRecordInterface(RECORD_STATE_CANCEL_BY_GESTURE);
//                }
//                return true;
//            }
//            view.onTouchEvent(motionEvent);
//            return true;
//        });

        micOutline = getResources().getDrawable(R.drawable.input_mic).mutate();
        ThemeBind.drawableColorFilter(this, micOutline, Theme.key_glass_defaultIcon, resourcesProvider, PorterDuff.Mode.MULTIPLY);

        cameraOutline = getResources().getDrawable(R.drawable.input_video).mutate();
        ThemeBind.drawableColorFilter(this, cameraOutline, Theme.key_glass_defaultIcon, resourcesProvider, PorterDuff.Mode.MULTIPLY);

        audioVideoSendButton = new ChatActivityEnterViewAnimatedIconView(context, 24) {
            private final Rect tmpRectF = new Rect();
            @Override
            public void draw(@NonNull Canvas canvas) {
                if (audioVideoButtonContainerForbidden && !NyaConfig.useChatAttachMediaMenu.Bool()) {
                    tmpRectF.set(0, 0, getMeasuredWidth(), getMeasuredHeight());
                    tmpRectF.inset(dp(7.5f), dp(7.5f));
                    Drawable d = getCurrentState() == State.VIDEO ? cameraOutline : micOutline;
                    d.setBounds(tmpRectF);
                    d.draw(canvas);
                } else {
                    super.draw(canvas);
                }
            }
        };
        audioVideoSendButton.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
//        audioVideoSendButton.setFocusable(true);
//        audioVideoSendButton.setAccessibilityDelegate(mediaMessageButtonsDelegate);
        padding = dp(10f);
        audioVideoSendButton.setPadding(padding, padding, padding, padding);
        audioVideoButtonContainer.addView(audioVideoSendButton, LayoutHelper.createFrame(DEFAULT_HEIGHT, DEFAULT_HEIGHT));

        cancelBotButton = new ImageView(context);
        cancelBotButton.setVisibility(INVISIBLE);
        cancelBotButton.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        cancelBotButton.setImageDrawable(progressDrawable = new CloseProgressDrawable2() {
            @Override
            protected int getCurrentColor() {
                return Theme.getColor(Theme.key_chat_messagePanelCancelInlineBot);
            }
        });
        cancelBotButton.setContentDescription(getString("Cancel", R.string.Cancel));
        cancelBotButton.setSoundEffectsEnabled(false);
        cancelBotButton.setScaleX(0.1f);
        cancelBotButton.setScaleY(0.1f);
        cancelBotButton.setAlpha(0.0f);
        ThemeBind.selectorBackground(cancelBotButton, Theme.createSelectorDrawable(getThemedColor(Theme.key_listSelector)), null, ThemeBind.of(Theme.key_listSelector), resourcesProvider);
        sendButtonContainer.addView(cancelBotButton, LayoutHelper.createFrame(DEFAULT_HEIGHT, DEFAULT_HEIGHT, Gravity.RIGHT | Gravity.BOTTOM));
        cancelBotButton.setOnClickListener(view -> {
            String text = messageEditText != null ? messageEditText.getText().toString() : "";
            int idx = text.indexOf(' ');
            if (idx == -1 || idx == text.length() - 1) {
                setFieldText("");
            } else {
                setFieldText(text.substring(0, idx + 1));
            }
        });

        sendButton = new SendButton(context, isInScheduleMode() ? R.drawable.input_schedule : R.drawable.send_plane_24, resourcesProvider, true) {
            @Override
            public boolean isInScheduleMode() {
                return ChatActivityEnterView.this.isInScheduleMode();
            }

            @Override
            public boolean isOpen() {
                return messageSendPreview != null && messageSendPreview.isShowing() || super.isOpen();
            }

            @Override
            public boolean isInactive() {
                return !isInScheduleMode() && slowModeTimer == Integer.MAX_VALUE;
            }

            @Override
            public boolean shouldDrawBackground() {
                return shouldDrawBackground;
            }

            @Override
            public boolean shouldDrawInternalCircle() {
                return !isIosInputAppearance();
            }

            @Override
            public void setAlpha(float alpha) {
                super.setAlpha(alpha);
                updateAttachButtonTranslationX();
            }
        };
        sendButton.setVisibility(INVISIBLE);
        sendButton.setContentDescription(getString(R.string.Send));
        sendButton.setSoundEffectsEnabled(false);
        sendButton.setScaleX(0.1f);
        sendButton.setScaleY(0.1f);
        sendButton.setAlpha(0.0f);
        sendButtonContainer.addView(sendButton, LayoutHelper.createFrame(100, DEFAULT_HEIGHT, Gravity.RIGHT | Gravity.BOTTOM));
        sendButton.setOnClickListener(view -> {
            if ((messageSendPreview != null && messageSendPreview.isShowing()) || (runningAnimationAudio != null && runningAnimationAudio.isRunning()) || moveToSendStateRunnable != null) {
                return;
            }
            sendMessage();
        });
        sendButton.setOnLongClickListener(this::onSendLongClick);
        if (AndroidUtilities.isAccessibilityScreenReaderEnabled()) {
            sendButtonContainer.setOnLongClickListener(this::onSendLongClick);
        }

        sendButtonBlockedByTypingView = new SendButtonBlockedByTypingView(context, resourcesProvider);
        sendButtonBlockedByTypingView.setVisibility(View.INVISIBLE);
        sendButtonContainer.addView(sendButtonBlockedByTypingView, LayoutHelper.createFrame(DEFAULT_HEIGHT, DEFAULT_HEIGHT, Gravity.RIGHT | Gravity.BOTTOM));

        slowModeButton = new SlowModeBtn(context);
        slowModeButton.setTextSize(16);
        slowModeButton.setVisibility(INVISIBLE);
        slowModeButton.setSoundEffectsEnabled(false);
        slowModeButton.setScaleX(0.1f);
        slowModeButton.setScaleY(0.1f);
        slowModeButton.setAlpha(0.0f);
        slowModeButton.setPadding(dp(14), 0, dp(14), 0);
        slowModeButton.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        slowModeButton.setTextColor(getThemedColor(Theme.key_glass_defaultIcon));
        sendButtonContainer.addView(slowModeButton, LayoutHelper.createFrame(74, DEFAULT_HEIGHT, Gravity.RIGHT | Gravity.BOTTOM));
        slowModeButton.setOnClickListener(v -> {
            if (delegate != null) {
                if (delegate.checkCanRemoveRestrictionsByBoosts()) {
                    return;
                }
                delegate.onUpdateSlowModeButton(slowModeButton, true, slowModeButton.getText());
            }
        });
        slowModeButton.setOnLongClickListener(v -> {
            if (messageEditText == null || messageEditText.length() <= 0) {
                return false;
            }
            return onSendLongClick(v);
        });

        SharedPreferences sharedPreferences = MessagesController.getGlobalEmojiSettings();
        keyboardHeight = sharedPreferences.getInt("kbd_height", dp(200));
        keyboardHeightLand = sharedPreferences.getInt("kbd_height_land3", dp(200));

        setRecordVideoButtonVisible(false, false);
        checkSendButton(false);
        checkChannelRights();

        createMessageEditText();
    }

    public void setViewParentForEmoji(ViewGroup viewParent) {
        viewParentForEmojiView = viewParent;
    }

    private long paidMessagesPrice;
    public void updateSendButtonPaid() {
        long paidMessagesPrice = getStarsPrice();
        if (paidMessagesPrice > 0) {
            paidMessagesPrice *= getMessagesCount();
        }
        if (this.paidMessagesPrice != paidMessagesPrice) {
            final View oldSendButton = getSendButtonInternal();
            this.paidMessagesPrice = paidMessagesPrice;
            final View newSendButton = getSendButtonInternal();
            if (oldSendButton != newSendButton) {
                newSendButton.setVisibility(oldSendButton.getVisibility());
                newSendButton.setAlpha(oldSendButton.getAlpha());
                newSendButton.setScaleX(oldSendButton.getScaleX());
                newSendButton.setScaleY(oldSendButton.getScaleY());

                oldSendButton.setVisibility(View.GONE);
            }
            sendButton.setStarsPrice(paidMessagesPrice, 1);
            updateFieldRight(lastAttachVisible);
        }
        if (isLiveComment) {
            createCaptionLimitView();
            int newLimit = areLiveCommentsFree() ? HighlightMessageSheet.getMaxLength(currentAccount) : HighlightMessageSheet.getTierOption(currentAccount, (int) paidMessagesPrice, HighlightMessageSheet.TIER_LENGTH);
            if (currentLimit != newLimit) {
                currentLimit = newLimit;
                int beforeLimit;
                if (currentLimit > 0 && (beforeLimit = currentLimit - codePointCount) <= (isLiveComment ? 5 : 100)) {
                    if (beforeLimit < -9999) {
                        beforeLimit = -9999;
                    }
                    createCaptionLimitView();
                    captionLimitView.setNumber(beforeLimit, captionLimitView.getVisibility() == View.VISIBLE);
                    if (captionLimitView.getVisibility() != View.VISIBLE) {
                        captionLimitView.setVisibility(View.VISIBLE);
                        captionLimitView.setAlpha(0);
                        captionLimitView.setScaleX(0.5f);
                        captionLimitView.setScaleY(0.5f);
                    }
                    captionLimitView.animate().setListener(null).cancel();
                    captionLimitView.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(100).start();
                    captionLimitView.setTextColor(getThemedColor(beforeLimit < 0 ? Theme.key_text_RedRegular : Theme.key_windowBackgroundWhiteGrayText));
                } else if (captionLimitView != null) {
                    captionLimitView.animate().alpha(0).scaleX(0.5f).scaleY(0.5f).setDuration(100).setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            captionLimitView.setVisibility(View.GONE);
                        }
                    });
                }
            }
        }
    }

    public void setOnSendButtonLongClick(OnLongClickListener listener) {
        sendButton.setOnLongClickListener(listener != null ? listener : this::onSendLongClick);
    }

    public long getStarsPrice() {
        return parentFragment != null ?
                parentFragment.getMessagesController().getSendPaidMessagesStars(parentFragment.getDialogId()) :
                MessagesController.getInstance(currentAccount).getSendPaidMessagesStars(dialog_id);
    }

    public boolean areLiveCommentsFree() {
        return false;
    }

    public int getMessagesCount() {
        int count = 0;
        if (parentFragment != null && parentFragment.messagePreviewParams != null) {
            count += parentFragment.messagePreviewParams.getForwardedMessagesCount();
        }
        if (messageEditText != null && !TextUtils.isEmpty(messageEditText.getText())) {
            String textFinal = SendMessagesHelper.getTrimmedString(messageEditText.getText().toString());
            final int limit = accountInstance.getMessagesController().getMaxMessageLength();
            if (textFinal.length() != 0) {
                count += (int) Math.ceil(textFinal.length() / (float) limit);
            } else {
                count++;
            }
        } else if (hasAudioToSend()) {
            count++;
        }
        return Math.max(1, count);
    }

    private void createCaptionLimitView() {
        if (captionLimitView != null) {
            return;
        }

        captionLimitView = new NumberTextView(getContext());
        captionLimitView.setVisibility(View.GONE);
        captionLimitView.setTextSize(15);
        captionLimitView.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteGrayText));
        captionLimitView.setTypeface(AndroidUtilities.bold());
        captionLimitView.setCenterAlign(true);
        addView(captionLimitView, Math.min(2, getChildCount()), LayoutHelper.createFrame(DEFAULT_HEIGHT, 20, Gravity.BOTTOM | Gravity.RIGHT, 3, 0, 0, DEFAULT_HEIGHT));
    }

    private void createScheduledButton() {
        if (scheduledButton != null || parentFragment == null) {
            return;
        }

        final Drawable drawable1 = getContext().getResources().getDrawable(R.drawable.input_calendar1).mutate();
        final Drawable drawable2 = getContext().getResources().getDrawable(R.drawable.input_calendar2).mutate();
        ThemeBind.drawableColorFilter(this, drawable1, Theme.key_glass_defaultIcon, resourcesProvider, PorterDuff.Mode.MULTIPLY);
        ThemeBind.drawableColorFilter(this, drawable2, Theme.key_chat_recordedVoiceDot, resourcesProvider, PorterDuff.Mode.MULTIPLY);
        CombinedDrawable combinedDrawable = new CombinedDrawable(drawable1, drawable2);

        scheduledButton = new ImageView(getContext()) {
            private float innerTranslationX;
            @Override
            public float getTranslationX() {
                return innerTranslationX;
            }
            @Override
            public void setTranslationX(float translationX) {
                innerTranslationX = translationX;
                if (isIosButtonPlacement()) {
                    super.setTranslationX(translationX);
                    return;
                }
                super.setTranslationX(attachLayoutPaddingTranslationX + attachLayoutTranslationX);
            }
        };
        scheduledButton.setImageDrawable(combinedDrawable);
        scheduledButton.setVisibility(GONE);
        scheduledButton.setContentDescription(getString(R.string.ScheduledMessages));
        scheduledButton.setScaleType(ImageView.ScaleType.CENTER);
        ThemeBind.selectorBackground(scheduledButton, Theme.createSelectorDrawable(getThemedColor(Theme.key_listSelector)), null, ThemeBind.of(Theme.key_listSelector), resourcesProvider);
        messageEditTextContainer.addView(scheduledButton, 2, LayoutHelper.createFrame(DEFAULT_HEIGHT, DEFAULT_HEIGHT, Gravity.BOTTOM | Gravity.RIGHT));
        scheduledButton.setOnClickListener(v -> {
            if (delegate != null) {
                delegate.openScheduledMessages();
            }
        });
        scheduledButton.setTranslationX(0);
        updateScheduledButtonLayoutParams();
    }

    private void updateScheduledButtonLayoutParams() {
        if (scheduledButton == null) {
            return;
        }
        int newRightMargin;
        if (isIosButtonPlacement()) {
            newRightMargin = dp(iosGapDp);
        } else {
            boolean isPaperclipVisible = attachButton != null && attachButton.getVisibility() == VISIBLE;
            boolean isBotVisible = botButton != null && botButton.getVisibility() == VISIBLE;
            boolean isGiftVisible = giftButton != null && giftButton.getVisibility() == VISIBLE;
            int slotsDp = (isPaperclipVisible ? DEFAULT_HEIGHT : 0) + (isBotVisible ? DEFAULT_HEIGHT : 0) + (isGiftVisible ? DEFAULT_HEIGHT : 0);
            newRightMargin = dp(slotsDp);
        }
        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) scheduledButton.getLayoutParams();
        if (layoutParams.rightMargin != newRightMargin) {
            layoutParams.rightMargin = newRightMargin;
            scheduledButton.setLayoutParams(layoutParams);
        }
    }

    private ValueAnimator animateScheduledTranslationX(float to) {
        ValueAnimator va = ValueAnimator.ofFloat(scheduledButton.getTranslationX(), to);
        va.addUpdateListener(a -> {
            scheduledButton.setTranslationX((float) a.getAnimatedValue());
        });
        return va;
    }

    @SuppressLint("AppCompatCustomView")
    private void createGiftButton() {
        if (giftButton != null || parentFragment == null) {
            return;
        }

        giftButton = new ImageView(getContext()) {
            @Override
            protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
                super.onLayout(changed, left, top, right, bottom);
                post(ChatActivityEnterView.this::checkBirthdayHint);
            }
            @Override
            public void setAlpha(float alpha) {
                super.setAlpha(alpha);
                if (scheduledButton != null) {
                    scheduledButton.setTranslationX(scheduledButton.getTranslationX());
                }
                updateScheduledButtonLayoutParams();
            }
            @Override
            public void setVisibility(int visibility) {
                super.setVisibility(visibility);
                updateScheduledButtonLayoutParams();
            }
        };
        giftButton.setImageResource(R.drawable.msg_input_gift);
        ThemeBind.colorFilter(giftButton, Theme.key_glass_defaultIcon, resourcesProvider, PorterDuff.Mode.MULTIPLY);
        giftButton.setVisibility(GONE);
        giftButton.setContentDescription(getString(R.string.GiftPremium));
        giftButton.setScaleType(ImageView.ScaleType.CENTER);
        ThemeBind.selectorBackground(giftButton, Theme.createSelectorDrawable(getThemedColor(Theme.key_listSelector)), null, ThemeBind.of(Theme.key_listSelector), resourcesProvider);
        attachLayout.addView(giftButton, 0, LayoutHelper.createFrame(DEFAULT_HEIGHT, DEFAULT_HEIGHT, Gravity.CENTER_VERTICAL | Gravity.RIGHT));
        giftButton.setOnClickListener(v -> {
            SharedPreferences.Editor edit = MessagesController.getInstance(currentAccount).getMainSettings().edit();
            if (BirthdayController.isToday(parentFragment.getCurrentUserInfo())) {
                edit.putBoolean(Calendar.getInstance().get(Calendar.YEAR) + "show_gift_for_" + parentFragment.getDialogId(), false);
            } else {
                edit.putBoolean("show_gift_for_" + parentFragment.getDialogId(), false);
            }
            if (
                MessagesController.getInstance(currentAccount).giftAttachMenuIcon &&
                MessagesController.getInstance(currentAccount).giftTextFieldIcon
            ) {
                edit.putBoolean("show_gift_for_" + parentFragment.getDialogId(), false);
            }
            edit.apply();

            final TLRPC.UserFull myUserInfo = MessagesController.getInstance(currentAccount).getUserFull(UserConfig.getInstance(currentAccount).getClientUserId());
            if (!(getParentFragment().getCurrentUserInfo() != null && getParentFragment().getCurrentUserInfo().display_gifts_button || myUserInfo != null && myUserInfo.display_gifts_button)) {
                AndroidUtilities.updateViewVisibilityAnimated(giftButton, false);
            }

            TLRPC.User user = getParentFragment().getCurrentUser();
            if (user == null) return;
            final boolean birthday = getParentFragment().getCurrentUserInfo() != null && BirthdayController.isToday(getParentFragment().getCurrentUserInfo().birthday);
            final AlertDialog progressDialog = new AlertDialog(getContext(), AlertDialog.ALERT_TYPE_SPINNER);
            progressDialog.showDelayed(200);
            final int reqId = BoostRepository.loadGiftOptions(currentAccount, null, loadedOptions -> {
                progressDialog.dismiss();
                loadedOptions = BoostRepository.filterGiftOptions(loadedOptions, 1);
                loadedOptions = BoostRepository.filterGiftOptionsByBilling(loadedOptions);
                new GiftSheet(getContext(), currentAccount, user.id, loadedOptions, null).setBirthday(birthday).show();
            });
            progressDialog.setOnCancelListener(di -> {
                parentFragment.getConnectionsManager().cancelRequest(reqId, true);
            });
        });
    }

    public void createSuggestionButton() {
        if (suggestButton != null) {
            return;
        }

        suggestButton = new ImageView(getContext());
        suggestButton.setScaleType(ImageView.ScaleType.CENTER);
        ThemeBind.colorFilter(suggestButton, Theme.key_glass_defaultIcon, resourcesProvider, PorterDuff.Mode.MULTIPLY);
        suggestButton.setImageResource(R.drawable.input_suggest_paid_24);
        ThemeBind.selectorBackground(suggestButton, Theme.createSelectorDrawable(getThemedColor(Theme.key_listSelector)), null, ThemeBind.of(Theme.key_listSelector), resourcesProvider);
        if (isLiveComment) {
            suggestButton.setTranslationX(dp(42));
            textFieldContainer.addView(suggestButton, LayoutHelper.createFrame(DEFAULT_HEIGHT, DEFAULT_HEIGHT, Gravity.BOTTOM | Gravity.RIGHT, 0, 0, 6 + DEFAULT_HEIGHT, 0));
        } else {
            attachLayout.addView(suggestButton, 0, LayoutHelper.createLinear(DEFAULT_HEIGHT, DEFAULT_HEIGHT));
        }
        suggestButton.setOnClickListener(v -> {
            if (adjustPanLayoutHelper != null && adjustPanLayoutHelper.animationInProgress() || attachLayoutPaddingAlpha == 0f) {
                return;
            }
            delegate.didPressSuggestionButton();
        });
        suggestButton.setContentDescription(getString(R.string.AccDescrAttachButton));
    }

    private boolean suggestButtonVisible;
    private ValueAnimator suggestButtonAppear;
    public void setSuggestionButtonVisible(boolean visible, boolean animated) {
        if (suggestButtonVisible == visible && animated) return;
        if (suggestButton == null) {
            if (visible || isLiveComment) {
                createSuggestionButton();
            } else {
                return;
            }
        }
        final boolean changed = suggestButtonVisible != visible;
        suggestButtonVisible = visible;

        final float s = visible ? 1 : 0.6f;
        final float alpha = visible ? 1 : 0;

        suggestButton.setEnabled(visible);
        suggestButton.setClickable(visible);
        if (suggestButtonAppear != null) {
            suggestButtonAppear.cancel();
            suggestButtonAppear = null;
        }
        if (animated) {
            if (isLiveComment) {
                suggestButton.setVisibility(View.VISIBLE);
            }
            suggestButtonAppear = ValueAnimator.ofFloat(suggestButton.getAlpha(), alpha);
            suggestButtonAppear.addUpdateListener(a -> {
                final float t = (float) a.getAnimatedValue();
                suggestButton.setScaleX(lerp(0.6f, 1, t));
                suggestButton.setScaleY(lerp(0.6f, 1, t));
                suggestButton.setAlpha(t);
            });
            suggestButtonAppear.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    if (isLiveComment) {
                        suggestButton.setVisibility(visible ? View.VISIBLE : View.GONE);
                    }
                }
            });
            suggestButtonAppear.setDuration(220);
            suggestButtonAppear.setInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT);
            suggestButtonAppear.start();
        } else {
            suggestButton.setScaleX(s);
            suggestButton.setScaleY(s);
            suggestButton.setAlpha(alpha);
            if (isLiveComment) {
                suggestButton.setVisibility(visible ? View.VISIBLE : View.GONE);
            }
        }

        updateFieldRight(lastAttachVisible);
        if (changed) {
            checkSendButton(true);
        }
    }

    private void createBotButton() {
        if (botButton != null) {
            return;
        }
        botButton = new ImageView(getContext()) {
            @Override
            public void setAlpha(float alpha) {
                super.setAlpha(alpha);
                if (scheduledButton != null) {
                    scheduledButton.setTranslationX(scheduledButton.getTranslationX());
                }
                updateScheduledButtonLayoutParams();
                if (isIosButtonPlacement() && messageEditTextContainer != null) {
                    messageEditTextContainer.requestLayout();
                }
            }
            @Override
            public void setVisibility(int visibility) {
                super.setVisibility(visibility);
                updateScheduledButtonLayoutParams();
            }
        };
        botButton.setImageDrawable(botButtonDrawable = new ReplaceableIconDrawable(getContext()));
        ThemeBind.drawableColorFilter(this, botButtonDrawable, Theme.key_glass_defaultIcon, resourcesProvider, PorterDuff.Mode.MULTIPLY);
        botButtonDrawable.setIcon(R.drawable.input_bot2, false);
        botButton.setScaleType(ImageView.ScaleType.CENTER);
        ThemeBind.selectorBackground(botButton, Theme.createSelectorDrawable(getThemedColor(Theme.key_listSelector)), null, ThemeBind.of(Theme.key_listSelector), resourcesProvider);
        botButton.setVisibility(GONE);
        AndroidUtilities.updateViewVisibilityAnimated(botButton, false, 0.1f, false);
        attachLayout.addView(botButton, 0, LayoutHelper.createLinear(BOT_BUTTON_WIDTH_DP, BOT_BUTTON_WIDTH_DP, Gravity.CENTER_VERTICAL));
        botButton.setOnClickListener(v -> {
//            if (hasBotWebView() && botCommandsMenuIsShowing()) {
//                botWebViewMenuContainer.dismiss(v::callOnClick);
//                return;
//            }
            if (searchingType != 0) {
                setSearchingTypeInternal(0, false);
                emojiView.closeSearch(false);
                if (messageEditText != null) {
                    messageEditText.requestFocus();
                }
            }
            if (botReplyMarkup != null) {
                if (!isPopupShowing() || currentPopupContentType != POPUP_CONTENT_BOT_KEYBOARD) {
                    showPopup(1, POPUP_CONTENT_BOT_KEYBOARD);
                } else if (isPopupShowing() && currentPopupContentType == POPUP_CONTENT_BOT_KEYBOARD) {
                    showPopup(0, POPUP_CONTENT_BOT_KEYBOARD, true, false);
                }
            } else if (hasBotCommands || hasQuickReplies) {
                setFieldText("/");
                if (messageEditText != null) {
                    messageEditText.requestFocus();
                }
                openKeyboard();
            }
            if (stickersExpanded) {
                setStickersExpanded(false, false, false);
            }
        });
    }

    private void createDoneButton(boolean bounceable) {
        if (doneButton != null) {
            return;
        }

        doneButton = new SendButton(getContext(), R.drawable.input_done, resourcesProvider, true) {
            @Override
            public boolean isOpen() {
                return true;
            }

            @Override
            public boolean isInactive() {
                return !doneButtonEnabled;
            }

            @Override
            public boolean shouldDrawInternalCircle() {
                return !isIosInputAppearance();
            }
        };
        doneButton.setContentDescription(getString(R.string.EditMessage));
        if (bounceable) {
            ScaleStateListAnimator.apply(doneButton);
        }
        textFieldContainer.addView(doneButton, LayoutHelper.createFrame(DEFAULT_HEIGHT, DEFAULT_HEIGHT, Gravity.BOTTOM | Gravity.RIGHT));
    }

    @SuppressLint("AppCompatCustomView")
    private void createExpandStickersButton() {
        if (expandStickersButton != null) {
            return;
        }
        expandStickersButton = new ImageView(getContext()) {
            @Override
            public void setAlpha(float alpha) {
                super.setAlpha(alpha);
                if (audioVideoButtonContainer != null) {
                    audioVideoButtonContainer.invalidate();
                }
            }

            @Override
            public void setVisibility(int visibility) {
                super.setVisibility(visibility);
                if (audioVideoButtonContainer != null) {
                    audioVideoButtonContainer.invalidate();
                }
            }

            @Override
            public boolean onTouchEvent(MotionEvent event) {
                if (getAlpha() <= 0f) { // for accessibility
                    return false;
                }
                return super.onTouchEvent(event);
            }
        };
        expandStickersButton.setScaleType(ImageView.ScaleType.CENTER);
        expandStickersButton.setImageDrawable(stickersArrow = new AnimatedArrowDrawable(getThemedColor(Theme.key_glass_defaultIcon), false));
        expandStickersButton.setVisibility(GONE);
        expandStickersButton.setScaleX(0.1f);
        expandStickersButton.setScaleY(0.1f);
        expandStickersButton.setAlpha(0.0f);
        ThemeBind.selectorBackground(expandStickersButton, Theme.createSelectorDrawable(getThemedColor(Theme.key_listSelector)), null, ThemeBind.of(Theme.key_listSelector), resourcesProvider);
        sendButtonContainer.addView(expandStickersButton, LayoutHelper.createFrame(DEFAULT_HEIGHT, DEFAULT_HEIGHT, Gravity.RIGHT | Gravity.BOTTOM));
        expandStickersButton.setOnClickListener(v -> {
            if (expandStickersButton.getVisibility() != VISIBLE || expandStickersButton.getAlpha() != 1.0f || waitingForKeyboardOpen || (keyboardVisible && messageEditText != null && messageEditText.isFocused())) {
                return;
            }
            if (stickersExpanded) {
                if (searchingType != 0) {
                    setSearchingTypeInternal(0, true);
                    emojiView.closeSearch(true);
                    emojiView.hideSearchKeyboard();
                    if (emojiTabOpen) {
                        checkSendButton(true);
                    }
                } else if (!stickersDragging) {
                    if (emojiView != null) {
                        emojiView.showSearchField(false);
                    }
                }
            } else if (!stickersDragging) {
                emojiView.showSearchField(true);
            }
            if (!stickersDragging) {
                setStickersExpanded(!stickersExpanded, true, false);
            }
        });
        expandStickersButton.setContentDescription(getString("AccDescrExpandPanel", R.string.AccDescrExpandPanel));
    }

    private void createRecordAudioPanel() {
        if (recordedAudioPanel != null) {
            return;
        }

        recordedAudioPanel = new FrameLayout(getContext()) {
            @Override
            public void setVisibility(int visibility) {
                super.setVisibility(visibility);
                updateSendAsButton();
            }

            @Override
            protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
                if (!isStories && isIosInputAppearance() && child == recordDeleteImageView) {
                    if (recordDeleteBubbleDrawable == null) {
                        initRecordDeleteBubble();
                    }
                    if (recordDeleteBubbleDrawable != null) {
                        drawIosBubble(canvas, recordDeleteBubbleDrawable, child);
                    }
                }
                return super.drawChild(canvas, child, drawingTime);
            }
        };
        recordedAudioPanel.setVisibility(audioToSend == null ? GONE : VISIBLE);
        recordedAudioPanel.setFocusable(true);
        recordedAudioPanel.setFocusableInTouchMode(true);
        recordedAudioPanel.setClickable(true);
        messageEditTextContainer.addView(recordedAudioPanel, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, DEFAULT_HEIGHT, Gravity.BOTTOM));

        recordDeleteImageView = new RLottieImageView(getContext());
        recordDeleteImageView.setScaleType(ImageView.ScaleType.CENTER);
        recordDeleteImageView.setAnimation(R.raw.chat_audio_record_delete_2, 28, 28);
        recordDeleteImageView.getAnimatedDrawable().setInvalidateOnProgressSet(true);
        updateRecordedDeleteIconColors();
        recordDeleteImageView.setContentDescription(getString("Delete", R.string.Delete));
        ThemeBind.selectorBackground(recordDeleteImageView, Theme.createSelectorDrawable(getThemedColor(Theme.key_listSelector)), null, ThemeBind.of(Theme.key_listSelector), resourcesProvider);
        recordedAudioPanel.addView(recordDeleteImageView, LayoutHelper.createFrame(DEFAULT_HEIGHT, DEFAULT_HEIGHT));
        recordDeleteImageView.setOnClickListener(v -> {
            if (runningAnimationAudio != null && runningAnimationAudio.isRunning()) {
                return;
            }
            resetRecordedState();
        });

        videoTimelineView = new VideoTimelineView(getContext());
        videoTimelineView.setVisibility(INVISIBLE);
        videoTimelineView.useClip = !shouldDrawBackground;
        videoTimelineView.setRoundFrames(true);
        videoTimelineView.setDelegate(new VideoTimelineView.VideoTimelineViewDelegate() {
            @Override
            public void onLeftProgressChanged(float progress) {
                if (videoToSendMessageObject == null) {
                    return;
                }
                videoToSendMessageObject.startTime = (long) (progress * videoToSendMessageObject.estimatedDuration);
                delegate.needChangeVideoPreviewState(2, progress);
            }

            @Override
            public void onRightProgressChanged(float progress) {
                if (videoToSendMessageObject == null) {
                    return;
                }
                videoToSendMessageObject.endTime = (long) (progress * videoToSendMessageObject.estimatedDuration);
                delegate.needChangeVideoPreviewState(2, progress);
            }

            @Override
            public void didStartDragging() {
                delegate.needChangeVideoPreviewState(1, 0);
            }

            @Override
            public void didStopDragging() {
                delegate.needChangeVideoPreviewState(0, 0);
            }
        });
        recordedAudioPanel.addView(videoTimelineView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.CENTER_VERTICAL | Gravity.LEFT, 56, 0, 8, 0));

        VideoTimelineView.TimeHintView videoTimeHintView = new VideoTimelineView.TimeHintView(getContext());
        videoTimelineView.setTimeHintView(videoTimeHintView);
        sizeNotifierLayout.addView(videoTimeHintView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.BOTTOM, 0, 0, 0, 52));

        audioTimelineView = new RecordedAudioPlayerView(getContext(), resourcesProvider);
        float timelineLeftInsetDp = (isIosInputAppearance() && !isStories) ? iosGapDp + 12 : 0;
        float timelineRightInsetDp = (isIosInputAppearance() && !isStories) ? iosGapDp + 8 : 0;
        recordedAudioPanel.addView(audioTimelineView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 32, Gravity.CENTER_VERTICAL | Gravity.LEFT, DEFAULT_HEIGHT + timelineLeftInsetDp, 0, 4 + timelineRightInsetDp, 0));

        updateFieldRight(lastAttachVisible);
    }

    private void resetRecordedState() {
        if (audioTimelineView != null) {
            audioTimelineView.setPlaying(false);
        }
        if (videoToSendMessageObject != null) {
            CameraController.getInstance().cancelOnInitRunnable(onFinishInitCameraRunnable);
            pendingCameraFront = null;
            delegate.needStartRecordVideo(2, true, 0, 0, voiceOnce ? 0x7FFFFFFF : 0, effectId, 0);
            sendButton.setEffect(effectId = 0);
        } else {
            MessageObject playing = MediaController.getInstance().getPlayingMessageObject();
            if (playing != null && playing == audioToSendMessageObject) {
                MediaController.getInstance().cleanupPlayer(true, true);
            }
        }
        if (audioToSendPath != null) {
            if (BuildVars.LOGS_ENABLED) {
                FileLog.d("delete file " + audioToSendPath);
            }
            new File(audioToSendPath).delete();
        }
        MediaController.getInstance().cleanRecording(true);
        MediaDataController.getInstance(currentAccount).pushDraftVoiceMessage(dialog_id, parentFragment != null && parentFragment.isTopic ? parentFragment.getTopicId() : 0, null);
        MediaController.getInstance().stopRecording(0, false, 0, false, 0);
        millisecondsRecorded = 0;
        hideRecordedAudioPanel(false);
        checkSendButton(true);
    }

    private void createSenderSelectView() {
        if (senderSelectView != null || getContext() == null) {
            return;
        }
        senderSelectView = new SenderSelectView(getContext());
        senderSelectView.setOnClickListener(v -> {
            if (isLiveComment ? isPopupShowing() : getTranslationY() != 0) {
                onEmojiSearchClosed = () -> senderSelectView.callOnClick();
                if (isLiveComment) {
                    hidePopup(true, false);
                } else {
                    hidePopup(true, true);
                }
                return;
            }
            if (delegate.measureKeyboardHeight() > dp(20)) {
                int totalHeight = delegate.getContentViewHeight();
                int keyboard = delegate.measureKeyboardHeight();
                if (keyboard <= dp(20)) {
                    totalHeight += keyboard;
                }
                if (emojiViewVisible) {
                    totalHeight -= getEmojiPadding();
                }

                if (totalHeight < dp(200)) {
                    onKeyboardClosed = () -> senderSelectView.callOnClick();
                    closeKeyboard();
                    return;
                }
            }
            if (delegate.getSendAsPeers() != null) {
                try {
                    if (!NyaConfig.disableVibration.Bool()) v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
                } catch (Exception ignored) {
                }
                if (senderSelectPopupWindow != null) {
                    senderSelectPopupWindow.setPauseNotifications(false);
                    senderSelectPopupWindow.startDismissAnimation();
                    return;
                }
                MessagesController controller = MessagesController.getInstance(currentAccount);

                final TLRPC.ChatFull chatFull;
                TLRPC.Peer defPeer;
                if (isLiveComment) {
                    final TLRPC.Chat chat = null;
                    chatFull = null;
                    defPeer = delegate.getDefaultSendAs();
                } else {
                    final TLRPC.Chat chat = MessagesController.getInstance(currentAccount).getChat(-dialog_id);
                    chatFull = MessagesController.getInstance(currentAccount).getChatFull(-dialog_id);
                    defPeer = chatFull != null ? chatFull.default_send_as : null;
                }
                if (defPeer == null && delegate.getSendAsPeers() != null && !delegate.getSendAsPeers().peers.isEmpty()) {
                    defPeer = delegate.getSendAsPeers().peers.get(0).peer;
                }
                final boolean isChannel = ChatObject.isChannelAndNotMegaGroup(controller.getChat(-dialog_id));

                ViewGroup fl;
                if (isLiveComment) {
                    fl = (ViewGroup) getParent();
                } else {
                    fl = parentFragment.getParentLayout().getOverlayContainerView();
                }

                senderSelectPopupWindow = new SenderSelectPopup(getContext(), parentFragment, controller, isChannel, defPeer, delegate.getSendAsPeers(), (recyclerView, senderView, peer) -> {
                    if (senderSelectPopupWindow == null) return;
                    if (chatFull != null) {

                        var chat = controller.getChat(chatFull.id);
                        if (chat != null && ChatObject.isMegagroup(chat) && chat.creator) {
                            var self = UserConfig.getInstance(currentAccount).getCurrentUser();

                            if (peer.channel_id == chat.id) {
                                var rights = chat.admin_rights;
                                rights.anonymous = true;
                                var rank = MessagesController.getInstance(currentAccount).getAdminRank(chat.id, self.id);
                                MessagesController.getInstance(currentAccount).setUserAdminRole(chat.id, self, rights,  rank, false, parentFragment, false, false, null, null);
                            } else if (peer.user_id == self.id) {
                                var rights = chat.admin_rights;
                                rights.anonymous = false;
                                var rank = MessagesController.getInstance(currentAccount).getAdminRank(chat.id, self.id);
                                MessagesController.getInstance(currentAccount).setUserAdminRole(chat.id, self, rights,  rank, false, parentFragment, false, false, null, null);
                            }
                        }

                        chatFull.default_send_as = peer;
                    }
                    updateSendAsButton();

                    if (delegate == null || !delegate.setDefaultSendAs(dialog_id, DialogObject.getPeerDialogId(peer))) {
                        controller.setDefaultSendAs(dialog_id, DialogObject.getPeerDialogId(peer));
                    }

                    int[] loc = new int[2];
                    boolean wasSelected = senderView.avatar.isSelected();
                    senderView.avatar.getLocationInWindow(loc);
                    senderView.avatar.setSelected(true, true);

                    SimpleAvatarView avatar = new SimpleAvatarView(getContext());
                    if (peer.channel_id != 0) {
                        TLRPC.Chat chat = controller.getChat(peer.channel_id);
                        if (chat != null) {
                            avatar.setAvatar(chat);
                        }
                    } else if (peer.user_id != 0) {
                        TLRPC.User user = controller.getUser(peer.user_id);
                        if (user != null) {
                            avatar.setAvatar(user);
                        }
                    }
                    for (int i = 0; i < recyclerView.getChildCount(); i++) {
                        View ch = recyclerView.getChildAt(i);
                        if (ch instanceof SenderSelectPopup.SenderView && ch != senderView) {
                            SenderSelectPopup.SenderView childSenderView = (SenderSelectPopup.SenderView) ch;
                            childSenderView.avatar.setSelected(false, true);
                        }
                    }

                    AndroidUtilities.runOnUIThread(() -> {
                        if (senderSelectPopupWindow == null) {
                            return;
                        }
                        Dialog d = new Dialog(getContext(), R.style.TransparentDialogNoAnimation);
                        FrameLayout aFrame = new FrameLayout(getContext());
                        aFrame.addView(avatar, LayoutHelper.createFrame(SenderSelectPopup.AVATAR_SIZE_DP, SenderSelectPopup.AVATAR_SIZE_DP, Gravity.LEFT));
                        d.setContentView(aFrame);
                        d.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
                        d.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                        d.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
                        d.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
                        d.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                        d.getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
                        d.getWindow().addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
                        d.getWindow().getAttributes().windowAnimations = 0;
                        d.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
                        d.getWindow().setStatusBarColor(0);
                        d.getWindow().setNavigationBarColor(0);

                        int color = Theme.getColor(Theme.key_actionBarDefault, null, true);
                        AndroidUtilities.setLightStatusBar(d, color == Color.WHITE);

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            int color2 = Theme.getColor(Theme.key_windowBackgroundGray, null, true);
                            float brightness = AndroidUtilities.computePerceivedBrightness(color2);
                            AndroidUtilities.setLightNavigationBar(d, brightness >= 0.721f);
                        }
                        float offX = 0, offY = 0;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            WindowInsets wi = getRootWindowInsets();
                            popupX += wi.getSystemWindowInsetLeft();
                        }

                        senderSelectView.getLocationInWindow(location);
                        float endX = location[0], endY = location[1];

                        float off = dp(5);
                        float startX = loc[0] + popupX + off + dp(4) + offX, startY = loc[1] + popupY + off + offY;
                        avatar.setTranslationX(startX);
                        avatar.setTranslationY(startY);

                        final float startScale = (float) (SenderSelectPopup.AVATAR_SIZE_DP - 10) / SenderSelectPopup.AVATAR_SIZE_DP;
                        final float endScale = senderSelectView.getLayoutParams().width * (isLiveComment ? senderSelectView.getScaleX() : 1.0f) / (float) dp(SenderSelectPopup.AVATAR_SIZE_DP);
                        avatar.setPivotX(0);
                        avatar.setPivotY(0);
                        avatar.setScaleX(startScale);
                        avatar.setScaleY(startScale);

                        avatar.getViewTreeObserver().addOnDrawListener(new ViewTreeObserver.OnDrawListener() {
                            @Override
                            public void onDraw() {
                                avatar.post(() -> {
                                    avatar.getViewTreeObserver().removeOnDrawListener(this);
                                    senderView.avatar.setHideAvatar(true);
                                });
                            }
                        });
                        d.show();

                        if (!isLiveComment) {
                            senderSelectView.setScaleX(1f);
                            senderSelectView.setScaleY(1f);
                        }
                        senderSelectView.setAlpha(1f);

                        float translationStiffness = 700f;
                        senderSelectPopupWindow.startDismissAnimation(
                                isLiveComment ? null :
                                new SpringAnimation(senderSelectView, DynamicAnimation.SCALE_X)
                                        .setSpring(new SpringForce(0.5f)
                                                .setStiffness(SenderSelectPopup.SPRING_STIFFNESS)
                                                .setDampingRatio(SpringForce.DAMPING_RATIO_NO_BOUNCY)),
                                isLiveComment ? null :
                                new SpringAnimation(senderSelectView, DynamicAnimation.SCALE_Y)
                                        .setSpring(new SpringForce(0.5f)
                                                .setStiffness(SenderSelectPopup.SPRING_STIFFNESS)
                                                .setDampingRatio(SpringForce.DAMPING_RATIO_NO_BOUNCY)),
                                new SpringAnimation(senderSelectView, DynamicAnimation.ALPHA)
                                        .setSpring(new SpringForce(0f)
                                                .setStiffness(SenderSelectPopup.SPRING_STIFFNESS)
                                                .setDampingRatio(SpringForce.DAMPING_RATIO_NO_BOUNCY))
                                        .addEndListener((animation, canceled, value, velocity) -> {
                                            if (d.isShowing()) {
                                                avatar.setTranslationX(endX);
                                                avatar.setTranslationY(endY);

                                                senderSelectView.setProgress(0, false);
                                                if (!isLiveComment) {
                                                    senderSelectView.setScaleX(1);
                                                    senderSelectView.setScaleY(1);
                                                }
                                                senderSelectView.setAlpha(1);

                                                senderSelectView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                                                    @Override
                                                    public boolean onPreDraw() {
                                                        senderSelectView.getViewTreeObserver().removeOnPreDrawListener(this);
                                                        senderSelectView.postDelayed(d::dismiss, 100);
                                                        return true;
                                                    }
                                                });
                                            }
                                        }),
                                new SpringAnimation(avatar, DynamicAnimation.TRANSLATION_X)
                                        .setStartValue(MathUtils.clamp(startX, endX - dp(6), startX))
                                        .setSpring(new SpringForce(endX)
                                                .setStiffness(translationStiffness)
                                                .setDampingRatio(SpringForce.DAMPING_RATIO_LOW_BOUNCY))
                                        .setMinValue(endX - dp(6)),
                                new SpringAnimation(avatar, DynamicAnimation.TRANSLATION_Y)
                                        .setStartValue(MathUtils.clamp(startY, startY, endY + dp(6)))
                                        .setSpring(new SpringForce(endY)
                                                .setStiffness(translationStiffness)
                                                .setDampingRatio(SpringForce.DAMPING_RATIO_LOW_BOUNCY))
                                        .setMaxValue(endY + dp(6))
                                        .addUpdateListener(new DynamicAnimation.OnAnimationUpdateListener() {
                                            boolean performedHapticFeedback = false;

                                            @Override
                                            public void onAnimationUpdate(DynamicAnimation animation, float value, float velocity) {
                                                if (!performedHapticFeedback && value >= endY) {
                                                    performedHapticFeedback = true;
                                                    try {
                                                        if (!NyaConfig.disableVibration.Bool()) avatar.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
                                                    } catch (Exception ignored) {
                                                    }
                                                }
                                            }
                                        })
                                        .addEndListener((animation, canceled, value, velocity) -> {
                                            if (d.isShowing()) {
                                                avatar.setTranslationX(endX);
                                                avatar.setTranslationY(endY);

                                                senderSelectView.setProgress(0, false);
                                                if (!isLiveComment) {
                                                    senderSelectView.setScaleX(1);
                                                    senderSelectView.setScaleY(1);
                                                }
                                                senderSelectView.setAlpha(1);

                                                senderSelectView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                                                    @Override
                                                    public boolean onPreDraw() {
                                                        senderSelectView.getViewTreeObserver().removeOnPreDrawListener(this);
                                                        senderSelectView.postDelayed(d::dismiss, 100);
                                                        return true;
                                                    }
                                                });
                                            }
                                        }),
                                new SpringAnimation(avatar, DynamicAnimation.SCALE_X)
                                        .setSpring(new SpringForce(endScale)
                                                .setStiffness(1000f)
                                                .setDampingRatio(SpringForce.DAMPING_RATIO_NO_BOUNCY)),
                                new SpringAnimation(avatar, DynamicAnimation.SCALE_Y)
                                        .setSpring(new SpringForce(endScale)
                                                .setStiffness(1000f)
                                                .setDampingRatio(SpringForce.DAMPING_RATIO_NO_BOUNCY)));
                    }, wasSelected ? 0 : SimpleAvatarView.SELECT_ANIMATION_DURATION);
                }, resourcesProvider) {
                    @Override
                    public void dismiss() {
                        if (senderSelectPopupWindow != this) {
                            super.dismiss();
                            return;
                        }

                        senderSelectPopupWindow = null;

                        if (!runningCustomSprings) {
                            startDismissAnimation();
                            senderSelectView.setProgress(0, true, true);
                        } else {
                            for (SpringAnimation springAnimation : springAnimations) {
                                springAnimation.cancel();
                            }
                            springAnimations.clear();
                            super.dismiss();
                        }
                    }
                };
                senderSelectPopupWindow.setPauseNotifications(true);
                senderSelectPopupWindow.setDismissAnimationDuration(220);
                senderSelectPopupWindow.setOutsideTouchable(true);
                senderSelectPopupWindow.setClippingEnabled(true);
                senderSelectPopupWindow.setFocusable(true);
                senderSelectPopupWindow.getContentView().measure(MeasureSpec.makeMeasureSpec(dp(1000), MeasureSpec.AT_MOST), MeasureSpec.makeMeasureSpec(dp(1000), MeasureSpec.AT_MOST));
                senderSelectPopupWindow.setInputMethodMode(ActionBarPopupWindow.INPUT_METHOD_NOT_NEEDED);
                senderSelectPopupWindow.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_UNSPECIFIED);
                senderSelectPopupWindow.getContentView().setFocusableInTouchMode(true);
                senderSelectPopupWindow.setAnimationEnabled(false);

                int pad = -dp(4);
                int[] location = new int[2];
                int popupX = pad;
                if (AndroidUtilities.isTablet() && parentFragment != null) {
                    parentFragment.getFragmentView().getLocationInWindow(location);
                    popupX += location[0];
                }
                int totalHeight = delegate.getContentViewHeight();
                int height = senderSelectPopupWindow.getContentView().getMeasuredHeight();
                int keyboard = delegate.measureKeyboardHeight();
                if (keyboard <= dp(20)) {
                    totalHeight += keyboard;
                }
                if (emojiViewVisible) {
                    totalHeight -= getEmojiPadding();
                }

                int shadowPad = dp(1);
                int popupY;
                if (height < totalHeight + pad * 2 - (parentFragment != null && parentFragment.isInBubbleMode() ? 0 : AndroidUtilities.statusBarHeight) - senderSelectPopupWindow.headerText.getMeasuredHeight()) {
                    ChatActivityEnterView.this.getLocationInWindow(location);
                    popupY = location[1] - height - pad - dp(2);
                } else {
                    popupY = parentFragment != null && parentFragment.isInBubbleMode() ? 0 : AndroidUtilities.statusBarHeight;
                    int off = dp(14);
                    senderSelectPopupWindow.recyclerContainer.getLayoutParams().height = totalHeight - popupY - off - getHeightWithTopView();
                }

                senderSelectPopupWindow.startShowAnimation();

                senderSelectPopupWindow.showAtLocation(v, Gravity.LEFT | Gravity.TOP, this.popupX = popupX, this.popupY = popupY);
                senderSelectView.setProgress(1);
            }
        });
        senderSelectView.setVisibility(GONE);
        messageEditTextContainer.addView(senderSelectView, LayoutHelper.createFrame(36, 36, Gravity.BOTTOM | Gravity.LEFT, 4.66f, 4, 4.66f, 4));
    }

    private void createBotCommandsMenuButton() {
        if (botCommandsMenuButton != null) {
            return;
        }
        botCommandsMenuButton = new BotCommandsMenuView(getContext());
        botCommandsMenuButton.setOnClickListener(view -> {
            boolean open = !botCommandsMenuButton.isOpened();
            botCommandsMenuButton.setOpened(open);
            try {
                if (!NyaConfig.disableVibration.Bool()) performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
            } catch (Exception ignore) {}
            if (hasBotWebView()) {
                if (open) {
                    if (emojiViewVisible || botKeyboardViewVisible) {
                        AndroidUtilities.runOnUIThread(this::openWebViewMenu, 275);
                        hidePopup(false);
                        return;
                    }

                    openWebViewMenu();
                }
//                else if (botWebViewMenuContainer != null) {
//                    botWebViewMenuContainer.dismiss();
//                }
                return;
            }

            if (open) {
                createBotCommandsMenuContainer();
                botCommandsMenuContainer.show();
            } else if (botCommandsMenuContainer != null) {
                botCommandsMenuContainer.dismiss();
            }
        });
        messageEditTextContainer.addView(botCommandsMenuButton, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, 32, Gravity.BOTTOM | Gravity.LEFT, 8, 6, 8, 6));
        AndroidUtilities.updateViewVisibilityAnimated(botCommandsMenuButton, false, 1f, false);
        botCommandsMenuButton.setExpanded(true, false);
    }

    private void createBotWebViewButton() {
        if (botWebViewButton != null) {
            return;
        }
        botWebViewButton = new ChatActivityBotWebViewButton(getContext());
        botWebViewButton.setVisibility(GONE);
        createBotCommandsMenuButton();
        botWebViewButton.setBotMenuButton(botCommandsMenuButton);
        messageEditTextContainer.addView(botWebViewButton, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.BOTTOM));
    }

    private void createRecordCircle() {
        createControlsView();
        if (recordCircle != null) {
            return;
        }
        recordCircle = new RecordCircle(getContext());
        recordCircle.setVisibility(GONE);
        sizeNotifierLayout.addView(recordCircle, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.BOTTOM));
    }

    private void createControlsView() {
        if (controlsView != null) {
            return;
        }
        controlsView = new ControlsView(getContext());
        controlsView.setVisibility(GONE);
        sizeNotifierLayout.addView(controlsView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.BOTTOM));
    }

    public boolean isRecordCircleOrControlsView(View view) {
        return view != null && (view == controlsView || view == recordCircle);
    }

    private void showRestrictedHint() {
        if (delegate != null && delegate.checkCanRemoveRestrictionsByBoosts()) {
            return;
        }
        if (DialogObject.isChatDialog(dialog_id)) {
            TLRPC.Chat chat = accountInstance.getMessagesController().getChat(-dialog_id);
            BulletinFactory.of(parentFragment).createSimpleBulletin(R.raw.passcode_lock_close, LocaleController.formatString("SendPlainTextRestrictionHint", R.string.SendPlainTextRestrictionHint, ChatObject.getAllowedSendString(chat)), 3).show();
        }
    }

    private void openWebViewMenu() {
        createBotWebViewMenuContainer();
        Runnable onRequestWebView = () -> {
            AndroidUtilities.hideKeyboard(this);
            WebViewRequestProps props = WebViewRequestProps.of(currentAccount, dialog_id, dialog_id, botMenuWebViewTitle, botMenuWebViewUrl, BotWebViewAttachedSheet.TYPE_BOT_MENU_BUTTON, 0, parentFragment == null ? 0L : parentFragment.getSendMonoForumPeerId(), false, null, false, null, null, 0, false, false);
            if (LaunchActivity.instance != null && LaunchActivity.instance.getBottomSheetTabs() != null && LaunchActivity.instance.getBottomSheetTabs().tryReopenTab(props) != null) {
                if (botCommandsMenuButton != null) {
                    botCommandsMenuButton.setOpened(false);
                }
                return;
            }
            if (LinkManager.isWebAppLink(botMenuWebViewUrl)) {
                Browser.Progress progress = new Browser.Progress();
                progress.onEnd(() -> {
                    AndroidUtilities.runOnUIThread(() -> {
                        if (botCommandsMenuButton != null) {
                            botCommandsMenuButton.setOpened(false);
                        }
                    });
                });
                Browser.openAsInternalIntent(getContext(), botMenuWebViewUrl, false, false, progress);
                return;
            }
            final TLRPC.User bot = MessagesController.getInstance(currentAccount).getUser(dialog_id);
            final String restriction_reason = MessagesController.getInstance(currentAccount).getRestrictionReason(bot == null ? null : bot.restriction_reason);
            if (!TextUtils.isEmpty(restriction_reason)) {
                MessagesController.getInstance(currentAccount).showCantOpenAlert(parentFragment, restriction_reason);
                return;
            }
//            if (AndroidUtilities.isTablet() || true) {
                BotWebViewSheet webViewSheet = new BotWebViewSheet(getContext(), resourcesProvider);
                webViewSheet.setDefaultFullsize(false);
                webViewSheet.setNeedsContext(true);
                webViewSheet.setParentActivity(parentActivity);
                webViewSheet.requestWebView(parentFragment, props);
                webViewSheet.show();

                if (botCommandsMenuButton != null) {
                    botCommandsMenuButton.setOpened(false);
                }
//            } else {
//                if (parentFragment != null && parentFragment.getParentActivity() != null) {
//                    BotWebViewAttachedSheet sheet = parentFragment.createBotViewer();
//                    sheet.setDefaultFullsize(false);
//                    sheet.setNeedsContext(false);
//                    sheet.setParentActivity(parentFragment.getParentActivity());
//                    sheet.requestWebView(parentFragment, props);
//                    sheet.show();
//
//                    if (botCommandsMenuButton != null) {
//                        botCommandsMenuButton.setOpened(false);
//                    }
//                }
//            }
        };

        if (SharedPrefsHelper.isWebViewConfirmShown(currentAccount, dialog_id) || MessagesController.getInstance(currentAccount).whitelistedBots.contains(dialog_id)) {
            onRequestWebView.run();
        } else {
            AlertsCreator.createBotLaunchAlert(parentFragment, MessagesController.getInstance(currentAccount).getUser(dialog_id), () -> {
                onRequestWebView.run();
                SharedPrefsHelper.setWebViewConfirmShown(currentAccount, dialog_id, true);
            }, () -> {
                if (botCommandsMenuButton != null && !SharedPrefsHelper.isWebViewConfirmShown(currentAccount, dialog_id))  {
                    botCommandsMenuButton.setOpened(false);
                }
            });
        }
    }

    public void setBotWebViewButtonOffsetX(float offset) {
        emojiButton.setTranslationX(offset);
        if (messageEditText != null) {
            messageTextTranslationX = offset;
            updateMessageTextParams();
        }
        attachButton.setTranslationX(attachLayoutPaddingTranslationX + attachLayoutTranslationX + offset);
        audioVideoSendButton.setTranslationX(offset);
        if (botButton != null) {
            botButton.setTranslationX(offset);
        }
    }

    public void setComposeShadowAlpha(float alpha) {
        composeShadowAlpha = alpha;
        invalidate();
    }

    public ChatActivityBotWebViewButton getBotWebViewButton() {
        createBotWebViewButton();
        return botWebViewButton;
    }

    public ChatActivity getParentFragment() {
        return parentFragment;
    }

    private void checkBotMenu() {
        final boolean shouldBeExpanded = (messageEditText == null || TextUtils.isEmpty(messageEditText.getText())) && !(keyboardVisible || waitingForKeyboardOpen || isPopupShowing());
        if (shouldBeExpanded) {
            createBotCommandsMenuButton();
        }
        if (botCommandsMenuButton != null) {
            boolean wasExpanded = botCommandsMenuButton.expanded;
            botCommandsMenuButton.setExpanded(shouldBeExpanded, true);
            if (wasExpanded != botCommandsMenuButton.expanded) {
                beginDelayedTransition();
            }
        }
    }

    public void forceSmoothKeyboard(boolean smoothKeyboard) {
        this.smoothKeyboard = smoothKeyboard && !AndroidUtilities.isInMultiwindow && (parentFragment == null || !parentFragment.isInBubbleMode());
    }

    protected void onLineCountChanged(int oldLineCount, int newLineCount) {

    }

    private void startLockTransition() {
        AnimatorSet animatorSet = new AnimatorSet();
        try {
            if (!NyaConfig.disableVibration.Bool()) performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
        } catch (Exception ignored) {}

        ObjectAnimator translate = ObjectAnimator.ofFloat(this, "lockAnimatedTranslation", startTranslation);
        translate.setStartDelay(100);
        translate.setDuration(350);
        ObjectAnimator snap = ObjectAnimator.ofFloat(this, "snapAnimationProgress", 1f);
        snap.setInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT);
        snap.setDuration(250);

        SharedConfig.removeLockRecordAudioVideoHint();

        animatorSet.playTogether(
                snap,
                translate,
                ObjectAnimator.ofFloat(this, "slideToCancelProgress", 1f).setDuration(200),
                ObjectAnimator.ofFloat(slideText, "cancelToProgress", 1f)
        );

        animatorSet.start();
    }

    public int getBackgroundTop() {
        int t = getTop();
        if (topView != null && topView.getVisibility() == View.VISIBLE) {
            t += topView.getLayoutParams().height;
        }
        return t;
    }

    @Override
    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
        boolean clip = child == topView || (child == textFieldContainer && topView != null && topView.getVisibility() == VISIBLE);
        if (clip) {
            final float separatorY = getMeasuredHeight() - animatorInputFieldHeight.getFactor();
            canvas.save();
            if (child == textFieldContainer) {
                canvas.clipRect(0, separatorY, getMeasuredWidth(), getMeasuredHeight());
            }
            if (child == topView) {
                canvas.clipRect(0, 0, getMeasuredWidth(), separatorY);
            }
        }
        if (isIosInputAppearance() && topViewBubbleDrawable != null && child == topView && child.getVisibility() == VISIBLE && child.getAlpha() > 0) {
            int translationY = (int) child.getTranslationY();
            topViewBubbleDrawable.setRadius(child.getHeight() / 2f);
            topViewBubbleDrawable.setBounds(child.getLeft(), child.getTop() + translationY, child.getRight(), child.getBottom() + translationY);
            topViewBubbleDrawable.setAlpha((int) (255 * child.getAlpha()));
            topViewBubbleDrawable.draw(canvas);
        }
        boolean result = super.drawChild(canvas, child, drawingTime);
        if (clip) {
            canvas.restore();
        }
        return result;
    }

    public boolean allowBlur = true;
    public boolean shouldDrawBackground = true;
    public boolean shouldDrawRecordedAudioPanelInParent;
    public boolean isStories;

    public boolean isIosButtonPlacement() {
        return NyaConfig.INSTANCE.getIosButtonPlacement().Bool();
    }

    public boolean isIosInputAppearance() {
        return NyaConfig.INSTANCE.getIosInputAppearance().Bool();
    }

    public boolean isCompactInputSize() {
        return NyaConfig.INSTANCE.getCompactInputSize().Bool() && isIosInputAppearance();
    }

    private BlurredBackgroundDrawableViewFactory glassBackgroundDrawableFactory;
    private BlurredBackgroundColorProviderThemed blurredBackgroundColorProvider;
    private BlurredBackgroundColorProviderThemed whiteSendBubbleColorProvider;
    private BlurredBackgroundColorProviderThemed accentSendBubbleColorProvider;
    private BlurredBackgroundColorProviderThemed currentSendBubbleColorProvider;
    private BlurredBackgroundDrawable fieldPillDrawable;
    private BlurredBackgroundDrawable attachBubbleDrawable;
    private BlurredBackgroundDrawable topViewBubbleDrawable;
    private BlurredBackgroundDrawable sendBubbleDrawable;
    private BlurredBackgroundDrawable voiceBubbleDrawable;
    private BlurredBackgroundDrawable doneBubbleDrawable;
    private BlurredBackgroundDrawable emojiBubbleDrawable;
    private BlurredBackgroundDrawable expandStickersBubbleDrawable;
    private BlurredBackgroundDrawable cancelBotBubbleDrawable;
    private BlurredBackgroundDrawable aiBubbleDrawable;
    private BlurredBackgroundDrawable richBubbleDrawable;
    private BlurredBackgroundDrawable recordDeleteBubbleDrawable;

    public void setInputBarGlassFactory(BlurredBackgroundDrawableViewFactory factory, BlurredBackgroundColorProviderThemed colorProvider, BlurredBackgroundColorProviderThemed whiteColorProvider) {
        setInputBarGlassFactory(factory, colorProvider, whiteColorProvider, null);
    }

    public void setInputBarGlassFactory(BlurredBackgroundDrawableViewFactory factory, BlurredBackgroundColorProviderThemed colorProvider, BlurredBackgroundColorProviderThemed whiteColorProvider, BlurredBackgroundColorProviderThemed accentColorProvider) {
        glassBackgroundDrawableFactory = factory;
        blurredBackgroundColorProvider = colorProvider;
        whiteSendBubbleColorProvider = whiteColorProvider;
        accentSendBubbleColorProvider = accentColorProvider;
        initIosBubbles(factory, colorProvider);
    }

    private BlurredBackgroundColorProviderThemed resolveSendBubbleColorProvider() {
        return ActionButtonStyle.resolveBubbleColorProvider(whiteSendBubbleColorProvider, blurredBackgroundColorProvider, accentSendBubbleColorProvider);
    }

    private void updateSendBubbleGlass() {
        if (sendBubbleDrawable == null || blurredBackgroundColorProvider == null) return;
        BlurredBackgroundColorProviderThemed target = resolveSendBubbleColorProvider();
        if (target != currentSendBubbleColorProvider) {
            currentSendBubbleColorProvider = target;
            sendBubbleDrawable.setColorProvider(target);
            if (voiceBubbleDrawable != null) {
                voiceBubbleDrawable.setColorProvider(target);
            }
            if (doneBubbleDrawable != null) {
                doneBubbleDrawable.setColorProvider(target);
            }
        }
        if (sendButton != null) {
            sendButton.setActionBubbleColorProvider(target);
        }
        if (doneButton != null) {
            doneButton.setActionBubbleColorProvider(target);
        }
    }

    private void initIosBubbles(BlurredBackgroundDrawableViewFactory factory, BlurredBackgroundColorProviderThemed colorProvider) {
        fieldPillDrawable = factory.create(messageEditTextContainer, colorProvider);
        fieldPillDrawable.setRadius(dp(IOS_BUBBLE_RADIUS_DP));
        attachBubbleDrawable = factory.create(messageEditTextContainer, colorProvider);
        attachBubbleDrawable.setRadius(dp(IOS_BUBBLE_RADIUS_DP));
        topViewBubbleDrawable = factory.create(this, colorProvider);
        topViewBubbleDrawable.setRadius(dp(IOS_BUBBLE_RADIUS_DP));
        sendBubbleDrawable = factory.create(sendButtonContainer, resolveSendBubbleColorProvider());
        sendBubbleDrawable.setRadius(dp(IOS_BUBBLE_RADIUS_DP));
        voiceBubbleDrawable = factory.create(sendButtonContainer, resolveSendBubbleColorProvider());
        voiceBubbleDrawable.setRadius(dp(IOS_BUBBLE_RADIUS_DP));
        doneBubbleDrawable = factory.create(textFieldContainer, resolveSendBubbleColorProvider());
        doneBubbleDrawable.setRadius(dp(IOS_BUBBLE_RADIUS_DP));
        emojiBubbleDrawable = factory.create(messageEditTextContainer, colorProvider);
        emojiBubbleDrawable.setRadius(dp(IOS_BUBBLE_RADIUS_DP));
        expandStickersBubbleDrawable = factory.create(sendButtonContainer, colorProvider);
        expandStickersBubbleDrawable.setRadius(dp(IOS_BUBBLE_RADIUS_DP));
        cancelBotBubbleDrawable = factory.create(sendButtonContainer, colorProvider);
        cancelBotBubbleDrawable.setRadius(dp(IOS_BUBBLE_RADIUS_DP));
        aiBubbleDrawable = factory.create(textFieldContainer, colorProvider);
        aiBubbleDrawable.setRadius(dp(IOS_BUBBLE_RADIUS_DP));
        richBubbleDrawable = factory.create(textFieldContainer, colorProvider);
        richBubbleDrawable.setRadius(dp(IOS_BUBBLE_RADIUS_DP));
        initRecordDeleteBubble();
        if (doneButton != null) {
            doneButton.setActionBubbleColorProvider(resolveSendBubbleColorProvider());
        }
    }

    private void initRecordDeleteBubble() {
        if (recordDeleteBubbleDrawable == null && recordedAudioPanel != null && glassBackgroundDrawableFactory != null && blurredBackgroundColorProvider != null) {
            recordDeleteBubbleDrawable = glassBackgroundDrawableFactory.create(recordedAudioPanel, blurredBackgroundColorProvider);
            recordDeleteBubbleDrawable.setRadius(dp(IOS_BUBBLE_RADIUS_DP));
        }
    }

    Paint backgroundPaint = new Paint();
    private float composeShadowAlpha = 1f;
    private Rect blurBounds = new Rect();

    @Override
    protected void onDraw(Canvas canvas) {
        drawBackground(canvas, true);
        drawIosFieldPill(canvas);
    }

    public void drawBackground(Canvas canvas, boolean withComposeShadowDrawable) {
        if (!shouldDrawBackground) {
            return;
        }
        int top = animatedTop;
        top += Theme.chat_composeShadowDrawable.getIntrinsicHeight() * (1f - composeShadowAlpha);
        if (topView != null && topView.getVisibility() == View.VISIBLE) {
            top += (1f - getTopViewEnterProgress()) * topView.getLayoutParams().height;
        }
        int bottom = top + Theme.chat_composeShadowDrawable.getIntrinsicHeight();

        if (withComposeShadowDrawable) {
            Theme.chat_composeShadowDrawable.setAlpha((int) (composeShadowAlpha * 0xFF));
            Theme.chat_composeShadowDrawable.setBounds(0, top, getMeasuredWidth(), bottom);
            Theme.chat_composeShadowDrawable.draw(canvas);
        }

        if (allowBlur) {
            backgroundPaint.setColor(getThemedColor(Theme.key_chat_messagePanelBackground));
            if (SharedConfig.chatBlurEnabled() && sizeNotifierLayout != null) {
                blurBounds.set(0, bottom, getWidth(), getHeight());
                sizeNotifierLayout.drawBlurRect(canvas, getTop(), blurBounds, backgroundPaint, false);
            } else {
                canvas.drawRect(0, bottom, getWidth(), getHeight(), backgroundPaint);
            }
        } else {
            canvas.drawRect(0, bottom, getWidth(), getHeight(), getThemedPaint(Theme.key_paint_chatComposeBackground));
        }
    }

    private void drawIosFieldPill(Canvas canvas) {
        if (fieldPillDrawable == null || !isIosInputAppearance() || messageEditTextContainer == null || messageEditTextContainer.getWidth() == 0) {
            return;
        }
        View outsideLeftButton = (!isStories && isIosButtonPlacement() && attachButton != null) ? attachButton
                : (!isStories && !isIosButtonPlacement() && emojiButton != null) ? emojiButton : null;
        int pillLeft = (outsideLeftButton != null && outsideLeftButton.getVisibility() == VISIBLE && outsideLeftButton.getAlpha() > 0) ? outsideLeftButton.getRight() + dp(iosGapDp) : 0;
        if (pillLeft == 0 && !isStories && recordedAudioPanel != null && recordDeleteImageView != null
                && recordedAudioPanel.getVisibility() == VISIBLE && recordedAudioPanel.getAlpha() > 0) {
            pillLeft = recordDeleteImageView.getRight() + dp(iosGapDp);
        }
        canvas.save();
        canvas.translate(textFieldContainer.getLeft() + messageEditTextContainer.getLeft(), textFieldContainer.getTop() + messageEditTextContainer.getTop());
        fieldPillDrawable.setBounds(pillLeft, 0, messageEditTextContainer.getWidth() - dp(iosGapDp), messageEditTextContainer.getHeight());
        fieldPillDrawable.draw(canvas);
        canvas.restore();
    }

    private void drawIosBubble(Canvas canvas, BlurredBackgroundDrawable bubble, View view) {
        if (bubble == null || view.getVisibility() != VISIBLE || view.getAlpha() <= 0) {
            return;
        }
        bubble.setBounds(view.getLeft(), view.getTop(), view.getRight(), view.getBottom());
        bubble.setAlpha((int) (255 * view.getAlpha()));
        bubble.draw(canvas);
    }

    private void drawIosBubbleSquare(Canvas canvas, BlurredBackgroundDrawable bubble, View view) {
        if (bubble == null || view.getVisibility() != VISIBLE || view.getAlpha() <= 0) {
            return;
        }
        bubble.setBounds(view.getRight() - dp(DEFAULT_HEIGHT), view.getBottom() - dp(DEFAULT_HEIGHT), view.getRight(), view.getBottom());
        bubble.setAlpha((int) (255 * view.getAlpha()));
        bubble.draw(canvas);
    }

    public float getVisualHeight() {
        float top = animatedTop;
        if (topView != null && topView.getVisibility() == View.VISIBLE) {
            top += (1f - getTopViewEnterProgress()) * topView.getLayoutParams().height;
        }
        return getMeasuredHeight() - top;
    }


    private boolean dismissSendPreviewSent;
    private final Runnable dismissSendPreview = () -> {
        if (messageSendPreview != null) {
            messageSendPreview.dismiss(dismissSendPreviewSent);
            messageSendPreview = null;
        };
    };

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

    private boolean isInInput;
    private ActionBarPopupWindow menuPopupWindow;

    private boolean checkMenuPermissions() {
        return checkMenuPermissions(isInVideoMode());
    }

    private boolean checkMenuPermissions(boolean isVideoMode) {
        if (isVideoMode) {
            boolean hasAudio = parentActivity.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
            boolean hasVideo = parentActivity.checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
            if (!hasAudio || !hasVideo) {
                String[] permissions = new String[!hasAudio && !hasVideo ? 2 : 1];
                if (!hasAudio && !hasVideo) {
                    permissions[0] = Manifest.permission.RECORD_AUDIO;
                    permissions[1] = Manifest.permission.CAMERA;
                } else if (!hasAudio) {
                    permissions[0] = Manifest.permission.RECORD_AUDIO;
                } else {
                    permissions[0] = Manifest.permission.CAMERA;
                }
                parentActivity.requestPermissions(permissions, BasePermissionsActivity.REQUEST_CODE_VIDEO_MESSAGE);
                return false;
            }
        } else {
            if (parentActivity.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                parentActivity.requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 3);
                return false;
            }
        }
        return true;
    }

    @SuppressLint("ClickableViewAccessibility")
    private void onMenuClick(View view) {
        if (parentFragment == null || slowModeTimer > 0 || delegate == null) {
            return;
        }
        ActionBarPopupWindow.ActionBarPopupWindowLayout menuPopupLayout = new ActionBarPopupWindow.ActionBarPopupWindowLayout(parentActivity, R.drawable.popup_fixed_alert4, resourcesProvider);

        menuPopupLayout.setAnimationEnabled(false);
        menuPopupLayout.setOnTouchListener(new OnTouchListener() {

            private android.graphics.Rect popupRect = new android.graphics.Rect();

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                    if (menuPopupWindow != null && menuPopupWindow.isShowing()) {
                        v.getHitRect(popupRect);
                        if (!popupRect.contains((int) event.getX(), (int) event.getY())) {
                            menuPopupWindow.dismiss();
                        }
                    }
                }
                return false;
            }
        });

        int a = 0;

        ActionBarMenuSubItem cell = new ActionBarMenuSubItem(getContext(), true, false);
        int disableLinkPreviewStatus = delegate.getDisableLinkPreviewStatus();

        if (!isInInput) {
            cell.setTextAndIcon(getString(R.string.ChatAttachEnterMenuRecordAudio), R.drawable.input_mic);
            cell.setOnClickListener(v -> {
                if (menuPopupWindow != null && menuPopupWindow.isShowing()) {
                    menuPopupWindow.dismiss();
                }

                if (parentFragment != null) {
                    TLRPC.Chat chat = parentFragment.getCurrentChat();
                    if (chat != null && !ChatObject.canSendVoice(chat)) {
                        delegate.needShowMediaBanHint();
                        return;
                    }
                }

                isInVideoMode = false;
                if (checkMenuPermissions()) {
                    recordAudioVideoRunnable.run();
                    delegate.onSwitchRecordMode(isInVideoMode);
                    setRecordVideoButtonVisible(isInVideoMode, true);
                    if (!NyaConfig.disableVibration.Bool()) performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                    sendButtonVisible = true;
                    startLockTransition();
                }
            });
            cell.setMinimumWidth(AndroidUtilities.dp(196));
            menuPopupLayout.addView(cell, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 48, LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT, 0, 48 * a++, 0, 0));

            if (SharedConfig.inappCamera) {
                cell = new ActionBarMenuSubItem(getContext(), false, true);
                cell.setTextAndIcon(getString(R.string.ChatAttachEnterMenuRecordVideo), R.drawable.input_video);
                cell.setOnClickListener(v -> {
                    if (menuPopupWindow != null && menuPopupWindow.isShowing()) {
                        menuPopupWindow.dismiss();
                    }

                    if (parentFragment != null) {
                        TLRPC.Chat chat = parentFragment.getCurrentChat();
                        if (chat != null && !ChatObject.canSendRoundVideo(chat)) {
                            delegate.needShowMediaBanHint();
                            return;
                        }
                    }

                    if (checkMenuPermissions(true)) {
                        int cameraMode = NyaConfig.INSTANCE.getCameraInVideoMessages().Int();
                        if (cameraMode == 2) {
                            showCameraSelectionPopup(audioVideoButtonContainer, () -> {
                                pendingCameraFront = true;
                                proceedWithVideoRecording();
                            }, () -> {
                                pendingCameraFront = false;
                                proceedWithVideoRecording();
                            });
                            return;
                        }
                        isInVideoMode = true;
                        recordAudioVideoRunnable.run();
                        delegate.onSwitchRecordMode(isInVideoMode);
                        setRecordVideoButtonVisible(isInVideoMode, true);
                        if (!NyaConfig.disableVibration.Bool()) performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                        sendButtonVisible = true;
                        startLockTransition();
                    }
                });

                cell.setMinimumWidth(AndroidUtilities.dp(196));
                menuPopupLayout.addView(cell, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 48, LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT, 0, 48 * a++, 0, 0));
            }
        } else {
            long chatId = ChatsHelper.getChatId();
            String languageText = Translator.getInputTranslateLangForChat(chatId).toUpperCase();

            // LinkPreview toggle
            if (disableLinkPreviewStatus > 0) {
                cell.setTextAndIcon(disableLinkPreviewStatus != 1 ?
                        getString(R.string.ChatAttachEnterMenuEnableLinkPreview) :
                        getString(R.string.ChatAttachEnterMenuDisableLinkPreview), R.drawable.msg_link);
                final ActionBarMenuSubItem linkPreviewCell = cell;
                cell.setOnClickListener(v -> {
                    if (menuPopupWindow != null && menuPopupWindow.isShowing()) {
                        menuPopupWindow.dismiss();
                    }
                    delegate.toggleDisableLinkPreview();
                    messageWebPageSearch = delegate.getDisableLinkPreviewStatus() == 1;

                    linkPreviewCell.setTextAndIcon(delegate.getDisableLinkPreviewStatus() != 1 ?
                            getString(R.string.ChatAttachEnterMenuEnableLinkPreview) :
                            getString(R.string.ChatAttachEnterMenuDisableLinkPreview), R.drawable.msg_link);
                });
                cell.setMinimumWidth(AndroidUtilities.dp(196));
                menuPopupLayout.addView(cell, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 48, LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT, 0, 48 * a++, 0, 0));
                cell = new ActionBarMenuSubItem(getContext(), true, false);
            }

            // AI Translate
            if (LlmConfig.isLLMTranslatorAvailable() && !LlmConfig.llmIsDefaultProvider()) {
                cell.setTextAndIcon(getString(R.string.TranslateMessageLLM) + " (" + languageText + ")", R.drawable.magic_stick_solar);
                cell.setOnClickListener(v -> {
                    if (menuPopupWindow != null && menuPopupWindow.isShowing()) {
                        menuPopupWindow.dismiss();
                    }
                    translateComment(Translator.getInputTranslateLangLocaleForChat(chatId), Translator.providerLLMTranslator);
                });
                final ActionBarMenuSubItem llmTranslateCell = cell;
                cell.setOnLongClickListener(v -> {
                    Translator.showTargetLangSelect(llmTranslateCell, true, (locale) -> {
                        if (menuPopupWindow != null && menuPopupWindow.isShowing()) {
                            menuPopupWindow.dismiss();
                        }
                        translateComment(locale, Translator.providerLLMTranslator);
                        Translator.setInputTranslateLangForChat(chatId, TranslatorKt.getLocale2code(locale));
                        return Unit.INSTANCE;
                    });
                    return true;
                });
                cell.setMinimumWidth(AndroidUtilities.dp(196));
                menuPopupLayout.addView(cell, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 48, LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT, 0, 48 * a++, 0, 0));
                cell = new ActionBarMenuSubItem(getContext(), false, false);
            }

            // Normal Translate
            cell.setTextAndIcon(getString(R.string.TranslateMessage) + " (" + languageText + ")", LlmConfig.llmIsDefaultProvider() ? R.drawable.magic_stick_solar : R.drawable.ic_translate);
            cell.setOnClickListener(v -> {
                if (menuPopupWindow != null && menuPopupWindow.isShowing()) {
                    menuPopupWindow.dismiss();
                }
                translateComment(Translator.getInputTranslateLangLocaleForChat(chatId));
            });
            final ActionBarMenuSubItem translateCell = cell;
            cell.setOnLongClickListener(v -> {
                Translator.showTargetLangSelect(translateCell, true, (locale) -> {
                    if (menuPopupWindow != null && menuPopupWindow.isShowing()) {
                        menuPopupWindow.dismiss();
                    }
                    translateComment(locale);
                    Translator.setInputTranslateLangForChat(chatId, TranslatorKt.getLocale2code(locale));
                    return Unit.INSTANCE;
                });
                return true;
            });
            cell.setMinimumWidth(AndroidUtilities.dp(196));
            menuPopupLayout.addView(cell, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 48, LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT, 0, 48 * a++, 0, 0));

            // Replace Text
            cell = new ActionBarMenuSubItem(getContext(), false, false);
            cell.setTextAndIcon(getString(R.string.ReplaceText), R.drawable.msg_edit);
            cell.setOnClickListener(v -> {
                if (menuPopupWindow != null && menuPopupWindow.isShowing()) {
                    menuPopupWindow.dismiss();
                }
                showReplace();
            });
            cell.setMinimumWidth(AndroidUtilities.dp(196));
            menuPopupLayout.addView(cell, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 48, LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT, 0, 48 * a++, 0, 0));

            // Attach
            if (attachButton != null) {
                cell = new ActionBarMenuSubItem(getContext(), false, true);
                cell.setTextAndIcon(getString(R.string.AttachMenu), R.drawable.input_attach);
                cell.setOnClickListener(v -> {
                    if (menuPopupWindow != null && menuPopupWindow.isShowing()) {
                        menuPopupWindow.dismiss();
                    }
                    if (adjustPanLayoutHelper != null && adjustPanLayoutHelper.animationInProgress()) {
                        return;
                    }
                    if (delegate != null) {
                        delegate.didPressAttachButton();
                    }
                });
                cell.setMinimumWidth(AndroidUtilities.dp(196));
                menuPopupLayout.addView(cell, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 48, LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT, 0, 48 * a++, 0, 0));
            }
        }

        menuPopupLayout.updateRadialSelectors();

        menuPopupWindow = new ActionBarPopupWindow(menuPopupLayout, LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT);
        menuPopupWindow.setAnimationEnabled(false);
        menuPopupWindow.setAnimationStyle(R.style.PopupContextAnimation2);
        menuPopupWindow.setOutsideTouchable(true);
        menuPopupWindow.setClippingEnabled(true);
        menuPopupWindow.setInputMethodMode(ActionBarPopupWindow.INPUT_METHOD_NOT_NEEDED);
        menuPopupWindow.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_UNSPECIFIED);
        menuPopupWindow.getContentView().setFocusableInTouchMode(true);

        if (delegate != null) {
            delegate.onSendLongClick();
        }

        menuPopupLayout.measure(MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(1000), MeasureSpec.AT_MOST), MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(1000), MeasureSpec.AT_MOST));
        menuPopupWindow.setFocusable(true);
        int[] location = new int[2];
        view.getLocationInWindow(location);
        int y;
        if (keyboardVisible && ChatActivityEnterView.this.getMeasuredHeight() > AndroidUtilities.dp(topView != null && topView.getVisibility() == VISIBLE ? 48 + 58 : 58)) {
            y = location[1] + view.getMeasuredHeight();
        } else {
            y = location[1] - menuPopupLayout.getMeasuredHeight() - AndroidUtilities.dp(2);
        }
        y += AndroidUtilities.dp(48);
        int x = location[0] + view.getMeasuredWidth() - menuPopupLayout.getMeasuredWidth() + AndroidUtilities.dp(8);
        if (isInInput) {
            x += view.getMeasuredWidth();
        }
        menuPopupWindow.showAtLocation(view, Gravity.LEFT | Gravity.TOP, x, y);
        menuPopupWindow.dimBehind();

    }

    private ActionBarMenuSubItem actionScheduleButton;
    private boolean onSendLongClick(View view) {
        if (isInScheduleMode() || parentFragment != null && parentFragment.getChatMode() == ChatActivity.MODE_QUICK_REPLIES || animatorEphemeralMessageVisibility.getValue()) {
            return false;
        }
        boolean sendWithoutSoundNax = NyaConfig.INSTANCE.getSilentMessageByDefault().Bool();
        if (isStories || (messageEditText == null || TextUtils.isEmpty(messageEditText.getText())) && parentFragment != null && parentFragment.messagePreviewParams != null && parentFragment.messagePreviewParams.forwardMessages != null && parentFragment.messagePreviewParams.forwardMessages.messages != null && !parentFragment.messagePreviewParams.forwardMessages.messages.isEmpty()) {

            boolean self = parentFragment != null && UserObject.isUserSelf(parentFragment.getCurrentUser());

            if (sendPopupLayout == null) {
                sendPopupLayout = new ActionBarPopupWindow.ActionBarPopupWindowLayout(parentActivity, resourcesProvider);
                sendPopupLayout.setAnimationEnabled(false);
                sendPopupLayout.setOnTouchListener(new OnTouchListener() {

                    private android.graphics.Rect popupRect = new android.graphics.Rect();

                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                            if (sendPopupWindow != null && sendPopupWindow.isShowing()) {
                                v.getHitRect(popupRect);
                                if (!popupRect.contains((int) event.getX(), (int) event.getY())) {
                                    sendPopupWindow.dismiss();
                                }
                            }
                        }
                        return false;
                    }
                });
                sendPopupLayout.setDispatchKeyEventListener(keyEvent -> {
                    if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_BACK && keyEvent.getRepeatCount() == 0 && sendPopupWindow != null && sendPopupWindow.isShowing()) {
                        sendPopupWindow.dismiss();
                    }
                });
                sendPopupLayout.setShownFromBottom(false);

                boolean scheduleButtonValue = parentFragment != null && parentFragment.canScheduleMessage();
                boolean sendWithoutSoundButtonValue = !(self || slowModeTimer > 0 && !isInScheduleMode());
                if (scheduleButtonValue) {
                    actionScheduleButton = new ActionBarMenuSubItem(getContext(), true, !sendWithoutSoundButtonValue, resourcesProvider);
                    if (self) {
                        actionScheduleButton.setTextAndIcon(LocaleController.getString(R.string.SetReminder), R.drawable.msg_calendar2);
                    } else {
                        actionScheduleButton.setTextAndIcon(LocaleController.getString(R.string.ScheduleMessage), R.drawable.msg_calendar2);
                    }
                    actionScheduleButton.setMinimumWidth(dp(196));
                    actionScheduleButton.setOnClickListener(v -> {
                        if (sendPopupWindow != null && sendPopupWindow.isShowing()) {
                            sendPopupWindow.dismiss();
                        }
                        AlertsCreator.createScheduleDatePickerDialog(parentActivity, parentFragment.getDialogId(), new AlertsCreator.ScheduleDatePickerDelegate() {
                            @Override
                            public void didSelectDate(boolean notify, int scheduleDate, int scheduleRepeatPeriod) {
                                sendMessageInternal(notify, scheduleDate, 0, 0, true);
                            }
                        }, resourcesProvider);
                    });
                    sendPopupLayout.addView(actionScheduleButton, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, DEFAULT_HEIGHT));
                    SharedConfig.removeScheduledHint();
                    if (!self && dialog_id > 0) {
                        sendWhenOnlineButton = new ActionBarMenuSubItem(getContext(), true, !sendWithoutSoundButtonValue, resourcesProvider);
                        sendWhenOnlineButton.setTextAndIcon(LocaleController.getString(R.string.SendWhenOnline), R.drawable.msg_online);
                        sendWhenOnlineButton.setMinimumWidth(dp(196));
                        sendWhenOnlineButton.setOnClickListener(v -> {
                            if (sendPopupWindow != null && sendPopupWindow.isShowing()) {
                                sendPopupWindow.dismiss();
                            }
                            sendMessageInternal(true, 0x7FFFFFFE, 0, 0, true);
                        });
                        sendPopupLayout.addView(sendWhenOnlineButton, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, DEFAULT_HEIGHT));
                    }
                }
                if (sendWithoutSoundButtonValue) {
                    ActionBarMenuSubItem sendWithoutSoundButton = new ActionBarMenuSubItem(getContext(), !scheduleButtonValue, true, resourcesProvider);
                    sendWithoutSoundButton.setTextAndIcon(sendWithoutSoundNax ? getString(R.string.SendWithSound) : getString(R.string.SendWithoutSound), sendWithoutSoundNax ? R.drawable.input_notify_on : R.drawable.input_notify_off);
                    sendWithoutSoundButton.setMinimumWidth(dp(196));
                    sendWithoutSoundButton.setOnClickListener(v -> {
                        if (sendPopupWindow != null && sendPopupWindow.isShowing()) {
                            sendPopupWindow.dismiss();
                        }
                        sendMessageInternal(sendWithoutSoundNax, 0, 0, 0, true);
                    });
                    sendPopupLayout.addView(sendWithoutSoundButton, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, DEFAULT_HEIGHT));
                }
                        runningAnimation2 = new AnimatorSet();
                        ArrayList<Animator> animators = new ArrayList<>();
                        animators.add(ObjectAnimator.ofFloat(attachLayout, ATTACH_LAYOUT_ALPHA, 0.0f));
                        animators.add(ObjectAnimator.ofFloat(attachLayout, View.SCALE_X, 0.5f));
                        if (attachButtonAnimator != null) {
                            attachButtonAnimator.cancel();
                            attachButtonAnimator = null;
                        }
                        if (sideButtons != null) {
                            sideButtons.showButton(ChatActivitySideControlsButtonsLayout.BUTTON_ATTACH, captionNearAttach && !recordShown, true);
                            if (attachButton != null) {
                                animators.add(ObjectAnimator.ofFloat(attachButton, View.ALPHA, attachButtonAlpha = attachInInput ? 1.0f : 0.0f));
                                animators.add(ObjectAnimator.ofFloat(attachButton, View.SCALE_X, attachInInput ? 1.0f : 0.5f));
                                animators.add(ObjectAnimator.ofFloat(attachButton, View.SCALE_Y, attachInInput ? 1.0f : 0.5f));
                            }
                        } else if (attachButton != null) {
                            animators.add(ObjectAnimator.ofFloat(attachButton, View.ALPHA, attachButtonAlpha = 0.0f));
                            animators.add(ObjectAnimator.ofFloat(attachButton, View.SCALE_X, 0.5f));
                            animators.add(ObjectAnimator.ofFloat(attachButton, View.SCALE_Y, 0.5f));
                        }
                        boolean hasScheduled = delegate != null && delegate.hasScheduledMessages();
                        scheduleButtonHidden = true;
                        if (scheduledButton != null) {
                            scheduledButton.setScaleY(1.0f);
                            if (hasScheduled) {
                                scheduledButton.setTag(null);
                                animators.add(ObjectAnimator.ofFloat(scheduledButton, View.ALPHA, 0.0f));
                                animators.add(ObjectAnimator.ofFloat(scheduledButton, View.SCALE_X, 0.0f));
                                animators.add(animateScheduledTranslationX(0));
                            } else {
                                scheduledButton.setAlpha(0.0f);
                                scheduledButton.setScaleX(0.0f);
                                scheduledButton.setTranslationX(0);
                            }
                        }
                        runningAnimation2.playTogether(animators);
                        runningAnimation2.setDuration(100);
                        runningAnimation2.addListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                if (animation.equals(runningAnimation2)) {
                                    attachLayout.setVisibility(GONE);
                                    if (hasScheduled && scheduledButton != null) {
                                        scheduledButton.setVisibility(GONE);
                                    }
                                    runningAnimation2 = null;
                                }
                            }

                        if ((!NyaConfig.useChatAttachMediaMenu.Bool() || isStories) && !(isStories && isIosButtonPlacement())) {
                            runningAnimation2 = new AnimatorSet();
                            ArrayList<Animator> animators = new ArrayList<>();
                            animators.add(ObjectAnimator.ofFloat(attachLayout, ATTACH_LAYOUT_ALPHA, 0.0f));
                            animators.add(ObjectAnimator.ofFloat(attachLayout, View.SCALE_X, 0.5f));
                            if (attachButtonAnimator != null) {
                                attachButtonAnimator.cancel();
                                attachButtonAnimator = null;
                            }
                            if (sideButtons != null && (!NyaConfig.useChatAttachMediaMenu.Bool() || isStories)) {
                                sideButtons.showButton(ChatActivitySideControlsButtonsLayout.BUTTON_ATTACH, captionNearAttach, true);
                                if (attachButton != null) {
                                    animators.add(ObjectAnimator.ofFloat(attachButton, View.ALPHA, attachButtonAlpha = captionNearAttach ? 0.0f : 1.0f));
                                    animators.add(ObjectAnimator.ofFloat(attachButton, View.SCALE_X, captionNearAttach ? 0.5f : 1.0f));
                                    animators.add(ObjectAnimator.ofFloat(attachButton, View.SCALE_Y, captionNearAttach ? 0.5f : 1.0f));
                                }
                            } else if (attachButton != null && (!NyaConfig.useChatAttachMediaMenu.Bool() || isStories)) {
                                animators.add(ObjectAnimator.ofFloat(attachButton, View.ALPHA, attachButtonAlpha = 0.0f));
                                animators.add(ObjectAnimator.ofFloat(attachButton, View.SCALE_X, 0.5f));
                                animators.add(ObjectAnimator.ofFloat(attachButton, View.SCALE_Y, 0.5f));
                            }
                            boolean hasScheduled = delegate != null && delegate.hasScheduledMessages();
                            scheduleButtonHidden = true;
                            if (scheduledButton != null) {
                                scheduledButton.setScaleY(1.0f);
                                if (hasScheduled) {
                                    scheduledButton.setTag(null);
                                    animators.add(ObjectAnimator.ofFloat(scheduledButton, View.ALPHA, 0.0f));
                                    animators.add(ObjectAnimator.ofFloat(scheduledButton, View.SCALE_X, 0.0f));
                                    animators.add(animateScheduledTranslationX(0));
                                } else {
                                    scheduledButton.setAlpha(0.0f);
                                    scheduledButton.setScaleX(0.0f);
                                    scheduledButton.setTranslationX(0);
                                }
                            }
                            runningAnimation2.playTogether(animators);
                            runningAnimation2.setDuration(100);
                            runningAnimation2.addListener(new AnimatorListenerAdapter() {
                                @Override
                                public void onAnimationEnd(Animator animation) {
                                    if (animation.equals(runningAnimation2)) {
                                        attachLayout.setVisibility(GONE);
                                        if (hasScheduled && scheduledButton != null) {
                                            scheduledButton.setVisibility(GONE);
                                        }
                                        runningAnimation2 = null;
                                    }
                                }

                                @Override
                                public void onAnimationCancel(Animator animation) {
                                    if (animation.equals(runningAnimation2)) {
                                        runningAnimation2 = null;
                                    }
                                }
                            });
                            runningAnimation2.start();
                            updateFieldRight(0);
                            if (delegate != null && getVisibility() == VISIBLE) {
                                delegate.onAttachButtonHidden();
                            }
                        } else if (!(isStories && isIosButtonPlacement())) {
                            checkAttachButton(true, 150);
                        }
                    }

                    runningAnimation = new AnimatorSet();

                    ArrayList<Animator> animators = new ArrayList<>();
                    if (botButton != null && NyaConfig.useChatAttachMediaMenu.Bool() && !isStories && botButton.getVisibility() == VISIBLE) {
                        animators.add(ObjectAnimator.ofFloat(botButton, View.SCALE_X, 0.1f));
                        animators.add(ObjectAnimator.ofFloat(botButton, View.SCALE_Y, 0.1f));
                        animators.add(ObjectAnimator.ofFloat(botButton, View.ALPHA, 0.0f));
                    }
                    if (audioVideoButtonContainer.getVisibility() == VISIBLE) {
                        animators.add(ObjectAnimator.ofFloat(audioVideoSendButton, View.SCALE_X, 0.1f));
                        animators.add(ObjectAnimator.ofFloat(audioVideoSendButton, View.SCALE_Y, 0.1f));
                        animators.add(ObjectAnimator.ofFloat(audioVideoSendButton, View.ALPHA, 0.0f));
                    }
                    if (expandStickersButton != null && expandStickersButton.getVisibility() == VISIBLE) {
                        animators.add(ObjectAnimator.ofFloat(expandStickersButton, View.SCALE_X, 0.1f));
                        animators.add(ObjectAnimator.ofFloat(expandStickersButton, View.SCALE_Y, 0.1f));
                        animators.add(ObjectAnimator.ofFloat(expandStickersButton, View.ALPHA, 0.0f));
                    }
                    if (slowModeButton.getVisibility() == VISIBLE) {
                        animators.add(ObjectAnimator.ofFloat(slowModeButton, View.SCALE_X, 0.1f));
                        animators.add(ObjectAnimator.ofFloat(slowModeButton, View.SCALE_Y, 0.1f));
                        animators.add(ObjectAnimator.ofFloat(slowModeButton, View.ALPHA, 0.0f));
                    }
                    if (showBotButton) {
                        animators.add(animateSendButton(false));
                    } else if (showSendButton) {
                        animators.add(ObjectAnimator.ofFloat(cancelBotButton, View.SCALE_X, 0.1f));
                        animators.add(ObjectAnimator.ofFloat(cancelBotButton, View.SCALE_Y, 0.1f));
                        animators.add(ObjectAnimator.ofFloat(cancelBotButton, View.ALPHA, 0.0f));
                    }
                    if (caption != null) {
                        runningAnimationType = 3;
                        animators.add(ObjectAnimator.ofFloat(cancelBotButton, View.SCALE_X, 1.0f));
                        animators.add(ObjectAnimator.ofFloat(cancelBotButton, View.SCALE_Y, 1.0f));
                        animators.add(ObjectAnimator.ofFloat(cancelBotButton, View.ALPHA, 1.0f));
                        cancelBotButton.setVisibility(VISIBLE);
                    } else {
                        runningAnimationType = 1;
                        animators.add(animateSendButton(true));
                        getSendButtonInternal().setVisibility(VISIBLE);
                    }

                    runningAnimation.playTogether(animators);
                    runningAnimation.setDuration(220);
                    runningAnimation.setInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT);
                    runningAnimation.addListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            if (animation.equals(runningAnimation)) {
                                if (NyaConfig.useChatAttachMediaMenu.Bool() && !isStories && botButton != null) {
                                    botButton.setVisibility(View.GONE);
                                    updateFieldRight(1);
                                }
                                if (caption != null) {
                                    cancelBotButton.setVisibility(VISIBLE);
                                    getSendButtonInternal().setVisibility(GONE);
                                } else {
                                    getSendButtonInternal().setVisibility(VISIBLE);
                                    cancelBotButton.setVisibility(GONE);
                                }
                                audioVideoButtonContainer.setVisibility(GONE);
                                if (expandStickersButton != null) {
                                    expandStickersButton.setVisibility(GONE);
                                }
                                setSlowModeButtonVisible(false);
                                runningAnimation = null;
                                runningAnimationType = 0;
                            }
                        }

                        @Override
                        public void onAnimationCancel(Animator animation) {
                            if (animation.equals(runningAnimation)) {
                                runningAnimation = null;
                            }
                        }
                    });
                    runningAnimation.start();
                } else {
                    audioVideoSendButton.setScaleX(0.1f);
                    audioVideoSendButton.setScaleY(0.1f);
                    audioVideoSendButton.setAlpha(0.0f);
                    audioVideoButtonContainer.setVisibility(GONE);
                    if (slowModeButton.getVisibility() == VISIBLE) {
                        slowModeButton.setScaleX(0.1f);
                        slowModeButton.setScaleY(0.1f);
                        slowModeButton.setAlpha(0.0f);
                        setSlowModeButtonVisible(false);
                    }
                    if (caption != null) {
                        getSendButtonInternal().setScaleX(0.1f);
                        getSendButtonInternal().setScaleY(0.1f);
                        getSendButtonInternal().setAlpha(0.0f);
                        getSendButtonInternal().setVisibility(GONE);
                        cancelBotButton.setScaleX(1.0f);
                        cancelBotButton.setScaleY(1.0f);
                        cancelBotButton.setAlpha(1.0f);
                        cancelBotButton.setVisibility(VISIBLE);
                    } else {
                        cancelBotButton.setScaleX(0.1f);
                        cancelBotButton.setScaleY(0.1f);
                        cancelBotButton.setAlpha(0.0f);
                        getSendButtonInternal().setVisibility(VISIBLE);
                        getSendButtonInternal().setScaleX(1.0f);
                        getSendButtonInternal().setScaleY(1.0f);
                        getSendButtonInternal().setAlpha(1.0f);
                        cancelBotButton.setVisibility(GONE);
                    }
                    if (expandStickersButton != null && expandStickersButton.getVisibility() == VISIBLE) {
                        expandStickersButton.setScaleX(0.1f);
                        expandStickersButton.setScaleY(0.1f);
                        expandStickersButton.setAlpha(0.0f);
                        expandStickersButton.setVisibility(GONE);
                    }
                    if (attachLayout != null) {
        updateColorsExtra();
        if (messageSendPreview != null) {
            messageSendPreview.updateColors();
        }

        updateRecordedDeleteIconColors();
        if (recordCircle != null) {
            recordCircle.updateColors();
        }
        if (recordDot != null) {
            recordDot.updateColors();
        }
        if (slideText != null) {
            slideText.updateColors();
        }
        if (recordTimerView != null) {
            recordTimerView.updateColors();
        }
        if (videoTimelineView != null) {
            videoTimelineView.updateColors();
        }

        if (captionLimitView != null && messageEditText != null) {
            if (codePointCount - currentLimit < 0) {
                captionLimitView.setTextColor(getThemedColor(Theme.key_text_RedRegular));
            } else {
                captionLimitView.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteGrayText));
            }
        }
        int color = getThemedColor(Theme.key_chat_messagePanelVoicePressed);
        int defaultAlpha = Color.alpha(color);
        if (doneCheckDrawable != null) {
            final int doneColor = ColorUtils.setAlphaComponent(color, (int) (defaultAlpha * (0.58f + 0.42f * doneButtonEnabledProgress)));
            if (doneCheckDrawable != lastDoneCheckDrawable || doneColor != lastDoneCheckColor) {
                lastDoneCheckDrawable = doneCheckDrawable;
                lastDoneCheckColor = doneColor;
                doneCheckDrawable.setColorFilter(new PorterDuffColorFilter(doneColor, PorterDuff.Mode.MULTIPLY));
            }
        }
        if (botCommandsMenuContainer != null) {
            botCommandsMenuContainer.updateColors();
        }
        if (botKeyboardView != null) {
            botKeyboardView.updateColors();
        }
        // emojiButton / deleteRichDraftButton / sendOutlineView icon colors are ThemeBind bindings
        final int audioVideoColor = audioVideoButtonContainerForbidden ? getThemedColor(Theme.key_glass_defaultIcon) : Color.WHITE;
        if (audioVideoSendButton != null && (!audioVideoColorApplied || audioVideoColor != lastAudioVideoColor)) {
            audioVideoColorApplied = true;
            lastAudioVideoColor = audioVideoColor;
            audioVideoSendButton.setColorFilter(new PorterDuffColorFilter(audioVideoColor, PorterDuff.Mode.SRC_IN));
        }
        final int selectorColor = getThemedColor(Theme.key_listSelector);
        if (!emojiSelectorApplied || selectorColor != lastEmojiSelectorColor) {
            emojiSelectorApplied = true;
            lastEmojiSelectorColor = selectorColor;
            if (emojiButton != null) {
                emojiButton.setBackground(Theme.createSelectorDrawable(selectorColor));
            }
            if (deleteRichDraftButton != null) {
                deleteRichDraftButton.setBackground(Theme.createInsetRoundRectDrawable(selectorColor, dp(19), dp(1), dp(3)));
            }
        }
    }

    private int lastDoneCheckColor, lastAudioVideoColor, lastEmojiSelectorColor;
    private Drawable lastDoneCheckDrawable;
    private boolean audioVideoColorApplied, emojiSelectorApplied;
    @Override
    public void addColorKeys(HashSet<Integer> colors) {
        colors.add(Theme.key_chat_messagePanelBackground);
        colors.add(Theme.key_chat_messagePanelVoiceDelete);
        colors.add(Theme.key_text_RedRegular);
        colors.add(Theme.key_windowBackgroundWhiteGrayText);
        colors.add(Theme.key_chat_messagePanelVoicePressed);
        colors.add(Theme.key_chat_emojiPanelNewTrending);
        colors.add(Theme.key_chat_messagePanelSend);
    }

    private void updateColorsExtra() {
        try {
            if (messageEditText != null) {
                ThemeBind.editHintColor(messageEditText, Theme.key_chat_messagePanelHint, resourcesProvider);
                ThemeBind.cursorColor(messageEditText, Theme.key_chat_messagePanelCursor, resourcesProvider);
                ThemeBind.editHandlesColor(messageEditText, Theme.key_chat_TextSelectionCursor, resourcesProvider);
            }
            if (dotPaint != null) {
                dotPaint.setColor(getThemedColor(Theme.key_chat_emojiPanelNewTrending));
            }
            if (slowModeButton != null) {
                slowModeButton.setTextColor(getThemedColor(Theme.key_glass_defaultIcon));
            }
            if (stickersArrow != null) {
                stickersArrow.setColor(getThemedColor(Theme.key_glass_defaultIcon));
            }
            if (sendButton != null && sendButtonBackgroundColor != 0 && sendButton.getBackground() != null) {
                final int color = slowModeTimer == Integer.MAX_VALUE && !isSlowModeIgnored() ? getThemedColor(Theme.key_glass_defaultIcon) : getThemedColor(Theme.key_chat_messagePanelSend);
                if (color != sendButtonBackgroundColor) {
                    sendButtonBackgroundColor = color;
                    Theme.setSelectorDrawableColor(sendButton.getBackground(), Color.argb(24, Color.red(color), Color.green(color), Color.blue(color)), true);
                }
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    private void updateRecordedDeleteIconColors() {
        int dotColor = getThemedColor(Theme.key_chat_recordedVoiceDot);
        int background = getThemedColor(Theme.key_chat_messagePanelBackground);
        int greyColor = getThemedColor(Theme.key_chat_messagePanelVoiceDelete);

        if (recordDeleteImageView != null) {
            recordDeleteImageView.setLayerColor("Cup Red", dotColor);
            recordDeleteImageView.setLayerColor("Box Red", dotColor);
            recordDeleteImageView.setLayerColor("Cup Grey", greyColor);
            recordDeleteImageView.setLayerColor("Box Grey", greyColor);
            recordDeleteImageView.setLayerColor("Box_Grey 2", greyColor);
            recordDeleteImageView.setLayerColor("Line 1", greyColor);
            recordDeleteImageView.setLayerColor("Line 2", greyColor);
            recordDeleteImageView.setLayerColor("Line 3", greyColor);
            recordDeleteImageView.setLayerColor("Line 1 Dup", background);
            recordDeleteImageView.setLayerColor("Line 2 Dup", background);
            recordDeleteImageView.setLayerColor("Line 3 Dup", background);
        }
    }

    public void setFieldText(CharSequence text) {
        setFieldText(text, true, false);
    }

    public void setFieldText(CharSequence text, boolean ignoreChange) {
        setFieldText(text, ignoreChange, false);
    }

    public void setFieldText(CharSequence text, boolean ignoreChange, boolean fromDraft) {
        if (messageEditText == null) {
            return;
        }
        ignoreTextChange = ignoreChange;
        if (TextUtils.isEmpty(text)) {
            messageEditText.setTextWithoutInputAnimation(text);
        } else {
            messageEditText.setText(text);
        }
        messageEditText.invalidateQuotes(true);
        messageEditText.setSelection(messageEditText.getText().length());
        ignoreTextChange = false;
        if (ignoreChange && delegate != null) {
            delegate.onTextChanged(messageEditText.getText(), true, fromDraft);
        }
    }

    public void setVoiceDraft(MediaDataController.DraftVoice draft) {
        if (draft == null) return;
        voiceOnce = draft.once;
        if (controlsView != null) {
            controlsView.periodDrawable.setValue(1, voiceOnce, true);
        }
        TL_stories.StoryItem storyItem = delegate != null ? delegate.getReplyToStory() : null;
        MediaController.getInstance().prepareResumedRecording(currentAccount, draft, dialog_id, replyingMessageObject, getThreadMessage(), storyItem, recordingGuid, parentFragment != null ? parentFragment.getMessageChatSendParams() : null, getSendMonoForumPeerId(), getSendMessageSuggestionParams());
    }

    public void setSelection(int start) {
        if (messageEditText == null) {
            return;
        }
        messageEditText.setSelection(start, messageEditText.length());
    }

    /** Synchronizes the video timeline with an external trim control. */
    public void setVideoTimelineTrim(float start, float end) {
        if (videoTimelineView != null) {
            videoTimelineView.setTrimProgress(start, end);
        }
    }

    /** Selects camera-preview-driven animation ticks for the new round-video recorder. */
    public void setRoundVideoUiFrameClockActive(boolean active) {
        if (roundVideoUiFrameClockActive == active) return;
        roundVideoUiFrameClockActive = active;
        if (recordTimerView != null) recordTimerView.setExternalFrameClock(active);
        if (recordDot != null) recordDot.setExternalFrameClock(active);
        if (slideText != null) slideText.setExternalFrameClock(active);
    }

    /** Advances recording UI on the frame consumed by the camera preview TextureView. */
    public void onRoundVideoUiFrame(long durationMs) {
        if (!roundVideoUiFrameClockActive) return;
        millisecondsRecorded = durationMs;
        if (recordTimerView != null) recordTimerView.onExternalFrame(durationMs);
        if (recordDot != null) recordDot.onExternalFrame(durationMs);
        if (slideText != null) slideText.onExternalFrame();
    }

    public int getCursorPosition() {
        if (messageEditText == null) {
            return 0;
        }
        return messageEditText.getSelectionStart();
    }

    public int getSelectionLength() {
        if (messageEditText == null) {
            return 0;
        }
        try {
            return messageEditText.getSelectionEnd() - messageEditText.getSelectionStart();
        } catch (Exception e) {
            FileLog.e(e);
        }
        return 0;
    }

    public void replaceWithText(int start, int len, CharSequence text, boolean parseEmoji) {
        if (messageEditText == null) {
            return;
        }
        try {
            SpannableStringBuilder builder = new SpannableStringBuilder(messageEditText.getText());
            builder.replace(start, start + len, text);
            if (parseEmoji) {
                Emoji.replaceEmoji(builder, messageEditText.getPaint().getFontMetricsInt(), false, null);
            }
            messageEditText.setText(builder);
            messageEditText.setSelection(start + text.length());
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    public void setFieldFocused() {
        AccessibilityManager am = (AccessibilityManager) parentActivity.getSystemService(Context.ACCESSIBILITY_SERVICE);
        if (messageEditText != null && !am.isTouchExplorationEnabled()) {
            try {
                messageEditText.requestFocus();
            } catch (Exception e) {
                FileLog.e(e);
            }
        }
    }

    public void setFieldFocused(boolean focus) {
        AccessibilityManager am = (AccessibilityManager) parentActivity.getSystemService(Context.ACCESSIBILITY_SERVICE);
        if (messageEditText == null || am.isTouchExplorationEnabled()) {
            return;
        }
        if (focus && BaseFragment.hasSheets(parentFragment)) {
            focus = false;
        }
        if (focus) {
            if (searchingType == 0 && !messageEditText.isFocused()/* && (botWebViewMenuContainer == null || botWebViewMenuContainer.getVisibility() == View.GONE)*/) {
                AndroidUtilities.runOnUIThread(focusRunnable = () -> {
                    focusRunnable = null;
                    boolean allowFocus;
                    if (AndroidUtilities.isTablet()) {
                        if (parentActivity instanceof LaunchActivity) {
                            LaunchActivity launchActivity = (LaunchActivity) parentActivity;
                            View layout = launchActivity != null && launchActivity.getLayersActionBarLayout() != null ? launchActivity.getLayersActionBarLayout().getView() : null;
                            allowFocus = layout == null || layout.getVisibility() != View.VISIBLE;
                        } else {
                            allowFocus = true;
                        }
                    } else {
                        allowFocus = true;
                    }
                    if (!isPaused && allowFocus && messageEditText != null) {
                        try {
                            messageEditText.requestFocus();
                        } catch (Exception e) {
                            FileLog.e(e);
                        }
                    }
                }, 600);
            }
        } else {
            if (messageEditText != null && messageEditText.isFocused() && (!keyboardVisible || isPaused)) {
                messageEditText.clearFocus();
            }
        }
    }

    public boolean hasText() {
        return messageEditText != null && messageEditText.length() > 0;
    }

    @Nullable
    public EditTextCaption getEditField() {
        return messageEditText;
    }

    public SenderSelectView getSenderSelectView() {
        return senderSelectView;
    }

    public Editable getEditText() {
        if (messageEditText == null) {
            return null;
        }
        return messageEditText.getText();
    }

    public CharSequence getDraftMessage() {
        if (editingMessageObject != null) {
            return TextUtils.isEmpty(draftMessage) ? null : draftMessage;
        }
        if (messageEditText != null && hasText()) {
            return messageEditText.getText();
        }
        return null;
    }

    public CharSequence getFieldText() {
        if (messageEditText != null && hasText()) {
            return messageEditText.getText();
        }
        return null;
    }

    public void openRichEditor() {
        if (messageEditText == null || parentFragment == null) return;
        if (!MessagesController.getInstance(currentAccount).richEditorAvailable()) return;
        final RichEditor editor;
        if (richDraftMessage != null) {
            editor = new RichEditor(richDraftMessage);
        } else {
            final CharSequence fieldText = messageEditText.getText();
            final CharSequence initialText = parseFieldMarkdown(fieldText);
            editor = new RichEditor(initialText);
            if (initialText == fieldText) {
                int selStart = messageEditText.getSelectionStart();
                int selEnd = messageEditText.getSelectionEnd();
                if (selStart < 0) selStart = messageEditText.length();
                if (selEnd < 0) selEnd = selStart;
                editor.setInitialSelection(selStart, selEnd);
            }
            editor.setOnCleared(() -> {
                if (messageEditText != null) {
                    messageEditText.setTextWithoutInputAnimation("");
                }
            });
        }
        editor.setResourceProvider(resourcesProvider);
        editor.setChatActivity(parentFragment);
        editor.animateFrom(parentFragment);
        editor.setOnSent(() -> {
            if (messageEditText != null) {
                messageEditText.setTextWithoutInputAnimation("");
            }
            checkSendButton(true);
        });
        parentFragment.presentFragment(editor);
    }

    public void openRichEditorWithHtml(CharSequence before, String html, CharSequence after) {
        if (messageEditText == null || parentFragment == null) return;
        if (!MessagesController.getInstance(currentAccount).richEditorAvailable()) return;
        final RichEditor editor = new RichEditor(html, true).setHtmlSurrounding(before, after);
        editor.setResourceProvider(resourcesProvider);
        editor.setChatActivity(parentFragment);
        editor.animateFrom(parentFragment);
        editor.setOnCleared(() -> {
            if (messageEditText != null) {
                messageEditText.setTextWithoutInputAnimation("");
            }
        });
        editor.setOnSent(() -> {
            if (messageEditText != null) {
                messageEditText.setTextWithoutInputAnimation("");
            }
            checkSendButton(true);
        });
        parentFragment.presentFragment(editor);
    }

    private boolean handleRichHtmlPaste() {
        if (messageEditText == null) return false;
        try {
            final ClipboardManager cm = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
            final ClipData clip = cm == null ? null : cm.getPrimaryClip();
            if (clip == null || clip.getItemCount() < 1 || clip.getDescription() == null
                    || !clip.getDescription().hasMimeType("text/html")) {
                return false;
            }
            final String html = clip.getItemAt(0).getHtmlText();
            if (TextUtils.isEmpty(html)) return false;
            final java.util.HashMap<Long, TL_iv.RichText> authors = new java.util.HashMap<>();
            final List<BlockRow> rows = RichHtml.parse(html, authors);
            if (rows == null || rows.isEmpty()) return false;

            if (RichMessageConvert.isLossy(rows, authors)) {
                if (!MessagesController.getInstance(currentAccount).richEditorAvailable()) {
                    return false;
                }
                final int selStart = Math.max(0, messageEditText.getSelectionStart());
                final int selEnd = Math.min(messageEditText.getText().length(), messageEditText.getSelectionEnd());
                final CharSequence before = messageEditText.getText().subSequence(0, Math.min(selStart, selEnd));
                final CharSequence after = messageEditText.getText().subSequence(Math.max(selStart, selEnd), messageEditText.getText().length());
                openRichEditorWithHtml(before, html, after);
                return true;
            }

            final SpannableStringBuilder pasted = new SpannableStringBuilder(RichMessageConvert.rowsToCharSequence(rows));
            Emoji.replaceEmoji(pasted, messageEditText.getPaint().getFontMetricsInt(), false, null);
            final AnimatedEmojiSpan[] emoji = pasted.getSpans(0, pasted.length(), AnimatedEmojiSpan.class);
            if (emoji != null) {
                for (AnimatedEmojiSpan span : emoji) {
                    span.applyFontMetrics(messageEditText.getPaint().getFontMetricsInt(), AnimatedEmojiDrawable.getCacheTypeForEnterView());
                }
            }
            final int start = Math.max(0, messageEditText.getSelectionStart());
            final int end = Math.min(messageEditText.getText().length(), messageEditText.getSelectionEnd());
            final QuoteSpan.QuoteStyleSpan[] quotesInSelection = messageEditText.getText().getSpans(start, end, QuoteSpan.QuoteStyleSpan.class);
            if (quotesInSelection != null && quotesInSelection.length > 0) {
                final QuoteSpan.QuoteStyleSpan[] toDelete = pasted.getSpans(0, pasted.length(), QuoteSpan.QuoteStyleSpan.class);
                for (int i = 0; i < toDelete.length; ++i) {
                    pasted.removeSpan(toDelete[i]);
                    pasted.removeSpan(toDelete[i].span);
                }
            } else {
                QuoteSpan.normalizeQuotes(pasted);
            }
            messageEditText.setText(messageEditText.getText().replace(start, end, pasted));
            messageEditText.setSelection(Math.min(start + pasted.length(), messageEditText.getText().length()));
            return true;
        } catch (Exception e) {
            FileLog.e(e);
            return false;
        }
    }

    private CharSequence parseFieldMarkdown(CharSequence text) {
        if (TextUtils.isEmpty(text) || TextUtils.indexOf(text, '`') < 0) {
            return text;
        }
        try {
            final CharSequence[] message = new CharSequence[]{ new SpannableStringBuilder(text) };
            final ArrayList<TLRPC.MessageEntity> entities = MediaDataController.getInstance(currentAccount).getEntities(message, true);
            if (entities == null || entities.isEmpty()) {
                return text;
            }
            final SpannableStringBuilder spanned = new SpannableStringBuilder(message[0]);
            MessageObject.addEntitiesToText(spanned, entities, false, false, false, false);
            return spanned;
        } catch (Exception e) {
            FileLog.e(e);
            return text;
        }
    }

    public boolean isRichDraftActive() {
        return richDraftActive;
    }

    public void setRichDraftPreview(TL_iv.RichMessage rich) {
        if (richDraftPreview == null) return;
        if (!MessagesController.getInstance(currentAccount).richEditorAvailable())
            rich = null;
        richDraftMessage = rich;
        updateRichDraftPreview();
    }

    private void updateRichDraftPreview() {
        if (richDraftPreview == null) return;
        final boolean wasActive = richDraftActive;
        richDraftActive = richDraftMessage != null && editingMessageObject == null;
        if (richDraftActive) {
            richDraftPreview.setResourcesProvider(resourcesProvider);
            richDraftPreview.set(richDraftMessage);
            richDraftPreview.setVisibility(View.VISIBLE);
            if (messageEditText != null) {
                messageEditText.setVisibility(View.GONE);
            }
            emojiButton.setVisibility(View.GONE);
            deleteRichDraftButton.setVisibility(View.VISIBLE);
            sendButton.setLocked(!UserConfig.getInstance(currentAccount).isPremium());
        } else {
            richDraftPreview.setVisibility(View.GONE);
            if (messageEditText != null) {
                messageEditText.setVisibility(View.VISIBLE);
            }
            emojiButton.setVisibility(View.VISIBLE);
            deleteRichDraftButton.setVisibility(View.GONE);
            sendButton.setLocked(false);
        }
        updateButtons();
        if (wasActive != richDraftActive) {
            checkSendButton(true);
        }
    }

    private void updateButtons() {
        showAiButton(messageEditText != null && messageEditText.getLineCount() > AI_RICH_LINE_THRESHOLD && messageEditText.getText() != null && !TextUtils.isEmpty(messageEditText.getText().toString().trim()));
        showRichButton(messageEditText != null && messageEditText.getLineCount() > AI_RICH_LINE_THRESHOLD && messageEditText.getText() != null && !TextUtils.isEmpty(messageEditText.getText().toString().trim()));
    }

    private void sendRichDraftAsSimpleMessage() {
        final TL_iv.RichMessage rich = richDraftMessage;
        if (rich == null || messageEditText == null) return;
        final SpannableStringBuilder simple = new SpannableStringBuilder(RichMessageConvert.toCharSequence(rich));
        Emoji.replaceEmoji(simple, messageEditText.getPaint().getFontMetricsInt(), false, null);
        final AnimatedEmojiSpan[] emoji = simple.getSpans(0, simple.length(), AnimatedEmojiSpan.class);
        if (emoji != null) {
            for (AnimatedEmojiSpan span : emoji) {
                span.applyFontMetrics(messageEditText.getPaint().getFontMetricsInt(), AnimatedEmojiDrawable.getCacheTypeForEnterView());
            }
        }
        QuoteSpan.normalizeQuotes(simple);
        clearRichDraft();
        setFieldText(simple);
        sendMessage();
    }

    // Sends non-lossy rich editor content as a simple message through the normal composer pipeline
    // (used when a non-premium user sends content that has no rich-only formatting).
    public void sendConvertedRichAsSimple(CharSequence simpleText, boolean notify, int scheduleDate, int scheduleRepeatPeriod) {
        if (messageEditText == null) return;
        final SpannableStringBuilder simple = new SpannableStringBuilder(simpleText == null ? "" : simpleText);
        Emoji.replaceEmoji(simple, messageEditText.getPaint().getFontMetricsInt(), false, null);
        final AnimatedEmojiSpan[] emoji = simple.getSpans(0, simple.length(), AnimatedEmojiSpan.class);
        if (emoji != null) {
            for (AnimatedEmojiSpan span : emoji) {
                span.applyFontMetrics(messageEditText.getPaint().getFontMetricsInt(), AnimatedEmojiDrawable.getCacheTypeForEnterView());
            }
        }
        QuoteSpan.normalizeQuotes(simple);
        clearRichDraft();
        setFieldText(simple);
        sendMessageInternal(notify, scheduleDate, scheduleRepeatPeriod, 0, true);
    }

    private void openRichEditorWithoutFormatting() {
        final TL_iv.RichMessage rich = richDraftMessage;
        if (rich == null || messageEditText == null || parentFragment == null) return;
        if (!MessagesController.getInstance(currentAccount).richEditorAvailable()) {
            sendRichDraftAsSimpleMessage();
            return;
        }
        // open with the rich draft, then convert to simple right away (undo restores the formatting)
        final RichEditor editor = new RichEditor(rich).convertToSimpleOnOpen();
        editor.setResourceProvider(resourcesProvider);
        editor.setChatActivity(parentFragment);
        editor.animateFrom(parentFragment);
        editor.setOnCleared(() -> {
            if (messageEditText != null) {
                messageEditText.setTextWithoutInputAnimation("");
            }
        });
        editor.setOnSent(() -> {
            if (messageEditText != null) {
                messageEditText.setTextWithoutInputAnimation("");
            }
            checkSendButton(true);
        });
        parentFragment.presentFragment(editor);
    }

    private void sendRichDraft(boolean notify, int scheduleDate, int scheduleRepeatPeriod, long payStars) {
        final TL_iv.RichMessage rich = richDraftMessage;
        if (rich == null) {
            return;
        }
        SendMessagesHelper.prepareSendingArticle(
            accountInstance,
            rich.blocks,
            rich.photos,
            rich.documents,
            null,
            false,
            dialog_id,
            replyingMessageObject,
            getThreadMessage(),
            replyingQuote,
            notify,
            scheduleDate,
            scheduleRepeatPeriod,
            parentFragment != null ? parentFragment.getMessageChatSendParams() : null,
            effectId,
            getSendMonoForumPeerId(),
            payStars
        );
        sendButton.setEffect(effectId = 0);
        messageEditText.setTextWithoutInputAnimation("");
        clearRichDraft();
        if (delegate != null) {
            delegate.onMessageSend(null, notify, scheduleDate, scheduleRepeatPeriod, payStars);
        }
        checkSendButton(true);
    }

    // RichEditor sent a message itself: let the chat hide the reply panel, as after any other send.
    public void onRichEditorSent(boolean notify, int scheduleDate, int scheduleRepeatPeriod) {
        if (delegate != null) {
            delegate.onMessageSend(null, notify, scheduleDate, scheduleRepeatPeriod, 0);
        }
    }

    public void clearRichDraft() {
        if (parentFragment != null) {
            MediaDataController.getInstance(currentAccount).saveDraft(
                parentFragment.getDialogId(),
                parentFragment.getDraftThreadId(),
                "",
                null,
                null,
                null,
                null,
                0,
                false,
                true,
                null
            );
        }
        setRichDraftPreview(null);
    }

    public void saveRichDraft(TL_iv.RichMessage rich) {
        if (parentFragment != null) {
            MediaDataController.getInstance(currentAccount).saveDraft(parentFragment.getDialogId(), parentFragment.getDraftThreadId(), "", null, null, null, null, 0, false, false, rich);
        }
        setRichDraftPreview(rich);
    }

    public void applyConvertedSimpleDraft(CharSequence simple) {
        if (messageEditText == null) return;
        final SpannableStringBuilder text = new SpannableStringBuilder(simple == null ? "" : simple);
        Emoji.replaceEmoji(text, messageEditText.getPaint().getFontMetricsInt(), false, null);
        final AnimatedEmojiSpan[] emoji = text.getSpans(0, text.length(), AnimatedEmojiSpan.class);
        if (emoji != null) {
            for (AnimatedEmojiSpan span : emoji) {
                span.applyFontMetrics(messageEditText.getPaint().getFontMetricsInt(), AnimatedEmojiDrawable.getCacheTypeForEnterView());
            }
        }
        QuoteSpan.normalizeQuotes(text);
        if (parentFragment != null) {
            final CharSequence[] msg = { new SpannableStringBuilder(text) };
            final ArrayList<TLRPC.MessageEntity> entities = MediaDataController.getInstance(currentAccount).getEntities(msg, true, false);
            MediaDataController.getInstance(currentAccount).saveDraft(parentFragment.getDialogId(), parentFragment.getDraftThreadId(), msg[0], entities, null, null, null, 0, false, false, null);
        }
        setRichDraftPreview(null);
        setFieldText(text);
    }

    public void updateGiftButton(boolean animated) {
        if (true) return; // hide gift button

        final TLRPC.UserFull userInfo = getParentFragment() == null ? null : getParentFragment().getCurrentUserInfo();
        final TLRPC.UserFull myUserInfo = MessagesController.getInstance(currentAccount).getUserFull(UserConfig.getInstance(currentAccount).getClientUserId());
        final TLRPC.User user = getParentFragment() == null ? null : getParentFragment().getCurrentUser();
        final boolean visible =
            !MessagesController.getInstance(currentAccount).premiumPurchaseBlocked() &&
            getParentFragment() != null && user != null &&
            !BuildVars.IS_BILLING_UNAVAILABLE &&
            (!UserObject.isUserSelf(user) || myUserInfo != null && myUserInfo.display_gifts_button) &&
            !UserObject.isBot(user) &&
            !MessagesController.isSupportUser(user) &&
            userInfo != null &&
            (
                (
                    !user.premium &&
                    MessagesController.getInstance(currentAccount).giftAttachMenuIcon &&
                    MessagesController.getInstance(currentAccount).giftTextFieldIcon &&
                    MessagesController.getInstance(currentAccount).getMainSettings().getBoolean("show_gift_for_" + parentFragment.getDialogId(), true)
                ) || (
                    BirthdayController.isToday(userInfo.birthday) &&
                    MessagesController.getInstance(currentAccount).getMainSettings().getBoolean(Calendar.getInstance().get(Calendar.YEAR) + "show_gift_for_" + parentFragment.getDialogId(), true)
                ) ||
                (userInfo.display_gifts_button || myUserInfo != null && myUserInfo.display_gifts_button) && !(
                    userInfo.disallowed_stargifts != null &&
                    userInfo.disallowed_stargifts.disallow_premium_gifts &&
                    userInfo.disallowed_stargifts.disallow_limited_stargifts &&
                    userInfo.disallowed_stargifts.disallow_unlimited_stargifts &&
                    userInfo.disallowed_stargifts.disallow_unique_stargifts
                )
            ) &&
            parentFragment != null && parentFragment.getChatMode() == 0;

        if (!visible && birthdayHint != null) {
            birthdayHint.hide();
        }
        if (!visible && giftButton == null) {
            return;
        }
        createGiftButton();

        AndroidUtilities.updateViewVisibilityAnimated(giftButton, visible, 1f, true, 1f, animated, a -> {
            if (scheduledButton != null) {
                scheduledButton.setTranslationX(scheduledButton.getTranslationX());
            }
        });
        if (visible) {
            checkBirthdayHint();
        }
    }

    private void checkBirthdayHint() {
        if (
            birthdayHint == null && giftButton != null && giftButton.getRight() != 0 &&
            parentFragment != null && BirthdayController.isToday(parentFragment.getCurrentUserInfo()) &&
            MessagesController.getInstance(currentAccount).getMainSettings().getBoolean(Calendar.getInstance().get(Calendar.YEAR) + "bdayhint_" + parentFragment.getDialogId(), true)
        ) {
            MessagesController.getInstance(currentAccount).getMainSettings().edit().putBoolean(Calendar.getInstance().get(Calendar.YEAR) + "bdayhint_" + parentFragment.getDialogId(), false).apply();

            birthdayHint = new HintView2(getContext(), HintView2.DIRECTION_BOTTOM);
            birthdayHint.setRounding(13);
            birthdayHint.setMultilineText(true);
            setBirthdayHintText();
            birthdayHint.setPadding(dp(12), 0, dp(12), 0);
            birthdayHint.setJointPx(1f, -(getWidth() - dp(12) - (messageEditTextContainer.getX() + attachLayout.getX() + giftButton.getX() + giftButton.getMeasuredWidth() / 2f)));
            addView(birthdayHint, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 200, Gravity.TOP, 0, -200 + 8, 0, 0));
            birthdayHint.setOnHiddenListener(() -> removeView(birthdayHint));
            birthdayHint.setDuration(8000);
            birthdayHint.show();
        }
    }

    private void setBirthdayHintText() {
        if (birthdayHint == null) return;
        birthdayHint.setText(Emoji.replaceWithRestrictedEmoji(AndroidUtilities.replaceTags(LocaleController.formatString(R.string.UserBirthdayHint, UserObject.getFirstName(parentFragment.getCurrentUser()))), birthdayHint.getTextPaint().getFontMetricsInt(), this::setBirthdayHintText));
        birthdayHint.setMaxWidthPx(HintView2.cutInFancyHalf(birthdayHint.getText(), birthdayHint.getTextPaint()));
    }

    public boolean showSendSuggestionHint() {
        if (sendSuggestHintView == null && suggestButton != null && suggestButton.getVisibility() == VISIBLE && MessagesController.getGlobalMainSettings().getInt("channelsuggesthint2", 0) < 2) {
            sendSuggestHintView = new HintView2(getContext(), HintView2.DIRECTION_BOTTOM);
            sendSuggestHintView.setRounding(13);
            sendSuggestHintView.setMultilineText(true);
            sendSuggestHintView.setText(LocaleController.formatString(R.string.SuggestAPostBelowHint, ForumUtilities.getMonoForumTitle(currentAccount, dialog_id, true)));
            sendSuggestHintView.setPadding(dp(12), 0, dp(12), 0);
            sendSuggestHintView.setJointPx(1f, -(getWidth() - dp(12) - (messageEditTextContainer.getX() + attachLayout.getX() + suggestButton.getX() + suggestButton.getMeasuredWidth() / 2f)));
            addView(sendSuggestHintView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 200, Gravity.TOP, 0, -200 + 8, 0, 0));
            sendSuggestHintView.setOnHiddenListener(() -> AndroidUtilities.removeFromParent(sendSuggestHintView));
            sendSuggestHintView.setDuration(8000);
            sendSuggestHintView.show();
            MessagesController.getGlobalMainSettings().edit().putInt("channelsuggesthint2", MessagesController.getGlobalMainSettings().getInt("channelsuggesthint2", 0) + 1).apply();
            return true;
        }

        return false;
    }

    public void hideHints() {
        if (sendSuggestHintView != null) {
            sendSuggestHintView.hide();
        }
        if (birthdayHint != null) {
            birthdayHint.hide();
        }
    }

    public void updateScheduleButton(boolean animated) {
        boolean notifyVisible = false;
        if (DialogObject.isChatDialog(dialog_id)) {
            TLRPC.Chat currentChat = accountInstance.getMessagesController().getChat(-dialog_id);
            silent = MessagesController.getNotificationsSettings(currentAccount).getBoolean("silent_" + dialog_id, false);
            canWriteToChannel = ChatObject.isChannel(currentChat) && (currentChat.creator || currentChat.admin_rights != null && currentChat.admin_rights.post_messages) && !currentChat.megagroup;
            if (notifyButton != null) {
                notifyVisible = canWriteToChannel;
                if (notifySilentDrawable == null) {
                    notifySilentDrawable = new CrossOutDrawable(getContext(), R.drawable.input_notify_on, Theme.key_glass_defaultIcon);
                }
                notifySilentDrawable.setCrossOut(silent, false);
                notifyButton.setImageDrawable(notifySilentDrawable);
            }
            if (attachLayout != null) {
                updateFieldRight(attachLayout.getVisibility() == VISIBLE ? 1 : 0);
            }
        }
        boolean hasScheduled = delegate != null && !isInScheduleMode() && delegate.hasScheduledMessages();
        boolean visible = hasScheduled && !scheduleButtonHidden && !recordingAudioVideo;
        if (visible) {
            createScheduledButton();
        }
        if (scheduledButton != null) {
            if (scheduledButton.getTag() != null && visible || scheduledButton.getTag() == null && !visible) {
                if (notifyButton != null) {
                    int newVisibility = !hasScheduled && notifyVisible && scheduledButton.getVisibility() != VISIBLE ? VISIBLE : GONE;
                    if (newVisibility != notifyButton.getVisibility()) {
                        notifyButton.setVisibility(newVisibility);
                    }
                }
                return;
            }
            scheduledButton.setTag(visible ? 1 : null);
        } else if (notifyButton != null) {
            int newVisibility = !hasScheduled && notifyVisible ? VISIBLE : GONE;
            if (newVisibility != notifyButton.getVisibility()) {
                notifyButton.setVisibility(newVisibility);
            }
        }
        if (scheduledButtonAnimation != null) {
            scheduledButtonAnimation.cancel();
            scheduledButtonAnimation = null;
        }
        if (!animated || notifyVisible) {
            if (scheduledButton != null) {
                scheduledButton.setVisibility(visible ? VISIBLE : GONE);
                scheduledButton.setAlpha(visible ? 1.0f : 0.0f);
                scheduledButton.setScaleX(visible ? 1.0f : 0.1f);
                scheduledButton.setScaleY(visible ? 1.0f : 0.1f);
                if (notifyButton != null) {
                    notifyButton.setVisibility(notifyVisible && scheduledButton.getVisibility() != VISIBLE ? VISIBLE : GONE);
                }
                scheduledButton.setTranslationX(0);
            } else if (notifyButton != null) {
                notifyButton.setVisibility(notifyVisible ? VISIBLE : GONE);
            }
        } else if (scheduledButton != null) {
            if (visible) {
                scheduledButton.setVisibility(VISIBLE);
            }
            scheduledButton.setPivotX(dp(24));
            scheduledButtonAnimation = new AnimatorSet();
            scheduledButtonAnimation.playTogether(ObjectAnimator.ofFloat(scheduledButton, View.ALPHA, visible ? 1.0f : 0.0f),
                    ObjectAnimator.ofFloat(scheduledButton, View.SCALE_X, visible ? 1.0f : 0.1f),
                    ObjectAnimator.ofFloat(scheduledButton, View.SCALE_Y, visible ? 1.0f : 0.1f));
            scheduledButtonAnimation.setDuration(180);
            scheduledButtonAnimation.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    scheduledButtonAnimation = null;
                    if (!visible) {
                        scheduledButton.setVisibility(GONE);
                    }
                }
            });
            scheduledButtonAnimation.start();
        }
        updateScheduledButtonLayoutParams();
        updateFieldRight(lastAttachVisible);
    }

    public void updateSendAsButton() {
        updateSendAsButton(true);
    }

    public void updateSendAsButton(boolean animated) {
        updateSendAsButton(false, animated);
    }

    public void updateSendAsButton(boolean forceHide, boolean animated) {
        if (delegate == null) {
            return;
        }
        createMessageEditText();
        if (NyaConfig.hideSendAsChannel.Bool())
            return;
        TLRPC.Chat chat;
        TLRPC.Peer defPeer;
        if (isLiveComment) {
            chat = null;
            defPeer = delegate.getDefaultSendAs();
        } else {
            chat = MessagesController.getInstance(currentAccount).getChat(-dialog_id);
            TLRPC.ChatFull full = MessagesController.getInstance(currentAccount).getChatFull(-dialog_id);
            defPeer = full != null ? full.default_send_as : null;
        }
        if (defPeer == null && delegate.getSendAsPeers() != null && !delegate.getSendAsPeers().peers.isEmpty()) {
            defPeer = delegate.getSendAsPeers().peers.get(0).peer;
        }
        boolean isVisible = !forceHide && defPeer != null && (delegate.getSendAsPeers() == null || delegate.getSendAsPeers().peers.size() > 1) &&
            !isEditingMessage() && !isRecordingAudioVideo() && (recordedAudioPanel == null || recordedAudioPanel.getVisibility() != View.VISIBLE) &&
            (isLiveComment || (!ChatObject.isChannelAndNotMegaGroup(chat) || ChatObject.canSendAsPeers(chat)) && !ChatObject.isMonoForum(chat));
        if (isVisible) {
            createSenderSelectView();
        }
        if (defPeer != null) {
            if (defPeer.channel_id != 0) {
                TLRPC.Chat ch = MessagesController.getInstance(currentAccount).getChat(defPeer.channel_id);
                if (ch != null && senderSelectView != null) {
                    senderSelectView.setAvatar(ch);
                    senderSelectView.setContentDescription(LocaleController.formatString(R.string.AccDescrSendAs, ch.title));
                }
            } else {
                TLRPC.User user = MessagesController.getInstance(currentAccount).getUser(defPeer.user_id);
                if (user != null && senderSelectView != null) {
                    senderSelectView.setAvatar(user);
                    senderSelectView.setContentDescription(LocaleController.formatString(R.string.AccDescrSendAs, ContactsController.formatName(user.first_name, user.last_name)));
                }
            }
        }
        boolean wasVisible = senderSelectView != null && senderSelectView.getVisibility() == View.VISIBLE;
        int pad = dp(2);
        float startAlpha = isVisible ? 0 : 1;
        float endAlpha = isVisible ? 1 : 0;
        final float startX, endX;
        if (senderSelectView != null) {
            MarginLayoutParams params = (MarginLayoutParams) senderSelectView.getLayoutParams();
            startX = isVisible ? -senderSelectView.getLayoutParams().width - params.leftMargin - pad : 0;
            endX = isVisible ? 0 : -senderSelectView.getLayoutParams().width - params.leftMargin - pad;
        } else {
            startX = endX = 0;
        }

        if (wasVisible == isVisible) {
            if (isIosButtonPlacement() && isSenderSelectSlotReserved && !isVisible && !isRecordingAudioVideo()
                    && (recordedAudioPanel == null || recordedAudioPanel.getVisibility() != View.VISIBLE)) {
                isSenderSelectSlotReserved = false;
                updateFieldLeftIos();
                requestLayout();
            }
            return;
        }

        ValueAnimator animator = senderSelectView == null ? null : (ValueAnimator) senderSelectView.getTag();
        if (animator != null) {
            animator.cancel();
            senderSelectView.setTag(null);
        }

        if (isIosButtonPlacement() && senderSelectView != null) {
            if (!isVisible && (isRecordingAudioVideo() || (recordedAudioPanel != null && recordedAudioPanel.getVisibility() == View.VISIBLE))) {
                isSenderSelectSlotReserved = true;
            }
            if (!(isLiveComment || parentFragment != null && parentFragment.getOtherSameChatsDiff() == 0 && parentFragment.fragmentOpened) || !animated) {
                senderSelectView.setVisibility(isVisible ? VISIBLE : GONE);
                senderSelectView.setScaleX(isVisible ? 1f : 0f);
                senderSelectView.setScaleY(isVisible ? 1f : 0f);
                senderSelectView.setAlpha(isVisible ? 1f : 0f);
                if (isVisible) {
                    isSenderSelectSlotReserved = false;
                }
                emojiButton.setTranslationX(0);
                messageTextTranslationX = 0;
                updateMessageTextParams();
                updateFieldLeftIos();
                requestLayout();
                return;
            }
            senderSelectView.setVisibility(VISIBLE);
            updateFieldLeftIos();
            float fromScale = isVisible ? 0f : senderSelectView.getScaleX();
            float fromAlpha = isVisible ? 0f : senderSelectView.getAlpha();
            senderSelectView.setScaleX(fromScale);
            senderSelectView.setScaleY(fromScale);
            senderSelectView.setAlpha(fromAlpha);
            ValueAnimator popAnim = ValueAnimator.ofFloat(0, 1).setDuration(150);
            popAnim.addUpdateListener(animation -> {
                float val = (float) animation.getAnimatedValue();
                float scale = fromScale + ((isVisible ? 1f : 0f) - fromScale) * val;
                float alpha = fromAlpha + ((isVisible ? 1f : 0f) - fromAlpha) * val;
                senderSelectView.setScaleX(scale);
                senderSelectView.setScaleY(scale);
                senderSelectView.setAlpha(alpha);
            });
            popAnim.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    senderSelectView.setTag(null);
                    senderSelectView.setScaleX(isVisible ? 1f : 0f);
                    senderSelectView.setScaleY(isVisible ? 1f : 0f);
                    senderSelectView.setAlpha(isVisible ? 1f : 0f);
                    if (!isVisible) {
                        senderSelectView.setVisibility(GONE);
                    } else {
                        isSenderSelectSlotReserved = false;
                    }
                    updateFieldLeftIos();
                    requestLayout();
                }
            });
            popAnim.start();
            senderSelectView.setTag(popAnim);
            return;
        }

        if ((isLiveComment || parentFragment != null && parentFragment.getOtherSameChatsDiff() == 0 && parentFragment.fragmentOpened) && animated) {
            ValueAnimator anim = ValueAnimator.ofFloat(0, 1).setDuration(150);
            if (senderSelectView != null) {
                senderSelectView.setTranslationX(startX);
            }
            messageTextTranslationX = startX;
            updateMessageTextParams();
            anim.addUpdateListener(animation -> {
                final float val = (float) animation.getAnimatedValue();
                final float tx = startX + (endX - startX) * val;
                if (senderSelectView != null) {
                    senderSelectView.setAlpha(startAlpha + (endAlpha - startAlpha) * val);
                    senderSelectView.setTranslationX(tx);
                }
                emojiButton.setTranslationX(tx);
                messageTextTranslationX = tx;
                updateMessageTextParams();
            });
            anim.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                    if (isVisible) {
                        createSenderSelectView();
                        senderSelectView.setVisibility(VISIBLE);
                    }
                    float tx = 0;
                    if (senderSelectView != null) {
                        senderSelectView.setAlpha(startAlpha);
                        senderSelectView.setTranslationX(startX);
                        tx = senderSelectView.getTranslationX();
                    }
                    emojiButton.setTranslationX(tx);
                    messageTextTranslationX = tx;
                    updateMessageTextParams();

                    if (botCommandsMenuButton != null && botCommandsMenuButton.getTag() == null) {
                        animationParamsX.clear();
                    }
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    if (senderSelectView != null) {
                        senderSelectView.setTag(null);
                    }
                    if (!isVisible) {
                        if (senderSelectView != null) {
                            senderSelectView.setVisibility(GONE);
                        }
                        emojiButton.setTranslationX(0);
                        messageTextTranslationX = 0;
                        updateMessageTextParams();
                    }
                    requestLayout();
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                    float tx = 0;
                    if (isVisible) {
                        createSenderSelectView();
                    }
                    if (senderSelectView != null) {
                        senderSelectView.setVisibility(isVisible ? VISIBLE : GONE);
                        senderSelectView.setAlpha(endAlpha);
                        senderSelectView.setTranslationX(endX);
                        tx = senderSelectView.getTranslationX();
                    }
                    emojiButton.setTranslationX(tx);
                    messageTextTranslationX = tx;
                    updateMessageTextParams();
                    requestLayout();
                }
            });
            anim.start();
            if (senderSelectView != null) {
                senderSelectView.setTag(anim);
            }
        } else {
            if (isVisible) {
                createSenderSelectView();
            }
            if (senderSelectView != null) {
                senderSelectView.setVisibility(isVisible ? VISIBLE : GONE);
                senderSelectView.setTranslationX(endX);
            }
            float translationX = isVisible ? endX : 0;
            emojiButton.setTranslationX(translationX);
            messageTextTranslationX = translationX;
            updateMessageTextParams();
            if (senderSelectView != null) {
                senderSelectView.setAlpha(endAlpha);
                senderSelectView.setTag(null);
            }
        }
    }

    public boolean onBotWebViewBackPressed() {
        return false;
    }

    public boolean hasBotWebView() {
        return botMenuButtonType == BotMenuButtonType.WEB_VIEW;
    }

    private void updateBotButton(boolean animated) {
        if (!isChat) {
            return;
        }
        if (parentFragment != null && !parentFragment.openAnimationEnded) {
            animated = false;
        }
        boolean hasBotWebView = hasBotWebView();
        boolean canShowBotsMenu = botMenuButtonType != BotMenuButtonType.NO_BUTTON && dialog_id > 0;
        boolean wasVisible = botButton != null && botButton.getVisibility() == VISIBLE;
        if (hasBotWebView || hasBotCommands || hasQuickReplies || botReplyMarkup != null) {
            if (botReplyMarkup != null) {
                if (isPopupShowing() && currentPopupContentType == POPUP_CONTENT_BOT_KEYBOARD && botReplyMarkup.is_persistent) {
                    if (botButton != null && botButton.getVisibility() != GONE) {
                        botButton.setVisibility(GONE);
                    }
                } else {
                    createBotButton();
                    if (botButton.getVisibility() != VISIBLE) {
                        botButton.setVisibility(VISIBLE);
                    }

                    botButtonDrawable.setIcon(R.drawable.input_bot2, true);
                    botButton.setContentDescription(getString("AccDescrBotKeyboard", R.string.AccDescrBotKeyboard));
                }
            } else {
                if (!canShowBotsMenu && !NyaConfig.INSTANCE.getHideBotButtonInInputField().Bool()) {
                    createBotButton();
                    botButtonDrawable.setIcon(R.drawable.input_bot1, true);
                    botButton.setContentDescription(getString("AccDescrBotCommands", R.string.AccDescrBotCommands));
                    botButton.setVisibility(VISIBLE);
                } else if (botButton != null) {
                    botButton.setVisibility(GONE);
                }
            }
        } else if (botButton != null) {
            botButton.setVisibility(GONE);
        }
        if (canShowBotsMenu) {
            createBotCommandsMenuButton();
        }
        boolean changed = (botButton != null && botButton.getVisibility() == VISIBLE) != wasVisible;
        if (botCommandsMenuButton != null) {
            boolean wasWebView = botCommandsMenuButton.isWebView;
            botCommandsMenuButton.setWebView(botMenuButtonType == BotMenuButtonType.WEB_VIEW);
            boolean textChanged = botCommandsMenuButton.setMenuText(botMenuButtonType == BotMenuButtonType.COMMANDS ? getString(R.string.BotsMenuTitle) : botMenuWebViewTitle);
            AndroidUtilities.updateViewVisibilityAnimated(botCommandsMenuButton, canShowBotsMenu, 0.5f, animated);
            changed = changed || textChanged || wasWebView != botCommandsMenuButton.isWebView;
        }
        if (changed && animated) {
            if (!isIosButtonPlacement()) {
                beginDelayedTransition();
            }

            boolean show = botButton != null && botButton.getVisibility() == VISIBLE;
            if (show != wasVisible && botButton != null) {
                botButton.setVisibility(VISIBLE);
                if (show) {
                    botButton.setAlpha(0f);
                    botButton.setScaleX(0.1f);
                    botButton.setScaleY(0.1f);
                    botButton.setTag(null);
                } else if (!show) {
                    botButton.setAlpha(1f);
                    botButton.setScaleX(1f);
                    botButton.setScaleY(1f);
                    botButton.setTag(1);
                }
                AndroidUtilities.updateViewVisibilityAnimated(botButton, show, 0.1f, true, 1f, !isIosButtonPlacement(), a -> {
                    if (scheduledButton != null) {
                        scheduledButton.setTranslationX(scheduledButton.getTranslationX());
                    }
                });
            }
        }
        updateFieldRight(botButton != null && botButton.getVisibility() == VISIBLE && TextUtils.isEmpty(messageEditText == null ? "" : AndroidUtilities.getTrimmedString(messageEditText.getTextToUse())) ? 2 : lastAttachVisible);
    }

    private boolean checkBotButton() {
        if (botButton == null) {
            return false;
        }
        boolean canShowBotsMenu = hasBotCommands && dialog_id > 0;
//        if (canShowBotsMenu && ) {
//            TLRPC.Chat chat = accountInstance.getMessagesController().getChat(-(int) dialog_id);
//            canShowBotsMenu = chat == null || !chat.megagroup;
//        }

        if (hasBotCommands || botReplyMarkup != null) {
            if (botReplyMarkup != null) {
                if (isPopupShowing() && currentPopupContentType == 1) {
                    botButtonDrawable.setIcon(R.drawable.input_keyboard, true);
                    botButton.setContentDescription(LocaleController.getString(R.string.AccDescrShowKeyboard));
                } else {
                    botButtonDrawable.setIcon(R.drawable.input_bot2, true);
                    botButton.setContentDescription(LocaleController.getString("AccDescrBotKeyboard", R.string.AccDescrBotKeyboard));
                }
                if (botButton.getVisibility() != VISIBLE) {
                    return true;
                }
            } else {
                if (!canShowBotsMenu && !NyaConfig.INSTANCE.getHideBotButtonInInputField().Bool()) {
                    botButtonDrawable.setIcon(R.drawable.input_bot1, true);
                    botButton.setContentDescription(LocaleController.getString("AccDescrBotCommands", R.string.AccDescrBotCommands));
                    return true;
                }
            }
        }
        return false;

    }

    public boolean isRtlText() {
        try {
            return messageEditText != null && messageEditText.getLayout().getParagraphDirection(0) == Layout.DIR_RIGHT_TO_LEFT;
        } catch (Throwable ignore) {

        }
        return false;
    }

    public void updateBotWebView(boolean animated) {
        if (botMenuButtonType != BotMenuButtonType.NO_BUTTON && dialog_id > 0) {
            createBotCommandsMenuButton();
        }
        if (botCommandsMenuButton != null) {
            botCommandsMenuButton.setWebView(hasBotWebView());
        }
        updateBotButton(animated);
    }

    public void setBotsCount(int count, boolean hasCommands, boolean hasQuickReplies, boolean animated) {
        botCount = count;
        if (hasBotCommands != hasCommands || this.hasQuickReplies != hasQuickReplies) {
            hasBotCommands = hasCommands;
            this.hasQuickReplies = hasQuickReplies;
            if (!hasText()) updateBotButton(animated);
        }
    }

    public void setButtons(MessageObject messageObject) {
        setButtons(messageObject, true);
    }

    public void setButtons(MessageObject messageObject, boolean openKeyboard) {
        setButtons(messageObject, true, openKeyboard);
    }

    public void setButtons(MessageObject messageObject, boolean openBotKeyboard, boolean openKeyboard) {
        if (replyingMessageObject != null && replyingMessageObject == botButtonsMessageObject && replyingMessageObject != messageObject) {
            botMessageObject = messageObject;
            return;
        }
        if (botButtonsMessageObject != null && botButtonsMessageObject == messageObject || botButtonsMessageObject == null && messageObject == null) {
            return;
        }
        if (botKeyboardView == null) {
            botKeyboardView = new BotKeyboardView(parentActivity, resourcesProvider) {
                @Override
                public void setTranslationY(float translationY) {
                    super.setTranslationY(translationY);
                    if (panelAnimation != null && animatingContentType == 1) {
                        delegate.bottomPanelTranslationYChanged(translationY);
                    }
                }
            };
            botKeyboardView.setVisibility(GONE);
            botKeyboardViewVisible = false;
            botKeyboardView.setDelegate(button -> {
                boolean replyingIsTopicStarter = replyingMessageObject != null && parentFragment != null && parentFragment.isTopic && parentFragment.getTopicId() == replyingMessageObject.getId();
                MessageObject object = replyingMessageObject != null && !replyingIsTopicStarter || BotForumHelper.isBotForum(currentAccount, dialog_id) ? replyingMessageObject : (DialogObject.isChatDialog(dialog_id) ? botButtonsMessageObject : null);
                if (delegate != null) {
                    delegate.beforeMessageSend(null, true, 0, 0);
                }
                boolean open = didPressedBotButton(button, object, replyingMessageObject != null && !replyingIsTopicStarter ? replyingMessageObject : botButtonsMessageObject);
                if (replyingMessageObject != null && !replyingIsTopicStarter) {
                    openKeyboardInternal();
                    setButtons(botMessageObject, false);
                } else if (botButtonsMessageObject != null && botButtonsMessageObject.messageOwner.reply_markup.single_use) {
                    if (open) {
                        openKeyboardInternal();
                    } else {
                        showPopup(0, POPUP_CONTENT_EMOJI_KEYBOARD);
                    }
                    SharedPreferences preferences = MessagesController.getMainSettings(currentAccount);
                    preferences.edit().putInt("answered_" + getTopicKeyString(), botButtonsMessageObject.getId()).commit();
                }
                if (delegate != null) {
                    delegate.onMessageSend(null, true, 0, 0, 0);
                }
            });
            viewParentForEmojiView.addView(botKeyboardView);
        }
        botButtonsMessageObject = messageObject;
        botReplyMarkup = messageObject != null && messageObject.messageOwner.reply_markup instanceof TLRPC.TL_replyKeyboardMarkup ? (TLRPC.TL_replyKeyboardMarkup) messageObject.messageOwner.reply_markup : null;

        botKeyboardView.setPanelHeight(AndroidUtilities.displaySize.x > AndroidUtilities.displaySize.y ? keyboardHeightLand : keyboardHeight);

        if (botReplyMarkup != null) {
            SharedPreferences preferences = MessagesController.getMainSettings(currentAccount);
            boolean showPopup = true;
            if (botButtonsMessageObject != replyingMessageObject) {
                if (messageObject != null && (
                        botReplyMarkup.single_use && preferences.getInt("answered_" + getTopicKeyString(), 0) == messageObject.getId() ||
                        !botReplyMarkup.is_persistent && preferences.getInt("closed_botkeyboard_" + getTopicKeyString(), 0) == messageObject.getId()
                )) {
                    showPopup = false;
                }
            }
            if (!openBotKeyboard) {
                showPopup = false;
            }
            botKeyboardView.setButtons(botReplyMarkup);
            if (showPopup && (messageEditText == null || messageEditText.length() == 0) && !isPopupShowing()) {
                showPopup(1, POPUP_CONTENT_BOT_KEYBOARD);
            }
        } else {
            if (isPopupShowing() && currentPopupContentType == POPUP_CONTENT_BOT_KEYBOARD) {
                if (openKeyboard) {
                    clearBotButtonsOnKeyboardOpen = true;
                    openKeyboardInternal();
                } else {
                    showPopup(0, POPUP_CONTENT_BOT_KEYBOARD);
                }
            }
        }
        updateBotButton(true);
    }

    public boolean didPressedBotButton(final TL_keyboard.KeyboardButtonProto button, final MessageObject replyMessageObject, final MessageObject messageObject) {
        return didPressedBotButton(button, replyMessageObject, messageObject, null);
    }

    public boolean didPressedBotButton(final TL_keyboard.KeyboardButtonProto button, final MessageObject replyMessageObject, final MessageObject messageObject, final Browser.Progress progress) {
        if (button == null || messageObject == null) {
            return false;
        }
        if (parentFragment != null && parentFragment.getChatMode() == ChatActivity.MODE_QUICK_REPLIES) return false;

        final TL_keyboard.TL_inlineButtonTypeCopy buttonTypeCopy = TLKeyboardHelper.getType(button, TL_keyboard.TL_inlineButtonTypeCopy.class);
        final TL_keyboard.TL_inlineButtonTypeUserProfile buttonTypeUserProfile = TLKeyboardHelper.getType(button, TL_keyboard.TL_inlineButtonTypeUserProfile.class);
        final TL_keyboard.TL_buttonTypeRequestPeer buttonTypeRequestPeer = TLKeyboardHelper.getType(button, TL_keyboard.TL_buttonTypeRequestPeer.class);
        final TL_keyboard.TL_inlineButtonTypeSwitchInline buttonTypeSwitchInline = TLKeyboardHelper.getType(button, TL_keyboard.TL_inlineButtonTypeSwitchInline.class);
        final TL_keyboard.TL_inlineButtonTypeUrl buttonTypeUrl = TLKeyboardHelper.getType(button, TL_keyboard.TL_inlineButtonTypeUrl.class);

        if (buttonTypeCopy != null) {
            AndroidUtilities.addToClipboard(buttonTypeCopy.copy_text);
            BulletinFactory.of(parentFragment).createCopyBulletin(LocaleController.formatString(R.string.ExactTextCopied, buttonTypeCopy.copy_text)).show(true);
        } else if (button instanceof TL_keyboard.TL_keyboardButton && ((TL_keyboard.TL_keyboardButton) button).type instanceof TL_keyboard.TL_buttonTypeDefault) {
            final TL_keyboard.TL_keyboardButton keyboardButton = (TL_keyboard.TL_keyboardButton) button;
            SendMessagesHelper.SendMessageParams params = SendMessagesHelper.SendMessageParams.of(keyboardButton.text, dialog_id, replyMessageObject, getThreadMessage(), null, false, null, null, null, true, 0, 0, null, false);
            params.sendMessageChatArguments = parentFragment != null ? parentFragment.getMessageChatSendParams() : null;
            params.effect_id = effectId;
            params.canUsePangu = false;  // Na: Always Do not use pangu for bot button text
            sendButton.setEffect(effectId = 0);
            SendMessagesHelper.getInstance(currentAccount).sendMessage(params);
        } else if (buttonTypeUrl != null) {
            if (Browser.urlMustNotHaveConfirmation(buttonTypeUrl.url) && !NyaConfig.INSTANCE.getConfirmAllLinks().Bool()) {
                Browser.openUrl(parentActivity, Uri.parse(buttonTypeUrl.url), true, true, progress);
            } else {
                AlertsCreator.showOpenUrlAlert(parentFragment, buttonTypeUrl.url, false, true, true, progress, resourcesProvider);
            }
        } else if (TLKeyboardHelper.isType(button, TL_keyboard.TL_buttonTypeRequestPhone.class)) {
            parentFragment.shareMyContact(2, messageObject);
        } else if (TLKeyboardHelper.isType(button, TL_keyboard.TL_buttonTypeRequestPoll.class)) {
            final TL_keyboard.TL_buttonTypeRequestPoll typeRequestPoll = TLKeyboardHelper.getType(button, TL_keyboard.TL_buttonTypeRequestPoll.class);
            parentFragment.openPollCreate((typeRequestPoll.flags & 1) != 0 ? typeRequestPoll.quiz : null);
            return false;
        } else if (TLKeyboardHelper.isButtonWebView(button)) {
            long botId = messageObject.messageOwner.via_bot_id != 0 ? messageObject.messageOwner.via_bot_id : messageObject.messageOwner.from_id.user_id;
            TLRPC.User user = MessagesController.getInstance(currentAccount).getUser(botId);
            Runnable onRequestWebView = new Runnable() {
                @Override
                public void run() {
                    if (sizeNotifierLayout.measureKeyboardHeight() > dp(20) || isPopupShowing()) {
                        hidePopup(false);
                        AndroidUtilities.hideKeyboard(ChatActivityEnterView.this);
                        AndroidUtilities.runOnUIThread(this, 150);
                        return;
                    }

                    if (parentFragment == null) {
                        return;
                    }

                    final WebViewRequestProps props = WebViewRequestProps.of(currentAccount, messageObject.messageOwner.dialog_id, botId, button.getText(), button.getUrl(),
                            TLKeyboardHelper.isType(button, TL_keyboard.TL_buttonTypeSimpleWebView.class) ? BotWebViewAttachedSheet.TYPE_SIMPLE_WEB_VIEW_BUTTON : BotWebViewAttachedSheet.TYPE_WEB_VIEW_BUTTON,
                            replyMessageObject != null ? replyMessageObject.messageOwner.id : 0, parentFragment == null ? 0L : parentFragment.getSendMonoForumPeerId(), false, null, false, null, null, 0, false, false);
                    if (LaunchActivity.instance != null && LaunchActivity.instance.getBottomSheetTabs() != null && LaunchActivity.instance.getBottomSheetTabs().tryReopenTab(props) != null) {
                        if (botCommandsMenuButton != null) {
                            botCommandsMenuButton.setOpened(false);
                        }
                        return;
                    }
                    final String restriction_reason = user == null ? null : MessagesController.getInstance(currentAccount).getRestrictionReason(user.restriction_reason);
                    if (!TextUtils.isEmpty(restriction_reason)) {
                        MessagesController.getInstance(currentAccount).showCantOpenAlert(parentFragment, restriction_reason);
                        return;
                    }
                    BotWebViewSheet webViewSheet = new BotWebViewSheet(getContext(), resourcesProvider);
                    webViewSheet.setParentActivity(parentActivity);
                    webViewSheet.requestWebView(parentFragment, props);
                    webViewSheet.show();
                }
            };
            if (SharedPrefsHelper.isWebViewConfirmShown(currentAccount, botId) || MessagesController.getInstance(currentAccount).whitelistedBots.contains(botId)) {
                onRequestWebView.run();
            } else {
                AlertsCreator.createBotLaunchAlert(parentFragment, MessagesController.getInstance(currentAccount).getUser(dialog_id), () -> {
                    onRequestWebView.run();
                    SharedPrefsHelper.setWebViewConfirmShown(currentAccount, botId, true);
                }, null);
            }
        } else if (TLKeyboardHelper.isType(button, TL_keyboard.TL_buttonTypeRequestGeoLocation.class)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(parentActivity);
            builder.setTitle(getString("ShareYouLocationTitle", R.string.ShareYouLocationTitle));
            builder.setMessage(getString("ShareYouLocationInfo", R.string.ShareYouLocationInfo));
            builder.setPositiveButton(getString("OK", R.string.OK), (dialogInterface, i) -> {
                if (Build.VERSION.SDK_INT >= 23 && parentActivity.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    parentActivity.requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, 2);
                    pendingMessageObject = messageObject;
                    pendingLocationButton = button;
                    return;
                }
                SendMessagesHelper.getInstance(currentAccount).sendCurrentLocation(messageObject, button);
            });
            builder.setNegativeButton(getString("Cancel", R.string.Cancel), null);
            parentFragment.showDialog(builder.create());
        } else if (TLKeyboardHelper.isType(button, TL_keyboard.TL_inlineButtonTypeCallback.class) || TLKeyboardHelper.isType(button, TL_keyboard.TL_inlineButtonTypeGame.class) || TLKeyboardHelper.isType(button, TL_keyboard.TL_inlineButtonTypeBuy.class) || TLKeyboardHelper.isType(button, TL_keyboard.TL_inlineButtonTypeUrlAuth.class)) {
            SendMessagesHelper.getInstance(currentAccount).sendCallback(true, messageObject, button, parentFragment);
        } else if (buttonTypeSwitchInline != null) {
            if (parentFragment.processSwitchButton(buttonTypeSwitchInline)) {
                return true;
            }
            if (buttonTypeSwitchInline.same_peer) {
                long uid = messageObject.messageOwner.from_id.user_id;
                if (messageObject.messageOwner.via_bot_id != 0) {
                    uid = messageObject.messageOwner.via_bot_id;
                }
                TLRPC.User user = accountInstance.getMessagesController().getUser(uid);
                if (user == null) {
                    return true;
                }
                setFieldText("@" + UserObject.getPublicUsername(user) + " " + buttonTypeSwitchInline.query);
            } else {
                Bundle args = new Bundle();
                args.putBoolean("onlySelect", true);
                args.putInt("dialogsType", DialogsActivity.DIALOGS_TYPE_BOT_SHARE);

                if ((buttonTypeSwitchInline.flags & 2) != 0) {
                    args.putBoolean("allowGroups", false);
                    args.putBoolean("allowMegagroups", false);
                    args.putBoolean("allowLegacyGroups", false);
                    args.putBoolean("allowUsers", false);
                    args.putBoolean("allowChannels", false);
                    args.putBoolean("allowBots", false);
                    for (TLRPC.InlineQueryPeerType peerType : buttonTypeSwitchInline.peer_types) {
                        if (peerType instanceof TLRPC.TL_inlineQueryPeerTypePM) {
                            args.putBoolean("allowUsers", true);
                        } else if (peerType instanceof TLRPC.TL_inlineQueryPeerTypeBotPM) {
                            args.putBoolean("allowBots", true);
                        } else if (peerType instanceof TLRPC.TL_inlineQueryPeerTypeBroadcast) {
                            args.putBoolean("allowChannels", true);
                        } else if (peerType instanceof TLRPC.TL_inlineQueryPeerTypeChat) {
                            args.putBoolean("allowLegacyGroups", true);
                        } else if (peerType instanceof TLRPC.TL_inlineQueryPeerTypeMegagroup) {
                            args.putBoolean("allowMegagroups", true);
                        }
                    }
                }

                DialogsActivity fragment = new DialogsActivity(args);
                fragment.setDelegate((fragment1, dids, message, param, notify, scheduleDate, scheduleRepeatPeriod, topicsFragment) -> {
                    long uid = messageObject.messageOwner.from_id.user_id;
                    if (messageObject.messageOwner.via_bot_id != 0) {
                        uid = messageObject.messageOwner.via_bot_id;
                    }
                    TLRPC.User user = accountInstance.getMessagesController().getUser(uid);
                    if (user == null) {
                        fragment1.finishFragment();
                        return true;
                    }
                    long did = dids.get(0).dialogId;
                    MediaDataController.getInstance(currentAccount).saveDraft(did, 0, "@" + UserObject.getPublicUsername(user) + " " + buttonTypeSwitchInline.query, null, null, true, 0);
                    if (did != dialog_id) {
                        if (!DialogObject.isEncryptedDialog(did)) {
                            Bundle args1 = new Bundle();
                            if (DialogObject.isUserDialog(did)) {
                                args1.putLong("user_id", did);
                            } else {
                                args1.putLong("chat_id", -did);
                            }
                            if (!accountInstance.getMessagesController().checkCanOpenChat(args1, fragment1)) {
                                return true;
                            }
                            ChatActivity chatActivity = new ChatActivity(args1);
                            if (parentFragment.presentFragment(chatActivity, true)) {
                                if (!AndroidUtilities.isTablet()) {
                                    parentFragment.removeSelfFromStack();
                                }
                            } else {
                                fragment1.finishFragment();
                            }
                        } else {
                            fragment1.finishFragment();
                        }
                    } else {
                        fragment1.finishFragment();
                    }
                    return true;
                });
                parentFragment.presentFragment(fragment);
            }
        } else if (buttonTypeUserProfile != null) {
            if (MessagesController.getInstance(currentAccount).getUser(buttonTypeUserProfile.user_id) != null) {
                Bundle args = new Bundle();
                args.putLong("user_id", buttonTypeUserProfile.user_id);
                ProfileActivity fragment = new ProfileActivity(args);
                parentFragment.presentFragment(fragment);
            }
        } else if (buttonTypeRequestPeer != null) {
            if (buttonTypeRequestPeer.peer_type != null && messageObject != null && messageObject.messageOwner != null) {
                if (buttonTypeRequestPeer.peer_type instanceof TLRPC.TL_requestPeerTypeCreateBot) {
                    TLRPC.User bot;
                    if (getParentFragment() != null) {
                        bot = getParentFragment().getCurrentUser();
                    } else {
                        bot = MessagesController.getInstance(currentAccount).getUser(dialog_id);
                    }
                    if (bot == null) {
                        return false;
                    }
                    CreateBotAlert.show(getContext(), currentAccount, bot, (TLRPC.TL_requestPeerTypeCreateBot) buttonTypeRequestPeer.peer_type, false, newBot -> {
                        if (newBot != null) {
                            final TLRPC.TL_messages_sendBotRequestedPeer req = new TLRPC.TL_messages_sendBotRequestedPeer();
                            req.peer = MessagesController.getInstance(currentAccount).getInputPeer(messageObject.messageOwner.peer_id);
                            req.flags |= TLObject.FLAG_0;
                            req.msg_id = messageObject.getId();
                            req.button_id = buttonTypeRequestPeer.button_id;
                            req.requested_peers.add(MessagesController.getInputPeer(newBot));
                            ConnectionsManager.getInstance(currentAccount).sendRequest(req, null);

                            final long managerId = bot.id;
                            final Bundle args = new Bundle();
                            args.putLong("user_id", newBot.id);
                            final ChatActivity chatActivity = new ChatActivity(args) {
                                private boolean shownToast;
                                @Override
                                public void onBecomeFullyVisible() {
                                    super.onBecomeFullyVisible();
                                    if (!shownToast) {
                                        shownToast = true;
                                        BulletinFactory.of(this).createSimpleBulletin(
                                                R.raw.contact_check,
                                                formatString(R.string.CreateManagedBotCreatedTitle, UserObject.getUserName(newBot)),
                                                AndroidUtilities.replaceSingleTag(
                                                    formatString(R.string.CreateManagedBotCreatedText, UserObject.getUserName(bot)),
                                                    () -> presentFragment(ChatActivity.of(managerId))
                                                )
                                        ).show();
                                    }
                                }
                            };
                            if (parentFragment != null)
                                parentFragment.presentFragment(chatActivity);
                        }
                    }, resourcesProvider, null, false);
                    return false;
                }
                if (buttonTypeRequestPeer.peer_type instanceof TLRPC.TL_requestPeerTypeUser && buttonTypeRequestPeer.max_quantity > 1) {
                    TLRPC.TL_requestPeerTypeUser peer_type = (TLRPC.TL_requestPeerTypeUser) buttonTypeRequestPeer.peer_type;
                    MultiContactsSelectorBottomSheet.open(peer_type.bot, peer_type.premium, buttonTypeRequestPeer.max_quantity, ids -> {
                        if (ids != null && !ids.isEmpty()) {
                            TLRPC.TL_messages_sendBotRequestedPeer req = new TLRPC.TL_messages_sendBotRequestedPeer();
                            req.peer = MessagesController.getInstance(currentAccount).getInputPeer(messageObject.messageOwner.peer_id);
                            req.flags |= TLObject.FLAG_0;
                            req.msg_id = messageObject.getId();
                            req.button_id = buttonTypeRequestPeer.button_id;
                            for (Long id : ids) {
                                req.requested_peers.add(MessagesController.getInstance(currentAccount).getInputPeer(id));
                            }
                            ConnectionsManager.getInstance(currentAccount).sendRequest(req, null);
                        }
                    });
                    return false;
                }
                Bundle args = new Bundle();
                args.putBoolean("onlySelect", true);
                args.putInt("dialogsType", DialogsActivity.DIALOGS_TYPE_BOT_REQUEST_PEER);
                if (messageObject != null && messageObject.messageOwner != null && messageObject.messageOwner.from_id instanceof TLRPC.TL_peerUser) {
                    args.putLong("requestPeerBotId", messageObject.messageOwner.from_id.user_id);
                }
                try {
                    SerializedData buffer = new SerializedData(buttonTypeRequestPeer.peer_type.getObjectSize());
                    buttonTypeRequestPeer.peer_type.serializeToStream(buffer);
                    args.putByteArray("requestPeerType", buffer.toByteArray());
                    buffer.cleanup();
                } catch (Exception e) {
                    FileLog.e(e);
                }
                DialogsActivity fragment = new DialogsActivity(args);
                fragment.setDelegate((dialogFragment, dids, message, param, notify, scheduleDate, scheduleRepeatPeriod, topicsFragment) -> {
                    if (dids != null && !dids.isEmpty()) {
                        TLRPC.TL_messages_sendBotRequestedPeer req = new TLRPC.TL_messages_sendBotRequestedPeer();
                        req.peer = MessagesController.getInstance(currentAccount).getInputPeer(messageObject.messageOwner.peer_id);
                        req.flags |= TLObject.FLAG_0;
                        req.msg_id = messageObject.getId();
                        req.button_id = buttonTypeRequestPeer.button_id;
                        HashSet<Long> dialogIds = new HashSet<>();
                        for (MessagesStorage.TopicKey key : dids) {
                            dialogIds.add(key.dialogId);
                        }
                        for (long did : dialogIds) {
                            req.requested_peers.add(MessagesController.getInstance(currentAccount).getInputPeer(did));
                        }
                        ConnectionsManager.getInstance(currentAccount).sendRequest(req, null);
                    }
                    dialogFragment.finishFragment();
                    return true;
                });
                parentFragment.presentFragment(fragment);
                return false;
            } else {
                FileLog.e("button.peer_type is null");
            }
        }
        return true;
    }

    public boolean isPopupView(View view) {
        return view == botKeyboardView || view == emojiView;
    }

    public int getPopupViewHeight(View view) {
        if (view == botKeyboardView && botKeyboardView != null) {
            return botKeyboardView.getKeyboardHeight();
        }
        return -1;
    }

    public boolean isRecordCircle(View view) {
        return view == recordCircle;
    }

    public SizeNotifierFrameLayout getSizeNotifierLayout() {
        return sizeNotifierLayout;
    }

    private EditTextBoldCursor mOverrideEditTextView;

    public void overrideEditTextView(EditTextBoldCursor editText) {
        mOverrideEditTextView = editText;
    }

    protected void createEmojiView() {
        if (emojiView != null && emojiView.currentAccount != UserConfig.selectedAccount) {
            viewParentForEmojiView.removeView(emojiView);
            emojiView = null;
        }
        if (emojiView != null) {
            return;
        }
        emojiView = new EmojiView(parentFragment, allowAnimatedEmoji, true, true, getContext(), true, info, sizeNotifierLayout, shouldDrawBackground, resourcesProvider, emojiViewFrozen, windowInsetsInAppController != null) {
            @Override
            public void setTranslationY(float translationY) {
                super.setTranslationY(translationY);
                if (panelAnimation != null && animatingContentType == 0) {
                    delegate.bottomPanelTranslationYChanged(translationY);
                }
            }
        };
        emojiView.shouldDrawStickerSettings = true;
        if (!shouldDrawBackground) {
            emojiView.updateColors();
        }
        emojiView.setAllow(allowStickers, allowGifs, true);
        emojiView.setVisibility(GONE);
        emojiView.setShowing(false);
        if (windowInsetsInAppController != null) {
            emojiView.shouldLightenBackground = false;
            emojiView.setShouldDrawBackground(false);
            emojiView.isNewHeightControl = true;
        }
        emojiView.setDelegate(new EmojiView.EmojiViewDelegate() {

            @Override
            public boolean isUserSelf() {
                return dialog_id == UserConfig.getInstance(currentAccount).getClientUserId();
            }

            @Override
            public boolean onBackspace() {
                final EditTextBoldCursor messageEditText = mOverrideEditTextView != null ?
                    mOverrideEditTextView : ChatActivityEnterView.this.messageEditText;

                if (messageEditText == null || messageEditText.length() == 0) {
                    return false;
                }
                messageEditText.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL));
                return true;
            }

            @Override
            public void onEmojiSelected(String symbol) {
                final EditTextBoldCursor messageEditText = mOverrideEditTextView != null ?
                    mOverrideEditTextView : ChatActivityEnterView.this.messageEditText;

                if (messageEditText == null) {
                    return;
                }
                int i = messageEditText.getSelectionEnd();
                if (i < 0) {
                    i = 0;
                }
                try {
                    innerTextChange = 2;
                    CharSequence localCharSequence = Emoji.replaceEmoji(symbol, messageEditText.getPaint().getFontMetricsInt(), false, null);
                    messageEditText.setText(messageEditText.getText().insert(i, localCharSequence));
                    int j = i + localCharSequence.length();
                    messageEditText.setSelection(j, j);
                } catch (Exception e) {
                    FileLog.e(e);
                } finally {
                    innerTextChange = 0;
                }
            }

            public void onCustomEmojiSelected(long documentId, TLRPC.Document document, String emoticon, boolean isRecent) {
                final EditTextBoldCursor messageEditText = mOverrideEditTextView != null ?
                    mOverrideEditTextView : ChatActivityEnterView.this.messageEditText;
                AndroidUtilities.runOnUIThread(() -> {
                    if (messageEditText == null) {
                        return;
                    }
                    int i = messageEditText.getSelectionEnd();
                    if (i < 0) {
                        i = 0;
                    }
                    try {
                        innerTextChange = 2;
                        SpannableString emoji = new SpannableString(emoticon == null ? "\uD83D\uDE00" : emoticon);
                        AnimatedEmojiSpan span;
                        if (document != null) {
                            span = new AnimatedEmojiSpan(document, messageEditText.getPaint().getFontMetricsInt());
                        } else {
                            span = new AnimatedEmojiSpan(documentId, messageEditText.getPaint().getFontMetricsInt());
                        }
                        if (!isRecent) {
                            span.fromEmojiKeyboard = true;
                        }
                        span.cacheType = AnimatedEmojiDrawable.getCacheTypeForEnterView();
                        emoji.setSpan(span, 0, emoji.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        messageEditText.setText(messageEditText.getText().insert(i, emoji));
                        messageEditText.setSelection(i + emoji.length(), i + emoji.length());
                    } catch (Exception e) {
                        FileLog.e(e);
                    } finally {
                        innerTextChange = 0;
                    }
                });
            }

            @Override
            public void onAnimatedEmojiUnlockClick() {
                BaseFragment fragment = parentFragment;
                if (fragment == null) {
                    fragment = getLastFragment();
                }
                BottomSheet alert = new PremiumFeatureBottomSheet(fragment, PremiumPreviewFragment.PREMIUM_FEATURE_ANIMATED_EMOJI, false);
                if (fragment != null) {
                    fragment.showDialog(alert);
                } else {
                    alert.show();
                }
            }

            @Override
            public void onStickerSelected(View view, TLRPC.Document sticker, String query, Object parent, MessageObject.SendAnimationData sendAnimationData, boolean notify, int scheduleDate, int scheduleRepeatPeriod) {
                if (isLiveComment) return;
                if (trendingStickersAlert != null) {
                    trendingStickersAlert.dismiss();
                    trendingStickersAlert = null;
                }
                if (slowModeTimer > 0 && !isInScheduleMode()) {
                    if (delegate != null) {
                        delegate.onUpdateSlowModeButton(view != null ? view : slowModeButton, true, slowModeButton.getText());
                    }
                    return;
                }
                if (stickersExpanded) {
                    if (searchingType != 0) {
                        setSearchingTypeInternal(0, true);
                        emojiView.closeSearch(true, MessageObject.getStickerSetId(sticker));
                        emojiView.hideSearchKeyboard();
                    }
                    setStickersExpanded(false, true, false);
                }
                ChatActivityEnterView.this.onStickerSelected(sticker, query, parent, sendAnimationData, false, notify, scheduleDate, 0);
                if (DialogObject.isEncryptedDialog(dialog_id) && MessageObject.isGifDocument(sticker)) {
                    accountInstance.getMessagesController().saveGif(parent, sticker);
                }
            }

            @Override
            public void onStickersSettingsClick() {
                if (parentFragment != null) {
                    parentFragment.presentFragment(new StickersActivity(MediaDataController.TYPE_IMAGE, null));
                }
            }

            @Override
            public void onEmojiSettingsClick(ArrayList<TLRPC.TL_messages_stickerSet> frozenEmojiPacks) {
                if (parentFragment != null) {
                    parentFragment.presentFragment(new StickersActivity(MediaDataController.TYPE_EMOJIPACKS, frozenEmojiPacks));
                }
            }

            @Override
            public boolean canAddCaptionToGif(TLRPC.Document document) {
                return true;
            }

            @Override
            public void onGifSelectedForAddCaption(View view, Object gif, String query, Object parent, boolean notify, int scheduleDate, int scheduleRepeatPeriod) {
                if (parentFragment == null) {
                    return;
                }

                PhotoViewer.getInstance().setParentActivity(parentFragment, parentFragment.themeDelegate);
                File file = null;
                if (gif instanceof TLRPC.Document) {
                    file = FileLoader.getInstance(currentAccount).getPathToAttach((TLRPC.Document) gif);
                }
                if (file == null) {
                    return;
                }
                File file2 = new File(FileLoader.getDirectory(FileLoader.MEDIA_DIR_CACHE), file.getName());
                if (!file.exists()) {
                    if (file2.exists()) {
                        file = file2;
                    } else {
                        //TODO photo not downloaded yet
                        return;
                    }
                }

                final ArrayList<Object> photos = new ArrayList<>();
                final MediaController.PhotoEntry entry = new MediaController.PhotoEntry(
                    0, 0, 0,
                    file.getAbsolutePath(),
                    0, false,
                    0, 0, 0
                );
                entry.caption = null;
                entry.isVideo = true;
                photos.add(entry);

                PhotoViewer.getInstance().openPhotoForSelect(photos, 0, PhotoViewer.SELECT_TYPE_GIF, false, new PhotoViewer.PhotoViewerProvider() {
                    @Override
                    public PhotoViewer.PlaceProviderObject getPlaceForPhoto(MessageObject messageObject, TLRPC.FileLocation fileLocation, int index, boolean needPreview, boolean closing) {
                        return null;
                    }

                    @Override
                    public ImageReceiver.BitmapHolder getThumbForPhoto(MessageObject messageObject, TLRPC.FileLocation fileLocation, int index) {
                        return null;
                    }

                    @Override
                    public void willSwitchFromPhoto(MessageObject messageObject, TLRPC.FileLocation fileLocation, int index) {

                    }

                    @Override
                    public void willHidePhotoViewer() {

                    }

                    @Override
                    public boolean isPhotoChecked(int index) {
                        return false;
                    }

                    @Override
                    public int setPhotoChecked(int index, VideoEditedInfo videoEditedInfo) {
                        return 0;
                    }

                    @Override
                    public int setPhotoUnchecked(Object photoEntry) {
                        return 0;
                    }

                    @Override
                    public boolean cancelButtonPressed() {
                        return false;
                    }

                    @Override
                    public void needAddMorePhotos() {

                    }

                    @Override
                    public void sendButtonPressed(int index, VideoEditedInfo videoEditedInfo, boolean notify, int scheduleDate, int scheduleRepeatPeriod, boolean forceDocument) {
                        onGifSelected(view, gif, query, parent, notify, scheduleDate, scheduleRepeatPeriod, entry, isCaptionAbove);
                    }

                    @Override
                    public void replaceButtonPressed(int index, VideoEditedInfo videoEditedInfo) {

                    }

                    @Override
                    public boolean canEdit(int index) {
                        return false;
                    }

                    @Override
                    public boolean canReplace(int index) {
                        return false;
                    }

                    @Override
                    public int getSelectedCount() {
                        return 0;
                    }

                    @Override
                    public void updatePhotoAtIndex(int index) {

                    }

                    @Override
                    public boolean allowSendingSubmenu() {
                        return true;
                    }



                    @Override
                    public boolean scaleToFill() {
                        return false;
                    }

                    @Override
                    public ArrayList<Object> getSelectedPhotosOrder() {
                        return null;
                    }

                    @Override
                    public HashMap<Object, Object> getSelectedPhotos() {
                        return null;
                    }

                    @Override
                    public boolean canScrollAway() {
                        return false;
                    }

                    @Override
                    public int getPhotoIndex(int index) {
                        return 0;
                    }

                    @Override
                    public void deleteImageAtIndex(int index) {

                    }

                    @Override
                    public String getDeleteMessageString() {
                        return "";
                    }

                    @Override
                    public boolean canCaptureMorePhotos() {
                        return false;
                    }

                    @Override
                    public void openPhotoForEdit(String file, String thumb, boolean isVideo) {

                    }

                    @Override
                    public int getTotalImageCount() {
                        return 0;
                    }

                    @Override
                    public boolean loadMore() {
                        return false;
                    }

                    @Override
                    public CharSequence getTitleFor(int index) {
                        return null;
                    }

                    @Override
                    public CharSequence getSubtitleFor(int index) {
                        return null;
                    }

                    @Override
                    public MessageObject getEditingMessageObject() {
                        return null;
                    }

                    @Override
                    public void onCaptionChanged(CharSequence caption) {

                    }

                    @Override
                    public boolean closeKeyboard() {
                        return false;
                    }

                    @Override
                    public boolean validateGroupId(long groupId) {
                        return false;
                    }

                    @Override
                    public void onApplyCaption(CharSequence caption) {

                    }

                    @Override
                    public void onOpen() {
                        PhotoViewer.getInstance().openKeyboard();
                    }

                    @Override
                    public void onClose() {

                    }



                    private boolean isCaptionAbove;

                    @Override
                    public boolean allowCaption() {
                        return true;
                    }

                    @Override
                    public boolean canMoveCaptionAbove() {
                        return true;
                    }

                    @Override
                    public void moveCaptionAbove(boolean above) {
                        isCaptionAbove = above;
                    }

                    @Override
                    public boolean isCaptionAbove() {
                        return isCaptionAbove;
                    }
                }, parentFragment);
            }

            @Override
            public void onGifSelected(View view, Object gif, String query, Object parent, boolean notify, int scheduleDate, int scheduleRepeatPeriod) {
                onGifSelected(view, gif, query, parent, notify, scheduleDate, scheduleRepeatPeriod, null, false);
            }

            public void onGifSelected(View view, Object gif, String query, Object parent, boolean notify, int scheduleDate, int scheduleRepeatPeriod, MediaController.PhotoEntry entry, boolean invertMedia) {
                if (replyingQuote != null && parentFragment != null && replyingQuote.outdated) {
                    parentFragment.showQuoteMessageUpdate();
                    return;
                }
                if (isInScheduleMode() && scheduleDate == 0) {
                    AlertsCreator.createScheduleDatePickerDialog(parentActivity, parentFragment.getDialogId(), (n, s, r) -> onGifSelected(view, gif, query, parent, n, s, r, entry, invertMedia), resourcesProvider);
                } else {
                    if (slowModeTimer > 0 && !isInScheduleMode()) {
                        if (delegate != null) {
                            delegate.onUpdateSlowModeButton(view != null ? view : slowModeButton, true, slowModeButton.getText());
                        }
                        return;
                    }
                    AlertsCreator.ensurePaidMessageConfirmation(currentAccount, dialog_id, 1, stars -> {
                        Runnable runnable = () -> {
                            if (delegate != null) {
                                delegate.beforeMessageSend(null, notify, scheduleDate, stars);
                            }
                            if (stickersExpanded) {
                                if (searchingType != 0) {
                                    emojiView.hideSearchKeyboard();
                                }
                                setStickersExpanded(false, true, false);
                            }
                            TL_stories.StoryItem storyItem = delegate != null ? delegate.getReplyToStory() : null;
                            if (gif instanceof TLRPC.Document) {
                                TLRPC.Document document = (TLRPC.Document) gif;
                                boolean applyEdit = false;

                                final VideoEditedInfo videoEditedInfo = entry != null ? entry.editedInfo : null;
                                if (videoEditedInfo != null && entry != null) {
                                    videoEditedInfo.roundVideo = true;
                                    applyEdit = videoEditedInfo.needConvert();
                                    videoEditedInfo.roundVideo = false;
                                    videoEditedInfo.muted = true;
                                }
                                if (applyEdit) {
                                    ArrayList<SendMessagesHelper.SendingMediaInfo> photos = new ArrayList<>();
                                    SendMessagesHelper.SendingMediaInfo info = new SendMessagesHelper.SendingMediaInfo();
                                    if (!entry.isVideo && entry.imagePath != null) {
                                        info.path = entry.imagePath;
                                        if (entry.isHighQuality()) {
                                            info.originalPhotoEntry = entry.clone();
                                        }
                                    } else if (entry.path != null) {
                                        info.path = entry.path;
                                    }
                                    info.thumbPath = entry.thumbPath;
                                    info.coverPath = entry.coverPath;
                                    info.coverPhoto = entry.coverPhoto;
                                    info.isLivePhoto = entry.isLivePhoto();
                                    info.isVideo = entry.isVideo;
                                    info.discardLivePhoto = entry.isUnalivePhoto();
                                    info.livePhotoVideoOffset = entry.livePhotoVideoOffset;
                                    info.livePhotoTimestampUs = entry.livePhotoTimestampUs;
                                    info.caption = entry.caption != null ? entry.caption.toString() : null;
                                    info.entities = entry.entities;
                                    info.masks = entry.stickers;
                                    info.ttl = entry.ttl;
                                    info.videoEditedInfo = entry.editedInfo;
                                    info.canDeleteAfter = entry.canDeleteAfter;
                                    info.updateStickersOrder = SendMessagesHelper.checkUpdateStickersOrder(entry.caption);
                                    info.hasMediaSpoilers = entry.hasSpoiler;
                                    info.stars = entry.starsAmount;
                                    info.highQuality = entry.isHighQuality();
                                    photos.add(info);
                                    entry.reset();

                                    SendMessagesHelper.prepareSendingMedia(AccountInstance.getInstance(currentAccount), photos, dialog_id, replyingMessageObject, getThreadMessage(), null, replyingQuote, false, false, editingMessageObject, notify, scheduleDate, scheduleRepeatPeriod, 0, false, null, parentFragment != null ? parentFragment.getMessageChatSendParams() : null, effectId, invertMedia, stars, getSendMonoForumPeerId(),  getSendMessageSuggestionParams());
                                } else {
                                    SendMessagesHelper.getInstance(currentAccount).sendSticker(document, query, dialog_id, entry != null ? entry.caption : null, videoEditedInfo, replyingMessageObject, getThreadMessage(), storyItem, replyingQuote, null, notify, scheduleDate, scheduleRepeatPeriod, false, parent, parentFragment != null ? parentFragment.getMessageChatSendParams() : null, stars, getSendMonoForumPeerId(), getSendMessageSuggestionParams(), invertMedia);
                                    MediaDataController.getInstance(currentAccount).addRecentGif(document, (int) (System.currentTimeMillis() / 1000), true);
                                    if (DialogObject.isEncryptedDialog(dialog_id)) {
                                        accountInstance.getMessagesController().saveGif(parent, document);
                                    }
                                }
                            } else if (gif instanceof TLRPC.BotInlineResult) {
                                TLRPC.BotInlineResult result = (TLRPC.BotInlineResult) gif;

                                if (result.document != null) {
                                    MediaDataController.getInstance(currentAccount).addRecentGif(result.document, (int) (System.currentTimeMillis() / 1000), false);
                                    if (DialogObject.isEncryptedDialog(dialog_id)) {
                                        accountInstance.getMessagesController().saveGif(parent, result.document);
                                    }
                                }

                                TLRPC.User bot = (TLRPC.User) parent;

                                HashMap<String, String> params = new HashMap<>();
                                params.put("id", result.id);
                                params.put("query_id", "" + result.query_id);
                                params.put("force_gif", "1");

                                if (storyItem == null) {
                                    SendMessagesHelper.prepareSendingBotContextResult(parentFragment, accountInstance, result, params, dialog_id, replyingMessageObject, getThreadMessage(), null, replyingQuote, notify, scheduleDate, 0, parentFragment != null ? parentFragment.getMessageChatSendParams() : null, stars, getSendMonoForumPeerId());
                                } else {
                                    SendMessagesHelper.getInstance(currentAccount).sendSticker(result.document, query, dialog_id, replyingMessageObject, getThreadMessage(), storyItem, replyingQuote, null, notify, scheduleDate, scheduleRepeatPeriod, false, parent, parentFragment != null ? parentFragment.getMessageChatSendParams() : null, stars, getSendMonoForumPeerId(), getSendMessageSuggestionParams());
                                }
                                if (searchingType != 0) {
                                    setSearchingTypeInternal(0, true);
                                    emojiView.closeSearch(true);
                                    emojiView.hideSearchKeyboard();
                                }
                            }
                            if (delegate != null) {
                                delegate.onMessageSend(null, notify, scheduleDate, 0, 0);
                            }
                        };
                        if (!showConfirmAlert(runnable)) {
                            runnable.run();
                        }
                    });
                }
            }

            @Override
            public void onTabOpened(int type) {
                delegate.onEmojiViewTabChanged();
                delegate.onStickersTab(type == 3);
                post(updateExpandabilityRunnable);
            }

            @Override
            public void onClearEmojiRecent() {
                if (parentFragment == null || parentActivity == null) {
                    return;
                }
                AlertDialog.Builder builder = new AlertDialog.Builder(parentActivity, resourcesProvider);
                builder.setTitle(getString(R.string.ClearRecentEmojiTitle));
                builder.setMessage(getString(R.string.ClearRecentEmojiText));
                builder.setPositiveButton(getString(R.string.ClearButton), (dialogInterface, i) -> emojiView.clearRecentEmoji());
                builder.setNegativeButton(getString(R.string.Cancel), null);
                parentFragment.showDialog(builder.create());
            }

            @Override
            public void onShowStickerSet(TLRPC.StickerSet stickerSet, TLRPC.InputStickerSet inputStickerSet, boolean edit) {
                if (trendingStickersAlert != null && !trendingStickersAlert.isDismissed()) {
                    trendingStickersAlert.getLayout().showStickerSet(stickerSet, inputStickerSet);
                    return;
                }
                BaseFragment fragment = parentFragment;
                if (fragment == null) {
                    fragment = getLastFragment();
                }
                if (fragment == null || parentActivity == null) {
                    return;
                }
                if (stickerSet != null) {
                    inputStickerSet = new TLRPC.TL_inputStickerSetID();
                    inputStickerSet.access_hash = stickerSet.access_hash;
                    inputStickerSet.id = stickerSet.id;
                }
                StickersAlert alert = new StickersAlert(parentActivity, fragment, inputStickerSet, null, ChatActivityEnterView.this, resourcesProvider, false);
                fragment.showDialog(alert);
                if (edit) {
                    alert.enableEditMode();
                }
            }

            @Override
            public void onStickerSetAdd(TLRPC.StickerSetCovered stickerSet) {
                MediaDataController.getInstance(currentAccount).toggleStickerSet(parentActivity, stickerSet, 2, parentFragment, false, false);
            }

            @Override
            public void onStickerSetRemove(TLRPC.StickerSetCovered stickerSet) {
                MediaDataController.getInstance(currentAccount).toggleStickerSet(parentActivity, stickerSet, 0, parentFragment, false, false);
            }

            @Override
            public void onStickersGroupClick(long chatId) {
                if (parentFragment != null) {
                    if (AndroidUtilities.isTablet()) {
                        hidePopup(false);
                    }
                    GroupStickersActivity fragment = new GroupStickersActivity(chatId);
                    fragment.setInfo(info);
                    parentFragment.presentFragment(fragment);
                }
            }

            @Override
            public void onSearchOpenClose(int type) {
                setSearchingTypeInternal(type, true);
                if (type != 0) {
                    setStickersExpanded(true, true, false, type == 1);
                }
                if (emojiTabOpen && searchingType == 2) {
                    checkStickresExpandHeight();
                }
            }

            @Override
            public boolean isSearchOpened() {
                return searchingType != 0;
            }

            @Override
            public boolean isExpanded() {
                return stickersExpanded;
            }

            @Override
            public boolean canSchedule() {
                return parentFragment != null && parentFragment.canScheduleMessage();
            }

            @Override
            public boolean isInScheduleMode() {
                return parentFragment != null && parentFragment.isInScheduleMode();
            }

            @Override
            public long getDialogId() {
                return dialog_id;
            }

            @Override
            public int getThreadId() {
                return getThreadMessageId();
            }

            @Override
            public void showTrendingStickersAlert(TrendingStickersLayout layout) {
                BaseFragment fragment = parentFragment;
                if (fragment == null) {
                    fragment = getLastFragment();
                }
                if (fragment != null) {
                    trendingStickersAlert = new TrendingStickersAlert(getContext(), fragment, layout, resourcesProvider) {
                        @Override
                        public void dismiss() {
                            super.dismiss();
                            if (trendingStickersAlert == this) {
                                trendingStickersAlert = null;
                            }
                            if (ChatActivityEnterView.this.delegate != null) {
                                ChatActivityEnterView.this.delegate.onTrendingStickersShowed(false);
                            }
                        }
                    };
                    if (ChatActivityEnterView.this.delegate != null) {
                        ChatActivityEnterView.this.delegate.onTrendingStickersShowed(true);
                    }
                    fragment.showDialog(trendingStickersAlert);
                }
            }

            @Override
            public void invalidateEnterView() {
                invalidate();
            }

            @Override
            public float getProgressToSearchOpened() {
                return searchToOpenProgress;
            }
        });
        emojiView.setDragListener(new EmojiView.DragListener() {

            boolean wasExpanded;
            int initialOffset;

            @Override
            public void onDragStart() {
                if (!allowDragging()) {
                    return;
                }
                if (stickersExpansionAnim != null) {
                    stickersExpansionAnim.cancel();
                }
                stickersDragging = true;
                wasExpanded = stickersExpanded;
                stickersExpanded = true;
                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.stopAllHeavyOperations, 1);
                stickersExpandedHeight = sizeNotifierLayout.getHeight()
                    - AndroidUtilities.statusBarHeight
                    - AndroidUtilities.navigationBarHeight
                    - dp(6)
                    - ActionBar.getCurrentActionBarHeight()
                    - getHeight();
                if (searchingType == 2) {
                    stickersExpandedHeight = Math.min(stickersExpandedHeight, dp(175) + (AndroidUtilities.displaySize.x > AndroidUtilities.displaySize.y ? keyboardHeightLand : keyboardHeight));
                }
                if (windowInsetsInAppController == null) {
                    emojiView.getLayoutParams().height = stickersExpandedHeight;
                }
                emojiView.setLayerType(LAYER_TYPE_HARDWARE, null);
                sizeNotifierLayout.requestLayout();
                if (shouldDrawBackground) {
                    sizeNotifierLayout.setForeground(new ScrimDrawable());
                }
                initialOffset = (int) getTranslationY();
                if (delegate != null) {
                    delegate.onStickersExpandedChange();
                }
            }

            @Override
            public void onDragEnd(float velocity) {
                if (!allowDragging()) {
                    return;
                }
                stickersDragging = false;
                if ((wasExpanded && velocity >= dp(200)) || (!wasExpanded && velocity <= dp(-200)) || (wasExpanded && stickersExpansionProgress <= 0.6f) || (!wasExpanded && stickersExpansionProgress >= 0.4f)) {
                    setStickersExpanded(!wasExpanded, true, true);
                } else {
                    setStickersExpanded(wasExpanded, true, true);
                }
            }

            @Override
            public void onDragCancel() {
                if (!stickersTabOpen) {
                    return;
                }
                stickersDragging = false;
                setStickersExpanded(wasExpanded, true, false);
            }

            @Override
            public void onDrag(int offset) {
                if (!allowDragging()) {
                    return;
                }
                int origHeight = AndroidUtilities.displaySize.x > AndroidUtilities.displaySize.y ? keyboardHeightLand : keyboardHeight;
                offset += initialOffset;
                offset = Math.max(Math.min(offset, 0), -(stickersExpandedHeight - origHeight));
                if (windowInsetsInAppController == null) {
                    emojiView.setTranslationY(offset);
                    setTranslationY(offset);
                }
                stickersExpansionProgress = (float) offset / (-(stickersExpandedHeight - origHeight));
                sizeNotifierLayout.invalidate();
            }

            private boolean allowDragging() {
                return stickersTabOpen && !(!stickersExpanded && messageEditText != null && messageEditText.length() > 0) && emojiView.areThereAnyStickers() && !waitingForKeyboardOpen;
            }
        });
        if (emojiView != null) {
            emojiView.setStickersBanned(!sendPlainEnabled, !stickersEnabled, -dialog_id);
        }
        attachEmojiView();
        checkChannelRights();
    }

    @Override
    public void onStickerSelected(TLRPC.Document sticker, String query, Object parent, MessageObject.SendAnimationData sendAnimationData, boolean clearsInputField, boolean notify, int scheduleDate, int scheduleRepeatPeriod) {
        if (isLiveComment) return;
        if (replyingQuote != null && parentFragment != null && replyingQuote.outdated) {
            parentFragment.showQuoteMessageUpdate();
            return;
        }
        if (isInScheduleMode() && scheduleDate == 0) {
            AlertsCreator.createScheduleDatePickerDialog(parentActivity, parentFragment.getDialogId(), (n, s, r) -> onStickerSelected(sticker, query, parent, sendAnimationData, clearsInputField, n, s, r), resourcesProvider);
        } else {
            AlertsCreator.ensurePaidMessageConfirmation(currentAccount, dialog_id, 1, stars -> {
                final Runnable runnable = () -> {
                    if (slowModeTimer > 0 && !isInScheduleMode()) {
                        if (delegate != null) {
                            delegate.onUpdateSlowModeButton(slowModeButton, true, slowModeButton.getText());
                        }
                        return;
                    }
                    if (searchingType != 0) {
                        setSearchingTypeInternal(0, true);
                        emojiView.closeSearch(true);
                        emojiView.hideSearchKeyboard();
                    }
                    if (delegate != null) {
                        delegate.beforeMessageSend(null, notify, scheduleDate, stars);
                    }
                    setStickersExpanded(false, true, false);
                    final TL_stories.StoryItem storyItem = delegate != null ? delegate.getReplyToStory() : null;
                    SendMessagesHelper.getInstance(currentAccount).sendSticker(sticker, query, dialog_id, replyingMessageObject, getThreadMessage(), storyItem, replyingQuote, sendAnimationData, notify, scheduleDate, scheduleRepeatPeriod, parent instanceof TLRPC.TL_messages_stickerSet, parent, parentFragment != null ? parentFragment.getMessageChatSendParams() : null, stars, getSendMonoForumPeerId(), getSendMessageSuggestionParams());
                    if (delegate != null) {
                        delegate.onMessageSend(null, true, scheduleDate, 0, 0);
                    }
                    if (clearsInputField) {
                        setFieldText("");
                    }
                    MediaDataController.getInstance(currentAccount).addRecentSticker(MediaDataController.TYPE_IMAGE, parent, sticker, (int) (System.currentTimeMillis() / 1000), false);
                };
                if (!showConfirmAlert(runnable)) {
                    runnable.run();
                }
            });
        }
    }

    @Override
    public boolean canSchedule() {
        return parentFragment != null && parentFragment.canScheduleMessage();
    }

    @Override
    public boolean isInScheduleMode() {
        return parentFragment != null && parentFragment.isInScheduleMode();
    }

    private boolean isEditingBusinessLink() {
        return editingBusinessLink != null;
    }

    public void addStickerToRecent(TLRPC.Document sticker) {
        createEmojiView();
        emojiView.addRecentSticker(sticker);
    }

    public void hideEmojiView() {
        if (!emojiViewVisible && emojiView != null && emojiView.getVisibility() != GONE) {
            viewParentForEmojiView.removeView(emojiView);
            emojiView.setVisibility(GONE);
            emojiView.setShowing(false);
        }
    }

    public void showEmojiView() {
        showPopup(1, POPUP_CONTENT_EMOJI_KEYBOARD);
    }

    private void showPopup(int show, int contentType) {
        showPopup(show, contentType, true, true);
    }

    private void showPopup(int show, int contentType, boolean allowAnimation) {
        showPopup(show, contentType, allowAnimation, true);
    }

    private void showPopup(int show, int contentType, boolean allowAnimation, boolean waitKeyboard) {
        if (show == 2) {
            return;
        }
        if (show == 1) {
            if (contentType == 0) {
                if (parentActivity == null && emojiView == null) {
                    return;
                }
                createEmojiView();
            }

            View currentView = null;
            boolean anotherPanelWasVisible = false;
            boolean samePannelWasVisible = false;
            int previousHeight = 0;
            if (contentType == POPUP_CONTENT_EMOJI_KEYBOARD) {
                attachEmojiView();
                samePannelWasVisible = emojiViewVisible && emojiView.getVisibility() == View.VISIBLE;
                emojiView.setVisibility(VISIBLE);
                emojiViewVisible = true;
                if (botKeyboardView != null && botKeyboardView.getVisibility() != GONE) {
                    botKeyboardView.setVisibility(GONE);
                    botKeyboardViewVisible = false;
                    anotherPanelWasVisible = true;
                    previousHeight = botKeyboardView.getMeasuredHeight();
                }
                emojiView.setShowing(true);
                currentView = emojiView;
                animatingContentType = 0;
            } else if (contentType == POPUP_CONTENT_BOT_KEYBOARD) {
                samePannelWasVisible = botKeyboardViewVisible && botKeyboardView.getVisibility() == View.VISIBLE;
                botKeyboardViewVisible = true;
                if (emojiView != null && emojiView.getVisibility() != GONE) {
                    viewParentForEmojiView.removeView(emojiView);
                    emojiView.setVisibility(GONE);
                    emojiView.setShowing(false);
                    emojiViewVisible = false;
                    anotherPanelWasVisible = true;
                    previousHeight = emojiView.getMeasuredHeight();
                }
                botKeyboardView.setVisibility(VISIBLE);
                currentView = botKeyboardView;
                animatingContentType = POPUP_CONTENT_BOT_KEYBOARD;
                MessagesController.getMainSettings(currentAccount).edit().remove("closed_botkeyboard_" + getTopicKeyString()).apply();
            }
            currentPopupContentType = contentType;

            if (keyboardHeight <= 0) {
                keyboardHeight = MessagesController.getGlobalEmojiSettings().getInt("kbd_height", dp(200));
            }
            if (keyboardHeightLand <= 0) {
                keyboardHeightLand = MessagesController.getGlobalEmojiSettings().getInt("kbd_height_land3", dp(200));
            }
            int currentHeight = AndroidUtilities.displaySize.x > AndroidUtilities.displaySize.y ? keyboardHeightLand : keyboardHeight;
            if (parentFragment != null && parentFragment.getParentLayout() != null) {
                currentHeight -= parentFragment.getParentLayout().getBottomTabsHeight(false);
            }
            /*if (!samePannelWasVisible && !anotherPanelWasVisible) {
                currentHeight = 0;
            } else */
            if (contentType == POPUP_CONTENT_BOT_KEYBOARD) {
                currentHeight = Math.min(botKeyboardView.getKeyboardHeight(), currentHeight);
            }
            if (botKeyboardView != null) {
                botKeyboardView.setPanelHeight(currentHeight);
            }
            if (currentView != null) {
                if (windowInsetsInAppController == null) {
                    FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) currentView.getLayoutParams();
                    layoutParams.height = currentHeight;
                    currentView.setLayoutParams(layoutParams);
                }
            }
            if (!AndroidUtilities.isInMultiwindow) {
                AndroidUtilities.hideKeyboard(messageEditText);
            }
            if (sizeNotifierLayout != null) {
                emojiPadding = currentHeight;
                sizeNotifierLayout.requestLayout();
                setEmojiButtonImage(true, true);
                updateBotButton(true);
                onWindowSizeChanged();
                if (smoothKeyboard && !keyboardVisible && currentHeight != previousHeight && allowAnimation) {
                    Runnable onAnimationEndRunnuble = () -> {
                        if (delegate != null) {
                            delegate.bottomPanelTranslationYChanged(0);
                        }
                        requestLayout();
                    };
                    if (overrideKeyboardAnimation) {
                        this.animationEndRunnable = onAnimationEndRunnuble;
                    } else {
                        panelAnimation = new AnimatorSet();
                        if (windowInsetsInAppController != null) {
                            panelAnimation.playTogether(ObjectAnimator.ofFloat(currentHeight - previousHeight, 0));
                        } else {
                            currentView.setTranslationY(currentHeight - previousHeight);
                            panelAnimation.playTogether(ObjectAnimator.ofFloat(currentView, View.TRANSLATION_Y, currentHeight - previousHeight, 0));
                        }
                        panelAnimation.setInterpolator(AdjustPanLayoutHelper.keyboardInterpolator);
                        panelAnimation.setDuration(AdjustPanLayoutHelper.keyboardDuration);
                        panelAnimation.addListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                panelAnimation = null;
                                notificationsLocker.unlock();
                                onAnimationEndRunnuble.run();
                            }
                        });
                        AndroidUtilities.runOnUIThread(runEmojiPanelAnimation, 50);
                        notificationsLocker.lock();
                    }
                    requestLayout();
                }
            }
            if (windowInsetsInAppController != null) {
                windowInsetsInAppController.requestInAppKeyboardHeightIncludeNavbar(currentHeight);
            }
        } else {
            if (emojiButton != null) {
                setEmojiButtonImage(false, true);
            }
            currentPopupContentType = -1;
            if (emojiView != null) {
                if (show != 2 || AndroidUtilities.usingHardwareInput || AndroidUtilities.isInMultiwindow) {
                    if (smoothKeyboard && !keyboardVisible && !stickersExpanded) {
                        if (emojiViewVisible = true) {
                            animatingContentType = 0;
                        }
                        emojiView.setShowing(false);
                        Runnable animationEndRunnable = () -> {
                            if (show == 0) {
                                emojiPadding = 0;
                            }
                            panelAnimation = null;
                            if (emojiView != null) {
                                if (windowInsetsInAppController == null) {
                                    emojiView.setTranslationY(0);
                                }
                                emojiView.setVisibility(GONE);
                                viewParentForEmojiView.removeView(emojiView);
                                if (removeEmojiViewAfterAnimation) {
                                    removeEmojiViewAfterAnimation = false;
                                    emojiView = null;
                                }
                            }
                            if (delegate != null) {
                                delegate.bottomPanelTranslationYChanged(0);
                            }
                            requestLayout();
                        };
                        if (!overrideKeyboardAnimation) {
                            panelAnimation = new AnimatorSet();
                            if (windowInsetsInAppController != null) {
                                panelAnimation.playTogether(
                                    ObjectAnimator.ofFloat(emojiView.getMeasuredHeight()),
                                    ObjectAnimator.ofFloat(0, 1));
                            } else {
                                panelAnimation.playTogether(ObjectAnimator.ofFloat(emojiView, View.TRANSLATION_Y, emojiView.getMeasuredHeight()));
                            }
                            panelAnimation.setInterpolator(AdjustPanLayoutHelper.keyboardInterpolator);
                            panelAnimation.setDuration(AdjustPanLayoutHelper.keyboardDuration);
                            notificationsLocker.lock();
                            panelAnimation.addListener(new AnimatorListenerAdapter() {
                                @Override
                                public void onAnimationEnd(Animator animation) {
                                    animationEndRunnable.run();
                                    notificationsLocker.unlock();
                                }
                            });
                        } else {
                            this.animationEndRunnable = animationEndRunnable;
                        }
                        AndroidUtilities.runOnUIThread(runEmojiPanelAnimation, 50);
                        requestLayout();
                    } else {
                        if (delegate != null) {
                            delegate.bottomPanelTranslationYChanged(0);
                        }
                        emojiPadding = 0;
                        viewParentForEmojiView.removeView(emojiView);
                        emojiView.setVisibility(GONE);
                        emojiView.setShowing(false);
                    }
                } else {
                    removeEmojiViewAfterAnimation = false;
                    if (delegate != null) {
                        delegate.bottomPanelTranslationYChanged(0);
                    }
                    viewParentForEmojiView.removeView(emojiView);
                    emojiView = null;
                }
                emojiViewVisible = false;
            }
            if (botKeyboardView != null && botKeyboardView.getVisibility() == View.VISIBLE) {
                if (show != 2 || AndroidUtilities.usingHardwareInput || AndroidUtilities.isInMultiwindow) {
                    if (smoothKeyboard && !keyboardVisible) {
                        if (botKeyboardViewVisible) {
                            animatingContentType = 1;
                        }

                        panelAnimation = new AnimatorSet();
                        if (windowInsetsInAppController != null) {
                            panelAnimation.playTogether(ObjectAnimator.ofFloat(botKeyboardView.getMeasuredHeight()));
                        } else {
                            panelAnimation.playTogether(ObjectAnimator.ofFloat(botKeyboardView, View.TRANSLATION_Y, botKeyboardView.getMeasuredHeight()));
                        }
                        panelAnimation.setInterpolator(AdjustPanLayoutHelper.keyboardInterpolator);
                        panelAnimation.setDuration(AdjustPanLayoutHelper.keyboardDuration);
                        panelAnimation.addListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                if (show == 0) {
                                    emojiPadding = 0;
                                }
                                panelAnimation = null;
                                botKeyboardView.setTranslationY(0);
                                botKeyboardView.setVisibility(GONE);
                                notificationsLocker.unlock();
                                if (delegate != null) {
                                    delegate.bottomPanelTranslationYChanged(0);
                                }
                                requestLayout();
                            }
                        });
                        notificationsLocker.lock();
                        AndroidUtilities.runOnUIThread(runEmojiPanelAnimation, 50);
                        requestLayout();
                    } else {
                        if (!waitingForKeyboardOpen) {
                            botKeyboardView.setVisibility(GONE);
                        }
                    }
                }
                botKeyboardViewVisible = false;
            }
            if (contentType == POPUP_CONTENT_BOT_KEYBOARD && botButtonsMessageObject != null) {
                MessagesController.getMainSettings(currentAccount).edit().putInt("closed_botkeyboard_" + getTopicKeyString(), botButtonsMessageObject.getId()).apply();
            }
            updateBotButton(true);

            if (windowInsetsInAppController != null) {
                windowInsetsInAppController.resetInAppKeyboardHeight(waitKeyboard);
            }
        }

        if (stickersTabOpen || emojiTabOpen) {
            checkSendButton(true);
        }
        if (stickersExpanded && show != 1) {
            setStickersExpanded(false, false, false);
        }
        updateFieldHint(false);
        checkBotMenu();
    }

    private void attachEmojiView() {
        if (emojiView.getParent() == null) {
            if (windowInsetsInAppController == null) {
                viewParentForEmojiView.addView(emojiView);
            } else {
                viewParentForEmojiView.addView(emojiView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
            }
        }
    }

    private String getTopicKeyString() {
        if (parentFragment != null && parentFragment.isTopic) {
            return dialog_id + "_" + parentFragment.getTopicId();
        }
        return "" + dialog_id;
    }

    private ItemOptions formatOptions;

    private void updateFormatButton(boolean animated) {
        if (emojiButton == null) {
            return;
        }
        final boolean show = parentFragment != null && messageEditText != null && messageEditText.getSelectionEnd() != messageEditText.getSelectionStart();
        emojiButton.setFormatMode(show, animated);
        if (!show && formatOptions != null) {
            formatOptions.dismiss();
            formatOptions = null;
        }
    }

    private void toggleFormatOptions() {
        if (formatOptions != null) {
            formatOptions.dismiss();
            formatOptions = null;
            return;
        }
        if (messageEditText == null || parentFragment == null) {
            return;
        }
        messageEditText.hideActionMode();
        final ItemOptions options = ItemOptions.makeOptions(sizeNotifierLayout != null ? sizeNotifierLayout : this, resourcesProvider, emojiButton, false, false, true);
        options.setMaxHeight(dp(280));
        ChatActivity.fillActionModeMenu(new MenuToItemOptions(options, messageEditText), parentFragment.getCurrentEncryptedChat(), true, true);
        options.forceTop(true);
        options.setOnDismiss(() -> {
            if (formatOptions == options) {
                formatOptions = null;
            }
        });
        formatOptions = options;
        options.show();
    }

    private void setEmojiButtonImage(boolean byOpen, boolean animated) {
        if (emojiButton == null) {
            return;
        }
        boolean showingRecordInterface = recordInterfaceState == 1 || (recordedAudioPanel != null && recordedAudioPanel.getVisibility() == View.VISIBLE);
        if (showingRecordInterface) {
            emojiButtonAlpha = emojiButtonScale = 0f;
            updateEmojiButtonParams();
            animated = false;
        }
        ChatActivityEnterViewAnimatedIconView.State nextIcon;
        if (byOpen && currentPopupContentType == 0) {
            if (!sendPlainEnabled) {
                return;
            }
            nextIcon = ChatActivityEnterViewAnimatedIconView.State.KEYBOARD;
        } else {
            int currentPage;
            if (emojiView == null) {
                currentPage = MessagesController.getGlobalEmojiSettings().getInt("selected_page", 0);
            } else {
                currentPage = emojiView.getCurrentPage();
            }
            if (currentPage == 0 || !allowStickers && !allowGifs) {
                nextIcon = ChatActivityEnterViewAnimatedIconView.State.SMILE;
            } else if (messageEditText != null && !TextUtils.isEmpty(messageEditText.getText())) {
                nextIcon = ChatActivityEnterViewAnimatedIconView.State.SMILE;
            } else {
                if (currentPage == 1) {
                    nextIcon = ChatActivityEnterViewAnimatedIconView.State.STICKER;
                } else {
                    nextIcon = ChatActivityEnterViewAnimatedIconView.State.GIF;
                }
            }
        }
        if (!sendPlainEnabled && nextIcon == ChatActivityEnterViewAnimatedIconView.State.SMILE) {
            nextIcon = ChatActivityEnterViewAnimatedIconView.State.GIF;
        } else if (!stickersEnabled && nextIcon != ChatActivityEnterViewAnimatedIconView.State.SMILE) {
            nextIcon = ChatActivityEnterViewAnimatedIconView.State.SMILE;
        }

        emojiButton.setState(nextIcon, animated);
        onEmojiIconChanged(nextIcon);
    }

    protected void onEmojiIconChanged(ChatActivityEnterViewAnimatedIconView.State currentIcon) {
        if (currentIcon == ChatActivityEnterViewAnimatedIconView.State.GIF && emojiView == null) {
            MediaDataController.getInstance(currentAccount).loadRecents(MediaDataController.TYPE_IMAGE, true, true, false);
            final ArrayList<String> gifSearchEmojies = MessagesController.getInstance(currentAccount).gifSearchEmojies;
            for (int i = 0, N = Math.min(10, gifSearchEmojies.size()); i < N; i++) {
                Emoji.preloadEmoji(gifSearchEmojies.get(i));
            }
        }
    }

    public boolean hidePopup(boolean byBackButton) {
        return hidePopup(byBackButton, false);
    }

    public boolean hidePopup(boolean byBackButton, boolean forceAnimate) {
        return hidePopup(byBackButton, forceAnimate, true);
    }

    public boolean hidePopup(boolean byBackButton, boolean forceAnimate, boolean waitKeyboardOpen) {
        if (isPopupShowing()) {
            if (currentPopupContentType == POPUP_CONTENT_BOT_KEYBOARD && botReplyMarkup != null && byBackButton && botButtonsMessageObject != null) {
                if (botReplyMarkup.is_persistent) {
                    return false;
                }
                MessagesController.getMainSettings(currentAccount).edit().putInt("closed_botkeyboard_" + getTopicKeyString(), botButtonsMessageObject.getId()).apply();
            }
            if (byBackButton && searchingType != 0 || forceAnimate) {
                setSearchingTypeInternal(0, true);
                if (emojiView != null) {
                    emojiView.closeSearch(true);
                }
                if (messageEditText != null) {
                    messageEditText.requestFocus();
                }
                setStickersExpanded(false, true, false);
                if (emojiTabOpen) {
                    checkSendButton(true);
                }
            } else {
                if (searchingType != 0) {
                    setSearchingTypeInternal(0, false);
                    emojiView.closeSearch(false);
                    if (messageEditText != null) {
                        messageEditText.requestFocus();
                    }
                } else if (stickersExpanded) {
                    setStickersExpanded(false, true, false);
                } else {
                    showPopup(0, POPUP_CONTENT_EMOJI_KEYBOARD, true, waitKeyboardOpen && !byBackButton);
                }
            }
            return true;
        }
        return false;
    }

    private void setSearchingTypeInternal(int searchingType, boolean animated) {
        boolean showSearchingNew = searchingType != 0;
        boolean showSearchingOld = this.searchingType != 0;
        if (showSearchingNew != showSearchingOld) {
            if (searchAnimator != null) {
                searchAnimator.removeAllListeners();
                searchAnimator.cancel();
            }
            if (!animated) {
                searchToOpenProgress = showSearchingNew ? 1f : 0f;
                if (emojiView != null) {
                    emojiView.searchProgressChanged();
                }
            } else {
                searchAnimator = ValueAnimator.ofFloat(searchToOpenProgress, showSearchingNew ? 1f : 0f);
                searchAnimator.addUpdateListener(valueAnimator -> {
                    searchToOpenProgress = (float) valueAnimator.getAnimatedValue();
                    if (emojiView != null) {
                        emojiView.searchProgressChanged();
                    }
                });
                searchAnimator.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        searchToOpenProgress = showSearchingNew ? 1f : 0f;
                        if (emojiView != null) {
                            emojiView.searchProgressChanged();
                        }
                    }
                });
                searchAnimator.setDuration(220);
                searchAnimator.setInterpolator(CubicBezierInterpolator.DEFAULT);
                searchAnimator.start();
            }
        }

        this.searchingType = searchingType;
    }

    public boolean isCurrentPageEmoji() {
        return emojiView != null && emojiView.getCurrentPage() == 0;
    }

    public void openKeyboardInternal() {
        if (hasBotWebView() && botCommandsMenuIsShowing() || BaseFragment.hasSheets(parentFragment)) {
            return;
        }
        showPopup(AndroidUtilities.usingHardwareInput || AndroidUtilities.isInMultiwindow || parentFragment != null && parentFragment.isInBubbleMode() || isPaused ? 0 : 2, POPUP_CONTENT_EMOJI_KEYBOARD);
        if (delegate != null) {
            delegate.onKeyboardRequested();
        }
        if (messageEditText != null) {
            messageEditText.requestFocus();
        }
        AndroidUtilities.showKeyboard(messageEditText);
        if (isPaused) {
            showKeyboardOnResume = true;
        } else if (!AndroidUtilities.usingHardwareInput && !keyboardVisible && !AndroidUtilities.isInMultiwindow && (parentFragment == null || !parentFragment.isInBubbleMode())) {
            waitingForKeyboardOpen = true;
            if (emojiView != null) {
                emojiView.onTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_CANCEL, 0, 0, 0));
            }
            AndroidUtilities.cancelRunOnUIThread(openKeyboardRunnable);
            AndroidUtilities.runOnUIThread(openKeyboardRunnable, 100);
        }
    }

    public boolean isEditingMessage() {
        return editingMessageObject != null;
    }

    public MessageObject getEditingMessageObject() {
        return editingMessageObject;
    }

    public boolean isEditingCaption() {
        return editingCaption;
    }

    public boolean hasAudioToSend() {
        return audioToSendMessageObject != null || videoToSendMessageObject != null;
    }

    public void openKeyboard() {
        if (hasBotWebView() && botCommandsMenuIsShowing() || BaseFragment.hasSheets(parentFragment)) {
            return;
        }
        if (delegate != null) {
            delegate.onKeyboardRequested();
        }
        if (messageEditText != null && !AndroidUtilities.showKeyboard(messageEditText)) {
            messageEditText.clearFocus();
            messageEditText.requestFocus();
        }
    }

    public void closeKeyboard() {
        AndroidUtilities.hideKeyboard(messageEditText);
    }

    public boolean isPopupShowing() {
        return emojiViewVisible || botKeyboardViewVisible;
    }

    public boolean canHidePopupByBackButton() {
        if (!isPopupShowing()) {
            return false;
        }
        return !(currentPopupContentType == POPUP_CONTENT_BOT_KEYBOARD && botReplyMarkup != null && botReplyMarkup.is_persistent && botButtonsMessageObject != null);
    }

    public boolean closeCreationLinkDialog(boolean invoked) {
        return messageEditText != null && messageEditText.closeCreationLinkDialog(invoked);
    }

    public boolean isKeyboardVisible() {
        return keyboardVisible;
    }

    public boolean isWaitingForKeyboard() {
        return waitingForKeyboardOpen;
    }

    public void addRecentGif(TLRPC.Document searchImage) {
        MediaDataController.getInstance(currentAccount).addRecentGif(searchImage, (int) (System.currentTimeMillis() / 1000), true);
        if (emojiView != null) {
            emojiView.addRecentGif(searchImage);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w != oldw && stickersExpanded) {
            setSearchingTypeInternal(0, false);
            emojiView.closeSearch(false);
            setStickersExpanded(false, false, false);
        }
        if (videoTimelineView != null) {
            videoTimelineView.clearFrames();
        }
    }

    public boolean isStickersExpanded() {
        return stickersExpanded;
    }

    @Override
    public void onSizeChanged(int height, boolean isWidthGreater) {
        if (searchingType != 0) {
            lastSizeChangeValue1 = height;
            lastSizeChangeValue2 = isWidthGreater;
            keyboardVisible = height > 0;
            checkBotMenu();
            return;
        }
        if (height > dp(50) && keyboardVisible && !AndroidUtilities.isInMultiwindow) {
            if (isWidthGreater) {
                keyboardHeightLand = height;
                MessagesController.getGlobalEmojiSettings().edit().putInt("kbd_height_land3", keyboardHeightLand).commit();
            } else {
                keyboardHeight = height;
                MessagesController.getGlobalEmojiSettings().edit().putInt("kbd_height", keyboardHeight).commit();
            }
        }

        if (keyboardVisible) {
            if (emojiViewVisible && emojiView == null) {
                emojiViewVisible = false;
            }
        }

        if (isPopupShowing()) {
            int newHeight = isWidthGreater ? keyboardHeightLand : keyboardHeight;
            if (parentFragment != null && parentFragment.getParentLayout() != null) {
                newHeight -= parentFragment.getParentLayout().getBottomTabsHeight(false);
            }
            if (currentPopupContentType == POPUP_CONTENT_BOT_KEYBOARD && !botKeyboardView.isFullSize()) {
                newHeight = Math.min(botKeyboardView.getKeyboardHeight(), newHeight);
            }

            View currentView = null;
            if (currentPopupContentType == 0) {
                currentView = emojiView;
            } else if (currentPopupContentType == POPUP_CONTENT_BOT_KEYBOARD) {
                currentView = botKeyboardView;
            }
            if (botKeyboardView != null) {
                botKeyboardView.setPanelHeight(newHeight);
                if (windowInsetsInAppController != null && newHeight > 0 && currentPopupContentType == POPUP_CONTENT_BOT_KEYBOARD) {
                    windowInsetsInAppController.requestInAppKeyboardHeightIncludeNavbar(newHeight);
                }
            }

            if (currentView != null) {
                FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) currentView.getLayoutParams();
                final boolean panelSizeChanged = windowInsetsInAppController == null ?
                        layoutParams.width != AndroidUtilities.displaySize.x || layoutParams.height != newHeight :
                        emojiPadding != newHeight;
                final boolean needRebuild = !closeAnimationInProgress && !stickersExpanded && panelSizeChanged;

                if (needRebuild) {
                    int currentPanelHeight = newHeight;
                    if (windowInsetsInAppController == null) {
                        layoutParams.width = AndroidUtilities.displaySize.x;
                        layoutParams.height = newHeight;
                        currentView.setLayoutParams(layoutParams);
                        currentPanelHeight = layoutParams.height;
                    } else if (newHeight > 0) {
                        windowInsetsInAppController.requestInAppKeyboardHeightIncludeNavbar(newHeight);
                    }
                    if (sizeNotifierLayout != null) {
                        int oldHeight = emojiPadding;
                        emojiPadding = currentPanelHeight;
                        sizeNotifierLayout.requestLayout();
                        onWindowSizeChanged();
                        if (smoothKeyboard && !keyboardVisible && oldHeight != emojiPadding && pannelAnimationEnabled()) {
                            panelAnimation = new AnimatorSet();
                            if (windowInsetsInAppController != null) {
                                panelAnimation.playTogether(ObjectAnimator.ofFloat(emojiPadding - oldHeight, 0));
                            } else {
                                panelAnimation.playTogether(ObjectAnimator.ofFloat(currentView, View.TRANSLATION_Y, emojiPadding - oldHeight, 0));
                            }
                            panelAnimation.setInterpolator(AdjustPanLayoutHelper.keyboardInterpolator);
                            panelAnimation.setDuration(AdjustPanLayoutHelper.keyboardDuration);
                            panelAnimation.addListener(new AnimatorListenerAdapter() {
                                @Override
                                public void onAnimationEnd(Animator animation) {
                                    panelAnimation = null;
                                    if (delegate != null) {
                                        delegate.bottomPanelTranslationYChanged(0);
                                    }
                                    requestLayout();
                                    notificationsLocker.unlock();
                                }
                            });
                            AndroidUtilities.runOnUIThread(runEmojiPanelAnimation, 50);
                            notificationsLocker.lock();
                            requestLayout();
                        }
                    }
                }
            }
        }

        if (lastSizeChangeValue1 == height && lastSizeChangeValue2 == isWidthGreater) {
            onWindowSizeChanged();
            return;
        }
        lastSizeChangeValue1 = height;
        lastSizeChangeValue2 = isWidthGreater;

        boolean oldValue = keyboardVisible;
        keyboardVisible = height > 0;
        checkBotMenu();
        if (keyboardVisible && isPopupShowing() && stickersExpansionAnim == null) {
            showPopup(0, currentPopupContentType);
        } else if (!keyboardVisible && !isPopupShowing() && botButtonsMessageObject != null && replyingMessageObject != botButtonsMessageObject && (!hasBotWebView() && !botCommandsMenuIsShowing() && !BaseFragment.hasSheets(parentFragment)) && (messageEditText == null || TextUtils.isEmpty(messageEditText.getText())) && botReplyMarkup != null && !botReplyMarkup.rows.isEmpty()) {
            if (sizeNotifierLayout.adjustPanLayoutHelper.animationInProgress()) {
                sizeNotifierLayout.adjustPanLayoutHelper.stopTransition();
            } else {
                sizeNotifierLayout.adjustPanLayoutHelper.ignoreOnce();
            }
            showPopup(1, POPUP_CONTENT_BOT_KEYBOARD, false);
        }
        if (emojiPadding != 0 && !keyboardVisible && keyboardVisible != oldValue && !isPopupShowing()) {
            emojiPadding = 0;
            sizeNotifierLayout.requestLayout();
        }
        if (keyboardVisible && waitingForKeyboardOpen) {
            waitingForKeyboardOpen = false;
            if (clearBotButtonsOnKeyboardOpen) {
                clearBotButtonsOnKeyboardOpen = false;
                botKeyboardView.setButtons(botReplyMarkup);
            }

            AndroidUtilities.cancelRunOnUIThread(openKeyboardRunnable);
        }
        onWindowSizeChanged();
    }

    public void checkReactionsButton(boolean visible) {
        AndroidUtilities.updateViewVisibilityAnimated(reactionsButton, visible, 0.1f, true);
    }

    public int getEmojiPadding() {
        return emojiPadding;
    }

    public int getVisibleEmojiPadding() {
        return emojiViewVisible ? emojiPadding : 0;
    }

    private MessageObject getThreadMessage() {
        return parentFragment != null ? parentFragment.getThreadMessage() : null;
    }

    private int getThreadMessageId() {
        return parentFragment != null && parentFragment.getThreadMessage() != null ? parentFragment.getThreadMessage().getId() : 0;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.didSetNewTheme) {
            updateFieldTextSize();
        } else if (id == NotificationCenter.reloadInterface) {
            updateButtons();
        } else if (id == NotificationCenter.emojiLoaded) {
            if (emojiView != null) {
                emojiView.invalidateViews();
            }
            if (botKeyboardView != null) {
                botKeyboardView.invalidateViews();
            }
            if (messageEditText != null) {
                messageEditText.postInvalidate();
                messageEditText.invalidateForce();
            }
        } else if (id == NotificationCenter.recordProgressChanged) {
            int guid = (Integer) args[0];
            if (guid != recordingGuid) {
                return;
            }

            if (recordInterfaceState != 0 && !wasSendTyping && !isInScheduleMode()) {
                wasSendTyping = true;
                accountInstance.getMessagesController().sendTyping(dialog_id, getThreadMessageId(), isInVideoMode() ? 7 : 1, 0);
            }

            if (recordCircle != null) {
                recordCircle.setAmplitude((Double) args[1]);
            }
        } else if (id == NotificationCenter.closeChats) {
            if (messageEditText != null && messageEditText.isFocused()) {
                AndroidUtilities.hideKeyboard(messageEditText);
            }
        } else if (id == NotificationCenter.recordStartError || id == NotificationCenter.recordStopped) {
            int guid = (Integer) args[0];
            if (guid != recordingGuid) {
                return;
            }
            if (recordingAudioVideo) {
                recordingAudioVideo = false;
                if (id == NotificationCenter.recordStopped) {
                    Integer reason = (Integer) args[1];
                    int state;
                    if (reason == 4) {
                        state = RECORD_STATE_CANCEL_BY_TIME;
                    } else if (isInVideoMode() && reason == 5) {
                        state = RECORD_STATE_SENDING;
                    } else {
                        if (reason == 0) {
                            state = RECORD_STATE_CANCEL_BY_GESTURE;
                        } else if (reason == 6) {
                            state = RECORD_STATE_CANCEL;
                        } else {
                            state = RECORD_STATE_PREPARING;
                        }
                    }
                    if (state != RECORD_STATE_PREPARING) {
                        updateRecordInterface(state, true);
                    }
                } else {
                    updateRecordInterface(RECORD_STATE_CANCEL, true);
                }
            }
        } else if (id == NotificationCenter.recordStarted) {
            int guid = (Integer) args[0];
            if (guid != recordingGuid) {
                return;
            }
            boolean audio = (Boolean) args[1];
            isInVideoMode = !audio;
            if (audioVideoSendButton != null && (!NyaConfig.useChatAttachMediaMenu.Bool() || isStories)) {
                audioVideoSendButton.setState(audio ? ChatActivityEnterViewAnimatedIconView.State.VOICE : ChatActivityEnterViewAnimatedIconView.State.VIDEO, true);
            }
            if (!recordingAudioVideo) {
                recordingAudioVideo = true;
                updateRecordInterface(RECORD_STATE_ENTER, true);
            } else if (recordCircle != null) {
                recordCircle.showWaves(true, true);
            }
            if (recordTimerView != null) {
                recordTimerView.start(millisecondsRecorded);
            }
            if (recordDot != null) {
                recordDot.enterAnimation = false;
            }
        } else if (id == NotificationCenter.recordPaused) {
            recordingAudioVideo = false;
            audioToSend = null;
            videoToSendMessageObject = null;
        } else if (id == NotificationCenter.recordResumed) {
            audioToSend = null;
            videoToSendMessageObject = null;
            if (recordTimerView != null) {
                recordTimerView.start(millisecondsRecorded);
            }
            checkSendButton(true);
            recordingAudioVideo = true;
            updateRecordInterface(RECORD_STATE_ENTER, true);
        } else if (id == NotificationCenter.audioDidSent) {
            int guid = (Integer) args[0];
            if (guid != recordingGuid) {
                return;
            }
            millisecondsRecorded = 0;
            Object audio = args[1];
            if (audio instanceof VideoEditedInfo) {
                videoToSendMessageObject = (VideoEditedInfo) audio;

                audioToSendPath = (String) args[2];
                ArrayList<Bitmap> keyframes = (ArrayList<Bitmap>) args[3];
                millisecondsRecorded = videoToSendMessageObject.estimatedDuration;

                if (videoTimelineView != null) {
                    videoTimelineView.setVideoPath(audioToSendPath);
                    videoTimelineView.setKeyframes(keyframes);
                    videoTimelineView.setVisibility(VISIBLE);
                    videoTimelineView.setMinProgressDiff(1000.0f / videoToSendMessageObject.estimatedDuration);
                    isRecordingStateChanged();
                }
                updateRecordInterface(RECORD_STATE_PREPARING, true);
                checkSendButton(false);
            } else {
                audioToSend = (TLRPC.TL_document) args[1];
                audioToSendPath = (String) args[2];
                boolean fromDraft = args.length >= 4 && (boolean) args[3];
                float left = args.length >= 5 ? (float) args[4] : 0.0f;
                float right = args.length >= 6 ? (float) args[5] : 1.0f;
                if (audioToSend != null) {
                    createRecordAudioPanel();
                    if (recordedAudioPanel == null) {
                        return;
                    }

                    TLRPC.TL_message message = new TLRPC.TL_message();
                    message.out = true;
                    message.id = 0;
                    message.peer_id = new TLRPC.TL_peerUser();
                    message.from_id = new TLRPC.TL_peerUser();
                    message.peer_id.user_id = message.from_id.user_id = UserConfig.getInstance(currentAccount).getClientUserId();
                    message.date = (int) (System.currentTimeMillis() / 1000);
                    message.message = "";
                    message.attachPath = audioToSendPath;
                    message.media = new TLRPC.TL_messageMediaDocument();
                    message.media.flags |= 3;
                    message.media.document = audioToSend;
                    message.flags |= TLRPC.MESSAGE_FLAG_HAS_MEDIA | TLRPC.MESSAGE_FLAG_HAS_FROM_ID;
                    audioToSendMessageObject = new MessageObject(UserConfig.selectedAccount, message, false, true);

                    recordedAudioPanel.setAlpha(1.0f);
                    recordedAudioPanel.setVisibility(VISIBLE);
                    recordDeleteImageView.setVisibility(VISIBLE);
                    recordDeleteImageView.setAlpha(0f);
                    recordDeleteImageView.setScaleY(0f);
                    recordDeleteImageView.setScaleX(0f);
                    double duration = 0;
                    for (int a = 0; a < audioToSend.attributes.size(); a++) {
                        TLRPC.DocumentAttribute attribute = audioToSend.attributes.get(a);
                        if (attribute instanceof TLRPC.TL_documentAttributeAudio) {
                            duration = attribute.duration;
                            break;
                        }
                    }

                    byte[] waveform = null;
                    for (int a = 0; a < audioToSend.attributes.size(); a++) {
                        TLRPC.DocumentAttribute attribute = audioToSend.attributes.get(a);
                        if (attribute instanceof TLRPC.TL_documentAttributeAudio) {
                            if (attribute.waveform == null || attribute.waveform.length == 0) {
                                attribute.waveform = MediaController.getWaveform(audioToSendPath);
                            }
                            waveform = attribute.waveform;
                            break;
                        }
                    }
                    if (fromDraft && attachButton != null) {
                        attachButton.setAlpha(attachButtonAlpha = 0f);
                        attachButton.setScaleX(0);
                        attachButton.setScaleY(0);
                    }
                    millisecondsRecorded = (long) (duration * 1000L);
                    audioTimelineView.init(audioToSendPath, duration, waveform, left, right);
                    checkSendButton(false);
                    if (fromDraft) {
                        createRecordCircle();
                        createRecordPanel();
                        createRecordAudioPanel();
                        recordInterfaceState = 1;
                        recordCircle.resetLockTranslation(false);
                        recordControlsCircleScale.set(recordCircle, 1f);
                        if (controlsView != null) {
                            controlsView.setVisibility(VISIBLE);
                            controlsView.setAlpha(1f);
                        }
                    }
                    updateRecordInterface(RECORD_STATE_PREPARING, !fromDraft);
                } else {
                    if (delegate != null) {
                        delegate.onMessageSend(null, true, 0, 0, 0);
                    }
                }
            }
        } else if (id == NotificationCenter.beforeAudioDidSent) {
            if (delegate != null) {
                delegate.beforeMessageSend(null, true, 0, 0);
            }
        } else if (id == NotificationCenter.audioRouteChanged) {
            if (parentActivity != null) {
                boolean frontSpeaker = (Boolean) args[0];
                parentActivity.setVolumeControlStream(frontSpeaker ? AudioManager.STREAM_VOICE_CALL : AudioManager.USE_DEFAULT_STREAM_TYPE);
            }
        } else if (id == NotificationCenter.messagePlayingProgressDidChanged) {
            Integer mid = (Integer) args[0];
            if (audioToSendMessageObject != null && MediaController.getInstance().isPlayingMessage(audioToSendMessageObject)) {
                MessageObject player = MediaController.getInstance().getPlayingMessageObject();
                audioToSendMessageObject.audioProgress = player.audioProgress;
                audioToSendMessageObject.audioProgressSec = player.audioProgressSec;
//                if (!recordedAudioSeekBar.isDragging()) {
//                    recordedAudioSeekBar.setProgress(audioToSendMessageObject.audioProgress);
//                }
            }
        } else if (id == NotificationCenter.featuredStickersDidLoad) {
            if (emojiButton != null) {
                emojiButton.invalidate();
            }
        } else if (id == NotificationCenter.messageReceivedByServer2) {
            Boolean scheduled = (Boolean) args[6];
            if (scheduled) {
                return;
            }
            long did = (Long) args[3];
            Integer newMsgId = (Integer) args[1];
            if (did == dialog_id && info != null && info.slowmode_seconds != 0 && !MessageObject.isEphemeralMessageId(newMsgId)) {
                TLRPC.Chat chat = accountInstance.getMessagesController().getChat(info.id);
                if (chat != null && !ChatObject.hasAdminRights(chat) && !ChatObject.isIgnoredChatRestrictionsForBoosters(chat)) {
                    info.slowmode_next_send_date = ConnectionsManager.getInstance(currentAccount).getCurrentTime() + info.slowmode_seconds;
                    info.flags |= 262144;
                    setSlowModeTimer(info.slowmode_next_send_date);
                }
            }
        } else if (id == NotificationCenter.sendingMessagesChanged) {
            if (info != null) {
                updateSlowModeText();
            }
        } else if (id == NotificationCenter.audioRecordTooShort) {
            audioToSend = null;
            videoToSendMessageObject = null;
            updateRecordInterface(RECORD_STATE_CANCEL_BY_TIME, true);
        } else if (id == NotificationCenter.updateBotMenuButton) {
            long botId = (long) args[0];
            TL_bots.BotMenuButton botMenuButton = (TL_bots.BotMenuButton) args[1];

            if (botId == dialog_id) {
                if (botMenuButton instanceof TL_bots.TL_botMenuButton) {
                    TL_bots.TL_botMenuButton webViewButton = (TL_bots.TL_botMenuButton) botMenuButton;
                    botMenuWebViewTitle = webViewButton.text;
                    botMenuWebViewUrl = webViewButton.url;
                    botMenuButtonType = BotMenuButtonType.WEB_VIEW;
                } else if (hasBotCommands || hasQuickReplies) {
                    botMenuButtonType = BotMenuButtonType.COMMANDS;
                } else {
                    botMenuButtonType = BotMenuButtonType.NO_BUTTON;
                }

                updateBotButton(false);
            }
        } else if (id == NotificationCenter.didUpdatePremiumGiftFieldIcon) {
            updateGiftButton(true);
        } else if (id == NotificationCenter.currentUserPremiumStatusChanged) {
            if (richDraftActive && sendButton != null) {
                sendButton.setLocked(!UserConfig.getInstance(currentAccount).isPremium());
            }
        }
    }

    public void onRequestPermissionsResultFragment(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == 2) {
            if (pendingLocationButton != null) {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    SendMessagesHelper.getInstance(currentAccount).sendCurrentLocation(pendingMessageObject, pendingLocationButton);
                }
                pendingLocationButton = null;
                pendingMessageObject = null;
            }
        }
    }

    public void checkStickresExpandHeight() {
        if (emojiView == null) {
            return;
        }
        final int origHeight = AndroidUtilities.displaySize.x > AndroidUtilities.displaySize.y ? keyboardHeightLand : keyboardHeight;
        int newHeight = originalViewHeight
            - AndroidUtilities.statusBarHeight
            - AndroidUtilities.navigationBarHeight
            - dp(6)
            - ActionBar.getCurrentActionBarHeight()
            - getHeight();
        if (searchingType == 2) {
            newHeight = Math.min(newHeight, dp(175) + origHeight);
        }
        int currentHeight = emojiView.getLayoutParams().height;
        if (currentHeight == newHeight) {
            return;
        }
        if (stickersExpansionAnim != null) {
            stickersExpansionAnim.cancel();
            stickersExpansionAnim = null;
        }
        stickersExpandedHeight = newHeight;
        if (currentHeight > newHeight) {
            Runnable animationEndRunnable = () -> {
                if (emojiView != null) {
                    if (windowInsetsInAppController == null) {
                        emojiView.getLayoutParams().height = stickersExpandedHeight;
                    }
                    emojiView.setLayerType(LAYER_TYPE_NONE, null);
                }
            };
            emojiView.setLayerType(LAYER_TYPE_HARDWARE, null);
            if (overrideKeyboardAnimation) {
                this.animationEndRunnable = animationEndRunnable;
            } else {
                AnimatorSet anims = new AnimatorSet();
                if (windowInsetsInAppController != null) {
                    anims.playTogether(
                            ObjectAnimator.ofInt(-(stickersExpandedHeight - origHeight)),
                            ObjectAnimator.ofInt(-(stickersExpandedHeight - origHeight))
                    );
                } else {
                    anims.playTogether(
                            ObjectAnimator.ofInt(this, roundedTranslationYProperty, -(stickersExpandedHeight - origHeight)),
                            ObjectAnimator.ofInt(emojiView, roundedTranslationYProperty, -(stickersExpandedHeight - origHeight))
                    );
                    ((ObjectAnimator) anims.getChildAnimations().get(0)).addUpdateListener(animation -> sizeNotifierLayout.invalidate());

                }
                anims.setDuration(300);
                anims.setInterpolator(CubicBezierInterpolator.DEFAULT);
                anims.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        stickersExpansionAnim = null;
                        animationEndRunnable.run();
                    }
                });
                stickersExpansionAnim = anims;
                anims.start();
            }
        } else {
            if (windowInsetsInAppController == null) {
                emojiView.getLayoutParams().height = stickersExpandedHeight;
            }
            sizeNotifierLayout.requestLayout();
            int start = 0, end = 0;
            if (messageEditText != null) {
                start = messageEditText.getSelectionStart();
                end = messageEditText.getSelectionEnd();
                messageEditText.setText(messageEditText.getText()); // dismiss action mode, if any
                messageEditText.setSelection(start, end);
            }
            AnimatorSet anims = new AnimatorSet();
            if (windowInsetsInAppController != null) {
                anims.playTogether(
                        ObjectAnimator.ofInt(-(stickersExpandedHeight - origHeight)),
                        ObjectAnimator.ofInt(-(stickersExpandedHeight - origHeight))
                );
            } else {
                anims.playTogether(
                        ObjectAnimator.ofInt(this, roundedTranslationYProperty, -(stickersExpandedHeight - origHeight)),
                        ObjectAnimator.ofInt(emojiView, roundedTranslationYProperty, -(stickersExpandedHeight - origHeight))
                );
                ((ObjectAnimator) anims.getChildAnimations().get(0)).addUpdateListener(animation -> sizeNotifierLayout.invalidate());
            }
            anims.setDuration(300);
            anims.setInterpolator(CubicBezierInterpolator.DEFAULT);
            anims.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    stickersExpansionAnim = null;
                    emojiView.setLayerType(LAYER_TYPE_NONE, null);
                }
            });
            stickersExpansionAnim = anims;
            emojiView.setLayerType(LAYER_TYPE_HARDWARE, null);
            anims.start();
        }

        if (windowInsetsInAppController != null) {
            windowInsetsInAppController.requestInAppKeyboardHeightIncludeNavbar(newHeight);
        }
    }

    public void setStickersExpanded(boolean expanded, boolean animated, boolean byDrag) {
        setStickersExpanded(expanded, animated, byDrag, true);
    }

    public void setStickersExpanded(boolean expanded, boolean animated, boolean byDrag, boolean stopAllHeavy) {
        if (adjustPanLayoutHelper != null && adjustPanLayoutHelper.animationInProgress() || waitingForKeyboardOpenAfterAnimation) {
            return;
        }
        if (emojiView == null || !byDrag && stickersExpanded == expanded) {
            return;
        }
        stickersExpanded = expanded;
        if (delegate != null) {
            delegate.onStickersExpandedChange();
        }
        final int origHeight = AndroidUtilities.displaySize.x > AndroidUtilities.displaySize.y ? keyboardHeightLand : keyboardHeight;
        if (stickersExpansionAnim != null) {
            stickersExpansionAnim.cancel();
            stickersExpansionAnim = null;
        }
        if (stickersExpanded) {
            if (stopAllHeavy) {
                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.stopAllHeavyOperations, 1);
            }
            originalViewHeight = sizeNotifierLayout.getHeight();
            stickersExpandedHeight = originalViewHeight
                - AndroidUtilities.statusBarHeight
                - AndroidUtilities.navigationBarHeight
                - dp(6)
                - ActionBar.getCurrentActionBarHeight()
                - getHeight();
            if (searchingType == 2) {
                stickersExpandedHeight = Math.min(stickersExpandedHeight, dp(175) + origHeight);
            }
            if (windowInsetsInAppController == null) {
                emojiView.getLayoutParams().height = stickersExpandedHeight;
            }
            sizeNotifierLayout.requestLayout();
            if (shouldDrawBackground) {
                sizeNotifierLayout.setForeground(new ScrimDrawable());
            }
            int start = 0, end = 0;
            if (messageEditText != null) {
                start = messageEditText.getSelectionStart();
                end = messageEditText.getSelectionEnd();
                messageEditText.setText(messageEditText.getText()); // dismiss action mode, if any
                messageEditText.setSelection(start, end);
            }
            if (animated) {
                AnimatorSet anims = new AnimatorSet();
                if (windowInsetsInAppController != null) {
                    anims.playTogether(
                            ObjectAnimator.ofInt(-(stickersExpandedHeight - origHeight)),
                            ObjectAnimator.ofInt(-(stickersExpandedHeight - origHeight)),
                            ObjectAnimator.ofFloat(stickersArrow, "animationProgress", 1)
                    );
                } else {
                    anims.playTogether(
                            ObjectAnimator.ofInt(this, roundedTranslationYProperty, -(stickersExpandedHeight - origHeight)),
                            ObjectAnimator.ofInt(emojiView, roundedTranslationYProperty, -(stickersExpandedHeight - origHeight)),
                            ObjectAnimator.ofFloat(stickersArrow, "animationProgress", 1)
                    );
                }
                anims.setDuration(300);
                anims.setInterpolator(CubicBezierInterpolator.DEFAULT);
                if (windowInsetsInAppController == null) {
                    ((ObjectAnimator) anims.getChildAnimations().get(0)).addUpdateListener(animation -> {
                        stickersExpansionProgress = Math.abs(getTranslationY() / (-(stickersExpandedHeight - origHeight)));
                        sizeNotifierLayout.invalidate();
                    });
                }

                anims.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        stickersExpansionAnim = null;
                        emojiView.setLayerType(LAYER_TYPE_NONE, null);
                        notificationsLocker.unlock();
                    }
                });
                stickersExpansionAnim = anims;
                emojiView.setLayerType(LAYER_TYPE_HARDWARE, null);
                notificationsLocker.lock();
                stickersExpansionProgress = 0f;
                sizeNotifierLayout.invalidate();
                anims.start();
            } else {
                stickersExpansionProgress = 1;
                if (windowInsetsInAppController == null) {
                    setTranslationY(-(stickersExpandedHeight - origHeight));
                    emojiView.setTranslationY(-(stickersExpandedHeight - origHeight));
                }
                if (stickersArrow != null) {
                    stickersArrow.setAnimationProgress(1);
                }
            }

            if (windowInsetsInAppController != null) {
                windowInsetsInAppController.requestInAppKeyboardHeightIncludeNavbar(stickersExpandedHeight);
            }

        } else {
            if (stopAllHeavy) {
                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.startAllHeavyOperations, 1);
            }
            if (animated) {
                closeAnimationInProgress = true;
                AnimatorSet anims = new AnimatorSet();
                if (windowInsetsInAppController != null) {
                    anims.playTogether(
                            ObjectAnimator.ofInt(0),
                            ObjectAnimator.ofInt(0),
                            ObjectAnimator.ofFloat(stickersArrow, "animationProgress", 0)
                    );
                } else {
                    anims.playTogether(
                            ObjectAnimator.ofInt(this, roundedTranslationYProperty, 0),
                            ObjectAnimator.ofInt(emojiView, roundedTranslationYProperty, 0),
                            ObjectAnimator.ofFloat(stickersArrow, "animationProgress", 0)
                    );
                }
                anims.setDuration(300);
                anims.setInterpolator(CubicBezierInterpolator.DEFAULT);
                if (windowInsetsInAppController == null) {
                    ((ObjectAnimator) anims.getChildAnimations().get(0)).addUpdateListener(animation -> {
                        stickersExpansionProgress = getTranslationY() / (-(stickersExpandedHeight - origHeight));
                        sizeNotifierLayout.invalidate();
                    });
                }
                anims.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        closeAnimationInProgress = false;
                        stickersExpansionAnim = null;
                        if (emojiView != null) {
                            if (windowInsetsInAppController == null) {
                                emojiView.getLayoutParams().height = origHeight;
                            }
                            emojiView.setLayerType(LAYER_TYPE_NONE, null);
                        }
                        if (sizeNotifierLayout != null) {
                            sizeNotifierLayout.requestLayout();
                            sizeNotifierLayout.setForeground(null);
                            sizeNotifierLayout.setWillNotDraw(false);
                        }
                        if (keyboardVisible && isPopupShowing()) {
                            showPopup(0, currentPopupContentType);
                        }
                        if (onEmojiSearchClosed != null) {
                            onEmojiSearchClosed.run();
                            onEmojiSearchClosed = null;
                        }
                        notificationsLocker.unlock();
                    }
                });
                stickersExpansionProgress = 1f;
                sizeNotifierLayout.invalidate();
                stickersExpansionAnim = anims;
                emojiView.setLayerType(LAYER_TYPE_HARDWARE, null);
                notificationsLocker.lock();
                anims.start();
            } else {
                stickersExpansionProgress = 0;
                if (windowInsetsInAppController == null) {
                    setTranslationY(0);
                    emojiView.setTranslationY(0);
                    emojiView.getLayoutParams().height = origHeight;
                }
                sizeNotifierLayout.requestLayout();
                sizeNotifierLayout.setForeground(null);
                sizeNotifierLayout.setWillNotDraw(false);
                if (stickersArrow != null) {
                    stickersArrow.setAnimationProgress(0);
                }
            }

            if (windowInsetsInAppController != null) {
                windowInsetsInAppController.requestInAppKeyboardHeightIncludeNavbar(origHeight);
            }
        }
        if (expandStickersButton != null) {
            if (stickersExpanded) {
                expandStickersButton.setContentDescription(getString("AccDescrCollapsePanel", R.string.AccDescrCollapsePanel));
            } else {
                expandStickersButton.setContentDescription(getString("AccDescrExpandPanel", R.string.AccDescrExpandPanel));
            }
        }
    }

    public boolean swipeToBackEnabled() {
        if (recordingAudioVideo) {
            return false;
        }
        if (isInVideoMode() && recordedAudioPanel != null && recordedAudioPanel.getVisibility() == View.VISIBLE) {
            return false;
        }
        if (hasBotWebView() && botCommandsMenuButton.isOpened() || BaseFragment.hasSheets(parentFragment)) {
            return false;
        }
        return true;
    }

    public int getHeightWithTopView() {
        int h = getMeasuredHeight();
        if (topView != null && topView.getVisibility() == View.VISIBLE) {
            h -= (1f - getTopViewEnterProgress()) * topView.getLayoutParams().height;
        }
        return h;
    }

    public void setAdjustPanLayoutHelper(AdjustPanLayoutHelper adjustPanLayoutHelper) {
        this.adjustPanLayoutHelper = adjustPanLayoutHelper;
    }

    public AdjustPanLayoutHelper getAdjustPanLayoutHelper() {
        return adjustPanLayoutHelper;
    }

    public boolean panelAnimationInProgress() {
        return panelAnimation != null;
    }

    public float getTopViewTranslation() {
        if (topView == null || topView.getVisibility() == View.GONE) {
            return 0;
        }
        return topView.getTranslationY();
    }

    public int getAnimatedTop() {
        return animatedTop;
    }

    public void checkAnimation() {

    }

    private class ScrimDrawable extends Drawable {

        private Paint paint;

        public ScrimDrawable() {
            paint = new Paint();
            paint.setColor(0);
        }

        @Override
        public void draw(Canvas canvas) {
            if (emojiView == null) {
                return;
            }
            paint.setAlpha(Math.round(102 * stickersExpansionProgress));
            canvas.drawRect(0, 0, getWidth(), emojiView.getY() - getHeight() + Theme.chat_composeShadowDrawable.getIntrinsicHeight() + (messageEditText == null ? 0 : messageEditText.getOffsetY()), paint);
        }

        @Override
        public void setAlpha(int alpha) {

        }

        @Override
        public void setColorFilter(ColorFilter colorFilter) {

        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSPARENT;
        }
    }

    private class SlideTextView extends View {

        TextPaint grayPaint;
        TextPaint bluePaint;

        Paint arrowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        String slideToCancelString;
        String cancelString;

        float slideToCancelWidth;
        float cancelWidth;
        float cancelToProgress;
        float slideProgress;

        float slideToAlpha;
        float cancelAlpha;

        float xOffset = 0;
        boolean moveForward;
        long lastUpdateTime;

        int cancelCharOffset;

        Path arrowPath = new Path();

        StaticLayout slideToLayout;
        StaticLayout cancelLayout;

        private boolean pressed;
        private boolean externalFrameClock;
        public Rect cancelRect = new Rect();

        void setExternalFrameClock(boolean enabled) {
            externalFrameClock = enabled;
            lastUpdateTime = System.currentTimeMillis();
            invalidate();
        }

        void onExternalFrame() {
            if (externalFrameClock && cancelToProgress != 1f) invalidate();
        }

        Drawable selectableBackground;
        private int lastSize;

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_CANCEL || event.getAction() == MotionEvent.ACTION_UP) {
                setPressed(false);
            }
            if (cancelToProgress == 0 || !isEnabled()) {
                return false;
            }
            int x = (int) event.getX();
            int y = (int) event.getY();
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                pressed = cancelRect.contains(x, y);
                if (pressed) {
                    selectableBackground.setHotspot(x, y);
                    setPressed(true);
                }
                return pressed;
            } else if (pressed) {
                if (event.getAction() == MotionEvent.ACTION_MOVE && !cancelRect.contains(x, y)) {
                    setPressed(false);
                    return false;
                }
                if (event.getAction() == MotionEvent.ACTION_UP && cancelRect.contains(x, y)) {
                    onCancelButtonPressed();
                }
                return true;
            }
            return pressed;
        }

        public void onCancelButtonPressed() {
            if (hasRecordVideo && isInVideoMode()) {
                CameraController.getInstance().cancelOnInitRunnable(onFinishInitCameraRunnable);
                pendingCameraFront = null;
                delegate.needStartRecordVideo(5, true, 0, 0, voiceOnce ? 0x7FFFFFFF : 0, effectId, 0);
                sendButton.setEffect(effectId = 0);
            } else {
                delegate.needStartRecordAudio(0);
                MediaController.getInstance().stopRecording(0, false, 0, voiceOnce, 0);
            }
            audioToSend = null;
            audioToSendMessageObject = null;
            videoToSendMessageObject = null;
            millisecondsRecorded = 0;
            recordingAudioVideo = false;
            MediaDataController.getInstance(currentAccount).pushDraftVoiceMessage(dialog_id, parentFragment != null && parentFragment.isTopic ? parentFragment.getTopicId() : 0, null);
            updateRecordInterface(RECORD_STATE_CANCEL, true);
            checkSendButton(true);
        }


        boolean smallSize;

        public SlideTextView(@NonNull Context context) {
            super(context);
            smallSize = AndroidUtilities.displaySize.x <= dp(320);
            grayPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
            grayPaint.setTextSize(dp(smallSize ? 13 : 15));

            bluePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
            bluePaint.setTextSize(dp(15));

            bluePaint.setTypeface(AndroidUtilities.bold());

            arrowPaint.setColor(getThemedColor(Theme.key_glass_defaultIcon));
            arrowPaint.setStyle(Paint.Style.STROKE);
            arrowPaint.setStrokeWidth(dpf2(smallSize ? 1f : 1.6f));
            arrowPaint.setStrokeCap(Paint.Cap.ROUND);
            arrowPaint.setStrokeJoin(Paint.Join.ROUND);

            slideToCancelString = getString(R.string.SlideToCancel2);

            cancelString = getString("Cancel", R.string.Cancel).toUpperCase();

            cancelCharOffset = slideToCancelString.indexOf(cancelString);

            updateColors();
        }

        public void updateColors() {
            grayPaint.setColor(getThemedColor(Theme.key_chat_recordTime));
            bluePaint.setColor(getThemedColor(Theme.key_chat_recordVoiceCancel));
            slideToAlpha = grayPaint.getAlpha();
            cancelAlpha = bluePaint.getAlpha();
            selectableBackground = Theme.createSimpleSelectorCircleDrawable(dp(60), 0, ColorUtils.setAlphaComponent(getThemedColor(Theme.key_chat_recordVoiceCancel), 26));
            selectableBackground.setCallback(this);
        }

        @Override
        protected void drawableStateChanged() {
            super.drawableStateChanged();
            selectableBackground.setState(getDrawableState());
        }

        @Override
        public boolean verifyDrawable(Drawable drawable) {
            return selectableBackground == drawable || super.verifyDrawable(drawable);
        }

        @Override
        public void jumpDrawablesToCurrentState() {
            super.jumpDrawablesToCurrentState();
            if (selectableBackground != null) {
                selectableBackground.jumpToCurrentState();
            }
        }

        @SuppressLint("DrawAllocation")
        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            int currentSize = getMeasuredHeight() + (getMeasuredWidth() << 16);
            if (lastSize != currentSize) {
                lastSize = currentSize;
                slideToCancelWidth = grayPaint.measureText(slideToCancelString);
                cancelWidth = bluePaint.measureText(cancelString);
                lastUpdateTime = System.currentTimeMillis();

                int heightHalf = getMeasuredHeight() >> 1;
                arrowPath.reset();
                if (smallSize) {
                    arrowPath.setLastPoint(dpf2(2.5f), heightHalf - dpf2(3.12f));
                    arrowPath.lineTo(0, heightHalf);
                    arrowPath.lineTo(dpf2(2.5f), heightHalf + dpf2(3.12f));
                } else {
                    arrowPath.setLastPoint(dpf2(4f), heightHalf - dpf2(5f));
                    arrowPath.lineTo(0, heightHalf);
                    arrowPath.lineTo(dpf2(4f), heightHalf + dpf2(5f));
                }

                slideToLayout = new StaticLayout(slideToCancelString, grayPaint, (int) slideToCancelWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
                cancelLayout = new StaticLayout(cancelString, bluePaint, (int) cancelWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
            }
        }

        @Override
        protected void onDraw(Canvas canvas) {
            if (slideToLayout == null || cancelLayout == null || recordCircle == null) {
                return;
            }
            int w = cancelLayout.getWidth() + dp(16);

            grayPaint.setColor(getThemedColor(Theme.key_chat_recordTime));
            grayPaint.setAlpha((int) (slideToAlpha * (1f - cancelToProgress) * slideProgress));
            bluePaint.setAlpha((int) (cancelAlpha * cancelToProgress));
            arrowPaint.setColor(grayPaint.getColor());

            if (smallSize) {
                xOffset = dp(16);
            } else {
                long dt = (System.currentTimeMillis() - lastUpdateTime);
                lastUpdateTime = System.currentTimeMillis();
                if (cancelToProgress == 0 && slideProgress > 0.8f) {
                    if (moveForward) {
                        xOffset += (dp(3) / 250f) * dt;
                        if (xOffset > dp(6)) {
                            xOffset = dp(6);
                            moveForward = false;
                        }
                    } else {
                        xOffset -= (dp(3) / 250f) * dt;
                        if (xOffset < -dp(6)) {
                            xOffset = -dp(6);
                            moveForward = true;
                        }
                    }
                }
            }

            boolean enableTransition = cancelCharOffset >= 0;

            int slideX = (int) ((getMeasuredWidth() - slideToCancelWidth) / 2) + dp(5);
            int cancelX = (int) ((getMeasuredWidth() - cancelWidth) / 2);
            float offset = enableTransition ? slideToLayout.getPrimaryHorizontal(cancelCharOffset) : 0;
            float cancelDiff = enableTransition ? slideX + offset - cancelX : 0;
            float x = slideX + xOffset * (1f - cancelToProgress) * slideProgress - cancelDiff * cancelToProgress + dp(16);

            float offsetY = enableTransition ? 0 : cancelToProgress * dp(12);

            if (cancelToProgress != 1) {
                int slideDelta = (int) (-getMeasuredWidth() / 4 * (1f - slideProgress) + recordCircle.getTranslationX() * 0.3f);
                canvas.save();
                canvas.clipRect((recordTimerView == null ? 0 : recordTimerView.getLeftProperty()) + dp(4), 0, getMeasuredWidth(), getMeasuredHeight());
                canvas.save();
                canvas.translate((int) x - (smallSize ? dp(7) : dp(10)) + slideDelta, offsetY);
                canvas.drawPath(arrowPath, arrowPaint);
                canvas.restore();

                canvas.save();
                canvas.translate((int) x + slideDelta, (getMeasuredHeight() - slideToLayout.getHeight()) / 2f + offsetY);
                slideToLayout.draw(canvas);
                canvas.restore();
                canvas.restore();
            }

            float xi;
            float yi = (getMeasuredHeight() - cancelLayout.getHeight()) / 2f;
            if (!enableTransition) {
                yi -= (dp(12) - offsetY);
            }
            if (enableTransition) {
                xi = x + offset;
            } else {
                xi = cancelX;
            }
            cancelRect.set((int) xi, (int) yi, (int) (xi + cancelLayout.getWidth()), (int) (yi + cancelLayout.getHeight()));
            cancelRect.inset(-dp(16), -dp(16));
            if (cancelToProgress > 0) {
                selectableBackground.setBounds(
                        getMeasuredWidth() / 2 - w, getMeasuredHeight() / 2 - w,
                        getMeasuredWidth() / 2 + w, getMeasuredHeight() / 2 + w
                );
                selectableBackground.draw(canvas);

                canvas.save();
                canvas.translate(xi, yi);
                cancelLayout.draw(canvas);
                canvas.restore();
            } else {
                setPressed(false);
            }

            if (cancelToProgress != 1 && !externalFrameClock) {
                invalidate();
            }
        }

        @Keep
        public void setCancelToProgress(float cancelToProgress) {
            this.cancelToProgress = cancelToProgress;
        }

        @Keep
        public float getSlideToCancelWidth() {
            return slideToCancelWidth;
        }

        public void setSlideX(float v) {
            slideProgress = v;
        }
    }

    public class TimerView extends View {
        boolean isRunning;
        boolean stoppedInternal;
        String oldString;
        long startTime;
        long stopTime;
        long lastSendTypingTime;
        long externalElapsedMs;
        long lastDrawRealtimeMs;
        boolean externalFrameClock;

        SpannableStringBuilder replaceIn = new SpannableStringBuilder();
        SpannableStringBuilder replaceOut = new SpannableStringBuilder();
        SpannableStringBuilder replaceStable = new SpannableStringBuilder();

        StaticLayout inLayout;
        StaticLayout outLayout;

        float replaceTransition;

        TextPaint textPaint;
        final float replaceDistance = dp(15);
        float left;

        public TimerView(Context context) {
            super(context);
        }

        public void start(long milliseconds) {
            isRunning = true;
            startTime = System.currentTimeMillis() - milliseconds;
            externalElapsedMs = milliseconds;
            lastSendTypingTime = startTime;
            invalidate();
        }

        void setExternalFrameClock(boolean enabled) {
            externalFrameClock = enabled;
            lastDrawRealtimeMs = SystemClock.elapsedRealtime();
            invalidate();
        }

        void onExternalFrame(long durationMs) {
            if (!externalFrameClock) return;
            externalElapsedMs = durationMs;
            invalidate();
        }

        public void stop() {
            if (isRunning) {
                isRunning = false;
                if (startTime > 0) {
                    stopTime = System.currentTimeMillis();
                }
                invalidate();
            }
            lastSendTypingTime = 0;
        }

        @SuppressLint("DrawAllocation")
        @Override
        protected void onDraw(Canvas canvas) {
            if (textPaint == null) {
                textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
                textPaint.setTextSize(dp(15));
                textPaint.setTypeface(AndroidUtilities.bold());
                textPaint.setColor(getThemedColor(Theme.key_chat_recordTime));
            }
            long currentTimeMillis = System.currentTimeMillis();
            long t = isRunning
                    ? externalFrameClock ? externalElapsedMs : currentTimeMillis - startTime
                    : stopTime - startTime;
            long time = t / 1000;
            int ms = (int) (t % 1000L) / 10;

            if (isInVideoMode()) {
                if (t >= 59500 && !stoppedInternal) {
                    startedDraggingX = -1;
                    delegate.needStartRecordVideo(3, true, 0, 0, voiceOnce ? 0x7FFFFFFF : 0, effectId, 0);
                    sendButton.setEffect(effectId = 0);
                    stoppedInternal = true;
                }
            }

            if (isRunning && currentTimeMillis > lastSendTypingTime + 5000) {
                lastSendTypingTime = currentTimeMillis;
                MessagesController.getInstance(currentAccount).sendTyping(dialog_id, getThreadMessageId(), isInVideoMode() ? 7 : 1, 0);
            }

            String newString = AndroidUtilities.formatTimerDurationFast((int) time, ms);
            if (newString.length() >= 3 && oldString != null && oldString.length() >= 3 && newString.length() == oldString.length() && newString.charAt(newString.length() - 3) != oldString.charAt(newString.length() - 3)) {
                int n = newString.length();

                replaceIn.clear();
                replaceOut.clear();
                replaceStable.clear();
                replaceIn.append(newString);
                replaceOut.append(oldString);
                replaceStable.append(newString);

                int inLast = -1;
                int inCount = 0;
                int outLast = -1;
                int outCount = 0;


                for (int i = 0; i < n - 1; i++) {
                    if (oldString.charAt(i) != newString.charAt(i)) {
                        if (outCount == 0) {
                            outLast = i;
                        }
                        outCount++;

                        if (inCount != 0) {
                            EmptyStubSpan span = new EmptyStubSpan();
                            if (i == n - 2) {
                                inCount++;
                            }
                            replaceIn.setSpan(span, inLast, inLast + inCount, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            replaceOut.setSpan(span, inLast, inLast + inCount, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            inCount = 0;
                        }
                    } else {
                        if (inCount == 0) {
                            inLast = i;
                        }
                        inCount++;
                        if (outCount != 0) {
                            replaceStable.setSpan(new EmptyStubSpan(), outLast, outLast + outCount, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            outCount = 0;
                        }
                    }
                }

                if (inCount != 0) {
                    EmptyStubSpan span = new EmptyStubSpan();
                    replaceIn.setSpan(span, inLast, inLast + inCount + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    replaceOut.setSpan(span, inLast, inLast + inCount + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
                if (outCount != 0) {
                    replaceStable.setSpan(new EmptyStubSpan(), outLast, outLast + outCount, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }

                inLayout = new StaticLayout(replaceIn, textPaint, getMeasuredWidth(), Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
                outLayout = new StaticLayout(replaceOut, textPaint, getMeasuredWidth(), Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);

                replaceTransition = 1f;
            } else {
                if (replaceStable == null) {
                    replaceStable = new SpannableStringBuilder(newString);
                }
                if (replaceStable.length() == 0 || replaceStable.length() != newString.length()) {
                    replaceStable.clear();
                    replaceStable.append(newString);
                } else {
                    replaceStable.replace(replaceStable.length() - 1, replaceStable.length(), newString, newString.length() - 1 - (newString.length() - replaceStable.length()), newString.length());
                }
            }

            long drawRealtimeMs = SystemClock.elapsedRealtime();
            long drawDeltaMs = lastDrawRealtimeMs == 0L
                    ? 16L
                    : Math.min(50L, drawRealtimeMs - lastDrawRealtimeMs);
            lastDrawRealtimeMs = drawRealtimeMs;
            if (replaceTransition != 0) {
                replaceTransition -= drawDeltaMs / 116f;
                if (replaceTransition < 0f) {
                    replaceTransition = 0f;
                }
            }

            float y = getMeasuredHeight() / 2;
            float x = 0;

            if (replaceTransition == 0) {
                replaceStable.clearSpans();
                StaticLayout staticLayout = new StaticLayout(replaceStable, textPaint, getMeasuredWidth(), Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
                canvas.save();
                canvas.translate(x, y - staticLayout.getHeight() / 2f);
                staticLayout.draw(canvas);
                canvas.restore();
                left = x + staticLayout.getLineWidth(0);
            } else {
                if (inLayout != null) {
                    canvas.save();
                    textPaint.setAlpha((int) (255 * (1f - replaceTransition)));
                    canvas.translate(x, y - inLayout.getHeight() / 2f - (replaceDistance * replaceTransition));
                    inLayout.draw(canvas);
                    canvas.restore();
                }

                if (outLayout != null) {
                    canvas.save();
                    textPaint.setAlpha((int) (255 * replaceTransition));
                    canvas.translate(x, y - outLayout.getHeight() / 2f + (replaceDistance * (1f - replaceTransition)));
                    outLayout.draw(canvas);
                    canvas.restore();
                }

                canvas.save();
                textPaint.setAlpha(255);
                StaticLayout staticLayout = new StaticLayout(replaceStable, textPaint, getMeasuredWidth(), Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
                canvas.translate(x, y - staticLayout.getHeight() / 2f);
                staticLayout.draw(canvas);
                canvas.restore();
                left = x + staticLayout.getLineWidth(0);
            }

            oldString = newString;

            if ((isRunning || replaceTransition != 0) && !externalFrameClock) {
                invalidate();
            }
        }

        public void updateColors() {
            if (textPaint != null) {
                textPaint.setColor(getThemedColor(Theme.key_chat_recordTime));
            }
        }

        public float getLeftProperty() {
            return left;
        }

        public void reset() {
            isRunning = false;
            stopTime = startTime = 0;
            externalElapsedMs = 0;
            lastDrawRealtimeMs = 0;
            stoppedInternal = false;
        }
    }

    protected boolean pannelAnimationEnabled() {
        return true;
    }

    @Nullable
    public RecordCircle getRecordCircle() {
        return recordCircle;
    }

    int botCommandLastPosition = -1;
    int botCommandLastTop;

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int wasHeight = textFieldContainer.getMeasuredHeight();
        updateIosLayoutSeeds();
        if (isIosButtonPlacement()) {
            if (botCommandsMenuButton != null && botCommandsMenuButton.getVisibility() == VISIBLE) {
                botCommandsMenuButton.measure(widthMeasureSpec, heightMeasureSpec);
            }
            updateFieldLeftIos();
        } else if (botCommandsMenuButton != null && botCommandsMenuButton.getTag() != null) {
            botCommandsMenuButton.measure(widthMeasureSpec, heightMeasureSpec);
            if (isIosInputAppearance()) {
                int cursorDp = IOS_LEFT_EDGE_MARGIN_DP + DEFAULT_HEIGHT + iosGapDp + CAPSULE_INSET_DP;
                ((MarginLayoutParams) emojiButton.getLayoutParams()).leftMargin = dp(IOS_LEFT_EDGE_MARGIN_DP);
                if (deleteRichDraftButton != null) {
                    ((MarginLayoutParams) deleteRichDraftButton.getLayoutParams()).leftMargin = dp(IOS_LEFT_EDGE_MARGIN_DP);
                }
                ((MarginLayoutParams) botCommandsMenuButton.getLayoutParams()).leftMargin = dp(cursorDp);
                cursorDp += Math.round(botCommandsMenuButton.getMeasuredWidth() / AndroidUtilities.density)
                        + (isCompactInputSize() ? COMPACT_TEXT_INSET_DP : NO_ICON_TEXT_INSET_DP);
                if (messageEditText != null) {
                    ((MarginLayoutParams) messageEditText.getLayoutParams()).leftMargin = dp(cursorDp);
                }
                if (richDraftPreview != null) {
                    ((MarginLayoutParams) richDraftPreview.getLayoutParams()).leftMargin = dp(cursorDp - 8);
                }
            } else {
                ((MarginLayoutParams) botCommandsMenuButton.getLayoutParams()).leftMargin = dp(8);
                ((MarginLayoutParams) emojiButton.getLayoutParams()).leftMargin = dp(10) + botCommandsMenuButton.getMeasuredWidth();
                if (deleteRichDraftButton != null) {
                    ((MarginLayoutParams) deleteRichDraftButton.getLayoutParams()).leftMargin = dp(10) + botCommandsMenuButton.getMeasuredWidth();
                }
                if (messageEditText != null) {
                    ((MarginLayoutParams) messageEditText.getLayoutParams()).leftMargin = dp(57) + botCommandsMenuButton.getMeasuredWidth();
                }
                if (richDraftPreview != null) {
                    ((MarginLayoutParams) richDraftPreview.getLayoutParams()).leftMargin = dp(57) + botCommandsMenuButton.getMeasuredWidth();
                }
            }
        } else if (senderSelectView != null && senderSelectView.getVisibility() == View.VISIBLE) {
            int width = senderSelectView.getLayoutParams().width, height = senderSelectView.getLayoutParams().height;
            senderSelectView.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
            if (isIosInputAppearance()) {
                int cursorDp = IOS_LEFT_EDGE_MARGIN_DP + DEFAULT_HEIGHT + iosGapDp + CAPSULE_INSET_DP;
                ((MarginLayoutParams) emojiButton.getLayoutParams()).leftMargin = dp(IOS_LEFT_EDGE_MARGIN_DP);
                ((MarginLayoutParams) senderSelectView.getLayoutParams()).leftMargin = dp(cursorDp);
                cursorDp += SENDER_SELECT_WIDTH_DP + iosGapDp;
                if (deleteRichDraftButton != null) {
                    ((MarginLayoutParams) deleteRichDraftButton.getLayoutParams()).leftMargin = dp(IOS_LEFT_EDGE_MARGIN_DP);
                }
                if (messageEditText != null) {
                    ((MarginLayoutParams) messageEditText.getLayoutParams()).leftMargin = dp(cursorDp);
                }
                if (richDraftPreview != null) {
                    ((MarginLayoutParams) richDraftPreview.getLayoutParams()).leftMargin = dp(cursorDp);
                }
            } else {
                ((MarginLayoutParams) emojiButton.getLayoutParams()).leftMargin = dp(7) + width;
                if (deleteRichDraftButton != null) {
                    ((MarginLayoutParams) deleteRichDraftButton.getLayoutParams()).leftMargin = dp(7) + width;
                }
                if (messageEditText != null) {
                    ((MarginLayoutParams) messageEditText.getLayoutParams()).leftMargin = dp(54) + width;
                }
                if (richDraftPreview != null) {
                    ((MarginLayoutParams) richDraftPreview.getLayoutParams()).leftMargin = dp(54) + width;
                }
            }
        } else if (isIosInputAppearance()) {
            int cursorDp = IOS_LEFT_EDGE_MARGIN_DP + DEFAULT_HEIGHT + iosGapDp + CAPSULE_INSET_DP;
            ((MarginLayoutParams) emojiButton.getLayoutParams()).leftMargin = dp(IOS_LEFT_EDGE_MARGIN_DP);
            if (deleteRichDraftButton != null) {
                ((MarginLayoutParams) deleteRichDraftButton.getLayoutParams()).leftMargin = dp(IOS_LEFT_EDGE_MARGIN_DP);
            }
            if (messageEditText != null) {
                ((MarginLayoutParams) messageEditText.getLayoutParams()).leftMargin = dp(cursorDp + NO_ICON_TEXT_INSET_DP);
            }
            if (richDraftPreview != null) {
                ((MarginLayoutParams) richDraftPreview.getLayoutParams()).leftMargin = dp(cursorDp);
            }
        } else {
            ((MarginLayoutParams) emojiButton.getLayoutParams()).leftMargin = dp(3);
            if (deleteRichDraftButton != null) {
                ((MarginLayoutParams) deleteRichDraftButton.getLayoutParams()).leftMargin = dp(3);
            }
            if (messageEditText != null) {
                ((MarginLayoutParams) messageEditText.getLayoutParams()).leftMargin = dp(50);
            }
            if (richDraftPreview != null) {
                ((MarginLayoutParams) richDraftPreview.getLayoutParams()).leftMargin = dp(50);
            }
        }
        updateBotCommandsMenuContainerTopPadding();
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        if (botWebViewButton != null) {
            if (botCommandsMenuButton != null) {
                botWebViewButton.setMeasuredButtonWidth(botCommandsMenuButton.getMeasuredWidth());
            }
            botWebViewButton.getLayoutParams().height = getMeasuredHeight() - dp(2);
            measureChild(botWebViewButton, widthMeasureSpec, heightMeasureSpec);
        }
//        if (botWebViewMenuContainer != null) {
//            MarginLayoutParams params = (MarginLayoutParams) botWebViewMenuContainer.getLayoutParams();
//            params.bottomMargin = messageEditText == null ? 0 : messageEditText.getMeasuredHeight();
//            measureChild(botWebViewMenuContainer, widthMeasureSpec, heightMeasureSpec);
//        }

        checkUi_IslandTotalHeight();
        checkUi_TopViewVisibility();

        if (wasHeight > 0 && textFieldContainer.getMeasuredHeight() != wasHeight) {
            final int delta = textFieldContainer.getMeasuredHeight() - wasHeight;
            if (Math.abs(delta) >= dp(12)) {
                for (int i = 0; i < 2; ++i) {
                    final View view = i == 0 ? aiButton : richButton;
                    if (view.getVisibility() != VISIBLE) continue;
                    view.animate().cancel();
                    view.setTranslationY(view.getTranslationY() + delta);
                    view.animate()
                        .translationY(0)
                        .setInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT).setDuration(250)
                        .start();
                }
                if (aiHint != null) {
                    aiHint.animate().cancel();
                    aiHint.setTranslationY(aiHint.getTranslationY() + delta);
                    aiHint.animate()
                        .translationY(0)
                        .setInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT).setDuration(250)
                        .start();
                }
            }
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (botCommandLastPosition != -1 && botCommandsMenuContainer != null) {
            LinearLayoutManager layoutManager = (LinearLayoutManager) botCommandsMenuContainer.listView.getLayoutManager();
            if (layoutManager != null) {
                layoutManager.scrollToPositionWithOffset(botCommandLastPosition, botCommandLastTop);
            }
            botCommandLastPosition = -1;
        }
    }

    private void beginDelayedTransition() {
        animationParamsX.put(emojiButton, emojiButton.getX());
        if (messageEditText != null) {
            animationParamsX.put(messageEditText, messageEditText.getX());
        }
    }

    private LongSparseArray<TL_bots.BotInfo> lastBotInfo;

    public void setBotInfo(LongSparseArray<TL_bots.BotInfo> botInfo) {
        setBotInfo(botInfo, true);
    }

    public void setBotInfo(LongSparseArray<TL_bots.BotInfo> botInfo, boolean animate) {
        lastBotInfo = botInfo;

        if (botInfo.size() == 1 && botInfo.valueAt(0).user_id == dialog_id) {
            TL_bots.BotInfo info = botInfo.valueAt(0);
            TL_bots.BotMenuButton menuButton = info.menu_button;
            if (menuButton instanceof TL_bots.TL_botMenuButton) {
                TL_bots.TL_botMenuButton webViewButton = (TL_bots.TL_botMenuButton) menuButton;
                botMenuWebViewTitle = webViewButton.text;
                botMenuWebViewUrl = webViewButton.url;
                botMenuButtonType = BotMenuButtonType.WEB_VIEW;
            } else if (!info.commands.isEmpty()) {
                botMenuButtonType = BotMenuButtonType.COMMANDS;
            } else {
                botMenuButtonType = BotMenuButtonType.NO_BUTTON;
            }
        } else {
            botMenuButtonType = BotMenuButtonType.NO_BUTTON;
        }

        if (botCommandsAdapter != null) {
            botCommandsAdapter.setBotInfo(botInfo);
        }

        updateBotButton(animate);
        checkIsEphemeralMessage(animate);
    }

    public boolean botCommandsMenuIsShowing() {
        return botCommandsMenuButton != null && botCommandsMenuButton.isOpened();
    }

    public void hideBotCommands() {
        if (botCommandsMenuButton != null) {
            botCommandsMenuButton.setOpened(false);
        }

        if (hasBotWebView()) {
//            if (botWebViewMenuContainer != null) {
//                botWebViewMenuContainer.dismiss();
//            }
        } else {
            if (botCommandsMenuContainer != null) {
                botCommandsMenuContainer.dismiss();
            }
        }
    }

    public void setTextTransitionIsRunning(boolean b) {
        textTransitionIsRunning = b;
        sendButtonContainer.invalidate();
    }

    public float getTopViewHeight() {
        if (topView != null && topView.getVisibility() == View.VISIBLE) {
            return topView.getLayoutParams().height;
        }
        return 0;
    }

    public void runEmojiPanelAnimation() {
        AndroidUtilities.cancelRunOnUIThread(runEmojiPanelAnimation);
        runEmojiPanelAnimation.run();
    }

    public Drawable getStickersArrowDrawable() {
        return stickersArrow;
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        if (emojiView == null || emojiView.getVisibility() != View.VISIBLE || emojiView.getStickersExpandOffset() == 0) {
            super.dispatchDraw(canvas);
        } else {
            canvas.save();
            canvas.clipRect(0, dp(2), getMeasuredWidth(), getMeasuredHeight());
            canvas.translate(0, -emojiView.getStickersExpandOffset());
            super.dispatchDraw(canvas);
            canvas.restore();
        }
    }

    int getThemedColor(int key) {
        if (resourcesProvider != null) {
            return resourcesProvider.getColor(key);
        }
        return Theme.getColor(key);
    }

    private Paint getThemedPaint(String paintKey) {
        Paint paint = resourcesProvider != null ? resourcesProvider.getPaint(paintKey) : null;
        return paint != null ? paint : Theme.getThemePaint(paintKey);
    }

    public void setHorizontalPadding(float leftPadding, float rightPadding, float progress, boolean allowShare) {
        leftPadding *= (1f - progress);
        rightPadding *= (1f - progress);
        float s = 0.5f + 0.5f * progress;
        emojiButtonPaddingScale = s;
        emojiButtonPaddingAlpha = progress;
        updateEmojiButtonParams();
        emojiButton.setTranslationX(-leftPadding);
        messageTextPaddingTranslationX = -leftPadding - (messageEditText == null ? 0 : dp(40) + (senderSelectView != null && senderSelectView.getVisibility() == View.VISIBLE ? dp(18) : 0)) * (1f - progress);
        if (isStories) {
            messageTextPaddingTranslationX = 0;
        }
        if (recordDeleteImageView != null) {
            recordDeleteImageView.setTranslationX(-leftPadding);
        }
        if (recordCircle != null) {
            recordCircle.setTranslationX(rightPadding);
        }
        if (controlsView != null) {
            controlsView.setTranslationX(rightPadding);
        }
        if (recordTimeContainer != null) {
            recordTimeContainer.setTranslationX(-leftPadding);
        }
//        if (recordedAudioPlayButton != null) {
//            recordedAudioPlayButton.setTranslationX(-leftPadding);
//        }
//        if (recordedAudioTimeTextView != null) {
//            recordedAudioTimeTextView.setTranslationX(leftPadding);
//        }
        sendButtonContainer.setTranslationX(rightPadding);
        sendButtonContainer.setAlpha(progress);
        if (suggestButton != null) {
            suggestButton.setAlpha(suggestButton.getScaleX() > 0.7f ? progress : 0);
        }
        sendButtonEnabled = allowShare ? progress == 1f : true;
        attachLayoutPaddingTranslationX = rightPadding;
        attachLayoutPaddingAlpha = progress;
        updateAttachLayoutParams();
        updateMessageTextParams();
        float newPadding = leftPadding * (1f - progress);

        if (horizontalPadding != newPadding) {
            horizontalPadding = newPadding;
//            if (seekBarWaveform != null && recordedAudioSeekBar != null) {
//                recordedAudioSeekBar.setTranslationX(horizontalPadding);
//                recordedAudioSeekBar.invalidate();
//                seekBarWaveform.setSize((int) (recordedAudioSeekBar.getMeasuredWidth() - horizontalPadding * 2), recordedAudioSeekBar.getMeasuredHeight());
//            }
//            if (recordedAudioBackground != null) {
//                recordedAudioBackground.invalidate();
//            }
            if (audioTimelineView != null) {
                audioTimelineView.setTranslationX(horizontalPadding);
                audioTimelineView.invalidate();
            }
        }
        if (messageEditText != null) {
            float scale = isStories ? 1f : lerp(0.88f, 1f, progress);
            messageEditText.setPivotX(0);
            messageEditText.setPivotY(messageEditText.getMeasuredHeight() / 2f);
            messageEditText.setScaleX(scale);
            messageEditText.setScaleY(scale);
            messageEditText.setHintRightOffset(lerp(dp(60), 0, progress));
        }
    }

    private void updateMessageTextParams() {
        if (messageEditText != null) {
            messageEditText.setTranslationX(messageTextPaddingTranslationX + messageTextTranslationX);
        }
    }

    private void updateAttachLayoutParams() {
        updateAttachButtonTranslationX();
        if (attachLayout != null) {
            attachLayout.setTranslationX(isIosButtonPlacement() ? 0 : attachLayoutPaddingTranslationX + attachLayoutTranslationX);
            attachLayout.setAlpha(attachLayoutAlpha * attachLayoutPaddingAlpha);
            attachLayout.setVisibility(attachLayout.getAlpha() > 0 ? View.VISIBLE : View.GONE);
            if (attachButton != null && isStories) {
                attachButton.setAlpha(attachButtonAlpha * attachLayoutPaddingAlpha);
            }
        }
        if (scheduledButton != null) {
            scheduledButton.setTranslationX(scheduledButton.getTranslationX());
        }
    }

    private boolean isSendButtonEnabled() {
        return sendButtonEnabled && !animatorIsBlockedByStreaming.getValue();
    }

    private void updateAttachButtonTranslationX() {
        if (attachButton == null) return;
        if (isIosButtonPlacement()) {
            attachButton.setTranslationX(0);
            return;
        }
        attachButton.setTranslationX(attachLayoutPaddingTranslationX + attachLayoutTranslationX + (sendButton != null ? (
            -Math.max(0, sendButton.width() - dp(DEFAULT_HEIGHT + 12)) * sendButton.getAlpha()
        ) : 0));
    }

    private void updateEmojiButtonParams() {
        emojiButton.setScaleX(emojiButtonPaddingScale * emojiButtonScale);
        emojiButton.setScaleY(emojiButtonPaddingScale * emojiButtonScale);
        emojiButton.setAlpha(emojiButtonPaddingAlpha * emojiButtonAlpha);
    }

    public void setOverrideHint(CharSequence overrideHint) {
        setOverrideHint(overrideHint, false);
    }

    public void setOverrideHint(CharSequence overrideHint, boolean animated) {
        this.overrideHint = overrideHint;
        this.overrideHint2 = null;
        updateFieldHint(animated);
    }

    public void setOverrideHint(CharSequence overrideHint, CharSequence overrideHint2, boolean animated) {
        this.overrideHint = overrideHint;
        this.overrideHint2 = overrideHint2;
        updateFieldHint(animated);
    }

    public void setOverrideKeyboardAnimation(boolean overrideKeyboardAnimation) {
        this.overrideKeyboardAnimation = overrideKeyboardAnimation;
    }

    public void onOverrideAnimationEnd() {
        if (animationEndRunnable != null) {
            animationEndRunnable.run();
            animationEndRunnable = null;
        }
    }

    public int getStickersExpandedHeight() {
        return stickersExpandedHeight;
    }

    public void reset() {
        setStickersExpanded(false, true, false);
        showPopup(0, POPUP_CONTENT_EMOJI_KEYBOARD, false);
        if (getEditField() != null) {
            if (!TextUtils.isEmpty(getEditField().getText())) {
                getEditField().setText("");
            }
        }
        recordingAudioVideo = false;
        if (audioVideoSendButton != null) {
            audioVideoSendButton.setVisibility(View.VISIBLE);
        }
        recordIsCanceled = true;
        isRecordingStateChanged();
        cancelRecordInterfaceInternal();
        hideRecordedAudioPanelInternal();
        if (controlsView != null) {
            controlsView.setVisibility(GONE);
        }
        if (recordCircle != null) {
            recordCircle.setSendButtonInvisible();
        }
    }

    public enum BotMenuButtonType {
        NO_BUTTON,
        COMMANDS,
        WEB_VIEW
    }

    public void freezeEmojiView(boolean freeze) {
        emojiViewFrozen = freeze;
        if (emojiView != null) {
            emojiView.freeze(freeze);
        }
    }

    @SuppressLint("ViewConstructor")
    public static class SendButton extends View implements ItemOptions.ScrimView {
        public static final float INFINITE_LOADING = -3f;

        public final Theme.ResourcesProvider resourcesProvider;

        public int resId;

        private Drawable drawable;
        private Drawable inactiveDrawable;
        private Drawable drawableInverse;
        private final AnimatedEmojiDrawable.SwapAnimatedEmojiDrawable emojiDrawable;
        private float ephemeralFactor;
        private float sameWidthFactor;

        private long starsPrice;
        private int messagesCount;
        private boolean hidePrice;
        private final AnimatedTextView.AnimatedTextDrawable priceText;
        private final AnimatedFloat animatedPriceVisible = new AnimatedFloat(this, 0, 320, CubicBezierInterpolator.EASE_OUT_QUINT);

        public boolean center;

        private final Paint backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        public SendButton(Context context, int resId, Theme.ResourcesProvider resourcesProvider) {
            this(context, resId, resourcesProvider, false);
        }

        public void setEphemeralFactor(float factor) {
            if (ephemeralFactor != factor) {
                ephemeralFactor = factor;
                invalidate();
            }
        }

        public void setSameWidthFactor(float factor) {
            if (sameWidthFactor != factor) {
                sameWidthFactor = factor;
                invalidate();
            }
        }

        private boolean isNewDesignSendButton;

        public SendButton(Context context, int resId, Theme.ResourcesProvider resourcesProvider, boolean isNewDesignSendButton) {
            super(context);

            this.resId = resId;
            this.resourcesProvider = resourcesProvider;
            this.isNewDesignSendButton = isNewDesignSendButton;

            priceText = new AnimatedTextView.AnimatedTextDrawable();
            priceText.setTextSize(dp(15));
            priceText.setTypeface(AndroidUtilities.bold());
            priceText.setTextColor(0xFFFFFFFF);
            priceText.setGravity(Gravity.LEFT);
            priceText.setCallback(this);
            priceText.setOverrideFullWidth(AndroidUtilities.displaySize.x);

            drawable = context.getResources().getDrawable(resId).mutate();
            inactiveDrawable = context.getResources().getDrawable(resId).mutate();
            drawableInverse = context.getResources().getDrawable(resId).mutate();
            emojiDrawable = new AnimatedEmojiDrawable.SwapAnimatedEmojiDrawable(this, dp(14));

            loadingPaint.setStyle(Paint.Style.STROKE);
            loadingPaint.setStrokeWidth(dp(2));
            loadingPaint.setStrokeJoin(Paint.Join.ROUND);
            loadingPaint.setStrokeCap(Paint.Cap.ROUND);

            count.setCallback(this);
            count.setTextColor(0xFFFFFFFF);
            count.setTextSize(dp(12));
            count.setTypeface(AndroidUtilities.bold());
            count.setGravity(Gravity.CENTER);
        }

        private boolean locked;
        private Drawable lockIcon;
        private int lockIconColor;
        public void setLocked(boolean locked) {
            if (this.locked == locked) return;
            this.locked = locked;
            invalidate();
        }

        public void setResourceId(int resId) {
            if (this.resId != resId) {
                this.resId = resId;
                drawable = getContext().getResources().getDrawable(resId).mutate();
                inactiveDrawable = getContext().getResources().getDrawable(resId).mutate();
                drawableInverse = getContext().getResources().getDrawable(resId).mutate();
                invalidate();
            }
        }

        private int circleWidth = -1, circleHeight = -1;
        public void setCircleSize(int size) {
            this.circleWidth = size;
            this.circleHeight = size;
        }

        public void setCircleSize(int width, int height) {
            this.circleWidth = width;
            this.circleHeight = height;
        }

        public int getCircleWidth() {
            if (circleWidth >= 0)
                return circleWidth;
            return getMeasuredHeight() - dp(8);
        }

        public int getCircleHeight() {
            if (circleHeight >= 0)
                return circleHeight;
            return getMeasuredHeight() - dp(8);
        }

        @Override
        public void getBounds(RectF bounds) {
            final float w = getCircleWidth();
            final float h = getCircleHeight();
            final float right = getMeasuredWidth() - dp(4) - circlePadX;
            final float bottom = getMeasuredHeight() - dp(4) - circlePadY;
            bounds.set(right - w, bottom - h, right, bottom);
        }

        private int scrimViewBackgroundColor;
        private final Paint scrimViewBackgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        public void setScrimViewBackgroundColor(int color) {
            scrimViewBackgroundColor = color;
            scrimViewBackgroundPaint.setColor(color);
        }

        @Override
        public void drawScrim(Canvas canvas, float progress) {
            if (scrimViewBackgroundColor != 0) {
                scrimViewBackgroundPaint.setColor(scrimViewBackgroundColor);
                scrimViewBackgroundPaint.setAlpha((int) (Color.alpha(scrimViewBackgroundColor) * progress));

                final float openProgress = open.get();
                final float priceProgress = animatedPriceVisible.get();

                final float right, cy, h;
                if (newCounterPos) {
                    right = lerp(getMeasuredWidth() - getMeasuredHeight() / 2.0f, getMeasuredWidth() - dp(4), openProgress) - circlePadX;
                    h = getCircleHeight() * openProgress;
                    cy = getMeasuredHeight() - circlePadY - dp(4) - h / 2f;
                } else {
                    right = lerp(
                            lerp(getMeasuredWidth() - getMeasuredHeight() / 2.0f, getMeasuredWidth() - dp(4), openProgress) - circlePadX,
                            getMeasuredWidth() - dp(9),
                            priceProgress);
                    cy = lerp(
                            getMeasuredHeight() - circlePadY - dp(4) - getCircleHeight() / 2f,
                            getMeasuredHeight() - dp(24),
                            priceProgress);
                    h = lerp(getCircleHeight(), dp(32), priceProgress) * openProgress;
                }
                final float w = lerp(getCircleWidth(), dp(isNewDesignSendButton ? (10 + 10) : (11 + 11)) + priceText.getCurrentWidth(), priceProgress) * openProgress;

                if (openProgress > 0 && w > 0 && h > 0) {
                    AndroidUtilities.rectTmp.set(right - w, cy - h / 2f, right, cy + h / 2f);
                    AndroidUtilities.rectTmp.inset(-dp(4), -dp(4));
                    final float r = Math.min(w, h) / 2f + dp(4);
                    canvas.drawRoundRect(AndroidUtilities.rectTmp, r, r, scrimViewBackgroundPaint);
                }

                final float countScale = count.isNotEmpty() * (1.0f - priceProgress);
                if (countScale > 0) {
                    final float sz = Math.max(dp(9) + count.getCurrentWidth(), dp(18));
                    final float _cx, _cy;
                    if (newCounterPos) {
                        _cx = right - dp(50f);
                        _cy = cy - getCircleHeight() / 2f + sz / 2f;
                    } else {
                        _cx = getMeasuredWidth() - circlePadX - sz / 2f;
                        _cy = getMeasuredHeight() - circlePadY - sz / 2f;
                    }
                    canvas.drawCircle(_cx, _cy, (sz / 2f + dp(2)) * countScale * countBounceScale, scrimViewBackgroundPaint);
                }
            }
            draw(canvas);
        }

        private float circlePadX, circlePadY;
        public void setCirclePadding(float x, float y) {
            circlePadX = x;
            circlePadY = y;
        }

        private final ColoredImageSpan[] spans = new ColoredImageSpan[1];
        public void setStarsPrice(long price, int count) {
            setStarsPrice(price, count, true);
        }
        public void setStarsPrice(long price, int count, boolean animated) {
            if (starsPrice == price && messagesCount == count) return;
            starsPrice = price;
            messagesCount = count;
            if (price > 0) {
                priceText.setText(StarsIntroActivity.replaceStars("猸愶笍" + LocaleController.formatNumber(price * Math.max(1, messagesCount), ','), spans), animated);
            } else {
                priceText.setText("", animated);
            }
            if (!animated) {
                animatedPriceVisible.force(starsPrice > 0);
            } else {
                invalidate();
            }
        }

        @Override
        protected boolean verifyDrawable(@NonNull Drawable who) {
            return who == count || who == emojiDrawable || who == priceText || super.verifyDrawable(who);
        }

        public void setEffect(long effectId) {
            TLRPC.TL_availableEffect effect = MessagesController.getInstance(UserConfig.selectedAccount).getEffect(effectId);
            setEmoji(effect != null ? Emoji.getEmojiDrawable(effect.emoticon) : null);
        }

        public void setEmoji(Drawable drawable) {
            emojiDrawable.set(drawable, true);
        }

        public boolean isOpen() {
            return starsPrice > 0;
        }

        public boolean isInScheduleMode() {
            return false;
        }

        public boolean isInactive() {
            return false;
        }

        public boolean shouldDrawBackground() {
            return false;
        }

        public boolean shouldDrawInternalCircle() {
            return true;
        }

        public int getFillColor() {
            return Theme.getColor(Theme.key_chat_messagePanelSend, resourcesProvider);
        }

        public final AnimatedFloat open = new AnimatedFloat(this, 0, 420, CubicBezierInterpolator.EASE_OUT_QUINT);
        public final ButtonBounce bounce = new ButtonBounce(this);

        private boolean infiniteLoading;
        private final FastOutSlowInInterpolator loadingInterpolator = new FastOutSlowInInterpolator();

        private boolean loadingShown;
        private float loadingProgress;
        private final AnimatedFloat loadingAnimatedShown = new AnimatedFloat(this, 0, 420, CubicBezierInterpolator.EASE_OUT_QUINT);
        private final AnimatedFloat loadingAnimatedProgress = new AnimatedFloat(this, 0, 500, CubicBezierInterpolator.EASE_OUT_QUINT);

        private final Path path = new Path();
        private final Paint loadingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        private final AnimatedTextView.AnimatedTextDrawable count = new AnimatedTextView.AnimatedTextDrawable(true, true, true);
        private float countBounceScale = 1f;

        private final AnimatedFloat appear = new AnimatedFloat(this, 0, 320, CubicBezierInterpolator.EASE_OUT_QUINT);
        public void appear() {
            appear.force(0);
            invalidate();
        }

        @Override
        public void setPressed(boolean pressed) {
            super.setPressed(pressed);
            bounce.setPressed(pressed);
        }

        private Drawable ephemeralOutlineDrawable;

        @Override
        protected void onDraw(@NonNull Canvas canvas) {
            final int saved = canvas.save();

            if (!isNewDesignSendButton) {
                canvas.saveLayerAlpha(0, 0, getWidth(), getHeight(), 0xFF, Canvas.ALL_SAVE_FLAG);
            }
            updateColors();
            checkBackgroundRect();
            if (isNewDesignSendButton && shouldDrawInternalCircle()) {
                canvas.drawRoundRect(backgroundRect, dp(RADIUS), dp(RADIUS), backgroundPaint);
            }

            final boolean inactive = isInactive();
            final Drawable drawable = inactive ? this.inactiveDrawable : this.drawable;

            int x, y;
            if (isNewDesignSendButton) {
                x = Math.round(backgroundRect.right - backgroundRect.height() / 2f - drawable.getIntrinsicWidth() / 2f);
                y = Math.round(backgroundRect.top + backgroundRect.height() / 2f - drawable.getIntrinsicHeight() / 2f);
            } else {
                x = getMeasuredWidth() - getMeasuredHeight() / 2 - drawable.getIntrinsicWidth() / 2;
                y = (getMeasuredHeight() - drawable.getIntrinsicHeight()) / 2;
                if (center) {

                } else if (isInScheduleMode()) {
                    y -= dp(1);
                } else {
                    x += dp(2);
                }
            }

            final float loadingShown = this.loadingAnimatedShown.set(this.loadingShown);

            final float openProgress = open.set(isOpen());
            final float priceProgress = animatedPriceVisible.set(starsPrice > 0 && !hidePrice) * (1f - ephemeralFactor);
            final float appear = this.appear.set(1);
            if (openProgress < 1) {
                canvas.save();
                canvas.translate(-dp(24) * (1f - appear), dp(24) * (1f - appear));
                final float s = lerp(0.35f, 1.0f, appear);
                canvas.scale(s, s, x + drawable.getIntrinsicWidth() / 2f, y + drawable.getIntrinsicHeight() / 2f);
                canvas.rotate(60 * (1f - appear), x + drawable.getIntrinsicWidth() / 2f, y + drawable.getIntrinsicHeight() / 2f);
                drawable.setBounds(x, y, x + drawable.getIntrinsicWidth(), y + drawable.getIntrinsicHeight());
                drawable.setAlpha((int) (0xFF * (1.0f - priceProgress)));
                drawable.draw(canvas);
                canvas.restore();
            }
            final float right, cy, h;
            if (newCounterPos) {
                right = lerp(getMeasuredWidth() - getMeasuredHeight() / 2.0f, getMeasuredWidth() - dp(4), openProgress) - circlePadX;
                h = getCircleHeight() * openProgress;
                cy = getMeasuredHeight() - circlePadY - dp(4) - h / 2f;
            } else {
                right = lerp(
                    lerp(getMeasuredWidth() - getMeasuredHeight() / 2.0f, getMeasuredWidth() - dp(4), openProgress) - circlePadX,
                    getMeasuredWidth() - dp(9),
                    priceProgress);
                cy = lerp(
                    getMeasuredHeight() - circlePadY - dp(4) - getCircleHeight() / 2f,
                    getMeasuredHeight() - dp(24),
                    priceProgress);
                h = lerp(getCircleHeight(), dp(32), priceProgress) * openProgress;
            }

            final float wn = lerp(getCircleWidth(), dp(isNewDesignSendButton ? (10 + 10) : (11 + 11)) + priceText.getCurrentWidth(), priceProgress);
            final float w = lerp(wn, h, sameWidthFactor) * openProgress;
            final float wOffset = wn - w;

            setPivotX(right - w / 2.0f);
            setPivotY(cy);

            final float eS = lerp(1, 0.79f, ephemeralFactor);
            if (openProgress > 0) {
                canvas.save();
                path.rewind();
                final float r = Math.min(w, h) / 2.0f;
                AndroidUtilities.rectTmp.set(right - w, cy - h / 2.0f, right, cy + h / 2.0f);
                path.addRoundRect(AndroidUtilities.rectTmp, r, r, Path.Direction.CW);

                final float bCX = AndroidUtilities.rectTmp.centerX();
                final float bCY = AndroidUtilities.rectTmp.centerY();

                if (ephemeralFactor > 0) {
                    if (ephemeralOutlineDrawable == null) {
                        ephemeralOutlineDrawable = getContext().getResources().getDrawable(R.drawable.send_outline);
                    }
                    ephemeralOutlineDrawable.setColorFilter(backgroundPaint.getColor(), PorterDuff.Mode.MULTIPLY);
                    DrawableUtils.setBounds(ephemeralOutlineDrawable, bCX, bCY, Gravity.CENTER);
                    DrawableUtils.drawWithScale(canvas, ephemeralOutlineDrawable, ephemeralFactor);
                }

                canvas.scale(eS, eS, bCX, bCY);

                if (blurredBackgroundDrawable != null) {
                    AndroidUtilities.rectTmp.round(AndroidUtilities.rectTmp2);
                    AndroidUtilities.rectTmp2.inset(-dp(7), -dp(7));
                    blurredBackgroundDrawable.setBounds(AndroidUtilities.rectTmp2);
                    blurredBackgroundDrawable.draw(canvas);
                }

                if (!isNewDesignSendButton && shouldDrawInternalCircle()) {
                    canvas.drawPath(path, backgroundPaint);
                }
                canvas.clipPath(path);
                if (loadingShown > 0) {
                    loadingPaint.setColor(0xFFFFFFFF);
                    loadingPaint.setAlpha((int) (0xFF * loadingShown));
                    final float R = dp(8.66f);
                    AndroidUtilities.rectTmp.set(right - w / 2.0f - R, cy - R, right - w / 2.0f + R, cy + R);
                    if (infiniteLoading) {
                        final long t = System.currentTimeMillis() % 5400L;
                        float segment0 = Math.max(0, 1520 * t / 5400f - 20);
                        float segment1 = 1520 * t / 5400f;
                        for (int i = 0; i < 4; ++i) {
                            segment1 += loadingInterpolator.getInterpolation((t - i * 1350) / 667f) * 250;
                            segment0 += loadingInterpolator.getInterpolation((t - (667 + i * 1350)) / 667f) * 250;
                        }
                        canvas.drawArc(AndroidUtilities.rectTmp, segment0, segment1 - segment0, false, loadingPaint);
                    } else {
                        float startAngle = -90f;
                        startAngle += (System.currentTimeMillis() % 3000L / 1000f * 120f) % 360f;
                        canvas.drawArc(AndroidUtilities.rectTmp, startAngle, 360 * loadingAnimatedProgress.set(this.loadingProgress), false, loadingPaint);
                    }
                    canvas.save();
                    final float s = AndroidUtilities.lerp(1f, .6f, loadingShown);
                    canvas.scale(s, s, right - w / 2.0f, cy);
                    invalidate();
                }
                if (priceProgress > 0) {
                    if (newCounterPos) {
                        priceText.setBounds(right - priceText.getAnimateToWidth() - dp(11), cy - h / 2, right - dp(11), cy + h / 2);
                    } else if (isNewDesignSendButton) {
                        priceText.setBounds(backgroundRect.left + dp(10), backgroundRect.top, backgroundRect.right, backgroundRect.bottom);
                    } else {
                        priceText.setBounds(getMeasuredWidth() - priceText.getAnimateToWidth() - dp(9 + 11), getMeasuredHeight() - dp(48), getMeasuredWidth() - dp(9 + 11), getMeasuredHeight());
                    }
                    priceText.setAlpha((int) (0xFF * priceProgress * (1.0f - loadingShown)));
                    priceText.draw(canvas);
                }
                drawableInverse.setAlpha((int) (0xFF * (1f - loadingShown) * (1f - priceProgress)));
                if (circleWidth > 0) {
                    drawableInverse.setBounds((int) (right - w / 2.0f - drawableInverse.getIntrinsicWidth() / 2.0f), (int) (cy - drawableInverse.getIntrinsicHeight() / 2.0f), (int) (right - w / 2.0f + drawableInverse.getIntrinsicWidth() / 2.0f), (int) (cy + drawableInverse.getIntrinsicHeight() / 2.0f));
                } else {
                    drawableInverse.setBounds(x, y, x + drawable.getIntrinsicWidth(), y + drawable.getIntrinsicHeight());
                }
                drawableInverse.draw(canvas);
                if (loadingShown > 0) {
                    canvas.restore();
                }
                canvas.restore();
            }

            final float countScale = count.isNotEmpty() * (1.0f - priceProgress);
            // when locked, the badge cut-out is drawn in draw() so it can punch through the View background too
            if (!locked) {
                final float sz = Math.max(dp(9) + count.getCurrentWidth(), dp(18));
                final float _cx, _cy, o;
                if (newCounterPos) {
                    _cx = right - dp(50f) + wOffset;
                    _cy = cy - getCircleHeight() / 2f + sz / 2f;
                    o = dp(0.66f);
                } else {
                    _cx = getMeasuredWidth() - circlePadX - sz / 2f;
                    _cy = getMeasuredHeight() - circlePadY - sz / 2f;
                    o = 0;
                }

                count.setBounds((int) (_cx - sz / 2f), (int) (_cy - sz / 2f - o), (int) (_cx + sz / 2f), (int) (_cy + sz / 2f - o));
                if (countScale > 0) {
                    final float eS2 = lerp(1, 0.85f, ephemeralFactor);
                    canvas.save();
                    canvas.scale(eS2, eS2, _cx, _cy);
                    if (!isNewDesignSendButton) {
                        canvas.drawCircle(_cx, _cy, (sz / 2f + dp(2)) * countScale * countBounceScale, Theme.PAINT_CLEAR);
                        canvas.drawCircle(_cx, _cy, sz / 2f * countScale * countBounceScale, backgroundPaint);
                    }
                    count.setAlpha((int) (0xFF * countScale));
                    count.draw(canvas);
                    canvas.restore();
                }
            }

            if (countScale < 1) {
                final int r = dp(8);
                final int _cx = (int) lerp(getMeasuredWidth() - getCircleWidth() / 2.0f - circlePadX + dp(12), right - dp(2), priceProgress);
                final int _cy = (int) lerp(getMeasuredHeight() - getCircleHeight() / 2.0f - circlePadY + dp(10), getMeasuredHeight() - dp(12), priceProgress);

                emojiDrawable.setBounds(_cx - r, _cy - r, _cx + r, _cy + r);
                emojiDrawable.setAlpha((int) (0xFF * (1.0f - countScale)));
                emojiDrawable.draw(canvas);
            }

            if (!isNewDesignSendButton) {
                canvas.restore();
            }

            canvas.restoreToCount(saved);
            super.onDraw(canvas);
        }

        @Override
        public void draw(@NonNull Canvas canvas) {
            if (!locked) {
                super.draw(canvas);
                return;
            }
            canvas.saveLayerAlpha(-dp(6), -dp(6), getWidth(), getHeight(), 0xFF, Canvas.ALL_SAVE_FLAG);
            super.draw(canvas);

            final float sz = dp(18);
            final float _cx = backgroundRect.left + sz / 2f - dp(6);
            final float _cy = backgroundRect.top + sz / 2f - dp(6);
            canvas.drawCircle(_cx, _cy, sz / 2f + dp(2), Theme.PAINT_CLEAR);
            canvas.drawCircle(_cx, _cy, sz / 2f, backgroundPaint);
            if (lockIcon == null) {
                lockIcon = getContext().getResources().getDrawable(R.drawable.mini_switch_lock).mutate();
                lockIcon.setColorFilter(new PorterDuffColorFilter(lockIconColor = drawableColor, PorterDuff.Mode.SRC_IN));
            }
            if (lockIconColor != drawableColor) {
                lockIcon.setColorFilter(new PorterDuffColorFilter(lockIconColor = drawableColor, PorterDuff.Mode.SRC_IN));
            }
            lockIcon.setBounds((int) (_cx - dp(8)), (int) (_cy - dp(8)), (int) (_cx + dp(8)), (int) (_cy + dp(8)));
            lockIcon.draw(canvas);

            canvas.restore();
        }

        private BlurredBackgroundDrawable blurredBackgroundDrawable;
        private BlurredBackgroundColorProviderThemed actionBubbleColorProvider;

        public void setActionBubbleColorProvider(BlurredBackgroundColorProviderThemed actionBubbleColorProvider) {
            this.actionBubbleColorProvider = actionBubbleColorProvider;
        }

        public BlurredBackgroundColorProviderThemed getActionBubbleColorProvider() {
            return actionBubbleColorProvider;
        }

        public void setBlurredBackgroundDrawable(BlurredBackgroundDrawable blurredBackgroundDrawable) {
            this.blurredBackgroundDrawable = blurredBackgroundDrawable;
            this.blurredBackgroundDrawable.setRadius(dp(IOS_BUBBLE_RADIUS_DP));
            this.blurredBackgroundDrawable.setPadding(dp(4));
        }

        public boolean newCounterPos;

        public int width() {
            return width(getMeasuredHeight());
        }

        public int width(int h) {
            final float openProgress = isOpen() ? 1.0f : 0.0f;
            final float priceProgress = starsPrice > 0 ? 1.0f : 0.0f;
            return (int) lerp(circlePadX + getCircleWidth() + circlePadX, dp(9 + 9) + dp(isNewDesignSendButton ? (10 + 10) : (11 + 11)) + priceText.getAnimateToWidth(), priceProgress * openProgress);
        }

        public int height() {
            return this.height(getMeasuredHeight());
        }

        public int height(int h) {
            final float priceProgress = starsPrice > 0 ? 1.0f : 0.0f;
            return (int) lerp(circlePadY + getCircleHeight() + circlePadY, dp(32), priceProgress);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (getAlpha() <= 0f) { // for accessibility
                return false;
            }
            if (event.getAction() == MotionEvent.ACTION_DOWN && (event.getX() < getWidth() - width() || event.getY() < getHeight() - height())) {
                return false;
            }
            return super.onTouchEvent(event);
        }

        private int drawableColor, inactiveDrawableColor, drawableInverseColor;

        public int resolveSendIconColor(int themeColor) {
            if (ActionButtonStyle.getCurrentStyle() == ActionButtonStyle.ACCENT) {
                return isNewDesignSendButton ? Color.WHITE : themeColor;
            }
            if (shouldUseActionStyleColors()) {
                return ActionButtonStyle.resolveIconColor(resourcesProvider);
            }
            return themeColor;
        }

        private boolean shouldUseActionStyleColors() {
            return isNewDesignSendButton || shouldDrawBackground();
        }

        public void updateColors() {
            int color = resolveSendIconColor(isNewDesignSendButton ? Color.WHITE : Theme.getColor(Theme.key_chat_messagePanelSend, resourcesProvider));

                drawableColor = color;
                inactiveDrawableColor = c;
                drawableInverseColor = inverseColor;
                drawable.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN));
                inactiveDrawable.setColorFilter(new PorterDuffColorFilter(Color.argb(0xb4, Color.red(c), Color.green(c), Color.blue(c)), PorterDuff.Mode.SRC_IN));
                drawableInverse.setColorFilter(new PorterDuffColorFilter(inverseColor, PorterDuff.Mode.SRC_IN));
            }
            if (ActionButtonStyle.getCurrentStyle() != ActionButtonStyle.ACCENT && shouldUseActionStyleColors()) {
                backgroundPaint.setColor(ActionButtonStyle.resolveBackgroundColor(resourcesProvider));
            } else if (isNewDesignSendButton) {
                backgroundPaint.setColor(Theme.getColor(Theme.key_chat_messagePanelSend, resourcesProvider));
            } else if (shouldDrawBackground()) {
                backgroundPaint.setColor(getFillColor());
            } else {
                backgroundPaint.setColor(ColorUtils.setAlphaComponent(Color.WHITE, 75));
            }
        }

        public void setLoading(boolean shown, float progress) {
            if (loadingShown == shown && (!shown || Math.abs(loadingProgress - progress) < 0.01f) && infiniteLoading == Math.abs(progress - INFINITE_LOADING) < 0.01f)
                return;
            infiniteLoading = Math.abs(progress - INFINITE_LOADING) < 0.01f;
            if (!loadingShown && shown) {
                loadingAnimatedProgress.set(0, true);
            }
            loadingAnimatedShown.setDelay(shown && loadingAnimatedShown.get() >= 1f ? 650 : 0);
            loadingShown = shown;
            loadingProgress = !shown ? 1f : progress;
            invalidate();
        }

        public void setCount(int count, boolean animated) {
            this.count.setText(count <= 0 ? "" : "" + count, animated);
            invalidate();
        }

        public void copyTo(SendButton btn) {
            btn.isNewDesignSendButton = isNewDesignSendButton;
            btn.newCounterPos = newCounterPos;
            btn.count.setText(this.count.getText(), false);
            btn.countBounceScale = countBounceScale;
            btn.setEmoji(emojiDrawable.getDrawable());
            btn.setStarsPrice(starsPrice, messagesCount);
            btn.open.force(open.get());
            btn.animatedPriceVisible.force(animatedPriceVisible.get());
            btn.setCircleSize(circleWidth, circleHeight);
            btn.setCirclePadding(circlePadX, circlePadY);
        }

        private ValueAnimator bounceCountAnimator;
        public void bounceCount() {
            if (bounceCountAnimator != null) {
                bounceCountAnimator.cancel();
            }
            bounceCountAnimator = ValueAnimator.ofFloat(.9f, 1f);
            bounceCountAnimator.addUpdateListener(anm -> {
                countBounceScale = (float) anm.getAnimatedValue();
            });
            bounceCountAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    countBounceScale = 1f;
                }
            });
            bounceCountAnimator.setDuration(180);
            bounceCountAnimator.setInterpolator(new OvershootInterpolator());
            bounceCountAnimator.start();
        }

        /* * */

        private final RectF backgroundRect = new RectF();
        private static final int RADIUS = 19;

        private void checkBackgroundRect() {
            final float margin = dpf2(3);
            final float height = dpf2(38);
            final float width = lerp(
                Math.max(height, dpf2(10 + 10) + priceText.getCurrentWidth()),
                height, sameWidthFactor);
            backgroundRect.set(
                    getMeasuredWidth() - width - margin,
                    getMeasuredHeight() - height - margin,
                    getMeasuredWidth() - margin,
                    getMeasuredHeight() - margin
            );
        }
    }

    private final Paint gradientPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final LinearGradient clipGradient = new LinearGradient(0, 0, 0, 16, new int[] { 0xffffffff, 0x00ffffff }, new float[] { 0, 1 }, Shader.TileMode.CLAMP);
    private final Matrix clipMatrix = new Matrix();
    private final AnimatedFloat topGradientAlpha = new AnimatedFloat(this, 0, 280, CubicBezierInterpolator.EASE_OUT_QUINT);
    private final AnimatedFloat bottomGradientAlpha = new AnimatedFloat(this, 0, 280, CubicBezierInterpolator.EASE_OUT_QUINT);
    {
        gradientPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));
        gradientPaint.setShader(clipGradient);
    }

    public boolean drawMessageEditText(Canvas canvas, Utilities.Callback0Return<Boolean> drawChild) {
        float topAlpha = topGradientAlpha.set(messageEditText.canScrollVertically(-1));
        float bottomAlpha = bottomGradientAlpha.set(messageEditText.canScrollVertically(1));
        if (topAlpha <= 0 && bottomAlpha <= 0) {
            return drawChild.run();
        }
        canvas.saveLayerAlpha(0, 0, messageEditText.getX() + messageEditText.getMeasuredWidth() + dp(5), messageEditText.getY() + messageEditText.getMeasuredHeight() + dp(2), 0xFF, Canvas.ALL_SAVE_FLAG);
        final boolean result = drawChild.run();
        canvas.save();

        if (topAlpha > 0) {
            AndroidUtilities.rectTmp.set(
                    messageEditText.getX() - dp(5),
                    messageEditText.getY() + animatedTop - 1,
                    messageEditText.getX() + messageEditText.getMeasuredWidth() + dp(5),
                    messageEditText.getY() + animatedTop + dp(13)
            );
            clipMatrix.reset();
            clipMatrix.postScale(1f, AndroidUtilities.rectTmp.height() / 16f);
            clipMatrix.postTranslate(AndroidUtilities.rectTmp.left, AndroidUtilities.rectTmp.top);
            clipGradient.setLocalMatrix(clipMatrix);
            gradientPaint.setAlpha((int) (0xFF * topAlpha));
            canvas.drawRect(AndroidUtilities.rectTmp, gradientPaint);
        }

        if (bottomAlpha > 0) {
            AndroidUtilities.rectTmp.set(
                    messageEditText.getX() - dp(5),
                    messageEditText.getY() + messageEditText.getMeasuredHeight() - dp(13 + 2),
                    messageEditText.getX() + messageEditText.getMeasuredWidth() + dp(5),
                    messageEditText.getY() + messageEditText.getMeasuredHeight() + dp(2) + 1
            );
            clipMatrix.reset();
            clipMatrix.postScale(1f, AndroidUtilities.rectTmp.height() / 16f);
            clipMatrix.postRotate(180);
            clipMatrix.postTranslate(AndroidUtilities.rectTmp.left, AndroidUtilities.rectTmp.bottom);
            clipGradient.setLocalMatrix(clipMatrix);
            gradientPaint.setAlpha((int) (0xFF * bottomAlpha));
            canvas.drawRect(AndroidUtilities.rectTmp, gradientPaint);
        }

        canvas.restore();
        canvas.restore();
        return result;
    }

    public TLRPC.TL_textWithEntities getTextWithEntities() {
        final TLRPC.TL_textWithEntities text = new TLRPC.TL_textWithEntities();
        final CharSequence[] message = new CharSequence[]{ new SpannableStringBuilder(getEditText()) };
        text.entities = MediaDataController.getInstance(UserConfig.selectedAccount).getEntities(message, true);
        text.text = message[0].toString();
        return text;
    }

    private WindowInsetsInAppController windowInsetsInAppController;

    public void setInAppInsetsController(WindowInsetsInAppController inAppInsetsController) {
        this.windowInsetsInAppController = inAppInsetsController;
    }

    private ChatActivitySideControlsButtonsLayout sideButtons;
    public void setSideButtonsForAttach(ChatActivitySideControlsButtonsLayout sideButtons) {
        if (this.sideButtons == sideButtons) {
            return;
        }
        if (this.sideButtons != null) {
            this.sideButtons.showButton(ChatActivitySideControlsButtonsLayout.BUTTON_ATTACH, false, true);
        }
        this.sideButtons = sideButtons;
        if (attachButton == null || attachLayout == null || attachLayout.getVisibility() == VISIBLE) {
            return;
        }
        // the attach layout is hidden by the text: move its button into (or out of) the input
        final boolean captionNearAttach = sideButtons != null && isCaptionNearAttach();
        final boolean inInput = sideButtons != null && !captionNearAttach;
        if (sideButtons != null) {
            sideButtons.showButton(ChatActivitySideControlsButtonsLayout.BUTTON_ATTACH, captionNearAttach, true);
        }
        if (attachButtonAnimator != null) {
            attachButtonAnimator.cancel();
        }
        attachButtonAnimator = attachButton.animate()
            .alpha(attachButtonAlpha = inInput ? 1.0f : 0.0f)
            .scaleX(inInput ? 1.0f : 0.5f)
            .scaleY(inInput ? 1.0f : 0.5f)
            .setInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT)
            .setDuration(320);
        attachButtonAnimator.start();
    }

    private boolean isCaptionNearAttach() {
        return messageEditText != null && (!TextUtils.isEmpty(messageEditText.getCaption()) || messageEditText.isNearRightCaption(dp(DEFAULT_HEIGHT))) || LocaleController.isRTL;
    }

    public boolean isInTopViewVisibilityAnimating() {
        return animatorTopViewVisibility.isAnimating();
    }

    private static final int ANIMATOR_ID_INPUT_FIELD_HEIGHT = 0;
    private static final int ANIMATOR_ID_TOP_VIEW_VISIBILITY = 1;
    private static final int ANIMATOR_ID_BLOCKED_BY_BOT_TYPING = 2;
    private static final int ANIMATOR_ID_EPHEMERAL_MESSAGE_VISIBILITY = 3;

    private final FactorAnimator animatorInputFieldHeight = new FactorAnimator(ANIMATOR_ID_INPUT_FIELD_HEIGHT, this, ChatListItemAnimator.DEFAULT_INTERPOLATOR, ChatListItemAnimator.DEFAULT_DURATION);
    private final BoolAnimator animatorTopViewVisibility = new BoolAnimator(ANIMATOR_ID_TOP_VIEW_VISIBILITY, this, ChatListItemAnimator.DEFAULT_INTERPOLATOR, ChatListItemAnimator.DEFAULT_DURATION);
    private final BoolAnimator animatorIsBlockedByStreaming = new BoolAnimator(ANIMATOR_ID_BLOCKED_BY_BOT_TYPING, this, CubicBezierInterpolator.EASE_OUT_QUINT, 320);
    private final BoolAnimator animatorEphemeralMessageVisibility = new BoolAnimator(ANIMATOR_ID_EPHEMERAL_MESSAGE_VISIBILITY, this, CubicBezierInterpolator.EASE_OUT_QUINT, 320);

    @Override
    public void onFactorChanged(int id, float factor, float fraction, FactorAnimator callee) {
        // todo: notificationsLocker.lock(); notificationsLocker.unlock();

        if (id == ANIMATOR_ID_INPUT_FIELD_HEIGHT) {
            checkUi_IslandTotalHeight();
            checkUi_TopViewVisibility();
        } else if (id == ANIMATOR_ID_TOP_VIEW_VISIBILITY) {
            checkUi_IslandTotalHeight();
            checkUi_TopViewVisibility();
        } else if (id == ANIMATOR_ID_BLOCKED_BY_BOT_TYPING) {
            sendButtonBlockedByTypingView.setAlpha(factor);
            sendButtonBlockedByTypingView.setScaleX(lerp(0.5f, 1, factor));
            sendButtonBlockedByTypingView.setScaleY(lerp(0.5f, 1, factor));
            sendButtonBlockedByTypingView.setVisibility(factor > 0 ? View.VISIBLE : View.INVISIBLE);
        } else if (id == ANIMATOR_ID_EPHEMERAL_MESSAGE_VISIBILITY) {
            sendButtonContainer.setScaleX(lerp(1, 0.79f, factor));
            sendButtonContainer.setScaleY(lerp(1, 0.79f, factor));
            sendOutlineView.setScaleX(lerp(0.79f, 1, factor));
            sendOutlineView.setScaleY(lerp(0.79f, 1, factor));
            sendOutlineView.setVisibility(factor > 0 ? View.VISIBLE : View.GONE);
            sendOutlineView.setAlpha(factor);
            if (sendButton != null) {
                sendButton.setSameWidthFactor(factor);
            }
        }

        invalidate();
    }

    private void checkUi_TopViewVisibility() {
        final float visibility = animatorTopViewVisibility.getFloatValue();

        if (topView != null) {
            final float y = getMeasuredHeight() - animatorInputFieldHeight.getFactor();

            float replyFieldGapPx = isIosInputAppearance() ? dp(iosGapDp) : 0f;
            topView.setTranslationY(y - topView.getMeasuredHeight() * visibility - replyFieldGapPx);
            topView.setVisibility(visibility > 0 ? VISIBLE : GONE);
        }

        resizeForTopView(visibility > 0);
    }



    private float calculateIslandTotalHeight(boolean target) {
        final float fieldHeight = target ?
            animatorInputFieldHeight.getToFactor():
            animatorInputFieldHeight.getFactor();

        final float topViewVisibility = target ?
            (animatorTopViewVisibility.getValue() ? 1 : 0):
            animatorTopViewVisibility.getFloatValue();

        return fieldHeight + (topView != null ? topView.getMeasuredHeight() : 0) * topViewVisibility
                + (isIosInputAppearance() ? dp(iosGapDp) * topViewVisibility : 0);
    }


    private float currentIslandTotalHeight;
    private float currentIslandTotalHeightTarget;

    private void checkUi_IslandTotalHeight() {
        currentIslandTotalHeightTarget = calculateIslandTotalHeight(true);

        final float newIslandTotalHeight = calculateIslandTotalHeight(false);
        if (currentIslandTotalHeight != newIslandTotalHeight) {
            currentIslandTotalHeight = newIslandTotalHeight;
            onChangedIslandTotalHeight(newIslandTotalHeight);
        }
    }

    public float getIslandTotalHeight(boolean target) {
        return target ?
            currentIslandTotalHeightTarget:
            currentIslandTotalHeight;
    }

    protected void onChangedIslandTotalHeight(float newTotalHeight) {

    }

    private boolean isLiveComment;
    public void setLiveComment(boolean isLiveComment, boolean isAdmin) {
        if (this.isLiveComment == isLiveComment) return;
        this.isLiveComment = isLiveComment;
        attachButton.setVisibility(isLiveComment ? View.GONE : View.VISIBLE);
        if (isLiveComment) {
            AndroidUtilities.removeFromParent(notifyButton);
        }
        if (isLiveComment) {
            audioVideoSendButton.setVisibility(View.GONE);
        } else {
            reset();
        }
        if (!isLiveComment) {
            currentLimit = -1;
            if (captionLimitView != null) {
                captionLimitView.setVisibility(View.GONE);
            }
        }
        updateFieldRight(lastAttachVisible);
        checkSendButton(false);
    }

    private void proceedWithVideoRecording() {
        if (delegate == null || parentActivity == null) {
            pendingCameraFront = null;
            return;
        }
        if (!checkMenuPermissions(true)) {
            pendingCameraFront = null;
            return;
        }
        isInVideoMode = true;
        // initialize state that would have been set in recordAudioVideoRunnable
        delegate.onPreAudioVideoRecord();
        calledRecordRunnable = true;
        recordAudioVideoRunnableStarted = false;
        if (slideText != null) {
            slideText.setAlpha(1.0f);
            slideText.setTranslationY(0);
        }
        audioToSendPath = null;
        audioToSend = null;
        // initialize camera and start recording
        if (!CameraController.getInstance().isCameraInitied()) {
            CameraController.getInstance().initCamera(onFinishInitCameraRunnable);
        } else {
            onFinishInitCameraRunnable.run();
        }
        if (!recordingAudioVideo) {
            recordingAudioVideo = true;
            updateRecordInterface(RECORD_STATE_ENTER, true);
            if (recordCircle != null) {
                recordCircle.showWaves(false, false);
            }
            if (recordTimerView != null) {
                recordTimerView.reset();
            }
        }
        // setup recording mode
        delegate.onSwitchRecordMode(isInVideoMode());
        setRecordVideoButtonVisible(isInVideoMode(), true);
        if (!NyaConfig.disableVibration.Bool()) performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
        sendButtonVisible = true;
        startLockTransition();
    }

    private void showCameraSelectionPopup(View anchorView, Runnable onFrontSelected, Runnable onRearSelected) {
        if (parentActivity == null) {
            return;
        }

        final boolean[] cameraSelected = {false};

        ActionBarPopupWindow.ActionBarPopupWindowLayout popupLayout = new ActionBarPopupWindow.ActionBarPopupWindowLayout(parentActivity, R.drawable.popup_fixed_alert4, resourcesProvider);
        popupLayout.setAnimationEnabled(false);

        // front camera option
        ActionBarMenuSubItem frontItem = new ActionBarMenuSubItem(getContext(), true, false);
        frontItem.setTextAndIcon(getString(R.string.CameraInVideoMessagesFront), R.drawable.msg_openprofile_solar);
        frontItem.setOnClickListener(v -> {
            cameraSelected[0] = true;
            if (cameraSelectionPopup != null && cameraSelectionPopup.isShowing()) {
                cameraSelectionPopup.dismiss();
            }
            onFrontSelected.run();
        });
        frontItem.setMinimumWidth(dp(196));
        popupLayout.addView(frontItem, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 48, LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT, 0, 0, 0, 0));
        // rear camera option
        ActionBarMenuSubItem rearItem = new ActionBarMenuSubItem(getContext(), false, true);
        rearItem.setTextAndIcon(getString(R.string.CameraInVideoMessagesRear), R.drawable.msg_rear_camera_solar);
        rearItem.setOnClickListener(v -> {
            cameraSelected[0] = true;
            if (cameraSelectionPopup != null && cameraSelectionPopup.isShowing()) {
                cameraSelectionPopup.dismiss();
            }
            onRearSelected.run();
        });
        rearItem.setMinimumWidth(dp(196));
        popupLayout.addView(rearItem, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 48, LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT, 0, 48, 0, 0));
        popupLayout.updateRadialSelectors();

        cameraSelectionPopup = new ActionBarPopupWindow(popupLayout, LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT);
        cameraSelectionPopup.setAnimationEnabled(false);
        cameraSelectionPopup.setAnimationStyle(R.style.PopupContextAnimation2);
        cameraSelectionPopup.setOutsideTouchable(true);
        cameraSelectionPopup.setClippingEnabled(true);
        cameraSelectionPopup.setInputMethodMode(ActionBarPopupWindow.INPUT_METHOD_NOT_NEEDED);
        cameraSelectionPopup.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_UNSPECIFIED);
        cameraSelectionPopup.getContentView().setFocusableInTouchMode(true);
        cameraSelectionPopup.setOnDismissListener(() -> {
            if (!cameraSelected[0]) {
                // reset state only when popup is dismissed without selection
                calledRecordRunnable = false;
                pendingCameraFront = null;
            }
            cameraSelectionPopup = null;
        });

        popupLayout.measure(MeasureSpec.makeMeasureSpec(dp(1000), MeasureSpec.AT_MOST), MeasureSpec.makeMeasureSpec(dp(1000), MeasureSpec.AT_MOST));
        cameraSelectionPopup.setFocusable(true);
        int[] location = new int[2];
        anchorView.getLocationInWindow(location);
        int popupWidth = popupLayout.getMeasuredWidth();
        int popupHeight = popupLayout.getMeasuredHeight();

        int y = location[1] - popupHeight - dp(2);
        if (y < 0) {
            y = location[1] + anchorView.getMeasuredHeight() + dp(2);
        }
        int x = location[0] + anchorView.getMeasuredWidth() - popupWidth + dp(8);

        if (AndroidUtilities.displaySize.x > 0) {
            x = Math.max(0, Math.min(x, AndroidUtilities.displaySize.x - popupWidth));
        }
        if (AndroidUtilities.displaySize.y > 0) {
            y = Math.max(0, Math.min(y, AndroidUtilities.displaySize.y - popupHeight));
        }
        cameraSelectionPopup.showAtLocation(anchorView, Gravity.LEFT | Gravity.TOP, x, y);
        cameraSelectionPopup.dimBehind();
        if (!NyaConfig.disableVibration.Bool()) performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
    }

    public static void disableNewLines(EditText editText) {
        InputFilter noNewLinesFilter = (source, start, end, dest, dstart, dend) -> {
            for (int i = start; i < end; i++) {
                char c = source.charAt(i);
                if (c == '\n' || c == '\r') {
                    StringBuilder result = new StringBuilder(end - start);
                    for (int j = start; j < end; j++) {
                        char ch = source.charAt(j);
                        if (ch != '\n' && ch != '\r') {
                            result.append(ch);
                        }
                    }
                    return result;
                }
            }
            return null;
        };

        InputFilter[] oldFilters = editText.getFilters();
        if (oldFilters == null) {
            editText.setFilters(new InputFilter[] {noNewLinesFilter});
            return;
        }

        InputFilter[] newFilters = Arrays.copyOf(oldFilters, oldFilters.length + 1);
        newFilters[oldFilters.length] = noNewLinesFilter;
        editText.setFilters(newFilters);
    }
}