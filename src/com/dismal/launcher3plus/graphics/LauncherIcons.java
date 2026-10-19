/*
 * Copyright (C) 2016 The Android Open Source Project
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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.Intent.ShortcutIconResource;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PaintDrawable;
import android.os.Build;
import android.os.Process;
import android.os.UserHandle;

import com.dismal.launcher3plus.AppInfo;
import com.dismal.launcher3plus.IconCache;
import com.dismal.launcher3plus.LauncherAppState;
import com.dismal.launcher3plus.R;
import com.dismal.launcher3plus.Utilities;
import com.dismal.launcher3plus.config.FeatureFlags;
import com.dismal.launcher3plus.model.PackageItemInfo;
import com.dismal.launcher3plus.shortcuts.DeepShortcutManager;
import com.dismal.launcher3plus.shortcuts.ShortcutInfoCompat;

/**
 * Helper methods for generating various launcher icons
 */
public class LauncherIcons {

    private static final Rect sOldBounds = new Rect();
    private static final Canvas sCanvas = new Canvas();

    static {
        sCanvas.setDrawFilter(new PaintFlagsDrawFilter(Paint.DITHER_FLAG,
                Paint.FILTER_BITMAP_FLAG));
    }

    /**
     * Returns a bitmap suitable for the all apps view. If the package or the resource do not
     * exist, it returns null.
     */
    public static Bitmap createIconBitmap(ShortcutIconResource iconRes, Context context) {
        PackageManager packageManager = context.getPackageManager();
        // the resource
        try {
            Resources resources = packageManager.getResourcesForApplication(iconRes.packageName);
            if (resources != null) {
                final int id = resources.getIdentifier(iconRes.resourceName, null, null);
                return createIconBitmap(resources.getDrawableForDensity(
                        id, LauncherAppState.getIDP(context).fillResIconDpi), context);
            }
        } catch (Exception e) {
            // Icon not found.
        }
        return null;
    }

    /**
     * Returns a bitmap which is of the appropriate size to be displayed as an icon
     */
    public static Bitmap createIconBitmap(Bitmap icon, Context context) {
        final int iconBitmapSize = LauncherAppState.getIDP(context).iconBitmapSize;
        if (iconBitmapSize == icon.getWidth() && iconBitmapSize == icon.getHeight()) {
            return icon;
        }
        return createIconBitmap(new BitmapDrawable(context.getResources(), icon), context);
    }

    /**
     * Returns a bitmap suitable for the all apps view. The icon is badged for {@param user}.
     * The bitmap is also visually normalized with other icons.
     */
    public static Bitmap createBadgedIconBitmap(
            Drawable icon, UserHandle user, Context context, int iconAppTargetSdk) {

        IconNormalizer normalizer;
        float scale = 1f;
        if (!FeatureFlags.LAUNCHER3_DISABLE_ICON_NORMALIZATION) {
            normalizer = IconNormalizer.getInstance(context);
            boolean applyLegacyTreatment = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
                    || IconShapeCompat.hasCustomShape();
                if (Utilities.isAtLeastO() && (iconAppTargetSdk >= Build.VERSION_CODES.O
                    || applyLegacyTreatment)) {
                boolean[] outShape = new boolean[1];
                Drawable dr = context.getDrawable(R.drawable.adaptive_icon_drawable_wrapper).mutate();
                dr.setBounds(0, 0, 1, 1);
                Path mask = IconShapeCompat.getUnitMask(dr);
                scale = normalizer.getScale(icon, null, mask, outShape);
                if (applyLegacyTreatment && !outShape[0]
                    && !com.dismal.launcher3plus.graphics.AdaptiveIconCompat.isAdaptiveIconDrawable(icon)) {
                    Drawable wrappedIcon = com.dismal.launcher3plus.graphics.AdaptiveIconCompat
                        .wrapToAdaptiveIconDrawable(context, icon, scale);
                    if (wrappedIcon != icon) {
                    icon = wrappedIcon;
                    scale = normalizer.getScale(icon, null,
                        IconShapeCompat.hasCustomShape()
                            ? IconShapeCompat.getUnitMask(dr) : null,
                        null);
                    }
                }
            } else {
                scale = normalizer.getScale(icon, null, null, null);
            }
        }
        Bitmap bitmap = createIconBitmap(icon, context, scale);
        if (FeatureFlags.ADAPTIVE_ICON_SHADOW && Utilities.isAtLeastO() &&
            com.dismal.launcher3plus.graphics.AdaptiveIconCompat.isAdaptiveIconDrawable(icon)) {
            bitmap = ShadowGenerator.getInstance(context).recreateIcon(bitmap);
        }
        return badgeIconForUser(bitmap, user, context);
    }

    /**
     * Badges the provided icon with the user badge if required.
     */
    public static Bitmap badgeIconForUser(Bitmap icon, UserHandle user, Context context) {
        if (user != null && !Process.myUserHandle().equals(user)) {
            BitmapDrawable drawable = new FixedSizeBitmapDrawable(icon);
            Drawable badged = context.getPackageManager().getUserBadgedIcon(
                    drawable, user);
            if (badged instanceof BitmapDrawable) {
                return ((BitmapDrawable) badged).getBitmap();
            } else {
                return createIconBitmap(badged, context);
            }
        } else {
            return icon;
        }
    }

