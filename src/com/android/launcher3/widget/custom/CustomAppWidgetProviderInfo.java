/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.launcher3.widget.custom;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Process;

import androidx.annotation.VisibleForTesting;

import com.android.launcher3.InvariantDeviceProfile;
import com.android.launcher3.Utilities;
import com.android.launcher3.widget.LauncherAppWidgetProviderInfo;

import java.lang.reflect.Field;

/**
 * Custom app widget provider info that can be used as a widget, but provide extra functionality
 * by allowing custom code and views.
 */
public class CustomAppWidgetProviderInfo extends LauncherAppWidgetProviderInfo
        implements Parcelable {

    public CustomAppWidgetProviderInfo(Parcel parcel, boolean readSelf) {
        super(parcel);
        if (readSelf) {
            String pkg = parcel.readString();
            String cls = parcel.readString();
            provider = new ComponentName(pkg, cls.startsWith(CLS_CUSTOM_WIDGET_PREFIX)
                    ? cls : CLS_CUSTOM_WIDGET_PREFIX + cls);

            label = parcel.readString();
            initialLayout = parcel.readInt();
            icon = parcel.readInt();
            previewImage = parcel.readInt();

            resizeMode = parcel.readInt();
            spanX = parcel.readInt();
            spanY = parcel.readInt();
            minSpanX = parcel.readInt();
            minSpanY = parcel.readInt();
        }
        setupProviderInfo();
    }

    @VisibleForTesting
    public CustomAppWidgetProviderInfo() {}

    @Override
    public void initSpans(Context context, InvariantDeviceProfile idp) {
        super.initSpans(context, idp);
        mIsMinSizeFulfilled = Math.min(spanX, minSpanX) <= idp.numColumns
                && Math.min(spanY, minSpanY) <= idp.numRows;
    }

    @Override
    public CharSequence getLabel() {
        return Utilities.trim(label);
    }

    @Override
    public ActivityInfo getActivityInfo() {
        ActivityInfo ai = new ActivityInfo();
        ai.applicationInfo = getApplicationInfo();
        ai.packageName = provider.getPackageName();
        ai.name = provider.getClassName();
        return ai;
    }

    @Override
    public ApplicationInfo getApplicationInfo() {
        // Create synthetic ApplicationInfo for custom widgets
        // Note: Don't call super.getApplicationInfo() as it may call getActivityInfo()
        // which in turn calls this method, creating infinite recursion
        ApplicationInfo ai = new ApplicationInfo();
        ai.packageName = provider.getPackageName();
        ai.flags = ApplicationInfo.FLAG_INSTALLED;
        ai.uid = Process.myUid();
        return ai;
    }

    public void setupProviderInfo() {
        if (provider == null) return;
        try {
            Field f = android.appwidget.AppWidgetProviderInfo.class.getDeclaredField("providerInfo");
            f.setAccessible(true);
            ActivityInfo activityInfo = getActivityInfo();
            f.set(this, activityInfo);
        } catch (NoSuchFieldException | IllegalAccessException ignored) {
            // Field may not exist or be accessible on some Android versions.
            // The getActivityInfo() override will provide fallback values when getProfile() is called.
        } catch (Exception e) {
            // Catch other exceptions and log for debugging
            android.util.Log.w("CustomAppWidgetProviderInfo", "Error setting providerInfo: " + e.getMessage());
        }
    }

    @Override
    public String toString() {
        return "WidgetProviderInfo(" + provider + ")";
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        super.writeToParcel(out, flags);
        out.writeString(provider.getPackageName());
        out.writeString(provider.getClassName());

        out.writeString(label);
        out.writeInt(initialLayout);
        out.writeInt(icon);
        out.writeInt(previewImage);

        out.writeInt(resizeMode);
        out.writeInt(spanX);
        out.writeInt(spanY);
        out.writeInt(minSpanX);
        out.writeInt(minSpanY);
    }

    public static final Parcelable.Creator<CustomAppWidgetProviderInfo> CREATOR =
            new Parcelable.Creator<>() {

        @Override
        public CustomAppWidgetProviderInfo createFromParcel(Parcel parcel) {
            return new CustomAppWidgetProviderInfo(parcel, true);
        }

        @Override
        public CustomAppWidgetProviderInfo[] newArray(int size) {
            return new CustomAppWidgetProviderInfo[size];
        }
    };
}
