package com.android.launcher3.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class StackWidgetView extends FrameLayout {

    private final List<View> mCards = new ArrayList<>();
    private SnapScrollView mScrollView;
    private LinearLayout mCardContainer;

    private int mCardSpacingPx;
    private int mOuterPaddingPx;
    private int mCardWidthPx;
    private int mPageSizePx;
    private boolean mLayoutDone;

    public StackWidgetView(@NonNull Context context) {
        super(context);
        init(context);
    }

    public StackWidgetView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public StackWidgetView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        setClipToPadding(false);
        setClipChildren(false);

        mOuterPaddingPx = dpToPx(12);
        mCardSpacingPx = dpToPx(12);

        mScrollView = new SnapScrollView(context);
        LayoutParams scrollLp = new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT);
        addView(mScrollView, scrollLp);

        mCardContainer = new LinearLayout(context);
        mCardContainer.setOrientation(LinearLayout.HORIZONTAL);
        mCardContainer.setPadding(mOuterPaddingPx, mOuterPaddingPx, mOuterPaddingPx, mOuterPaddingPx);
        mScrollView.addView(mCardContainer, new HorizontalScrollView.LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.MATCH_PARENT));

        setCards(createDefaultCards());
    }

    public void setCards(@Nullable List<View> cards) {
        mCards.clear();
        mCardContainer.removeAllViews();

        if (cards != null) {
            mCards.addAll(cards);
        }

        if (mCards.isEmpty()) {
            mCards.add(createPlaceholderCard("Empty"));
        }

        for (int index = 0; index < mCards.size(); index++) {
            View card = mCards.get(index);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LayoutParams.WRAP_CONTENT,
                    LayoutParams.MATCH_PARENT);
            if (index < mCards.size() - 1) {
                lp.rightMargin = mCardSpacingPx;
            }
            mCardContainer.addView(card, lp);
        }

        mLayoutDone = false;
        requestLayout();
    }

    private List<View> createDefaultCards() {
        List<View> defaultCards = new ArrayList<>(4);
        defaultCards.add(createPlaceholderCard("Stack Card 1"));
        defaultCards.add(createPlaceholderCard("Stack Card 2"));
        defaultCards.add(createPlaceholderCard("Stack Card 3"));
        defaultCards.add(createPlaceholderCard("Stack Card 4"));
        return defaultCards;
    }

    private View createPlaceholderCard(String title) {
        StackCardView cardView = new StackCardView(getContext());
        cardView.bindTitle(title);
        return cardView;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w > 0 && h > 0) {
            updateCardSizes(w);
        }
    }

    private void updateCardSizes(int width) {
        int contentWidth = Math.max(0, width - (mOuterPaddingPx * 2));
        mCardWidthPx = Math.max(dpToPx(180), contentWidth - dpToPx(28));
        mPageSizePx = mCardWidthPx + mCardSpacingPx;

        for (int index = 0; index < mCardContainer.getChildCount(); index++) {
            View card = mCardContainer.getChildAt(index);
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) card.getLayoutParams();
            lp.width = mCardWidthPx;
            lp.height = LayoutParams.MATCH_PARENT;
            card.setLayoutParams(lp);
        }

        if (!mLayoutDone) {
            mLayoutDone = true;
            post(() -> snapToPage(0, false));
        }
    }

    private void snapToNearestPage() {
        if (mPageSizePx <= 0 || mCardContainer.getChildCount() == 0) {
            return;
        }
        int scrollX = mScrollView.getScrollX();
        int page = Math.round(scrollX / (float) mPageSizePx);
        page = Math.max(0, Math.min(page, mCardContainer.getChildCount() - 1));
        snapToPage(page, true);
    }

    private void snapToPage(int page, boolean smooth) {
        if (mPageSizePx <= 0) {
            return;
        }
        int targetX = page * mPageSizePx;
        if (smooth) {
            mScrollView.smoothScrollTo(targetX, 0);
        } else {
            mScrollView.scrollTo(targetX, 0);
        }
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private final class SnapScrollView extends HorizontalScrollView {

        private final int mTouchSlop;
        private float mDownX;
        private float mDownY;
        private boolean mDisallowParentIntercept;

        SnapScrollView(Context context) {
            super(context);
            setHorizontalScrollBarEnabled(false);
            setVerticalScrollBarEnabled(false);
            setFillViewport(true);
            setSmoothScrollingEnabled(true);
            setOverScrollMode(OVER_SCROLL_NEVER);
            mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        }

        @Override
        public boolean onTouchEvent(MotionEvent ev) {
            int action = ev.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN) {
                mDownX = ev.getX();
                mDownY = ev.getY();
                mDisallowParentIntercept = false;
            } else if (action == MotionEvent.ACTION_MOVE) {
                if (!mDisallowParentIntercept) {
                    float dx = Math.abs(ev.getX() - mDownX);
                    float dy = Math.abs(ev.getY() - mDownY);
                    if (dx > mTouchSlop && dx > dy) {
                        requestDisallowInterceptTouchEvent(true);
                        mDisallowParentIntercept = true;
                    }
                }
            } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                snapToNearestPage();
                requestDisallowInterceptTouchEvent(false);
                mDisallowParentIntercept = false;
            }
            return super.onTouchEvent(ev);
        }
    }
}
