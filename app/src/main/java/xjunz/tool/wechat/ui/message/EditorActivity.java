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

import java.util.Objects;

import xjunz.tool.wechat.R;
import xjunz.tool.wechat.data.viewmodel.EditorViewModel;
import xjunz.tool.wechat.databinding.ActivityEditorBinding;
import xjunz.tool.wechat.impl.DatabaseModifier;
import xjunz.tool.wechat.impl.model.message.Message;
import xjunz.tool.wechat.impl.repo.MessageRepository;
import xjunz.tool.wechat.impl.repo.RepositoryFactory;
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
     * 源消息，如果是编辑模式，则为欲编辑的消息，如果是添加模式，则为选中的消息
     */
    public static final String EXTRA_ORIGINAL_MESSAGE_ID = "EditorActivity.extra.OriginalMessageId";
    /**
     * 发送时间的起始时限，添加模式传入
     */
    public static final String EXTRA_SEND_TIMESTAMP_START = "EditorActivity.extra.SendTimestamp.START";
    /**
     * 发送时间的结束时限，添加模式传入
     */
    public static final String EXTRA_SEND_TIMESTAMP_STOP = "EditorActivity.extra.SendTimestamp.STOP";
    private EditorViewModel mModel;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityEditorBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_editor);
        mModel = new ViewModelProvider(this, new ViewModelProvider.NewInstanceFactory()).get(EditorViewModel.class);
        Intent intent = getIntent();
        mModel.editMode = intent.getIntExtra(EXTRA_EDIT_MODE, -1);
        int msgId = intent.getIntExtra(EXTRA_ORIGINAL_MESSAGE_ID, -1);
        Message original = RepositoryFactory.get(MessageRepository.class).queryMessageByMsgId(msgId);
        Objects.requireNonNull(original, "Null message: " + msgId);
        mModel.originalMessage = original;
        if (mModel.editMode == EditorViewModel.EDIT_MODE_EDIT) {
            mModel.modifiedMessage.set(original.deepClone());
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
            DatabaseModifier modifier = getEnvironment().modifyDatabase();
            modifier.replace(modified).commit();
            finish();
        }
    }


    public void editTimestamp(View view) {
        new TimestampEditorDialog().show(getSupportFragmentManager(), "timestamp");
    }
}
