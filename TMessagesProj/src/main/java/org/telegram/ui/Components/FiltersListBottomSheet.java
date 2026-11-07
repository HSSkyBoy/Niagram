package org.telegram.ui.Components;

import static org.telegram.messenger.AndroidUtilities.dp;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AccountInstance;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.Emoji;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.Theme;
                ThemeBind.textColor(cell.getTextView(), Theme.key_dialogTextBlack);
                int icon;
                if ((filter.flags & MessagesController.DIALOG_FILTER_FLAG_ALL_CHATS) == (MessagesController.DIALOG_FILTER_FLAG_CONTACTS | MessagesController.DIALOG_FILTER_FLAG_NON_CONTACTS)) {
                    icon = R.drawable.msg_openprofile;
                } else if ((filter.flags & MessagesController.DIALOG_FILTER_FLAG_EXCLUDE_READ) != 0 && (filter.flags & MessagesController.DIALOG_FILTER_FLAG_ALL_CHATS) == MessagesController.DIALOG_FILTER_FLAG_ALL_CHATS) {
                    icon = R.drawable.msg_markunread;
                } else if ((filter.flags & MessagesController.DIALOG_FILTER_FLAG_ALL_CHATS) == MessagesController.DIALOG_FILTER_FLAG_CHANNELS) {
                    icon = R.drawable.msg_channel;
                } else if ((filter.flags & MessagesController.DIALOG_FILTER_FLAG_ALL_CHATS) == MessagesController.DIALOG_FILTER_FLAG_GROUPS) {
                    icon = R.drawable.msg_groups;
                } else if ((filter.flags & MessagesController.DIALOG_FILTER_FLAG_ALL_CHATS) == MessagesController.DIALOG_FILTER_FLAG_CONTACTS) {
                    icon = R.drawable.msg_contacts;
                } else if ((filter.flags & MessagesController.DIALOG_FILTER_FLAG_ALL_CHATS) == MessagesController.DIALOG_FILTER_FLAG_BOTS) {
                    icon = R.drawable.msg_bots;
                } else {
                    icon = R.drawable.msg_folders;
                }
                CharSequence title = filter.name;
                title = Emoji.replaceEmoji(title, cell.getTextView().getPaint().getFontMetricsInt(), false);
                title = MessageObject.replaceAnimatedEmoji(title, filter.entities, cell.getTextView().getPaint().getFontMetricsInt());
                cell.setTextAndIcon(title, 0, new FolderDrawable(getContext(), FolderIconHelper.getTabIcon(filter.emoticon), filter.color), false);
                cell.getTextView().setEmojiColor(Theme.getColor(Theme.key_featuredStickers_addButton, resourcesProvider));
                boolean isChecked = true;
                for (int i = 0; i < selectedDialogs.size(); ++i) {
                    long did = selectedDialogs.get(i);
                    if (!filter.includesDialog(AccountInstance.getInstance(currentAccount), did)) {
                        isChecked = false;
                    }
                }
                cell.setChecked(isChecked);
            } else {
                cell.getImageView().setColorFilter(null);
                Drawable drawable1 = context.getResources().getDrawable(R.drawable.poll_add_circle);
                Drawable drawable2 = context.getResources().getDrawable(R.drawable.poll_add_plus);
                ThemeBind.drawableColorFilter(cell.getImageView(), drawable1, Theme.key_switchTrackChecked, PorterDuff.Mode.MULTIPLY);
                ThemeBind.drawableColorFilter(cell.getImageView(), drawable2, Theme.key_checkboxCheck, PorterDuff.Mode.MULTIPLY);
                CombinedDrawable combinedDrawable = new CombinedDrawable(drawable1, drawable2);
                ThemeBind.textColor(cell.getTextView(), Theme.key_windowBackgroundWhiteBlueText4);
                cell.setTextAndIcon(LocaleController.getString(R.string.CreateNewFilter), combinedDrawable);
            }
        }
    }
}
