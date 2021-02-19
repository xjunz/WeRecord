/*
 * Copyright (c) 2021 xjunz. 保留所有权利
 */
package xjunz.tool.werecord.ui.customview;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.view.OneShotPreDrawListener;
import androidx.customview.widget.ViewDragHelper;

import org.jetbrains.annotations.NotNull;

import xjunz.tool.werecord.R;

/**
 * @author xjunz 2021/1/2 13:11
 */
public class TemplatePanel extends ConstraintLayout {
    private int mBottomBarHeight;
    private View mHandler, mBottomSheet, mScrollView, mMask;
    private ViewDragHelper mHelper;
    private int mMaxTop, mMinTop;
    private int mCurrentTop;

    public TemplatePanel(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (!isLaidOut()) {
            mBottomBarHeight = findViewById(R.id.bottom_bar).getHeight();
            //当整个BottomSheet都展开时的高度，但是至少要留180px的顶部边距
            mMinTop = Math.max(180, getHeight() - mBottomSheet.getHeight() - mBottomBarHeight);
            //当只显示Handler时的top
            mMaxTop = getHeight() - mBottomBarHeight - mHandler.getHeight();
            mCurrentTop = mMaxTop;
        } else {
            mScrollView.setBottom(mCurrentTop);
        }
        mBottomSheet.layout(0, mCurrentTop, mBottomSheet.getRight(), mCurrentTop + mBottomSheet.getHeight());
    }

    /**
     * 监听DecorView的Layout事件，处理软键盘的弹起与关闭
     */
    private final ViewTreeObserver.OnGlobalLayoutListener mOnGlobalLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
        @Override
        public void onGlobalLayout() {
            //记录BottomSheet打开的高度
            int formerTopOffset = mMaxTop - mBottomSheet.getTop();
            //min:当整个BottomSheet都展开时的高度，但是至少要留180px的顶部边距
            mMinTop = Math.max(180, getHeight() - mBottomSheet.getHeight() - mBottomBarHeight);
            //max:当只显示Handler时的top
            mMaxTop = getHeight() - mBottomBarHeight - mHandler.getHeight();
            //更新currentTop
            mCurrentTop = mMaxTop - formerTopOffset;
            //重新放置我们的BottomSheet（保持之前的打开高度）
            mBottomSheet.layout(0, mCurrentTop, mBottomSheet.getRight(), mCurrentTop + mBottomSheet.getHeight());
            mScrollView.setBottom(mCurrentTop);
        }
    };

    private void init() {
        mHelper = ViewDragHelper.create(this, mCallback);
        getViewTreeObserver().addOnGlobalLayoutListener(mOnGlobalLayoutListener);
        OneShotPreDrawListener.add(this, () -> {
            EditText editor = findViewById(R.id.et_editor);
            editor.setMinHeight(mScrollView.getHeight());
        });
    }

    @Override
    public boolean onInterceptTouchEvent(@NotNull MotionEvent event) {
        return mHelper.shouldInterceptTouchEvent(event);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        getViewTreeObserver().removeOnGlobalLayoutListener(mOnGlobalLayoutListener);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(@NotNull MotionEvent event) {
        mHelper.processTouchEvent(event);
        return true;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mHandler = findViewById(R.id.handler);
        mBottomSheet = findViewById(R.id.bottom_sheet);
        mScrollView = findViewById(R.id.sv_editor);
        mMask = findViewById(R.id.mask);
        mMask.setOnClickListener(v -> closePanel());
        mHandler.setOnClickListener(v -> openPanel());
    }

    public void openPanel() {
        if (mHelper.smoothSlideViewTo(mBottomSheet, 0, mMinTop)) {
            postInvalidateOnAnimation();
        }
    }

    public void closePanel() {
        if (mHelper.smoothSlideViewTo(mBottomSheet, 0, mMaxTop)) {
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
        private int mFormerState = ViewDragHelper.STATE_IDLE;

        @Override
        public boolean tryCaptureView(@NonNull View child, int pointerId) {
            return child.getId() == R.id.bottom_sheet;
        }

        @Override
        public int clampViewPositionVertical(@NonNull View child, int top, int dy) {
            return Math.min(Math.max(top, mMinTop), mMaxTop);
        }

        @Override
        public void onViewDragStateChanged(int state) {
            super.onViewDragStateChanged(state);
            //开始滑动
            if (mFormerState == ViewDragHelper.STATE_IDLE && (state == ViewDragHelper.STATE_DRAGGING || state == ViewDragHelper.STATE_SETTLING)) {
                int top = mBottomSheet.getTop();
                boolean isToOpen = mMaxTop - top < top - mMinTop;
                if (isToOpen) {
                    mMask.setVisibility(View.VISIBLE);
                }
            }
            //停止滑动
            else if (state == ViewDragHelper.STATE_IDLE) {
                boolean isClosed = mBottomSheet.getTop() == mMaxTop;
                if (isClosed) {
                    mMask.setVisibility(GONE);
                }
            }
            mFormerState = state;
        }

        @Override
        public void onViewPositionChanged(@NonNull View changedView, int left, int top, int dx, int dy) {
            super.onViewPositionChanged(changedView, left, top, dx, dy);
            mCurrentTop = mBottomSheet.getTop();
            mMask.setAlpha((float) (mMaxTop - top) / (mMaxTop - mMinTop));
        }

        @Override
        public int getViewVerticalDragRange(@NonNull View child) {
            return mMaxTop - mMinTop;
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
