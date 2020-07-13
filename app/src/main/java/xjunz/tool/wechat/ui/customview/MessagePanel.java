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

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.customview.widget.ViewDragHelper;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

import xjunz.tool.wechat.R;

/**
 *
 */
public class MessagePanel extends ConstraintLayout {
    private ViewDragHelper mHelper;
    private View mIndicator, mIbStats, mIbSearch, mEtSearch, mIbEdit;
    private ViewGroup mViewPager, mBottomBar, mCurtain, mIbContainer;
    private int mCurtainHeight;
    private int mMaxTop, mMinTop;
    private int mIbContainerWidth, mMinContainerWidth;
    private boolean mReadyToUser = false;
    private float mBottomBarElevation;
    private ArrayList<OnPanelSlideListener> mListenerList;

    private void init() {
        mHelper = ViewDragHelper.create(this, 1.0f, mCallback);
        mListenerList = new ArrayList<>();

        this.addOnPanelSlideListener(new OnPanelSlideListener() {
            @Override
            public void onPanelSlideFinished(boolean isOpen) {
                if (!isOpen) {
                    mEtSearch.setVisibility(GONE);
                } else {
                    mIbEdit.setVisibility(INVISIBLE);
                }
            }

            @Override
            public void onPanelSlide(float fraction) {
                mEtSearch.setAlpha(fraction);
                mBottomBar.setElevation(fraction * mBottomBarElevation);
                mIbEdit.setAlpha(1 - fraction);
                int containerWidth = (int) (mMinContainerWidth + (1 - fraction) * (mIbContainerWidth - mMinContainerWidth));
                setWidth(mIbContainer, containerWidth);
                setWidth(mIndicator, containerWidth / 3);
                mEtSearch.setTranslationX(mIbContainerWidth - (mIbContainerWidth - mMinContainerWidth * (2f / 3f)) * fraction);
            }

            @Override
            public void onPanelSlideStart(boolean isToOpen) {
                mReadyToUser = true;
                if (isToOpen) {
                    mEtSearch.setAlpha(0f);
                    setWidth(mEtSearch, mIbContainerWidth - mMinContainerWidth);
                    mEtSearch.setTranslationX(mIbContainerWidth);
                    mEtSearch.setVisibility(VISIBLE);
                } else {
                    mIbEdit.setVisibility(VISIBLE);
                }
            }
        });
    }

    private void setWidth(View view, int width) {
        ViewGroup.LayoutParams lp = view.getLayoutParams();
        lp.width = width;
        view.setLayoutParams(lp);
    }

    public MessagePanel(Context context) {
        super(context);
        init();
    }

    public MessagePanel(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MessagePanel(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public void addOnPanelSlideListener(@NonNull OnPanelSlideListener listener) {
        this.mListenerList.add(listener);
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

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mViewPager = findViewById(R.id.vp_message);
        mBottomBar = findViewById(R.id.bottom_bar);
        mCurtain = findViewById(R.id.ll_curtain);
        mIndicator = findViewById(R.id.indicator);
        mIbEdit = findViewById(R.id.ib_edit);
        mIbSearch = findViewById(R.id.ib_search);
        mIbStats = findViewById(R.id.ib_stats);
        mEtSearch = findViewById(R.id.et_search);
        mIbContainer = findViewById(R.id.ll_ib_container);
        mBottomBarElevation = mBottomBar.getElevation();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (!mReadyToUser) {
            super.onLayout(changed, left, top, right, bottom);
            mCurtainHeight = mCurtain.getHeight();
            mMaxTop = mCurtainHeight - mIbContainer.getHeight();
            mMinTop = 0;
            mIbContainerWidth = mIbContainer.getWidth();
            mMinContainerWidth = (int) (mIbContainerWidth * 0.5);
            mCurtain.setTop(mMaxTop);
            mCurtain.setBottom(mMaxTop + mCurtainHeight);

        } else {
            int curtainCurTop = mCurtain.getTop();
            super.onLayout(changed, left, top, right, bottom);
            mCurtain.layout(left, curtainCurTop, right, curtainCurTop + mCurtainHeight);
        }
    }

    public void openPanel() {
        if (mHelper.smoothSlideViewTo(mCurtain, 0, mMinTop)) {
            postInvalidateOnAnimation();
        }
    }

    public void closePanel() {
        if (mHelper.smoothSlideViewTo(mCurtain, 0, mMaxTop)) {
            postInvalidateOnAnimation();
        }
    }

    @Override
    public void computeScroll() {
        if (mHelper.continueSettling(true)) {
            postInvalidateOnAnimation();
        }
    }

    private ViewDragHelper.Callback mCallback = new ViewDragHelper.Callback() {
        private int mFormerState = ViewDragHelper.STATE_IDLE;

        @Override
        public boolean tryCaptureView(@NonNull View child, int pointerId) {
            return child.getId() == mCurtain.getId();
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
                    listener.onPanelSlideStart(mMaxTop - top < top - mMinTop);
                }
            } else if (state == ViewDragHelper.STATE_IDLE) {
                for (OnPanelSlideListener listener : mListenerList) {
                    listener.onPanelSlideFinished(mCurtain.getTop() == mMinTop);
                }
            }
            mFormerState = state;
        }

        @Override
        public void onViewPositionChanged(@NonNull View changedView, int left, int top, int dx, int dy) {
            super.onViewPositionChanged(changedView, left, top, dx, dy);
            float fraction = (float) (mMaxTop - top) / (mMaxTop - mMinTop);
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
            super.onViewReleased(releasedChild, xvel, yvel);
        }
    };

    public interface OnPanelSlideListener {
        void onPanelSlideFinished(boolean isOpen);

        void onPanelSlide(float fraction);

        void onPanelSlideStart(boolean isToOpen);
    }

    public boolean isOpen() {
        return mCurtain.getTop() == mMinTop;
    }
}