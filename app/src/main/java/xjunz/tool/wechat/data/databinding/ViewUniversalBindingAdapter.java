/*
 * Copyright (c) 2020 xjunz. 保留所有权利
 */
package xjunz.tool.wechat.data.databinding;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.appcompat.widget.TooltipCompat;
import androidx.databinding.BindingAdapter;
import androidx.databinding.BindingMethod;
import androidx.databinding.BindingMethods;
import androidx.databinding.InverseBindingAdapter;
import androidx.databinding.InverseBindingListener;
import androidx.databinding.adapters.ListenerUtil;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;

import xjunz.tool.wechat.R;
import xjunz.tool.wechat.util.UiUtils;

@BindingMethods({
        @BindingMethod(type = View.class, attribute = "android:onClick2", method = "setOnClickListener")
})
public class ViewUniversalBindingAdapter {
    @BindingAdapter({"android:visible"})
    public static void setVisible(@NotNull View view, boolean isVisible) {
        view.setVisibility(isVisible ? View.VISIBLE : View.GONE);
    }

    @BindingAdapter({"android:invisible"})
    public static void setInvisible(@NotNull View view, boolean invisible) {
        view.setVisibility(invisible ? View.INVISIBLE : View.VISIBLE);
    }

    @BindingAdapter({"android:gone"})
    public static void setGone(@NotNull View view, boolean gone) {
        view.setVisibility(gone ? View.GONE : View.VISIBLE);
    }

    @BindingAdapter("android:tooltip")
    public static void setTooltip(View view, String tooltipText) {
        TooltipCompat.setTooltipText(view, tooltipText);
    }

    @BindingAdapter("android:width")
    public static void setLayoutWidth(@NotNull View view, float width) {
        ViewGroup.LayoutParams lp = view.getLayoutParams();
        lp.width = (int) (width + 0.5);
        view.setLayoutParams(lp);
    }

    @BindingAdapter("android:fade")
    public static void setFade(View view, boolean fadeIn) {
        if (fadeIn) {
            UiUtils.fadeIn(view);
        } else {
            UiUtils.fadeOut(view);
        }
    }

    @BindingAdapter("android:help")
    public static void setHelp(@NotNull View view, String helpText) {
        view.setOnClickListener(v -> UiUtils.createHelp(view.getContext(), helpText).show());
    }

    @BindingAdapter("android:error")
    public static void setError(@NotNull EditText editText, String errorText) {
        editText.setError(errorText);
    }

    @BindingAdapter(value = "android:marginTop")
    public static void setMarginTop(@NotNull View view, float marginTop) {
        ViewGroup.LayoutParams lp = view.getLayoutParams();
        try {
            Field field = lp.getClass().getField("topMargin");
            field.set(lp, (int) marginTop);
            view.setLayoutParams(lp);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    @BindingAdapter(value = "android:marginBottom")
    public static void setMarginBottom(@NotNull View view, float marginTop) {
        ViewGroup.LayoutParams lp = view.getLayoutParams();
        try {
            Field field = lp.getClass().getField("bottomMargin");
            field.set(lp, (int) marginTop);
            view.setLayoutParams(lp);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Praise Google
     * <href a="https://android.jlelse.eu/things-that-youll-know-about-databinding-in-future-1cfe62cf8a10"/>
     */
    @InverseBindingAdapter(attribute = "android:height")
    public static int getHeight(@NotNull View view) {
        return view.getHeight();
    }

    @BindingAdapter(value = {"android:heightAttrChanged"})
    public static void setHeightWatcher(View view, final InverseBindingListener listener) {
        View.OnLayoutChangeListener newValue = (v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            boolean isChanged = left != oldLeft || right != oldRight || top != oldTop || bottom != oldBottom;
            if (isChanged)
                listener.onChange();
        };
        View.OnLayoutChangeListener oldValue = ListenerUtil.trackListener(view, newValue, R.id.onHeightChanged);
        if (oldValue != null) {
            view.removeOnLayoutChangeListener(oldValue);
        }
        view.addOnLayoutChangeListener(newValue);
    }

    @InverseBindingAdapter(attribute = "android:width")
    public static int getWidth(@NotNull View view) {
        return view.getWidth();
    }

    @BindingAdapter("android:widthAttrChanged")
    public static void setWidthWatcher(View view, final InverseBindingListener listener) {
        View.OnLayoutChangeListener newValue = (v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            boolean isChanged = left != oldLeft || right != oldRight || top != oldTop || bottom != oldBottom;
            if (isChanged)
                listener.onChange();
        };
        View.OnLayoutChangeListener oldValue = ListenerUtil.trackListener(view, newValue, R.id.onWidthChanged);
        if (oldValue != null) {
            view.removeOnLayoutChangeListener(oldValue);
        }
        view.addOnLayoutChangeListener(newValue);
    }

    /**
     * 为{@link View}设置第二个点击事件，第二个点击事件会
     * 在第一个点击事件执行后执行。此属性作用在于变相地允许
     * {@code onClick}的{@code Lambda}语句的方法体内使用两行代码
     *
     * @param view             指定View
     * @param onClickListener  先执行的事件
     * @param onClickListener2 后执行的事件
     */
    @BindingAdapter(value = {"android:onClick", "android:onClick2"})
    public static void setOnClick2(@NotNull View view, View.OnClickListener onClickListener, View.OnClickListener onClickListener2) {
        view.setOnClickListener(v -> {
            if (onClickListener != null) {
                onClickListener.onClick(v);
            }
            if (onClickListener2 != null) {
                onClickListener2.onClick(v);
            }
        });
    }

    @BindingAdapter(value = {"android:rememberSelection"})
    public static void setRememberSelection(EditText editText, boolean remember) {
        if (remember) {
            TextWatcher newValue = new TextWatcher() {
                int startSel, endSel;

                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    startSel = editText.getSelectionStart();
                    endSel = editText.getSelectionEnd();
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(@NotNull Editable s) {
                    if (endSel <= s.length()) {
                        editText.setSelection(startSel, endSel);
                    }
                }
            };
            TextWatcher oldValue = ListenerUtil.trackListener(editText, newValue, R.id.rememberSelection);
            if (oldValue != null) {
                editText.removeTextChangedListener(oldValue);
            }
            editText.addTextChangedListener(newValue);
        } else {
            TextWatcher oldValue = ListenerUtil.getListener(editText, R.id.rememberSelection);
            editText.removeTextChangedListener(oldValue);
        }
    }

}
