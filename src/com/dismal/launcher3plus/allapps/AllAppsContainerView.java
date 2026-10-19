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

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import android.text.Selection;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.method.TextKeyListener;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import com.dismal.launcher3plus.AppInfo;
import com.dismal.launcher3plus.BaseContainerView;
import com.dismal.launcher3plus.BubbleTextView;
import com.dismal.launcher3plus.DeleteDropTarget;
import com.dismal.launcher3plus.DeviceProfile;
import com.dismal.launcher3plus.DragSource;
import com.dismal.launcher3plus.DropTarget;
import com.dismal.launcher3plus.ExtendedEditText;
import com.dismal.launcher3plus.Insettable;
import com.dismal.launcher3plus.ItemInfo;
import com.dismal.launcher3plus.Launcher;
import com.dismal.launcher3plus.R;
import com.dismal.launcher3plus.config.FeatureFlags;
import com.dismal.launcher3plus.Utilities;
import com.dismal.launcher3plus.discovery.AppDiscoveryItem;
import com.dismal.launcher3plus.discovery.AppDiscoveryUpdateState;
import com.dismal.launcher3plus.dragndrop.DragController;
import com.dismal.launcher3plus.dragndrop.DragOptions;
import com.dismal.launcher3plus.folder.Folder;
import com.dismal.launcher3plus.graphics.TintedDrawableSpan;
import com.dismal.launcher3plus.keyboard.FocusedItemDecorator;
import com.dismal.launcher3plus.userevent.LauncherLogProto.ContainerType;
import com.dismal.launcher3plus.userevent.LauncherLogProto.Target;
import com.dismal.launcher3plus.util.ComponentKey;
import com.dismal.launcher3plus.util.PackageUserKey;
import com.dismal.launcher3plus.util.Themes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * The all apps view container.
 */
