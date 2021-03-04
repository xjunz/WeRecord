/*
 * Copyright (c) 2021 xjunz. 保留所有权利
 */
package xjunz.tool.werecord.ui.message.fragment;

import android.annotation.SuppressLint;
import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.OneShotPreDrawListener;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import xjunz.tool.werecord.R;
import xjunz.tool.werecord.databinding.FragmentStatsBinding;
import xjunz.tool.werecord.databinding.ItemChartMsgCountBinding;
import xjunz.tool.werecord.databinding.ItemChartMsgCountCollapsedBinding;
import xjunz.tool.werecord.databinding.ItemChartMsgTypeBinding;
import xjunz.tool.werecord.impl.model.account.Account;
import xjunz.tool.werecord.impl.model.account.Group;
import xjunz.tool.werecord.impl.model.message.Message;
import xjunz.tool.werecord.impl.model.message.MessageFactory;
import xjunz.tool.werecord.impl.repo.ContactRepository;
import xjunz.tool.werecord.impl.repo.GroupRepository;
import xjunz.tool.werecord.impl.repo.RepositoryFactory;
import xjunz.tool.werecord.ui.main.DetailActivity;
import xjunz.tool.werecord.ui.message.MessageActivity;
import xjunz.tool.werecord.ui.viewmodel.MessageViewModel;
import xjunz.tool.werecord.util.RxJavaUtils;
import xjunz.tool.werecord.util.UiUtils;
import xjunz.tool.werecord.util.Utils;

