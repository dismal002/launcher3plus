package com.dismal.launcher3plus.graphics;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import androidx.core.graphics.PathParser;

import com.dismal.launcher3plus.LauncherFiles;

/**
 * Applies a user-selected icon shape ourselves instead of overriding the platform's
 * config_icon_mask, which only worked on Android 8.x.
 */
@TargetApi(Build.VERSION_CODES.O)
public final class IconShapeCompat {

    private static final String TAG = "IconShapeCompat";
    private static final float MASK_SIZE = 100f;
    // Adaptive layers are 108dp, the visible area is 72dp => layers are 1.5x the bounds.
    private static final float LAYER_SCALE = 1.5f;

    private static volatile Path sPath; // 100x100 space, null => platform default

    private IconShapeCompat() { }

    public static void load(Context context) {
        String s = context.getSharedPreferences(LauncherFiles.DEVICE_PREFERENCES_KEY, 0)
                .getString(IconShapeOverride.KEY_PREFERENCE, "");
        Path p = null;
        if (!TextUtils.isEmpty(s)) {
            try {
                p = PathParser.createPathFromPathData(s);
            } catch (Exception e) {
                Log.e(TAG, "Bad icon shape path: " + s, e);
            }
        }
        sPath = p;
        Log.i(TAG, p == null ? "Using platform icon mask" : "Loaded custom icon mask");
    }

    public static boolean hasCustomShape() {
        return sPath != null;
    }

    /** Fresh path in [0,1]x[0,1] (IconNormalizer transforms it in place). */
    public static Path getUnitMask(Drawable platformDefault) {
        Path src = sPath;
        if (src == null) {
            Path platformMask = com.dismal.launcher3plus.graphics.AdaptiveIconCompat.getIconMask(platformDefault);
            return platformMask;
        }
        Path out = new Path();
        Matrix m = new Matrix();
        m.setScale(1f / MASK_SIZE, 1f / MASK_SIZE);
        src.transform(m, out);
        return out;
    }

    /** Draws {@code drawable} (whose bounds are already set) clipped to the custom shape. */
    public static void draw(Canvas canvas, Drawable drawable) {
        Path src = sPath;
        Rect b = drawable.getBounds();
        if (src == null || b.isEmpty()) {
            drawable.draw(canvas);
            return;
        }

        Path mask = new Path();
        Matrix m = new Matrix();
        m.setScale(b.width() / MASK_SIZE, b.height() / MASK_SIZE);
        m.postTranslate(b.left, b.top);
        src.transform(m, mask);

        int dx = (int) (b.width() * (LAYER_SCALE - 1f) / 2f);
        int dy = (int) (b.height() * (LAYER_SCALE - 1f) / 2f);
        Rect layer = new Rect(b.left - dx, b.top - dy, b.right + dx, b.bottom + dy);

        int sc = canvas.save();
        canvas.clipPath(mask);
        Drawable bg = com.dismal.launcher3plus.graphics.AdaptiveIconCompat.getBackground(drawable);
        Drawable fg = com.dismal.launcher3plus.graphics.AdaptiveIconCompat.getForeground(drawable);
        if (bg != null) {
            Rect oldBounds = new Rect(bg.getBounds());
            bg.setBounds(layer);
            bg.draw(canvas);
            bg.setBounds(oldBounds);
        }
        if (fg != null) {
            Rect oldBounds = new Rect(fg.getBounds());
            fg.setBounds(layer);
            fg.draw(canvas);
            fg.setBounds(oldBounds);
        }
        canvas.restoreToCount(sc);
    }

    /** Draws an ordinary drawable clipped to the selected icon mask. */
    public static void draw(Canvas canvas, Drawable drawable, Rect bounds) {
        Path src = sPath;
        if (src == null || bounds.isEmpty()) {
            drawable.draw(canvas);
            return;
        }

        Path mask = new Path();
        Matrix m = new Matrix();
        m.setScale(bounds.width() / MASK_SIZE, bounds.height() / MASK_SIZE);
        m.postTranslate(bounds.left, bounds.top);
        src.transform(m, mask);
        int sc = canvas.save();
        canvas.clipPath(mask);
        drawable.draw(canvas);
        canvas.restoreToCount(sc);
    }
}
