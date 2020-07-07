package xjunz.tool.wechat.data.viewmodel;

import android.text.Editable;

import androidx.annotation.NonNull;
import androidx.databinding.Bindable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModel;

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
    private List<EventHandler> mHandlerList = new ArrayList<>();

    public interface EventHandler {
        void confirmFilter();

        void resetFilter();
    }

    public void addEventHandler(@NonNull EventHandler handler) {
        this.mHandlerList.add(handler);
    }

    /**
     * 下发筛选确认事件
     */
    public void notifyFilterConfirmed() {
        mCurrentConfig.getEventHandler().confirmFilter();
        for (EventHandler handler : mHandlerList) {
            handler.confirmFilter();
        }
    }

    /**
     * 下发筛选重置事件
     */
    public void notifyFilterReset() {
        mCurrentConfig.getEventHandler().resetFilter();
        for (EventHandler handler : mHandlerList) {
            handler.resetFilter();
        }
    }

    /**
     * 下发搜索事件
     */
    public void notifySearch(Editable editable) {
        mCurrentConfig.getEventHandler().onSearch();
    }


    /**
     * 设置当前配置并通知UI更新
     *
     * @param config 当前配置
     */
    public void updateCurrentConfig(@NonNull PageConfig config) {
        mCurrentConfig = config;
        notifyChange();
    }


    @Bindable
    public PageConfig getCurrentConfig() {
        return mCurrentConfig;
    }

}
