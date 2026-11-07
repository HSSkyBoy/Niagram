package org.telegram.ui.Components.Reactions;

import static org.telegram.messenger.AndroidUtilities.dp;
import static org.telegram.messenger.AndroidUtilities.lerp;
import static org.telegram.messenger.AndroidUtilities.replaceTags;
import static org.telegram.messenger.LocaleController.formatPluralString;
import static org.telegram.messenger.LocaleController.getString;
import static org.telegram.ui.Components.Reactions.ReactionsUtils.addReactionToEditText;
import static org.telegram.ui.Components.Reactions.ReactionsUtils.createAnimatedEmojiSpan;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.Editable;
import android.text.Layout;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.math.MathUtils;

import me.vkryl.android.animator.BoolAnimator;
import me.vkryl.android.animator.FactorAnimator;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.browser.Browser;
import org.telegram.tgnet.TLRPC;
import org.telegram.tgnet.tl.TL_stars;
import org.telegram.tgnet.tl.TL_stories;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AdjustPanLayoutHelper;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.INavigationLayout;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeBind;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.SlideIntChooseView;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Components.AnimatedEmojiDrawable;
import org.telegram.ui.Components.AnimatedEmojiSpan;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.CubicBezierInterpolator;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalRecyclerView;
import org.telegram.ui.SelectAnimatedEmojiDialog;
import org.telegram.utils.glass.BaseFragmentSimpleGlass;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

public class ChatCustomReactionsEditActivity extends BaseFragment implements NotificationCenter.NotificationCenterDelegate {

    public final static int SELECT_TYPE_NONE = 2,
            SELECT_TYPE_SOME = 1,
            SELECT_TYPE_ALL = 0;

    private boolean paid;

    private SelectAnimatedEmojiDialog selectAnimatedEmojiDialog;
    private FrameLayout bottomDialogLayout;
    private BackSpaceButtonView backSpaceButtonView;
    private TextCheckCell enableReactionsCell;
    private TextInfoPrivacyCell reactionsInfoCell;
    private CustomReactionEditText editText;
    private SlideIntChooseView slideView;
    private TextCheckCell paidCheckCell;
    private UpdateReactionsButton actionButton;
    private FrameLayout actionButtonContainer;
    private ImageView actionButtonContainerGradient;
    private int keyboardHeight;
    private int bottomInset;
    private final Paint bottomInsetPaint = new Paint();
    private UniversalRecyclerView scrollView;
    private final ArrayList<View> reactionSettingsViews = new ArrayList<>();
    private final ArrayList<Boolean> reactionSettingsShadows = new ArrayList<>();

    private final HashMap<Long, AnimatedEmojiSpan> selectedEmojisMap = new LinkedHashMap<>();
    private final List<Long> selectedEmojisIds = new ArrayList<>();
    private final HashMap<Long, AnimatedEmojiSpan> initialSelectedEmojis = new LinkedHashMap<>();
    private final List<TLRPC.TL_availableReaction> allAvailableReactions = new ArrayList<>();
    private boolean initialPaid;
    private int initialSelectedType = -1;

    private final int maxReactionsCount = getMessagesController().boostsChannelLevelMax;
    private boolean emojiKeyboardVisible = false;
    private boolean allowShowEmojiKeyboard;
    private BoolAnimator emojiKeyboardAnimator;
    private final TLRPC.ChatFull info;
    private final long chatId;
    private int currentReactionsCount, reactionsCount;
    private TLRPC.Chat currentChat;
    private TL_stories.TL_premium_boostsStatus boostsStatus;
    private int selectedCustomReactions;
    private int selectedType = -1;
    private boolean isPaused;
    private final Runnable checkAfterFastDeleteRunnable = () -> checkMaxCustomReactions(false);

    public ChatCustomReactionsEditActivity(long chatId, TLRPC.ChatFull info) {
        super();
        this.chatId = chatId;
        this.info = info;
    }

    private void addReactionSettingsView(View view, boolean shadow) {
        reactionSettingsViews.add(view);
        reactionSettingsShadows.add(shadow);
    }

    private void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        if (enableReactionsCell == null || reactionsInfoCell == null) return;

        items.add(UItem.asCustom(1, enableReactionsCell));
        items.add(UItem.asCustomShadow(2, reactionsInfoCell));

