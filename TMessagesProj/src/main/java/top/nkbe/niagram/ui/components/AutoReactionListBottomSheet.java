package top.nkbe.niagram.ui.components;

import static org.telegram.messenger.AndroidUtilities.dp;

import android.content.Context;
import android.graphics.Color;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.Emoji;
import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.AvatarDrawable;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.LayoutHelper;

import java.util.ArrayList;
import java.util.List;

import top.nkbe.niagram.helpers.ReactionLatencyHelper;

public class AutoReactionListBottomSheet extends BottomSheet {

    private final BaseFragment fragment;
    private final int currentAccount;
    private final long dialogId;
    private final TLRPC.Chat currentChat;
    private final List<ReactionLatencyHelper.MemberReactionStat> statsList = new ArrayList<>();
    private final Adapter adapter;
    private final TextView subtitleView;
    private final TextView emptyView;

    public AutoReactionListBottomSheet(BaseFragment fragment, int currentAccount, long dialogId, TLRPC.Chat currentChat, Theme.ResourcesProvider resourcesProvider) {
        super(fragment != null ? fragment.getParentActivity() : null, false, resourcesProvider);
        this.fragment = fragment;
        this.currentAccount = currentAccount;
        this.dialogId = dialogId;
        this.currentChat = currentChat;

        Context context = getContext();
        fixNavigationBar(getThemedColor(Theme.key_dialogBackground));

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(0, dp(14), 0, dp(14));

        // Header Title Layout
        LinearLayout header = new LinearLayout(context);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(20), 0, dp(20), dp(10));

        LinearLayout titleLayout = new LinearLayout(context);
        titleLayout.setOrientation(LinearLayout.VERTICAL);

        TextView titleView = new TextView(context);
        titleView.setText(LocaleController.getString(R.string.AutoReactionMonitor));
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
        titleView.setTypeface(AndroidUtilities.getTypeface(AndroidUtilities.TYPEFACE_ROBOTO_MEDIUM));
        titleView.setTextColor(getThemedColor(Theme.key_dialogTextBlack));
        titleLayout.addView(titleView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

        subtitleView = new TextView(context);
        subtitleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        subtitleView.setTextColor(getThemedColor(Theme.key_dialogTextGray2));
        titleLayout.addView(subtitleView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 2, 0, 0));

