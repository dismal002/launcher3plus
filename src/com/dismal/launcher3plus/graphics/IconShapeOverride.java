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
package com.dismal.launcher3plus.graphics;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;

import com.dismal.launcher3plus.LauncherAppState;
import com.dismal.launcher3plus.LauncherFiles;
import com.dismal.launcher3plus.LauncherModel;
import com.dismal.launcher3plus.Utilities;
import com.dismal.launcher3plus.util.LooperExecuter;
/**
 * Utility class to override shape of {@link android.graphics.drawable.AdaptiveIconDrawable}.
 */
@TargetApi(Build.VERSION_CODES.O)
public class IconShapeOverride {

    public static final String KEY_PREFERENCE = "pref_override_icon_shape";

    public static boolean isSupported(Context context) {
        return Utilities.isAtLeastO();
    }

    /** Loads the saved shape; called once at process start. */
    public static void apply(Context context) {
        IconShapeCompat.load(context);
    }

    private static String getAppliedValue(Context context) {
        return prefs(context).getString(KEY_PREFERENCE, "");
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(LauncherFiles.DEVICE_PREFERENCES_KEY, 0);
    }

    public static void handlePreferenceUi(ListPreference preference) {
        Context context = preference.getContext();
        preference.setValue(getAppliedValue(context));
        preference.setOnPreferenceChangeListener(new PreferenceChangeHandler(context));
    }

    private static class PreferenceChangeHandler implements OnPreferenceChangeListener {

        private final Context mContext;

        private PreferenceChangeHandler(Context context) {
            mContext = context;
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object o) {
            String newValue = (String) o;
            if (!getAppliedValue(mContext).equals(newValue)) {
                ((ListPreference) preference).setValue(newValue);
                new LooperExecuter(LauncherModel.getWorkerLooper()).execute(
                        new OverrideApplyHandler(mContext, newValue));
            }
            return false;
        }
    }

    private static class OverrideApplyHandler implements Runnable {

        private final Context mContext;
        private final String mValue;

        private OverrideApplyHandler(Context context, String value) {
            mContext = context;
            mValue = value;
        }

        @Override
        public void run() {
            prefs(mContext).edit().putString(KEY_PREFERENCE, mValue).commit();
            IconShapeCompat.load(mContext);

            LauncherAppState app = LauncherAppState.getInstance(mContext);
            app.getIconCache().clear();
            // Lawnchair clears its icon cache and reloads the model on the model
            // executor. This handler already runs on that executor.
            app.getModel().forceReload();
        }
    }
}
