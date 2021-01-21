/*
 * Copyright (c) 2021 xjunz. 保留所有权利
 */
package xjunz.tool.wechat.ui.main.fragment.dialog;

import android.app.ActivityOptions;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.transition.AutoTransition;
import android.transition.Transition;
import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ObservableBoolean;
import androidx.fragment.app.DialogFragment;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import androidx.recyclerview.widget.RecyclerView;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import xjunz.tool.wechat.R;
import xjunz.tool.wechat.databinding.DialogCheckZombiesBinding;
import xjunz.tool.wechat.databinding.ItemZombieBinding;
import xjunz.tool.wechat.impl.DatabaseModifier;
import xjunz.tool.wechat.impl.Environment;
import xjunz.tool.wechat.impl.model.account.Contact;
import xjunz.tool.wechat.impl.repo.ContactRepository;
import xjunz.tool.wechat.impl.repo.RepositoryFactory;
import xjunz.tool.wechat.ui.customview.MasterToast;
import xjunz.tool.wechat.ui.main.DetailActivity;
import xjunz.tool.wechat.util.IOUtils;
import xjunz.tool.wechat.util.RxJavaUtils;
import xjunz.tool.wechat.util.UiUtils;
import xjunz.tool.wechat.util.Utils;

/**
 * @author xjunz 2021/1/17 1:10
 */
public class CheckZombiesDialog extends DialogFragment {
    private DialogCheckZombiesBinding mBinding;

    public ObservableBoolean getFoundZombies() {
        return mFoundZombies;
    }

    private List<Contact> mPossibleZombies;
    private final ObservableBoolean mFoundZombies = new ObservableBoolean(false);

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NO_FRAME, R.style.Base_Dialog_Translucent);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.dialog_check_zombies, container, false);
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mBinding.setHost(this);
    }

    public void operate() {
        if (!mFoundZombies.get()) {
            searchForZombies();
        } else {
            if (mPossibleZombies.size() == 0) {
                return;
            }
            Dialog dialog = UiUtils.createProgress(requireActivity(), R.string.please_wait);
            dialog.show();
            RxJavaUtils.complete(() -> {
                DatabaseModifier modifier = Environment.getInstance().modifyDatabase();
                String labelName = getString(R.string.possible_zombies);
                modifier.createContactLabelIfNotExists(labelName);
                for (Contact contact : mPossibleZombies) {
                    modifier.attachLabelToContact(contact.id, labelName);
                }
                modifier.apply();
                Utils.openWeChat(requireActivity());
            }).subscribe(new RxJavaUtils.CompletableObservableAdapter() {
                @Override
                public void onComplete() {
                    dialog.dismiss();
                }

                @Override
                public void onError(@NotNull Throwable e) {
                    super.onError(e);
                    dialog.dismiss();
                    MasterToast.shortToast(R.string.error_occurred);
                }
            });
        }
    }

    public void searchForZombies() {
        mPossibleZombies = new ArrayList<>();
        setCancelable(false);
        for (int i = 0; i < mBinding.container.getChildCount(); i++) {
            View view = mBinding.container.getChildAt(i);
            if (view.getId() == R.id.pb_load) {
                UiUtils.visible(view);
            } else {
                UiUtils.gone(view);
            }
        }
        ViewGroup.LayoutParams lp = mBinding.container.getLayoutParams();
        lp.width = ViewGroup.LayoutParams.WRAP_CONTENT;
        mBinding.container.setLayoutParams(lp);
        Transition transition = new AutoTransition();
        transition.setInterpolator(new FastOutSlowInInterpolator());
        transition.addListener(new UiUtils.TransitionListenerAdapter() {
            @Override
            public void onTransitionEnd(@NonNull Transition transition) {
                super.onTransitionEnd(transition);
                RxJavaUtils.complete(() -> {
                    ContactRepository repository = RepositoryFactory.get(ContactRepository.class);
                    for (Contact contact : repository.getAllOfType(Contact.Type.FRIEND)) {
                        if (contact.isPossibleZombie()) {
                            mPossibleZombies.add(contact);
                        }
                    }
                    //假装加载了很久
                    Thread.sleep(500L);
                }).subscribe(new RxJavaUtils.CompletableObservableAdapter() {
                    @Override
                    public void onComplete() {
                        mFoundZombies.set(mPossibleZombies.size() != 0);
                        if (!mFoundZombies.get()) {
                            MasterToast.shortToast(R.string.nothing_was_found);
                        } else {
                            mBinding.rvZombies.setAdapter(new ZombieAdapter());
                        }
                        postSearch();
                    }

                    @Override
                    public void onError(@NotNull Throwable e) {
                        UiUtils.createError(requireContext(), IOUtils.readStackTraceFromThrowable(e)).show();
                        postSearch();
                    }
                });
            }
        });
        TransitionManager.beginDelayedTransition((ViewGroup) mBinding.getRoot(), transition);
    }

    public void postSearch() {
        setCancelable(true);
        for (int i = 0; i < mBinding.container.getChildCount(); i++) {
            View view = mBinding.container.getChildAt(i);
            if (view.getId() == R.id.pb_load) {
                UiUtils.gone(view);
            } else {
                UiUtils.visible(view);
            }
        }
        ViewGroup.LayoutParams lp = mBinding.container.getLayoutParams();
        lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
        mBinding.container.setLayoutParams(lp);
        Transition transition = new AutoTransition();
        transition.setInterpolator(new FastOutSlowInInterpolator());
        mBinding.executePendingBindings();
        TransitionManager.beginDelayedTransition((ViewGroup) mBinding.getRoot(), transition);
    }

    private class ZombieAdapter extends RecyclerView.Adapter<ZombieAdapter.ZombieViewHolder> {
        @NonNull
        @Override
        public ZombieViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ZombieViewHolder(DataBindingUtil.inflate(getLayoutInflater(), R.layout.item_zombie, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ZombieViewHolder holder, int position) {
            holder.binding.setContact(mPossibleZombies.get(position));
            holder.binding.executePendingBindings();
        }

        @Override
        public int getItemCount() {
            return mPossibleZombies.size();
        }

        private class ZombieViewHolder extends RecyclerView.ViewHolder {
            private final ItemZombieBinding binding;

            public ZombieViewHolder(@NonNull ItemZombieBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
                this.itemView.setOnClickListener(v -> {
                    Intent intent = new Intent(requireActivity(), DetailActivity.class);
                    intent.putExtra(DetailActivity.EXTRA_CONTACT, mPossibleZombies.get(getAdapterPosition()));
                    requireActivity().startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(requireActivity(), binding.ivAvatar,
                            binding.ivAvatar.getTransitionName()).toBundle());
                });
            }
        }

    }
}
