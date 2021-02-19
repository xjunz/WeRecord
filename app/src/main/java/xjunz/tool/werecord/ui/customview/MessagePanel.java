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
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.widget.NestedScrollView;
import androidx.customview.widget.ViewDragHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

import xjunz.tool.werecord.R;

/**
 * 消息页面面板
 *
 * @see R.layout#activity_message
 */
public class MessagePanel extends ConstraintLayout {
    private ViewDragHelper mHelper;
    private View mIndicator, mEtSearch, mIbEdit;
    private ViewPager2 mViewPager;
    private ViewGroup mBottomBar, mCurtain, mIbContainer;
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
                }
            }

            @Override
            public void onPanelSlide(float fraction) {
                mEtSearch.setAlpha(fraction);
                mBottomBar.setElevation(fraction * mBottomBarElevation);
                int containerWidth = (int) (mMinContainerWidth + (1 - fraction) * (mIbContainerWidth - mMinContainerWidth));
                mIbContainer.setRight(containerWidth);
                setWidth(mIbContainer, containerWidth);
                int indicatorWidth = containerWidth / 3;
                setWidth(mIndicator, indicatorWidth);
                mIndicator.setTranslationX(mViewPager.getCurrentItem() * indicatorWidth);
                mEtSearch.setTranslationX(mIbContainerWidth - (mIbContainerWidth - mMinContainerWidth /* *(2f / 3f)*/) * fraction);
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

    //这个方法会调用onLayout，所以要在onLayout中增加处理逻辑
    private void setWidth(@NotNull View view, int width) {
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
    public boolean onInterceptTouchEvent(@NotNull MotionEvent event) {
        mX = event.getX();
        mY = event.getY();
        return mHelper.shouldInterceptTouchEvent(event);
    }

    private float mX, mY;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(@NotNull MotionEvent event) {
        mX = event.getX();
        mY = event.getY();
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
        mEtSearch = findViewById(R.id.et_search);
        mIbEdit = findViewById(R.id.ib_edit);
        mIbContainer = findViewById(R.id.ll_ib_container);
        mBottomBarElevation = mBottomBar.getElevation();
        findViewById(R.id.ib_stats).setOnClickListener(v -> {
            mViewPager.setCurrentItem(0, true);
            if (!isOpen()) {
                openPanel();
            }
        });
        findViewById(R.id.ib_search).setOnClickListener(v -> {
            mViewPager.setCurrentItem(2, true);
            if (!isOpen()) {
                openPanel();
            }
        });
        findViewById(R.id.ib_edit).setOnClickListener(v -> {
            mViewPager.setCurrentItem(1, true);
            if (!isOpen()) {
                openPanel();
            }
        });
        mViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels);
                if (positionOffset != 0) {
                    mIndicator.setTranslationX(position * mIndicator.getWidth() + mIndicator.getWidth() * positionOffset);
                }
            }
        });
    }


    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (!mReadyToUser) {
            super.onLayout(changed, left, top, right, bottom);
            mCurtainHeight = mCurtain.getMeasuredHeight();
            mMaxTop = mCurtainHeight - mIbContainer.getMeasuredHeight();
            mMinTop = 0;
            mIbContainerWidth = mIbContainer.getMeasuredWidth();
            mMinContainerWidth = (int) (mIbContainerWidth * 0.5);
            mCurtain.setBottom(mMaxTop + mCurtainHeight);
            mCurtain.setTop(mMaxTop);
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

    private final ViewDragHelper.Callback mCallback = new ViewDragHelper.Callback() {
        private int mFormerState = ViewDragHelper.STATE_IDLE;

        @Override
        public boolean tryCaptureView(@NonNull View child, int pointerId) {
            if (child.getId() == mCurtain.getId()) {
                //当我们的搜索结果列表在点击范围内且不能下滑时，才返回true
                //不然我们的列表没法下滑
                RecyclerView listResult = mCurtain.findViewById(R.id.rv_result);
                RecyclerView listEdition = mCurtain.findViewById(R.id.rv_edition);
                NestedScrollView statsScrollView = mCurtain.findViewById(R.id.nsv_stats);
                if (listResult != null) {
                    int[] lt = new int[2];
                    listResult.getLocationInWindow(lt);
                    if (mX >= lt[0] && mX <= lt[0] + listResult.getWidth() && mY >= lt[1] && mY <= lt[1] + listResult.getHeight()) {
                        return !listResult.canScrollVertically(-1);
                    }
                }
                if (listEdition != null) {
                    int[] lt = new int[2];
                    listEdition.getLocationInWindow(lt);
                    if (mX >= lt[0] && mX <= lt[0] + listEdition.getWidth() && mY >= lt[1] && mY <= lt[1] + listEdition.getHeight()) {
                        return !listEdition.canScrollVertically(-1);
                    }
                }
                if (statsScrollView != null) {
                    int[] lt = new int[2];
                    statsScrollView.getLocationInWindow(lt);
                    if (mX >= lt[0] && mX <= lt[0] + statsScrollView.getWidth() && mY >= lt[1] && mY <= lt[1] + statsScrollView.getHeight()) {
                        return !statsScrollView.canScrollVertically(-1);
                    }
                }
                return true;
            }
            return false;
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