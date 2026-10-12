package top.nkbe.niagram.helpers;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileRefController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.RequestDelegate;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ChatActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;

/**
 * Sends copies of messages by referencing the media that already lives on Telegram's servers,
 * so nothing has to be downloaded and re-uploaded. When the server refuses the reference the
 * supplied fallback is run (the caller downloads the media and sends it the classic way).
 */
public class CopyReferenceSender {

    private static final int MAX_TRACKED_REFERENCE_FAILURES = 64;
    private static final int REFERENCE_REFRESH_TIMEOUT_MS = 30000;
    private static final LinkedHashSet<String> failedReferenceKeys = new LinkedHashSet<>();

    public static boolean canSendByReference(MessageObject messageObject) {
        if (messageObject == null || messageObject.messageOwner == null || messageObject.isAyuDeleted()) {
            return false;
        }
        if (messageObject.messageOwner.media == null || messageObject.messageOwner.media.ttl_seconds != 0
                || messageObject.isVoiceOnce() || messageObject.isRoundOnce()) {
            return false;
        }
        TLRPC.Photo photo = resolveReferencePhoto(messageObject);
        TLRPC.Document document = photo != null ? null : resolveReferenceDocument(messageObject);
        if (photo != null) {
            if (photo.id == 0 || photo.access_hash == 0 || photo.file_reference == null || photo.file_reference.length == 0) {
                return false;
            }
        } else if (document != null) {
            if (document.id == 0 || document.access_hash == 0 || document.file_reference == null || document.file_reference.length == 0) {
                return false;
            }
        } else {
            return false;
        }
        return !isFailedReference(messageObject);
    }

    public static boolean canSendGroupByReference(ArrayList<MessageObject> messages) {
        if (messages == null || messages.isEmpty()) {
            return false;
        }
        for (int i = 0; i < messages.size(); i++) {
            if (!canSendByReference(messages.get(i))) {
                return false;
            }
        }
        return true;
    }

    /** Ids of albums in which every message can be sent by reference. */
    public static HashSet<Long> resolveFullyReferenceCapableGroupIds(ArrayList<MessageObject> messages) {
        HashMap<Long, int[]> groupCounts = new HashMap<>();
        for (int i = 0; i < messages.size(); i++) {
            MessageObject messageObject = messages.get(i);
            long groupId = messageObject.getGroupIdForUse();
            if (groupId == 0) {
                continue;
            }
            int[] counts = groupCounts.get(groupId);
            if (counts == null) {
                counts = new int[2];
                groupCounts.put(groupId, counts);
            }
            counts[0]++;
            if (canSendByReference(messageObject)) {
                counts[1]++;
            }
        }
        HashSet<Long> capableGroupIds = new HashSet<>();
        for (HashMap.Entry<Long, int[]> entry : groupCounts.entrySet()) {
            int[] counts = entry.getValue();
            if (counts[0] > 0 && counts[0] == counts[1]) {
                capableGroupIds.add(entry.getKey());
            }
        }
        return capableGroupIds;
    }

    public static boolean canRouteByReference(MessageObject messageObject, HashSet<Long> fullyReferenceCapableGroupIds) {
        long groupId = messageObject.getGroupIdForUse();
        if (groupId != 0) {
            return fullyReferenceCapableGroupIds.contains(groupId);
        }
        return canSendByReference(messageObject);
    }

    /**
     * @param fallback run on the UI thread if the server refuses the reference
     * @return false when the request could not even be built; nothing was sent and fallback won't run
     */
    public static boolean sendByReference(int currentAccount, ArrayList<MessageObject> messages,
                                          long targetDialogId, MessageObject replyTopMsg,
                                          boolean notify, int scheduleDate, long monoForumPeerId,
                                          Runnable fallback) {
        if (messages == null || messages.isEmpty()) {
            return false;
        }
        TLRPC.InputPeer peer = MessagesController.getInstance(currentAccount).getInputPeer(targetDialogId);
        if (peer == null) {
            return false;
        }
        MessageObject carrier = messages.get(0);
        boolean invertMedia = carrier.messageOwner != null && carrier.messageOwner.invert_media;
        boolean silent = !notify;
        ArrayList<TLRPC.InputMedia> inputMedias = new ArrayList<>();
        TLObject request;
        if (messages.size() == 1) {
            TLRPC.TL_messages_sendMedia singleRequest = buildSingleMediaRequest(currentAccount, messages.get(0),
                    peer, replyTopMsg, silent, scheduleDate, monoForumPeerId, invertMedia);
            if (singleRequest == null) {
                return false;
            }
            inputMedias.add(singleRequest.media);
            request = singleRequest;
        } else {
            TLRPC.TL_messages_sendMultiMedia multiRequest = buildMultiMediaRequest(currentAccount, messages,
                    peer, replyTopMsg, silent, scheduleDate, invertMedia, monoForumPeerId);
            if (multiRequest == null) {
                return false;
            }
            for (int i = 0; i < multiRequest.multi_media.size(); i++) {
                inputMedias.add(multiRequest.multi_media.get(i).media);
            }
            request = multiRequest;
        }
        sendRequestWithRetry(currentAccount, request, messages, inputMedias, fallback);
        return true;
    }

