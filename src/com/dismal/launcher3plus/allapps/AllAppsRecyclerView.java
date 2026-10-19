/*
 * Copyright (C) 2015 The Android Open Source Project
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
package com.dismal.launcher3plus.allapps;

// Import statements and package declaration
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import androidx.recyclerview.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.SparseIntArray;
import android.view.MotionEvent;
import android.view.View;

import com.dismal.launcher3plus.BaseRecyclerView;
import com.dismal.launcher3plus.BubbleTextView;
import com.dismal.launcher3plus.DeviceProfile;
import com.dismal.launcher3plus.Launcher;
import com.dismal.launcher3plus.R;
import com.dismal.launcher3plus.config.FeatureFlags;
import com.dismal.launcher3plus.graphics.DrawableFactory;
import com.dismal.launcher3plus.userevent.LauncherLogProto.ContainerType;

import java.util.List;

/**
 * A RecyclerView with custom fast scroll support for the all apps view.
 */
public class AllAppsRecyclerView extends BaseRecyclerView {

    private AlphabeticalAppsList mApps;
    private AllAppsFastScrollHelper mFastScrollHelper;
    private int mNumAppsPerRow;

    // The specific view heights that we use to calculate scroll
    private SparseIntArray mViewHeights = new SparseIntArray();
    private SparseIntArray mCachedScrollPositions = new SparseIntArray();

    // The empty-search result background
    private AllAppsBackgroundDrawable mEmptySearchBackground;
    private int mEmptySearchBackgroundTopOffset;

    private HeaderElevationController mElevationController;
    private boolean mHorizontalPaging;
    private int mPageRows;
    private static final int PAGE_COLUMNS = 4;
    private boolean mSettlingPage;

    public AllAppsRecyclerView(Context context) {
        this(context, null);
    }