public class StatisticsFragment extends Fragment {
    private MessageViewModel mModel;
    private MessageActivity mHost;
    private List<Message> mAll;
    private FragmentStatsBinding mBinding;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mModel = Utils.getViewModel(requireActivity(), MessageViewModel.class);
        mHost = (MessageActivity) requireActivity();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = FragmentStatsBinding.inflate(inflater);
        mBinding.setHost(this);
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        //xml中设置minimumHeight:match_parent不生效，那么我们手动让Container填满NestedScrollView
        OneShotPreDrawListener.add(mBinding.nsvStats, () -> mBinding.statsContainer.setMinimumHeight(mBinding.nsvStats.getHeight()));
    }

    private void analyze() {
        mAll = mModel.currentLoadedMessages;
        analyzeMsgCountStats();
        analyzeMsgTypeStats();
    }

    public void startAnalyze(@NotNull View view) {
        view.setEnabled(false);
        if (!mModel.hasLoadedAll.get()) {
            mHost.loadAllMessages(this::analyze);
        } else {
            analyze();
        }
    }

    private void analyzeMsgTypeStats() {
        final List<MsgTypeStat> stats = new ArrayList<>();
        RxJavaUtils.complete(() -> {
            for (Message message : mAll) {
                MessageFactory.Type type = message.getType();
                boolean exists = false;
                for (MsgTypeStat stat : stats) {
                    if (Objects.equals(type, stat.type)) {
                        stat.increment();
                        exists = true;
                        break;
                    }
                }
                if (!exists) {
                    stats.add(new MsgTypeStat(type));
                }
            }
            MsgTypeStat.totalCount = mAll.size();
            Collections.sort(stats);
        }).subscribe(new RxJavaUtils.CompletableObservableAdapter() {
            @Override
            public void onComplete() {
                mBinding.rvStatsMsgType.setAdapter(new MsgStatAdapter(stats, MsgTypeStat.totalCount, false));
            }
        });
    }

    private void analyzeMsgCountStats() {
        final List<MsgCountStat> stats = new ArrayList<>();
        int[] count = new int[2];
        RxJavaUtils.complete(() -> {
            int sysCount = 0;
            for (Message message : mAll) {
                String id = message.getSenderId();
                if (id == null || (message.isInGroupChat() && id.equals(mModel.currentTalker.id))) {
                    sysCount += 1;
                } else {
                    boolean exists = false;
                    for (MsgCountStat stat : stats) {
                        if (Objects.equals(id, stat.sender)) {
                            stat.increment();
                            exists = true;
                            break;
                        }
                    }
                    if (!exists) {
                        stats.add(new MsgCountStat(id));
                    }
                }
            }
            int userCount = mAll.size() - sysCount;
            count[0] = userCount;
            MsgCountStat.totalCount = userCount;
            Collections.sort(stats);
            float sum = 0f;
            int showCount = 0;
            int size = stats.size();
            for (int i = 0; i < size; i++) {
                sum += stats.get(i).getFraction();
                if (sum >= .75f) {
                    showCount = i + 1;
                    break;
                }
            }
            showCount = Math.min(size, Math.max(5, Math.min(showCount, 10)));
            if (showCount != size && size - showCount < 3) {
                showCount = size - 3;
            }
            count[1] = showCount;
        }).subscribe(new RxJavaUtils.CompletableObservableAdapter() {
            @Override
            public void onComplete() {
                UiUtils.fadeOut(mBinding.mask);
                if (mModel.currentTalker.isGroup()) {
                    Group group = RepositoryFactory.get(GroupRepository.class).get(mModel.currentTalker.id);
                    mBinding.tvOverview.setText(getString(R.string.format_group_stats_overview, count[0], group.memberCount, stats.size()));
                } else {
                    mBinding.tvOverview.setText(getString(R.string.format_stats_overview, count[0]));
                }
                mBinding.tvTimeSpan.setText(getString(R.string.format_from_to, Utils.formatDate(mAll.get(mAll.size() - 1).getCreateTimeStamp()), Utils.formatDate(mAll.get(0).getCreateTimeStamp())));
                mBinding.rvStatsMsgCount.setAdapter(new MsgStatAdapter(stats, count[1], true));
            }
        });
    }

    public void showCollapsedSenders(View view, @NotNull List<?> list) {
        PopupMenu menu = new PopupMenu(requireContext(), view);
        for (int i = 0; i < list.size(); i++) {
            MsgCountStat stat = (MsgCountStat) list.get(i);
            Account account = stat.getSender();
            menu.getMenu().add(0, i, 0, (account == null ? stat.sender : account.getName()) + String.format(" (%s)", getString(R.string.format_msg_count, stat.count)));
        }
        menu.setOnMenuItemClickListener(item -> {
            Intent intent = new Intent(requireActivity(), DetailActivity.class);
            intent.putExtra(DetailActivity.EXTRA_CONTACT_ID, ((MsgCountStat) list.get(item.getItemId())).sender);
            startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(requireActivity()).toBundle());
            return true;
        });
        menu.setGravity(Gravity.END);
        menu.show();
    }

    public void showSenderDetail(@NotNull MsgStat stat, View sharedElement) {
        Intent intent = new Intent(requireActivity(), DetailActivity.class);
        intent.putExtra(DetailActivity.EXTRA_CONTACT_ID, ((MsgCountStat) stat).sender);
        startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(requireActivity(), sharedElement, sharedElement.getTransitionName()).toBundle());
    }

    public static abstract class MsgStat implements Comparable<MsgStat> {
        public int count = 1;

        public void increment() {
            count += 1;
        }

        public abstract String getName();

        protected abstract int getTotalCount();

        public float getFraction() {
            return (float) count / getTotalCount();
        }

        @SuppressLint("DefaultLocale")
        public String getPercentage() {
            return String.format("%.2f%%", getFraction() * 100);
        }

        @Override
        public int compareTo(@NotNull MsgStat o) {
            //降序，加负号
            return -Integer.compare(this.count, o.count);
        }
    }

    public static class MsgCountStat extends MsgStat {
        private final String sender;
        private static int totalCount;

        public MsgCountStat(String sender) {
            this.sender = sender;
        }

        public Account getSender() {
            return RepositoryFactory.get(ContactRepository.class).get(sender);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MsgCountStat stat = (MsgCountStat) o;
            return Objects.equals(sender, stat.sender);
        }

        @Override
        public int hashCode() {
            return Objects.hash(sender);
        }


        @Override
        public String getName() {
            return getSender().getName();
        }

        @Override
        protected int getTotalCount() {
            return totalCount;
        }
    }

    public static class MsgTypeStat extends MsgStat {
        public MessageFactory.Type type;
        private static int totalCount;

        public MsgTypeStat(MessageFactory.Type type) {
            this.type = type;
        }

        @Override
        public String getName() {
            return type.getCaption();
        }

        @Override
        protected int getTotalCount() {
            return totalCount;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MsgTypeStat stat = (MsgTypeStat) o;
            return Objects.equals(type, stat.type);
        }

        @Override
        public int hashCode() {
            return Objects.hash(type);
        }
    }

    private class MsgStatAdapter extends RecyclerView.Adapter<MsgStatAdapter.MsgStatViewHolder> {
        private final List<? extends MsgStat> mStats;
        private final int mShowCount;
        private final boolean mCollapsed;
        private final boolean mIsMsgCountChart;
        private final int TYPE_NORMAL = 0;
        private final int TYPE_COLLAPSED = 1;

        @Override
        public int getItemViewType(int position) {
            return mCollapsed && position == mShowCount ? TYPE_COLLAPSED : TYPE_NORMAL;
        }

        private MsgStatAdapter(List<? extends MsgStat> stats, int showCount, boolean count) {
            mStats = stats;
            mShowCount = showCount;
            mCollapsed = mShowCount < mStats.size();
            mIsMsgCountChart = count;
        }

        @NonNull
        @Override
        public MsgStatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == TYPE_COLLAPSED) {
                return new MsgStatViewHolder((ItemChartMsgCountCollapsedBinding) DataBindingUtil.inflate(getLayoutInflater(), R.layout.item_chart_msg_count_collapsed, parent, false));
            } else {
                if (mIsMsgCountChart) {
                    return new MsgStatViewHolder((ItemChartMsgCountBinding) DataBindingUtil.inflate(getLayoutInflater(), R.layout.item_chart_msg_count, parent, false));
                } else {
                    return new MsgStatViewHolder((ItemChartMsgTypeBinding) DataBindingUtil.inflate(getLayoutInflater(), R.layout.item_chart_msg_type, parent, false));
                }
            }
        }

        @Override
        public void onBindViewHolder(@NonNull MsgStatViewHolder holder, int position) {
            if (getItemViewType(position) == TYPE_NORMAL) {
                MsgStat stat = mStats.get(position);
                if (mIsMsgCountChart) {
                    holder.msgCountBinding.setStat((MsgCountStat) stat);
                    holder.msgCountBinding.chartBar.animateProgress();
                    holder.msgCountBinding.executePendingBindings();
                } else {
                    holder.msgTypeBinding.setStat(stat);
                    holder.msgTypeBinding.chartBar.animateProgress();
                    holder.msgTypeBinding.executePendingBindings();
                }
            } else {
                holder.collapsedBinding.setCollapsedStats(mStats.subList(mShowCount, mStats.size()));
                holder.collapsedBinding.executePendingBindings();
            }
        }

        @Override
        public int getItemCount() {
            return mCollapsed ? mShowCount + 1 : mStats.size();
        }

        private class MsgStatViewHolder extends RecyclerView.ViewHolder {
            ItemChartMsgCountBinding msgCountBinding;
            ItemChartMsgTypeBinding msgTypeBinding;
            ItemChartMsgCountCollapsedBinding collapsedBinding;

            public MsgStatViewHolder(@NonNull ItemChartMsgCountBinding binding) {
                super(binding.getRoot());
                this.msgCountBinding = binding;
                this.msgCountBinding.setHost(StatisticsFragment.this);
            }

            public MsgStatViewHolder(@NonNull ItemChartMsgCountCollapsedBinding binding) {
                super(binding.getRoot());
                this.collapsedBinding = binding;
                this.collapsedBinding.setHost(StatisticsFragment.this);
            }

            public MsgStatViewHolder(@NonNull ItemChartMsgTypeBinding binding) {
                super(binding.getRoot());
                this.msgTypeBinding = binding;
            }
        }
    }
}