    private static TLObject resolveInputPhotoOrDocument(TLRPC.InputMedia inputMedia) {
        if (inputMedia instanceof TLRPC.TL_inputMediaPhoto) {
            return ((TLRPC.TL_inputMediaPhoto) inputMedia).id;
        }
        if (inputMedia instanceof TLRPC.TL_inputMediaDocument) {
            return ((TLRPC.TL_inputMediaDocument) inputMedia).id;
        }
        return null;
    }

    private static TLRPC.TL_messages_sendMedia buildSingleMediaRequest(int currentAccount, MessageObject messageObject,
                                                                       TLRPC.InputPeer peer, MessageObject replyTopMsg,
                                                                       boolean silent, int scheduleDate, long monoForumPeerId,
                                                                       boolean invertMedia) {
        TLRPC.InputMedia inputMedia = buildInputMedia(messageObject);
        if (inputMedia == null) {
            return null;
        }
        TLRPC.TL_messages_sendMedia request = new TLRPC.TL_messages_sendMedia();
        request.peer = peer;
        request.media = inputMedia;
        request.random_id = SendMessagesHelper.getInstance(currentAccount).getNextRandomId();
        request.silent = silent;
        request.invert_media = invertMedia;
        CharSequence caption = ChatActivity.getMessageCaption(messageObject, null, null);
        request.message = caption != null ? caption.toString() : "";
        ArrayList<TLRPC.MessageEntity> entities = caption != null ? messageObject.messageOwner.entities : null;
        if (entities != null && !entities.isEmpty()) {
            request.entities = entities;
            request.flags |= 8;
        }
        request.reply_to = buildInputReplyTo(currentAccount, replyTopMsg, monoForumPeerId);
        if (request.reply_to != null) {
            request.flags |= 1;
        }
        if (scheduleDate != 0) {
            request.schedule_date = scheduleDate;
            request.flags |= 1024;
        }
        return request;
    }

    private static TLRPC.TL_messages_sendMultiMedia buildMultiMediaRequest(int currentAccount, ArrayList<MessageObject> messages,
                                                                           TLRPC.InputPeer peer, MessageObject replyTopMsg,
                                                                           boolean silent, int scheduleDate, boolean invertMedia, long monoForumPeerId) {
        TLRPC.TL_messages_sendMultiMedia request = new TLRPC.TL_messages_sendMultiMedia();
        request.peer = peer;
        request.silent = silent;
        request.invert_media = invertMedia;
        for (int i = 0; i < messages.size(); i++) {
            MessageObject messageObject = messages.get(i);
            TLRPC.InputMedia inputMedia = buildInputMedia(messageObject);
            if (inputMedia == null) {
                return null;
            }
            TLRPC.TL_inputSingleMedia singleMedia = new TLRPC.TL_inputSingleMedia();
            singleMedia.media = inputMedia;
            singleMedia.random_id = SendMessagesHelper.getInstance(currentAccount).getNextRandomId();
            CharSequence caption = ChatActivity.getMessageCaption(messageObject, null, null);
            singleMedia.message = caption != null ? caption.toString() : "";
            ArrayList<TLRPC.MessageEntity> entities = caption != null ? messageObject.messageOwner.entities : null;
            if (entities != null && !entities.isEmpty()) {
                singleMedia.entities = entities;
                singleMedia.flags |= 1;
            }
            request.multi_media.add(singleMedia);
        }
        request.reply_to = buildInputReplyTo(currentAccount, replyTopMsg, monoForumPeerId);
        if (request.reply_to != null) {
            request.flags |= 1;
        }
        if (scheduleDate != 0) {
            request.schedule_date = scheduleDate;
            request.flags |= 1024;
        }
        return request;
    }

    private static TLRPC.InputReplyTo buildInputReplyTo(int currentAccount, MessageObject replyTopMsg, long monoForumPeerId) {
        if (monoForumPeerId != 0) {
            if (replyTopMsg != null) {
                TLRPC.TL_inputReplyToMessage replyTo = new TLRPC.TL_inputReplyToMessage();
                replyTo.reply_to_msg_id = replyTopMsg.getId();
                replyTo.monoforum_peer_id = MessagesController.getInstance(currentAccount).getInputPeer(monoForumPeerId);
                replyTo.flags |= 32;
                return replyTo;
            }
            TLRPC.TL_inputReplyToMonoForum replyTo = new TLRPC.TL_inputReplyToMonoForum();
            replyTo.monoforum_peer_id = MessagesController.getInstance(currentAccount).getInputPeer(monoForumPeerId);
            return replyTo;
        }
        if (replyTopMsg != null) {
            return SendMessagesHelper.getInstance(currentAccount).createReplyInput(replyTopMsg.getId());
        }
        return null;
    }

