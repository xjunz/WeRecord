/*
 * Copyright (c) 2021 xjunz. 保留所有权利
 */
package xjunz.tool.werecord.ui.transition;


import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.RectEvaluator;
import android.app.Activity;
import android.app.SharedElementCallback;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.transition.Transition;
import android.transition.TransitionValues;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.ColorInt;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

import xjunz.tool.werecord.R;
import xjunz.tool.werecord.util.ActivityUtils;
import xjunz.tool.werecord.util.AnimUtils;

/**
 * @author xjunz 2021/2/4 17:27
 */
public class RectangularContainerTransform extends Transition {
    private View mSnapshotView;
    private boolean mIsEntering;
    private static final String PROP_NAME_POSITION_IN_SCREEN = "xjunz:RectangularContainerTransform:positionInScreen";
    private static final String PROP_NAME_RECT = "xjunz:RectangularContainerTransform:rect";
    private long mBaseDuration;
    @ColorInt
    private int mStartColor;

    public RectangularContainerTransform(Context context) {
        super();
        init(context);
    }

    public RectangularContainerTransform(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.Morph);
        mStartColor = ta.getColor(R.styleable.Morph_startColor, Color.TRANSPARENT);
        ta.recycle();
        init(context);
    }

    private void init(Context context) {
        mIsEntering = true;
        Activity activity = ActivityUtils.getHostActivity(context);
        activity.setEnterSharedElementCallback(new SharedElementCallbackImpl());
        mBaseDuration = getDuration();
        setDuration(-1L);
    }


    @Override
    public void captureStartValues(TransitionValues transitionValues) {
        captureValues(transitionValues);
    }

    @Override
    public void captureEndValues(TransitionValues transitionValues) {
        captureValues(transitionValues);
    }

    private void captureValues(@NotNull TransitionValues values) {
        int[] position = new int[2];
        View view = values.view;
        view.getLocationOnScreen(position);
        values.values.put(PROP_NAME_POSITION_IN_SCREEN, position);
        values.values.put(PROP_NAME_RECT, new Rect(view.getLeft(), view.getTop(), view.getRight(), view.getBottom()));
    }

    @Override
    public Animator createAnimator(ViewGroup sceneRoot, TransitionValues startValues, TransitionValues endValues) {
        if (startValues == null || endValues == null) {
            return null;
        }
        if (mSnapshotView == null) {
            return null;
        }
        View target = endValues.view;
        Rect startRect = (Rect) startValues.values.get(PROP_NAME_RECT);
        Rect endRect = (Rect) endValues.values.get(PROP_NAME_RECT);
        int[] startPosition = (int[]) startValues.values.get(PROP_NAME_POSITION_IN_SCREEN);
        int[] endPosition = (int[]) endValues.values.get(PROP_NAME_POSITION_IN_SCREEN);
        if (startRect == null || endRect == null || startPosition == null || endPosition == null) {
            return null;
        }
        RectEvaluator evaluator = new RectEvaluator(new Rect());
        Animator clipBounds, fadeOverlay;
        Animator fadeBackground = null;
        if (mIsEntering) {
            int relativeLeft = startPosition[0] - endPosition[0];
            int relativeTop = startPosition[1] - endPosition[1];
            Rect relativeStartRect = new Rect(relativeLeft, relativeTop, relativeLeft + startRect.width(), relativeTop + startRect.height());
            Rect relativeEndRect = new Rect(0, 0, endRect.width(), endRect.height());
            sceneRoot.getOverlay().add(mSnapshotView);
            clipBounds = ObjectAnimator.ofObject(endValues.view, "clipBounds", evaluator, relativeStartRect, relativeEndRect).setDuration(mBaseDuration);
            fadeOverlay = ObjectAnimator.ofFloat(mSnapshotView, View.ALPHA, 1, 0).setDuration(mBaseDuration / 2);
        } else {
            int relativeLeft = endPosition[0] - startPosition[0];
            int relativeTop = endPosition[1] - startPosition[1];
            target.layout(startRect.left, startRect.top, startRect.right, startRect.bottom);
            sceneRoot.getOverlay().add(mSnapshotView);
            Rect relativeStartRect = new Rect(0, 0, startRect.width(), startRect.height());
            Rect relativeEndRect = new Rect(relativeLeft, relativeTop, relativeLeft + endRect.width(), relativeTop + endRect.height());
            clipBounds = ObjectAnimator.ofObject(endValues.view, "clipBounds", evaluator, relativeStartRect, relativeEndRect).setDuration(mBaseDuration);
            fadeOverlay = ObjectAnimator.ofFloat(mSnapshotView, View.ALPHA, 0, 1).setDuration(mBaseDuration);
            if (mStartColor != Color.TRANSPARENT) {
                ColorDrawable background = new ColorDrawable(mStartColor);
                background.setBounds(startRect);
                target.getOverlay().add(background);
                fadeBackground = ObjectAnimator.ofInt(background, "alpha", 0, 255);
            }
        }
        AnimatorSet set = new AnimatorSet();
        set.setInterpolator(AnimUtils.getFastOutSlowInInterpolator());
        AnimatorSet.Builder builder = set.play(clipBounds).with(fadeOverlay);
        if (fadeBackground != null) {
            builder.with(fadeBackground);
        }
        return set;
    }

    private class SharedElementCallbackImpl extends SharedElementCallback {
        @Override
        public void onMapSharedElements(List<String> names, Map<String, View> sharedElements) {
            super.onMapSharedElements(names, sharedElements);
        }

        @Override
        public void onSharedElementStart(List<String> sharedElementNames, List<View> sharedElements, List<View> sharedElementSnapshots) {
            super.onSharedElementStart(sharedElementNames, sharedElements, sharedElementSnapshots);
            if (!sharedElements.isEmpty() && !sharedElementSnapshots.isEmpty()) {
                //Expect there is only one Shared Element View.
                mSnapshotView = sharedElementSnapshots.get(0);
            }
        }

        @Override
        public void onSharedElementEnd(List<String> sharedElementNames, List<View> sharedElements, List<View> sharedElementSnapshots) {
            super.onSharedElementEnd(sharedElementNames, sharedElements, sharedElementSnapshots);
            mIsEntering = false;
        }
    }

}
