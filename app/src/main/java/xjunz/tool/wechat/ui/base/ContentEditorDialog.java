/*
 * Copyright (c) 2021 xjunz. 保留所有权利
 */
package xjunz.tool.wechat.ui.base;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.databinding.DataBindingUtil;

import xjunz.tool.wechat.App;
import xjunz.tool.wechat.R;
import xjunz.tool.wechat.databinding.DialogContentEditorBinding;
import xjunz.tool.wechat.util.Passable;
import xjunz.tool.wechat.util.Utils;

public class ContentEditorDialog extends ConfirmationDialog<String> {
    private String mLabel;
    private DialogContentEditorBinding mBinding;
    private Passable<EditText> mEditTextConfiguration;

    public ContentEditorDialog setLabelRes(@StringRes int res) {
        this.mLabel = App.getStringOf(res);
        return this;
    }

    public ContentEditorDialog setLabel(String label) {
        this.mLabel = label;
        return this;
    }

    public ContentEditorDialog configEditText(Passable<EditText> config) {
        this.mEditTextConfiguration = config;
        return this;
    }


    @Override
    public int getStyleRes() {
        return R.style.Base_Dialog_Translucent_NoDim;
    }

    @Override
    protected String getResult() {
        return mBinding.etEditor.getText().toString();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.dialog_content_editor, container, false);
        mBinding.setLabel(mLabel);
        mBinding.setHost(this);
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (mEditTextConfiguration != null) {
            mEditTextConfiguration.pass(mBinding.etEditor);
        }
        mBinding.lineCounter.bindTo(mBinding.etEditor);
        mBinding.etEditor.post(() -> {
            mBinding.etEditor.setMinHeight(mBinding.svEditor.getHeight());
            if (mBinding.etEditor.getHeight() <= mBinding.svEditor.getHeight()) {
                mBinding.etEditor.setSelection(mBinding.etEditor.getText().length());
            }
            Utils.showImeFor(mBinding.etEditor);
        });
    }

    /**
     * 如果原消息是NULL，那么如果{@code TextUtils.isEmpty(newValue)}也会被视为消息未改变。
     */
    @Override
    public boolean isChanged(String newValue) {
        return (getDefaultValue() != null || !TextUtils.isEmpty(newValue)) && super.isChanged(newValue);
    }
}
