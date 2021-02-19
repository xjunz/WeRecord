/*
 * Copyright (c) 2021 xjunz. 保留所有权利
 */

package xjunz.tool.werecord.ui.main.fragment;

import android.annotation.SuppressLint;
import android.app.ActivityOptions;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.FlowableEmitter;
import xjunz.tool.werecord.R;
import xjunz.tool.werecord.data.viewmodel.PageConfig;
import xjunz.tool.werecord.data.viewmodel.SortBy;
import xjunz.tool.werecord.databinding.FragmentChatBinding;
import xjunz.tool.werecord.impl.DatabaseModifier;
import xjunz.tool.werecord.impl.Environment;
import xjunz.tool.werecord.impl.model.account.Contact;
import xjunz.tool.werecord.impl.model.account.Talker;
import xjunz.tool.werecord.impl.model.export.ExporterRegistry;
import xjunz.tool.werecord.impl.model.export.MessageExporter;
import xjunz.tool.werecord.impl.repo.RepositoryFactory;
import xjunz.tool.werecord.impl.repo.TalkerRepository;
import xjunz.tool.werecord.ui.customview.MasterToast;
import xjunz.tool.werecord.ui.export.MessageExportActivity;
import xjunz.tool.werecord.ui.main.DetailActivity;
import xjunz.tool.werecord.util.IoUtils;
import xjunz.tool.werecord.util.RxJavaUtils;
import xjunz.tool.werecord.util.UiUtils;

/**
 * 显示会话列表的{@link Fragment}
 */
public class ChatFragment extends ListPageFragment<Talker> {

    private ChatAdapter mAdapter;
    private TalkerRepository mRepository;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mRepository = RepositoryFactory.get(TalkerRepository.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return FragmentChatBinding.inflate(inflater, container, false).getRoot();
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
        return mRepository.getAllOfType(type);
    }

    @Override
    public List<Talker> getAll() {
        return mRepository.getAll();
    }

