/*
 * Copyright (c) 2020 xjunz. 保留所有权利
 */
package xjunz.tool.wechat.data.viewmodel;

import android.app.Activity;
import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.ViewModelProvider;

import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;

import xjunz.tool.wechat.App;
import xjunz.tool.wechat.impl.model.message.Message;

public class MessageEditorViewModel extends AndroidViewModel {
    private static final String MODEL_KEY = "AndroidViewModel.key.MessageEditor";

    private WeakReference<Message> mMessageToEdit;

    private WeakReference<EventHandler> mEventHandler;

    /**
     * 请勿通过此方法直接构造实例，此方法构造的实例不具备{@link Activity}间
     * 共享的能力，请使用{@link MessageEditorViewModel#get(Application)}
     *
     * @param application Application对象
     */
    public MessageEditorViewModel(@NonNull Application application) {
        super(application);
    }

    @NotNull
    public static MessageEditorViewModel get(Application application) {
        return new ViewModelProvider((App) application, new ViewModelProvider.AndroidViewModelFactory(application)).get(MODEL_KEY, MessageEditorViewModel.class);
    }

    public void registerEventHandler(@NonNull EventHandler handler) {
        mEventHandler = new WeakReference<>(handler);
    }

    public void passMessageToEdit(@NonNull Message message) {
        mMessageToEdit = new WeakReference<>(message);
    }

    public Message getMessageToEdit() {
        if (mMessageToEdit != null) {
            return mMessageToEdit.get();
        }
        return null;
    }

    public void notifyMessageChanged(Message message) {
        if (mEventHandler != null) {
            mEventHandler.get().onMessageChanged(message);
        }
    }

    public void notifyMessageInserted() {
        if (mEventHandler != null) {
            mEventHandler.get().onMessageInserted(mMessageToEdit.get());
        }
    }

    public void notifyMessageDeleted() {
        if (mEventHandler != null) {
            mEventHandler.get().onMessageDeleted(mMessageToEdit.get());
        }
    }

    public interface EventHandler {
        void onMessageChanged(Message changed);

        void onMessageInserted(Message inserted);

        void onMessageDeleted(Message deleted);
    }
}
