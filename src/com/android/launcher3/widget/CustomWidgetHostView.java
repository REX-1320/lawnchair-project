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

package com.android.launcher3.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.android.launcher3.model.data.LauncherAppWidgetInfo;
import com.android.launcher3.widget.custom.CustomAppWidgetProviderInfo;
import com.android.launcher3.widget.custom.MusicWidgetProvider;

/**
 * Host view wrapper for custom widgets that use Java views instead of RemoteViews.
 */
public class CustomWidgetHostView extends FrameLayout {

    private int mAppWidgetId;
    private LauncherAppWidgetProviderInfo mAppWidgetInfo;

    public CustomWidgetHostView(Context context) {
        super(context);
    }

    public CustomWidgetHostView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setAppWidget(int appWidgetId, LauncherAppWidgetProviderInfo info) {
        mAppWidgetId = appWidgetId;
        mAppWidgetInfo = info;
        inflateCustomWidget();
    }

    private void inflateCustomWidget() {
        removeAllViews();

        if (mAppWidgetInfo == null || !(mAppWidgetInfo instanceof CustomAppWidgetProviderInfo)) {
            return;
        }

        if (mAppWidgetInfo.provider.equals(MusicWidgetProvider.COMPONENT_NAME)) {
            MusicWidgetView musicView = new MusicWidgetView(getContext());
            LayoutParams params = new LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT);
            addView(musicView, params);
        }
    }

    public int getAppWidgetId() {
        return mAppWidgetId;
    }

    public LauncherAppWidgetProviderInfo getAppWidgetInfo() {
        return mAppWidgetInfo;
    }
}

