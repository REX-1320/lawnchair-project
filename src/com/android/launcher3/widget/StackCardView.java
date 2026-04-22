package com.android.launcher3.widget;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class StackCardView extends FrameLayout {

    private TextView mTitleView;

    public StackCardView(@NonNull Context context) {
        super(context);
        init(context);
    }

    public StackCardView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public StackCardView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        setClipToPadding(false);
        int padding = dpToPx(16);
        setPadding(padding, padding, padding, padding);

        GradientDrawable background = new GradientDrawable();
        background.setColor(0xFF1B1B1B);
        background.setCornerRadius(dpToPx(20));
        background.setStroke(dpToPx(1), 0x22FFFFFF);
        setBackground(background);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setElevation(dpToPx(6));
        }

        mTitleView = new TextView(context);
        mTitleView.setTextColor(0xFFFFFFFF);
        mTitleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        mTitleView.setGravity(Gravity.CENTER_VERTICAL);
        mTitleView.setText("Card");

        LayoutParams textLp = new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT);
        addView(mTitleView, textLp);
    }

    public void bindTitle(@NonNull String title) {
        mTitleView.setText(title);
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}