    public AllAppsRecyclerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AllAppsRecyclerView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public AllAppsRecyclerView(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr);
        Resources res = getResources();
        addOnItemTouchListener(this);
        mScrollbar.setDetachThumbOnFastScroll();
        mEmptySearchBackgroundTopOffset = res.getDimensionPixelSize(
                R.dimen.all_apps_empty_search_bg_top_offset);
        addOnScrollListener(new OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                if (mHorizontalPaging && newState == SCROLL_STATE_IDLE && !mSettlingPage) {
                    snapToNearestPage();
                }
            }
        });
    }

    /**
     * Sets the list of apps in this view, used to determine the fastscroll position.
     */
    public void setApps(AlphabeticalAppsList apps) {
        mApps = apps;
        mFastScrollHelper = new AllAppsFastScrollHelper(this, apps);
    }

    public void setElevationController(HeaderElevationController elevationController) {
        mElevationController = elevationController;
    }

    public void setHorizontalPaging(boolean horizontalPaging) {
        mHorizontalPaging = horizontalPaging;
        if (!horizontalPaging) {
            mSettlingPage = false;
        }
        if (horizontalPaging) {
            mScrollbar.setThumbOffsetY(-1);
        }
    }

    public boolean isHorizontalPaging() {
        return mHorizontalPaging;
    }

    @Override
    public boolean fling(int velocityX, int velocityY) {
        if (!mHorizontalPaging || velocityX == 0) {
            return super.fling(velocityX, velocityY);
        }
        int pageWidth = getPageWidth();
        if (pageWidth <= 0) {
            return true;
        }
        int offset = getHorizontalPageOffset();
        int targetPage = velocityX > 0
                ? (int) Math.floor((double) offset / pageWidth) + 1
                : (int) Math.ceil((double) offset / pageWidth) - 1;
        targetPage = Math.max(0, Math.min(getHorizontalPageCount() - 1, targetPage));
        settleToPage(targetPage, offset);
        return true;
    }

    public int getHorizontalPageOffset() {
        if (!mHorizontalPaging || mPageRows <= 0 || getChildCount() == 0) {
            return 0;
        }
        View firstIcon = null;
        int firstPosition = NO_POSITION;
        int firstLeft = Integer.MAX_VALUE;
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            int position = getChildAdapterPosition(child);
            if (position == NO_POSITION || !(child instanceof BubbleTextView)) {
                continue;
            }
            int left = getLayoutManager().getDecoratedLeft(child);
            if (left < firstLeft) {
                firstLeft = left;
                firstIcon = child;
                firstPosition = position;
            }
        }
        if (firstIcon == null || firstPosition >= mApps.getAdapterItems().size()) {
            return 0;
        }
        int dataPos = firstPosition;
        RecyclerView.Adapter adapter = getAdapter();
        if (adapter instanceof com.dismal.launcher3plus.allapps.AllAppsGridAdapter) {
            dataPos = ((com.dismal.launcher3plus.allapps.AllAppsGridAdapter) adapter)
                    .mapAdapterPositionToDataPosition(firstPosition);
        }
        AlphabeticalAppsList.AdapterItem item = mApps.getAdapterItems().get(dataPos);
        int cellWidth = getWidth() / PAGE_COLUMNS;
        return Math.max(0, item.rowIndex * cellWidth - firstLeft);
    }

    public int getHorizontalPageCount() {
        if (!mHorizontalPaging || mPageRows <= 0 || mApps == null) {
            return 1;
        }
        int appsPerPage = mPageRows * PAGE_COLUMNS;
        return Math.max(1, (mApps.getNumFilteredApps() + appsPerPage - 1) / appsPerPage);
    }

    public int getHorizontalPageWidth() {
        return getPageWidth();
    }

    public void snapToNearestPage() {
        if (!mHorizontalPaging) {
            return;
        }
        int offset = getHorizontalPageOffset();
        int pageWidth = getPageWidth();
        if (pageWidth <= 0) {
            return;
        }
        int page = Math.round((float) offset / pageWidth);
        page = Math.max(0, Math.min(getHorizontalPageCount() - 1, page));
        settleToPage(page, offset);
    }

    private int getPageWidth() {
        return (getWidth() / PAGE_COLUMNS) * PAGE_COLUMNS;
    }

    private void settleToPage(int page, int currentOffset) {
        int distance = page * getPageWidth() - currentOffset;
        if (Math.abs(distance) < 2) {
            return;
        }
        mSettlingPage = true;
        smoothScrollBy(distance, 0);
        postDelayed(new Runnable() {
            @Override
            public void run() {
                mSettlingPage = false;
                if (getScrollState() == SCROLL_STATE_IDLE) {
                    snapToNearestPage();
                }
            }
        }, 350);
    }

    /**
     * Sets the number of apps per row in this recycler view.
     */
    public void setNumAppsPerRow(DeviceProfile grid, int numAppsPerRow) {
        mNumAppsPerRow = numAppsPerRow;
        if (mHorizontalPaging) {
            mPageRows = numAppsPerRow;
        }

        RecyclerView.RecycledViewPool pool = getRecycledViewPool();
        int approxRows = (int) Math.ceil(grid.availableHeightPx / grid.allAppsIconSizePx);
        pool.setMaxRecycledViews(AllAppsGridAdapter.VIEW_TYPE_EMPTY_SEARCH, 1);
        pool.setMaxRecycledViews(AllAppsGridAdapter.VIEW_TYPE_SEARCH_DIVIDER, 1);
        pool.setMaxRecycledViews(AllAppsGridAdapter.VIEW_TYPE_SEARCH_MARKET_DIVIDER, 1);
        pool.setMaxRecycledViews(AllAppsGridAdapter.VIEW_TYPE_SEARCH_MARKET, 1);
        pool.setMaxRecycledViews(AllAppsGridAdapter.VIEW_TYPE_ICON, approxRows * mNumAppsPerRow);
        pool.setMaxRecycledViews(AllAppsGridAdapter.VIEW_TYPE_PREDICTION_ICON, mNumAppsPerRow);
        pool.setMaxRecycledViews(AllAppsGridAdapter.VIEW_TYPE_PREDICTION_DIVIDER, 1);
    }

    /**
     * Ensures that we can present a stable scrollbar for views of varying types by pre-measuring
     * all the different view types.
     */
    public void preMeasureViews(AllAppsGridAdapter adapter) {
        View icon = adapter.onCreateViewHolder(this, AllAppsGridAdapter.VIEW_TYPE_ICON).itemView;
        final int iconHeight = icon.getLayoutParams().height;
        mViewHeights.put(AllAppsGridAdapter.VIEW_TYPE_ICON, iconHeight);
        mViewHeights.put(AllAppsGridAdapter.VIEW_TYPE_PREDICTION_ICON, iconHeight);

        final int widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(
                getResources().getDisplayMetrics().widthPixels, View.MeasureSpec.AT_MOST);
        final int heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(
                getResources().getDisplayMetrics().heightPixels, View.MeasureSpec.AT_MOST);

        putSameHeightFor(adapter, widthMeasureSpec, heightMeasureSpec,
                AllAppsGridAdapter.VIEW_TYPE_PREDICTION_DIVIDER,
                AllAppsGridAdapter.VIEW_TYPE_SEARCH_MARKET_DIVIDER);
        putSameHeightFor(adapter, widthMeasureSpec, heightMeasureSpec,
                AllAppsGridAdapter.VIEW_TYPE_SEARCH_DIVIDER);
        putSameHeightFor(adapter, widthMeasureSpec, heightMeasureSpec,
                AllAppsGridAdapter.VIEW_TYPE_SEARCH_MARKET);
        putSameHeightFor(adapter, widthMeasureSpec, heightMeasureSpec,
                AllAppsGridAdapter.VIEW_TYPE_EMPTY_SEARCH);

        if (FeatureFlags.DISCOVERY_ENABLED) {
            putSameHeightFor(adapter, widthMeasureSpec, heightMeasureSpec,
                    AllAppsGridAdapter.VIEW_TYPE_APPS_LOADING_DIVIDER);
            putSameHeightFor(adapter, widthMeasureSpec, heightMeasureSpec,
                    AllAppsGridAdapter.VIEW_TYPE_DISCOVERY_ITEM);
        }
    }

    private void putSameHeightFor(AllAppsGridAdapter adapter, int w, int h, int... viewTypes) {
        View view = adapter.onCreateViewHolder(this, viewTypes[0]).itemView;
        view.measure(w, h);
        for (int viewType : viewTypes) {
            mViewHeights.put(viewType, view.getMeasuredHeight());
        }
    }

    /**
     * Scrolls this recycler view to the top.
     */
    public void scrollToTop() {
        // Ensure we reattach the scrollbar if it was previously detached while fast-scrolling
        if (mScrollbar.isThumbDetached()) {
            mScrollbar.reattachThumbToScroll();
        }
        scrollToPosition(0);
        if (mElevationController != null) {
            mElevationController.reset();
        }
    }

    @Override
    public void onDraw(Canvas c) {
        // Draw the background
        if (mEmptySearchBackground != null && mEmptySearchBackground.getAlpha() > 0) {
            mEmptySearchBackground.draw(c);
        }

        super.onDraw(c);
    }

    @Override
    protected boolean verifyDrawable(Drawable who) {
        return who == mEmptySearchBackground || super.verifyDrawable(who);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        updateEmptySearchBackgroundBounds();
    }

    public int getContainerType(View v) {
        if (mApps.hasFilter()) {
            return ContainerType.SEARCHRESULT;
        } else {
            if (v instanceof BubbleTextView) {
                BubbleTextView icon = (BubbleTextView) v;
                int position = getChildPosition(icon);
                if (position != NO_POSITION) {
                    List<AlphabeticalAppsList.AdapterItem> items = mApps.getAdapterItems();
                    AlphabeticalAppsList.AdapterItem item = items.get(position);
                    if (item.viewType == AllAppsGridAdapter.VIEW_TYPE_PREDICTION_ICON) {
                        return ContainerType.PREDICTION;
                    }
                }
            }
            return ContainerType.ALLAPPS;
        }
    }

    public void onSearchResultsChanged() {
        // Always scroll the view to the top so the user can see the changed results
        scrollToTop();

        if (mApps.shouldShowEmptySearch()) {
            if (mEmptySearchBackground == null) {
                mEmptySearchBackground = DrawableFactory.get(getContext())
                        .getAllAppsBackground(getContext());
                mEmptySearchBackground.setAlpha(0);
                mEmptySearchBackground.setCallback(this);
                updateEmptySearchBackgroundBounds();
            }
            mEmptySearchBackground.animateBgAlpha(1f, 150);
        } else if (mEmptySearchBackground != null) {
            // For the time being, we just immediately hide the background to ensure that it does
            // not overlap with the results
            mEmptySearchBackground.setBgAlpha(0f);
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent e) {
        boolean result = super.onInterceptTouchEvent(e);
        if (!result && e.getAction() == MotionEvent.ACTION_DOWN
                && mEmptySearchBackground != null && mEmptySearchBackground.getAlpha() > 0) {
            mEmptySearchBackground.setHotspot(e.getX(), e.getY());
        }
        return result;
    }

    /**
     * Maps the touch (from 0..1) to the adapter position that should be visible.
     */
    @Override
    public String scrollToPositionAtProgress(float touchFraction) {
        int rowCount = mApps.getNumAppRows();
        if (rowCount == 0) {
            return "";
        }

        // Stop the scroller if it is scrolling
        stopScroll();

        // Find the fastscroll section that maps to this touch fraction
        List<AlphabeticalAppsList.FastScrollSectionInfo> fastScrollSections =
                mApps.getFastScrollerSections();
        AlphabeticalAppsList.FastScrollSectionInfo lastInfo = fastScrollSections.get(0);
        for (int i = 1; i < fastScrollSections.size(); i++) {
            AlphabeticalAppsList.FastScrollSectionInfo info = fastScrollSections.get(i);
            if (info.touchFraction > touchFraction) {
                break;
            }
            lastInfo = info;
        }

        // Update the fast scroll
        int scrollY = getCurrentScrollY();
        int availableScrollHeight = getAvailableScrollHeight();
        mFastScrollHelper.smoothScrollToSection(scrollY, availableScrollHeight, lastInfo);
        return lastInfo.sectionName;
    }

    @Override
    public void onFastScrollCompleted() {
        super.onFastScrollCompleted();
        mFastScrollHelper.onFastScrollCompleted();
    }

    @Override
    public void setAdapter(Adapter adapter) {
        super.setAdapter(adapter);
        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            public void onChanged() {
                mCachedScrollPositions.clear();
            }
        });
        mFastScrollHelper.onSetAdapter((AllAppsGridAdapter) adapter);
    }

    /**
     * Updates the bounds for the scrollbar.
     */
    @Override
    public void onUpdateScrollbar(int dy) {
        if (mHorizontalPaging) {
            mScrollbar.setThumbOffsetY(-1);
            return;
        }
        List<AlphabeticalAppsList.AdapterItem> items = mApps.getAdapterItems();

        // Skip early if there are no items or we haven't been measured
        if (items.isEmpty() || mNumAppsPerRow == 0) {
            mScrollbar.setThumbOffsetY(-1);
            return;
        }

        // Skip early if, there no child laid out in the container.
        int scrollY = getCurrentScrollY();
        if (scrollY < 0) {
            mScrollbar.setThumbOffsetY(-1);
            return;
        }

        // Only show the scrollbar if there is height to be scrolled
        int availableScrollBarHeight = getAvailableScrollBarHeight();
        int availableScrollHeight = getAvailableScrollHeight();
        if (availableScrollHeight <= 0) {
            mScrollbar.setThumbOffsetY(-1);
            return;
        }

        if (mScrollbar.isThumbDetached()) {
            if (!mScrollbar.isDraggingThumb()) {
                // Calculate the current scroll position, the scrollY of the recycler view accounts
                // for the view padding, while the scrollBarY is drawn right up to the background
                // padding (ignoring padding)
                int scrollBarY = (int)
                        (((float) scrollY / availableScrollHeight) * availableScrollBarHeight);

                int thumbScrollY = mScrollbar.getThumbOffsetY();
                int diffScrollY = scrollBarY - thumbScrollY;
                if (diffScrollY * dy > 0f) {
                    // User is scrolling in the same direction the thumb needs to catch up to the
                    // current scroll position.  We do this by mapping the difference in movement
                    // from the original scroll bar position to the difference in movement necessary
                    // in the detached thumb position to ensure that both speed towards the same
                    // position at either end of the list.
                    if (dy < 0) {
                        int offset = (int) ((dy * thumbScrollY) / (float) scrollBarY);
                        thumbScrollY += Math.max(offset, diffScrollY);
                    } else {
                        int offset = (int) ((dy * (availableScrollBarHeight - thumbScrollY)) /
                                (float) (availableScrollBarHeight - scrollBarY));
                        thumbScrollY += Math.min(offset, diffScrollY);
                    }
                    thumbScrollY = Math.max(0, Math.min(availableScrollBarHeight, thumbScrollY));
                    mScrollbar.setThumbOffsetY(thumbScrollY);
                    if (scrollBarY == thumbScrollY) {
                        mScrollbar.reattachThumbToScroll();
                    }
                } else {
                    // User is scrolling in an opposite direction to the direction that the thumb
                    // needs to catch up to the scroll position.  Do nothing except for updating
                    // the scroll bar x to match the thumb width.
                    mScrollbar.setThumbOffsetY(thumbScrollY);
                }
            }
        } else {
            synchronizeScrollBarThumbOffsetToViewScroll(scrollY, availableScrollHeight);
        }
    }

    @Override
    protected boolean supportsFastScrolling() {
        // Only allow fast scrolling when the user is not searching, since the results are not
        // grouped in a meaningful order
        return !mApps.hasFilter();
    }

    @Override
    public int getCurrentScrollY() {
        // Return early if there are no items or we haven't been measured
        List<AlphabeticalAppsList.AdapterItem> items = mApps.getAdapterItems();
        if (items.isEmpty() || mNumAppsPerRow == 0 || getChildCount() == 0) {
            return -1;
        }

        // Calculate the y and offset for the item
        View child = getChildAt(0);
        int position = getChildPosition(child);
        if (position == NO_POSITION) {
            return -1;
        }
        return getCurrentScrollY(position, getLayoutManager().getDecoratedTop(child));
    }

    public int getCurrentScrollY(int position, int offset) {
        List<AlphabeticalAppsList.AdapterItem> items = mApps.getAdapterItems();
        RecyclerView.Adapter adapter = getAdapter();
        boolean useAdapterMapping = mHorizontalPaging && adapter instanceof com.dismal.launcher3plus.allapps.AllAppsGridAdapter
                && position < adapter.getItemCount();

        AlphabeticalAppsList.AdapterItem posItem = null;
        int dataPosForPos = position;
        if (useAdapterMapping) {
            dataPosForPos = ((com.dismal.launcher3plus.allapps.AllAppsGridAdapter) adapter)
                    .mapAdapterPositionToDataPosition(position);
            posItem = dataPosForPos < items.size() ? items.get(dataPosForPos) : null;
        } else {
            posItem = position < items.size() ? items.get(position) : null;
        }

        int y = mCachedScrollPositions.get(position, -1);
        if (y < 0) {
            y = 0;
            if (useAdapterMapping) {
                for (int i = 0; i < position; i++) {
                    int dataPos = ((com.dismal.launcher3plus.allapps.AllAppsGridAdapter) adapter)
                            .mapAdapterPositionToDataPosition(i);
                    AlphabeticalAppsList.AdapterItem item = items.get(dataPos);
                    if (AllAppsGridAdapter.isIconViewType(item.viewType)) {
                        if (posItem != null && posItem.viewType == item.viewType &&
                                posItem.rowIndex == item.rowIndex) {
                            break;
                        }
                        if (item.rowAppIndex == 0) {
                            y += mViewHeights.get(item.viewType, 0);
                        }
                    } else {
                        y += mViewHeights.get(item.viewType, 0);
                    }
                }
            } else {
                for (int i = 0; i < position; i++) {
                    AlphabeticalAppsList.AdapterItem item = items.get(i);
                    if (AllAppsGridAdapter.isIconViewType(item.viewType)) {
                        if (posItem != null && posItem.viewType == item.viewType &&
                                posItem.rowIndex == item.rowIndex) {
                            break;
                        }
                        if (item.rowAppIndex == 0) {
                            y += mViewHeights.get(item.viewType, 0);
                        }
                    } else {
                        y += mViewHeights.get(item.viewType, 0);
                    }
                }
            }
            mCachedScrollPositions.put(position, y);
        }

        return getPaddingTop() + y - offset;
    }

    @Override
    protected int getScrollbarTrackHeight() {
        return super.getScrollbarTrackHeight()
                - Launcher.getLauncher(getContext()).getDragLayer().getInsets().bottom;
    }

    /**
     * Returns the available scroll height:
     *   AvailableScrollHeight = Total height of the all items - last page height
     */
    @Override
    protected int getAvailableScrollHeight() {
        int paddedHeight = getCurrentScrollY(mApps.getAdapterItems().size(), 0);
        int totalHeight = paddedHeight + getPaddingBottom();
        return totalHeight - getScrollbarTrackHeight();
    }

    /**
     * Updates the bounds of the empty search background.
     */
    private void updateEmptySearchBackgroundBounds() {
        if (mEmptySearchBackground == null) {
            return;
        }

        // Center the empty search background on this new view bounds
        int x = (getMeasuredWidth() - mEmptySearchBackground.getIntrinsicWidth()) / 2;
        int y = mEmptySearchBackgroundTopOffset;
        mEmptySearchBackground.setBounds(x, y,
                x + mEmptySearchBackground.getIntrinsicWidth(),
                y + mEmptySearchBackground.getIntrinsicHeight());
    }

}
