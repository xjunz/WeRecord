/*
 * Copyright (c) 2020 xjunz. 保留所有权利
 */
package xjunz.tool.wechat.ui.transition;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.RectEvaluator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.transition.Transition;
import android.transition.TransitionValues;
import android.util.AttributeSet;
import android.util.Property;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.Button;

import androidx.annotation.ColorLong;

import org.jetbrains.annotations.NotNull;

import xjunz.tool.wechat.R;

/**
 * 一个通用的从{@link Button}到其他矩形UI的{@code Shared Element}{@link Transition}动画
 *
 * <p>实现方式为通过在XML中定义{@link Button}的各个外观属性（保证和原{@link Button}外观一致即可），
 * 利用这些属性渲染出一个假的{@link Button}，然后获取该{@link Button}的{@code drawingCache}，并添加到
 * {@code overlay}中，最后执行各种动画实现过渡。其他细节查看代码。</p>
 * <p>
 * {@link Button}的各个属性会从{@code transition}节点下获取，所以需要在{@code transition}节点下定义
 * {@link Button}的各个属性。
 * }
 *
 * @author xjunz 2020/7/15 2:41
 */
public class GenericButtonMorph extends Transition {
    /**
     * 储存和获取初始及结束视图的{@link Rect}的键值
     *
     * @see GenericButtonMorph#captureValues(TransitionValues)
     */
    private static final String PROPNAME_RECT = "GenericButtonMorph:rect";
    private static final String[] PROPERTIES = new String[]{
            PROPNAME_RECT
    };
    /**
     * 构造的和初始{@link Button}外观一致的假Button
     */
    private final Button mFakeButton;
    /**
     * 初始{@link Button}的视觉上的背景色，即实际看到的Button的颜色。比如
     * 当Button的{@link Button#getBackground()}返回的背景是透明时，它的视觉上
     * 的背景色就应当是其父布局的背景色（或是其他可能的情况），此时该属性
     * 就应当设置为相应的颜色而不是透明。
     * 这个颜色会用于使模拟的假Button更加逼真以及构造过渡动画。
     *
     * <p>
     * 如果此属性未被设置，其默认值为透明，这可能会导致过渡动画的不和谐。
     *
     * @see GenericButtonMorph#createAnimator(ViewGroup, TransitionValues, TransitionValues)
     * @see R.styleable#Transform_opticalButtonColor
     */
    @ColorLong
    private final int mOpticalButtonColor;

    public GenericButtonMorph(Context context, AttributeSet attrs) {
        super(context, attrs);
        //从transition节点获取button的属性并构造之
        mFakeButton = new Button(context, attrs);
        //获取opticalButtonColor
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.Transform);
        mOpticalButtonColor = ta.getColor(R.styleable.Transform_opticalButtonColor, Color.TRANSPARENT);
        ta.recycle();
    }


    private void captureValues(@NotNull TransitionValues transitionValues) {
        View view = transitionValues.view;
        transitionValues.values.put(PROPNAME_RECT, new Rect(view.getLeft(), view.getTop(), view.getRight(), view.getBottom()));
    }


    @Override
    public void captureStartValues(TransitionValues transitionValues) {
        captureValues(transitionValues);
    }

    @Override
    public void captureEndValues(TransitionValues transitionValues) {
        captureValues(transitionValues);
    }

    @Override
    public String[] getTransitionProperties() {
        return PROPERTIES;
    }


    /**
     * 自定义{@link Property}，通过{@link View#getClipBounds()}和{@link View#setClipBounds(Rect)}
     * 实现{@code clipBounds}动画。
     */
    private static final Property<View, Rect> CLIP_BOUNDS = new Property<View, Rect>(Rect.class, "clipBounds") {
        @Override
        public Rect get(@NotNull View object) {
            return object.getClipBounds();
        }

        @Override
        public void set(@NotNull View object, Rect value) {
            object.setClipBounds(value);
        }
    };


    @Override
    public Animator createAnimator(ViewGroup sceneRoot, TransitionValues
            startValues, TransitionValues endValues) {
        if (startValues == null || endValues == null) {
            return null;
        }
        Rect startRect = (Rect) startValues.values.get(PROPNAME_RECT);
        Rect endRect = (Rect) endValues.values.get(PROPNAME_RECT);

        if (endRect == null || startRect == null) {
            return null;
        }

        boolean fromLarger = endRect.width() < startRect.width();
        View target = endValues.view;
        Rect smallerRect = fromLarger ? endRect : startRect;
        Rect largerRect = fromLarger ? startRect : endRect;
        //从大视图过渡为小视图之前，Transition框架会将当前较大视图layout为较小视图用以captureEndValues
        //为了执行clipBounds，我们将其恢复layout为原来大小
        if (fromLarger) {
            target.layout(largerRect.left, largerRect.top, largerRect.right, largerRect.bottom);
        }
        mFakeButton.measure(View.MeasureSpec.makeMeasureSpec(smallerRect.width(), View.MeasureSpec.EXACTLY)
                , View.MeasureSpec.makeMeasureSpec(smallerRect.height(), View.MeasureSpec.EXACTLY));
        mFakeButton.layout(0, 0, mFakeButton.getMeasuredWidth(), mFakeButton.getMeasuredHeight());
        Bitmap bitmap = Bitmap.createBitmap(startRect.width(), startRect.height(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        mFakeButton.draw(canvas);
        Drawable overlayButton = new BitmapDrawable(bitmap);
        Drawable overlayBackColor = new ColorDrawable(mOpticalButtonColor);

        int left = (largerRect.width() - smallerRect.width()) / 2;
        int top = (largerRect.height() - smallerRect.height()) / 2;
        overlayButton.setBounds(left, top, left + startRect.width(), top + startRect.height());
        overlayBackColor.setBounds(0, 0, largerRect.width(), largerRect.height());
        target.getOverlay().add(overlayBackColor);
        target.getOverlay().add(overlayButton);

        long halfDuration = getDuration() / 2;
        ObjectAnimator fadeButton = ObjectAnimator.ofInt(overlayButton, "alpha", fromLarger ? new int[]{0, 255} : new int[]{255, 0}).setDuration(halfDuration);
        ObjectAnimator fadeButtonColor = ObjectAnimator.ofInt(overlayBackColor, "alpha", fromLarger ? new int[]{0, 255} : new int[]{255, 0}).setDuration(halfDuration);


        Rect rectSmall = new Rect(left, top, left + smallerRect.width(), top + smallerRect.height());
        Rect rectLarge = new Rect(0, 0, target.getWidth(), target.getHeight());
        ObjectAnimator changeClipBounds = ObjectAnimator.ofObject(target, CLIP_BOUNDS, new RectEvaluator(), !fromLarger ? new Rect[]{rectSmall
                , rectLarge} : new Rect[]{rectLarge, rectSmall});
        float transX = startRect.centerX() - endRect.centerX();
        float transY = startRect.centerY() - endRect.centerY();
        ObjectAnimator translate = ObjectAnimator.ofFloat(target, View.TRANSLATION_X, View.TRANSLATION_Y
                , fromLarger ? getPathMotion().getPath(0, 0, -transX, -transY) : getPathMotion().getPath(transX, transY, 0, 0));

        AnimatorSet set = new AnimatorSet();
        set.playTogether(changeClipBounds, translate, fadeButton, fadeButtonColor);
        set.setInterpolator(AnimationUtils.loadInterpolator(target.getContext(), android.R.interpolator.fast_out_slow_in));
        return set;

    }
}
