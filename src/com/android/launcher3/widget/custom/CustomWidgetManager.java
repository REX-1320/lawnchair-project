/*
 * Copyright (C) 2019 The Android Open Source Project
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

import static com.android.launcher3.model.data.LauncherAppWidgetInfo.CUSTOM_WIDGET_ID;
import static com.android.launcher3.util.Executors.MAIN_EXECUTOR;
import static com.android.launcher3.widget.LauncherAppWidgetProviderInfo.CLS_CUSTOM_WIDGET_PREFIX;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Context;
import android.os.Parcel;
import android.os.Process;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.android.launcher3.dagger.ApplicationContext;
import com.android.launcher3.dagger.LauncherAppSingleton;
import com.android.launcher3.dagger.LauncherBaseAppComponent;
import com.android.launcher3.util.DaggerSingletonObject;
import com.android.launcher3.util.DaggerSingletonTracker;
import com.android.launcher3.util.PluginManagerWrapper;
import com.android.launcher3.util.SafeCloseable;
import com.android.launcher3.widget.LauncherAppWidgetHostView;
import com.android.launcher3.widget.LauncherAppWidgetProviderInfo;
import com.android.systemui.plugins.CustomWidgetPlugin;
import com.android.systemui.plugins.PluginListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

import javax.inject.Inject;

/**
 * CustomWidgetManager handles custom widgets implemented as a plugin.
 */
@LauncherAppSingleton
public class CustomWidgetManager implements PluginListener<CustomWidgetPlugin> {

    public static final DaggerSingletonObject<CustomWidgetManager> INSTANCE =
            new DaggerSingletonObject<>(LauncherBaseAppComponent::getCustomWidgetManager);

    private static final String TAG = "CustomWidgetManager";
    private static final String PLUGIN_PKG = "android";
    private final Context mContext;
    private final HashMap<ComponentName, CustomWidgetPlugin> mPlugins;
    private final List<CustomAppWidgetProviderInfo> mCustomWidgets;
    private final HashMap<ComponentName, BuiltInCustomWidgetFactory> mBuiltInWidgets;
    private final List<Runnable> mWidgetRefreshCallbacks = new CopyOnWriteArrayList<>();
    private final @NonNull AppWidgetManager mAppWidgetManager;

    private interface BuiltInCustomWidgetFactory {
        CustomAppWidgetProviderInfo createInfo(Context context);
        View createView(Context context);
    }

    @Inject
    CustomWidgetManager(@ApplicationContext Context context, PluginManagerWrapper pluginManager,
            DaggerSingletonTracker tracker) {
        this(context, pluginManager, AppWidgetManager.getInstance(context), tracker);
    }

    @VisibleForTesting
    CustomWidgetManager(@ApplicationContext Context context,
            PluginManagerWrapper pluginManager,
            @NonNull AppWidgetManager widgetManager,
            DaggerSingletonTracker tracker) {
        mContext = context;
        mAppWidgetManager = widgetManager;
        mPlugins = new HashMap<>();
        mCustomWidgets = new ArrayList<>();
        mBuiltInWidgets = new HashMap<>();

        pluginManager.addPluginListener(this, CustomWidgetPlugin.class, true);
        tracker.addCloseable(() -> pluginManager.removePluginListener(this));

        android.util.Log.d(TAG, "CustomWidgetManager initialized");
        registerBuiltInCustomWidgets(context);
        android.util.Log.d(TAG, "Built-in widgets registered. Total: " + mBuiltInWidgets.size());
    }

    private void registerBuiltInCustomWidgets(Context context) {
        BuiltInCustomWidgetFactory stackProFactory = new BuiltInCustomWidgetFactory() {
            @Override
            public CustomAppWidgetProviderInfo createInfo(Context context) {
                return StackProWidgetProvider.getWidgetInfo(context);
            }

            @Override
            public View createView(Context context) {
                return StackProWidgetProvider.createView(context);
            }
        };
        registerBuiltInWidget(StackProWidgetProvider.COMPONENT_NAME, stackProFactory, context);
        registerBuiltInWidget(new ComponentName("android", "StackProWidgetProvider"), stackProFactory, context);

        BuiltInCustomWidgetFactory testFactory = new BuiltInCustomWidgetFactory() {
            @Override
            public CustomAppWidgetProviderInfo createInfo(Context context) {
                return TestWidgetProvider.getWidgetInfo(context);
            }

            @Override
            public View createView(Context context) {
                return TestWidgetProvider.createView(context);
            }
        };
        registerBuiltInWidget(TestWidgetProvider.COMPONENT_NAME, testFactory, context);
        registerBuiltInWidget(new ComponentName("android", "TestWidgetProvider"), testFactory, context);
    }

