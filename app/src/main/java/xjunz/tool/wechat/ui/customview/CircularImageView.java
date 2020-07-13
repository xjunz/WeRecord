/*
 * Copyright (c) 2020 xjunz. 保留所有权利
 */

package xjunz.tool.wechat.ui.customview;

import android.content.Context;
import android.graphics.Outline;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewOutlineProvider;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;

public class CircularImageView extends AppCompatImageView {
    public CircularImageView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setOval(getPaddingLeft(), getPaddingTop(), getWidth() - getPaddingLeft(), getHeight() - getPaddingTop());
            }
        });
        setClipToOutline(true);
    }
}
