/*
 * Copyright (c) 2020 xjunz. 保留所有权利
 */
package xjunz.tool.wechat.ui.message.fragment.dialog;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import java.util.concurrent.Callable;

import xjunz.tool.wechat.R;
import xjunz.tool.wechat.data.viewmodel.EditorViewModel;
import xjunz.tool.wechat.databinding.DialogSenderChooserBinding;
import xjunz.tool.wechat.databinding.ItemSenderBinding;
import xjunz.tool.wechat.impl.Environment;
import xjunz.tool.wechat.impl.model.account.Account;
import xjunz.tool.wechat.impl.repo.ContactRepository;
import xjunz.tool.wechat.impl.repo.RepositoryFactory;
import xjunz.tool.wechat.util.RxJavaUtils;

public class SenderChooserDialog extends DialogFragment {
    EditorViewModel mModel;
    private SenderAdapter mAdapter;
    private String[] mSenderIds;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, R.style.Base_Dialog_SenderChooser);
        mModel = new ViewModelProvider(requireActivity(), new ViewModelProvider.NewInstanceFactory()).get(EditorViewModel.class);
        mSenderIds = mModel.getOptionalSenderIds();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        DialogSenderChooserBinding binding = DataBindingUtil.inflate(inflater, R.layout.dialog_sender_chooser, container, false);
        mAdapter = new SenderAdapter();
        binding.setDialog(this);
        binding.setModel(mModel);
        binding.rvSender.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (recyclerView.canScrollVertically(1)) {
                    if (binding.dividerBottom.getVisibility() == View.GONE) {
                        binding.dividerBottom.setVisibility(View.VISIBLE);
                    }
                } else {
                    if (binding.dividerBottom.getVisibility() == View.VISIBLE) {
                        binding.dividerBottom.setVisibility(View.GONE);
                    }
                }
                if (recyclerView.canScrollVertically(-1)) {
                    if (binding.dividerTop.getVisibility() == View.GONE) {
                        binding.dividerTop.setVisibility(View.VISIBLE);
                    }
                } else {
                    if (binding.dividerTop.getVisibility() == View.VISIBLE) {
                        binding.dividerTop.setVisibility(View.GONE);
                    }
                }
            }
        });
        binding.rvSender.setAdapter(mAdapter);
        return binding.getRoot();
    }

    private class SenderAdapter extends RecyclerView.Adapter<SenderViewHolder> {

        private ContactRepository repository;

        private SenderAdapter() {
            this.repository = RepositoryFactory.get(ContactRepository.class);
        }

        @NonNull
        @Override
        public SenderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemSenderBinding binding = DataBindingUtil.inflate(getLayoutInflater(), R.layout.item_sender, parent, false);
            return new SenderViewHolder(binding);
        }

        @Override
        public void onBindViewHolder(@NonNull SenderViewHolder holder, int position) {
            String id = mSenderIds[position];
            if (id != null) {
                RxJavaUtils.single((Callable<Account>) () -> repository.get(id)).subscribe(new RxJavaUtils.SingleObserverAdapter<Account>() {
                    @Override
                    public void onSuccess(Account o) {
                        holder.binding.setAccount(o);
                        holder.binding.executePendingBindings();
                    }
                });
            } else {
                holder.binding.setAccount(Environment.getInstance().getCurrentUser());
                holder.binding.executePendingBindings();
            }
        }

        @Override
        public int getItemCount() {
            return mSenderIds.length;
        }
    }

    private class SenderViewHolder extends RecyclerView.ViewHolder {
        private ItemSenderBinding binding;

        public SenderViewHolder(@NonNull ItemSenderBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            this.binding.setModel(mModel);
        }
    }
}
