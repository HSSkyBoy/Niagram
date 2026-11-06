package top.nkbe.niagram.ui.components;

import static org.telegram.messenger.AndroidUtilities.dp;

import android.content.Context;
import android.graphics.Color;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.Emoji;
import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.UserObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.AvatarDrawable;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.LayoutHelper;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import top.nkbe.niagram.helpers.ReactionLatencyHelper;

public class AutoReactionReportBottomSheet extends BottomSheet {

    private final BaseFragment fragment;
    private final int currentAccount;
    private final long dialogId;
    private final TLRPC.Chat currentChat;
    private final TLRPC.User user;
    private final ReactionLatencyHelper.Info info;
    private final Runnable onReset;

    public AutoReactionReportBottomSheet(BaseFragment fragment, int currentAccount, long dialogId, TLRPC.Chat currentChat, TLRPC.User user, ReactionLatencyHelper.Info info, Theme.ResourcesProvider resourcesProvider, Runnable onReset) {
        super(fragment != null ? fragment.getParentActivity() : null, false, resourcesProvider);
        this.fragment = fragment;
        this.currentAccount = currentAccount;
        this.dialogId = dialogId;
        this.currentChat = currentChat;
        this.user = user;
        this.info = info;
        this.onReset = onReset;

        Context context = getContext();
        fixNavigationBar(getThemedColor(Theme.key_dialogBackground));

        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dp(20), dp(16), dp(20), dp(20));

        // Member Header
        LinearLayout header = new LinearLayout(context);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        BackupImageView avatarView = new BackupImageView(context);
        avatarView.setRoundRadius(dp(21));
        AvatarDrawable avatarDrawable = new AvatarDrawable();
        if (user != null) {
            avatarDrawable.setInfo(user);
            avatarView.setImage(ImageLocation.getForUser(user, ImageLocation.TYPE_SMALL), "50_50", avatarDrawable, user);
        }
        header.addView(avatarView, LayoutHelper.createLinear(42, 42));

        LinearLayout nameLayout = new LinearLayout(context);
        nameLayout.setOrientation(LinearLayout.VERTICAL);
        nameLayout.setPadding(dp(12), 0, 0, 0);

        TextView nameView = new TextView(context);
        nameView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        nameView.setTypeface(AndroidUtilities.getTypeface(AndroidUtilities.TYPEFACE_ROBOTO_MEDIUM));
        nameView.setTextColor(getThemedColor(Theme.key_dialogTextBlack));
        nameView.setSingleLine(true);
        nameView.setEllipsize(TextUtils.TruncateAt.END);
        String name = user != null ? UserObject.getUserName(user) : "Unknown";
        nameView.setText(Emoji.replaceEmoji(name, nameView.getPaint().getFontMetricsInt(), false));
        nameLayout.addView(nameView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

        TextView riskBadge = new TextView(context);
        riskBadge.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        riskBadge.setTypeface(AndroidUtilities.getTypeface(AndroidUtilities.TYPEFACE_ROBOTO_MEDIUM));
        if (info != null && info.suspected) {
            riskBadge.setText(LocaleController.formatString(R.string.AutoReactionSuspected, info.reason));
            riskBadge.setTextColor(Color.parseColor("#FF5252"));
        } else {
            riskBadge.setText(LocaleController.getString(R.string.AutoReactionNormal));
            riskBadge.setTextColor(Color.parseColor("#66BB6A"));
        }
        nameLayout.addView(riskBadge, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 2, 0, 0));
        header.addView(nameLayout, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        container.addView(header, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 16));

        // Metrics Grid (3 Cards)
        LinearLayout statsGrid = new LinearLayout(context);
        statsGrid.setOrientation(LinearLayout.HORIZONTAL);
        statsGrid.setWeightSum(3);

        int minLatency = info != null ? info.minLatency : 0;
        float avgLatency = info != null ? info.avgLatency : 0f;
        int zeroCount = info != null ? info.zeroCount : 0;

