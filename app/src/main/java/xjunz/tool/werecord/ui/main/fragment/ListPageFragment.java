/*
 * Copyright (c) 2021 xjunz. 保留所有权利
 */

package xjunz.tool.werecord.ui.main.fragment;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.util.Pair;
import android.view.View;
import android.view.ViewStub;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import org.jetbrains.annotations.NotNull;
import org.reactivestreams.Publisher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import io.reactivex.Flowable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.internal.functions.Functions;
import io.reactivex.schedulers.Schedulers;
import xjunz.tool.werecord.R;
import xjunz.tool.werecord.impl.model.account.Contact;
import xjunz.tool.werecord.ui.customview.MasterToast;
import xjunz.tool.werecord.ui.main.MainActivity;
import xjunz.tool.werecord.ui.viewmodel.PageConfig;
import xjunz.tool.werecord.ui.viewmodel.PageViewModel;
import xjunz.tool.werecord.ui.viewmodel.SortBy;
import xjunz.tool.werecord.util.RxJavaUtils;
import xjunz.tool.werecord.util.UiUtils;
import xjunz.tool.werecord.util.Utils;

public abstract class ListPageFragment<T extends Contact> extends PageFragment implements PageConfig.EventHandler {
    /**
     * 当前显示的数据列表
     */
    protected List<Item> mItemList;
    /**
     * 筛选后的数据列表（用于搜索）
     */
    private List<Item> mFilteredItemList;
    /**
     * 显示数据的{@link RecyclerView}
     */
    protected RecyclerView mList;
    /**
     * {@link ListPageFragment#mList}的{@link androidx.recyclerview.widget.RecyclerView.Adapter}
     */
    private ListPageAdapter<?> mAdapter;
    /**
     * {@link ListPageFragment#mIvNoResult}的{@link ViewStub}，用于懒加载
     */
    private ViewStub mStubNoResult;
    /**
     * 当数据为空时，显示占位图的{@link ImageView}
     *
     * @see R.mipmap#art_no_result
     */
    private ImageView mIvNoResult;
    /**
     * 当前数据所有分隔项的描述集合
     */
    protected List<String> mCurrentDescCacheList = new ArrayList<>();
    /**
     * 原始配置信息描述符，用于判断配置是否发生变化
     *
     * @see ListPageFragment#hasFilterConfigChanged()
     */
    private int mFilterConfigIdentifierCache;
    protected PageViewModel mModel;
    protected MainActivity mHost;

    /**
     * 获取当前的排序依据列表
     */
    public abstract SortBy[] getSortByList();

    /**
     * 获取类型列表
     */
    public abstract Contact.Type[] getTypeList();

    /**
     * 获取{@link ListPageFragment#mList}的适配器
     */
    public abstract ListPageAdapter<?> getAdapter();

