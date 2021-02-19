/*
 * Copyright (c) 2021 xjunz. 保留所有权利
 */
package xjunz.tool.werecord.ui.message.fragment;

import android.app.Dialog;
import android.os.Bundle;
import android.transition.AutoTransition;
import android.transition.Scene;
import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.Observable;
import androidx.databinding.ObservableInt;
import androidx.fragment.app.Fragment;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import androidx.recyclerview.widget.RecyclerView;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import xjunz.tool.werecord.R;
import xjunz.tool.werecord.data.viewmodel.MessageViewModel;
import xjunz.tool.werecord.databinding.FragmentEditionBinding;
import xjunz.tool.werecord.databinding.ItemEditionBinding;
import xjunz.tool.werecord.impl.DatabaseModifier;
import xjunz.tool.werecord.impl.Environment;
import xjunz.tool.werecord.impl.model.message.Message;
import xjunz.tool.werecord.impl.model.message.util.Edition;
import xjunz.tool.werecord.ui.message.MessageActivity;
import xjunz.tool.werecord.util.RxJavaUtils;
import xjunz.tool.werecord.util.UiUtils;
import xjunz.tool.werecord.util.Utils;

/**
 * 显示当前{@link xjunz.tool.werecord.impl.model.account.Talker}的消息中被编辑过的消息的{@link Fragment}。
 */
