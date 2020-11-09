/*
 * Copyright (c) 2020 xjunz. 保留所有权利
 */

package xjunz.tool.wechat.ui.customview;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.customview.widget.ViewDragHelper;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

import xjunz.tool.wechat.R;

/**
 * 主页面的面板视图
 */
public class MainPanel extends RelativeLayout {
    /**
     * {@link ViewDragHelper}实例，用于实现面板的拖拽
     */
    private ViewDragHelper mHelper;
    private ViewGroup mCurtain, mTopBar, mFilter;
    private View mMask;
    private int mCurtainHeight, mHandlerHeight, mFilterHeight;
    private int mMaxTop, mMinTop;
    private boolean mReadyToUser;
    private ArrayList<OnPanelSlideListener> mListenerList;

    public MainPanel(Context context) {
        super(context);
        init();
    }

    public MainPanel(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MainPanel(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public void addOnPanelSlideListener(@NonNull OnPanelSlideListener listener) {
        this.mListenerList.add(listener);
    }

    public void removeOnPanelSlideListener(@NonNull OnPanelSlideListener listener) {
        this.mListenerList.remove(listener);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        mCurtainHeight = mCurtain.getMeasuredHeight();
        mHandlerHeight = mTopBar.getMeasuredHeight();
        mFilterHeight = mFilter.getMeasuredHeight();
        mMinTop = mHandlerHeight - mCurtainHeight;
        mMaxTop = -mHandlerHeight;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (mReadyToUser) {
            int filterCurTop = mFilter.getTop();
            int curtainCurTop = mCurtain.getTop();
            super.onLayout(changed, l, t, r, b);
            mCurtain.layout(l, curtainCurTop, r, curtainCurTop + mCurtain.getHeight());
            mFilter.layout(l, filterCurTop, r, filterCurTop + mFilter.getHeight());
        } else {
            super.onLayout(changed, l, t, r, b);
            mCurtain.layout(l, mMinTop, r, mMinTop + mCurtainHeight);
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mCurtain = findViewById(R.id.ll_curtain);
        mTopBar = findViewById(R.id.top_bar);
        mFilter = findViewById(R.id.fl_filter);
        mMask = findViewById(R.id.mask);
        mMask.setOnClickListener(v -> closePanel());
    }

    private void init() {
        mHelper = ViewDragHelper.create(this, 1.0f, mCallback);
        mListenerList = new ArrayList<>();
        this.addOnPanelSlideListener(new OnPanelSlideListener() {

            @Override
            public void onPanelSlideFinished(boolean isOpen) {
                if (isOpen) {
                    mTopBar.setVisibility(INVISIBLE);
                }
                if (!isOpen) {
                    mMask.setVisibility(GONE);
                }
            }

            @Override
            public void onPanelSlide(float fraction) {
                mTopBar.setAlpha(1 - fraction);
                mFilter.setAlpha(fraction);
                mMask.setAlpha(fraction);
                mFilter.setTop((int) (fraction * mHandlerHeight));
                mFilter.setBottom(mFilter.getTop() + mFilterHeight);
            }


            @Override
            public void onPanelSlideStart(boolean isToOpen) {
                mReadyToUser = true;
                if (!isToOpen) {
                    mTopBar.findViewById(R.id.et_search).requestFocus();
                    mTopBar.setVisibility(VISIBLE);
                }
                if (isToOpen) {
                    mMask.setAlpha(0);
                    mMask.setVisibility(VISIBLE);
                }
            }
        });

    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        return mHelper.shouldInterceptTouchEvent(event);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mHelper.processTouchEvent(event);
        return true;
    }

    public void openPanel() {
        if (mHelper.smoothSlideViewTo(mCurtain, 0, mMaxTop)) {
            postInvalidateOnAnimation();
        }

    }

    public void closePanel() {
        if (mHelper.smoothSlideViewTo(mCurtain, 0, mMinTop)) {
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
            return child.getId() == R.id.ll_curtain;
        }

        @Override
        public void onViewCaptured(@NonNull View capturedChild, int activePointerId) {
            super.onViewCaptured(capturedChild, activePointerId);
        }

        @Override
        public int clampViewPositionVertical(@NonNull View child, int top, int dy) {
            return Math.min(mMaxTop, Math.max(top, mMinTop));
        }

        @Override
        public void onViewDragStateChanged(int state) {
            super.onViewDragStateChanged(state);
            if (mFormerState == ViewDragHelper.STATE_IDLE && (state == ViewDragHelper.STATE_DRAGGING || state == ViewDragHelper.STATE_SETTLING)) {
                int top = mCurtain.getTop();
                for (OnPanelSlideListener listener : mListenerList) {
                    listener.onPanelSlideStart(mMaxTop - top > top - mMinTop);
                }
            } else if (state == ViewDragHelper.STATE_IDLE) {
                for (OnPanelSlideListener listener : mListenerList) {
                    listener.onPanelSlideFinished(mCurtain.getTop() == mMaxTop);
                }
            }
            mFormerState = state;
        }

        @Override
        public void onViewPositionChanged(@NonNull View changedView, int left, int top, int dx, int dy) {
            super.onViewPositionChanged(changedView, left, top, dx, dy);
            float fraction = (float) (top - mMinTop) / (mMaxTop - mMinTop);
            for (OnPanelSlideListener listener : mListenerList) {
                listener.onPanelSlide(fraction);
            }
        }

        @Override
        public int getViewVerticalDragRange(@NonNull View child) {
            return mCurtainHeight;
        }

        @Override
        public void onViewReleased(@NotNull View releasedChild, float xvel, float yvel) {
            if (yvel > 0) {
                openPanel();
            } else if (yvel < 0) {
                closePanel();
            } else {
                if (((float) releasedChild.getTop() - mMinTop) / (mMaxTop - mMinTop) > .5f) {
                    openPanel();
                } else {
                    closePanel();
                }
            }
            super.onViewReleased(releasedChild, xvel, yvel);
        }
    };

    public interface OnPanelSlideListener {
        void onPanelSlideFinished(boolean isOpen);

        void onPanelSlide(float fraction);

        void onPanelSlideStart(boolean isToOpen);
    }

    public boolean isOpen() {
        return mCurtain.getTop() == mMaxTop;
    }
}
