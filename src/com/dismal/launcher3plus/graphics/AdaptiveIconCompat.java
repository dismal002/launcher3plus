package com.dismal.launcher3plus.graphics;

import android.content.Context;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

import com.dismal.launcher3plus.Utilities;

import java.lang.reflect.Method;

/**
 * Compatibility helper to interact with AdaptiveIconDrawable reflectively so the classes that
 * reference it do not trigger NoClassDefFoundError on pre-O devices.
 */
public final class AdaptiveIconCompat {

    private AdaptiveIconCompat() { }

    public static boolean isAdaptiveIconDrawable(Drawable d) {
        if (d == null || !Utilities.isAtLeastO()) return false;
        try {
            Class<?> cls = Class.forName("android.graphics.drawable.AdaptiveIconDrawable");
            return cls.isInstance(d);
        } catch (Throwable t) {
            return false;
        }
    }

    public static Drawable wrapToAdaptiveIconDrawable(Context context, Drawable drawable, float scale) {
        if (!Utilities.isAtLeastO()) return drawable;

        try {
            Drawable wrapper = context.getDrawable(com.dismal.launcher3plus.R.drawable.adaptive_icon_drawable_wrapper).mutate();
            // If the wrapper isn't an AdaptiveIconDrawable (unexpected), return original
            Class<?> cls = Class.forName("android.graphics.drawable.AdaptiveIconDrawable");
            if (!cls.isInstance(wrapper)) return drawable;

            // Try to set the foreground drawable and scale if the wrapper implements it
            Method getForeground = wrapper.getClass().getMethod("getForeground");
            Object fg = getForeground.invoke(wrapper);
            if (fg instanceof FixedScaleDrawable) {
                FixedScaleDrawable fsd = (FixedScaleDrawable) fg;
                fsd.setDrawable(drawable);
                fsd.setScale(scale);
            }
            return wrapper;
        } catch (Throwable t) {
            return drawable;
        }
    }

    public static Path getIconMask(Drawable platformDefault) {
        if (platformDefault == null || !Utilities.isAtLeastO()) return null;
        try {
            Method m = platformDefault.getClass().getMethod("getIconMask");
            Object res = m.invoke(platformDefault);
            if (res instanceof Path) return (Path) res;
        } catch (Throwable t) {
            // ignore
        }
        return null;
    }

    public static Drawable getBackground(Drawable adaptive) {
        if (adaptive == null || !Utilities.isAtLeastO()) return null;
        try {
            Method m = adaptive.getClass().getMethod("getBackground");
            Object res = m.invoke(adaptive);
            if (res instanceof Drawable) return (Drawable) res;
        } catch (Throwable t) {}
        return null;
    }

    public static Drawable getForeground(Drawable adaptive) {
        if (adaptive == null || !Utilities.isAtLeastO()) return null;
        try {
            Method m = adaptive.getClass().getMethod("getForeground");
            Object res = m.invoke(adaptive);
            if (res instanceof Drawable) return (Drawable) res;
        } catch (Throwable t) {}
        return null;
    }
}
