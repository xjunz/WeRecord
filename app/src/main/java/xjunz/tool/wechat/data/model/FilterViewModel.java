package xjunz.tool.wechat.data.model;

import androidx.annotation.NonNull;
import androidx.databinding.Bindable;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;

/**
 * 筛选器的{@link ViewModel}类，主要管理{@link FilterConfig}实例
 */
public class FilterViewModel extends ObservableViewModel {
    /**
     * 当前过滤器配置
     */
    private FilterConfig mCurrentConfig;
    /**
     * 全局过滤事件处理者器集合。无论当前{@link FilterConfig}是什么，当事件发出时，接口都会被调用
     */
    private List<EventHandler> mHandlerList = new ArrayList<>();

    public interface EventHandler {
        void confirmFilter();

        void resetFilter();
    }

    public void addEventHandler(@NonNull EventHandler handler) {
        this.mHandlerList.add(handler);
    }

    public void confirmFilter() {
        mCurrentConfig.getEventHandler().confirmFilter();
        for (EventHandler handler : mHandlerList) {
            handler.confirmFilter();
        }
    }

    public void resetFilter() {
        mCurrentConfig.getEventHandler().resetFilter();
        for (EventHandler handler : mHandlerList) {
            handler.resetFilter();
        }
    }

    public void search(String keyword) {
        mCurrentConfig.getEventHandler().onSearch(keyword);
    }

    /**
     * 设置当前配置并通知UI更新
     *
     * @param config 当前配置
     */
    public void updateCurrentConfig(@NonNull FilterConfig config) {
        mCurrentConfig = config;
        notifyChange();
    }


    @Bindable
    public FilterConfig getCurrentConfig() {
        return mCurrentConfig;
    }

}
