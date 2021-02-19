/*
 * Copyright (c) 2021 xjunz. 保留所有权利
 */
package xjunz.tool.werecord.data.databinding;

import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.widget.TextView;

import androidx.databinding.BindingAdapter;

import org.jetbrains.annotations.NotNull;

import xjunz.tool.werecord.App;
import xjunz.tool.werecord.R;
import xjunz.tool.werecord.util.UiUtils;


public class EditorActivityBindingAdapter {
    @BindingAdapter("android:timeSpan")
    public static void setTimeSpan(@NotNull TextView textView, boolean setSpan) {
        String text = textView.getText().toString();
    }

    @BindingAdapter(value = {"android:changed", "android:text"})
    public static void setChanged(@NotNull TextView textView, boolean changed, @NotNull String text) {
        String suffix = App.getStringOf(R.string.bracketed_modified);
        int index = text.indexOf(suffix);
        if (changed) {
            if (index < 0) {
                ForegroundColorSpan foregroundColorSpan = new ForegroundColorSpan(UiUtils.getTextColorSecondary());
                SpannableString span = new SpannableString(text + suffix);
                span.setSpan(foregroundColorSpan, text.length(), text.length() + suffix.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                textView.setText(span);
            } else {
                textView.setText(text);
            }
        } else {
            if (index >= 0) {
                textView.setText(text.substring(0, index));
            } else {
                textView.setText(text);
            }
        }
    }
}
