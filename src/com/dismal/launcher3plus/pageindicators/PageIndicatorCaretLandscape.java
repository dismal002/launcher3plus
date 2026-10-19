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
package com.dismal.launcher3plus.pageindicators;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;

import com.dismal.launcher3plus.Launcher;
import com.dismal.launcher3plus.R;
import com.dismal.launcher3plus.config.FeatureFlags;

/**
 * Simply draws the caret drawable bottom-right aligned in the view. This ensures that we can have
 * a view with as large an area as we want (for touching) while maintaining a caret of size
 * all_apps_caret_size.  Used only for the landscape layout.
 */
public class PageIndicatorCaretLandscape extends PageIndicator {
    // all apps pull up handle drawable.
    private final Launcher mLauncher;

    public PageIndicatorCaretLandscape(Context context) {
        this(context, null);
    }

    public PageIndicatorCaretLandscape(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PageIndicatorCaretLandscape(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        int caretSize = context.getResources().getDimensionPixelSize(R.dimen.all_apps_caret_size);
        CaretDrawable caretDrawable = new CaretDrawable(context);
        caretDrawable.setBounds(0, 0, caretSize, caretSize);
        setCaretDrawable(caretDrawable);

        mLauncher = Launcher.getLauncher(context);
        setOnTouchListener(mLauncher.getHapticFeedbackTouchListener());
        setOnClickListener(mLauncher);
        setOnLongClickListener(mLauncher);
        setOnFocusChangeListener(mLauncher.mFocusHandler);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        setAllAppsButtonMode(!FeatureFlags.NO_ALL_APPS_ICON);
    }

    @Override
    public void setAllAppsButtonMode(boolean showHotseatButton) {
        setVisibility(showHotseatButton ? GONE : VISIBLE);
        if (!showHotseatButton) {
            mLauncher.setAllAppsButton(this);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Rect drawableBounds = getCaretDrawable().getBounds();
        int count = canvas.save();
        canvas.translate(getWidth() - drawableBounds.width(),
                getHeight() - drawableBounds.height());
        getCaretDrawable().draw(canvas);
        canvas.restoreToCount(count);
    }
}