    private void registerBuiltInWidget(ComponentName componentName,
            BuiltInCustomWidgetFactory factory, Context context) {
        mBuiltInWidgets.put(componentName, factory);
        android.util.Log.d(TAG, "Registering built-in widget: " + componentName.flattenToString());
        try {
            CustomAppWidgetProviderInfo info = factory.createInfo(context);
            if (info != null) {
                info.initSpans(context, com.android.launcher3.LauncherAppState.getIDP(context));
                mCustomWidgets.add(info);
                android.util.Log.d(TAG, "Successfully registered widget: " + componentName + ", label: " + info.label);
            } else {
                android.util.Log.w(TAG, "Factory returned null info for widget: " + componentName);
            }
        } catch (Exception e) {
            android.util.Log.w(TAG, "Failed to register custom widget: " + componentName, e);
        }
    }

    @Override
    public void onPluginConnected(CustomWidgetPlugin plugin, Context context) {
        CustomAppWidgetProviderInfo info = getAndAddInfo(new ComponentName(
                PLUGIN_PKG, CLS_CUSTOM_WIDGET_PREFIX + plugin.getClass().getName()));
        if (info != null) {
            plugin.updateWidgetInfo(info, mContext);
            mPlugins.put(info.provider, plugin);
            mWidgetRefreshCallbacks.forEach(MAIN_EXECUTOR::execute);
        }
    }

    @Override
    public void onPluginDisconnected(CustomWidgetPlugin plugin) {
        // Leave the providerInfo as plugins can get disconnected/reconnected multiple times
        mPlugins.values().remove(plugin);
        mWidgetRefreshCallbacks.forEach(MAIN_EXECUTOR::execute);
    }

    @VisibleForTesting
    @NonNull
    Map<ComponentName, CustomWidgetPlugin> getPlugins() {
        return mPlugins;
    }

    /**
     * Inject a callback function to refresh the widgets.
     * @return a closeable to remove this callback
     */
    public SafeCloseable addWidgetRefreshCallback(Runnable callback) {
        mWidgetRefreshCallbacks.add(callback);
        return () -> mWidgetRefreshCallbacks.remove(callback);
    }

    /**
     * Callback method to inform a plugin it's corresponding widget has been created.
     */
    public void onViewCreated(LauncherAppWidgetHostView view) {
        if (!(view.getAppWidgetInfo() instanceof CustomAppWidgetProviderInfo info)
                || info.provider == null) {
            return;
        }
        CustomWidgetPlugin plugin = mPlugins.get(info.provider);
        if (plugin != null) {
            plugin.onViewCreated(view);
        }
    }

    /**
     * Returns the stream of custom widgets.
     */
    @NonNull
    public Stream<CustomAppWidgetProviderInfo> stream() {
        return mCustomWidgets.stream();
    }

