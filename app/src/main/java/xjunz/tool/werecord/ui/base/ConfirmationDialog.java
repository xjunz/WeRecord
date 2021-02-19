/*
 * Copyright (c) 2021 xjunz. 保留所有权利
 */
package xjunz.tool.werecord.ui.base;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.annotation.StyleRes;
import androidx.core.util.Predicate;
import androidx.fragment.app.DialogFragment;

import java.util.Objects;

import xjunz.tool.werecord.App;
import xjunz.tool.werecord.R;
import xjunz.tool.werecord.ui.customview.MasterToast;
import xjunz.tool.werecord.util.Passable;

public abstract class ConfirmationDialog<T> extends DialogFragment {

    protected String mLabel = "";
    private Predicate<T> mPredicateCallback;
    private Passable<T> mPassableCallback;
    protected T mDef;

    public ConfirmationDialog<T> setLabelRes(@StringRes int res) {
        this.mLabel = App.getStringOf(res);
        return this;
    }

    public ConfirmationDialog<T> setLabel(String label) {
        this.mLabel = label;
        return this;
    }

    public String getLabel() {
        return mLabel;
    }

    /**
     * 设置是否允许内容未改变仍然执行确认事件。默认情况下不允许，即内容未改变时只会弹出Toast提示，不执行任何操作。
     */
    public ConfirmationDialog<T> setAllowUnchanged(boolean allowUnchanged) {
        this.allowUnchanged = allowUnchanged;
        return this;
    }

    private boolean allowUnchanged;

    public ConfirmationDialog<T> setPredicateCallback(Predicate<T> callback) {
        this.mPredicateCallback = callback;
        return this;
    }


    public ConfirmationDialog<T> setPassableCallback(Passable<T> callback) {
        this.mPassableCallback = callback;
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
        return mDef;
    }

    public ConfirmationDialog<T> setDefault(@Nullable T def) {
        this.mDef = def;
        return this;
    }

    protected abstract T getResult();

    ;

    public void confirm() {
        confirm(getResult());
    }

    protected boolean isChanged(T newValue) {
        return !Objects.equals(mDef, newValue);
    }

    private void confirmInternal(T result) {
        if (mPredicateCallback != null) {
            if (mPredicateCallback.test(result)) {
                this.dismiss();
            }
        } else if (mPassableCallback != null) {
            mPassableCallback.pass(result);
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
