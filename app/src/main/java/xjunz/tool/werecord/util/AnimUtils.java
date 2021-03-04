/*
 * Copyright (c) 2021 xjunz. 保留所有权利
 */

package xjunz.tool.werecord.util;

import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;

import static xjunz.tool.werecord.App.getContext;

/**
 * Utility methods for working with animations.
 */
public class AnimUtils {

    private AnimUtils() {
    }

    private static Interpolator fastOutSlowIn;
    private static Interpolator fastOutLinearIn;
    private static Interpolator linearOutSlowIn;

    public static Interpolator getFastOutSlowInInterpolator() {
        if (fastOutSlowIn == null) {
            fastOutSlowIn = AnimationUtils.loadInterpolator(getContext(),
                    android.R.interpolator.fast_out_slow_in);
        }
        return fastOutSlowIn;
    }

    public static Interpolator getFastOutLinearInInterpolator() {
        if (fastOutLinearIn == null) {
            fastOutLinearIn = AnimationUtils.loadInterpolator(getContext(),
                    android.R.interpolator.fast_out_linear_in);
        }
        return fastOutLinearIn;
    }

    public static Interpolator getLinearOutSlowInInterpolator() {
        if (linearOutSlowIn == null) {
            linearOutSlowIn = AnimationUtils.loadInterpolator(getContext(),
                    android.R.interpolator.linear_out_slow_in);
        }
        return linearOutSlowIn;
    }

}
