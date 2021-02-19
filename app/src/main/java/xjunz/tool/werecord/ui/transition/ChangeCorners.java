/*
 * Copyright (c) 2021 xjunz. 保留所有权利
 */
package xjunz.tool.werecord.ui.transition;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Outline;
import android.transition.Transition;
import android.transition.TransitionValues;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;

import androidx.annotation.Dimension;

import org.jetbrains.annotations.NotNull;

import xjunz.tool.werecord.R;

/**
 * 一个改变{@link View}的圆角半径的{@code Shared Element} {@link Transition}
 * <p>
 * 实现方式为改变{@link View}的{@link Outline}的圆角半径，根据{@link ChangeCorners#mStartCorner}
 * 和{@link ChangeCorners#mEndCorner}构造{@link ValueAnimator}
 *
 * @author xjunz 2020/7/16 00:56
 * @see View#setOutlineProvider(ViewOutlineProvider)
 * @see ChangeCorners#createAnimator(ViewGroup, TransitionValues, TransitionValues)
 * @see R.styleable#Morph_startCornerRadius
 * @see R.styleable#Morph_endCornerRadius
 * </p>
 */
public class ChangeCorners extends Transition {

    /**
     * 起始{@link android.transition.Scene}的{@code Shared Element}的圆角半径
     */
    @Dimension
    private final int mStartCorner;
    /**
     * 结束{@link android.transition.Scene}的{@code Shared Element}的圆角半径
     */
    @Dimension
    private final int mEndCorner;

    public ChangeCorners(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.Morph);
        mStartCorner = ta.getDimensionPixelSize(R.styleable.Morph_startCornerRadius, 0);
        mEndCorner = ta.getDimensionPixelSize(R.styleable.Morph_endCornerRadius, 0);
        ta.recycle();
    }


    @Override
    public void captureStartValues(TransitionValues transitionValues) {
        //no-op
    }

    @Override
    public void captureEndValues(TransitionValues transitionValues) {
        //no-op
    }


    @Override
    public Animator createAnimator(ViewGroup sceneRoot, TransitionValues startValues, @NotNull TransitionValues endValues) {
        View target = endValues.view;
        target.setClipToOutline(true);
        ValueAnimator changeOutline = ValueAnimator.ofFloat(mStartCorner, mEndCorner);
        changeOutline.addUpdateListener(animation -> target.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                float corner = (float) animation.getAnimatedValue();
                outline.setRoundRect(view.getPaddingLeft(), view.getPaddingTop(), view.getWidth() - view.getPaddingLeft(),
                        view.getHeight() - view.getPaddingTop(), corner);
            }
        }));
        return changeOutline;
    }
}
