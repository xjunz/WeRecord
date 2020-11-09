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
import androidx.fragment.app.Fragment;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import xjunz.tool.wechat.R;
import xjunz.tool.wechat.data.viewmodel.MessageViewModel;
import xjunz.tool.wechat.databinding.FragmentEditionBinding;
import xjunz.tool.wechat.databinding.ItemEditionBinding;
import xjunz.tool.wechat.impl.DatabaseModifier;
import xjunz.tool.wechat.impl.Environment;
import xjunz.tool.wechat.impl.model.message.BackupMessage;
import xjunz.tool.wechat.impl.model.message.Edition;
import xjunz.tool.wechat.impl.repo.MessageRepository;
import xjunz.tool.wechat.impl.repo.RepositoryFactory;
import xjunz.tool.wechat.util.RxJavaUtils;

/**
 * 显示当前{@link xjunz.tool.wechat.impl.model.account.Talker}的消息中被编辑过的消息的{@link Fragment}。
 */
public class EditionFragment extends Fragment {
    private FragmentEditionBinding mBinding;
    private MessageViewModel mModel;
    private EditionAdapter mAdapter;
    private final List<EditionItem> mItemList = new ArrayList<>();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mModel = new ViewModelProvider(requireActivity(), new ViewModelProvider.NewInstanceFactory()).get(MessageViewModel.class);
    }

    public void restoreMessage(int msgId) {
        DatabaseModifier modifier = Environment.getInstance().modifyDatabase();

    }

    private void loadAllBackupMessages() {
        RxJavaUtils.complete(() -> {
            MessageRepository repository = RepositoryFactory.get(MessageRepository.class);
            List<BackupMessage> messages = repository.queryAllBackupMessages(mModel.currentTalker.id);
            for (BackupMessage backup : messages) {
                Edition edition;
                switch (backup.getEditionFlag()) {
                    case Edition.FLAG_DELETION:
                        edition = Edition.delete(backup);
                        break;
                    case Edition.FLAG_INSERTION:
                        edition = Edition.insert(backup);
                        break;
                    case Edition.FLAG_REPLACEMENT:
                        edition = Edition.replace(backup, repository.queryMessageByMsgId(backup.getMsgId()));
                        break;
                }
            }

        }).subscribe(new RxJavaUtils.CompletableObservableAdapter() {
            @Override
            public void onComplete() {
                super.onComplete();
                mAdapter = new EditionAdapter();
                mBinding.rvEdition.setAdapter(mAdapter);
            }
        });
    }

    public static class EditionItem {
        public Edition edition;
        public boolean expanded;

        public void collapseOrExpand() {
            expanded = !expanded;
        }

        public EditionItem(Edition edition) {
            this.edition = edition;
        }

        @StringRes
        public int getEditionFlagCaption() {
            switch (edition.getFlag()) {
                case Edition.FLAG_DELETION:
                    return R.string.edition_type_deleted;
                case Edition.FLAG_INSERTION:
                    return R.string.edition_type_insertion;
                case Edition.FLAG_REPLACEMENT:
                    return R.string.edition_type_rep;
            }
            throw new IllegalArgumentException("Unknown edition flag: " + edition.getFlag());
        }

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_edition, container, false);
        loadAllBackupMessages();
        return mBinding.getRoot();
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
            holder.binding.setItem(mItemList.get(position));
            holder.binding.executePendingBindings();
        }

        @Override
        public int getItemCount() {
            return mItemList.size();
        }

        private class EditionViewHolder extends RecyclerView.ViewHolder {
            ItemEditionBinding binding;

            public EditionViewHolder(@NonNull ItemEditionBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
                this.binding.setModel(mModel);
                this.binding.btnRestore.setOnClickListener(v -> restoreMessage(binding.getItem().edition.getOrigin().getMsgId()));
                this.itemView.setOnClickListener(v -> {
                    binding.getItem().collapseOrExpand();
                    binding.expandable.setVisibility(binding.getItem().expanded ? View.VISIBLE : View.GONE);
                    TransitionManager.go(new Scene(mBinding.rvEdition), transition);
                });
            }
        }
    }
}
