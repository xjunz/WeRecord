package xjunz.tool.wechat.ui.activity.main;

import android.graphics.Bitmap;
import android.os.Bundle;
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
import androidx.recyclerview.widget.LinearLayoutManager;
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
import xjunz.tool.wechat.impl.model.account.Contact;
import xjunz.tool.wechat.impl.repo.ContactRepository;
import xjunz.tool.wechat.ui.activity.main.model.FilterConfig;
import xjunz.tool.wechat.ui.activity.main.model.FilterViewModel;
import xjunz.tool.wechat.ui.activity.main.model.SortBy;
import xjunz.tool.wechat.util.UiUtils;


/**
 * 显示联系人列表的{@link Fragment}，大体构建和{@link ChatFragment}类似，重复或类似的代码不再注释
 *
 * @see ChatFragment 查看相关注释
 */
public class ContactFragment extends Fragment implements FilterConfig.EventHandler {
    private RecyclerView mList;
    /**
     * 右侧的“滑块”列表，为列表提供索引以快速访问
     */
    private RecyclerView mScroller;
    private ScrollerAdapter mScrollerAdapter;
    private List<Item> mItemList;
    private List<Contact> mRawItemList;
    private ContactAdapter mMainAdapter;
    private FilterConfig mConfig;
    private List<String> mSeparatorDescCache;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FilterViewModel model = new ViewModelProvider(requireActivity(), new ViewModelProvider.NewInstanceFactory()).get(FilterViewModel.class);
        initFilterConfig();
        model.updateCurrentConfig(mConfig);
    }

    private void initFilterConfig() {
        mConfig = new FilterConfig();
        mConfig.isChat.set(false);
        mConfig.sortBy.set(SortBy.NAME);
        List<String> captionList = Contact.Type.getCaptionList();
        captionList.add(0, getString(R.string.bracketed_all));
        mConfig.categoryList.addAll(captionList);
        mConfig.sortByList.add(SortBy.NAME.caption);
        mConfig.setEventHandler(this);
    }

    private void resetFilterConfig() {
        mConfig.orderBy.set(FilterConfig.ORDER_ASCENDING);
        mConfig.categorySelection.set(0);
        mConfig.descriptionSelectionMap.clear();
    }

    @Nullable
    public FilterConfig getFilterConfig() {
        return mConfig;
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = getLayoutInflater().inflate(R.layout.fragment_contact, container, false);
        mList = root.findViewById(R.id.rv_list);
        mScroller = root.findViewById(R.id.rv_scroller);
        loadList();
        return root;
    }

    private void loadList() {
        mItemList = new ArrayList<>();
        mSeparatorDescCache = new ArrayList<>();
        mRawItemList = Objects.requireNonNull(ContactRepository.getInstance().getAll());
        Disposable disposable = Flowable.fromIterable(mRawItemList).subscribeOn(Schedulers.computation()).observeOn(AndroidSchedulers.mainThread())
                .map(new Function<Contact, Item>() {
                    @Override
                    public Item apply(Contact contact) {
                        String desc = contact.describe();
                        Item item = new Item(contact, desc, Item.TYPE_DATA);
                        if (mSeparatorDescCache.size() == 0 || !mSeparatorDescCache.contains(desc)) {
                            item.type = Item.TYPE_SEPARATOR;
                            mSeparatorDescCache.add(desc);
                        }
                        return item;
                    }
                }).subscribe(new Consumer<Item>() {
                    @Override
                    public void accept(Item item) throws Exception {
                        if (item.type == Item.TYPE_SEPARATOR) {
                            mItemList.add(item);
                        }
                        mItemList.add(new Item(item.content, item.description, Item.TYPE_DATA));
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        //异常处理
                    }
                }, new Action() {
                    @Override
                    public void run() throws Exception {
                        Contact.setOrderBy(mConfig.isAscending());
                        Collections.sort(mItemList);
                        //更新UI
                        mMainAdapter = new ContactAdapter();
                        mList.setAdapter(mMainAdapter);
                        updateCountInfo();
                        //获取所有分隔项
                        collectSeparatorDescList();
                    }
                });
    }


    private void updateCountInfo() {
        mConfig.totalCount.set(mRawItemList.size());
        mConfig.filteredCount.set(mItemList.size() - mSeparatorDescCache.size());
    }


    private void collectSeparatorDescList() {
        Collections.sort(mSeparatorDescCache);
        //直接复制已有的缓存
        List<String> descriptions = new ArrayList<>(mSeparatorDescCache);
        if (descriptions.size() == 0) {
            //如果为空，添加一个“<无>”
            descriptions.add(getString(R.string.bracketed_none));
        } else {
            //否则在第一个位置插入“<全部>”
            descriptions.add(0, getString(R.string.bracketed_all));
        }
        //添加进配置中
        mConfig.descriptionListMap.put(SortBy.NAME, descriptions);
        //初始化滑块适配器
        mScrollerAdapter = new ScrollerAdapter();
        mScroller.setAdapter(mScrollerAdapter);
        mScroller.post(new Runnable() {
            @Override
            public void run() {
                LinearLayoutManager llm = (LinearLayoutManager) Objects.requireNonNull(mList.getLayoutManager());
                int first = llm.findFirstCompletelyVisibleItemPosition();
                if (first >= 0) {
                    mScrollerAdapter.setSelectedItemIndex(mScrollerAdapter.indicatorSerial.indexOf(mItemList.get(first).description));
                }
            }
        });
        mList.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                int first = getFirstVisibleItemIndexOfList();
                if (first >= 0) {
                    int scrollIndex = mScrollerAdapter.indicatorSerial.indexOf(mItemList.get(first).description);
                    mScrollerAdapter.setSelectedItemIndex(scrollIndex);
                }
            }
        });
    }

    private void doFilter() {
        mItemList.clear();
        mSeparatorDescCache.clear();
        if (mConfig.categorySelection.get() == 0) {
            mRawItemList = ContactRepository.getInstance().getAll();
        } else {
            mRawItemList = Objects.requireNonNull(ContactRepository.getInstance().getMap().get(Contact.Type.values()[mConfig.categorySelection.get() - 1]));
        }
        final SortBy by = SortBy.NAME;
        Integer selectionOfSortBy = mConfig.descriptionSelectionMap.get(by);
        final String desOfSortBy;
        if (selectionOfSortBy != null && selectionOfSortBy != 0) {
            desOfSortBy = Objects.requireNonNull(mConfig.descriptionListMap.get(by)).get(selectionOfSortBy);
        } else {
            desOfSortBy = null;
        }
        Disposable disposable = Flowable.fromIterable(mRawItemList).subscribeOn(Schedulers.computation()).filter(new Predicate<Contact>() {
            @Override
            public boolean test(Contact contact) {
                return desOfSortBy == null || contact.describe(by).equals(desOfSortBy);
            }
        }).map(new Function<Contact, Item>() {
            @Override
            public Item apply(Contact contact) {
                String desc = contact.describe();
                Item item = new Item(contact, desc, Item.TYPE_DATA);
                if (mSeparatorDescCache.size() == 0 || !mSeparatorDescCache.contains(desc)) {
                    item.type = Item.TYPE_SEPARATOR;
                    mSeparatorDescCache.add(desc);
                }
                return item;
            }
        }).doOnNext(new Consumer<Item>() {
            @Override
            public void accept(Item item) {
                if (item.type == Item.TYPE_SEPARATOR) {
                    mItemList.add(item);
                }
                mItemList.add(new Item(item.content, item.description, Item.TYPE_DATA));
            }
        }).observeOn(AndroidSchedulers.mainThread()).doOnComplete(new Action() {
            @Override
            public void run() {
                Collections.sort(mItemList);
                updateCountInfo();
                mMainAdapter.notifyDataSetChanged();
                mScrollerAdapter.reverseIndicatorSerial(!mConfig.isAscending());
                mScrollerAdapter.notifyDataSetChanged();
            }
        }).subscribe();
    }


    private int getFirstVisibleItemIndexOfList() {
        LinearLayoutManager llm = (LinearLayoutManager) Objects.requireNonNull(mList.getLayoutManager());
        return llm.findFirstVisibleItemPosition();
    }

    private int getLastVisibleItemIndexOfList() {
        LinearLayoutManager llm = (LinearLayoutManager) Objects.requireNonNull(mList.getLayoutManager());
        return llm.findLastVisibleItemPosition();
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
        //分隔项（排序归类）
        static final int TYPE_SEPARATOR = 0x1000;
        //数据项（显示数据）
        static final int TYPE_DATA = 0x1100;
        //类型
        int type;
        //内容
        Contact content;
        //描述文字（用于排序的依据）
        String description;
        //Span文字的参数
        int spanStartIndex, spanLength;

        //是否可见（默认可见）
        boolean isVisible = true;


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
            Contact.setOrderBy(mConfig.isAscending());
            if (this.type == o.type) {
                return this.content.compareTo(o.content);
            } else {
                int descCompareRes = this.description.compareTo(o.description);
                if (descCompareRes == 0) {
                    return this.isSeparator() ? -1 : 1;
                }
                return (mConfig.isAscending() ? 1 : -1) * descCompareRes;
            }
        }

        private boolean isSeparator() {
            return this.type == TYPE_SEPARATOR;
        }

        public Item(Contact content, String desc, int type) {
            this.type = type;
            this.content = content;
            this.description = desc;
        }

    }

    private class ContactAdapter extends RecyclerView.Adapter<ContactAdapter.ContactViewHolder> {
        private int spanColor;

        private ContactAdapter() {
            spanColor = UiUtils.getAttrColor(requireContext(), R.attr.colorAccent);
        }

        @Override
        public int getItemViewType(int position) {
            return mItemList.get(position).type;
        }

        @NonNull
        @Override
        public ContactViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(viewType == Item.TYPE_DATA ? R.layout.item_contact : R.layout.item_separator, parent, false);
            return new ContactViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ContactViewHolder holder, int position, @NonNull List<Object> payloads) {
            if (payloads.size() != 0) {
                holder.itemView.setPressed(true);
                holder.itemView.setPressed(false);
            } else {
                super.onBindViewHolder(holder, position, payloads);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull final ContactAdapter.ContactViewHolder holder, int position) {
            Item item = mItemList.get(position);
            switch (item.type) {
                //如果是数据项
                case Item.TYPE_DATA:
                    final Contact content = item.content;
                    //如果可见，初始化内容
                    if (item.isVisible) {
                        holder.itemView.setVisibility(View.VISIBLE);
                        //设置名称，如果存在，设置关键字高亮
                        int startIndex = item.spanStartIndex;
                        if (startIndex >= 0) {
                            ForegroundColorSpan foregroundColorSpan = new ForegroundColorSpan(spanColor);
                            SpannableString span = new SpannableString(content.getName());
                            span.setSpan(foregroundColorSpan, item.spanStartIndex, startIndex + item.spanLength, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            holder.tvName.setText(span);
                        } else {
                            holder.tvName.setText(content.getName());
                        }
                        //异步设置头像
                        Disposable disposable = Single.create(new SingleOnSubscribe<Bitmap>() {
                            @Override
                            public void subscribe(SingleEmitter<Bitmap> emitter) throws Exception {
                                Bitmap avatar = content.getAvatar();
                                if (avatar == null) {
                                    emitter.onError(null);
                                } else {
                                    emitter.onSuccess(avatar);
                                }
                            }
                        }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(new Consumer<Bitmap>() {
                            @Override
                            public void accept(Bitmap bitmap) {
                                holder.ivAvatar.setImageBitmap(bitmap);
                            }
                        }, new Consumer<Throwable>() {
                            @Override
                            public void accept(Throwable throwable) {
                                holder.ivAvatar.setImageResource(R.drawable.avatar_default);
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

        private class ContactViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvSeparator;
            ImageView ivAvatar;
            View bottomDivider;

            public ContactViewHolder(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tv_name);
                tvSeparator = itemView.findViewById(R.id.tv_separator);
                ivAvatar = itemView.findViewById(R.id.iv_avatar);
                bottomDivider = itemView.findViewById(R.id.divider_bottom);
            }
        }
    }

    private class ScrollerAdapter extends RecyclerView.Adapter<ScrollerAdapter.ScrollerViewHolder> {
        private List<String> indicatorSerial;
        private int selectedItemIndex = -1;

        void reverseIndicatorSerial(boolean reverse) {
            if (indicatorSerial.get(0).equals("#")) {
                if (reverse) {
                    Collections.reverse(indicatorSerial);
                }
            } else {
                if (!reverse) {
                    Collections.reverse(indicatorSerial);
                }
            }
        }

        void setSelectedItemIndex(int index) {
            if (index == selectedItemIndex) {
                return;
            }
            notifyItemChanged(selectedItemIndex, false);
            selectedItemIndex = index;
            notifyItemChanged(index, true);
            mScroller.smoothScrollToPosition(selectedItemIndex);
        }


        ScrollerAdapter() {
            indicatorSerial = new ArrayList<>();
            indicatorSerial.add("#");
            for (int i = 0; i < 10; i++) {
                indicatorSerial.add(String.valueOf(i));
            }
            indicatorSerial.add("?");
            for (char c = 'A'; c <= 'Z'; c++) {
                indicatorSerial.add(String.valueOf(c));
            }
        }

        @NonNull
        @Override
        public ScrollerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ScrollerViewHolder(getLayoutInflater().inflate(R.layout.item_scroller, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ScrollerViewHolder holder, int position, @NonNull List<Object> payloads) {
            if (payloads.size() != 0) {
                holder.tvText.setSelected((Boolean) payloads.get(0));
            } else {
                super.onBindViewHolder(holder, position, payloads);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull ScrollerViewHolder holder, int position) {
            String cur = indicatorSerial.get(position);
            holder.tvText.setText(cur);
            holder.tvText.setEnabled(mSeparatorDescCache.contains(cur));
            if (position == selectedItemIndex) {
                holder.tvText.setSelected(true);
            } else {
                holder.tvText.setSelected(false);
            }
        }

        @Override
        public int getItemCount() {
            return indicatorSerial.size();
        }

        class ScrollerViewHolder extends RecyclerView.ViewHolder {
            TextView tvText;

            /**
             * 二分法查找某个指定描述的分隔项的位置
             *
             * @param desc 指定的描述
             * @return 索引
             */
            private int binarySearch(String desc) {
                int l = 0;
                int r = mItemList.size() - 1;
                int m = (l + r) / 2;
                while (l <= r) {
                    int res = mItemList.get(m).description.compareTo(desc);
                    if (res > 0) {
                        if (mConfig.isAscending()) {
                            r = m - 1;
                        } else {
                            l = m + 1;
                        }
                    } else if (res < 0) {
                        if (mConfig.isAscending()) {
                            l = m + 1;
                        } else {
                            r = m - 1;
                        }
                    } else {
                        break;
                    }
                    m = (l + r) / 2;
                }
                //没找到
                if (l > r) {
                    return -1;
                }
                //找到某个描述相同的项，继续查找该项之前的分隔项
                while (!mItemList.get(m).isSeparator()) {
                    m = m - 1;
                }
                return m;
            }

            public ScrollerViewHolder(@NonNull View itemView) {
                super(itemView);
                tvText = itemView.findViewById(R.id.tv_text);
                tvText.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int posOfClickedItem = getAdapterPosition();
                        String descOfClickedItem = indicatorSerial.get(posOfClickedItem);
                        int targetIndex = binarySearch(descOfClickedItem);
                        if (targetIndex < 0) {
                            throw new RuntimeException("Index of the item scrolling to not found but expected to exist! ");
                        }
                        int firstVisibleItemOfMainList = getFirstVisibleItemIndexOfList();
                        int lastVisibleItemOfMainList = getLastVisibleItemIndexOfList();
                        //如果目标Item可见，直接提醒一下
                        if (targetIndex <= lastVisibleItemOfMainList && targetIndex >= firstVisibleItemOfMainList) {
                            mMainAdapter.notifyItemChanged(targetIndex + 1, true);
                        } else {
                            //如果是目标Item在当前列表下面，调整目标Item后移一个，保证可以看到数据项
                            if (targetIndex > lastVisibleItemOfMainList) {
                                targetIndex += 1;
                            }
                            //和当前Item距离太远直接跳转，不要滑动了，太慢
                            if (Math.abs(firstVisibleItemOfMainList - targetIndex) > 2 * (lastVisibleItemOfMainList - firstVisibleItemOfMainList)) {
                                mList.scrollToPosition(targetIndex);
                            } else {
                                mList.smoothScrollToPosition(targetIndex);
                            }
                        }

                        LinearLayoutManager llm = (LinearLayoutManager) Objects.requireNonNull(mScroller.getLayoutManager());
                        int f = llm.findFirstVisibleItemPosition();
                        int l = llm.findLastVisibleItemPosition();
                        if (posOfClickedItem == l && posOfClickedItem != getItemCount() - 1) {
                            mScroller.smoothScrollToPosition(posOfClickedItem + 1);
                        } else if (posOfClickedItem == f && f != 0) {
                            mScroller.smoothScrollToPosition(posOfClickedItem - 1);
                        }
                    }
                });
            }
        }
    }
}
