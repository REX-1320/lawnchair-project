package com.android.launcher3.widget.custom;

import android.content.ComponentName;
import android.content.Context;
import android.os.Parcel;

import com.android.launcher3.R;
import com.android.launcher3.widget.StackWidgetView;

public final class StackProWidgetProvider {

    public static final String PACKAGE_NAME = "android";
    public static final String CLASS_NAME = "#custom-widget-stack-pro";
    public static final ComponentName COMPONENT_NAME = new ComponentName(PACKAGE_NAME, CLASS_NAME);

    private StackProWidgetProvider() {
    }

    public static CustomAppWidgetProviderInfo getWidgetInfo(Context context) {
        CustomAppWidgetProviderInfo info = createFromTemplate(context);
        info.provider = COMPONENT_NAME;
        info.label = "Stack Pro";
        info.spanX = 4;
        info.spanY = 3;
        info.minSpanX = 3;
        info.minSpanY = 2;
        info.minResizeWidth = 40;
        info.minResizeHeight = 40;
        info.previewImage = R.drawable.stack_pro_widget_preview;
        info.icon = android.R.drawable.ic_menu_sort_by_size;
        return info;
    }

    public static StackWidgetView createView(Context context) {
        return new StackWidgetView(context);
    }

    private static CustomAppWidgetProviderInfo createFromTemplate(Context context) {
        android.appwidget.AppWidgetManager appWidgetManager =
                android.appwidget.AppWidgetManager.getInstance(context);
        java.util.List<android.appwidget.AppWidgetProviderInfo> providers = appWidgetManager
                .getInstalledProvidersForProfile(android.os.Process.myUserHandle());

        Parcel parcel = Parcel.obtain();
        try {
            if (!providers.isEmpty()) {
                providers.get(0).writeToParcel(parcel, 0);
            } else {
                parcel.writeString("");
                parcel.writeString("");
                parcel.writeString("");
                parcel.writeString("");
                parcel.writeString("");
                parcel.writeString("");
                parcel.writeInt(0);
                parcel.writeInt(1);
                parcel.writeInt(1);
                parcel.writeInt(0);
                parcel.writeInt(0);
                parcel.writeInt(0);
                parcel.writeInt(0);
                parcel.writeInt(0);
                parcel.writeInt(0);
                parcel.writeInt(0);
            }
            parcel.setDataPosition(0);
            return new CustomAppWidgetProviderInfo(parcel, false);
        } finally {
            parcel.recycle();
        }
    }
}
