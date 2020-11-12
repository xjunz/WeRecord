/*
 * Copyright (c) 2020 xjunz. 保留所有权利
 */
package xjunz.tool.wechat.data.viewmodel;

import android.app.Activity;
import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.ViewModelProvider;

import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import xjunz.tool.wechat.App;
import xjunz.tool.wechat.impl.model.message.Message;

public class MessageEditorViewModel extends AndroidViewModel implements LifecycleObserver {
    private static final String MODEL_KEY = "AndroidViewModel.key.MessageEditor";

    private WeakReference<Message> mMessageToEdit;

    private final ArrayList<EditorEventHandler> mEditorEventHandlers = new ArrayList<>();

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

    public void registerEventHandler(@NonNull EditorEventHandler handler) {
        mEditorEventHandlers.add(handler);
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

    public void notifyMessageChanged(boolean timestampChanged, Message message) {
        for (EditorEventHandler handler : mEditorEventHandlers) {
            handler.onMessageChanged(timestampChanged, message);
        }
    }

    public void notifyMessageInserted(boolean addBefore, Message message) {
        for (EditorEventHandler handler : mEditorEventHandlers) {
            handler.onMessageInserted(addBefore, message);
        }
    }

    public void notifyMessageDeleted() {
        for (EditorEventHandler handler : mEditorEventHandlers) {
            handler.onMessageDeleted();
        }
    }

    public interface EditorEventHandler {
        void onMessageChanged(boolean timestampChanged, Message changed);

        void onMessageInserted(boolean addBefore, Message inserted);

        void onMessageDeleted();
    }

    public void purge() {
        if (mMessageToEdit != null) {
            mMessageToEdit.clear();
        }
        mEditorEventHandlers.clear();
    }
}