    /**
     * 重置配置
     *
     * @param config 当前配置
     */
    public abstract void resetFilterConfig(PageConfig config);

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mModel = Utils.getViewModel(requireActivity(), PageViewModel.class);
        mHost = (MainActivity) requireActivity();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mList = view.findViewById(R.id.rv_list);
        mStubNoResult = view.findViewById(R.id.stub_no_result);
        mFilterConfigIdentifierCache = getCurrentConfig().getFilterId();
        initList(items -> {
            mItemList = items;
            mFilteredItemList = items;
            showOrHideNoResultArt(mItemList.isEmpty());
            //更新UI
            mAdapter = getAdapter();
            mList.setAdapter(mAdapter);
            updateCountInfo(items);
            //获取所有分隔项
            collectSeparatorDescListMap();
        });
    }

    /**
     * 判断配置信息是否改变
     * <p>从用户打开筛选面板到进行筛选配置修改到点击“确认”按钮，
     * 我们无法得知用户是否修改了配置，因此通过此方法判断配置是否改变，
     * 以减少不必要的数据刷新。</p>
     *
     * @return 配置是否改变
     */
    private boolean hasFilterConfigChanged() {
        int currentId = mConfig.getFilterId();
        if (currentId == mFilterConfigIdentifierCache) {
            return false;
        } else {
            mFilterConfigIdentifierCache = currentId;
            return true;
        }
    }

    /**
     * 根据当前所选{@link xjunz.tool.werecord.impl.model.account.Contact.Type}获取原始数据列表
     *
     * @return 原始数据列表
     */
    public abstract List<T> getAllOfType(@NonNull Contact.Type type);

    /**
     * 返回查询到的所有数据
     *
     * @return 所有数据
     */
    public abstract List<T> getAll();

    public int getCurrentListSize() {
        return mItemList.size();
    }

    private final class ItemRemovalCallback extends DiffUtil.Callback {
        private final List<Item> mNewItems;
        private static final int MODE_DETECT_REMOVAL = 1;
        private static final int MODE_DETECT_KEYWORD = 2;

        private ItemRemovalCallback(List<Item> newItems) {
            mNewItems = newItems;
        }

        @Override
        public int getOldListSize() {
            return mItemList.size();
        }

        @Override
        public int getNewListSize() {
            return mNewItems.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            return mItemList.get(oldItemPosition).equals(mNewItems.get(newItemPosition));
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            Item oldItem = mItemList.get(oldItemPosition);
            Item newItem = mNewItems.get(newItemPosition);
            if (oldItem.isSeparator() && newItem.isSeparator()) {
                return oldItem.equals(newItem);
            }
            return true;
        }
    }

    /**
     * 初始化列表
     */
    protected void initList(Consumer<List<Item>> onFinish) {
        if (onFinish == null) {
            onFinish = Functions.emptyConsumer();
        }
        RxJavaUtils.stream(getRawDataList())
                .flatMap((Function<T, Publisher<Item>>) t -> subscriber -> {
                    String description = t.describe(mConfig.getCurrentSortBy());
                    if (!mCurrentDescCacheList.contains(description)) {
                        subscriber.onNext(new Item(t, description, Item.TYPE_SEPARATOR));
                        mCurrentDescCacheList.add(description);
                    }
                    subscriber.onNext(new Item(t, description, Item.TYPE_DATA));
                    subscriber.onComplete();
                })
                .toSortedList()
                .doOnSuccess(onFinish).subscribe();
    }


    /**
     * 重新加载列表
     */
    protected void reloadList() {
        mCurrentDescCacheList.clear();
        mConfig.descriptionListMap.clear();
        initList(items -> {
            DiffUtil.calculateDiff(new ItemRemovalCallback(items)).dispatchUpdatesTo(mAdapter);
            mItemList = items;
            mFilteredItemList = items;
            showOrHideNoResultArt(mItemList.isEmpty());
            updateCountInfo(items);
            collectSeparatorDescListMap();
        });
    }

    /**
     * 筛选数据，返回包含筛选后的数据的{@link Single}，以便自定义订阅逻辑。
     *
     * @param dataList 欲筛选的原始数据
     * @return 包含筛选数据的{@link Single}
     */
    private Single<List<Item>> filter(List<T> dataList) {
        mCurrentDescCacheList.clear();
        return Flowable.fromIterable(dataList)
                .filter(t -> {
                    for (SortBy sortBy : getSortByList()) {
                        Integer descSelection = mConfig.descriptionSelectionMap.get(sortBy);
                        if (descSelection != null && descSelection != 0) {
                            String desc = Objects.requireNonNull(mConfig.descriptionListMap.get(sortBy)).get(descSelection);
                            if (!t.describe(sortBy).equals(desc)) {
                                return false;
                            }
                        }
                    }
                    return true;
                }).flatMap((Function<T, Publisher<Item>>) t -> subscriber -> {
                    String description = t.describe(mConfig.getCurrentSortBy());
                    if (!mCurrentDescCacheList.contains(description)) {
                        subscriber.onNext(new Item(t, description, Item.TYPE_SEPARATOR));
                        mCurrentDescCacheList.add(description);
                    }
                    subscriber.onNext(new Item(t, description, Item.TYPE_DATA));
                    subscriber.onComplete();
                }).toSortedList();
    }

    /**
     * 搜索数据，返回包含搜索后的数据的{@link Single}，方便自定义订阅逻辑。
     *
     * @param itemList 欲搜索的原始数据
     * @param keyword  搜索关键词
     * @return 包含搜索后数据的{@link Single}
     */
    private Single<List<Item>> search(List<Item> itemList, @NonNull String keyword) {
        mCurrentDescCacheList.clear();
        return Flowable.fromIterable(itemList).subscribeOn(Schedulers.computation()).observeOn(AndroidSchedulers.mainThread())
                .filter(Item::isData)
                .filter(item -> {
                    String name = item.content.getName();
                    int index;
                    if ((index = name.indexOf(keyword)) >= 0 || (index = item.content.getNamePyAttr().indexOf(keyword.toUpperCase())) >= 0) {
                        item.spanStartIndex = index;
                        item.spanLength = keyword.length();
                        return true;
                    }
                    return false;
                })
                .flatMap((Function<Item, Publisher<Item>>) item -> subscriber -> {
                    String description = item.description;
                    if (!mCurrentDescCacheList.contains(description)) {
                        subscriber.onNext(new Item(item.content, description, Item.TYPE_SEPARATOR));
                        mCurrentDescCacheList.add(description);
                    }
                    subscriber.onNext(item);
                    subscriber.onComplete();
                })
                .toSortedList();
    }

    public void quitMultiSelectionMode() {
        for (int i = 0; i < mItemList.size(); i++) {
            Item item = mItemList.get(i);
            if (item.isSelected()) {
                mItemList.get(i).setSelected(false);
                mAdapter.notifyItemChanged(i, "selected");
            }
        }
        mConfig.selectionCount.set(0);
        mConfig.isInMultiSelectionMode.set(false);
    }

    public void selectAll() {
        int count = 0;
        for (int i = 0; i < mItemList.size(); i++) {
            Item item = mItemList.get(i);
            if (item.isData()) {
                mItemList.get(i).setSelected(true);
                count++;
            }
        }
        mConfig.selectionCount.set(count);
        mAdapter.notifyItemRangeChanged(0, mItemList.size(), true);
    }

    /**
     * 确认筛选事件的回调
     * <p>判断当前配置是否发生改变以决定是否进行筛选</p>
     */
    @Override
    public void onConfirmFilter() {
        if (!hasFilterConfigChanged()) {
            MasterToast.shortToast(R.string.msg_filter_config_unchanged);
        } else {
            filter(getRawDataList()).doOnSuccess(newItemList -> {
                mFilteredItemList = newItemList;
                updateList(newItemList);
            }).subscribe();
        }
    }


    /**
     * 搜索事件的回调
     * <p>判断当前配置是否发生改变：如果改变，先筛选，再搜索；如果未改变，
     * 直接对{@link ListPageFragment#mFilteredItemList}进行搜索</p>
     *
     * @param keyword 关键词
     */
    @Override
    public void onSearch(@NotNull String keyword) {
        //如果关键字不为空且筛选配置改变了
        if (hasFilterConfigChanged()) {
            //则先筛选再搜索
            filter(getRawDataList()).doOnSuccess(items -> {
                mFilteredItemList = items;
                search(items, keyword).doOnSuccess(this::updateList).subscribe();
            }).subscribe();
        } else {
            //否则，直接搜索筛选后的数据
            search(mFilteredItemList, keyword).doOnSuccess(this::updateList).subscribe();
        }
    }

    /**
     * 重置筛选事件回调
     */
    @Override
    public void onResetFilter() {
        resetFilterConfig(mConfig);
        if (!hasFilterConfigChanged()) {
            MasterToast.shortToast(R.string.msg_filter_config_unchanged);
        } else {
            filter(getRawDataList()).doOnSuccess(newItemList -> {
                mFilteredItemList = newItemList;
                updateList(newItemList);
                MasterToast.shortToast(R.string.reset_completed);
            }).subscribe();
        }
    }


    /**
     * 并行获取所有分隔项，作为筛选依据，同时在{@link FilterFragment}中的{@link android.widget.Spinner}中显示。
     */
    protected void collectSeparatorDescListMap() {
        List<T> all = getAll();
        Disposable disposable = Flowable.fromArray(getSortByList()).parallel().runOn(Schedulers.computation())
                .map(sortBy -> {
                    List<String> descList = new ArrayList<>();
                    ArrayList<T> syncAll = new ArrayList<>(all);
                    //对数据进行排序
                    Collections.sort(syncAll, (o1, o2) -> o1.compareTo(o2, sortBy, true));
                    //获取描述列表
                    for (T t : syncAll) {
                        String des = t.describe(sortBy);
                        if (!descList.contains(des)) {
                            descList.add(des);
                        }
                    }
                    //添加通配项“<全部>”和“<无>”
                    if (descList.isEmpty()) {
                        descList.add(getString(R.string.bracketed_none));
                    } else {
                        descList.add(0, getString(R.string.bracketed_all));
                    }
                    return new Pair<>(sortBy, descList);
                }).sequential().observeOn(AndroidSchedulers.mainThread())
                .subscribe(sortByListPair -> mConfig.descriptionListMap.put(sortByListPair.first, sortByListPair.second));
    }


    /**
     * 更新统计数据信息，即更新{@link FilterFragment}中“共XX条数据 已筛选出XX条数据”的文本
     *
     * @param newItemList 新的数据列表
     */
    private void updateCountInfo(List<Item> newItemList) {
        Disposable disposable = RxJavaUtils.stream(newItemList).filter(Item::isData).count().subscribe(count -> {
            mConfig.totalCount.set(getRawDataList().size());
            mConfig.filteredCount.set(count.intValue());
        });
    }

    private List<T> getRawDataList() {
        Contact.Type selectedType = getTypeList()[mConfig.typeSelection.get()];
        return selectedType == null ? getAll() : getAllOfType(selectedType);
    }

    /**
     * 通知数据更新并更新UI
     *
     * @param newItemList 新的数据列表
     */
    protected void updateList(@NotNull List<Item> newItemList) {
        showOrHideNoResultArt(newItemList.isEmpty());
        mItemList = newItemList;
        updateCountInfo(newItemList);
        mAdapter.notifyDataSetChanged();
    }

    /**
     * 显示或隐藏“没有结果”的占位图
     *
     * @param toShow 是否显示
     */
    private void showOrHideNoResultArt(boolean toShow) {
        if (mIvNoResult == null) {
            mIvNoResult = (ImageView) mStubNoResult.inflate();
        }
        if (toShow) {
            mIvNoResult.setAlpha(0f);
            mIvNoResult.animate().alpha(1f).withStartAction(() -> mIvNoResult.setVisibility(View.VISIBLE)).start();
        } else {
            mIvNoResult.setVisibility(View.GONE);
        }
    }


    /**
     * 列表项目的抽象类，定义了列表项目的各种属性
     */
    protected class Item implements Comparable<Item> {
        /**
         * 分隔项标识
         * <p>为了方便用户区分不同的数据，我们对数据进行分类，而这些分组之间的分隔即分隔项。
         * 分隔项显示的是它所代表的分组的描述信息，它下面至另一个分隔项是具有相同描述信息的一组</p>
         */
        static final int TYPE_SEPARATOR = 0x1000;
        /**
         * 数据项标识，数据项的信息会显示在列表之中
         */
        static final int TYPE_DATA = 0x1100;

        /**
         * 数据类型
         *
         * @see Item#TYPE_SEPARATOR
         * @see Item#TYPE_DATA
         */
        int type;
        /**
         * 当前项所包含的内容
         * <p>对于类型为{@link Item#TYPE_DATA}的项而言，内容会被显示在列表之中</p>
         * <p>对于类型为{@link Item#TYPE_SEPARATOR}的项而言，内容会被用于排序时的比较。该项所包含的内容
         * 是不确定的，仅作为比较的标的</p>
         */
        T content;
        /**
         * 当前项的内容的描述，
         * <p>对于类型为{@link Item#TYPE_DATA}的项而言，描述是分类的依据，描述相同的项会被分为一类</p>
         * <p>对于类型为{@link Item#TYPE_SEPARATOR}的项而言，描述会被显示在列表中作为不同分组的数据之间的分隔</p>
         */
        String description;
        /**
         * Span文字的参数
         */
        int spanStartIndex, spanLength;
        /**
         * 是否被选中
         */
        private boolean isSelected = false;
        /**
         * 是否可见（默认可见）
         */
        boolean isVisible = true;

        void setSelected(boolean selected) {
            isSelected = isData() && selected;
        }

        public boolean isSelected() {
            return isSelected;
        }

        @SuppressWarnings("unchecked")
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null) {
                return false;
            }
            if (o instanceof ListPageFragment.Item) {
                Item item = (Item) o;
                if (this.type == item.type) {
                    if (this.type == TYPE_SEPARATOR) {
                        return this.description.equals(item.description);
                    } else {
                        return this.content.equals(item.content);
                    }
                }
            }
            return false;
        }

        @Override
        public int compareTo(@NotNull Item o) {
            //如果是相同类型的项，直接比较content即可
            if (this.type == o.type) {
                return this.content.compareTo(o.content, mConfig.getCurrentSortBy(), mConfig.isAscending());
            } else {
                //如果是不同类型的项，先比较描述是否相等，如果描述相等，说明这两项属于同一分组，分隔项应当小于数据项(无论当前配置是否为升序)；
                //如果描述不等，说明这两组不属于同一分组，再比较content确定顺序。
                //应当注意的是，不能通过描述的先后顺序确定数据的先后顺序，因为描述是数据的描述性文本，文本的字符性先后顺序
                //不能代表背后数据的先后顺序。
                int descCompareRes = this.description.compareTo(o.description);
                if (descCompareRes == 0) {
                    return this.isSeparator() ? -1 : 1;
                } else {
                    return this.content.compareTo(o.content, mConfig.getCurrentSortBy(), mConfig.isAscending());
                }
            }
        }

        boolean isSeparator() {
            return this.type == TYPE_SEPARATOR;
        }

        boolean isData() {
            return this.type == TYPE_DATA;
        }

        public Item(T content, String desc, int type) {
            this.type = type;
            this.content = content;
            this.description = desc;
        }
    }

    public abstract class ListPageAdapter<S extends ListPageViewHolder> extends RecyclerView.Adapter<S> {
        private final int foregroundSpanColor;
        private final int backgroundSpanColor;

        public ListPageAdapter() {
            foregroundSpanColor = UiUtils.getAttrColor(requireContext(), R.attr.colorAccent);
            backgroundSpanColor = UiUtils.getAttrColor(requireContext(), R.attr.colorControlHighlight);
        }

        @Override
        public int getItemViewType(int position) {
            return mItemList.get(position).type;
        }

        @Override
        public void onBindViewHolder(@NonNull S holder, int position, @NonNull List<Object> payloads) {
            if (payloads.size() != 0) {
                Object payload = payloads.get(0);
                if (payload instanceof String) {
                    String payloadStr = ((String) payload);
                    Item item = mItemList.get(position);
                    switch (payloadStr) {
                        case "selected":
                            holder.itemView.setSelected(item.isSelected());
                            break;
                        case "span":
                            //...
                            break;
                    }
                    return;
                }
            }
            super.onBindViewHolder(holder, position, payloads);
        }

        @Override
        public void onBindViewHolder(@NonNull S holder, int position) {
            Item item = mItemList.get(position);
            switch (item.type) {
                //如果是数据项
                case Item.TYPE_DATA:
                    final Contact content = item.content;
                    //如果可见，初始化内容
                    if (item.isVisible) {
                        holder.itemView.setVisibility(View.VISIBLE);
                        //设置是否被选中
                        holder.itemView.setSelected(item.isSelected);
                        //设置名称
                        int startIndex = item.spanStartIndex;
                        if (mConfig.isInSearchMode.get() && startIndex >= 0) {
                            //如果存在，设置关键字高亮
                            ForegroundColorSpan foregroundColorSpan = new ForegroundColorSpan(foregroundSpanColor);
                            BackgroundColorSpan backgroundColorSpan = new BackgroundColorSpan(backgroundSpanColor);
                            SpannableString span = new SpannableString(content.getName());
                            span.setSpan(foregroundColorSpan, startIndex, startIndex + item.spanLength, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            span.setSpan(backgroundColorSpan, startIndex, startIndex + item.spanLength, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                            holder.tvName.setText(span);
                        } else {
                            holder.tvName.setText(content.getName());
                        }
                        //异步设置头像
                        RxJavaUtils.maybe(content::getAvatar).subscribe(new RxJavaUtils.MaybeObserverAdapter<Bitmap>() {
                            @Override
                            public void onSuccess(@NotNull Bitmap bitmap) {
                                holder.ivAvatar.setImageBitmap(bitmap);
                            }

                            @Override
                            public void onComplete() {
                                holder.ivAvatar.setImageResource(R.mipmap.avatar_default);
                            }
                        });
                    } else {
                        //设置不可见
                        holder.itemView.setVisibility(View.GONE);
                    }
                    //设置下分割线可视性
                    if (position == mItemList.size() - 1 || mItemList.get(position + 1).type == Item.TYPE_SEPARATOR) {
                        holder.bottomDivider.setVisibility(View.GONE);
                    } else {
                        holder.bottomDivider.setVisibility(View.VISIBLE);
                    }
                    break;
                //如果是分隔项
                case Item.TYPE_SEPARATOR:
                    //仅设置描述文字
                    holder.tvSeparator.setText(item.description);
                    break;
            }
        }

        @Override
        public int getItemCount() {
            return mItemList.size();
        }
    }

    public static class ListPageViewHolder extends RecyclerView.ViewHolder {
        protected TextView tvName, tvSeparator;
        protected ImageView ivAvatar;
        protected View bottomDivider;

        public ListPageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_name);
            tvSeparator = itemView.findViewById(R.id.tv_separator);
            ivAvatar = itemView.findViewById(R.id.iv_avatar);
            bottomDivider = itemView.findViewById(R.id.divider_bottom);
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
