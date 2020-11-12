/*
 * Copyright (c) 2020 xjunz. 保留所有权利
 */
package xjunz.tool.wechat.ui.message.fragment;

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
import androidx.fragment.app.Fragment;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import xjunz.tool.wechat.R;
import xjunz.tool.wechat.data.viewmodel.MessageEditorViewModel;
import xjunz.tool.wechat.data.viewmodel.MessageViewModel;
import xjunz.tool.wechat.databinding.FragmentEditionBinding;
import xjunz.tool.wechat.databinding.ItemEditionBinding;
import xjunz.tool.wechat.impl.DatabaseModifier;
import xjunz.tool.wechat.impl.Environment;
import xjunz.tool.wechat.impl.model.message.BackupMessage;
import xjunz.tool.wechat.impl.model.message.Edition;
import xjunz.tool.wechat.impl.model.message.Message;
import xjunz.tool.wechat.util.RxJavaUtils;

/**
 * 显示当前{@link xjunz.tool.wechat.impl.model.account.Talker}的消息中被编辑过的消息的{@link Fragment}。
 */
public class EditionFragment extends Fragment {
    private FragmentEditionBinding mBinding;
    private MessageViewModel mModel;
    private EditionAdapter mAdapter;
    private final List<EditionItem> mConfirmedEditionItemList = new ArrayList<>();
    private final List<EditionItem> mUnconfirmedEditionItemList = new ArrayList<>();
    private List<EditionItem> mCurrentEditionItemList = new ArrayList<>();
    private DatabaseModifier mModifier;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mModel = new ViewModelProvider(requireActivity(), new ViewModelProvider.NewInstanceFactory()).get(MessageViewModel.class);
        mModifier = Environment.getInstance().modifyDatabase();
        MessageEditorViewModel.get(requireActivity().getApplication()).registerEventHandler(mHandler);
    }

    public void restoreMessage(int msgId) {
        DatabaseModifier modifier = Environment.getInstance().modifyDatabase();
    }

    private final MessageEditorViewModel.EditorEventHandler mHandler = new MessageEditorViewModel.EditorEventHandler() {
        @Override
        public void onMessageChanged(boolean timestampChanged, Message changed) {
            if (mModel.editionSetSelection.get() == 1) {
                int selection = mModel.editionFlagSelection.get();
                if (selection == 0 || selection == Edition.FLAG_REPLACEMENT) {
                    refreshList();
                }
            }
        }

        @Override
        public void onMessageInserted(boolean addBefore, Message inserted) {
            if (mModel.editionSetSelection.get() == 1) {
                int selection = mModel.editionFlagSelection.get();
                if (selection == 0 || selection == Edition.FLAG_INSERTION) {
                    refreshList();
                }
            }
        }

        @Override
        public void onMessageDeleted() {
            if (mModel.editionSetSelection.get() == 1) {
                int selection = mModel.editionFlagSelection.get();
                if (selection == 0 || selection == Edition.FLAG_REMOVAL) {
                    refreshList();
                }
            }
        }
    };

    private void loadUnconfirmedEditions() {
        mUnconfirmedEditionItemList.clear();
        for (int i = 0; i < mModifier.getAllPendingEditions().size(); i++) {
            mUnconfirmedEditionItemList.add(new EditionItem(mModifier.getAllPendingEditions().valueAt(i)));
        }
    }

    private void refreshList() {
        loadUnconfirmedEditions();
        mCurrentEditionItemList = mModel.editionSetSelection.get() == 0 ? mConfirmedEditionItemList : mUnconfirmedEditionItemList;
        int flag = mModel.editionFlagSelection.get();
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
        for (BackupMessage message : mModel.allBackupMessages) {
            mConfirmedEditionItemList.add(new EditionItem(message));
        }
        loadUnconfirmedEditions();
        mCurrentEditionItemList = mConfirmedEditionItemList;
        mAdapter = new EditionAdapter();
        mBinding.rvEdition.setAdapter(mAdapter);
        mModel.editionSetSelection.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
            @Override
            public void onPropertyChanged(Observable sender, int propertyId) {
                refreshList();
            }
        });
        mModel.editionFlagSelection.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
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

        public boolean isExpanded() {
            return expanded;
        }

        public int getMsgId() {
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

        public EditionItem(@NotNull BackupMessage backup) {
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
        mBinding.setModel(mModel);
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initList();
    }

    private class EditionAdapter extends RecyclerView.Adapter<EditionAdapter.EditionViewHolder> {
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
            holder.binding.setItem(mCurrentEditionItemList.get(position));
            holder.binding.executePendingBindings();
        }

        @Override
        public int getItemCount() {
            return mCurrentEditionItemList.size();
        }

        private class EditionViewHolder extends RecyclerView.ViewHolder {
            ItemEditionBinding binding;

            public EditionViewHolder(@NonNull ItemEditionBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
                this.binding.setModel(mModel);
                this.binding.btnRestore.setOnClickListener(v -> restoreMessage(binding.getItem().showcase.getMsgId()));
                this.itemView.setOnClickListener(v -> {
                    binding.getItem().collapseOrExpand();
                    binding.expandable.setVisibility(binding.getItem().expanded ? View.VISIBLE : View.GONE);
                    TransitionManager.go(new Scene(mBinding.rvEdition), transition);
                });
            }
        }
    }
}
