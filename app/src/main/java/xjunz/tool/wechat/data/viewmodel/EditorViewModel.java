/*
 * Copyright (c) 2020 xjunz. 保留所有权利
 */
package xjunz.tool.wechat.data.viewmodel;

import android.text.Editable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.ObservableBoolean;
import androidx.databinding.ObservableField;
import androidx.databinding.ObservableLong;

import org.jetbrains.annotations.NotNull;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

import xjunz.tool.wechat.App;
import xjunz.tool.wechat.R;
import xjunz.tool.wechat.impl.Environment;
import xjunz.tool.wechat.impl.model.account.Account;
import xjunz.tool.wechat.impl.model.account.Group;
import xjunz.tool.wechat.impl.model.message.Message;
import xjunz.tool.wechat.impl.repo.GroupRepository;
import xjunz.tool.wechat.impl.repo.RepositoryFactory;
import xjunz.tool.wechat.util.UniUtils;

/**
 * {@link xjunz.tool.wechat.ui.message.EditorActivity}及其内部{@link androidx.fragment.app.Fragment}
 * 共享的{@link androidx.lifecycle.ViewModel}
 */
public class EditorViewModel extends ObservableViewModel {
    /**
     * 默认添加的消息的时间差，默认值为一秒。即若“在前面添加”，则默认在目标消息
     * 前一秒添加消息，相反，在其后一秒添加消息。
     */
    public static final long DEFAULT_TIME_OFFSET = 1000L;
    /**
     * 编辑模式
     *
     * @see EditorViewModel#EDIT_MODE_EDIT
     * @see EditorViewModel#EDIT_MODE_ADD_BEFORE
     * @see EditorViewModel#EDIT_MODE_ADD_AFTER
     */
    public int editMode;
    /**
     * 欲编辑的原消息
     */
    public Message originalMessage;
    public Account defSender;
    public long defSendTimestamp = -1L;
    public final ObservableField<Account> selectedSender = new ObservableField<>();
    public final ObservableField<Account> candidateSender = new ObservableField<>();
    public final ObservableLong modifiedTimestamp = new ObservableLong();
    public long sendTimestampStartLimit = -1;
    public long sendTimestampEndLimit = -1;
    public final ObservableBoolean contentChanged = new ObservableBoolean(false);
    public final ObservableBoolean senderChanged = new ObservableBoolean(false);
    public final ObservableBoolean sendTimestampChanged = new ObservableBoolean(false);
    public final ObservableField<Message> modifiedMessage = new ObservableField<>();
    /**
     * 编辑模式：编辑原消息
     */
    public static final int EDIT_MODE_EDIT = 1;
    /**
     * 编辑模式：在前面添加消息
     */
    public static final int EDIT_MODE_ADD_BEFORE = 3;
    /**
     * 编辑模式：在后面添加消息
     */
    public static final int EDIT_MODE_ADD_AFTER = 5;

    @NonNull
    private Message requireModifiedMessage() {
        return Objects.requireNonNull(modifiedMessage.get());
    }

    public void setModifiedContent(String modified) {
        requireModifiedMessage().modifyContent(modified);
        modifiedMessage.notifyChange();
        contentChanged.set(!modified.equals(originalMessage.getContent()));
    }

    public boolean isInEditMode() {
        return editMode == EDIT_MODE_EDIT;
    }

    public void switchStatus() {
        if (!requireModifiedMessage().getStatus().equals(originalMessage.getStatus())) {
            requireModifiedMessage().setStatus(originalMessage.getStatus());
        } else {
            if (originalMessage.getStatus().equals(Message.STATUS_SEND_FAILED)) {
                requireModifiedMessage().setStatus(Message.STATUS_SEND_SUC);
            } else {
                requireModifiedMessage().setStatus(Message.STATUS_SEND_FAILED);
            }
        }
        modifiedMessage.notifyChange();
    }

    public void setStartAndEndSendTimestampLimit(long start, long end) {
        this.sendTimestampStartLimit = start;
        this.sendTimestampEndLimit = end;
        this.defSendTimestamp = getDefaultSendTimestamp();
        this.modifiedTimestamp.set(defSendTimestamp);
    }

