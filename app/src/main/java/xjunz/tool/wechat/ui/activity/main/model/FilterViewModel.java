package xjunz.tool.wechat.ui.activity.main.model;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.databinding.Bindable;
import androidx.databinding.Observable;
import androidx.databinding.PropertyChangeRegistry;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;

/**
 * 筛选器的{@link ViewModel}类，主要管理{@link FilterConfig}实例
 * <p>此类实现了{@link Observable}接口，将{@link FilterViewModel#mCurrentConfig}注解为可观察对象，以便实现数据绑定，参考自：
 *
 * @see <a href=https://developer.android.google.cn/topic/libraries/data-binding/two-way?hl=zh_cn>
 * @see <a href=https://developer.android.google.cn/topic/libraries/data-binding/architecture?hl=zh_cn#livedata>
 * </p>
 */
public class FilterViewModel extends ViewModel implements Observable {
    /**
     * 当前过滤器配置
     */
    private FilterConfig mCurrentConfig;
    private PropertyChangeRegistry mCallbacks = new PropertyChangeRegistry();
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

    public void confirmFilter(View view) {
        mCurrentConfig.getEventHandler().confirmFilter();
        for (EventHandler handler : mHandlerList) {
            handler.confirmFilter();
        }
    }

    public void resetFilter(View view) {
        mCurrentConfig.getEventHandler().resetFilter();
        for (EventHandler handler : mHandlerList) {
            handler.resetFilter();
        }
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


    @Override
    public void addOnPropertyChangedCallback(OnPropertyChangedCallback callback) {
        mCallbacks.add(callback);
    }

    @Override
    public void removeOnPropertyChangedCallback(OnPropertyChangedCallback callback) {
        mCallbacks.remove(callback);
    }


    /**
     * Notifies observers that all properties of this instance have changed.
     */
    void notifyChange() {
        mCallbacks.notifyCallbacks(this, 0, null);
    }

    /**
     * Notifies observers that a specific property has changed. The getter for the
     * property that changes should be marked with the @Bindable annotation to
     * generate a field in the BR class to be used as the fieldId parameter.
     *
     * @param fieldId The generated BR id for the Bindable field.
     */
    void notifyPropertyChanged(int fieldId) {
        mCallbacks.notifyCallbacks(this, fieldId, null);
    }
}