    @SuppressLint("NonConstantResourceId")
    public void onOptionMenuClicked(@NotNull MenuItem menuItem, View itemView) {
        int id = menuItem.getItemId();
        if (id == R.id.item_export_message) {
            List<Talker> selected = new ArrayList<>();
            for (Item item : mItemList) {
                if (item.isSelected()) {
                    selected.add(item.content);
                }
            }
            Intent intent = new Intent(requireActivity(), MessageExportActivity.class);
            ExporterRegistry.getInstance().register(new MessageExporter(selected));
            itemView.setTransitionName(getString(R.string.tn_source_list));
            intent.putExtra(MessageExportActivity.EXTRA_FROM_MULTI_SELECTION, true);
            startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(requireActivity(), itemView, itemView.getTransitionName()).toBundle());
        } else {
            AlertDialog.Builder builder;
            if (id == R.id.item_delete_chat) {
                builder = UiUtils.createCaveat(requireContext(), R.string.warning_delete_chat);
            } else {
                builder = UiUtils.createAlert(requireContext(), getString(R.string.alert_chat_option, menuItem.getTitle()));
            }
            builder.setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                        AtomicBoolean modified = new AtomicBoolean(false);
                        Dialog progress = UiUtils.createProgress(requireContext(), R.string.executing);
                        progress.show();
                        RxJavaUtils.flow((FlowableEmitter<Integer> emitter) -> {
                            DatabaseModifier modifier = Environment.getInstance().modifyDatabase();
                            for (int i = 0; i < mItemList.size(); i++) {
                                Item item = mItemList.get(i);
                                if (item.isSelected()) {
                                    Talker talker = item.content;
                                    switch (id) {
                                        case R.id.item_mark_as_read:
                                            if (talker.isUnread()) {
                                                modifier.markAsRead(talker);
                                                talker.markAsRead();
                                                emitter.onNext(i);
                                                modified.set(true);
                                            }
                                            break;
                                        case R.id.item_mark_as_unread:
                                            if (!talker.isUnread()) {
                                                modifier.markAsUnread(talker, 1);
                                                talker.setUnreadCount(1);
                                                emitter.onNext(i);
                                                modified.set(true);
                                            }
                                            break;
                                        case R.id.item_delete_chat:
                                            modifier.deleteConversationWithMessages(item.content);
                                            mRepository.remove(item.content);
                                            modified.set(true);
                                            break;
                                        case R.id.item_hide_chat:
                                            if (!talker.isHidden()) {
                                                modifier.hideConversation(talker);
                                                talker.setHidden(true);
                                                emitter.onNext(i);
                                                modified.set(true);
                                            }
                                            break;
                                        case R.id.item_reshow_chat:
                                            if (talker.isHidden()) {
                                                modifier.reshowConversation(talker);
                                                talker.setHidden(false);
                                                emitter.onNext(i);
                                                modified.set(true);
                                            }
                                            break;
                                    }
                                }
                            }
                            if (modified.get()) {
                                modifier.apply();
                            }
                            emitter.onComplete();
                        }).doFinally(progress::dismiss).subscribe(new RxJavaUtils.FlowableSubscriberAdapter<Integer>() {
                            @Override
                            public void onNext(Integer index) {
                                switch (id) {
                                    case R.id.item_delete_chat:
                                        //...see onComplete()
                                        break;
                                    case R.id.item_hide_chat:
                                    case R.id.item_reshow_chat:
                                        mAdapter.notifyItemChanged(index, "hidden");
                                        break;
                                    case R.id.item_mark_as_read:
                                    case R.id.item_mark_as_unread:
                                        mAdapter.notifyItemChanged(index, "unreadCount");
                                        break;
                                }
                            }

                            @Override
                            public void onComplete() {
                                if (modified.get()) {
                                    UiUtils.createLaunch(requireContext()).show();
                                    if (id == R.id.item_delete_chat) {
                                        mHost.quitMultiSelectionMode();
                                        reloadList();
                                    }
                                } else {
                                    MasterToast.shortToast(R.string.no_change_was_made);
                                }
                            }

                            @Override
                            public void onError(@NotNull Throwable e) {
                                UiUtils.createError(requireContext(), IoUtils.readStackTraceFromThrowable(e)).show();
                            }
                        });
                    });
            builder.show();
        }
    }

    private class ChatAdapter extends ListPageAdapter<ChatAdapter.ChatViewHolder> {

        @NonNull
        @Override
        public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(viewType == Item.TYPE_DATA ? R.layout.item_chat : R.layout.item_separator, parent, false);
            return new ChatViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ChatViewHolder holder, int position, @NonNull List<Object> payloads) {
            if (payloads.size() != 0) {
                Object payload = payloads.get(0);
                if (payloads.get(0) instanceof String) {
                    if (((String) payload).equals("hidden")) {
                        holder.ivHidden.setVisibility(mItemList.get(position).content.isHidden() ? View.VISIBLE : View.GONE);
                        return;
                    } else if (((String) payload).equals("unreadCount")) {
                        Talker talker = mItemList.get(position).content;
                        holder.tvUnreadCount.setText(String.valueOf(talker.getUnreadCount()));
                        holder.tvUnreadCount.setVisibility(talker.isUnread() ? View.VISIBLE : View.GONE);
                        return;
                    }
                }
            }
            super.onBindViewHolder(holder, position, payloads);
        }

        @Override
        public void onBindViewHolder(@NonNull final ChatViewHolder holder, int position) {
            super.onBindViewHolder(holder, position);
            Item item = mItemList.get(position);
            if (item.isData()) {
                Talker talker = item.content;
                //设置格式化后的时间
                holder.tvTime.setText(talker.formatTimestamp);
                //设置记录数
                holder.tvMsgCount.setText(Html.fromHtml(getString(R.string.format_total_records, talker.messageCount)));
                //是否为隐藏的消息
                holder.ivHidden.setVisibility(talker.isHidden() ? View.VISIBLE : View.GONE);
                //未读消息
                holder.tvUnreadCount.setText(String.valueOf(talker.getUnreadCount()));
                holder.tvUnreadCount.setVisibility(talker.isUnread() ? View.VISIBLE : View.GONE);
            }
        }

        private class ChatViewHolder extends ListPageViewHolder {
            TextView tvMsgCount, tvTime, tvUnreadCount;
            ImageView ivHidden;

            public ChatViewHolder(@NonNull View itemView) {
                super(itemView);
                tvMsgCount = itemView.findViewById(R.id.tv_msg_count);
                tvTime = itemView.findViewById(R.id.tv_time);
                ivHidden = itemView.findViewById(R.id.iv_hidden);
                tvUnreadCount = itemView.findViewById(R.id.tv_unread_count);
                itemView.setOnClickListener(v -> {
                    if (getItemViewType() == Item.TYPE_DATA) {
                        if (mConfig.isInMultiSelectionMode.get()) {
                            Item item = mItemList.get(getAdapterPosition());
                            if (item.isSelected()) {
                                item.setSelected(false);
                                mConfig.decreaseSelection();
                                if (mConfig.selectionCount.get() == 0) {
                                    mHost.quitMultiSelectionMode();
                                }
                            } else {
                                item.setSelected(true);
                                mConfig.increaseSelection();
                            }
                            itemView.setSelected(item.isSelected());
                        } else {
                            ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(requireActivity(), ivAvatar, ivAvatar.getTransitionName());
                            Intent i = new Intent(requireActivity(), DetailActivity.class);
                            i.putExtra(DetailActivity.EXTRA_CONTACT, mItemList.get(getAdapterPosition()).content);
                            startActivity(i, options.toBundle());
                        }
                    }
                });
                itemView.setOnLongClickListener(v -> {
                    if (getItemViewType() == Item.TYPE_DATA) {
                        Item item = mItemList.get(getAdapterPosition());
                        if (!item.isSelected() && !mConfig.isInMultiSelectionMode.get()) {
                            mHost.enterMultiSelectionMode();
                        }
                        v.performClick();
                    }
                    return true;
                });
            }
        }
    }

}
