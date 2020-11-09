/*
 * Copyright (c) 2020 xjunz. 保留所有权利
 */
package xjunz.tool.wechat.data.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.ViewModelProvider;

import org.jetbrains.annotations.NotNull;

import xjunz.tool.wechat.App;

/**
 * 设计为可在{@link android.app.Activity}之间共享数据以及通讯的{@link androidx.lifecycle.ViewModel}。
 * <p>
 * 此方法传递数据相比与传统的{@link android.content.Intent}，不需要进行数据的序列化与反序列化，
 * 支持任意对象的传递（如大{@link android.graphics.Bitmap}。
 * 此{@link androidx.lifecycle.ViewModel}的生命周期与{@link Application}的生命周期绑定，
 * 不会因为{@link android.app.Activity}的销毁或重建而改变
 * </p>
 */

public class InteractivityViewModel extends AndroidViewModel {
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

}
