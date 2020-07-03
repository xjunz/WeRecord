package xjunz.tool.wechat.data.model;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.databinding.ObservableArrayList;
import androidx.databinding.ObservableArrayMap;
import androidx.databinding.ObservableBoolean;
import androidx.databinding.ObservableField;
import androidx.databinding.ObservableInt;
import androidx.databinding.ObservableList;

import java.util.List;

/**
 * 定义各种筛选相关配置的实体类，这些配置会被会被显示在{@link xjunz.tool.wechat.ui.activity.main.FilterFragment}中。
 * </br>因为实现了数据绑定，所有字段都继承自{@link java.util.Observable}
 */
public class FilterConfig {
    /**
     * 顺序标签：升序
     */
    public static final int ORDER_ASCENDING = 0;
    /**
     * 顺序标签：降序
     */
    @Keep
    public static final int ORDER_DESCENDING = 1;
    /**
     * 候选筛选类型列表
     */
    public ObservableList<String> categoryList = new ObservableArrayList<>();
    /**
     * 候选排序方式列表
     */
    public ObservableList<String> sortByList = new ObservableArrayList<>();
    /**
     * 类型筛选{@link android.widget.Spinner}所选中的索引
     */
    public ObservableInt categorySelection = new ObservableInt(0);
    /**
     * 排序依据
     */
    public ObservableField<SortBy> sortBy = new ObservableField<>(SortBy.NAME);
    /**
     * 排序顺序
     */
    public ObservableInt orderBy = new ObservableInt(0);
    /**
     * 各个排序依据下的筛选{@link android.widget.Spinner}所选中的索引列表
     */
    public ObservableArrayMap<SortBy, Integer> descriptionSelectionMap = new ObservableArrayMap<>();
    /**
     * 数据总数
     */
    public ObservableInt totalCount = new ObservableInt(0);
    /**
     * 已筛选出的数据数
     */
    public ObservableInt filteredCount = new ObservableInt(0);
    /**
     * 各个排序依据下的描述（筛选依据）列表
     */
    public ObservableArrayMap<SortBy, List<String>> descriptionListMap = new ObservableArrayMap<>();
    /**
     * 当前配置是否为{@link xjunz.tool.wechat.ui.activity.main.ChatFragment}的配置
     */
    public ObservableBoolean isChat = new ObservableBoolean(true);
    /**
     * 事件处理者，响应{@link xjunz.tool.wechat.ui.activity.main.FilterFragment} 发出的筛选命令
     */
    private EventHandler mEventHandler;

    /**
     * @return 当前排序的顺序是否为升序
     */
    public boolean isAscending() {
        return orderBy.get() == ORDER_ASCENDING;
    }


    /**
     * 过滤事件处理器，只有当{@link FilterViewModel#getCurrentConfig()}为当前对象时，接口才会被调用。
     * 如果想要响应全局事件，使用{@link FilterViewModel#addEventHandler(FilterViewModel.EventHandler)}
     */
    public interface EventHandler {
        void confirmFilter();

        void resetFilter();

        void onSearch(String keyword);
    }

    public void setEventHandler(@NonNull EventHandler handler) {
        this.mEventHandler = handler;
    }

    @NonNull
    EventHandler getEventHandler() {
        if (mEventHandler == null) {
            throw new IllegalArgumentException("Please setEventHandler first");
        }
        return mEventHandler;
    }
}
