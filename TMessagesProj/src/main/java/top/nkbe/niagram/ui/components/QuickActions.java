package top.nkbe.niagram.ui.components;

import static org.telegram.messenger.AndroidUtilities.dp;
import static org.telegram.messenger.LocaleController.getString;
import static org.telegram.ui.ChatActivity.OPTION_COPY;
import static org.telegram.ui.ChatActivity.OPTION_COPY_LINK;
import static org.telegram.ui.ChatActivity.OPTION_DELETE;
import static org.telegram.ui.ChatActivity.OPTION_EDIT;
import static org.telegram.ui.ChatActivity.OPTION_FORWARD;
import static org.telegram.ui.ChatActivity.OPTION_REPLY;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;

import org.telegram.messenger.MessageObject;
import org.telegram.messenger.R;
import org.telegram.messenger.Utilities;
import org.telegram.ui.ActionBar.ActionBarPopupWindow.ActionBarPopupWindowLayout;
import org.telegram.ui.ActionBar.ActionBarPopupWindow.GapView;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.ScaleStateListAnimator;

import java.util.ArrayList;

@SuppressLint("ViewConstructor")
public final class QuickActions extends LinearLayout {

    public static final int OPTION_FORWARD_NO_QUOTE = 2011;
    public static final int OPTION_REPLY_PRIVATE = 2033;

    private final Theme.ResourcesProvider resourcesProvider;
    private View firstEnabledButton;

    private QuickActions(Context context, Theme.ResourcesProvider resourcesProvider) {
        super(context);
        this.resourcesProvider = resourcesProvider;
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER);
        setPadding(dp(4), dp(2), dp(4), dp(2));
    }

    public static QuickActions create(Context context, MessageObject message,
                                      ArrayList<Integer> icons, ArrayList<CharSequence> items,
                                      ArrayList<Integer> options, int copyPhotoOption, boolean replyPrivately,
                                      Utilities.Callback<Integer> onOption, Theme.ResourcesProvider resourcesProvider) {
        int reply = options.indexOf(OPTION_REPLY);
        int copy = options.indexOf(OPTION_COPY);
        int photo = options.indexOf(copyPhotoOption);
        int link = options.indexOf(OPTION_COPY_LINK);
        int delete = message.messageOwner.ttl_period != 0 || message.isPaidSuggestedPostProtected() ? -1 : options.indexOf(OPTION_DELETE);
        int edit = options.indexOf(OPTION_EDIT);
        int forward = options.indexOf(OPTION_FORWARD);
        int[] indexes = {
                reply,
                copy >= 0 ? copy : photo >= 0 ? photo : delete >= 0 ? link : -1,
                delete >= 0 ? delete : copy >= 0 && photo >= 0 ? photo : link,
                edit >= 0 ? edit : forward
        };
        if (indexes[0] < 0 && indexes[1] < 0 && indexes[2] < 0 && indexes[3] < 0) {
            return null;
        }

        QuickActions row = new QuickActions(context, resourcesProvider);
        int[] defaultIcons = {R.drawable.menu_reply, R.drawable.msg_copy, R.drawable.msg_link, R.drawable.msg_forward};
        int[] defaultTitles = {R.string.Reply, R.string.Copy, R.string.CopyLink, R.string.Forward};
        for (int i = 0; i < indexes.length; i++) {
            int index = indexes[i];
            ImageButton button = new ImageButton(context);
            button.setScaleType(ImageView.ScaleType.CENTER);
            button.setImageResource(index >= 0 ? icons.get(index) : defaultIcons[i]);
            button.setContentDescription(index >= 0 ? items.get(index) : getString(defaultTitles[i]));
            button.setEnabled(index >= 0);
            button.setFocusable(index >= 0);
            button.setAlpha(index >= 0 ? 1f : .4f);
            ScaleStateListAnimator.apply(button);
            row.addView(button, LayoutHelper.createLinear(48, 48, 1f));

            if (index >= 0) {
                if (row.firstEnabledButton == null) {
                    row.firstEnabledButton = button;
                }
                int option = options.get(index);
                button.setOnClickListener(view -> onOption.run(option));
                int longClickIndex = -1;
                if (index == copy) {
                    longClickIndex = message.isWebpage() && photo >= 0 && indexes[2] != photo ? photo : link;
                } else if (index == photo) {
                    longClickIndex = link;
                }
                if (longClickIndex >= 0) {
                    int longClickOption = options.get(longClickIndex);
                    button.setOnLongClickListener(view -> {
                        onOption.run(longClickOption);
                        return true;
                    });
                    ViewCompat.replaceAccessibilityAction(button, AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_LONG_CLICK,
                            items.get(longClickIndex), (view, arguments) -> view.performLongClick());
                }
                if (index == forward) {
                    button.setOnLongClickListener(view -> {
                        onOption.run(OPTION_FORWARD_NO_QUOTE);
                        return true;
                    });
                }
                if (index == reply && replyPrivately) {
                    button.setOnLongClickListener(view -> {
                        onOption.run(OPTION_REPLY_PRIVATE);
                        return true;
                    });
                }
            }
        }

        for (int i = options.size() - 1; i >= 0; i--) {
            if (i != indexes[0] && i != indexes[1] && i != indexes[2] && i != indexes[3]) {
                continue;
            }
            icons.remove(i);
            items.remove(i);
            options.remove(i);
        }
        row.updateColors();
        return row;
    }

    public void addTo(ActionBarPopupWindowLayout popup) {
        for (int i = popup.getItemsCount() - 1; i >= 0; i--) {
            View item = popup.getItemAt(i);
            if (item.getVisibility() == VISIBLE) {
                if (!(item instanceof GapView)) {
                    popup.addView(new GapView(getContext(), resourcesProvider), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 8));
                }
                break;
            }
        }
        popup.addView(this, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
    }

    public void updateColors() {
        int iconColor = Theme.getColor(Theme.key_actionBarDefaultSubmenuItemIcon, resourcesProvider);
        int selectorColor = Theme.getColor(Theme.key_dialogButtonSelector, resourcesProvider);
        for (int i = 0; i < getChildCount(); i++) {
            ImageButton button = (ImageButton) getChildAt(i);
            button.setColorFilter(new PorterDuffColorFilter(iconColor, PorterDuff.Mode.MULTIPLY));
            button.setBackground(Theme.createSelectorDrawable(selectorColor, Theme.RIPPLE_MASK_CIRCLE_20DP));
        }
    }

    public View getFirstEnabledButton() {
        return firstEnabledButton;
    }
}
