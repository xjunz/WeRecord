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
import android.graphics.Outline;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.transition.Transition;
import android.transition.TransitionValues;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;

import androidx.annotation.ColorInt;

import com.google.android.material.transition.MaterialArcMotion;

import org.jetbrains.annotations.NotNull;

import xjunz.tool.werecord.R;
import xjunz.tool.werecord.util.AnimUtils;

/**
 * 一个用于{@code Floating Action Button}和其他矩形UI的共享元素{@link Transition}。
 * 构造此Transition需要传入FAB的图标{@link Drawable}, FAB的背景颜色。
 * <p>
 * 实现方式为在{@link android.view.ViewOverlay}中构造一个假的FAB，并使用{@link ViewAnimationUtils#createCircularReveal(View, int, int, float, float)}
 * 执行揭示动画。
 *
 * @author xjunz 2021/1/24 23:30
 * @see R.styleable#Morph_fabIcon
 * @see R.styleable#Morph_startColor
 */
public class RevealTransform extends Transition {
    private Drawable mFabIconDrawable;
    @ColorInt
    private final int mFabColor;
    private static final String PROP_NAME_RECT = "xjunz:RevealTransform:rect";
    private static final String[] PROPERTIES = new String[]{
            PROP_NAME_RECT
    };
    /**
     * 最后一帧是否移除{@link android.view.ViewOverlay}中的{@link Drawable}, 可以防止
     * {@code Activity Shared Element Return Transition}最后一帧视图会一闪复原的情况。
     */
    private final boolean mHoldAtEnd;

    @Override
    public String[] getTransitionProperties() {
        return PROPERTIES;
    }

    public RevealTransform(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.Morph);
        mFabColor = typedArray.getColor(R.styleable.Morph_startColor, Color.TRANSPARENT);
        if (typedArray.hasValue(R.styleable.Morph_fabIcon)) {
            mFabIconDrawable = typedArray.getDrawable(R.styleable.Morph_fabIcon);
            mFabIconDrawable.setTint(typedArray.getColor(R.styleable.Morph_fabIconTint, Color.WHITE));
        }
        mHoldAtEnd = typedArray.getBoolean(R.styleable.Morph_holdAtEnd, false);
        setDuration(-1);
        typedArray.recycle();
    }

    @Override
    public void captureStartValues(TransitionValues transitionValues) {
        captureValues(transitionValues);
    }

    @Override
    public void captureEndValues(TransitionValues transitionValues) {
        captureValues(transitionValues);
    }

    private void captureValues(@NotNull TransitionValues transitionValues) {
        View view = transitionValues.view;
        transitionValues.values.put(PROP_NAME_RECT, new Rect(view.getLeft(), view.getTop(), view.getRight(), view.getBottom()));
    }

    @Override
    public Animator createAnimator(ViewGroup sceneRoot, TransitionValues startValues, TransitionValues endValues) {
        if (startValues == null || endValues == null) {
            return null;
        }
        Rect startRect = (Rect) startValues.values.get(PROP_NAME_RECT);
        Rect endRect = (Rect) endValues.values.get(PROP_NAME_RECT);
        boolean fromFab = startRect.width() < endRect.width();
        View target = endValues.view;
        target.getOverlay().clear();
        if (!fromFab) {
            target.measure(View.MeasureSpec.makeMeasureSpec(startRect.width(), View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(startRect.height(), View.MeasureSpec.EXACTLY));
            target.layout(startRect.left, startRect.top, startRect.right, startRect.bottom);
        }
        Animator reveal;
        //揭示动画
        if (fromFab) {
            float endRadius = (float) Math.hypot(endRect.width() / 2f, endRect.height() / 2f);
            reveal = ViewAnimationUtils.createCircularReveal(target, endRect.width() / 2, endRect.height() / 2
                    , startRect.width() / 2f, endRadius);
            reveal.setInterpolator(AnimUtils.getFastOutLinearInInterpolator());
        } else {
            float startRadius = (float) Math.hypot(startRect.width() / 2f, startRect.height() / 2f);
            reveal = ViewAnimationUtils.createCircularReveal(target, startRect.width() / 2, startRect.height() / 2
                    , startRadius, endRect.width() / 2f);
            reveal.setInterpolator(AnimUtils.getLinearOutSlowInInterpolator());
            reveal.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    if (mHoldAtEnd) {
                        target.setOutlineProvider(new ViewOutlineProvider() {
                            @Override
                            public void getOutline(View view, Outline outline) {
                                final int left = (view.getWidth() - endRect.width()) / 2;
                                final int top = (view.getHeight() - endRect.height()) / 2;
                                outline.setOval(left, top, left + endRect.width(), top + endRect.height());
                                view.setClipToOutline(true);
                            }
                        });
                    }
                }
            });
        }
        //位移动画
        Animator translate;
        int transX = startRect.centerX() - endRect.width() / 2 - endRect.left;
        int transY = startRect.centerY() - endRect.height() / 2 - endRect.top;
        if (fromFab) {
            translate = ObjectAnimator.ofFloat(target, View.TRANSLATION_X, View.TRANSLATION_Y,
                    new MaterialArcMotion().getPath(transX, transY, 0, 0));
        } else {
            translate = ObjectAnimator.ofFloat(target, View.TRANSLATION_X, View.TRANSLATION_Y,
                    new MaterialArcMotion().getPath(0, 0, -transX, -transY));
        }
        translate.setInterpolator(AnimUtils.getFastOutSlowInInterpolator());
        //FAB背景色遮罩
        ColorDrawable fabColorDrawable = new ColorDrawable(mFabColor);
        fabColorDrawable.setBounds(0, 0, fromFab ? endRect.width() : startRect.width(), fromFab ? endRect.height() : startRect.height());
        target.getOverlay().add(fabColorDrawable);
        Animator fadeFabColor = ObjectAnimator.ofInt(fabColorDrawable, "alpha", fromFab ? 255 : 0, fromFab ? 0 : 255);
        fadeFabColor.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (!fromFab && mHoldAtEnd) {
                    return;
                }
                target.getOverlay().remove(fabColorDrawable);
            }
        });
        //FAB图标
        Drawable fabIcon = mFabIconDrawable.mutate();
        int iconRadius = fabIcon.getIntrinsicWidth() / 2;
        int halfWidth = fromFab ? endRect.width() / 2 : startRect.width() / 2;
        int halfHeight = fromFab ? endRect.height() / 2 : startRect.height() / 2;
        fabIcon.setBounds(halfWidth - iconRadius, halfHeight - iconRadius, halfWidth + iconRadius, halfHeight + iconRadius);
        target.getOverlay().add(fabIcon);
        Animator fadeIcon = ObjectAnimator.ofInt(fabIcon, "alpha", fromFab ? 255 : 0, fromFab ? 0 : 255);
        fadeIcon.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (!fromFab && mHoldAtEnd) {
                    return;
                }
                target.getOverlay().remove(mFabIconDrawable);
            }
        });
        AnimatorSet set = new AnimatorSet();
        set.playTogether(reveal, translate, fadeFabColor, fadeIcon);
        return set;
    }
}
