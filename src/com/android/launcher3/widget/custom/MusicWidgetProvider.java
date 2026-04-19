/*
 * Copyright (C) 2025 The Android Open Source Project
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
import android.os.Parcel;

/**
 * Built-in custom widget provider for Music Pro widget.
 * Manages widget metadata and creation without using AppWidgetProvider.
 */
public class MusicWidgetProvider {

    public static final String PACKAGE_NAME = "android";
    public static final String CLASS_NAME = "#custom-widget-music-pro";
    public static final ComponentName COMPONENT_NAME = new ComponentName(PACKAGE_NAME, CLASS_NAME);

    public static CustomAppWidgetProviderInfo getWidgetInfo(Context context) {
        CustomAppWidgetProviderInfo info = createFromTemplate(context);
        info.provider = COMPONENT_NAME;
        info.label = "Music Pro";
        info.spanX = 4;
        info.spanY = 2;
        info.minSpanX = 4;
        info.minSpanY = 2;
        info.previewImage = android.R.drawable.ic_media_play;
        info.icon = android.R.drawable.ic_media_play;
        return info;
    }

    private static CustomAppWidgetProviderInfo createFromTemplate(Context context) {
        // Create from default parcel template to maintain AppWidget compatibility layer
        Parcel parcel = Parcel.obtain();
        try {
            // Write minimal valid AppWidgetProviderInfo data
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

            parcel.setDataPosition(0);
            return new CustomAppWidgetProviderInfo(parcel, false);
        } finally {
            parcel.recycle();
        }
    }
}