    /**
     * Creates a normalized bitmap suitable for the all apps view. The bitmap is also visually
     * normalized with other icons and has enough spacing to add shadow.
     */
    public static Bitmap createScaledBitmapWithoutShadow(Drawable icon, Context context, int iconAppTargetSdk) {
        RectF iconBounds = new RectF();
        IconNormalizer normalizer;
        float scale = 1f;
        if (!FeatureFlags.LAUNCHER3_DISABLE_ICON_NORMALIZATION) {
            normalizer = IconNormalizer.getInstance(context);
            boolean applyLegacyTreatment = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
                    || IconShapeCompat.hasCustomShape();
                if (Utilities.isAtLeastO() && (iconAppTargetSdk >= Build.VERSION_CODES.O
                    || applyLegacyTreatment)) {
                boolean[] outShape = new boolean[1];
                Drawable dr = context.getDrawable(R.drawable.adaptive_icon_drawable_wrapper).mutate();
                dr.setBounds(0, 0, 1, 1);
                Path mask = IconShapeCompat.getUnitMask(dr);
                scale = normalizer.getScale(icon, iconBounds, mask, outShape);
                if (applyLegacyTreatment && !outShape[0]
                    && !com.dismal.launcher3plus.graphics.AdaptiveIconCompat.isAdaptiveIconDrawable(icon)) {
                    Drawable wrappedIcon = com.dismal.launcher3plus.graphics.AdaptiveIconCompat
                        .wrapToAdaptiveIconDrawable(context, icon, scale);
                    if (wrappedIcon != icon) {
                    icon = wrappedIcon;
                    scale = normalizer.getScale(icon, iconBounds,
                        IconShapeCompat.hasCustomShape()
                            ? IconShapeCompat.getUnitMask(dr) : null,
                        null);
                    }
                }
            } else {
                scale = normalizer.getScale(icon, iconBounds, null, null);
            }

        }
        scale = Math.min(scale, ShadowGenerator.getScaleForBounds(iconBounds));
        return createIconBitmap(icon, context, scale);
    }

    /**
     * Adds a shadow to the provided icon. It assumes that the icon has already been scaled using
     * {@link #createScaledBitmapWithoutShadow(Drawable, Context, int)}
     */
    public static Bitmap addShadowToIcon(Bitmap icon, Context context) {
        return ShadowGenerator.getInstance(context).recreateIcon(icon);
    }

    /**
     * Adds the {@param badge} on top of {@param srcTgt} using the badge dimensions.
     */
    public static Bitmap badgeWithBitmap(Bitmap srcTgt, Bitmap badge, Context context) {
        int badgeSize = context.getResources().getDimensionPixelSize(R.dimen.profile_badge_size);
        synchronized (sCanvas) {
            sCanvas.setBitmap(srcTgt);
            sCanvas.drawBitmap(badge, new Rect(0, 0, badge.getWidth(), badge.getHeight()),
                    new Rect(srcTgt.getWidth() - badgeSize,
                            srcTgt.getHeight() - badgeSize, srcTgt.getWidth(), srcTgt.getHeight()),
                    new Paint(Paint.FILTER_BITMAP_FLAG));
            sCanvas.setBitmap(null);
        }
        return srcTgt;
    }

    /**
     * Returns a bitmap suitable for the all apps view.
     */
    public static Bitmap createIconBitmap(Drawable icon, Context context) {
        float scale = 1f;
        if (FeatureFlags.ADAPTIVE_ICON_SHADOW && Utilities.isAtLeastO() &&
            com.dismal.launcher3plus.graphics.AdaptiveIconCompat.isAdaptiveIconDrawable(icon)) {
            scale = ShadowGenerator.getScaleForBounds(new RectF(0, 0, 0, 0));
        }
        Bitmap bitmap =  createIconBitmap(icon, context, scale);
        if (FeatureFlags.ADAPTIVE_ICON_SHADOW && Utilities.isAtLeastO() &&
            com.dismal.launcher3plus.graphics.AdaptiveIconCompat.isAdaptiveIconDrawable(icon)) {
            bitmap = ShadowGenerator.getInstance(context).recreateIcon(bitmap);
        }
        return bitmap;
    }

