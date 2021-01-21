/*
 * Copyright (c) 2021 xjunz. 保留所有权利
 */
package xjunz.tool.wechat.ui.base;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;
import androidx.core.util.Predicate;
import androidx.fragment.app.DialogFragment;

import java.util.Objects;

import xjunz.tool.wechat.R;
import xjunz.tool.wechat.ui.customview.MasterToast;
import xjunz.tool.wechat.util.Passable;

public abstract class ConfirmationDialog<T> extends DialogFragment {

    private Predicate<T> predicateCallback;
    private Passable<T> passableCallback;
    protected T def;

    /**
     * 设置是否允许内容未改变仍然执行确认事件。默认情况下不允许，即内容未改变时只会弹出Toast提示，不执行任何操作。
     */
    public ConfirmationDialog<T> setAllowUnchanged(boolean allowUnchanged) {
        this.allowUnchanged = allowUnchanged;
        return this;
    }

    private boolean allowUnchanged;

    public ConfirmationDialog<T> setCallback(Predicate<T> callback) {
        this.predicateCallback = callback;
        return this;
    }


    public ConfirmationDialog<T> setCallback(Passable<T> callback) {
        this.passableCallback = callback;
        return this;
    }

    @StyleRes
    protected int getStyleRes() {
        return R.style.Base_Dialog_Normal;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NO_TITLE, getStyleRes());
    }

    @Nullable
    public T getDefaultValue() {
        return def;
    }

    public ConfirmationDialog<T> setDefault(@Nullable T def) {
        this.def = def;
        return this;
    }

    protected abstract T getResult();

    ;

    public void confirm() {
        confirm(getResult());
    }

    protected boolean isChanged(T newValue) {
        return !Objects.equals(def, newValue);
    }

    private void confirmInternal(T result) {
        if (predicateCallback != null) {
            boolean ok = predicateCallback.test(result);
            if (ok) {
                this.dismiss();
            }
        } else if (passableCallback != null) {
            passableCallback.pass(result);
            this.dismiss();
        } else {
            this.dismiss();
        }
    }

    private void confirm(T result) {
        if (allowUnchanged) {
            confirmInternal(result);
        } else {
            if (isChanged(result)) {
                confirmInternal(result);
            } else {
                MasterToast.shortToast(R.string.no_change_was_made);
            }
        }
    }
}
