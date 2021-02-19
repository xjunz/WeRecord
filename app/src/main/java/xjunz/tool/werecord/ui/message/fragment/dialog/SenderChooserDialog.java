/*
 * Copyright (c) 2021 xjunz. 保留所有权利
 */
package xjunz.tool.werecord.ui.message.fragment.dialog;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ObservableField;
import androidx.recyclerview.widget.RecyclerView;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Callable;

import xjunz.tool.werecord.R;
import xjunz.tool.werecord.databinding.DialogSenderChooserBinding;
import xjunz.tool.werecord.databinding.ItemSenderBinding;
import xjunz.tool.werecord.impl.Environment;
import xjunz.tool.werecord.impl.model.account.Account;
import xjunz.tool.werecord.impl.repo.ContactRepository;
import xjunz.tool.werecord.impl.repo.RepositoryFactory;
import xjunz.tool.werecord.ui.base.ConfirmationDialog;
import xjunz.tool.werecord.util.RxJavaUtils;

public class SenderChooserDialog extends ConfirmationDialog<Account> {
    private String[] senderIds;
    private ContactRepository repository;
    private final ObservableField<Account> candidate = new ObservableField<>();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        repository = RepositoryFactory.get(ContactRepository.class);
    }

    public SenderChooserDialog setSenderIds(String[] ids) {
        this.senderIds = ids;
        return this;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        DialogSenderChooserBinding binding = DataBindingUtil.inflate(inflater, R.layout.dialog_sender_chooser, container, false);
        SenderAdapter adapter = new SenderAdapter();
        binding.setHost(this);
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
        binding.rvSender.setAdapter(adapter);
        return binding.getRoot();
    }

    public ConfirmationDialog<Account> setDefaultId(String id) {
        return setDefault(repository.get(id));
    }

    @Override
    public ConfirmationDialog<Account> setDefault(Account def) {
        super.setDefault(def);
        setCandidate(def);
        return this;
    }

    @Override
    public Account getResult() {
        return candidate.get();
    }

    public ObservableField<Account> getCandidate() {
        return candidate;
    }

    public void setCandidate(Account candidate) {
        this.candidate.set(candidate);
    }

    private class SenderAdapter extends RecyclerView.Adapter<SenderViewHolder> {

        @NonNull
        @Override
        public SenderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemSenderBinding binding = DataBindingUtil.inflate(getLayoutInflater(), R.layout.item_sender, parent, false);
            return new SenderViewHolder(binding);
        }

        @Override
        public void onBindViewHolder(@NonNull SenderViewHolder holder, int position) {
            String id = senderIds[position];
            if (id != null) {
                RxJavaUtils.single((Callable<Account>) () -> repository.get(id)).subscribe(new RxJavaUtils.SingleObserverAdapter<Account>() {
                    @Override
                    public void onSuccess(@NotNull Account o) {
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
            return senderIds == null ? 0 : senderIds.length;
        }
    }

    private class SenderViewHolder extends RecyclerView.ViewHolder {
        private final ItemSenderBinding binding;

        public SenderViewHolder(@NonNull ItemSenderBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            this.binding.setHost(SenderChooserDialog.this);
        }
    }
}
