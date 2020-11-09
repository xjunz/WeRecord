/*
 * Copyright (c) 2020 xjunz. 保留所有权利
 */

package xjunz.tool.wechat.data.viewmodel;

import androidx.databinding.ObservableBoolean;
import androidx.databinding.ObservableField;

import xjunz.tool.wechat.impl.model.account.Talker;
import xjunz.tool.wechat.impl.model.message.BackupMessage;
import xjunz.tool.wechat.impl.model.message.Message;

/**
 * @author xjunz 2020/7/13 00:47
 */
public class MessageViewModel extends ObservableViewModel {
    public Talker currentTalker;
    public ObservableInt currentPageIndex = new ObservableInt(0);
    public ObservableBoolean hasLoadAll = new ObservableBoolean(false);
    public List<Message> currentLoadedMessages = new ArrayList<>();
    public List<BackupMessage> allBackupMessages = new ArrayList<>();
    public int actualMessageCount;
    public int selectedMessagePosition;
    private final ArrayList<EventHandler> eventHandlers = new ArrayList<>();

    public void addEventHandler(EventHandler eventHandler) {
        eventHandlers.add(eventHandler);
    }

    public void notifySearch(String keyword) {
        for (EventHandler eventHandler : eventHandlers) {
            eventHandler.onSearch(keyword);
        }
    }

    public void notifyNavigate(Message msg) {
        for (EventHandler eventHandler : eventHandlers) {
            eventHandler.onNavigate(msg);
        }
    }

    public void notifyAllLoaded(int preCount) {
        hasLoadAll.set(true);
        for (EventHandler eventHandler : eventHandlers) {
            eventHandler.onAllLoaded(preCount);
        }
    }

    /**
     * 向所有{@link EventHandler}发布消息变更事件
     *
     * @param msgId 变更的消息ID
     */
    public void notifyMessageChanged(int msgId) {

    }

    public void setSelectedMessagePosition(int position) {
        this.selectedMessagePosition = position;
    }

    public Message getSelectedMessage() {
        return currentLoadedMessages.get(selectedMessagePosition);
    }

    /**
     * 事件处理者。处理需要{@link android.app.Activity}和{@link androidx.fragment.app.Fragment}或
     * {@link androidx.fragment.app.Fragment}之间进行交互的事件。
     */
    public static class EventHandler {
        public void onSearch(String keyword) {
        }

        public void onNavigate(Message msg) {
        }

        public void onAllLoaded(int preCount) {

        }
    }
}