    /**
     * Returns the widget provider in respect to given widget id.
     */
    @Nullable
    public LauncherAppWidgetProviderInfo getWidgetProvider(ComponentName cn) {
        android.util.Log.d(TAG, "getWidgetProvider called for: " + cn);
        LauncherAppWidgetProviderInfo info = mCustomWidgets.stream()
                .filter(w -> w.getComponent().equals(cn)).findAny().orElse(null);
        if (info != null) {
            android.util.Log.d(TAG, "Found widget in mCustomWidgets: " + cn);
            return info;
        }

        android.util.Log.d(TAG, "Widget not in mCustomWidgets, checking mBuiltInWidgets");
        BuiltInCustomWidgetFactory builtInFactory = mBuiltInWidgets.get(cn);
        if (builtInFactory != null) {
            try {
                android.util.Log.d(TAG, "Found built-in factory for: " + cn);
                CustomAppWidgetProviderInfo builtInInfo = builtInFactory.createInfo(mContext);
                if (builtInInfo != null) {
                    builtInInfo.initSpans(mContext, com.android.launcher3.LauncherAppState.getIDP(mContext));
                    mCustomWidgets.add(builtInInfo);
                    android.util.Log.d(TAG, "Created built-in widget provider: " + cn);
                    return builtInInfo;
                } else {
                    android.util.Log.e(TAG, "Built-in factory returned null info for: " + cn);
                    return null;
                }
            } catch (Exception e) {
                android.util.Log.w(TAG, "Failed to create built-in provider: " + cn, e);
                return null;
            }
        }

        // If the info is not present, add a placeholder info since the
        // plugin might get loaded later
        android.util.Log.d(TAG, "No built-in factory found, creating placeholder for: " + cn);
        return getAndAddInfo(cn);
    }

    @Nullable
    public View createCustomWidgetView(@NonNull LauncherAppWidgetProviderInfo info,
            @NonNull Context context) {
        android.util.Log.d(TAG, "createCustomWidgetView called for: " + info.provider);
        
        if (info.provider == null) {
            android.util.Log.e(TAG, "ERROR: createCustomWidgetView - provider is NULL for widget: " + info.label);
            return null;
        }

        android.util.Log.d(TAG, "Looking up factory for provider: " + info.provider.flattenToString());
        BuiltInCustomWidgetFactory builtInFactory = mBuiltInWidgets.get(info.provider);
        if (builtInFactory == null) {
            android.util.Log.e(TAG, "ERROR: createCustomWidgetView - No factory registered for provider: " + info.provider.flattenToString());
            android.util.Log.e(TAG, "Registered factories: " + mBuiltInWidgets.keySet());
            return null;
        }

        try {
            android.util.Log.d(TAG, "Creating view from factory for: " + info.provider);
            View result = builtInFactory.createView(context);
            if (result == null) {
                android.util.Log.e(TAG, "ERROR: Factory returned null view for: " + info.provider);
            } else {
                android.util.Log.d(TAG, "Successfully created view for: " + info.provider);
            }
            return result;
        } catch (Exception e) {
            android.util.Log.e(TAG, "EXCEPTION: Failed to create custom widget view: " + info.provider, e);
            android.util.Log.e(TAG, "Stack trace:", e);
            return null;
        }
    }

    /**
     * Returns an id to set as the appWidgetId for a custom widget.
     */
    public int allocateCustomAppWidgetId(ComponentName componentName) {
        LauncherAppWidgetProviderInfo providerInfo = getWidgetProvider(componentName);
        if (providerInfo == null) {
            return CUSTOM_WIDGET_ID;
        }

        int index = -1;
        for (int i = 0; i < mCustomWidgets.size(); i++) {
            if (componentName.equals(mCustomWidgets.get(i).getComponent())) {
                index = i;
                break;
            }
        }

        if (index < 0 && providerInfo instanceof CustomAppWidgetProviderInfo customInfo) {
            mCustomWidgets.add(customInfo);
            index = mCustomWidgets.size() - 1;
        }

        return CUSTOM_WIDGET_ID - index;
    }

    @Nullable
    private CustomAppWidgetProviderInfo getAndAddInfo(ComponentName cn) {
        for (CustomAppWidgetProviderInfo info : mCustomWidgets) {
            if (info.provider.equals(cn)) return info;
        }

        List<AppWidgetProviderInfo> providers = mAppWidgetManager
                .getInstalledProvidersForProfile(Process.myUserHandle());
        if (providers.isEmpty()) return null;
        Parcel parcel = Parcel.obtain();
        providers.get(0).writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        CustomAppWidgetProviderInfo info = new CustomAppWidgetProviderInfo(parcel, false);
        parcel.recycle();

        info.provider = cn;
        info.initialLayout = 0;
        mCustomWidgets.add(info);
        return info;
    }
}
