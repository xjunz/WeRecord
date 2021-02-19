/*
 * Copyright (c) 2021 xjunz. 保留所有权利
 */
package xjunz.tool.werecord.ui.transition;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Path;
import android.transition.TransitionValues;
import android.transition.Visibility;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnticipateInterpolator;
import android.view.animation.OvershootInterpolator;

/**
 * @author xjunz 2021/1/24 19:11
 */
public class Pop extends Visibility {
    public Pop() {
        super();
    }

    public Pop(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public Animator onAppear(ViewGroup sceneRoot, View view, TransitionValues startValues, TransitionValues endValues) {
        Path path = new Path();
        path.moveTo(0, 0);
        path.lineTo(1, 1);
        Animator popIn = ObjectAnimator.ofFloat(view, View.SCALE_X, View.SCALE_Y, path);
        popIn.setInterpolator(new OvershootInterpolator());
        return popIn;
    }

    @Override
    public Animator onDisappear(ViewGroup sceneRoot, View view, TransitionValues startValues, TransitionValues endValues) {
        Path path = new Path();
        path.moveTo(1, 1);
        path.lineTo(0, 0);
        Animator popOut = ObjectAnimator.ofFloat(view, View.SCALE_X, View.SCALE_Y, path);
        popOut.setInterpolator(new AnticipateInterpolator());
        return popOut;
    }
}
