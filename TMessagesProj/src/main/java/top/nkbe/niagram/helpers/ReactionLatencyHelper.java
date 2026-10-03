package top.nkbe.niagram.helpers;

import android.content.Context;
import android.content.SharedPreferences;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import top.nkbe.niagram.config.NyaConfig;

/**
 * Tracks how quickly group members react to messages and flags members whose reactions are
 * consistently instant, which points to an auto-reaction client.
 * <p>
 * Everything here is local: nothing is sent to Telegram and nothing is reported. The latency of a
 * reaction is {@code reaction.date - message.date}, both in server seconds, so the best resolution
 * is one second.
 */
public final class ReactionLatencyHelper {

    /** A reaction counts as "instant" when it arrives within this many seconds of the message. */
    public static final int FAST_SECONDS = 1;
    /** Consecutive instant reactions needed to flag a member. */
    public static final int SUSPECT_STREAK = 3;
    /** Consecutive instant reactions needed to flag a member who has spoken since being flagged before. */
    public static final int RELAPSE_STREAK = 5;

    private static final int WINDOW = 20;
    private static final int MAX_ENTRIES = 1500;
    private static final int SAVE_DELAY_MS = 8000;
    private static final String PREFS = "reaction_latency";
    private static final String PREFS_KEY = "entries";

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

    private static final class UserEntry {
        /** Samples as {messageId, latencySeconds}, kept sorted by message id. */
        final ArrayList<int[]> samples = new ArrayList<>();
        /** 0 until the member speaks after being tracked; afterwards the stricter threshold applies. */
        int level;
    }

    public static final class Info {
        public final int streak;
        public final int total;
        public final boolean suspected;

        Info(int streak, int total, boolean suspected) {
            this.streak = streak;
            this.total = total;
            this.suspected = suspected;
        }
    }

    public static boolean isEnabled() {
        return NyaConfig.INSTANCE.getAutoReactionDetect().Bool();
    }

    /** Records the reactions currently visible on one message. Safe to call repeatedly. */
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
            changed |= addSample(dialogId, userId, messageId, latency);
        }
        if (changed) {
            scheduleSave();
        }
    }

    /** Backfills from a batch of loaded messages, e.g. when a chat's history is opened. */
    public static void recordMessages(int account, long dialogId, List<MessageObject> messages) {
        if (!isEnabled() || messages == null || messages.isEmpty() || !isTrackedGroup(account, dialogId)) {
            return;
        }
        for (int i = 0, n = messages.size(); i < n; i++) {
            recordMessage(account, dialogId, messages.get(i));
        }
    }

    /** Anyone who posts a message is clearly present, so their current suspicion is cleared. */
    public static void onNewMessages(long dialogId, List<MessageObject> messages) {
        if (!isEnabled() || dialogId >= 0 || messages == null) {
            return;
        }
        boolean changed = false;
        for (int i = 0, n = messages.size(); i < n; i++) {
            final MessageObject message = messages.get(i);
            if (message == null || message.messageOwner == null || message.isOut() || message.messageOwner.action != null) {
                continue;
            }
            if (!(message.messageOwner.from_id instanceof TLRPC.TL_peerUser)) {
                continue;
            }
            changed |= markSpoke(dialogId, message.messageOwner.from_id.user_id);
        }
        if (changed) {
            scheduleSave();
        }
    }

    public static Info getInfo(long dialogId, long userId) {
        synchronized (LOCK) {
            ensureLoaded();
            final UserEntry entry = entries.get(key(dialogId, userId));
            if (entry == null) {
                return new Info(0, 0, false);
            }
            final int streak = trailingFastStreak(entry);
            final int threshold = entry.level > 0 ? RELAPSE_STREAK : SUSPECT_STREAK;
            return new Info(streak, entry.samples.size(), streak >= threshold);
        }
    }

    /** Text for the member menu, or null when the member is not flagged or the feature is off. */
    public static String getSuspectText(long dialogId, long userId) {
        if (!isEnabled()) {
            return null;
        }
        final Info info = getInfo(dialogId, userId);
        if (!info.suspected) {
            return null;
        }
        return LocaleController.formatString(R.string.AutoReactionSuspected, info.streak);
    }

    /** Group chats only; broadcast channels do not expose who reacted. */
    private static boolean isTrackedGroup(int account, long dialogId) {
        if (dialogId >= 0) {
            return false;
        }
        final TLRPC.Chat chat = MessagesController.getInstance(account).getChat(-dialogId);
        return chat != null && !(ChatObject.isChannel(chat) && !chat.megagroup);
    }

    private static boolean addSample(long dialogId, long userId, int messageId, int latency) {
        synchronized (LOCK) {
            ensureLoaded();
            final String key = key(dialogId, userId);
            UserEntry entry = entries.get(key);
            if (entry == null) {
                entry = new UserEntry();
                entries.put(key, entry);
            }
            for (int i = 0, n = entry.samples.size(); i < n; i++) {
                if (entry.samples.get(i)[0] == messageId) {
                    return false;
                }
            }
            entry.samples.add(new int[]{messageId, latency});
            Collections.sort(entry.samples, (a, b) -> Integer.compare(a[0], b[0]));
            while (entry.samples.size() > WINDOW) {
                entry.samples.remove(0);
            }
            return true;
        }
    }

    private static boolean markSpoke(long dialogId, long userId) {
        synchronized (LOCK) {
            ensureLoaded();
            final UserEntry entry = entries.get(key(dialogId, userId));
            if (entry == null) {
                return false;
            }
            final boolean hadSamples = !entry.samples.isEmpty();
            final boolean wasFlagged = trailingFastStreak(entry) >= (entry.level > 0 ? RELAPSE_STREAK : SUSPECT_STREAK);
            entry.samples.clear();
            if (wasFlagged) {
                entry.level = 1;
            }
            return hadSamples;
        }
    }

    private static int trailingFastStreak(UserEntry entry) {
        int streak = 0;
        for (int i = entry.samples.size() - 1; i >= 0; i--) {
            if (entry.samples.get(i)[1] <= FAST_SECONDS) {
                streak++;
            } else {
                break;
            }
        }
        return streak;
    }

    private static String key(long dialogId, long userId) {
        return dialogId + ":" + userId;
    }

    // ---- persistence: one compact string, one entry per line ----------------------------------

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
                final String[] head = line.split("\\|", 3);
                if (head.length < 3) {
                    continue;
                }
                final UserEntry entry = new UserEntry();
                entry.level = Integer.parseInt(head[1]);
                if (!head[2].isEmpty()) {
                    for (String sample : head[2].split(";")) {
                        final String[] parts = sample.split(",");
                        entry.samples.add(new int[]{Integer.parseInt(parts[0]), Integer.parseInt(parts[1])});
                    }
                }
                entries.put(head[0], entry);
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
                if (entry.samples.isEmpty() && entry.level == 0) {
                    continue;
                }
                if (builder.length() > 0) {
                    builder.append('\n');
                }
                builder.append(item.getKey()).append('|').append(entry.level).append('|');
                for (int i = 0; i < entry.samples.size(); i++) {
                    if (i > 0) {
                        builder.append(';');
                    }
                    builder.append(entry.samples.get(i)[0]).append(',').append(entry.samples.get(i)[1]);
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
