/*
 * Copyright (c) 2020 xjunz. 保留所有权利
 */

package xjunz.tool.wechat.ui.transition;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.Build;
import android.transition.ChangeBounds;
import android.transition.TransitionValues;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.ColorInt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import xjunz.tool.wechat.R;
import xjunz.tool.wechat.util.AnimUtils;


/**
 * A transition that morphs a circle into a rectangle, changing it's background color.
 * <p/>
 * Modified from Plaid
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class MorphFabToDialog extends ChangeBounds {
    private static final String PROPERTY_COLOR = "plaid:circleMorph:color";
    private static final String PROPERTY_CORNER_RADIUS = "plaid:circleMorph:cornerRadius";
    private static final String[] TRANSITION_PROPERTIES = {
            PROPERTY_COLOR,
            PROPERTY_CORNER_RADIUS
    };
    private
    @ColorInt
    int startColor = Color.TRANSPARENT;
    private int endCornerRadius;
    private int startCornerRadius;

    public MorphFabToDialog(@ColorInt int startColor, int endCornerRadius) {
        this(startColor, endCornerRadius, -1);
    }

    public MorphFabToDialog(@ColorInt int startColor, int endCornerRadius, int startCornerRadius) {
        super();
        setStartColor(startColor);
        setEndCornerRadius(endCornerRadius);
        setStartCornerRadius(startCornerRadius);
    }

    public MorphFabToDialog(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.FabTransform);
        startColor = typedArray.getColor(R.styleable.FabTransform_startColor, startColor);
        endCornerRadius = typedArray.getDimensionPixelSize(R.styleable.FabTransform_endCornerRadius, endCornerRadius);
        startCornerRadius = typedArray.getDimensionPixelSize(R.styleable.FabTransform_startCornerRadius, startCornerRadius);
        typedArray.recycle();
    }

    public void setStartColor(@ColorInt int startColor) {
        this.startColor = startColor;
    }

    public void setEndCornerRadius(int endCornerRadius) {
        this.endCornerRadius = endCornerRadius;
    }

    public void setStartCornerRadius(int startCornerRadius) {
        this.startCornerRadius = startCornerRadius;
    }

    @Override
    public String[] getTransitionProperties() {
        ArrayList<String> boundsTransitionProps = new ArrayList<>(Arrays.asList(super.getTransitionProperties()));
        Collections.addAll(boundsTransitionProps, TRANSITION_PROPERTIES);
        return boundsTransitionProps.toArray(new String[0]);
    }

    @Override
    public void captureStartValues(TransitionValues transitionValues) {
        super.captureStartValues(transitionValues);
        final View view = transitionValues.view;
        transitionValues.values.put(PROPERTY_COLOR, startColor);
        transitionValues.values.put(PROPERTY_CORNER_RADIUS,
                startCornerRadius > 0 ? startCornerRadius : view.getHeight() / 2);
    }

    @Override
    public void captureEndValues(TransitionValues transitionValues) {
        super.captureEndValues(transitionValues);
        transitionValues.values.put(PROPERTY_COLOR, Color.WHITE);
        transitionValues.values.put(PROPERTY_CORNER_RADIUS, endCornerRadius);
    }

    @Override
    public Animator createAnimator(final ViewGroup sceneRoot,
                                   TransitionValues startValues,
                                   final TransitionValues endValues) {
        Animator changeBounds = super.createAnimator(sceneRoot, startValues, endValues);
        if (startValues == null || endValues == null || changeBounds == null) {
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

        MorphDrawable background = new MorphDrawable(startColor, startCornerRadius);
        endValues.view.setBackground(background);
        Animator color = ObjectAnimator.ofArgb(background, MorphDrawable.COLOR, endColor);
        Animator corners = ObjectAnimator.ofFloat(background, MorphDrawable.CORNER_RADIUS,
                endCornerRadius);
        // ease in the dialog's child views (slide up & fade in)
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
        }

        AnimatorSet transition = new AnimatorSet();
        transition.playTogether(changeBounds, corners, color);
        /// transition.setInterpolator(AnimUtils.getFastOutSlowInInterpolator());
        return transition;
    }

}
