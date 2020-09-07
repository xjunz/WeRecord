/*
 * Copyright (c) 2020 xjunz. 保留所有权利
 */
package xjunz.tool.wechat.data.databinding;

import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.widget.TextView;

import androidx.databinding.BindingAdapter;

import org.jetbrains.annotations.NotNull;

import xjunz.tool.wechat.App;
import xjunz.tool.wechat.R;
import xjunz.tool.wechat.util.UiUtils;


public class EditorActivityBindingAdapter {
    @BindingAdapter("android:timeSpan")
    public static void setTimeSpan(@NotNull TextView textView, boolean setSpan) {
        String text = textView.getText().toString();
    }

    @BindingAdapter("android:changed")
    public static void setChanged(@NotNull TextView textView, boolean changed) {
        String text = textView.getText().toString();
        String suffix = App.getStringOf(R.string.bracketed_modified);
        int index = text.indexOf(suffix);
        if (changed) {
            if (index < 0) {
                ForegroundColorSpan foregroundColorSpan = new ForegroundColorSpan(UiUtils.getTextColorSecondary());
                SpannableString span = new SpannableString(text + suffix);
                span.setSpan(foregroundColorSpan, text.length(), text.length() + suffix.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                textView.setText(span);
            }
        } else {
            if (index >= 0) {
                textView.setText(text.substring(0, index));
            }
        }
    }
}
