/*
 * Copyright (c) 2021 xjunz. 保留所有权利
 */
package xjunz.tool.werecord.ui.databinding;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.text.InputFilter;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.TooltipCompat;
import androidx.databinding.BindingAdapter;
import androidx.databinding.BindingMethod;
import androidx.databinding.BindingMethods;
import androidx.databinding.InverseBindingAdapter;
import androidx.databinding.InverseBindingListener;
import androidx.databinding.adapters.ListenerUtil;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import xjunz.tool.werecord.R;
import xjunz.tool.werecord.util.UiUtils;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

/**
 * 定义各种视图通用的{@link BindingAdapter}的工具类
 */
@BindingMethods({
        @BindingMethod(type = View.class, attribute = "android:onClick2", method = "setOnClickListener")
})
public class UniversalBindingAdapter {
    @BindingAdapter("android:visible")
    public static void setVisible(@NotNull View view, boolean isVisible) {
        view.setVisibility(isVisible ? View.VISIBLE : View.GONE);
    }

    @BindingAdapter("android:invisible")
    public static void setInvisible(@NotNull View view, boolean invisible) {
        view.setVisibility(invisible ? View.INVISIBLE : View.VISIBLE);
    }

    @BindingAdapter("android:gone")
    public static void setGone(@NotNull View view, boolean gone) {
        view.setVisibility(gone ? View.GONE : View.VISIBLE);
    }

    @BindingAdapter({"android:emptyFallback", "android:text"})
    public static void setEmptyFallback(TextView tv, boolean fallback, String text) {
        if (fallback) {
            if (text == null) {
                tv.setText(R.string.bracketed_none);
            } else if (text.length() == 0) {
                tv.setText(R.string.bracketed_empty);
            } else {
                tv.setText(text);
            }
        } else {
            tv.setText(text);
        }
    }

    @BindingAdapter("android:tooltip")
    public static void setTooltip(View view, String tooltipText) {
        TooltipCompat.setTooltipText(view, tooltipText);
        view.setContentDescription(tooltipText);
    }

    @BindingAdapter("android:textStyle")
    public static void setTextStyle(@NotNull TextView textView, int style) {
        textView.setTypeface(null, style);
    }

    @BindingAdapter("android:maxCount")
    public static void setMaxCount(@NonNull EditText et, int maxCount) {
        et.setFilters(new InputFilter[]{new InputFilter.LengthFilter(maxCount)});
    }

    @BindingAdapter("android:width")
    public static void setLayoutWidth(@NotNull View view, float width) {
        if (width == 0) {
            return;
        }
        ViewGroup.LayoutParams lp = view.getLayoutParams();
        if (width == WRAP_CONTENT) {
            lp.width = WRAP_CONTENT;
        } else if (width == MATCH_PARENT) {
            lp.width = MATCH_PARENT;
        } else {
            lp.width = (int) (width + 0.5);
        }
        view.setLayoutParams(lp);
    }

    @BindingAdapter("android:fadeVisible")
    public static void setFadeVisible(@NonNull View view, boolean oldValue, boolean visible) {
        if (oldValue == visible) {
            return;
        }
        if (visible && view.getVisibility() != View.VISIBLE) {
            view.setAlpha(0);
            view.setVisibility(View.VISIBLE);
            view.animate().alpha(1f).setListener(null).start();
        } else if (!visible && view.getVisibility() == View.VISIBLE) {
            view.animate().alpha(0f).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    view.setVisibility(View.GONE);
                }
            }).start();
        }
    }

    @BindingAdapter("android:help")
    public static void setHelp(@NotNull View view, CharSequence helpText) {
        if (helpText == null) {
            view.setVisibility(View.GONE);
        } else {
            setTooltip(view, view.getContext().getString(R.string.help));
            view.setOnClickListener(v -> UiUtils.createHelp(view.getContext(), helpText).show());
        }
    }

    @BindingAdapter("android:help")
    public static void setHelp(@NotNull View view, int helpTextRes) {
        if (helpTextRes < 0) {
            view.setVisibility(View.GONE);
        } else {
            setTooltip(view, view.getContext().getString(R.string.help));
            view.setOnClickListener(v -> UiUtils.createHelp(view.getContext(), view.getContext().getText(helpTextRes)).show());
        }
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
    public static void setMarginBottom(@NotNull View view, float marginBottom) {
        ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
        lp.bottomMargin = (int) marginBottom;
        view.setLayoutParams(lp);
    }

    @BindingAdapter(value = "android:marginEnd")
    public static void setMarginEnd(@NotNull View view, float marginEnd) {
        ViewGroup.LayoutParams lp = view.getLayoutParams();
        try {
            Method method = lp.getClass().getMethod("setMarginEnd", int.class);
            method.invoke(lp, (int) (marginEnd + .5));
            view.setLayoutParams(lp);
        } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    @BindingAdapter(value = "android:marginStart")
    public static void setMarginStart(@NotNull View view, float marginStart) {
        ViewGroup.LayoutParams lp = view.getLayoutParams();
        try {
            Method method = lp.getClass().getMethod("setMarginStart", int.class);
            method.invoke(lp, (int) (marginStart + .5));
            view.setLayoutParams(lp);
        } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
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
            if (top != oldTop || bottom != oldBottom) {
                listener.onChange();
            }
        };
        View.OnLayoutChangeListener oldValue = ListenerUtil.trackListener(view, newValue, R.id.onHeightChanged);
        if (oldValue != null) {
            view.removeOnLayoutChangeListener(oldValue);
        }
        view.addOnLayoutChangeListener(newValue);
    }

    @InverseBindingAdapter(attribute = "android:width", event = "android:widthAttrChanged")
    public static int getWidth(@NotNull View view) {
        return view.getWidth();
    }

    @BindingAdapter("android:width")
    public static void setWidth(@NotNull View view, int width) {
        ViewGroup.LayoutParams lp = view.getLayoutParams();
        lp.width = width;
        view.setLayoutParams(lp);
    }

    @BindingAdapter({"android:widthAttrChanged"})
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

}