        if (selectedType != SELECT_TYPE_SOME && selectedType != SELECT_TYPE_ALL && !paid) return;
        for (int i = 0; i < reactionSettingsViews.size(); i++) {
            final View view = reactionSettingsViews.get(i);
            items.add(reactionSettingsShadows.get(i) ?
                    UItem.asCustomShadow(100 + i, view) :
                    UItem.asCustom(100 + i, view));
        }
    }

    @Override
    public boolean onFragmentCreate() {
        currentChat = getMessagesController().getChat(chatId);
        if (currentChat == null) {
            currentChat = MessagesStorage.getInstance(currentAccount).getChatSync(chatId);
            if (currentChat != null) {
                getMessagesController().putChat(currentChat, true);
            } else {
                return false;
            }
        }

        if (info == null) {
            return false;
        }

        getMessagesController().getBoostsController().getBoostsStats(-chatId, boostsStatus -> {
            this.boostsStatus = boostsStatus;
            boolean hasChanges = !selectedEmojisMap.keySet().equals(initialSelectedEmojis.keySet());
            if (hasChanges) {
                checkMaxCustomReactions(false);
            }
        });
                actionButtonContainer.animate().alpha(0f).setDuration(350).setInterpolator(CubicBezierInterpolator.DEFAULT).setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        actionButtonContainer.setVisibility(View.INVISIBLE);
                    }
                }).start();
                editText.setFocusableInTouchMode(false);
                scrollView.adapter.update(true);
            } else {
                actionButtonContainer.setVisibility(View.INVISIBLE);
                editText.setFocusableInTouchMode(false);
                scrollView.adapter.update(false);
            }
        }
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
        AndroidUtilities.cancelRunOnUIThread(checkAfterFastDeleteRunnable);
        if (selectedType == SELECT_TYPE_NONE && reactionsCount != currentReactionsCount) {
            getMessagesController().setCustomChatReactions(chatId, selectedType, grabReactions(false), reactionsCount, null, null, null);
        }
        getNotificationCenter().removeObserver(this, NotificationCenter.reactionsDidLoad);
        getNotificationCenter().removeObserver(this, NotificationCenter.dialogDeleted);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isPaused) {
            isPaused = false;
            editText.setFocusable(true);
            editText.setFocusableInTouchMode(true);
            if (emojiKeyboardVisible) {
                editText.removeReactionsSpan(false);
                AndroidUtilities.runOnUIThread(() -> editText.requestFocus(), 250);
            }
        }
    }

    @Override
    public void onPause() {
        isPaused = true;
        editText.setFocusable(false);
        super.onPause();
    }

    @Override
    public boolean onBackPressed(boolean invoked) {
        if (emojiKeyboardVisible) {
            if (invoked) closeKeyboard();
            return false;
        }
        if (checkChangesBeforeExit(invoked)) {
            return false;
        }
        return super.onBackPressed(invoked);
    }

    private boolean checkChangesBeforeExit(boolean invoked) {
        boolean hasChanges = selectedType != initialSelectedType || !selectedEmojisMap.keySet().equals(initialSelectedEmojis.keySet());
        if (boostsStatus != null && boostsStatus.level < selectedCustomReactions) {
            hasChanges = false;
        }
        if (initialPaid != paid) {
            hasChanges = true;
        }
        if (invoked && hasChanges) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), getResourceProvider());
            builder.setTitle(getString("UnsavedChanges", R.string.UnsavedChanges));
            String text = getString("ReactionApplyChangesDialog", R.string.ReactionApplyChangesDialog);
            builder.setMessage(text);
            builder.setPositiveButton(getString("ApplyTheme", R.string.ApplyTheme), (dialogInterface, i) -> {
                actionButton.performClick();
            });
            builder.setNegativeButton(getString(R.string.Discard), (dialogInterface, i) -> finishFragment());
            builder.show();
        }
        return hasChanges;
    }

    private void checkMaxCustomReactions(boolean withToast) {
        if (boostsStatus == null) {
            return;
        }
        if (selectedType == SELECT_TYPE_ALL) {
            selectedType = SELECT_TYPE_SOME;
        }
        selectedCustomReactions = grabReactions(true).size();
        if (boostsStatus.level < selectedCustomReactions) {
            if (withToast) {
                CharSequence text = replaceTags(formatPluralString("ReactionReachLvlForReactionShort", selectedCustomReactions, selectedCustomReactions));
                BulletinFactory.of(this)
                        .createSimpleBulletin(R.raw.chats_infotip, text)
                        .show();
            }
            actionButton.setLvlRequiredState(selectedCustomReactions);
        } else {
            actionButton.removeLvlRequiredState();
        }
    }

    private List<TLRPC.Reaction> grabReactions(boolean onlyCustom) {
        List<TLRPC.Reaction> reactions = new ArrayList<>();
        List<TLRPC.Reaction> customReactions = new ArrayList<>();
        for (Long documentId : selectedEmojisIds) {
            if (documentId == -1) continue;

            boolean isReactionEmoji = false;
            for (TLRPC.TL_availableReaction availableReaction : allAvailableReactions) {
                if (documentId == availableReaction.activate_animation.id) {
                    TLRPC.TL_reactionEmoji emojiReaction = new TLRPC.TL_reactionEmoji();
                    emojiReaction.emoticon = availableReaction.reaction;
                    reactions.add(emojiReaction);
                    isReactionEmoji = true;
                    break;
                }
            }

            if (!isReactionEmoji) {
                TLRPC.TL_reactionCustomEmoji customEmoji = new TLRPC.TL_reactionCustomEmoji();
                customEmoji.document_id = documentId;
                reactions.add(customEmoji);
                customReactions.add(customEmoji);
            }
        }
        if (onlyCustom) {
            return customReactions;
        }
        return reactions;
    }

    private void showKeyboard() {
        if (!allowShowEmojiKeyboard) {
            return;
        }
        if (!emojiKeyboardVisible) {
            emojiKeyboardVisible = true;
            NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.stopAllHeavyOperations, 512);
            bottomDialogLayout.setVisibility(View.VISIBLE);
            bottomDialogLayout.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            setEmojiKeyboardProgress(emojiKeyboardAnimator.getFloatValue());
            emojiKeyboardAnimator.setValue(true, true);
        }
    }

    private boolean closeKeyboard() {
        if (emojiKeyboardVisible) {
            emojiKeyboardVisible = false;
            if (isClearFocusNotWorking()) {
                scrollView.setFocusableInTouchMode(true);
                scrollView.requestFocus();
            } else {
                editText.clearFocus();
            }
            NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.stopAllHeavyOperations, 512);
            bottomDialogLayout.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            emojiKeyboardAnimator.setValue(false, true);
            return true;
        }
        return false;
    }

    private boolean isClearFocusNotWorking() {
        return Build.MODEL.toLowerCase().startsWith("zte") && Build.VERSION.SDK_INT <= Build.VERSION_CODES.P;
    }

    @Override
    public boolean isSupportEdgeToEdge() {
        return true;
    }

    @Nullable
    @Override
    protected RecyclerListView getListViewForSimpleGlass() {
        return scrollView;
    }

    @Override
    public void onInsets(int left, int top, int right, int bottom) {
        bottomInset = bottom;
        updateActionButtonInsets();
        updateBottomInsets();
        updateListViewParams();
    }

    private void updateListViewParams() {
        if (scrollView == null) return;

        final float keyboardProgress = emojiKeyboardAnimator != null ? emojiKeyboardAnimator.getFloatValue() : 0.0f;
        final int buttonHeight = dp(12 + 48 + 12);
        final int keyboardHeight = Math.max(0, bottomDialogLayout.getMeasuredHeight() - mSystemInsets.bottom) + dp(12);
        final int paddingBottom = lerp(buttonHeight, keyboardHeight, keyboardProgress);

        BaseFragmentSimpleGlass.setListViewParams(
                scrollView,
                mSystemInsets.top,
                mSystemInsets.bottom,
                ActionBar.getCurrentActionBarHeight(),
                paddingBottom
        );
    }

    private void updateActionButtonInsets() {
        if (actionButtonContainer == null || actionButton == null) return;
        final float keyboardProgress = emojiKeyboardAnimator != null ? emojiKeyboardAnimator.getFloatValue() : 0.0f;
        final int buttonBottomInset = Math.round(bottomInset * (1.0f - keyboardProgress));

        final ViewGroup.LayoutParams containerLayoutParams = actionButtonContainer.getLayoutParams();
        containerLayoutParams.height = dp(13 + 48 + 13) + buttonBottomInset;
        actionButtonContainer.setLayoutParams(containerLayoutParams);

        final FrameLayout.LayoutParams buttonLayoutParams =
                (FrameLayout.LayoutParams) actionButton.getLayoutParams();
        buttonLayoutParams.bottomMargin = dp(13) + buttonBottomInset;
        actionButton.setLayoutParams(buttonLayoutParams);
    }

    private void setEmojiKeyboardProgress(float progress) {
        final float keyboardProgress = MathUtils.clamp(progress, 0.0f, 1.0f);
        if (bottomDialogLayout != null && actionButtonContainer != null) {
            bottomDialogLayout.setTranslationY((1.0f - keyboardProgress) * bottomDialogLayout.getMeasuredHeight());
            actionButtonContainer.setTranslationY(-keyboardProgress * bottomDialogLayout.getMeasuredHeight());
        }
        updateActionButtonInsets();
        updateListViewParams();
    }

    private void updateBottomInsets() {
        if (bottomDialogLayout != null) {
            bottomInsetPaint.setColor(Theme.getColor(Theme.key_windowBackgroundWhite, resourceProvider));
            bottomDialogLayout.setPadding(0, 0, 0, bottomInset);
            bottomDialogLayout.invalidate();
        }
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.dialogDeleted) {
            long dialogId = (long) args[0];
            if (dialogId == -this.chatId) {
                if (parentLayout != null && parentLayout.getLastFragment() == this) {
                    finishFragment();
                } else {
                    removeSelfFromStack();
                }
            }
        }
    }

    public void toggleStarsEnabled() {
        if (paidCheckCell.isChecked()) {
            paidCheckCell.setChecked(false);
            selectedEmojisIds.remove(-1L);
            AnimatedEmojiSpan removedSpan = selectedEmojisMap.remove(-1L);
            if (removedSpan != null) {
                removedSpan.setRemoved(() -> {
                    SpannableStringBuilder spanned = new SpannableStringBuilder(editText.getText());
                    AnimatedEmojiSpan[] spans = spanned.getSpans(0, spanned.length(), AnimatedEmojiSpan.class);
                    for (AnimatedEmojiSpan span : spans) {
                        if (span == removedSpan) {
                            int selectionEnd = editText.getEditTextSelectionEnd();
                            int spanEnd = spanned.getSpanEnd(span);
                            int spanStart = spanned.getSpanStart(span);
                            editText.getText().delete(spanStart, spanEnd);
                            int spanDiff = spanEnd - spanStart;
                            editText.setSelection(spanEnd <= selectionEnd ? selectionEnd - spanDiff : selectionEnd);
                            break;
                        }
                    }
                });
            }
            animateChangesInNextRows(removedSpan);
            selectAnimatedEmojiDialog.setMultiSelected(-1L, true);
            checkMaxCustomReactions(false);
            editText.setMaxLength(maxReactionsCount);
            setCheckedEnableReactionCell(selectedType, paid, true);
        } else {
            paidCheckCell.setChecked(true);
            try {
                editText.setMaxLength(maxReactionsCount + 1);
                SpannableString spannable = new SpannableString("b");
                AnimatedEmojiSpan span = new AnimatedEmojiSpan(-1, null) {
                    private final Bitmap bitmap = Bitmap.createBitmap(dp(24), dp(24), Bitmap.Config.ARGB_8888);
                    {
                        final Drawable drawable = getContext().getResources().getDrawable(R.drawable.star_small_inner).mutate();
                        drawable.setBounds(0, 0, dp(24), dp(24));
                        drawable.draw(new Canvas(bitmap));
                    }
                    @Override
                    public void draw(@NonNull Canvas canvas, CharSequence charSequence, int start, int end, float x, int top, int y, int bottom, @NonNull Paint paint) {
                        super.draw(canvas, charSequence, start, end, x, top, y, bottom, paint);
                        canvas.save();
                        canvas.translate(x, (top + bottom) / 2f - dp(12));
                        canvas.scale(extraScale, extraScale, x + dp(12), dp(12));
                        canvas.drawBitmap(bitmap, 0, 0, null);
                        canvas.restore();
                    }
                    @Override
                    public int getSize(Paint paint, CharSequence text, int start, int end, Paint.FontMetricsInt fm) {
                        return super.getSize(paint, text, start, end, fm) + dp(5);
                    }
                };
                span.cacheType = AnimatedEmojiDrawable.getCacheTypeForEnterView();
                span.setAdded();
                selectedEmojisIds.add(0, -1L);
                selectedEmojisMap.put(-1L, span);
                spannable.setSpan(span, 0, spannable.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                editText.getText().insert(0, spannable);
                selectAnimatedEmojiDialog.setMultiSelected(-1L, true);
                checkMaxCustomReactions(true);
                animateChangesInNextRows(span);
            } catch (Exception e) {
                FileLog.e(e);
            }
            setCheckedEnableReactionCell(selectedType, true, true);
        }
        editText.updateAnimatedEmoji(true);
    }
}
