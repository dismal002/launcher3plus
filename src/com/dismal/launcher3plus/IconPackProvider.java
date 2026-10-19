package com.dismal.launcher3plus;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;

import org.xmlpull.v1.XmlPullParser;

import java.util.HashMap;
import java.util.Map;

/** Loads appfilter.xml mappings from installed third-party icon packs. */
public class IconPackProvider extends IconProvider {
    public static final String PREFERENCE_KEY = "pref_icon_pack";
    private static final String[] PACK_ACTIONS = {
            "com.fede.launcher.THEME_ICONPACK", "com.anddoes.launcher.THEME",
            "com.novalauncher.THEME", "com.teslacoilsw.launcher.THEME",
            "com.gau.go.launcherex.theme", "org.adw.launcher.THEMES",
            "org.adw.launcher.icons.ACTION_PICK_ICON"
    };

    private final Context context;
    private String loadedPackage;
    private Map<ComponentName, Integer> mappings = new HashMap<>();

    public IconPackProvider(Context context) {
        this.context = context.getApplicationContext();
    }

    @Override
    public synchronized void updateSystemStateString() {
        super.updateSystemStateString();
        String selected = getSelectedPack(context);
        mSystemState += ":iconpack=" + selected;
        if (!TextUtils.equals(selected, loadedPackage)) loadPack(selected);
    }

    @Override
    public synchronized Drawable getIcon(LauncherActivityInfo info, int iconDpi) {
        String selected = getSelectedPack(context);
        if (!TextUtils.equals(selected, loadedPackage)) loadPack(selected);
        Integer id = mappings.get(info.getComponentName());
        if (id != null) {
            try {
                return context.getPackageManager().getDrawable(selected, id, null);
            } catch (RuntimeException ignored) {
                // A pack may be removed while its cached mapping is being read.
            }
        }
        return super.getIcon(info, iconDpi);
    }

    static String getSelectedPack(Context context) {
        return Utilities.getPrefs(context).getString(PREFERENCE_KEY, "");
    }

    static Map<String, CharSequence> discoverPacks(Context context) {
        PackageManager pm = context.getPackageManager();
        Map<String, CharSequence> packs = new java.util.TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (String action : PACK_ACTIONS) {
            for (ResolveInfo info : pm.queryIntentActivities(new Intent(action), PackageManager.GET_META_DATA)) {
                packs.put(info.activityInfo.packageName, info.loadLabel(pm));
            }
        }
        return packs;
    }

    private synchronized void loadPack(String pack) {
        loadedPackage = pack;
        mappings = new HashMap<>();
        if (TextUtils.isEmpty(pack)) return;
        XmlResourceParser parser = null;
        try {
            PackageManager pm = context.getPackageManager();
            Resources res = pm.getResourcesForApplication(pack);
            int xmlId = res.getIdentifier("appfilter", "xml", pack);
            if (xmlId == 0) return;
            parser = pm.getXml(pack, xmlId, null);
            while (parser.next() != XmlPullParser.END_DOCUMENT) {
                if (parser.getEventType() != XmlPullParser.START_TAG || !"item".equals(parser.getName())) continue;
                String component = parser.getAttributeValue(null, "component");
                String drawable = parser.getAttributeValue(null, "drawable");
                if (component == null || drawable == null || !component.startsWith("ComponentInfo{")) continue;
                ComponentName name = ComponentName.unflattenFromString(component.substring(14, component.length() - 1));
                int id = res.getIdentifier(drawable, "drawable", pack);
                if (name != null && id != 0) mappings.put(name, id);
            }
        } catch (Exception ignored) {
            mappings.clear();
        } finally {
            if (parser != null) parser.close();
        }
    }
}
