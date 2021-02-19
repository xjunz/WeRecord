/*
 * Copyright (c) 2021 xjunz. 保留所有权利
 */

package xjunz.tool.werecord.data.viewmodel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.ObservableBoolean;
import androidx.databinding.ObservableInt;

import java.util.ArrayList;
import java.util.List;

import xjunz.tool.werecord.impl.model.account.Talker;
import xjunz.tool.werecord.impl.model.message.Message;

/**
 * @author xjunz 2020/7/13 00:47
 */
public class MessageViewModel extends ObservableViewModel {
    public Talker currentTalker;
    public ObservableInt currentPageIndex = new ObservableInt(0);
    public ObservableBoolean hasLoadedAll = new ObservableBoolean(false);
    public ObservableBoolean isLoadingAll = new ObservableBoolean(false);
    public List<Message> currentLoadedMessages = new ArrayList<>();
    /**
     * 已确认的修改的备份
     */
    public List<Message> confirmedBackups = new ArrayList<>();
    /**
     * 未确认的修改的备份
     */
    private final List<Message> unconfirmedBackups = new ArrayList<>();
    public long actualMessageCount;
    private EventHandler mEventHandler;

    public void handleEvent(EventHandler eventHandler) {
        this.mEventHandler = eventHandler;
    }

    public void notifyMessageDeleted() {
        if (mEventHandler != null) {
            mEventHandler.onMessageDeleted();
        }
    }

    public void notifyMessageInserted() {
        if (mEventHandler != null) {
            mEventHandler.onMessageInserted();
        }
    }

    public void notifyMessageRestored(int editionFlag, int setFlag) {
        if (mEventHandler != null) {
            mEventHandler.onMessageRestored(editionFlag, setFlag);
        }
    }

    public void notifyMessageChanged() {
        if (mEventHandler != null) {
            mEventHandler.onMessageChanged();
        }
    }

    /**
     * 已编辑消息列表改变，在不知道改变的消息时或者有大量已编辑消息改变时调用此方法
     */
    public void notifyEditionListChanged(int setFlag) {
        if (mEventHandler != null) {
            mEventHandler.onEditionListChanged(setFlag);
        }
    }

    public void addUnconfirmedBackupIfNotExists(@NonNull Message raw) {
        if (!unconfirmedBackups.contains(raw)) {
            unconfirmedBackups.add(raw);
        }
    }

    public void removeUnconfirmedBackup(@NonNull Message raw) {
        unconfirmedBackups.remove(raw);
    }

    @Nullable
    public Message getUnconfirmedBackup(long msgId) {
        for (Message backup : unconfirmedBackups) {
            if (backup.getMsgId() == msgId) {
                return backup;
            }
        }
        return null;
    }

    /**
     * 事件处理者。处理需要{@link android.app.Activity}和{@link androidx.fragment.app.Fragment}或
     * {@link androidx.fragment.app.Fragment}之间进行交互的事件。
     */
    public static class EventHandler {

        public void onMessageDeleted() {
        }

        public void onMessageRestored(int editionFlag, int setFlag) {
        }

        public void onMessageChanged() {
        }

        public void onMessageInserted() {
        }

        public void onEditionListChanged(int set) {
        }
    }

    private SearchDelegate mSearchDelegate;

    public SearchDelegate getSearchDelegate() {
        return mSearchDelegate;
    }

    public void delegateSearch(SearchDelegate searchDelegate) {
        this.mSearchDelegate = searchDelegate;
    }

    public interface SearchDelegate {
        void search(String keyword);
    }

}
