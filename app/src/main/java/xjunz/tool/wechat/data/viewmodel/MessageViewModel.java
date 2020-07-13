/*
 * Copyright (c) 2020 xjunz. 保留所有权利
 */

package xjunz.tool.wechat.data.viewmodel;

import androidx.databinding.ObservableBoolean;
import androidx.databinding.ObservableField;

import xjunz.tool.wechat.impl.model.message.Message;

/**
 * @author xjunz 2020/7/13 00:47
 */
public class MessageViewModel extends ObservableViewModel {
    public ObservableBoolean isInEditMode = new ObservableBoolean(false);
    public ObservableField<Message> currentSelectedMessage = new ObservableField<>();
}
