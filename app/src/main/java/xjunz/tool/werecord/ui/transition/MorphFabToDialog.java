/*
 * Copyright (c) 2021 xjunz. 保留所有权利
 */

package xjunz.tool.werecord.ui.transition;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.transition.Transition;
import android.transition.TransitionValues;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.ColorInt;

import com.google.android.material.transition.platform.MaterialArcMotion;

import org.jetbrains.annotations.NotNull;

import xjunz.tool.werecord.R;


/**
 * A transition that morphs a circle into a rectangle, changing it's background color.
 * <p>
 * Modified from Plaid
 */
public class MorphFabToDialog extends Transition {
    private static final String PROPERTY_COLOR = "MorphFabToDialog:color";
    private static final String PROPERTY_CORNER_RADIUS = "MorphFabToDialog:cornerRadius";
    private static final String[] TRANSITION_PROPERTIES = {
            PROPERTY_COLOR,
            PROPERTY_CORNER_RADIUS
    };
    private Drawable mFabIconDrawable;
    @ColorInt
    private int mFabColor = Color.TRANSPARENT;
    private int mEndCornerRadius;
    private int mStartCornerRadius;

    public MorphFabToDialog(@ColorInt int fabColor, int endCornerRadius) {
        this(fabColor, endCornerRadius, -1);
    }

    public MorphFabToDialog(@ColorInt int fabColor, int endCornerRadius, int startCornerRadius) {
        super();
        setFabColor(fabColor);
        setEndCornerRadius(endCornerRadius);
        setStartCornerRadius(startCornerRadius);
    }

    public MorphFabToDialog(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.Morph);
        mFabColor = typedArray.getColor(R.styleable.Morph_startColor, mFabColor);
        mEndCornerRadius = typedArray.getDimensionPixelSize(R.styleable.Morph_endCornerRadius, mEndCornerRadius);
        mStartCornerRadius = typedArray.getDimensionPixelSize(R.styleable.Morph_startCornerRadius, mStartCornerRadius);
        if (typedArray.hasValue(R.styleable.Morph_fabIcon)) {
            mFabIconDrawable = typedArray.getDrawable(R.styleable.Morph_fabIcon).mutate();
        }
        setPathMotion(new MaterialArcMotion());
        typedArray.recycle();
    }

    public void setFabColor(@ColorInt int fabColor) {
        this.mFabColor = fabColor;
    }

    public void setEndCornerRadius(int endCornerRadius) {
        this.mEndCornerRadius = endCornerRadius;
    }

    public void setStartCornerRadius(int startCornerRadius) {
        this.mStartCornerRadius = startCornerRadius;
    }

    @Override
    public void captureStartValues(@NotNull TransitionValues transitionValues) {
        final View view = transitionValues.view;
        transitionValues.values.put(PROPERTY_COLOR, mFabColor);
        transitionValues.values.put(PROPERTY_CORNER_RADIUS,
                mStartCornerRadius > 0 ? mStartCornerRadius : view.getHeight() / 2);
    }

    @Override
    public void captureEndValues(@NotNull TransitionValues transitionValues) {
        transitionValues.values.put(PROPERTY_COLOR, Color.WHITE);
        transitionValues.values.put(PROPERTY_CORNER_RADIUS, mEndCornerRadius);
    }

    @Override
    public Animator createAnimator(final ViewGroup sceneRoot,
                                   TransitionValues startValues,
                                   final TransitionValues endValues) {
        if (startValues == null || endValues == null) {
            return null;
        }
        Integer startColor = (Integer) startValues.values.get(PROPERTY_COLOR);
        Integer startCornerRadius = (Integer) startValues.values.get(PROPERTY_CORNER_RADIUS);
        Integer endColor = (Integer) endValues.values.get(PROPERTY_COLOR);
        Integer endCornerRadius = (Integer) endValues.values.get(PROPERTY_CORNER_RADIUS);
        if (startColor == null || startCornerRadius == null || endColor == null ||
                endCornerRadius == null) {
            return null;
        }
        View target = endValues.view;
        int fabRadius = target.getWidth() / 2;
        int iconRadius = mFabIconDrawable.getIntrinsicWidth() / 2;
        mFabIconDrawable.setBounds(fabRadius - iconRadius, fabRadius - iconRadius, fabRadius + iconRadius, fabRadius + iconRadius);
        target.getOverlay().add(mFabIconDrawable);
        ObjectAnimator fadeIcon = ObjectAnimator.ofInt(mFabIconDrawable, "alpha", 255, 0).setDuration(getDuration() / 2);
        fadeIcon.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                target.getOverlay().clear();
            }
        });
        MorphDrawable background = new MorphDrawable(startColor, startCornerRadius);
        target.setBackground(background);
        Animator color = ObjectAnimator.ofArgb(background, MorphDrawable.COLOR, endColor).setDuration(1000);
        Animator corners = ObjectAnimator.ofFloat(background, MorphDrawable.CORNER_RADIUS,
                endCornerRadius);
     /*   // ease in the dialog's child views (slide up & fade in)
        if (endValues.view instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) endValues.view;
            float offset = vg.getHeight() / 4f;
            for (int i = 0; i < vg.getChildCount(); i++) {
                View v = vg.getChildAt(i);
                //v.setTranslationY(offset);
                v.setAlpha(0f);
                v.animate()
                        .alpha(1f)
                        // .translationY(0f)
                        .setDuration(150)
                        .setStartDelay(180)
                        .setInterpolator(AnimUtils.getFastOutSlowInInterpolator());
                offset *= 1.5f;
            }
        }*/
        AnimatorSet transition = new AnimatorSet();
        transition.playTogether(corners, color, fadeIcon);
        return transition;
    }

}
