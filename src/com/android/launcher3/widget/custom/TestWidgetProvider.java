package com.android.launcher3.widget.custom;

import android.content.ComponentName;
import android.content.Context;
import android.os.Parcel;

import com.android.launcher3.R;
import com.android.launcher3.widget.TestWidgetView;

public final class TestWidgetProvider {

    public static final String PACKAGE_NAME = "android";
    public static final String CLASS_NAME = "#custom-widget-test";
    public static final ComponentName COMPONENT_NAME = new ComponentName(PACKAGE_NAME, CLASS_NAME);

    private TestWidgetProvider() {
    }

    public static CustomAppWidgetProviderInfo getWidgetInfo(Context context) {
        CustomAppWidgetProviderInfo info = createFromTemplate(context);
        info.provider = COMPONENT_NAME;
        info.label = "Test Widget";
        info.spanX = 4;
        info.spanY = 2;
        info.minSpanX = 2;
        info.minSpanY = 1;
        info.minResizeWidth = 40;
        info.minResizeHeight = 40;
        info.previewImage = R.drawable.test_widget_preview;
        info.icon = android.R.drawable.ic_menu_info_details;
        return info;
    }

    public static TestWidgetView createView(Context context) {
        return new TestWidgetView(context);
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
