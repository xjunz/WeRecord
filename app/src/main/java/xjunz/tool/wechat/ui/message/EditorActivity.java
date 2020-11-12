/*
 * Copyright (c) 2020 xjunz. 保留所有权利
 */
package xjunz.tool.wechat.ui.message;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;

import xjunz.tool.wechat.R;
import xjunz.tool.wechat.data.viewmodel.EditorViewModel;
import xjunz.tool.wechat.data.viewmodel.MessageEditorViewModel;
import xjunz.tool.wechat.databinding.ActivityEditorBinding;
import xjunz.tool.wechat.impl.DatabaseModifier;
import xjunz.tool.wechat.impl.Environment;
import xjunz.tool.wechat.impl.model.message.Edition;
import xjunz.tool.wechat.impl.model.message.Message;
import xjunz.tool.wechat.ui.BaseActivity;
import xjunz.tool.wechat.ui.message.fragment.dialog.ContentEditorDialog;
import xjunz.tool.wechat.ui.message.fragment.dialog.SenderChooserDialog;
import xjunz.tool.wechat.ui.message.fragment.dialog.TimestampEditorDialog;

public class EditorActivity extends BaseActivity {
    /**
     * 编辑模式
     *
     * @see EditorViewModel#EDIT_MODE_ADD_BEFORE
     * @see EditorViewModel#EDIT_MODE_ADD_AFTER
     * @see EditorViewModel#EDIT_MODE_EDIT
     */
    public static final String EXTRA_EDIT_MODE = "EditorActivity.extra.EditMode";
    /**
     * 发送时间的起始时限，添加模式传入
     */
    public static final String EXTRA_SEND_TIMESTAMP_START = "EditorActivity.extra.SendTimestamp.START";
    /**
     * 发送时间的结束时限，添加模式传入
     */
    public static final String EXTRA_SEND_TIMESTAMP_STOP = "EditorActivity.extra.SendTimestamp.STOP";
    private EditorViewModel mModel;
    private MessageEditorViewModel mMessageEditorViewModel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityEditorBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_editor);
        mModel = new ViewModelProvider(this, new ViewModelProvider.NewInstanceFactory()).get(EditorViewModel.class);
        mMessageEditorViewModel = MessageEditorViewModel.get(getApplication());
        Intent intent = getIntent();
        Message original = mMessageEditorViewModel.getMessageToEdit();
        mModel.originalMessage = original;
        mModel.editMode = intent.getIntExtra(EXTRA_EDIT_MODE, -1);
        if (mModel.editMode == EditorViewModel.EDIT_MODE_EDIT) {
            mModel.modifiedMessage.set(original.deepFactoryClone());
        }
        mModel.setStartAndEndSendTimestampLimit(intent.getLongExtra(EXTRA_SEND_TIMESTAMP_START, -1),
                intent.getLongExtra(EXTRA_SEND_TIMESTAMP_STOP, -1));
        mModel.setDefSender(original.getSenderAccount());
        binding.setModel(mModel);
    }

    public void editContent(View view) {
        new ContentEditorDialog().show(getSupportFragmentManager(), "content");
    }

    public void editSender(View view) {
        new SenderChooserDialog().show(getSupportFragmentManager(), "sender");
    }

    public void confirmEdition(View view) {
        Message modified = mModel.modifiedMessage.get();
        if (modified != null) {
            DatabaseModifier modifier = Environment.getInstance().modifyDatabase();
            if (mModel.editMode == EditorViewModel.EDIT_MODE_EDIT) {
                modified.setEditionFlag(Edition.FLAG_REPLACEMENT);
                modifier.putPendingEdition(Edition.replace(mModel.originalMessage, modified));
                mMessageEditorViewModel.notifyMessageChanged(mModel.sendTimestampChanged.get(), modified);
            } else {
                modified.setEditionFlag(Edition.FLAG_INSERTION);
                modifier.putPendingEdition(Edition.insert(modified));
                mMessageEditorViewModel.notifyMessageInserted(mModel.editMode == EditorViewModel.EDIT_MODE_ADD_BEFORE, modified);
            }
        }
        finishAfterTransition();
    }


    public void editTimestamp(View view) {
        new TimestampEditorDialog().show(getSupportFragmentManager(), "timestamp");
    }


}
