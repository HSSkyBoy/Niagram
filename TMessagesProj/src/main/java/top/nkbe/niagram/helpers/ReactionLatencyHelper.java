package top.nkbe.niagram.helpers;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.UserObject;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.BulletinFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import top.nkbe.niagram.config.NyaConfig;

/**
 * Tracks reaction latency to detect automated reactions / auto-reaction bots.
 * Real-time per-reaction timing with streak, emoji, and average latency detection.
 */
public final class ReactionLatencyHelper {

    public static final int FAST_SECONDS = 3;
    public static final int SUSPECT_STREAK = 3;
    public static final int RELAPSE_STREAK = 5;

    private static final int WINDOW = 30;
    private static final int MAX_ENTRIES = 2000;
    private static final int SAVE_DELAY_MS = 5000;
    private static final String PREFS = "reaction_latency";
    private static final String PREFS_KEY = "entries_v2";

    private static final Object LOCK = new Object();
    private static final Map<String, UserEntry> entries = new LinkedHashMap<String, UserEntry>(256, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, UserEntry> eldest) {
            return size() > MAX_ENTRIES;
        }
    };
    private static boolean loaded;
    private static boolean saveScheduled;

    private ReactionLatencyHelper() {
    }

    public static final class Sample {
        public final int messageId;
        public final int latency;
        public final long timestamp;
        public final String emoji;

        public Sample(int messageId, int latency, long timestamp, String emoji) {
            this.messageId = messageId;
            this.latency = latency;
            this.timestamp = timestamp;
            this.emoji = emoji != null ? emoji : "";
        }
    }

    private static final class UserEntry {
        final ArrayList<Sample> samples = new ArrayList<>();
        int level;
        int zeroLatencyCount;
    }

    public static final class Info {
        public final int streak;
        public final int zeroCount;
        public final int total;
        public final int minLatency;
        public final float avgLatency;
        public final int sameEmojiStreak;
        public final String lastEmoji;
        public final boolean confirmedAuto;
        public final boolean suspected;
        public final String reason;
        public final List<Sample> samples;

        public Info(int streak, int zeroCount, int total, int minLatency, float avgLatency,
                    int sameEmojiStreak, String lastEmoji, boolean confirmedAuto, boolean suspected,
                    String reason, List<Sample> samples) {
            this.streak = streak;
            this.zeroCount = zeroCount;
            this.total = total;
            this.minLatency = minLatency;
            this.avgLatency = avgLatency;
            this.sameEmojiStreak = sameEmojiStreak;
            this.lastEmoji = lastEmoji != null ? lastEmoji : "";
            this.confirmedAuto = confirmedAuto;
            this.suspected = suspected;
            this.reason = reason != null ? reason : "";
            this.samples = samples != null ? Collections.unmodifiableList(new ArrayList<>(samples)) : Collections.emptyList();
        }
    }

    public static final class MemberReactionStat {
        public final long userId;
        public final Info info;

        public MemberReactionStat(long userId, Info info) {
            this.userId = userId;
            this.info = info;
        }
    }

    public static boolean isEnabled() {
        return NyaConfig.INSTANCE.getAutoReactionDetect().Bool();
    }

    public static void recordMessage(int account, long dialogId, MessageObject message) {
        if (!isEnabled() || message == null || message.messageOwner == null) {
            return;
        }
        final TLRPC.Message owner = message.messageOwner;
        final TLRPC.TL_messageReactions reactions = owner.reactions;
        if (reactions == null || reactions.recent_reactions == null || reactions.recent_reactions.isEmpty()) {
            return;
        }
        if (!isTrackedGroup(account, dialogId)) {
            return;
        }
        final int messageId = message.getId();
        if (messageId <= 0) {
            return;
        }
        final long self = UserConfig.getInstance(account).getClientUserId();
        boolean changed = false;
        for (int i = 0, n = reactions.recent_reactions.size(); i < n; i++) {
            final TLRPC.MessagePeerReaction reaction = reactions.recent_reactions.get(i);
            if (reaction == null || !(reaction.peer_id instanceof TLRPC.TL_peerUser)) {
                continue;
            }
            final long userId = reaction.peer_id.user_id;
            if (userId == self) {
                continue;
            }
            final int latency = reaction.date - owner.date;
            if (latency < 0) {
                continue;
            }
            final TLRPC.User user = MessagesController.getInstance(account).getUser(userId);
            if (user != null && user.bot) {
                continue;
            }
            final String emoji = extractEmoji(reaction.reaction);
            changed |= addSample(account, dialogId, userId, messageId, latency, reaction.date * 1000L, emoji);
        }
        if (changed) {
            scheduleSave();
        }
    }

    public static void recordMessages(int account, long dialogId, List<MessageObject> messages) {
        if (!isEnabled() || messages == null || messages.isEmpty() || !isTrackedGroup(account, dialogId)) {
            return;
        }
        boolean changed = false;
        for (int i = 0, n = messages.size(); i < n; i++) {
            final MessageObject msg = messages.get(i);
            if (msg == null || msg.messageOwner == null) {
                continue;
            }
            final TLRPC.Message owner = msg.messageOwner;
            final TLRPC.TL_messageReactions reactions = owner.reactions;
            if (reactions == null || reactions.recent_reactions == null || reactions.recent_reactions.isEmpty()) {
                continue;
            }
            final int messageId = msg.getId();
            if (messageId <= 0) {
                continue;
            }
            final long self = UserConfig.getInstance(account).getClientUserId();
            for (int r = 0, rn = reactions.recent_reactions.size(); r < rn; r++) {
                final TLRPC.MessagePeerReaction reaction = reactions.recent_reactions.get(r);
                if (reaction == null || !(reaction.peer_id instanceof TLRPC.TL_peerUser)) {
                    continue;
                }
                final long userId = reaction.peer_id.user_id;
                if (userId == self) {
                    continue;
                }
                final int latency = reaction.date - owner.date;
                if (latency < 0) {
                    continue;
                }
                final TLRPC.User user = MessagesController.getInstance(account).getUser(userId);
                if (user != null && user.bot) {
                    continue;
                }
                final String emoji = extractEmoji(reaction.reaction);
                changed |= addSample(account, dialogId, userId, messageId, latency, reaction.date * 1000L, emoji);
            }
        }
        if (changed) {
            scheduleSave();
        }
    }

    public static void onNewMessages(int account, long dialogId, List<MessageObject> messages) {
        if (!isEnabled() || messages == null || messages.isEmpty() || !isTrackedGroup(account, dialogId)) {
            return;
        }
        boolean changed = false;
        for (int i = 0, n = messages.size(); i < n; i++) {
            final MessageObject msg = messages.get(i);
            if (msg == null) {
                continue;
            }
            final long fromId = msg.getFromChatId();
            if (fromId > 0 && !msg.isOutOwner()) {
                changed |= markSpoke(account, dialogId, fromId);
            }
        }
        if (changed) {
            scheduleSave();
        }
    }

    public static Info getInfo(int account, long dialogId, long userId) {
        if (!isEnabled()) {
            return null;
        }
        synchronized (LOCK) {
            ensureLoaded();
            final UserEntry entry = entries.get(key(account, dialogId, userId));
            return buildInfoLocked(entry);
        }
    }

    private static Info buildInfoLocked(UserEntry entry) {
        if (entry == null || entry.samples.isEmpty()) {
            return null;
        }
        final int streak = trailingFastStreak(entry);
        final int sameEmojiStreak = trailingSameEmojiStreak(entry);
        final String lastEmoji = entry.samples.get(entry.samples.size() - 1).emoji;
        final int zeroCount = entry.zeroLatencyCount;
        final int total = entry.samples.size();
        int min = Integer.MAX_VALUE;
        int sum = 0;
        for (Sample s : entry.samples) {
            if (s.latency < min) {
                min = s.latency;
            }
            sum += s.latency;
        }
        float avg = total > 0 ? (float) sum / total : 0f;

        boolean suspected = false;
        String reason = "";

        if (zeroCount > 0) {
            suspected = true;
            reason = "0s 秒回 (" + zeroCount + "次)";
        } else if (total >= 3 && avg <= 3.0f) {
            suspected = true;
            if (sameEmojiStreak >= 3 && !TextUtils.isEmpty(lastEmoji)) {
                reason = String.format(Locale.US, "均速 %.1fs · 連續 %d次 %s", avg, sameEmojiStreak, lastEmoji);
            } else {
                reason = String.format(Locale.US, "均速 %.1fs (≤3s)", avg);
            }
        } else if (sameEmojiStreak >= 3 && (avg <= 5.0f || streak >= 2)) {
            suspected = true;
            reason = "連續 " + sameEmojiStreak + "次 " + lastEmoji;
        } else if (streak >= SUSPECT_STREAK) {
            suspected = true;
            reason = "連續 " + streak + "次 ≤3s";
        }

        boolean confirmed = zeroCount >= 1 || (total >= 5 && avg <= 2.5f) || streak >= 5;

        return new Info(streak, zeroCount, total, min == Integer.MAX_VALUE ? 0 : min, avg,
                sameEmojiStreak, lastEmoji, confirmed, suspected, reason, entry.samples);
    }

    public static List<MemberReactionStat> getGroupMembers(int account, long dialogId) {
        if (!isEnabled()) {
            return Collections.emptyList();
        }
        final List<MemberReactionStat> list = new ArrayList<>();
        synchronized (LOCK) {
            ensureLoaded();
            final String prefix = account + ":" + dialogId + ":";
            for (Map.Entry<String, UserEntry> e : entries.entrySet()) {
                if (e.getKey().startsWith(prefix)) {
                    final String userIdStr = e.getKey().substring(prefix.length());
                    try {
                        long uid = Long.parseLong(userIdStr);
                        // Avoid entries.get() while iterating an accessOrder LinkedHashMap.
                        Info info = buildInfoLocked(e.getValue());
                        if (info != null && !info.samples.isEmpty()) {
                            list.add(new MemberReactionStat(uid, info));
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        }
        Collections.sort(list, (a, b) -> {
            if (a.info.suspected != b.info.suspected) {
                return a.info.suspected ? -1 : 1;
            }
            if (a.info.suspected) {
                if (a.info.zeroCount != b.info.zeroCount) {
                    return Integer.compare(b.info.zeroCount, a.info.zeroCount);
                }
            }
            int cmp = Integer.compare(b.info.total, a.info.total);
            if (cmp != 0) return cmp;
            return Float.compare(a.info.avgLatency, b.info.avgLatency);
        });
        return list;
    }

    public static void clearMember(int account, long dialogId, long userId) {
        synchronized (LOCK) {
            ensureLoaded();
            entries.remove(key(account, dialogId, userId));
        }
        scheduleSave();
    }

    public static void clearGroup(int account, long dialogId) {
        synchronized (LOCK) {
            ensureLoaded();
            final String prefix = account + ":" + dialogId + ":";
            for (Iterator<Map.Entry<String, UserEntry>> it = entries.entrySet().iterator(); it.hasNext(); ) {
                if (it.next().getKey().startsWith(prefix)) {
                    it.remove();
                }
            }
        }
        scheduleSave();
    }

    public static void banUserWithConfirmation(BaseFragment fragment, TLRPC.Chat currentChat, TLRPC.User targetUser, Runnable onBanned) {
        if (fragment == null || fragment.getParentActivity() == null || currentChat == null || targetUser == null) {
            return;
        }
        final int account = fragment.getCurrentAccount();
        final long selfId = UserConfig.getInstance(account).getClientUserId();
        if (targetUser.id == selfId) {
            BulletinFactory.of(fragment).createSimpleBulletin(R.raw.error, LocaleController.getString(R.string.UserRestricted)).show();
            return;
        }
        if (currentChat.creator && targetUser.id == currentChat.id) {
            BulletinFactory.of(fragment).createSimpleBulletin(R.raw.error, LocaleController.getString(R.string.UserRestricted)).show();
            return;
        }
        if (!ChatObject.canBlockUsers(currentChat)) {
            BulletinFactory.of(fragment).createSimpleBulletin(R.raw.error, LocaleController.getString(R.string.UserRestricted)).show();
            return;
        }

        final AlertDialog.Builder builder = new AlertDialog.Builder(fragment.getParentActivity(), fragment.getResourceProvider());
        builder.setTitle(LocaleController.getString(R.string.KickFromGroup));
        String userName = UserObject.getUserName(targetUser);
        builder.setMessage(LocaleController.formatString(R.string.AutoReactionBanConfirm, userName));
        builder.setPositiveButton(LocaleController.getString(R.string.KickFromGroup), (dialog, which) -> {
            fragment.getMessagesController().deleteParticipantFromChat(currentChat.id, targetUser, null, false, false);
            clearMember(account, -currentChat.id, targetUser.id);
            if (BulletinFactory.canShowBulletin(fragment)) {
                BulletinFactory.createRemoveFromChatBulletin(fragment, targetUser, currentChat.title).show();
            }
            if (onBanned != null) {
                onBanned.run();
            }
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        final AlertDialog dialog = builder.create();
        dialog.show();
        final TextView button = (TextView) dialog.getButton(DialogInterface.BUTTON_POSITIVE);
        if (button != null) {
            button.setTextColor(Theme.getColor(Theme.key_text_RedBold, fragment.getResourceProvider()));
        }
    }

    private static String extractEmoji(TLRPC.Reaction reaction) {
        if (reaction instanceof TLRPC.TL_reactionEmoji) {
            return ((TLRPC.TL_reactionEmoji) reaction).emoticon;
        } else if (reaction instanceof TLRPC.TL_reactionCustomEmoji) {
            return "★";
        }
        return "";
    }

    private static boolean isTrackedGroup(int account, long dialogId) {
        if (dialogId >= 0) {
            return false;
        }
        final TLRPC.Chat chat = MessagesController.getInstance(account).getChat(-dialogId);
        return chat != null && !(ChatObject.isChannel(chat) && !chat.megagroup);
    }

    private static boolean addSample(int account, long dialogId, long userId, int messageId, int latency, long timestamp, String emoji) {
        synchronized (LOCK) {
            ensureLoaded();
            final String k = key(account, dialogId, userId);
            UserEntry entry = entries.get(k);
            if (entry == null) {
                entry = new UserEntry();
                entries.put(k, entry);
            }
            for (int i = 0, n = entry.samples.size(); i < n; i++) {
                if (entry.samples.get(i).messageId == messageId) {
                    return false;
                }
            }
            if (latency == 0) {
                entry.zeroLatencyCount++;
            }
            entry.samples.add(new Sample(messageId, latency, timestamp, emoji));
            Collections.sort(entry.samples, (a, b) -> Integer.compare(a.messageId, b.messageId));
            while (entry.samples.size() > WINDOW) {
                Sample removed = entry.samples.remove(0);
                if (removed.latency == 0 && entry.zeroLatencyCount > 0) {
                    entry.zeroLatencyCount--;
                }
            }
            return true;
        }
    }

    private static boolean markSpoke(int account, long dialogId, long userId) {
        synchronized (LOCK) {
            ensureLoaded();
            final UserEntry entry = entries.get(key(account, dialogId, userId));
            if (entry == null) {
                return false;
            }
            final boolean hadSamples = !entry.samples.isEmpty();
            final boolean wasFlagged = entry.zeroLatencyCount > 0 || trailingFastStreak(entry) >= (entry.level > 0 ? RELAPSE_STREAK : SUSPECT_STREAK);
            entry.samples.clear();
            entry.zeroLatencyCount = 0;
            if (wasFlagged) {
                entry.level = 1;
            }
            return hadSamples;
        }
    }

    private static int trailingFastStreak(UserEntry entry) {
        int streak = 0;
        for (int i = entry.samples.size() - 1; i >= 0; i--) {
            if (entry.samples.get(i).latency <= FAST_SECONDS) {
                streak++;
            } else {
                break;
            }
        }
        return streak;
    }

    private static int trailingSameEmojiStreak(UserEntry entry) {
        if (entry.samples.isEmpty()) return 0;
        final String target = entry.samples.get(entry.samples.size() - 1).emoji;
        if (TextUtils.isEmpty(target)) return 0;
        int streak = 0;
        for (int i = entry.samples.size() - 1; i >= 0; i--) {
            if (target.equals(entry.samples.get(i).emoji)) {
                streak++;
            } else {
                break;
            }
        }
        return streak;
    }

    private static String key(int account, long dialogId, long userId) {
        return account + ":" + dialogId + ":" + userId;
    }

    private static void ensureLoaded() {
        if (loaded) {
            return;
        }
        loaded = true;
        try {
            final String data = prefs().getString(PREFS_KEY, null);
            if (data == null || data.isEmpty()) {
                return;
            }
            for (String line : data.split("\n")) {
                if (line.isEmpty()) continue;
                try {
                    final String[] head = line.split("\\|", 4);
                    if (head.length < 3) continue;
                    final UserEntry entry = new UserEntry();
                    entry.level = Integer.parseInt(head[1]);
                    entry.zeroLatencyCount = Integer.parseInt(head[2]);
                    if (head.length >= 4 && !head[3].isEmpty()) {
                        for (String sample : head[3].split(";")) {
                            if (sample.isEmpty()) continue;
                            final String[] parts = sample.split(",");
                            if (parts.length >= 2) {
                                int mid = Integer.parseInt(parts[0]);
                                int lat = Integer.parseInt(parts[1]);
                                long ts = parts.length >= 3 ? Long.parseLong(parts[2]) : System.currentTimeMillis();
                                String emoji = parts.length >= 4 ? parts[3] : "";
                                entry.samples.add(new Sample(mid, lat, ts, emoji));
                            }
                        }
                    }
                    entries.put(head[0], entry);
                } catch (Exception e) {
                    FileLog.e(e);
                }
            }
        } catch (Exception e) {
            FileLog.e(e);
            entries.clear();
        }
    }

    private static void scheduleSave() {
        synchronized (LOCK) {
            if (saveScheduled) {
                return;
            }
            saveScheduled = true;
        }
        Utilities.globalQueue.postRunnable(ReactionLatencyHelper::save, SAVE_DELAY_MS);
    }

    private static void save() {
        final StringBuilder builder = new StringBuilder();
        synchronized (LOCK) {
            saveScheduled = false;
            for (Iterator<Map.Entry<String, UserEntry>> it = entries.entrySet().iterator(); it.hasNext(); ) {
                final Map.Entry<String, UserEntry> item = it.next();
                final UserEntry entry = item.getValue();
                if (entry.samples.isEmpty() && entry.level == 0 && entry.zeroLatencyCount == 0) {
                    continue;
                }
                if (builder.length() > 0) {
                    builder.append('\n');
                }
                builder.append(item.getKey()).append('|').append(entry.level).append('|').append(entry.zeroLatencyCount).append('|');
                for (int i = 0; i < entry.samples.size(); i++) {
                    if (i > 0) {
                        builder.append(';');
                    }
                    final Sample s = entry.samples.get(i);
                    builder.append(s.messageId).append(',').append(s.latency).append(',').append(s.timestamp).append(',').append(s.emoji != null ? s.emoji : "");
                }
            }
        }
        try {
            prefs().edit().putString(PREFS_KEY, builder.toString()).apply();
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    private static SharedPreferences prefs() {
        return ApplicationLoader.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
