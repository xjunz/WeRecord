/*
 * Copyright (c) 2021 xjunz. 保留所有权利
 */

package xjunz.tool.werecord.ui.databinding;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.databinding.BindingAdapter;

import de.hdodenhof.circleimageview.CircleImageView;
import xjunz.tool.werecord.R;
import xjunz.tool.werecord.util.UiUtils;
import xjunz.tool.werecord.util.Utils;

public class DetailActivityBindingAdapter {

    @BindingAdapter(value = "android:src")
    public static void setSrc(CircleImageView imageView, @Nullable Bitmap bitmap) {
        if (bitmap == null) {
            imageView.setImageResource(R.mipmap.avatar_default);
        } else {
            imageView.setImageBitmap(bitmap);
        }
    }


    @BindingAdapter("android:copyable")
    public static void setCopyable(TextView textView, boolean isCopyable) {
        if (isCopyable) {
            Drawable drawable = textView.getContext().getTheme().getDrawable(R.drawable.bg_clickable_text).mutate();
            textView.setClickable(true);
            textView.setFocusable(true);
            textView.setBackground(drawable);
            textView.setOnClickListener(v -> {
                Utils.copyPlainText("From WeRecord", textView.getText());
                UiUtils.toast(R.string.has_copied_to_clipboard);
            });
        }
    }
}