    private static TLRPC.InputMedia buildInputMedia(MessageObject messageObject) {
        TLRPC.Photo photo = resolveReferencePhoto(messageObject);
        if (photo != null) {
            TLRPC.TL_inputMediaPhoto media = new TLRPC.TL_inputMediaPhoto();
            media.id = new TLRPC.TL_inputPhoto();
            media.id.id = photo.id;
            media.id.access_hash = photo.access_hash;
            media.id.file_reference = photo.file_reference;
            media.spoiler = messageObject.hasMediaSpoilers();
            return media;
        }
        TLRPC.Document document = resolveReferenceDocument(messageObject);
        if (document != null) {
            TLRPC.TL_inputMediaDocument media = new TLRPC.TL_inputMediaDocument();
            media.id = new TLRPC.TL_inputDocument();
            media.id.id = document.id;
            media.id.access_hash = document.access_hash;
            media.id.file_reference = document.file_reference;
            media.spoiler = messageObject.hasMediaSpoilers();
            return media;
        }
        return null;
    }

    private static TLRPC.Photo resolveReferencePhoto(MessageObject messageObject) {
        TLRPC.MessageMedia media = messageObject.messageOwner.media;
        if (media instanceof TLRPC.TL_messageMediaPhoto) {
            TLRPC.Photo photo = ((TLRPC.TL_messageMediaPhoto) media).photo;
            if (photo instanceof TLRPC.TL_photo) {
                return photo;
            }
        }
        return null;
    }

    private static TLRPC.Document resolveReferenceDocument(MessageObject messageObject) {
        TLRPC.MessageMedia media = messageObject.messageOwner.media;
        if (media instanceof TLRPC.TL_messageMediaDocument) {
            TLRPC.Document document = ((TLRPC.TL_messageMediaDocument) media).document;
            if (document instanceof TLRPC.TL_document) {
                return document;
            }
        }
        return null;
    }

    private static void sendRequestWithRetry(int currentAccount, TLObject request, ArrayList<MessageObject> messages,
                                             ArrayList<TLRPC.InputMedia> inputMedias, Runnable fallback) {
        boolean[] fallbackHandled = {false};
        boolean[] retryDispatched = {false};
        int[] pendingRefreshCallbacks = {inputMedias.size()};
        Runnable[] timeoutGuard = new Runnable[1];
        Runnable guardedFallback = () -> AndroidUtilities.runOnUIThread(() -> {
            if (fallbackHandled[0]) {
                return;
            }
            fallbackHandled[0] = true;
            AndroidUtilities.cancelRunOnUIThread(timeoutGuard[0]);
            markFailedReference(currentAccount, messages);
            fallback.run();
        });
        Runnable retryWithGuard = () -> {
            if (fallbackHandled[0] || retryDispatched[0]) {
                return;
            }
            if (pendingRefreshCallbacks[0] > 0 && --pendingRefreshCallbacks[0] > 0) {
                return;
            }
            retryDispatched[0] = true;
            AndroidUtilities.cancelRunOnUIThread(timeoutGuard[0]);
            sendAndProcess(currentAccount, request, (response, error) -> guardedFallback.run());
        };
        sendAndProcess(currentAccount, request, (response, error) -> {
            if (!FileRefController.isFileRefError(error.text)) {
                guardedFallback.run();
                return;
            }
            boolean anyRefreshRequested = false;
            for (int i = 0; i < inputMedias.size(); i++) {
                TLObject inputPhotoOrDocument = resolveInputPhotoOrDocument(inputMedias.get(i));
                if (inputPhotoOrDocument != null) {
                    FileRefController.getInstance(currentAccount).requestReference(messages.get(Math.min(i, messages.size() - 1)), inputPhotoOrDocument, (Runnable) retryWithGuard);
                    anyRefreshRequested = true;
                }
            }
            if (!anyRefreshRequested) {
                guardedFallback.run();
                return;
            }
            timeoutGuard[0] = guardedFallback;
            AndroidUtilities.runOnUIThread(guardedFallback, REFERENCE_REFRESH_TIMEOUT_MS);
        });
    }

    private static void sendAndProcess(int currentAccount, TLObject request, RequestDelegate delegate) {
        ConnectionsManager.getInstance(currentAccount).sendRequest(request, (response, error) -> {
            if (error == null) {
                MessagesController.getInstance(currentAccount).processUpdates((TLRPC.Updates) response, false);
                return;
            }
            delegate.run(response, error);
        });
    }

    private static void markFailedReference(int currentAccount, ArrayList<MessageObject> messages) {
        synchronized (failedReferenceKeys) {
            for (int i = 0; i < messages.size(); i++) {
                MessageObject messageObject = messages.get(i);
                failedReferenceKeys.add(currentAccount + ":" + messageObject.getDialogId() + ":" + messageObject.getId());
            }
            while (failedReferenceKeys.size() > MAX_TRACKED_REFERENCE_FAILURES) {
                Iterator<String> iterator = failedReferenceKeys.iterator();
                if (!iterator.hasNext()) {
                    break;
                }
                iterator.next();
                iterator.remove();
            }
        }
    }

    private static boolean isFailedReference(MessageObject messageObject) {
        synchronized (failedReferenceKeys) {
            return failedReferenceKeys.contains(messageObject.currentAccount + ":" + messageObject.getDialogId() + ":" + messageObject.getId());
        }
    }
}
