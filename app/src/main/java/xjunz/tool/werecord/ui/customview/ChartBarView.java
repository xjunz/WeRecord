/*
 * Copyright (c) 2021 xjunz. 保留所有权利
 */
package xjunz.tool.werecord.ui.customview;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.FloatRange;
import androidx.annotation.Nullable;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;

import xjunz.tool.werecord.R;

/**
 * @author xjunz 2021/1/12 14:42
 */
public class ChartBarView extends View {
    /**
     * 百分比
     */
    private float mFraction;
    private float mCurrentFraction;
    private final Paint mPaint;

    public ChartBarView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.ChartBarView);
        mPaint = new Paint();
        mPaint.setColor(ta.getColor(R.styleable.ChartBarView_barColor, 0));
        mPaint.setAntiAlias(true);
        ta.recycle();
    }


    public void setFraction(@FloatRange(from = 0.0, to = 1.0) float fraction) {
        mFraction = fraction;
    }

    public void animateProgress() {
        this.post(() -> {
            ValueAnimator animator = ValueAnimator.ofFloat(0, mFraction);
            animator.addUpdateListener(animation -> {
                mCurrentFraction = (float) animation.getAnimatedValue();
                invalidate();
            });
            animator.setDuration(500L);
            animator.setInterpolator(new FastOutSlowInInterpolator());
            animator.start();
        });
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int height = getMeasuredHeight();
        //canvas.drawRect(0, 0, getMeasuredWidth() * mCurrentFraction, height, mPaint);
        canvas.drawRoundRect(0, 0, getMeasuredWidth() * mCurrentFraction, height, height / 2f, height / 2f, mPaint);
    }
}
