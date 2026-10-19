package com.dismal.launcher3plus.allapps;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

import androidx.recyclerview.widget.RecyclerView;

import com.dismal.launcher3plus.R;
import com.dismal.launcher3plus.Utilities;
import com.dismal.launcher3plus.util.Themes;

/** Draws one stationary card behind the horizontally paged app grid. */
class AllAppsPageBackgroundDecoration extends RecyclerView.ItemDecoration {

    private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float mInset;
    private final float mRadius;

    AllAppsPageBackgroundDecoration(Context context, float inset, float radius) {
        mInset = inset;
        mRadius = radius;
        String theme = Utilities.getPrefs(context).getString(
                Utilities.LAUNCHER_THEME_PREFERENCE_KEY, "light");
        mPaint.setColor(("dark".equals(theme) || "dark_text".equals(theme))
                ? Themes.getAttrColor(context, android.R.attr.colorPrimary)
                : Color.WHITE);
    }

    @Override
    public void onDraw(Canvas canvas, RecyclerView parent, RecyclerView.State state) {
        if (!(parent instanceof AllAppsRecyclerView)
                || !((AllAppsRecyclerView) parent).isHorizontalPaging()) {
            return;
        }
        // Keep the card fixed while the app pages move inside it. Drawing a card per page
        // makes the card edges slide past one another during swipes.
        RectF card = new RectF(mInset, -mInset, parent.getWidth() - mInset,
                parent.getHeight() - mInset);
        canvas.drawRoundRect(card, mRadius, mRadius, mPaint);
    }
}
