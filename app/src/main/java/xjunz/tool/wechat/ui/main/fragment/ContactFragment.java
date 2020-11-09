/*
 * Copyright (c) 2020 xjunz. 保留所有权利
 */

package xjunz.tool.wechat.ui.main.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import xjunz.tool.wechat.R;
import xjunz.tool.wechat.data.viewmodel.PageConfig;
import xjunz.tool.wechat.data.viewmodel.SortBy;
import xjunz.tool.wechat.impl.model.account.Contact;
import xjunz.tool.wechat.impl.repo.ContactRepository;
import xjunz.tool.wechat.impl.repo.RepositoryFactory;

import static xjunz.tool.wechat.util.UiUtils.getFirstVisibleItemIndexOfList;
import static xjunz.tool.wechat.util.UiUtils.getLastVisibleItemIndexOfList;


/**
 * 显示联系人列表的{@link Fragment}
 */
public class ContactFragment extends ListPageFragment<Contact> implements PageConfig.EventHandler {
    /**
     * 右侧的“指示器（indicator）”列表，为列表提供索引以快速访问
     */
    private RecyclerView mScroller;
    private ScrollerAdapter mScrollerAdapter;
    private ContactAdapter mAdapter;

    @NotNull
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        mScroller = view.findViewById(R.id.rv_scroller);
        mList.addOnScrollListener(new RecyclerView.OnScrollListener() {
            /**
             * 当用户开始滑动时，如果当前指示器位置不可见（用户滑动了指示器列表），则滑动到当前指示器的位置
             */
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    int firstIndicatorIndex = getFirstVisibleItemIndexOfList(mScroller, true);
                    int lastIndicatorIndex = getLastVisibleItemIndexOfList(mScroller, true);
                    if (firstIndicatorIndex >= 0 && lastIndicatorIndex >= 0) {
                        int currentIndicatorIndex = mScrollerAdapter.selectedItemIndex;
                        if (currentIndicatorIndex < firstIndicatorIndex || currentIndicatorIndex > lastIndicatorIndex) {
                            mScroller.smoothScrollToPosition(currentIndicatorIndex);
                        }
                    }
                }
            }

            /**
             * 根据当前主列表第一个可见的Item设置当前指示器
             */
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                int first = getFirstVisibleItemIndexOfList(mList, false);
                if (first >= 0) {
                    int scrollIndex = mScrollerAdapter.getIndexOfIndicator(mItemList.get(first).description);
                    mScrollerAdapter.setSelectedItemIndex(scrollIndex);
                }
            }
        });
        return view;
    }

    @Override
    public List<Contact> getAllOfType(@NonNull Contact.Type type) {
        return RepositoryFactory.get(ContactRepository.class).getAllOfType(type);
    }

    @Override
    public List<Contact> getAll() {
        return RepositoryFactory.get(ContactRepository.class).getAll();
    }

    @Override
    public int getLayoutResource() {
        return R.layout.fragment_contact;
    }

    @Override
    public SortBy[] getSortByList() {
        return new SortBy[]{SortBy.NAME};
    }

    @Override
    public Contact.Type[] getTypeList() {
        return new Contact.Type[]{Contact.Type.FRIEND, Contact.Type.JOINED_GROUP, Contact.Type.FOLLOWING_GZH,};
    }

    @Override
    public void initPageConfig(PageConfig config) {
        config.caption = getString(R.string.contact);
        config.isChat.set(false);
        config.sortBy.set(SortBy.NAME);
        List<String> captionList = Contact.Type.getCaptionList(getTypeList());
        config.typeList.addAll(captionList);
        config.sortByList.add(SortBy.NAME.caption);
        config.setEventHandler(this);
    }


    @Override
    public ContactAdapter getAdapter() {
        return mAdapter = mAdapter == null ? new ContactAdapter() : mAdapter;
    }


    @Override
    public void resetFilterConfig(PageConfig config) {
        config.orderBy.set(PageConfig.ORDER_ASCENDING);
        config.typeSelection.set(0);
        config.descriptionSelectionMap.clear();
    }


    @Override
    protected void collectSeparatorDescListMap() {
        super.collectSeparatorDescListMap();
        //初始化滑块适配器
        mScrollerAdapter = new ScrollerAdapter();
        mScroller.setAdapter(mScrollerAdapter);
        mScroller.post(() -> {
            int first = getFirstVisibleItemIndexOfList(mList, true);
            if (first >= 0) {
                mScrollerAdapter.setSelectedItemIndex(mScrollerAdapter.getIndexOfIndicator(mItemList.get(first).description));
            }
        });
    }


    @Override
    protected void updateList(@NotNull List<Item> newItemList) {
        super.updateList(newItemList);
        mScrollerAdapter.reverseIndicatorSerial(!getCurrentConfig().isAscending());
        mScrollerAdapter.notifyDataSetChanged();
    }


    private class ContactAdapter extends ListPageAdapter<ListPageViewHolder> {

        @Override
        public void onBindViewHolder(@NonNull ListPageViewHolder holder, int position, @NonNull List<Object> payloads) {
            if (payloads.size() != 0) {
                holder.itemView.setPressed(true);
                holder.itemView.setPressed(false);
            } else {
                super.onBindViewHolder(holder, position, payloads);
            }
        }

        @NonNull
        @Override
        public ListPageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(viewType == Item.TYPE_DATA ? R.layout.item_contact : R.layout.item_separator, parent, false);
            return new ListPageViewHolder(view);
        }
    }

    private class ScrollerAdapter extends RecyclerView.Adapter<ScrollerAdapter.ScrollerViewHolder> {
        private final List<String> indicatorSerial;
        private int selectedItemIndex = -1;

        int getIndexOfIndicator(String indicator) {
            return indicatorSerial.indexOf(indicator);
        }

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
            holder.tvText.setEnabled(mCurrentDescList.contains(cur));
            holder.tvText.setSelected(position == selectedItemIndex);
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
                        if (getCurrentConfig().isAscending()) {
                            r = m - 1;
                        } else {
                            l = m + 1;
                        }
                    } else if (res < 0) {
                        if (getCurrentConfig().isAscending()) {
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
                tvText.setOnClickListener(v -> {
                    int posOfClickedItem = getAdapterPosition();
                    String descOfClickedItem = indicatorSerial.get(posOfClickedItem);
                    int targetIndex = binarySearch(descOfClickedItem);
                    if (targetIndex < 0) {
                        throw new RuntimeException("Index of the item scrolling to not found but expected to exist! ");
                    }
                    int firstVisibleItemOfMainList = getFirstVisibleItemIndexOfList(mList, true);
                    int lastVisibleItemOfMainList = getLastVisibleItemIndexOfList(mList, false);
                    //如果目标Item可见，直接提醒一下
                    if (targetIndex <= lastVisibleItemOfMainList && targetIndex >= firstVisibleItemOfMainList) {
                        mList.smoothScrollToPosition(targetIndex + 1);
                        mAdapter.notifyItemChanged(targetIndex + 1, true);
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
                });
            }
        }
    }
}
