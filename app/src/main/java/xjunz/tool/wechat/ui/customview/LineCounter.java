/*
 * Copyright (c) 2021 xjunz. 保留所有权利
 */
package xjunz.tool.wechat.ui.customview;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.text.Layout;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import xjunz.tool.wechat.R;

public class LineCounter extends View {
    private int mPreLineCount, mCurLineCount;
    private TextView mTarget;
    private final Paint mTextPaint;
    private ColorStateList mPaintColor;

    public LineCounter(@NonNull Context context) {
        super(context);
        mTextPaint = new TextView(context).getPaint();
    }

    public LineCounter(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        mTextPaint = new TextView(context).getPaint();
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.LineCounter);
        mPaintColor = ta.getColorStateList(R.styleable.LineCounter_lineCounterTextColor);
        if (mPaintColor == null) {
            mPaintColor = ColorStateList.valueOf(Color.BLACK);
        }
        mTextPaint.setColor(Color.BLACK);
        mTextPaint.setTextSize(ta.getDimensionPixelSize(R.styleable.LineCounter_lineCounterTextSize, (int) mTextPaint.getTextSize()));
        ta.recycle();
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        mTextPaint.setColor(mPaintColor.getColorForState(getDrawableState(), mPaintColor.getDefaultColor()));
    }

    /**
     * @see android.view.View#measure(int, int)
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(measureWidth(widthMeasureSpec),
                measureHeight(heightMeasureSpec));
    }

    /**
     * Determines the width of this view
     *
     * @param measureSpec A measureSpec packed into an int
     * @return The width of the view, honoring constraints from measureSpec
     */
    private int measureWidth(int measureSpec) {
        int result;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);

        if (specMode == MeasureSpec.EXACTLY) {
            // We were told how big to be
            result = specSize;
        } else {
            // Measure the text
            result = (int) mTextPaint.measureText(String.valueOf(mCurLineCount)) + getPaddingLeft()
                    + getPaddingRight();
            if (specMode == MeasureSpec.AT_MOST) {
                // Respect AT_MOST value if that was what is called for by measureSpec
                result = Math.min(result, specSize);
            }
        }

        return result;
    }

    /**
     * Determines the height of this view
     *
     * @param measureSpec A measureSpec packed into an int
     * @return The height of the view, honoring constraints from measureSpec
     */
    private int measureHeight(int measureSpec) {
        int result;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);

        int ascent = (int) mTextPaint.ascent();
        if (specMode == MeasureSpec.EXACTLY) {
            // We were told how big to be
            result = specSize;
        } else {
            // Measure the text (beware: ascent is a negative number)
            result = (int) (-ascent + mTextPaint.descent()) + getPaddingTop()
                    + getPaddingBottom();
            if (specMode == MeasureSpec.AT_MOST) {
                // Respect AT_MOST value if that was what is called for by measureSpec
                result = Math.min(result, specSize);
            }
        }
        return result;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Layout layout = mTarget.getLayout();
        int paddingTop = mTarget.getPaddingTop();
        for (int i = 0; i < mCurLineCount; i++) {
            float width = mTextPaint.measureText(String.valueOf(i + 1));
            canvas.drawText(String.valueOf(i + 1), getMeasuredWidth() - width - getPaddingStart(), layout.getLineBaseline(i) + paddingTop, mTextPaint);
        }
    }


    private final ViewTreeObserver.OnPreDrawListener mCounterSetter = new ViewTreeObserver.OnPreDrawListener() {
        @Override
        public boolean onPreDraw() {
            mCurLineCount = mTarget.getLineCount();
            if (mCurLineCount != mPreLineCount) {
                invalidate();
                requestLayout();
                mPreLineCount = mCurLineCount;
            }
            return true;
        }
    };

    public void bindTo(@NotNull TextView target) {
        mTarget = target;
        mTarget.getViewTreeObserver().addOnPreDrawListener(mCounterSetter);
    }
}
