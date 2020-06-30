package xjunz.tool.wechat.ui.activity.main;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import io.reactivex.Flowable;
import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import io.reactivex.schedulers.Schedulers;
import xjunz.tool.wechat.R;
import xjunz.tool.wechat.impl.model.account.Talker;
import xjunz.tool.wechat.impl.repo.TalkerRepository;
import xjunz.tool.wechat.ui.activity.main.model.FilterConfig;
import xjunz.tool.wechat.ui.activity.main.model.FilterViewModel;
import xjunz.tool.wechat.ui.activity.main.model.SortBy;
import xjunz.tool.wechat.ui.customview.MasterToast;
import xjunz.tool.wechat.util.UiUtils;


/**
 * 显示会话列表的{@link Fragment}
 */
public class ChatFragment extends Fragment implements FilterConfig.EventHandler {
    private RecyclerView mList;
    private ChatAdapter mAdapter;
    /**
     * 未经加工的原始Talker数据列表，从数据库中查询获得
     */
    private ArrayList<Talker> mRawDataList;
    /**
     * 由{@link ChatFragment#mRawDataList}映射而来的{@link Item}列表
     */
    private List<Item> mItemList;

    /**
     * 当前的过滤配置
     */
    private FilterConfig mConfig;
    /**
     * 分割项描述列表缓存，每一次加载列表都使用此变量储存分隔项的描述
     */
    private List<String> mSeparatorDescCache;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mItemList = new ArrayList<>();
        FilterViewModel model = new ViewModelProvider(requireActivity(), new ViewModelProvider.NewInstanceFactory()).get(FilterViewModel.class);
        initFilterConfig();
        model.updateCurrentConfig(mConfig);
    }

    /**
     * 初始化过滤器配置
     */
    private void initFilterConfig() {
        mConfig = new FilterConfig();
        mConfig.isChat.set(true);
        mConfig.sortBy.set(SortBy.TIMESTAMP);
        List<String> captionList = Talker.Type.getCaptionList();
        captionList.add(0, getString(R.string.bracketed_all));
        mConfig.categoryList.addAll(captionList);
        mConfig.sortByList.addAll(SortBy.getCaptionList());
        mConfig.setEventHandler(this);
    }

    /**
     * 重置过滤器配置
     */
    private void resetFilterConfig() {
        mConfig.sortBy.set(SortBy.TIMESTAMP);
        mConfig.orderBy.set(FilterConfig.ORDER_ASCENDING);
        mConfig.categorySelection.set(0);
        mConfig.descriptionSelectionMap.clear();
    }


    /**
     * @return 当前过滤器配置
     */
    @Nullable
    public FilterConfig getFilterConfig() {
        return mConfig;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat, container, false);
        mList = view.findViewById(R.id.rv_list);
        initList();
        return view;
    }


    /**
     * 初始化列表
     */
    private void initList() {
        mSeparatorDescCache = new ArrayList<>();
        mRawDataList = TalkerRepository.getInstance().getAll();
        Talker.setSortByAndOrderBy(Talker.DEFAULT_SORT_BY, Talker.DEFAULT_IS_ASCENDING);
        mLoadListDisposable = Flowable.fromIterable(mRawDataList).subscribeOn(Schedulers.computation()).map(new Function<Talker, Item>() {
            @Override
            public Item apply(Talker talker) {
                //将源数据映射为列表项
                String desc = talker.describe();
                Item item = new Item(talker, desc);
                //判断当前数据是否为分隔点，是的话传入一个分隔项
                if (mSeparatorDescCache.size() == 0 || !mSeparatorDescCache.contains(desc)) {
                    item.type = Item.TYPE_SEPARATOR;
                    mSeparatorDescCache.add(desc);
                }
                return item;
            }
        }).doOnNext(new Consumer<Item>() {
            @Override
            public void accept(Item item) throws Exception {
                //如果传来的是分隔项，先插入分隔项，再插入相同content和description的数据项
                if (item.type == Item.TYPE_SEPARATOR) {
                    mItemList.add(item);
                }
                mItemList.add(new Item(item.content, item.description, Item.TYPE_DATA));
            }
        }).observeOn(AndroidSchedulers.mainThread()).doOnComplete(new Action() {
            @Override
            public void run() {
                Collections.sort(mItemList);
                //更新UI
                mAdapter = new ChatAdapter();
                mList.setAdapter(mAdapter);
                updateCountInfo();
                //获取所有分隔项
                collectSeparatorDescListMap();
            }
        }).subscribe();
    }

    /**
     * 更新统计数据信息，即更新{@link FilterFragment}中“共XX条数据 已过滤出XX条数据”的文本
     */
    private void updateCountInfo() {
        mConfig.totalCount.set(mRawDataList.size());
        mConfig.filteredCount.set(mItemList.size() - mSeparatorDescCache.size());
    }


    /**
     * 获取所有分隔项，作为筛选依据，同时在{@link FilterFragment}中的{@link android.widget.Spinner}中显示
     */
    private void collectSeparatorDescListMap() {
        //初始化Map
        for (final SortBy by : SortBy.values()) {
            List<String> descList = new ArrayList<>();
            //配置排序信息
            Talker.setSortByAndOrderBy(by, true);
            //对数据进行排序
            Collections.sort(mRawDataList);
            //获取描述列表
            for (Talker talker : mRawDataList) {
                String des = talker.describe();
                if (!descList.contains(des)) {
                    descList.add(des);
                }
            }
            //添加通配项“<全部>”和“<无>”
            if (descList.size() == 0) {
                descList.add(getString(R.string.bracketed_none));
            } else {
                descList.add(0, getString(R.string.bracketed_all));
            }
            mConfig.descriptionListMap.put(by, descList);
        }
        //以上代码执行时间5毫秒左右（90条记录）
    }


    /**
     * 执行筛选并更新UI
     */
    private void doFilter() {
        mSeparatorDescCache.clear();
        mItemList.clear();
        Talker.setSortByAndOrderBy(mConfig.sortBy.get(), mConfig.isAscending());
        mLoadListDisposable = Flowable.fromIterable(mRawDataList).subscribeOn(Schedulers.computation()).filter(new Predicate<Talker>() {
            @Override
            public boolean test(Talker talker) {
                //如果类型为“<全部>"或所选类型为当前Talker类型，通过第一步类型筛选
                if (mConfig.categorySelection.get() == 0 || talker.type == Talker.Type.values()[mConfig.categorySelection.get() - 1]) {
                    //对三个排序依据的条件进行筛选
                    for (SortBy by : SortBy.values()) {
                        Integer selectionOfSortBy = mConfig.descriptionSelectionMap.get(by);
                        //如果筛选条件为“<全部>”,此排序依据直接跳过
                        if (selectionOfSortBy == null || selectionOfSortBy == 0) {
                            continue;
                        }
                        //否则，获取该筛选条件的描述
                        String desOfSortBy = Objects.requireNonNull(mConfig.descriptionListMap.get(by)).get(selectionOfSortBy);
                        //如果筛选条件的描述和当前项描述不一致，此项不通过
                        if (!talker.describe(by).equals(desOfSortBy)) {
                            return false;
                        }
                    }
                } else {
                    //否则不通过
                    return false;
                }
                //以上全满足，此项通过
                return true;
            }
        }).map(new Function<Talker, Item>() {
            @Override
            public Item apply(Talker talker) {
                //将源数据映射为列表项
                String desc = talker.describe();
                Item item = new Item(talker, desc);
                //判断当前数据是否为分隔点，是的话传入一个分隔项
                if (mSeparatorDescCache.size() == 0 || !mSeparatorDescCache.contains(desc)) {
                    item.type = Item.TYPE_SEPARATOR;
                    mSeparatorDescCache.add(desc);
                }
                return item;
            }
        }).doOnNext(new Consumer<Item>() {
            @Override
            public void accept(Item item) throws Exception {
                //如果传来的是分隔项，先插入分隔项，再插入相同content和description的数据项
                if (item.type == Item.TYPE_SEPARATOR) {
                    mItemList.add(item);
                }
                mItemList.add(new Item(item.content, item.description, Item.TYPE_DATA));
            }
        }).observeOn(AndroidSchedulers.mainThread()).doOnComplete(new Action() {
            @Override
            public void run() {
                Talker.setSortByAndOrderBy(mConfig.sortBy.get(), mConfig.isAscending());
                Collections.sort(mItemList);
                mAdapter.notifyDataSetChanged();
                updateCountInfo();
            }
        }).subscribe();
    }


    @Override
    public void confirmFilter() {
        doFilter();
    }

    @Override
    public void resetFilter() {
        resetFilterConfig();
        doFilter();
    }


    /**
     * 列表项目的抽象类，定义了列表项目的各种属性
     */
    private class Item implements Comparable<Item> {
        /**
         * 分隔项标识
         * <p>为了方便用户区分不同的数据，我们对数据进行分类，而这些分组之间的分隔即分隔项。
         * 分隔项显示的是它所代表的分组的描述信息，它下面至另一个分隔项是具有相同描述信息的一组</p>
         */
        static final int TYPE_SEPARATOR = 0x1000;
        /**
         * 数据项标识，数据项的{@link Item#content}字段的某些信息会显示在列表之中
         */
        static final int TYPE_DATA = 0x1100;
        //类型
        int type;
        /**
         * 当前项所包含的内容
         * <p>对于类型为{@link Item#TYPE_DATA}的项而言，内容会被显示在列表之中</p>
         * <p>对于类型为{@link Item#TYPE_SEPARATOR}的项而言，内容会被用于排序时的比较。该项所包含的内容
         * 是不确定的，仅作为比较的标的</p>
         */
        Talker content;
        /**
         * 当前项的内容的描述，
         * <p>对于类型为{@link Item#TYPE_DATA}的项而言，描述是分类的依据，描述相同的项会被分为一类</p>
         * <p>对于类型为{@link Item#TYPE_SEPARATOR}的项而言，描述会被显示在列表中作为不同分组的数据之间的分隔</p>
         */
        String description;
        //Span文字的参数
        int spanStartIndex, spanLength;
        //是否被选中
        boolean isSelected;
        //是否可见（默认可见）
        boolean isVisible = true;

        public Item(Talker talker, String desc) {
            this.content = talker;
            this.description = desc;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Item item = (Item) o;
            if (this.type == item.type) {
                if (this.type == TYPE_SEPARATOR) {
                    return this.description.equals(item.description);
                } else {
                    return this.content.equals(item.content);
                }
            }
            return false;
        }

        @Override
        public int compareTo(@NotNull Item o) {
            Talker.setSortByAndOrderBy(mConfig.sortBy.get(), mConfig.isAscending());
            //如果是相同类型的项，直接比较content即可
            if (this.type == o.type) {
                return this.content.compareTo(o.content);
            } else {
                //如果是不同类型的项，先比较描述是否相等，如果描述相等，说明这两项属于同一分组，分隔项应当小于数据项(无论当前配置是否为升序)；
                //如果描述不等，说明这两组不属于同一分组，再比较content确定顺序。
                //应当注意的是，不能通过描述的先后顺序确定数据的先后顺序，因为描述是数据的描述性文本，文本的字符性先后顺序
                //不能代表背后数据的先后顺序。
                int descCompareRes = this.description.compareTo(o.description);
                if (descCompareRes == 0) {
                    return this.isSeparator() ? -1 : 1;
                } else {
                    return this.content.compareTo(o.content);
                }
            }
        }

        private boolean isSeparator() {
            return this.type == TYPE_SEPARATOR;
        }

        public Item(Talker content, String desc, int type) {
            this.type = type;
            this.content = content;
            this.description = desc;
        }

    }


    private class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

        private int spanColor;

        private ChatAdapter() {
            //因为是用TypedArray获取的，比较吃资源，设为成员变量
            //直接在构造器里初始化，防止每次用到都要获取
            spanColor = UiUtils.getAttrColor(requireContext(), R.attr.colorAccent);
        }

        @Override
        public int getItemViewType(int position) {
            return mItemList.get(position).type;
        }

        @NonNull
        @Override
        public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(viewType == Item.TYPE_DATA ? R.layout.item_chat : R.layout.item_separator, parent, false);
            return new ChatViewHolder(view);
        }


        /**
         * 实现一下Payload加载列表，更新列表更轻量流畅
         */
        @Override
        public void onBindViewHolder(@NonNull ChatViewHolder holder, int position, @NonNull List<Object> payloads) {

            if (payloads.size() != 0) {
                int payload = (int) payloads.get(0);
                holder.itemView.setAlpha(0);
                holder.itemView.animate().alpha(1).start();
            } else {
                super.onBindViewHolder(holder, position, payloads);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull final ChatViewHolder holder, int position) {
            Item item = mItemList.get(position);
            switch (item.type) {
                //如果是数据项
                case Item.TYPE_DATA:
                    final Talker talker = item.content;
                    //如果可见，初始化内容
                    if (item.isVisible) {
                        holder.itemView.setVisibility(View.VISIBLE);
                        //设置名称，如果存在，设置关键字高亮
                        int startIndex = item.spanStartIndex;
                        if (startIndex >= 0) {
                            ForegroundColorSpan foregroundColorSpan = new ForegroundColorSpan(spanColor);
                            SpannableString span = new SpannableString(talker.getName());
                            span.setSpan(foregroundColorSpan, item.spanStartIndex, startIndex + item.spanLength, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            holder.tvName.setText(span);
                        } else {
                            holder.tvName.setText(talker.getName());
                        }
                        //设置记录数
                        holder.tvMsgCount.setText(Html.fromHtml(getString(R.string.format_total_records, talker.messageCount)));
                        //异步设置头像
                        Disposable disposable = Single.create(new SingleOnSubscribe<Bitmap>() {
                            @Override
                            public void subscribe(SingleEmitter<Bitmap> emitter) throws Exception {
                                Bitmap avatar = talker.getAvatar();
                                if (avatar == null) {
                                    emitter.onError(null);
                                } else {
                                    emitter.onSuccess(talker.getAvatar());
                                }
                            }
                        }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(new Consumer<Bitmap>() {
                            @Override
                            public void accept(Bitmap bitmap) throws Exception {
                                holder.ivAvatar.setImageBitmap(bitmap);
                            }
                        }, new Consumer<Throwable>() {
                            @Override
                            public void accept(Throwable throwable) throws Exception {
                                holder.ivAvatar.setImageResource(R.drawable.avatar_default);
                            }
                        });
                        //设置是否被选中
                        holder.itemView.setSelected(item.isSelected);
                        //设置下分割线可视性
                        if (position == mItemList.size() - 1 || mItemList.get(position + 1).type == Item.TYPE_SEPARATOR) {
                            holder.bottomDivider.setVisibility(View.GONE);
                        } else {
                            holder.bottomDivider.setVisibility(View.VISIBLE);
                        }
                    } else {
                        //设置不可见
                        holder.itemView.setVisibility(View.GONE);
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


        private class ChatViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvMsgCount, tvSeparator;
            ImageView ivAvatar;
            View bottomDivider;

            public ChatViewHolder(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tv_name);
                tvMsgCount = itemView.findViewById(R.id.tv_msg_count);
                ivAvatar = itemView.findViewById(R.id.iv_avatar);
                tvSeparator = itemView.findViewById(R.id.tv_separator);
                bottomDivider = itemView.findViewById(R.id.divider_bottom);
                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        MasterToast.shortToast("敬请期待!");
                    }
                });
            }
        }

    }


    private Disposable mLoadListDisposable;

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        //销毁加载任务
        if (mLoadListDisposable != null && !mLoadListDisposable.isDisposed()) {
            mLoadListDisposable.dispose();
        }
    }

}
