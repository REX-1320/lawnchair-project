package com.android.launcher3.model;

import static com.android.launcher3.Utilities.ATLEAST_S;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ActivityInfo;

import com.android.launcher3.InvariantDeviceProfile;
import com.android.launcher3.Utilities;
import com.android.launcher3.icons.BitmapInfo;
import com.android.launcher3.icons.IconCache;
import com.android.launcher3.pm.ShortcutConfigActivityInfo;
import com.android.launcher3.util.ComponentKey;
import com.android.launcher3.widget.LauncherAppWidgetProviderInfo;

/**
 * An wrapper over various items displayed in a widget picker,
 * {@link LauncherAppWidgetProviderInfo} & {@link ActivityInfo}. This provides easier access to
 * common attributes like spanX and spanY.
 */
public class WidgetItem extends ComponentKey {

    private static final ComponentName FALLBACK_COMPONENT = new ComponentName(
            "android", LauncherAppWidgetProviderInfo.CLS_CUSTOM_WIDGET_PREFIX + "fallback");

    public final LauncherAppWidgetProviderInfo widgetInfo;
    public final ShortcutConfigActivityInfo activityInfo;

    public BitmapInfo bitmap = BitmapInfo.LOW_RES_INFO;
    public final String label;
    public final CharSequence description;
    public final int spanX, spanY;

    public WidgetItem(LauncherAppWidgetProviderInfo info,
            InvariantDeviceProfile idp, IconCache iconCache, Context context) {
        super(getComponentSafe(info), getProfileSafe(info));

        sanitizeWidgetInfo(info, idp);

        label = getTitleSafe(iconCache, info);
        description = ATLEAST_S && info.loadDescription(context) != null ? info.loadDescription(context) : "";
        widgetInfo = info;
        activityInfo = null;

        spanX = Math.max(1, Math.min(info.spanX, idp.numColumns));
        spanY = Math.max(1, Math.min(info.spanY, idp.numRows));
    }

    private static ComponentName getComponentSafe(LauncherAppWidgetProviderInfo info) {
        return info.provider != null ? info.provider : FALLBACK_COMPONENT;
    }

    private static void sanitizeWidgetInfo(LauncherAppWidgetProviderInfo info,
            InvariantDeviceProfile idp) {
        if (info.provider == null) {
            info.provider = FALLBACK_COMPONENT;
        }
        if (info.spanX < 1) {
            info.spanX = 1;
        }
        if (info.spanY < 1) {
            info.spanY = 1;
        }
        info.spanX = Math.min(info.spanX, idp.numColumns);
        info.spanY = Math.min(info.spanY, idp.numRows);
        info.minSpanX = Math.max(1, info.minSpanX);
        info.minSpanY = Math.max(1, info.minSpanY);
        if (info.label == null || info.label.trim().isEmpty()) {
            info.label = "Widget";
        }
    }

    private static android.os.UserHandle getProfileSafe(LauncherAppWidgetProviderInfo info) {
        try {
            return info.getUser();
        } catch (Exception e) {
            // Catch any exception from getUser() as last resort fallback
            return android.os.Process.myUserHandle();
        }
    }

    private static String getTitleSafe(IconCache iconCache, LauncherAppWidgetProviderInfo info) {
        try {
            String title = iconCache.getTitleNoCache(info);
            if (title == null || title.trim().isEmpty()) {
                return info.label != null && !info.label.trim().isEmpty() ? info.label : "Widget";
            }
            return title;
        } catch (Exception e) {
            // Custom widgets may have null applicationInfo or other issues
            // Fall back to widget label
            return info.label != null && !info.label.trim().isEmpty() ? info.label : "Widget";
        }
    }

    public WidgetItem(ShortcutConfigActivityInfo info, IconCache iconCache) {
        super(info.getComponent(), info.getUser());
        String shortcutLabel = info.isPersistable()
            ? iconCache.getTitleNoCache(info)
            : Utilities.trim(info.getLabel());
        label = shortcutLabel == null || shortcutLabel.trim().isEmpty()
            ? "Shortcut"
            : shortcutLabel;
        description = null;
        widgetInfo = null;
        activityInfo = info;
        spanX = spanY = 1;
    }

    /**
     * Returns {@code true} if this {@link WidgetItem} has the same type as the given
     * {@code otherItem}.
     *
     * For example, both items are widgets or both items are shortcuts.
     */
    public boolean hasSameType(WidgetItem otherItem) {
        if (widgetInfo != null && otherItem.widgetInfo != null) {
            return true;
        }
        if (activityInfo != null && otherItem.activityInfo != null) {
            return true;
        }
        return false;
    }

    /** Returns whether this {@link WidgetItem} is for a shortcut rather than an app widget. */
    public boolean isShortcut() {
        return activityInfo != null;
    }
}
