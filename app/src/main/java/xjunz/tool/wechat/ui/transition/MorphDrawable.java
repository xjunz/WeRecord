/*
 * Copyright (c) 2020 xjunz. 保留所有权利
 */

package xjunz.tool.wechat.ui.transition;

import android.annotation.TargetApi;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.Property;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

import xjunz.tool.wechat.util.AnimUtils;

/**
 * A drawable that can morph size, shape (via it's corner radius) and color.  Specifically this is
 * useful for animating between a FAB and a dialog.
 */
public class MorphDrawable extends Drawable {

    static final Property<MorphDrawable, Float> CORNER_RADIUS = new AnimUtils
            .FloatProperty<MorphDrawable>("cornerRadius") {

        @Override
        public void setValue(MorphDrawable morphDrawable, float value) {
            morphDrawable.setCornerRadius(value);
        }

        @Override
        public Float get(MorphDrawable morphDrawable) {
            return morphDrawable.getCornerRadius();
        }
    };
    static final Property<MorphDrawable, Integer> COLOR = new AnimUtils
            .IntProperty<MorphDrawable>("color") {

        @Override
        public void setValue(MorphDrawable morphDrawable, int value) {
            morphDrawable.setColor(value);
        }

        @Override
        public Integer get(MorphDrawable morphDrawable) {
            return morphDrawable.getColor();
        }
    };
    private float cornerRadius;
    private Paint paint;

    public MorphDrawable(@ColorInt int color, float cornerRadius) {
        this.cornerRadius = cornerRadius;
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(color);
    }

    private float getCornerRadius() {
        return cornerRadius;
    }

    private void setCornerRadius(float cornerRadius) {
        this.cornerRadius = cornerRadius;
        invalidateSelf();
    }

    public int getColor() {
        return paint.getColor();
    }

    public void setColor(int color) {
        paint.setColor(color);
        invalidateSelf();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void draw(@NonNull Canvas canvas) {
        canvas.drawRoundRect(getBounds().left, getBounds().top, getBounds().right, getBounds()
                .bottom, cornerRadius, cornerRadius, paint);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void getOutline(@NonNull Outline outline) {
        outline.setRoundRect(getBounds(), cornerRadius);
    }

    @Override
    public void setAlpha(int alpha) {
        paint.setAlpha(alpha);
        invalidateSelf();
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        paint.setColorFilter(cf);
        invalidateSelf();
    }

    @Override
    public int getOpacity() {
        return paint.getAlpha();
    }

}