    private long getDefaultSendTimestamp() {
        //如果是编辑原消息
        if (isInEditMode()) {
            return originalMessage.getCreateTimeStamp();
        }
        //如果是添加消息
        //如果起始和结束都有时限
        if (sendTimestampStartLimit != -1 && sendTimestampEndLimit != -1) {
            //且两者时间差超过默认时间差
            if (sendTimestampEndLimit - sendTimestampStartLimit >= DEFAULT_TIME_OFFSET) {
                //根据是否为在前面添加消息，向前或向后调整发送时间
                return editMode == EDIT_MODE_ADD_BEFORE ? sendTimestampEndLimit - DEFAULT_TIME_OFFSET : sendTimestampStartLimit + DEFAULT_TIME_OFFSET;
            } else {
                //否则，选择两者中间的时间点
                return (sendTimestampEndLimit + sendTimestampStartLimit) / 2;
            }
        } else if (sendTimestampStartLimit == -1) {
            //如果没有起始时限，则返回结束时限前的默认时间差的时间戳
            return sendTimestampEndLimit - DEFAULT_TIME_OFFSET;
        } else {
            //如果没有结束时限，返回起始时限后的默认时间差的时间戳
            return sendTimestampStartLimit + DEFAULT_TIME_OFFSET;
        }
    }

    @Nullable
    public Group getGroupTalker() {
        if (isGroupTalker()) {
            GroupRepository groupRepository = RepositoryFactory.get(GroupRepository.class);
            return groupRepository.get(originalMessage.getTalkerId());
        }
        return null;
    }

    @Nullable
    public String[] getOptionalSenderIds() {
        if (isGroupTalker()) {
            Group group = getGroupTalker();
            return group != null ? group.getMemberIdList() : null;
        } else {
            //搁着和自己聊天呢
            if (Environment.getInstance().getCurrentUser().id.equals(originalMessage.getTalkerId())) {
                return new String[]{null};
            }
            return new String[]{null, originalMessage.getTalkerId()};
        }
    }

    public boolean isGroupTalker() {
        return originalMessage.getTalkerId().endsWith("@chatroom");
    }

    public void setDefSender(Account account) {
        this.defSender = account;
        this.candidateSender.set(account);
        this.selectedSender.set(account);
    }

    public void setSelectedSender(@NotNull Account selectedSender) {
        requireModifiedMessage().modifySenderId(selectedSender.id);
        this.candidateSender.set(selectedSender);
        this.selectedSender.set(selectedSender);
        this.senderChanged.set(!selectedSender.equals(defSender));
        modifiedMessage.notifyChange();
    }

    public void setCandidateSender(Account selectedSender) {
        this.candidateSender.set(selectedSender);
    }

    public void setModifiedTimestamp(long modifiedTimestamp) {
        requireModifiedMessage().setCreateTimeStamp(modifiedTimestamp);
        this.modifiedTimestamp.set(modifiedTimestamp);
        this.sendTimestampChanged.set(modifiedTimestamp != defSendTimestamp);
    }

    public final ObservableField<String> hintText = new ObservableField<>();
    public final ObservableLong parsedTimestamp = new ObservableLong(-1L);

    public void notifyTextChanged(@NotNull Editable editable) {
        String text = editable.toString();
        long timestamp = parseDate(text);
        parsedTimestamp.set(-1L);
        if (timestamp < 0) {
            hintText.set(App.getStringOf(R.string.timestamp_parse_failed));
        } else if (sendTimestampEndLimit > 0 && timestamp >= sendTimestampEndLimit) {
            hintText.set(App.getStringOf(R.string.exceeded_stop_limit));
        } else if (sendTimestampStartLimit > 0 && timestamp <= sendTimestampStartLimit) {
            hintText.set(App.getStringOf(R.string.exceeded_start_limit));
        } else {
            parsedTimestamp.set(timestamp);
            hintText.set(UniUtils.formatDateChinese(timestamp));
        }
    }

    private long parseDate(String dateStr) {
        SimpleDateFormat format = (SimpleDateFormat) SimpleDateFormat.getDateTimeInstance();
        format.applyPattern("yyyy-MM-dd HH:mm:ss:SSS");
        try {
            Date date = format.parse(dateStr);
            if (date == null) {
                return -1L;
            } else {
                return date.getTime();
            }
        } catch (ParseException e) {
            e.printStackTrace();
            return -1L;
        }
    }
}
