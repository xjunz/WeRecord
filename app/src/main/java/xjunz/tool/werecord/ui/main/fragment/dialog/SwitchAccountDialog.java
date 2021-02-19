/*
 * Copyright (c) 2021 xjunz. 保留所有权利
 */
package xjunz.tool.werecord.ui.main.fragment.dialog;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.OneShotPreDrawListener;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import xjunz.tool.werecord.App;
import xjunz.tool.werecord.R;
import xjunz.tool.werecord.databinding.DialogSwitchAccountBinding;
import xjunz.tool.werecord.databinding.ItemSwitchAccountBinding;
import xjunz.tool.werecord.impl.Environment;
import xjunz.tool.werecord.impl.model.account.User;
import xjunz.tool.werecord.impl.repo.ContactRepository;
import xjunz.tool.werecord.impl.repo.RepositoryFactory;
import xjunz.tool.werecord.ui.main.MainActivity;
import xjunz.tool.werecord.util.UiUtils;

/**
 * @author xjunz 2021/2/18 0:14
 */
public class SwitchAccountDialog extends DialogFragment {
    private DialogSwitchAccountBinding mBinding;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NO_FRAME, R.style.Base_Dialog_Normal);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return (mBinding = DialogSwitchAccountBinding.inflate(inflater, container, false)).getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setCancelable(true);
        mBinding.rvUserList.setAdapter(new SwitchAccountAdapter());
        mBinding.ibClose.setOnClickListener(v -> dismiss());
    }

    private class SwitchAccountAdapter extends RecyclerView.Adapter<SwitchAccountAdapter.SwitchAccountViewHolder> {
        private final List<User> mUsers;
        private final ContactRepository mRepository;

        public SwitchAccountAdapter() {
            mUsers = Environment.getInstance().getUserList();
            mRepository = RepositoryFactory.get(ContactRepository.class);
        }

        @NonNull
        @Override
        public SwitchAccountAdapter.SwitchAccountViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new SwitchAccountViewHolder(ItemSwitchAccountBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull SwitchAccountAdapter.SwitchAccountViewHolder holder, int position) {
            User user = mUsers.get(position);
            holder.binding.setAccount(mRepository.get(user.id));
            holder.binding.setIsCurrentUsed(user.isCurrentUsed);
            OneShotPreDrawListener.add(holder.itemView, () -> {
                View itemView = holder.itemView;
                holder.binding.tvName.setMaxWidth(itemView.getWidth() - itemView.getPaddingStart() * 2 - holder.binding.tvName.getLeft() - holder.binding.tvCurrentUsed.getWidth());
            });
            holder.binding.executePendingBindings();
        }

        @Override
        public int getItemCount() {
            return mUsers.size();
        }

        private class SwitchAccountViewHolder extends RecyclerView.ViewHolder {
            private final ItemSwitchAccountBinding binding;

            public SwitchAccountViewHolder(@NonNull ItemSwitchAccountBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
                this.itemView.setOnClickListener(v -> {
                    if (binding.getIsCurrentUsed()) {
                        UiUtils.swing(v);
                        return;
                    }
                    UiUtils.createAlert(v.getContext(), App.getStringOf(R.string.format_switch_account, binding.getAccount().getName()))
                            .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                                App.config().lastUsedUin.setValue(mUsers.get(getAdapterPosition()).uin);
                                ((MainActivity) requireActivity()).restartWithoutVerification();
                            }).setNegativeButton(android.R.string.cancel, null).show();
                });
            }
        }
    }
}
