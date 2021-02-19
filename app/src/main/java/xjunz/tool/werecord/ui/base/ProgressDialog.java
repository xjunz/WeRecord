/*
 * Copyright (c) 2021 xjunz. 保留所有权利
 */
package xjunz.tool.werecord.ui.base;

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.widget.ProgressBar;

import androidx.appcompat.app.AlertDialog;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import xjunz.tool.werecord.R;
import xjunz.tool.werecord.databinding.DialogProgressBinding;

/**
 * @author xjunz 2021/2/7 16:12
 */
public class ProgressDialog {
    private final DialogProgressBinding mBinding;
    private final AlertDialog.Builder mBuilder;
    private Dialog mDialog;

    @NotNull
    @Contract("_ -> new")
    public static ProgressDialog build(Context context) {
        return new ProgressDialog(context);
    }

    public ProgressDialog(Context context) {
        mBuilder = new AlertDialog.Builder(context, R.style.Theme_AppCompat_Light_Dialog_Alert_Material).setCancelable(false);
        mBinding = DialogProgressBinding.inflate(LayoutInflater.from(context));
        mBuilder.setView(mBinding.getRoot());
    }

    public ProgressDialog setCancelableAction(Runnable cancelAction) {
        mBuilder.setNegativeButton(android.R.string.cancel, (dialog, which) -> {
            cancelAction.run();
            dismiss();
        });
        mBinding.setCancelable(true);
        return this;
    }

    public ProgressBar getProgressBar() {
        return mBinding.progressBar;
    }

    public void setSecondaryProgress(int progress) {
        mBinding.progressBar.setSecondaryProgress(progress);
    }

    public ProgressDialog setTitle(String title) {
        mBinding.tvTitle.setText(title);
        return this;
    }

    public ProgressDialog setTitle(int title) {
        mBinding.tvTitle.setText(title);
        return this;
    }

    public void setProgress(int progress) {
        mBinding.setProgress(progress);
    }

    public void setMaxProgress(int maxProgress) {
        mBinding.setMax(maxProgress);
    }

    public ProgressDialog setDeterminate(boolean determinate) {
        mBinding.setDeterminate(determinate);
        return this;
    }

    private long mLastShowTime;

    public Dialog show() {
        mLastShowTime = System.currentTimeMillis();
        return mDialog = mBuilder.show();
    }

    public long dismiss() {
        if (mDialog != null) {
            mDialog.dismiss();
        } else {
            throw new NullPointerException("Dialog is not currently shown.");
        }
        return System.currentTimeMillis() - mLastShowTime;
    }
}
