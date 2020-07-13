/*
 * Copyright (c) 2020 xjunz. 保留所有权利
 */
package xjunz.tool.wechat.data.databinding;

import android.view.View;

import androidx.appcompat.widget.TooltipCompat;
import androidx.databinding.BindingAdapter;

public class ViewUniversalBindingAdapter {
    @BindingAdapter("android:visible")
    public static void setVisible(View view, boolean isVisible) {
        view.setVisibility(isVisible ? View.VISIBLE : View.GONE);
    }

    @BindingAdapter("android:tooltip")
    public static void setTooltip(View view, String tooltipText) {
        TooltipCompat.setTooltipText(view, tooltipText);
    }
}
