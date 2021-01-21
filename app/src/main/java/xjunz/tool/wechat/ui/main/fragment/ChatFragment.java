/*
 * Copyright (c) 2021 xjunz. 保留所有权利
 */

package xjunz.tool.wechat.ui.main.fragment;

import android.text.Html;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

import xjunz.tool.wechat.R;
import xjunz.tool.wechat.data.viewmodel.PageConfig;
import xjunz.tool.wechat.data.viewmodel.SortBy;
import xjunz.tool.wechat.impl.model.account.Contact;
import xjunz.tool.wechat.impl.model.account.Talker;
import xjunz.tool.wechat.impl.repo.RepositoryFactory;
import xjunz.tool.wechat.impl.repo.TalkerRepository;


/**
 * 显示会话列表的{@link Fragment}
 */
public class ChatFragment extends ListPageFragment<Talker> {

    private ChatAdapter mAdapter;

    @Override
    public int getLayoutResource() {
        return R.layout.fragment_chat;
    }

    @Override
    public SortBy[] getSortByList() {
        return SortBy.values();
    }

    @Override
    public Contact.Type[] getTypeList() {
        //null表示全部匹配
        return new Contact.Type[]{null, Contact.Type.FRIEND, Contact.Type.JOINED_GROUP, Contact.Type.FOLLOWING_GZH, Contact.Type.SERVICE};
    }

    @Override
    public PageConfig getInitialConfig() {
        PageConfig config = new PageConfig();
        config.caption = getString(R.string.chat);
        config.isChat.set(true);
        config.sortBy.set(SortBy.TIMESTAMP);
        List<String> captionList = Arrays.asList(getResources().getStringArray(R.array.type_talker));
        config.typeList.addAll(captionList);
        config.sortByList.addAll(SortBy.getCaptionList());
        config.setEventHandler(this);
        return config;
    }


    @Override
    public ChatAdapter getAdapter() {
        return mAdapter = mAdapter == null ? new ChatAdapter() : mAdapter;
    }

    @Override
    public void resetFilterConfig(@NotNull PageConfig config) {
        config.sortBy.set(SortBy.TIMESTAMP);
        config.orderBy.set(PageConfig.ORDER_ASCENDING);
        config.typeSelection.set(0);
        config.descriptionSelectionMap.clear();
    }


    @Override
    public List<Talker> getAllOfType(@NonNull Contact.Type type) {
        return RepositoryFactory.get(TalkerRepository.class).getAllOfType(type);
    }

    @Override
    public List<Talker> getAll() {
        return RepositoryFactory.get(TalkerRepository.class).getAll();
    }


    private class ChatAdapter extends ListPageAdapter<ChatAdapter.ChatViewHolder> {

        @NonNull
        @Override
        public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(viewType == Item.TYPE_DATA ? R.layout.item_chat : R.layout.item_separator, parent, false);
            return new ChatViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull final ChatViewHolder holder, int position) {
            super.onBindViewHolder(holder, position);
            Item item = mItemList.get(position);
            if (item.isData()) {
                //设置格式化后的时间
                holder.tvTime.setText(item.content.formatTimestamp);
                //设置记录数
                holder.tvMsgCount.setText(Html.fromHtml(getString(R.string.format_total_records, item.content.messageCount)));
                //是否为隐藏的消息
                holder.ivHidden.setVisibility(item.content.isHidden() ? View.VISIBLE : View.GONE);
            }
        }

        private class ChatViewHolder extends ListPageViewHolder {
            TextView tvMsgCount, tvTime;
            ImageView ivHidden;

            public ChatViewHolder(@NonNull View itemView) {
                super(itemView);
                tvMsgCount = itemView.findViewById(R.id.tv_msg_count);
                tvTime = itemView.findViewById(R.id.tv_time);
                ivHidden = itemView.findViewById(R.id.iv_hidden);
            }
        }
    }

}
