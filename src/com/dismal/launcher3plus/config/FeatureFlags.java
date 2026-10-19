package com.dismal.launcher3plus.config;

import android.content.Context;
import android.content.res.Configuration;

import com.dismal.launcher3plus.Utilities;

public class FeatureFlags {
    public static final boolean IS_DOGFOOD_BUILD = false;
    public static final String PREF_SHOW_ALL_APPS_BUTTON = "pref_show_all_apps_button";
    public static final String PREF_SHOW_QUICK_SEARCH_BOX = "pref_show_quick_search_box";
    public static final String PREF_QUICK_SEARCH_BOX_POSITION = "pref_quick_search_box_position";
    public static final String PREF_ALL_APPS_SCROLL_MODE = "pref_all_apps_scroll_mode";
    public static boolean NO_ALL_APPS_ICON = true;
    public static boolean QSB_ENABLED = true;
    public static boolean QSB_ON_FIRST_SCREEN = true;
    public static boolean QSB_ON_HOTSEAT = false;
    public static final boolean LIGHT_STATUS_BAR = false;
    public static final boolean LAUNCHER3_ALL_APPS_PULL_UP = true;
    public static final boolean LAUNCHER3_ALLAPPS_PULL_UP = true;
    public static final boolean LAUNCHER3_LEGACY_FOLDER_ICON = false;
    public static final boolean LAUNCHER3_UPDATE_SOFT_INPUT_MODE = false;
    public static final boolean PULLDOWN_SEARCH = false;
    public static final boolean LAUNCHER3_DISABLE_ICON_NORMALIZATION = false;
    public static final boolean LAUNCHER3_DISABLE_PINCH_TO_OVERVIEW = false;
    public static final boolean LAUNCHER3_PROMISE_APPS_IN_ALL_APPS = false;
    public static final boolean LAUNCHER3_PROMISE_APPS_NEW_INSTALLS = false;
    public static final boolean LAUNCHER3_FOLDER_TRANSITION = true;
    public static final boolean LAUNCHER3_SPRING_ICONS = true;
    public static final boolean LAUNCHER3_ICON_SHAPES = true;
    public static final boolean LAUNCHER3_NOTIFICATION_DOTS = true;
    public static final boolean LAUNCHER3_DISCOVERY_ENABLED = false;
    public static final boolean LAUNCHER3_SWIPE_BNB = false;
    public static final boolean LAUNCHER3_USE_WAKE_LOCK = false;
    public static final boolean LAUNCHER3_LEGACY_WORKSPACE_DND = false;
    public static final boolean LAUNCHER3_PHYSICS = false;
    public static final boolean LAUNCHER3_ICON_OUTLINE_PULSE = false;
    public static final boolean LAUNCHER3_BADGE_ICONS = true;
    public static final boolean BADGE_ICONS = true;
    public static final boolean LAUNCHER3_DEEP_SHORTCUT_ICON_CACHE = false;
    public static final boolean LAUNCHER3_DEEP_SHORTCUTS = true;
    public static final boolean LAUNCHER3_SHORTCUTS_IN_ZERO_STATE = false;
    public static final boolean LAUNCHER3_ALLAPPS_PREDICTION_ROW = false;
    public static final boolean LAUNCHER3_ALLAPPS_DISCOVERY = false;
    public static final boolean LAUNCHER3_ALLAPPS_SEARCH_PIPELINE = false;
    public static final boolean LAUNCHER3_OVERVIEW_ACTIONS = false;
    public static final boolean LAUNCHER3_OVERVIEW_SWIPE_UP = false;
    public static final boolean LAUNCHER3_DIRECT_SCROLL = false;
    public static final boolean LAUNCHER3_NEW_FOLDER_ANIMATION = false;
    public static final boolean DISCOVERY_ENABLED = false;
    public static final boolean ALL_APPS_HANDLE = true;
    public static final boolean LEGACY_ICON_TREATMENT = false;
    public static final boolean ADAPTIVE_ICON_SHADOW = false;

    /** Loads user-configurable workspace features before the launcher UI is constructed. */
    public static boolean loadUserPreferences(Context context) {
        boolean oldNoAllAppsIcon = NO_ALL_APPS_ICON;
        boolean oldQsbOnFirstScreen = QSB_ON_FIRST_SCREEN;
        boolean oldQsbOnHotseat = QSB_ON_HOTSEAT;
        boolean oldQsbEnabled = QSB_ENABLED;
        QSB_ENABLED = Utilities.getPrefs(context)
                .getBoolean(PREF_SHOW_QUICK_SEARCH_BOX, true);
        QSB_ON_HOTSEAT = QSB_ENABLED
                && context.getResources().getConfiguration().orientation
                        != Configuration.ORIENTATION_LANDSCAPE
                && "bottom".equals(Utilities.getPrefs(context)
                        .getString(PREF_QUICK_SEARCH_BOX_POSITION, "top"));
        NO_ALL_APPS_ICON = !Utilities.getPrefs(context)
                .getBoolean(PREF_SHOW_ALL_APPS_BUTTON, false);
        QSB_ON_FIRST_SCREEN = QSB_ENABLED && !QSB_ON_HOTSEAT;
        return oldNoAllAppsIcon != NO_ALL_APPS_ICON
                || oldQsbOnFirstScreen != QSB_ON_FIRST_SCREEN
                || oldQsbOnHotseat != QSB_ON_HOTSEAT
                || oldQsbEnabled != QSB_ENABLED;
    }
}
