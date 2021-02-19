/*
 * Copyright (c) 2021 xjunz. 保留所有权利
 */

package xjunz.tool.werecord.data.viewmodel;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.databinding.ObservableArrayList;
import androidx.databinding.ObservableArrayMap;
import androidx.databinding.ObservableBoolean;
import androidx.databinding.ObservableField;
import androidx.databinding.ObservableInt;
import androidx.databinding.ObservableList;

import java.util.List;
import java.util.Objects;

import xjunz.tool.werecord.ui.main.fragment.ChatFragment;
import xjunz.tool.werecord.ui.main.fragment.FilterFragment;

/**
 * 定义主界面的{@code Page}（页面）的相关配置和信息的实体类，这些配置会被会被显示在UI之中。为了实现数据绑定，某些字段继承自{@link java.util.Observable}
 */
public class PageConfig {
    /**
     * 是否启用筛选，默认为true
     */
    public boolean filterEnabled = true;
    /**
     * 当前页面的描述
     */
    public String caption;
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
    public ObservableList<String> typeList = new ObservableArrayList<>();
    /**
     * 候选排序方式列表
     */
    public ObservableList<String> sortByList = new ObservableArrayList<>();
    /**
     * 类型筛选{@link android.widget.Spinner}所选中的索引
     */
    public ObservableInt typeSelection = new ObservableInt(0);
    /**
     * 排序依据
     */
    @NonNull
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
     * 当前配置是否为{@link ChatFragment}的配置
     */
    public ObservableBoolean isChat = new ObservableBoolean(false);
    /**
     * 是否处于搜索模式
     */
    public ObservableBoolean isInSearchMode = new ObservableBoolean();
    /**
     * 是否处于多选模式
     */
    public ObservableBoolean isInMultiSelectionMode = new ObservableBoolean();
    public ObservableInt selectionCount = new ObservableInt();
    /**
     * 搜索关键词
     */
    public ObservableField<String> searchKeyword = new ObservableField<>();
    /**
     * 筛选是否确认
     */
    public boolean filterConfirmed = true;
    /**
     * 事件处理者，响应{@link FilterFragment} 发出的筛选命令
     */
    private EventHandler mEventHandler;

    /**
     * @return 当前排序的顺序是否为升序
     */
    public boolean isAscending() {
        return orderBy.get() == ORDER_ASCENDING;
    }

    /**
     * 获取当前排序依据，此返回值不为空
     *
     * @return 当前排序依据
     */
    @NonNull
    public SortBy getCurrentSortBy() {
        return Objects.requireNonNull(sortBy.get());
    }

    /**
     * 筛选事件处理器，只有当{@link PageViewModel#getCurrentConfig()}为当前对象时，接口才会被调用。
     * 如果想要响应全局事件，使用{@link PageViewModel#addEventHandler(PageViewModel.EventHandler)}
     */
    public interface EventHandler {
        /**
         * 确认筛选
         */
        void onConfirmFilter();

        /**
         * 重置筛选
         */
        void onResetFilter();

        /**
         * 搜索内容
         *
         * @param keyword 关键词
         */
        void onSearch(@NonNull String keyword);

    }

    public void increaseSelection() {
        selectionCount.set(selectionCount.get() + 1);
    }

    public void decreaseSelection() {
        selectionCount.set(selectionCount.get() - 1);
    }

    public void setEventHandler(@NonNull EventHandler handler) {
        this.mEventHandler = handler;
    }

    public void toggleSearchMode() {
        isInSearchMode.set(!isInSearchMode.get());
    }

    @NonNull
    EventHandler getEventHandler() {
        if (mEventHandler == null) {
            throw new IllegalArgumentException("Please call setEventHandler() first");
        }
        return mEventHandler;
    }

    public int getFilterId() {
        return Objects.hash(typeSelection.get(),
                descriptionSelectionMap.toString(),
                isAscending(),
                getCurrentSortBy().caption);
    }
}