    /**
     * @param scale the scale to apply before drawing {@param icon} on the canvas
     */
    public static Bitmap createIconBitmap(Drawable icon, Context context, float scale) {
        synchronized (sCanvas) {
            final int iconBitmapSize = LauncherAppState.getIDP(context).iconBitmapSize;
            int width = iconBitmapSize;
            int height = iconBitmapSize;

            if (icon instanceof PaintDrawable) {
                PaintDrawable painter = (PaintDrawable) icon;
                painter.setIntrinsicWidth(width);
                painter.setIntrinsicHeight(height);
            } else if (icon instanceof BitmapDrawable) {
                // Ensure the bitmap has a density.
                BitmapDrawable bitmapDrawable = (BitmapDrawable) icon;
                Bitmap bitmap = bitmapDrawable.getBitmap();
                if (bitmap != null && bitmap.getDensity() == Bitmap.DENSITY_NONE) {
                    bitmapDrawable.setTargetDensity(context.getResources().getDisplayMetrics());
                }
            }

            int sourceWidth = icon.getIntrinsicWidth();
            int sourceHeight = icon.getIntrinsicHeight();
            if (sourceWidth > 0 && sourceHeight > 0) {
                // Scale the icon proportionally to the icon dimensions
                final float ratio = (float) sourceWidth / sourceHeight;
                if (sourceWidth > sourceHeight) {
                    height = (int) (width / ratio);
                } else if (sourceHeight > sourceWidth) {
                    width = (int) (height * ratio);
                }
            }
            // no intrinsic size --> use default size
            int textureWidth = iconBitmapSize;
            int textureHeight = iconBitmapSize;

            Bitmap bitmap = Bitmap.createBitmap(textureWidth, textureHeight,
                    Bitmap.Config.ARGB_8888);
            final Canvas canvas = sCanvas;
            canvas.setBitmap(bitmap);

            final int left = (textureWidth-width) / 2;
            final int top = (textureHeight-height) / 2;

            sOldBounds.set(icon.getBounds());
            if (Utilities.isAtLeastO() && com.dismal.launcher3plus.graphics.AdaptiveIconCompat.isAdaptiveIconDrawable(icon)) {
                int offset = Math.max((int)(ShadowGenerator.BLUR_FACTOR * iconBitmapSize),
                        Math.min(left, top));
                int size = Math.max(width, height);
                icon.setBounds(offset, offset, size, size);
            } else {
                icon.setBounds(left, top, left+width, top+height);
            }
            canvas.save();
            canvas.scale(scale, scale, textureWidth / 2, textureHeight / 2);
            if (Utilities.isAtLeastO() && IconShapeCompat.hasCustomShape()) {
                IconShapeCompat.draw(canvas, icon);
            } else {
                icon.draw(canvas);
            }
            canvas.restore();
            icon.setBounds(sOldBounds);
            canvas.setBitmap(null);

            return bitmap;
        }
    }

    /**
     * Wraps legacy icons in an adaptive icon with a white background and a centered,
     * scaled foreground, matching Launcher3's Android P legacy icon treatment.
     */
    

    public static Bitmap createShortcutIcon(ShortcutInfoCompat shortcutInfo, Context context) {
        return createShortcutIcon(shortcutInfo, context, true /* badged */);
    }

    public static Bitmap createShortcutIcon(ShortcutInfoCompat shortcutInfo, Context context,
            boolean badged) {
        LauncherAppState app = LauncherAppState.getInstance(context);
        Drawable unbadgedDrawable = DeepShortcutManager.getInstance(context)
                .getShortcutIconDrawable(shortcutInfo,
                        app.getInvariantDeviceProfile().fillResIconDpi);
        IconCache cache = app.getIconCache();
        Bitmap unbadgedBitmap = unbadgedDrawable == null
                ? cache.getDefaultIcon(Process.myUserHandle())
                : LauncherIcons.createScaledBitmapWithoutShadow(unbadgedDrawable, context,
                Build.VERSION_CODES.O);

        if (!badged) {
            return unbadgedBitmap;
        }
        unbadgedBitmap = LauncherIcons.addShadowToIcon(unbadgedBitmap, context);

        final Bitmap badgeBitmap;
        ComponentName cn = shortcutInfo.getActivity();
        if (cn != null) {
            // Get the app info for the source activity.
            AppInfo appInfo = new AppInfo();
            appInfo.user = shortcutInfo.getUserHandle();
            appInfo.componentName = cn;
            appInfo.intent = new Intent(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_LAUNCHER)
                    .setComponent(cn);
            cache.getTitleAndIcon(appInfo, false);
            badgeBitmap = appInfo.iconBitmap;
        } else {
            PackageItemInfo pkgInfo = new PackageItemInfo(shortcutInfo.getPackage());
            cache.getTitleAndIconForApp(pkgInfo, false);
            badgeBitmap = pkgInfo.iconBitmap;
        }
        return badgeWithBitmap(unbadgedBitmap, badgeBitmap, context);
    }

    /**
     * An extension of {@link BitmapDrawable} which returns the bitmap pixel size as intrinsic size.
     * This allows the badging to be done based on the action bitmap size rather than
     * the scaled bitmap size.
     */
    private static class FixedSizeBitmapDrawable extends BitmapDrawable {

        public FixedSizeBitmapDrawable(Bitmap bitmap) {
            super(null, bitmap);
        }

        @Override
        public int getIntrinsicHeight() {
            return getBitmap().getWidth();
        }

        @Override
        public int getIntrinsicWidth() {
            return getBitmap().getWidth();
        }
    }
}