        header.addView(titleLayout, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f));

        TextView clearAllBtn = new TextView(context);
        clearAllBtn.setText(LocaleController.getString(R.string.AutoReactionReset));
        clearAllBtn.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        clearAllBtn.setTextColor(Color.parseColor("#FF5252"));
        clearAllBtn.setBackground(Theme.createSimpleSelectorRoundRectDrawable(dp(6), Color.TRANSPARENT, Color.parseColor("#26FF5252")));
        clearAllBtn.setPadding(dp(8), dp(6), dp(8), dp(6));
        clearAllBtn.setOnClickListener(v -> {
            ReactionLatencyHelper.clearGroup(currentAccount, dialogId);
            reload();
        });
        header.addView(clearAllBtn, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

        root.addView(header, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        // Divider
        View divider = new View(context);
        divider.setBackgroundColor(getThemedColor(Theme.key_divider));
        root.addView(divider, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 1, 0, 4, 0, 6));

        // Content Frame (RecyclerView + EmptyView)
        FrameLayout contentFrame = new FrameLayout(context);

        emptyView = new TextView(context);
        emptyView.setText(LocaleController.getString(R.string.AutoReactionNoData));
        emptyView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        emptyView.setTextColor(getThemedColor(Theme.key_dialogTextGray2));
        emptyView.setGravity(Gravity.CENTER);
        contentFrame.addView(emptyView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 200));

        RecyclerView recyclerView = new RecyclerView(context);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        adapter = new Adapter();
        recyclerView.setAdapter(adapter);
        contentFrame.addView(recyclerView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 320));

        root.addView(contentFrame, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        setCustomView(root);
        reload();
    }

    private void reload() {
        statsList.clear();
        statsList.addAll(ReactionLatencyHelper.getGroupMembers(currentAccount, dialogId));
        subtitleView.setText(LocaleController.formatString(R.string.AutoReactionMemberCount, statsList.size()));
        if (statsList.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
        } else {
            emptyView.setVisibility(View.GONE);
        }
        adapter.notifyDataSetChanged();
    }

    private class Adapter extends RecyclerView.Adapter<Adapter.Holder> {

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(new MemberCell(parent.getContext()));
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            ReactionLatencyHelper.MemberReactionStat stat = statsList.get(position);
            TLRPC.User user = MessagesController.getInstance(currentAccount).getUser(stat.userId);
            ((MemberCell) holder.itemView).bind(user, stat.info);
            holder.itemView.setOnClickListener(v -> {
                new AutoReactionReportBottomSheet(fragment, currentAccount, dialogId, currentChat, user, stat.info, resourcesProvider, () -> reload()).show();
            });
        }

        @Override
        public int getItemCount() {
            return statsList.size();
        }

        class Holder extends RecyclerView.ViewHolder {
            public Holder(@NonNull View itemView) {
                super(itemView);
            }
        }
    }

    private class MemberCell extends LinearLayout {

        private final BackupImageView avatarView;
        private final TextView nameView;
        private final TextView descView;
        private final TextView badgeView;

        public MemberCell(Context context) {
            super(context);
            setOrientation(HORIZONTAL);
            setGravity(Gravity.CENTER_VERTICAL);
            setPadding(dp(18), dp(8), dp(18), dp(8));
            setBackground(Theme.createSimpleSelectorRoundRectDrawable(0, Color.TRANSPARENT, getThemedColor(Theme.key_listSelector)));

            avatarView = new BackupImageView(context);
            avatarView.setRoundRadius(dp(20));
            addView(avatarView, LayoutHelper.createLinear(40, 40));

            LinearLayout infoLayout = new LinearLayout(context);
            infoLayout.setOrientation(VERTICAL);
            infoLayout.setPadding(dp(12), 0, dp(8), 0);

            nameView = new TextView(context);
            nameView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
            nameView.setTypeface(AndroidUtilities.getTypeface(AndroidUtilities.TYPEFACE_ROBOTO_MEDIUM));
            nameView.setTextColor(getThemedColor(Theme.key_dialogTextBlack));
            nameView.setSingleLine(true);
            nameView.setEllipsize(TextUtils.TruncateAt.END);
            infoLayout.addView(nameView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

            descView = new TextView(context);
            descView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
            descView.setTextColor(getThemedColor(Theme.key_dialogTextGray2));
            descView.setSingleLine(true);
            infoLayout.addView(descView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 2, 0, 0));

            addView(infoLayout, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f));

            badgeView = new TextView(context);
            badgeView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11);
            badgeView.setTypeface(AndroidUtilities.getTypeface(AndroidUtilities.TYPEFACE_ROBOTO_MEDIUM));
            badgeView.setPadding(dp(6), dp(3), dp(6), dp(3));
            addView(badgeView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));
        }

        public void bind(TLRPC.User user, ReactionLatencyHelper.Info info) {
            AvatarDrawable avatarDrawable = new AvatarDrawable();
            if (user != null) {
                avatarDrawable.setInfo(user);
                avatarView.setImage(ImageLocation.getForUser(user, ImageLocation.TYPE_SMALL), "50_50", avatarDrawable, user);
                String name = UserObject.getUserName(user);
                nameView.setText(Emoji.replaceEmoji(name, nameView.getPaint().getFontMetricsInt(), false));
            } else {
                avatarView.setImageDrawable(avatarDrawable);
                nameView.setText("ID: " + (info != null && !info.samples.isEmpty() ? info.samples.get(0).messageId : ""));
            }

            int total = info != null ? info.total : 0;
            float avg = info != null ? info.avgLatency : 0f;
            descView.setText(String.format(java.util.Locale.US, "%d 次反應 · 均速 %.1fs", total, avg));

            if (info != null && info.suspected) {
                badgeView.setText(info.reason);
                badgeView.setTextColor(Color.parseColor("#FF5252"));
                badgeView.setBackground(Theme.createRoundRectDrawable(dp(4), Color.parseColor("#26FF5252")));
            } else {
                badgeView.setText(LocaleController.getString(R.string.AutoReactionNormal));
                badgeView.setTextColor(Color.parseColor("#66BB6A"));
                badgeView.setBackground(Theme.createRoundRectDrawable(dp(4), Color.parseColor("#1A66BB6A")));
            }
        }
    }
}
