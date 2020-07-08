package xjunz.tool.wechat.data.databinding;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.databinding.BindingAdapter;

import de.hdodenhof.circleimageview.CircleImageView;
import xjunz.tool.wechat.R;
import xjunz.tool.wechat.ui.customview.MasterToast;
import xjunz.tool.wechat.util.UniUtils;

public class DetailActivityBindingAdapter {
    @BindingAdapter(value = "android:src")
    public static void setSrc(CircleImageView imageView, @Nullable Bitmap bitmap) {
        if (bitmap == null) {
            imageView.setImageResource(R.mipmap.avatar_default);
        } else {
            imageView.setImageBitmap(bitmap);
        }
    }

    @BindingAdapter("android:visible")
    public static void setVisible(View view, boolean isVisible) {
        view.setVisibility(isVisible ? View.VISIBLE : View.GONE);
    }

    @BindingAdapter("android:copyable")
    public static void setCopyable(TextView textView, boolean isCopyable) {
        Drawable drawable = textView.getContext().getTheme().getDrawable(R.drawable.bg_clickable_text).mutate();
        if (isCopyable) {
            textView.setClickable(true);
            textView.setFocusable(true);
            textView.setBackground(drawable);
            textView.setOnClickListener(v -> {
                UniUtils.copyPlainText("Contact info", textView.getText());
                MasterToast.shortToast("已复制到剪贴板");
            });
        }
    }
}
