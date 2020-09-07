/*
 * Copyright (c) 2020 xjunz. 保留所有权利
 */
package xjunz.tool.wechat.data.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.ViewModelProvider;

import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;

import xjunz.tool.wechat.App;
import xjunz.tool.wechat.impl.DatabaseModifier;
import xjunz.tool.wechat.impl.model.message.Message;

/**
 * 设计为可在{@link android.app.Activity}之间共享数据的{@link androidx.lifecycle.ViewModel}。
 * <p>
 * 此方法传递数据相比与传统的{@link android.content.Intent}，不需要进行数据的序列化与反序列化，
 * 支持任意对象的传递（如大{@link android.graphics.Bitmap}。<strong>注意，此类会一直持有这些对象的引用
 * 直到{@link Application}被销毁，因此请勿传递{@link android.app.Activity}或持有{@link android.app.Activity}
 * 的变量，从而导致可能的内存泄露。</strong>
 * 此{@link androidx.lifecycle.ViewModel}的生命周期与{@link Application}的生命周期绑定，
 * 不会因为{@link android.app.Activity}的销毁或重建而改变
 * </p>
 */

public class InteractivityViewModel extends AndroidViewModel {

    /**
     * {@link xjunz.tool.wechat.ui.message.MessageActivity}传递给{@link xjunz.tool.wechat.ui.message.EditorActivity}
     * 的用于编辑的{@link Message}
     */
    private WeakReference<Message> mMessageToEdit;
    private WeakReference<DatabaseModifier> mModifier;

    /**
     * 请勿通过此方法直接构造实例，此方法构造的实例不具备{@link android.app.Activity}间
     * 共享的能力，请使用{@link InteractivityViewModel#get(Application)}
     */
    public InteractivityViewModel(@NonNull Application application) {
        super(application);
    }

    @NotNull
    public static InteractivityViewModel get(Application application) {
        return new ViewModelProvider((App) application, new ViewModelProvider.AndroidViewModelFactory(application)).get(InteractivityViewModel.class);
    }

    public void passMessageToEdit(Message message) {
        mMessageToEdit = new WeakReference<>(message);
    }

    public Message getMessageToEdit() {
        return mMessageToEdit.get();
    }

    public void passDatabaseModifier(DatabaseModifier modifier) {
        mModifier = new WeakReference<>(modifier);
    }

    public DatabaseModifier getDatabaseModifier() {
        return mModifier.get();
    }

}