        statsGrid.addView(createStatCard(context, LocaleController.getString(R.string.AutoReactionMinLatency), minLatency + "s", Color.parseColor("#FF5252")), LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f));
        statsGrid.addView(createStatCard(context, LocaleController.getString(R.string.AutoReactionAvgLatency), String.format(Locale.US, "%.1fs", avgLatency), Color.parseColor("#FFA726")), LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f, 6, 0, 6, 0));
        statsGrid.addView(createStatCard(context, LocaleController.getString(R.string.AutoReactionZeroCount), String.valueOf(zeroCount), Color.parseColor("#42A5F5")), LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f));

        container.addView(statsGrid, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 16));

        // Timeline Title
        TextView timelineTitle = new TextView(context);
        timelineTitle.setText(LocaleController.getString(R.string.AutoReactionTimeline));
        timelineTitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        timelineTitle.setTypeface(AndroidUtilities.getTypeface(AndroidUtilities.TYPEFACE_ROBOTO_MEDIUM));
        timelineTitle.setTextColor(getThemedColor(Theme.key_dialogTextGray2));
        container.addView(timelineTitle, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 8));

        // Timeline Scroll View
        ScrollView scrollView = new ScrollView(context);
        LinearLayout timelineLayout = new LinearLayout(context);
        timelineLayout.setOrientation(LinearLayout.VERTICAL);

        if (info != null && !info.samples.isEmpty()) {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
            for (int i = info.samples.size() - 1; i >= 0; i--) {
                ReactionLatencyHelper.Sample s = info.samples.get(i);
                timelineLayout.addView(createTimelineRow(context, s, sdf));
            }
        } else {
            TextView emptyView = new TextView(context);
            emptyView.setText(LocaleController.getString(R.string.AutoReactionNoData));
            emptyView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
            emptyView.setTextColor(getThemedColor(Theme.key_dialogTextGray2));
            emptyView.setPadding(0, dp(12), 0, dp(12));
            timelineLayout.addView(emptyView);
        }
        scrollView.addView(timelineLayout);
        container.addView(scrollView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, dp(160), 0, 0, 0, 16));

        // Bottom Action Buttons
        boolean canBan = false;
        if (fragment != null && currentChat != null && user != null) {
            long selfId = UserConfig.getInstance(currentAccount).getClientUserId();
            canBan = ChatObject.canBlockUsers(currentChat) && user.id != selfId && !(currentChat.creator && user.id == currentChat.id);
        }

        LinearLayout buttonsLayout = new LinearLayout(context);
        buttonsLayout.setOrientation(LinearLayout.HORIZONTAL);
        buttonsLayout.setWeightSum(canBan ? 3 : 2);

        if (canBan) {
            TextView banBtn = new TextView(context);
            banBtn.setText(LocaleController.getString(R.string.AutoReactionBanMember));
            banBtn.setGravity(Gravity.CENTER);
            banBtn.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
            banBtn.setTypeface(AndroidUtilities.getTypeface(AndroidUtilities.TYPEFACE_ROBOTO_MEDIUM));
            banBtn.setTextColor(Color.parseColor("#FF5252"));
            banBtn.setBackground(Theme.createSimpleSelectorRoundRectDrawable(dp(8), Color.TRANSPARENT, Color.parseColor("#33FF5252")));
            banBtn.setPadding(0, dp(10), 0, dp(10));
            banBtn.setOnClickListener(v -> {
                dismiss();
                ReactionLatencyHelper.banUserWithConfirmation(fragment, currentChat, user, onReset);
            });
            buttonsLayout.addView(banBtn, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f, 0, 0, 6, 0));
        }

        TextView resetBtn = new TextView(context);
        resetBtn.setText(LocaleController.getString(R.string.AutoReactionReset));
        resetBtn.setGravity(Gravity.CENTER);
        resetBtn.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        resetBtn.setTypeface(AndroidUtilities.getTypeface(AndroidUtilities.TYPEFACE_ROBOTO_MEDIUM));
        resetBtn.setTextColor(getThemedColor(Theme.key_dialogTextGray2));
        resetBtn.setBackground(Theme.createSimpleSelectorRoundRectDrawable(dp(8), Color.TRANSPARENT, Color.parseColor("#1A888888")));
        resetBtn.setPadding(0, dp(10), 0, dp(10));
        resetBtn.setOnClickListener(v -> {
            if (user != null) {
                ReactionLatencyHelper.clearMember(currentAccount, dialogId, user.id);
            }
            dismiss();
            if (onReset != null) {
                onReset.run();
            }
        });
        buttonsLayout.addView(resetBtn, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f, 0, 0, 6, 0));

        TextView closeBtn = new TextView(context);
        closeBtn.setText(LocaleController.getString(R.string.Close));
        closeBtn.setGravity(Gravity.CENTER);
        closeBtn.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        closeBtn.setTypeface(AndroidUtilities.getTypeface(AndroidUtilities.TYPEFACE_ROBOTO_MEDIUM));
        closeBtn.setTextColor(getThemedColor(Theme.key_dialogButton));
        closeBtn.setBackground(Theme.createSimpleSelectorRoundRectDrawable(dp(8), Color.TRANSPARENT, getThemedColor(Theme.key_dialogButtonSelector)));
        closeBtn.setPadding(0, dp(10), 0, dp(10));
        closeBtn.setOnClickListener(v -> dismiss());
        buttonsLayout.addView(closeBtn, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f));

        container.addView(buttonsLayout, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        setCustomView(container);
    }

    private View createStatCard(Context context, String label, String value, int valueColor) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setBackground(Theme.createRoundRectDrawable(dp(8), getThemedColor(Theme.key_chat_inBubble)));
        card.setPadding(dp(8), dp(10), dp(8), dp(10));

        TextView valView = new TextView(context);
        valView.setText(value);
        valView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        valView.setTypeface(AndroidUtilities.getTypeface(AndroidUtilities.TYPEFACE_ROBOTO_MEDIUM));
        valView.setTextColor(valueColor);
        card.addView(valView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

        TextView lblView = new TextView(context);
        lblView.setText(label);
        lblView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11);
        lblView.setTextColor(getThemedColor(Theme.key_dialogTextGray2));
        card.addView(lblView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 2, 0, 0));

        return card;
    }

    private View createTimelineRow(Context context, ReactionLatencyHelper.Sample sample, SimpleDateFormat sdf) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(6), 0, dp(6));

        TextView timeView = new TextView(context);
        timeView.setText(sdf.format(new Date(sample.timestamp)));
        timeView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        timeView.setTextColor(getThemedColor(Theme.key_dialogTextGray2));
        row.addView(timeView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

        if (!TextUtils.isEmpty(sample.emoji)) {
            TextView emojiView = new TextView(context);
            emojiView.setText(Emoji.replaceEmoji(sample.emoji, emojiView.getPaint().getFontMetricsInt(), false));
            emojiView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
            emojiView.setPadding(dp(8), 0, 0, 0);
            row.addView(emojiView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));
        }

        TextView msgView = new TextView(context);
        msgView.setText("  #" + sample.messageId);
        msgView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        msgView.setTextColor(getThemedColor(Theme.key_dialogTextBlack));
        row.addView(msgView, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f));

        TextView tagView = new TextView(context);
        tagView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11);
        tagView.setTypeface(AndroidUtilities.getTypeface(AndroidUtilities.TYPEFACE_ROBOTO_MEDIUM));
        tagView.setPadding(dp(6), dp(2), dp(6), dp(2));

        if (sample.latency == 0) {
            tagView.setText("⚡ 0s");
            tagView.setTextColor(Color.parseColor("#FF5252"));
            tagView.setBackground(Theme.createRoundRectDrawable(dp(4), Color.parseColor("#33FF5252")));
        } else if (sample.latency <= 3) {
            tagView.setText(sample.latency + "s");
            tagView.setTextColor(Color.parseColor("#FFA726"));
            tagView.setBackground(Theme.createRoundRectDrawable(dp(4), Color.parseColor("#33FFA726")));
        } else {
            tagView.setText(sample.latency + "s");
            tagView.setTextColor(getThemedColor(Theme.key_dialogTextGray2));
            tagView.setBackground(Theme.createRoundRectDrawable(dp(4), Color.parseColor("#1A888888")));
        }
        row.addView(tagView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

        return row;
    }
}