public class EditionFragment extends Fragment {
    private FragmentEditionBinding mBinding;
    private MessageViewModel mModel;
    private EditionAdapter mAdapter;
    private final List<EditionItem> mConfirmedEditionItemList = new ArrayList<>();
    private final List<EditionItem> mUnconfirmedEditionItemList = new ArrayList<>();
    private List<EditionItem> mCurrentEditionItemList = new ArrayList<>();
    private DatabaseModifier mModifier;
    /**
     * 选中确认的Editions（0）还是未确认的Editions（1）
     */
    public final ObservableInt editionSetSelection = new ObservableInt(0);
    /**
     * 选中的Edition类型
     */
    public final ObservableInt editionFlagSelection = new ObservableInt(0);
    public static final int EDITION_SET_INDEX_CONFIRMED = 0;
    public static final int EDITION_SET_INDEX_UNCONFIRMED = 1;
    private MessageActivity mHost;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mModifier = Environment.getInstance().modifyDatabase();
        mModel = Utils.getViewModel(requireActivity(), MessageViewModel.class);
        mModel.handleEvent(mHandler);
        mHost = (MessageActivity) requireActivity();
    }


    public void restoreMessage(int position, Message msg) {
        //如果是未确认的还原，交由Host处理
        if (editionSetSelection.get() == EDITION_SET_INDEX_UNCONFIRMED) {
            int index = mModel.currentLoadedMessages.indexOf(msg);
            if (index >= 0) {
                mHost.restore(index, msg);
                mUnconfirmedEditionItemList.remove(position);
                mAdapter.notifyItemRemoved(position);
            } else {
                throw new IllegalArgumentException("Message " + msg.getMsgId() + " not found");
            }
        }
        //如果是确认的还原
        else {
            DatabaseModifier modifier = Environment.getInstance().modifyDatabase();
            RxJavaUtils.complete(() -> {
                modifier.restoreMessage(msg);
                modifier.apply();
                mHost.reloadMessages();
                //从列表中移除
                mConfirmedEditionItemList.remove(position);
                //移除备份消息
                mModel.confirmedBackups.remove(msg);
            }).subscribe(new RxJavaUtils.CompletableObservableAdapter() {
                Dialog progress;

                @Override
                public void onSubscribe(@NotNull Disposable d) {
                    progress = UiUtils.createProgress(requireContext(), R.string.loading);
                    progress.show();
                }

                @Override
                public void onComplete() {
                    //更新消息页面
                    mHost.notifyMessageListChanged();
                    //更新当前列表
                    mAdapter.notifyItemRemoved(position);
                    progress.dismiss();
                    UiUtils.createAlert(requireContext(), getString(R.string.alert_restart_after_changes_applied))
                            .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                                startActivity(requireActivity().getPackageManager().getLaunchIntentForPackage("com.tencent.mm"));
                            }).setNegativeButton(android.R.string.cancel, null).show();
                }

                @Override
                public void onError(@NotNull Throwable e) {
                    e.printStackTrace();
                    progress.dismiss();
                }
            });
        }
    }

    private final MessageViewModel.EventHandler mHandler = new MessageViewModel.EventHandler() {
        @Override
        public void onMessageChanged() {
            if (editionSetSelection.get() == EDITION_SET_INDEX_UNCONFIRMED) {
                int selection = editionFlagSelection.get();
                if (selection == 0 || selection == Edition.FLAG_REPLACEMENT) {
                    refreshList();
                }
            }
        }

        @Override
        public void onMessageInserted() {
            if (editionSetSelection.get() == EDITION_SET_INDEX_UNCONFIRMED) {
                int selection = editionFlagSelection.get();
                if (selection == 0 || selection == Edition.FLAG_INSERTION) {
                    refreshList();
                }
            }
        }

        @Override
        public void onMessageDeleted() {
            if (editionSetSelection.get() == EDITION_SET_INDEX_UNCONFIRMED) {
                int selection = editionFlagSelection.get();
                if (selection == 0 || selection == Edition.FLAG_REMOVAL) {
                    refreshList();
                }
            }
        }

        @Override
        public void onMessageRestored(int editionFlag, int setFlag) {
            if (editionSetSelection.get() == EDITION_SET_INDEX_UNCONFIRMED) {
                int selection = editionFlagSelection.get();
                if (selection == 0 || selection == editionFlag) {
                    refreshList();
                }
            }
        }

        @Override
        public void onEditionListChanged(int set) {
            refreshList();
        }
    };

    private void loadUnconfirmedEditions() {
        mUnconfirmedEditionItemList.clear();
        for (int i = 0; i < mModifier.getAllPendingEditions().size(); i++) {
            mUnconfirmedEditionItemList.add(new EditionItem(mModifier.getAllPendingEditions().valueAt(i)));
        }
    }

    private void loadConfirmedEditions() {
        mConfirmedEditionItemList.clear();
        for (Message message : mModel.confirmedBackups) {
            mConfirmedEditionItemList.add(new EditionItem(message));
        }
    }

    /**
     * 更新列表
     */
    private void refreshList() {
        loadUnconfirmedEditions();
        loadConfirmedEditions();
        mCurrentEditionItemList = editionSetSelection.get() == 0 ? mConfirmedEditionItemList : mUnconfirmedEditionItemList;
        int flag = editionFlagSelection.get();
        if (flag != 0) {
            RxJavaUtils.stream(mCurrentEditionItemList).parallel().filter(editionItem -> editionItem.getFlag() == flag).runOn(Schedulers.computation())
                    .sequential().toSortedList().observeOn(AndroidSchedulers.mainThread()).subscribe(new RxJavaUtils.SingleObserverAdapter<List<EditionItem>>() {
                @Override
                public void onSuccess(@NotNull List<EditionItem> o) {
                    mCurrentEditionItemList = o;
                    mAdapter.notifyDataSetChanged();
                }
            });
        } else {
            mAdapter.notifyDataSetChanged();
        }
    }

    private void initList() {
        loadUnconfirmedEditions();
        loadConfirmedEditions();
        mCurrentEditionItemList = mConfirmedEditionItemList;
        mAdapter = new EditionAdapter();
        mBinding.rvEdition.setAdapter(mAdapter);
        editionSetSelection.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
            @Override
            public void onPropertyChanged(Observable sender, int propertyId) {
                refreshList();
            }
        });
        editionFlagSelection.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
            @Override
            public void onPropertyChanged(Observable sender, int propertyId) {
                refreshList();
            }
        });
    }

    public static class EditionItem implements Comparable<EditionItem> {
        private final Message showcase;
        private final int flag;
        private boolean expanded;

        public Message getShowcase() {
            return showcase;
        }

        public int getFlag() {
            return flag;
        }

        public long getMsgId() {
            return getShowcase().getMsgId();
        }

        public void collapseOrExpand() {
            expanded = !expanded;
        }

        public EditionItem(@NotNull Edition edition) {
            if (edition.getFiller() == null) {
                showcase = edition.getVictim();
            } else {
                showcase = edition.getFiller();
            }
            flag = edition.getFlag();
        }

        public EditionItem(@NotNull Message backup) {
            showcase = backup;
            flag = backup.getEditionFlag();
        }

        @StringRes
        public int getEditionFlagCaption() {
            switch (getFlag()) {
                case Edition.FLAG_REMOVAL:
                    return R.string.edition_type_removal;
                case Edition.FLAG_INSERTION:
                    return R.string.edition_type_insertion;
                case Edition.FLAG_REPLACEMENT:
                    return R.string.edition_type_rep;
            }
            throw new IllegalArgumentException("Unknown edition flag: " + getFlag());
        }

        @Override
        public int compareTo(@NotNull EditionItem o) {
            return Long.compare(showcase.getCreateTimeStamp(), o.showcase.getCreateTimeStamp());
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_edition, container, false);
        mBinding.setEditionFlagSelection(editionFlagSelection);
        mBinding.setEditionSetSelection(editionSetSelection);
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initList();
    }

    public class EditionAdapter extends RecyclerView.Adapter<EditionAdapter.EditionViewHolder> {
        private final AutoTransition transition;

        private EditionAdapter() {
            transition = new AutoTransition();
            transition.setInterpolator(new FastOutSlowInInterpolator());
        }


        @NonNull
        @Override
        public EditionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemEditionBinding binding = DataBindingUtil.inflate(getLayoutInflater(), R.layout.item_edition, parent, false);
            return new EditionViewHolder(binding);
        }

        @Override
        public void onBindViewHolder(@NonNull EditionViewHolder holder, int position, @NonNull List<Object> payloads) {
            if (payloads.size() != 0) {
                holder.binding.getItem().collapseOrExpand();
                holder.binding.executePendingBindings();
            } else {
                super.onBindViewHolder(holder, position, payloads);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull EditionViewHolder holder, int position) {
            holder.binding.setVh(holder);
            holder.binding.setItem(mCurrentEditionItemList.get(position));
            holder.binding.executePendingBindings();
        }

        @Override
        public int getItemCount() {
            return mCurrentEditionItemList.size();
        }

        public class EditionViewHolder extends RecyclerView.ViewHolder {
            ItemEditionBinding binding;

            public EditionViewHolder(@NonNull ItemEditionBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
                this.binding.setFragment(EditionFragment.this);
                this.binding.setHost(mHost);
                this.itemView.setOnClickListener(v -> {
                    binding.getItem().collapseOrExpand();
                    binding.expandable.setVisibility(binding.getItem().expanded ? View.VISIBLE : View.GONE);
                    TransitionManager.go(new Scene(mBinding.rvEdition), transition);
                });
            }
        }
    }
}
