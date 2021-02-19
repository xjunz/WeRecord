/*
 * Copyright (c) 2021 xjunz. 保留所有权利
 */
package xjunz.tool.werecord.ui.customview;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.view.OneShotPreDrawListener;
import androidx.customview.widget.ViewDragHelper;

import org.jetbrains.annotations.NotNull;

import xjunz.tool.werecord.R;

/**
 * @author xjunz 2021/1/26 21:31
 */
public class ExportConfigPanel extends ConstraintLayout {
    private int mMaxTop, mMinTop;
    private ViewDragHelper mHelper;
    private ViewGroup mHandler, mSheet;
    private int mCurrentTop;

    public ExportConfigPanel(@NonNull Context context) {
        super(context);
        init();
    }

    public ExportConfigPanel(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    @Override
    public boolean onInterceptTouchEvent(@NotNull MotionEvent event) {
        return mHelper.shouldInterceptTouchEvent(event);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mHelper.processTouchEvent(event);
        return true;
    }


    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mHandler = findViewById(R.id.handler);
        mSheet = findViewById(R.id.config_sheet);
        mHandler.setOnClickListener(v -> openPanel());
    }

    private void init() {
        mHelper = ViewDragHelper.create(this, mCallback);
        OneShotPreDrawListener.add(this, () -> {
            int bottomBarTop = findViewById(R.id.bottom_bar).getTop();
            mCurrentTop = mSheet.getTop();
            mMaxTop = bottomBarTop - mHandler.getHeight();
            mMinTop = bottomBarTop - mSheet.getHeight();
            findViewById(R.id.rv_config).addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
                mCurrentTop = mSheet.getTop();
                mMaxTop = bottomBarTop - mHandler.getHeight();
                mMinTop = bottomBarTop - mSheet.getHeight();
            });
        });

    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (isLaidOut()) {
            mSheet.setTop(mCurrentTop);
            mSheet.setBottom(mCurrentTop + mSheet.getMeasuredHeight());
        }
    }

    public void openPanel() {
        if (mHelper.smoothSlideViewTo(mSheet, 0, mMinTop)) {
            postInvalidateOnAnimation();
        }
    }

    public void closePanel() {
        if (mHelper.smoothSlideViewTo(mSheet, 0, mMaxTop)) {
            postInvalidateOnAnimation();
        }
    }

    @Override
    public void computeScroll() {
        if (mHelper.continueSettling(true)) {
            postInvalidateOnAnimation();
        }
    }

    private final ViewDragHelper.Callback mCallback = new ViewDragHelper.Callback() {
        @Override
        public boolean tryCaptureView(@NonNull View child, int pointerId) {
            return child.getId() == R.id.config_sheet;
        }

        @Override
        public int getViewVerticalDragRange(@NonNull View child) {
            return mMaxTop - mMinTop;
        }

        @Override
        public int clampViewPositionVertical(@NonNull View child, int top, int dy) {
            return Math.max(mMinTop, Math.min(mMaxTop, top));
        }

        @Override
        public void onViewPositionChanged(@NonNull View changedView, int left, int top, int dx, int dy) {
            super.onViewPositionChanged(changedView, left, top, dx, dy);
            mCurrentTop = mSheet.getTop();
        }

        @Override
        public void onViewReleased(@NonNull View releasedChild, float xvel, float yvel) {
            super.onViewReleased(releasedChild, xvel, yvel);
            if (yvel > 0) {
                closePanel();
            } else if (yvel < 0) {
                openPanel();
            } else {
                if (((float) releasedChild.getTop() - mMinTop) / (mMaxTop - mMinTop) > .5f) {
                    closePanel();
                } else {
                    openPanel();
                }
            }
        }
    };
}
