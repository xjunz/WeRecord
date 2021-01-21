/*
 * Copyright (c) 2021 xjunz. 保留所有权利
 */

package xjunz.tool.wechat.data.viewmodel;

import android.text.Editable;

import androidx.annotation.NonNull;
import androidx.databinding.Bindable;
import androidx.databinding.library.baseAdapters.BR;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModel;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import xjunz.tool.wechat.ui.main.fragment.ChatFragment;
import xjunz.tool.wechat.ui.main.fragment.ContactFragment;

/**
 * {@link Fragment}的{@link ViewModel}类，主要管理{@link PageConfig}实例，以及调度各个筛选事件。
 * <p>此处的{@link Fragment}指{@link ChatFragment}和{@link ContactFragment}</p>
 */
public class PageViewModel extends ObservableViewModel {
    /**
     * 当前{@link Fragment}配置
     */
    private PageConfig mCurrentConfig;
    /**
     * 全局筛选事件处理器集合。无论当前{@link PageConfig}是什么，当事件发出时，接口都会被调用。
     */
    private final List<EventHandler> mHandlerList = new ArrayList<>();


    private boolean mNeedDemonstration;

    public static class EventHandler {
        public void onConfirmFilter() {
        }

        public void onResetFilter() {
        }

        public void onCancelFilter() {
        }

        public void onPrepareFilter() {
        }
    }

    public void requestDemonstrate(boolean needDemonstration) {
        mNeedDemonstration = needDemonstration;
    }

    public boolean needDemonstration() {
        return mNeedDemonstration;
    }

    public void addEventHandler(@NonNull EventHandler handler) {
        this.mHandlerList.add(handler);
    }

    /**
     * 下发筛选确认事件
     */
    public void notifyFilterConfirmed() {
        mCurrentConfig.filterConfirmed = true;
        mCurrentConfig.getEventHandler().onConfirmFilter();
        for (EventHandler handler : mHandlerList) {
            handler.onConfirmFilter();
        }
    }

    /**
     * 下发筛选重置事件
     */
    public void notifyFilterReset() {
        mCurrentConfig.filterConfirmed = true;
        mCurrentConfig.getEventHandler().onResetFilter();
        for (EventHandler handler : mHandlerList) {
            handler.onResetFilter();
        }
    }

    /**
     * 下发搜索事件
     */
    public void notifySearch(@NotNull Editable editable) {
        mCurrentConfig.getEventHandler().onSearch(editable.toString());
    }


    /**
     * 下发筛选取消事件（直接关闭，未点确认也未点重置）
     */
    public void notifyCancelFilter() {
        for (EventHandler handler : mHandlerList) {
            handler.onCancelFilter();
        }
    }

    public void notifyPrepareFilter() {
        mCurrentConfig.filterConfirmed = false;
        for (EventHandler handler : mHandlerList) {
            handler.onPrepareFilter();
        }
    }

    /**
     * 设置当前配置并通知UI更新
     *
     * @param config 当前配置
     */
    public void updateCurrentConfig(@NonNull PageConfig config) {
        mCurrentConfig = config;
        notifyPropertyChanged(BR.currentConfig);
    }


    @Bindable
    public PageConfig getCurrentConfig() {
        return mCurrentConfig;
    }

}