public class AllAppsContainerView extends BaseContainerView implements DragSource,
        View.OnLongClickListener, AllAppsSearchBarController.Callbacks, Insettable {

    private final Launcher mLauncher;
    private final AlphabeticalAppsList mApps;
    private final AllAppsGridAdapter mAdapter;
    private final RecyclerView.LayoutManager mLayoutManager;

    private AllAppsRecyclerView mAppsRecyclerView;
    private AllAppsSearchBarController mSearchBarController;

    private ViewGroup mSearchContainer;
    private int mSearchContainerMinHeight;
    private ExtendedEditText mSearchInput;
    private ColorStateList mSearchTextColors;
    private ColorStateList mSearchHintColors;
    private Typeface mSearchTypeface;
    private float mSearchTextSize;
    private int mSearchPaddingStart;
    private int mSearchPaddingEnd;
    private HeaderElevationController mElevationController;

    private SpannableStringBuilder mSearchQueryBuilder = null;

    private int mNumAppsPerRow;
    private int mNumPredictedAppsPerRow;
    private boolean mHorizontalPaging;
    private View mPageIndicator;
    private View mHorizontalSearchDivider;
    private CharSequence mDefaultSearchHint;
    private List<ComponentKey> mPredictedApps = Collections.emptyList();

    private static final int SEARCH_DIVIDER_LIGHT = 0xFFE0E0E0;
    private static final int SEARCH_DIVIDER_DARK = 0xFF424242;

    private boolean isDarkTheme() {
        String theme = Utilities.getPrefs(getContext()).getString(
                Utilities.LAUNCHER_THEME_PREFERENCE_KEY, "light");
        return "dark".equals(theme) || "dark_text".equals(theme);
    }

    public AllAppsContainerView(Context context) {
        this(context, null);
    }

    public AllAppsContainerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AllAppsContainerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mLauncher = Launcher.getLauncher(context);
        mApps = new AlphabeticalAppsList(context);
        mAdapter = new AllAppsGridAdapter(mLauncher, mApps, mLauncher, this);
        mApps.setAdapter(mAdapter);
        mLayoutManager = mAdapter.getLayoutManager();
        mSearchQueryBuilder = new SpannableStringBuilder();
        mSearchContainerMinHeight
                = getResources().getDimensionPixelSize(R.dimen.all_apps_search_bar_height);

        Selection.setSelection(mSearchQueryBuilder, 0);
    }

    @Override
    protected void updateBackground(
            int paddingLeft, int paddingTop, int paddingRight, int paddingBottom) {
        if (mHorizontalPaging) {
            setBackground(null);
            // BaseContainerView reads this drawable's padding during layout.
            getRevealView().setBackgroundColor(Color.TRANSPARENT);
            getContentView().setBackground(null);
            return;
        }
        if (FeatureFlags.LAUNCHER3_ALL_APPS_PULL_UP) {
            if (mLauncher.getDeviceProfile().isVerticalBarLayout()) {
                getRevealView().setBackground(new InsetDrawable(mBaseDrawable,
                        paddingLeft, paddingTop, paddingRight, paddingBottom));
                getContentView().setBackground(
                        new InsetDrawable(new ColorDrawable(Color.TRANSPARENT),
                                paddingLeft, paddingTop, paddingRight, paddingBottom));
            } else {
                getRevealView().setBackground(mBaseDrawable);
            }
        } else {
            super.updateBackground(paddingLeft, paddingTop, paddingRight, paddingBottom);
        }
    }

    /**
     * Sets the current set of predicted apps.
     */
    public void setPredictedApps(List<ComponentKey> apps) {
        mPredictedApps = apps == null ? Collections.<ComponentKey>emptyList() : apps;
        mApps.setPredictedApps(mHorizontalPaging
                ? Collections.<ComponentKey>emptyList() : mPredictedApps);
    }

    /**
     * Sets the current set of apps.
     */
    public void setApps(List<AppInfo> apps) {
        mApps.setApps(apps);
    }

    /**
     * Adds new apps to the list.
     */
    public void addApps(List<AppInfo> apps) {
        mApps.addApps(apps);
        mSearchBarController.refreshSearchResult();
    }

    /**
     * Updates existing apps in the list
     */
    public void updateApps(List<AppInfo> apps) {
        mApps.updateApps(apps);
        mSearchBarController.refreshSearchResult();
    }

    /**
     * Removes some apps from the list.
     */
    public void removeApps(List<AppInfo> apps) {
        mApps.removeApps(apps);
        mSearchBarController.refreshSearchResult();
    }

    public void setSearchBarVisible(boolean visible) {
        if (visible) {
            mSearchBarController.setVisibility(View.VISIBLE);
        } else {
            mSearchBarController.setVisibility(View.INVISIBLE);
        }
    }

    /**
     * Sets the search bar that shows above the a-z list.
     */
    public void setSearchBarController(AllAppsSearchBarController searchController) {
        if (mSearchBarController != null) {
            throw new RuntimeException("Expected search bar controller to only be set once");
        }
        mSearchBarController = searchController;
        mSearchBarController.initialize(mApps, mSearchInput, mLauncher, this);
        mAdapter.setSearchController(mSearchBarController);
    }

    /**
     * Scrolls this list view to the top.
     */
    public void scrollToTop() {
        mAppsRecyclerView.scrollToTop();
    }

    /**
     * Returns whether the view itself will handle the touch event or not.
     */
    public boolean shouldContainerScroll(MotionEvent ev) {
        int[] point = new int[2];
        point[0] = (int) ev.getX();
        point[1] = (int) ev.getY();
        Utilities.mapCoordInSelfToDescendant(mAppsRecyclerView, this, point);

        // IF the MotionEvent is inside the search box, and the container keeps on receiving
        // touch input, container should move down.
        if (mLauncher.getDragLayer().isEventOverView(mSearchContainer, ev)) {
            return true;
        }

        // IF the MotionEvent is inside the thumb, container should not be pulled down.
        if (mAppsRecyclerView.getScrollBar().isNearThumb(point[0], point[1])) {
            return false;
        }

        if (mHorizontalPaging) {
            return mAppsRecyclerView.computeHorizontalScrollOffset() == 0;
        }

        // IF scroller is at the very top OR there is no scroll bar because there is probably not
        // enough items to scroll, THEN it's okay for the container to be pulled down.
        if (mAppsRecyclerView.getCurrentScrollY() == 0) {
            return true;
        }
        return false;
    }

    /**
     * Focuses the search field and begins an app search.
     */
    public void startAppsSearch() {
        if (mSearchBarController != null) {
            mSearchBarController.focusSearchField();
        }
    }

    /**
     * Resets the state of AllApps.
     */
    public void reset() {
        // Reset the search bar and base recycler view after transitioning home
        scrollToTop();
        mSearchBarController.reset();
        mAppsRecyclerView.reset();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        // This is a focus listener that proxies focus from a view into the list view.  This is to
        // work around the search box from getting first focus and showing the cursor.
        getContentView().setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    mAppsRecyclerView.requestFocus();
                }
            }
        });

        mSearchContainer = (ViewGroup) findViewById(R.id.search_container_all_apps);
        mSearchInput = (ExtendedEditText) findViewById(R.id.search_box_input);
        mSearchTextColors = mSearchInput.getTextColors();
        mSearchHintColors = mSearchInput.getHintTextColors();
        mSearchTypeface = mSearchInput.getTypeface();
        mSearchTextSize = mSearchInput.getTextSize();
        mSearchPaddingStart = mSearchInput.getPaddingStart();
        mSearchPaddingEnd = mSearchInput.getPaddingEnd();

        // Update the hint to contain the icon.
        // Prefix the original hint with two spaces. The first space gets replaced by the icon
        // using span. The second space is used for a singe space character between the hint
        // and the icon.
        SpannableString spanned = new SpannableString("  " + mSearchInput.getHint());
        spanned.setSpan(new TintedDrawableSpan(getContext(), R.drawable.ic_allapps_search),
                0, 1, Spannable.SPAN_EXCLUSIVE_INCLUSIVE);
        mSearchInput.setHint(spanned);
        mDefaultSearchHint = spanned;
        mHorizontalSearchDivider = new View(getContext());
        mHorizontalSearchDivider.setBackgroundColor(isDarkTheme() ? SEARCH_DIVIDER_DARK
                : SEARCH_DIVIDER_LIGHT);
        FrameLayout.LayoutParams dividerLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                getResources().getDimensionPixelSize(R.dimen.all_apps_search_divider_height),
                Gravity.BOTTOM);
        mSearchContainer.addView(mHorizontalSearchDivider, dividerLp);
        mHorizontalSearchDivider.setVisibility(View.GONE);

        mElevationController = new HeaderElevationController(mSearchContainer);

        // Load the all apps recycler view
        mAppsRecyclerView = (AllAppsRecyclerView) findViewById(R.id.apps_list_view);
        mAppsRecyclerView.setApps(mApps);
        mAppsRecyclerView.setLayoutManager(mLayoutManager);
        mAppsRecyclerView.setAdapter(mAdapter);
        mAppsRecyclerView.setHasFixedSize(true);
        mAppsRecyclerView.addOnScrollListener(mElevationController);
        mAppsRecyclerView.setElevationController(mElevationController);
        mPageIndicator = findViewById(R.id.all_apps_page_indicator);
        mAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                updateHorizontalPageIndicator();
            }

            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                updateHorizontalPageIndicator();
            }

            @Override
            public void onItemRangeRemoved(int positionStart, int itemCount) {
                updateHorizontalPageIndicator();
            }
        });
        mAppsRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                updateHorizontalPageIndicator();
            }
        });

        FocusedItemDecorator focusedItemDecorator = new FocusedItemDecorator(mAppsRecyclerView);
        mAppsRecyclerView.addItemDecoration(focusedItemDecorator);
        mAppsRecyclerView.addItemDecoration(new AllAppsPageBackgroundDecoration(getContext(),
                getResources().getDimension(R.dimen.all_apps_page_card_inset),
                getResources().getDimension(R.dimen.all_apps_page_card_corner_radius)));
        mAppsRecyclerView.preMeasureViews(mAdapter);
        mAdapter.setIconFocusListener(focusedItemDecorator.getFocusListener());

        if (FeatureFlags.LAUNCHER3_ALL_APPS_PULL_UP) {
            getRevealView().setVisibility(View.VISIBLE);
            getContentView().setVisibility(View.VISIBLE);
            getContentView().setBackground(null);
        }
    }

    private void updateHorizontalPageIndicator() {
        if (mPageIndicator == null) {
            return;
        }
        int pageCount = mAppsRecyclerView.getHorizontalPageCount();
        mPageIndicator.setVisibility(mHorizontalPaging
                ? View.VISIBLE : View.GONE);
        if (mPageIndicator instanceof com.dismal.launcher3plus.pageindicators.PageIndicatorDots) {
            com.dismal.launcher3plus.pageindicators.PageIndicatorDots dots =
                    (com.dismal.launcher3plus.pageindicators.PageIndicatorDots) mPageIndicator;
            dots.setMarkersCount(pageCount);
            int pageWidth = mAppsRecyclerView.getHorizontalPageWidth();
            dots.setScroll(mAppsRecyclerView.getHorizontalPageOffset(),
                    Math.max(0, (pageCount - 1) * pageWidth));
        }
    }

    @Override
    public View getTouchDelegateTargetView() {
        return mAppsRecyclerView;
    }

    @Override
    public void onBoundsChanged(Rect newBounds) { }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        DeviceProfile grid = mLauncher.getDeviceProfile();
        grid.updateAppsViewNumCols();
        boolean horizontalPaging = "horizontal".equals(Utilities.getPrefs(getContext())
                .getString(FeatureFlags.PREF_ALL_APPS_SCROLL_MODE, "vertical"));
        if (getVisibility() == View.VISIBLE) {
            mLauncher.getDragLayer().setWorkspaceBackgroundVisible(!horizontalPaging);
        }
        int appsPerRow = horizontalPaging ? Math.max(1, grid.inv.numRows)
                : (FeatureFlags.LAUNCHER3_ALL_APPS_PULL_UP
                        ? grid.inv.numColumns : grid.allAppsNumCols);
        if (horizontalPaging != mHorizontalPaging) {
            mHorizontalPaging = horizontalPaging;
            mApps.setPredictedApps(horizontalPaging
                    ? Collections.<ComponentKey>emptyList() : mPredictedApps);
            mAppsRecyclerView.setHorizontalPaging(horizontalPaging);
            mAppsRecyclerView.scrollToPosition(0);
            mAdapter.setHorizontalPaging(horizontalPaging, appsPerRow);
            mNumAppsPerRow = 0;
            mNumPredictedAppsPerRow = 0;
            mSearchContainer.setVisibility(View.VISIBLE);
            mSearchInput.setGravity(horizontalPaging
                    ? Gravity.CENTER_VERTICAL | Gravity.START : Gravity.CENTER);
            mSearchInput.setHint(horizontalPaging
                    ? getResources().getString(R.string.all_apps_search_bar_hint_horizontal)
                    : mDefaultSearchHint);
            if (horizontalPaging) {
                // Marshmallow's search field used the standard Roboto face and neutral gray
                // colors instead of inheriting the launcher's accent styling.
                mSearchInput.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
                mSearchInput.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 16);
                if (isDarkTheme()) {
                    mSearchInput.setTextColor(Themes.getAttrColor(getContext(),
                            android.R.attr.textColorPrimary));
                    mSearchInput.setHintTextColor(Themes.getAttrColor(getContext(),
                            android.R.attr.textColorHint));
                } else {
                    mSearchInput.setTextColor(Color.rgb(0x4c, 0x4c, 0x4c));
                    mSearchInput.setHintTextColor(Color.rgb(0x9c, 0x9c, 0x9c));
                }
                int searchPadding = getResources().getDimensionPixelSize(
                        R.dimen.all_apps_search_horizontal_padding);
                mSearchInput.setPaddingRelative(searchPadding, mSearchInput.getPaddingTop(),
                        mSearchPaddingEnd, mSearchInput.getPaddingBottom());
            } else {
                mSearchInput.setTypeface(mSearchTypeface);
                mSearchInput.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,
                        mSearchTextSize);
                mSearchInput.setTextColor(mSearchTextColors);
                mSearchInput.setHintTextColor(mSearchHintColors);
                mSearchInput.setPaddingRelative(mSearchPaddingStart,
                        mSearchInput.getPaddingTop(), mSearchPaddingEnd,
                        mSearchInput.getPaddingBottom());
            }
            mHorizontalSearchDivider.setVisibility(horizontalPaging ? View.VISIBLE : View.GONE);
            RelativeLayout.LayoutParams listLp =
                    (RelativeLayout.LayoutParams) mAppsRecyclerView.getLayoutParams();
            if (horizontalPaging) {
                listLp.removeRule(RelativeLayout.ALIGN_PARENT_TOP);
                listLp.addRule(RelativeLayout.BELOW, R.id.search_container_all_apps);
                listLp.topMargin = 0;
                RelativeLayout.LayoutParams searchLp =
                        (RelativeLayout.LayoutParams) mSearchContainer.getLayoutParams();
                searchLp.leftMargin = getResources().getDimensionPixelSize(
                        R.dimen.all_apps_page_card_inset);
                searchLp.rightMargin = searchLp.leftMargin;
                mSearchContainer.setLayoutParams(searchLp);
                GradientDrawable searchBackground = new GradientDrawable();
                searchBackground.setColor(isDarkTheme()
                        ? Themes.getAttrColor(getContext(), android.R.attr.colorPrimary)
                        : Color.WHITE);
                float radius = getResources().getDimension(
                        R.dimen.all_apps_page_card_corner_radius);
                searchBackground.setCornerRadii(new float[] {
                        radius, radius, radius, radius, 0, 0, 0, 0});
                mSearchContainer.setBackground(searchBackground);
                // The stock header outline expands past its left and right edges for a
                // bottom-only shadow. That outline looks detached from the page cards here.
                mSearchContainer.setElevation(0);
            } else {
                listLp.removeRule(RelativeLayout.ALIGN_PARENT_TOP);
                listLp.addRule(RelativeLayout.BELOW, R.id.search_container_all_apps);
                listLp.topMargin = 0;
                RelativeLayout.LayoutParams searchLp =
                        (RelativeLayout.LayoutParams) mSearchContainer.getLayoutParams();
                searchLp.leftMargin = 0;
                searchLp.rightMargin = 0;
                mSearchContainer.setLayoutParams(searchLp);
                mSearchContainer.setBackground(null);
            }
            listLp.bottomMargin = horizontalPaging
                    ? getResources().getDimensionPixelSize(R.dimen.all_apps_page_indicator_space)
                    : 0;
            mAppsRecyclerView.setLayoutParams(listLp);
            mAppsRecyclerView.setBackground(null);
            updateBackground(0, 0, 0, 0);
        }
        if (FeatureFlags.LAUNCHER3_ALL_APPS_PULL_UP) {
            if (mNumAppsPerRow != appsPerRow ||
                    mNumPredictedAppsPerRow != appsPerRow) {
                mNumAppsPerRow = appsPerRow;
                mNumPredictedAppsPerRow = appsPerRow;

                mAppsRecyclerView.setNumAppsPerRow(grid, mNumAppsPerRow);
                mAdapter.setNumAppsPerRow(mNumAppsPerRow);
                mApps.setNumAppsPerRow(mNumAppsPerRow, mNumPredictedAppsPerRow);
                mAdapter.setHorizontalPaging(mHorizontalPaging, appsPerRow);
            }
            updateHorizontalPageIndicator();
            if (!grid.isVerticalBarLayout()) {
                MarginLayoutParams searchContainerLp =
                        (MarginLayoutParams) mSearchContainer.getLayoutParams();

                searchContainerLp.height = mLauncher.getDragLayer().getInsets().top
                        + mSearchContainerMinHeight;
                mSearchContainer.setLayoutParams(searchContainerLp);
            }
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }

        // --- remove START when {@code FeatureFlags.LAUNCHER3_ALL_APPS_PULL_UP} is enabled. ---

        // Update the number of items in the grid before we measure the view
        grid.updateAppsViewNumCols();
        int predictedAppsPerRow = horizontalPaging ? appsPerRow : grid.allAppsNumPredictiveCols;
        if (mNumAppsPerRow != appsPerRow ||
                mNumPredictedAppsPerRow != predictedAppsPerRow) {
            mNumAppsPerRow = appsPerRow;
            mNumPredictedAppsPerRow = predictedAppsPerRow;

            mAppsRecyclerView.setNumAppsPerRow(grid, mNumAppsPerRow);
            mAdapter.setNumAppsPerRow(mNumAppsPerRow);
            mApps.setNumAppsPerRow(mNumAppsPerRow, mNumPredictedAppsPerRow);
            mAdapter.setHorizontalPaging(mHorizontalPaging, appsPerRow);
        }

        // --- remove END when {@code FeatureFlags.LAUNCHER3_ALL_APPS_PULL_UP} is enabled. ---
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        // Determine if the key event was actual text, if so, focus the search bar and then dispatch
        // the key normally so that it can process this key event
        if (!mSearchBarController.isSearchFieldFocused() &&
                event.getAction() == KeyEvent.ACTION_DOWN) {
            final int unicodeChar = event.getUnicodeChar();
            final boolean isKeyNotWhitespace = unicodeChar > 0 &&
                    !Character.isWhitespace(unicodeChar) && !Character.isSpaceChar(unicodeChar);
            if (isKeyNotWhitespace) {
                boolean gotKey = TextKeyListener.getInstance().onKeyDown(this, mSearchQueryBuilder,
                        event.getKeyCode(), event);
                if (gotKey && mSearchQueryBuilder.length() > 0) {
                    mSearchBarController.focusSearchField();
                }
            }
        }

        return super.dispatchKeyEvent(event);
    }

    @Override
    public boolean onLongClick(final View v) {
        // Return early if this is not initiated from a touch
        if (!v.isInTouchMode()) return false;
        // When we have exited all apps or are in transition, disregard long clicks

        if (!mLauncher.isAppsViewVisible() ||
                mLauncher.getWorkspace().isSwitchingState()) return false;
        // Return if global dragging is not enabled or we are already dragging
        if (!mLauncher.isDraggingEnabled()) return false;
        if (mLauncher.getDragController().isDragging()) return false;

        // Start the drag
        final DragController dragController = mLauncher.getDragController();
        dragController.addDragListener(new DragController.DragListener() {
            @Override
            public void onDragStart(DropTarget.DragObject dragObject, DragOptions options) {
                v.setVisibility(INVISIBLE);
            }

            @Override
            public void onDragEnd() {
                v.setVisibility(VISIBLE);
                dragController.removeDragListener(this);
            }
        });
        mLauncher.getWorkspace().beginDragShared(v, this, new DragOptions());
        return false;
    }

    @Override
    public boolean supportsAppInfoDropTarget() {
        return true;
    }

    @Override
    public boolean supportsDeleteDropTarget() {
        return false;
    }

    @Override
    public float getIntrinsicIconScaleFactor() {
        DeviceProfile grid = mLauncher.getDeviceProfile();
        return (float) grid.allAppsIconSizePx / grid.iconSizePx;
    }

    @Override
    public void onDropCompleted(View target, DropTarget.DragObject d, boolean isFlingToDelete,
            boolean success) {
        if (isFlingToDelete || !success || (target != mLauncher.getWorkspace() &&
                !(target instanceof DeleteDropTarget) && !(target instanceof Folder))) {
            // Exit spring loaded mode if we have not successfully dropped or have not handled the
            // drop in Workspace
            mLauncher.exitSpringLoadedDragModeDelayed(true,
                    Launcher.EXIT_SPRINGLOADED_MODE_SHORT_TIMEOUT, null);
        }
        mLauncher.unlockScreenOrientation(false);

        if (!success) {
            d.deferDragViewCleanupPostAnimation = false;
        }
    }

    @Override
    public void onSearchResult(String query, ArrayList<ComponentKey> apps) {
        if (apps != null) {
            mApps.setOrderedFilter(apps);
            mAppsRecyclerView.onSearchResultsChanged();
            mAdapter.setLastSearchQuery(query);
        }
    }

    @Override
    public void onAppDiscoverySearchUpdate(@Nullable AppDiscoveryItem app,
            @NonNull AppDiscoveryUpdateState state) {
        if (!mLauncher.isDestroyed()) {
            mApps.onAppDiscoverySearchUpdate(app, state);
            mAppsRecyclerView.onSearchResultsChanged();
        }
    }

    @Override
    public void clearSearchResult() {
        if (mApps.setOrderedFilter(null)) {
            mAppsRecyclerView.onSearchResultsChanged();
        }

        // Clear the search query
        mSearchQueryBuilder.clear();
        mSearchQueryBuilder.clearSpans();
        Selection.setSelection(mSearchQueryBuilder, 0);
    }

    @Override
    public void fillInLogContainerData(View v, ItemInfo info, Target.Builder target, Target.Builder targetParent) {
        targetParent.setContainerType(mAppsRecyclerView.getContainerType(v));
    }

    public boolean shouldRestoreImeState() {
        return !TextUtils.isEmpty(mSearchInput.getText());
    }

    @Override
    public void setInsets(Rect insets) {
        DeviceProfile grid = mLauncher.getDeviceProfile();
        if (grid.isVerticalBarLayout()) {
            ViewGroup.MarginLayoutParams mlp = (MarginLayoutParams) getLayoutParams();
            mlp.leftMargin = insets.left;
            mlp.topMargin = insets.top;
            mlp.rightMargin = insets.right;
            setLayoutParams(mlp);
        } else {
            View navBarBg = findViewById(R.id.nav_bar_bg);
            ViewGroup.LayoutParams navBarBgLp = navBarBg.getLayoutParams();
            navBarBgLp.height = insets.bottom;
            navBarBg.setLayoutParams(navBarBgLp);
            navBarBg.setVisibility(View.VISIBLE);
        }
    }

    public void updateIconBadges(Set<PackageUserKey> updatedBadges) {
        final PackageUserKey packageUserKey = new PackageUserKey(null, null);
        final int n = mAppsRecyclerView.getChildCount();
        for (int i = 0; i < n; i++) {
            View child = mAppsRecyclerView.getChildAt(i);
            if (!(child instanceof BubbleTextView) || !(child.getTag() instanceof ItemInfo)) {
                continue;
            }
            ItemInfo info = (ItemInfo) child.getTag();
            if (packageUserKey.updateFromItemInfo(info) && updatedBadges.contains(packageUserKey)) {
                ((BubbleTextView) child).applyBadgeState(info, true /* animate */);
            }
        }
    }
}
