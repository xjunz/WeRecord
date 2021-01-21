/*
 * Copyright (c) 2021 xjunz. 保留所有权利
 */
package xjunz.tool.wechat.ui.base;

import android.os.Bundle;
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
import xjunz.tool.wechat.databinding.DialogSingleLineEditorBinding;
import xjunz.tool.wechat.util.Passable;

/**
 * 一行文字编辑对话框，适用于编辑名称、数字等单行文字的情形
 *
 * @author xjunz 2021/1/3 14:29
 */
public class SingleLineEditorDialog extends ConfirmationDialog<String> {
    private DialogSingleLineEditorBinding mBinding;
    private Passable<EditText> mConfig;
    private String mLabel;

    public String getLabel() {
        return mLabel;
    }

    public CharSequence getCaption() {
        return mCaption;
    }

    @Nullable
    public CharSequence getEditorTag() {
        return mTag;
    }

    private CharSequence mCaption;

    public SingleLineEditorDialog setEditorTag(CharSequence tag) {
        mTag = tag;
        return this;
    }

    private CharSequence mTag;

    @Override
    protected int getStyleRes() {
        return R.style.Base_Dialog_Normal;
    }

    public SingleLineEditorDialog setLabelRes(@StringRes int res) {
        mLabel = App.getStringOf(res);
        return this;
    }

    public SingleLineEditorDialog setLabel(String label) {
        mLabel = label;
        return this;
    }

    public SingleLineEditorDialog setCaptionRes(@StringRes int res) {
        mCaption = App.getTextOf(res);
        return this;
    }

    public SingleLineEditorDialog setCaption(CharSequence caption) {
        mCaption = caption;
        return this;
    }

    @Override
    protected String getResult() {
        return mBinding.etEditor.getText().toString();
    }

    public SingleLineEditorDialog setConfig(Passable<EditText> config) {
        mConfig = config;
        return this;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.dialog_single_line_editor, container, false);
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (mConfig != null) {
            mConfig.pass(mBinding.etEditor);
        }
        mBinding.setHost(this);
        mBinding.etEditor.post(() -> {
            mBinding.etEditor.requestFocus();
            mBinding.etEditor.setSelection(mBinding.etEditor.getText().length());
        });
    }
}
