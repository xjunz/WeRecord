/*
 * Copyright (c) 2021 xjunz. 保留所有权利
 */
package xjunz.tool.werecord.ui.main.fragment;

import android.os.Bundle;
import android.transition.Transition;
import android.transition.TransitionInflater;
import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import xjunz.tool.werecord.R;
import xjunz.tool.werecord.databinding.FragmentMultiSelectionBinding;
import xjunz.tool.werecord.databinding.ItemChatOptionMenuBinding;
import xjunz.tool.werecord.ui.main.MainActivity;
import xjunz.tool.werecord.ui.viewmodel.PageViewModel;
import xjunz.tool.werecord.util.UiUtils;
import xjunz.tool.werecord.util.Utils;

/**
 * @author xjunz 2021/1/23 0:23
 */
public class MultiSelectionFragment extends Fragment {
    private FragmentMultiSelectionBinding mBinding;
    private PageViewModel mModel;
    private MainActivity mHost;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mModel = Utils.getViewModel(requireActivity(), PageViewModel.class);
        Transition transition = TransitionInflater.from(requireContext()).inflateTransition(R.transition.multi_selection_show);
        setEnterTransition(transition);
        mHost = (MainActivity) requireActivity();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = FragmentMultiSelectionBinding.inflate(inflater, container, false);
        return mBinding.getRoot();
    }

    public boolean isOptionMenuShown() {
        return mBinding.rvMenu.isShown();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mBinding.setActivity((MainActivity) requireActivity());
        mBinding.setHost(this);
        mBinding.setModel(mModel);
        mBinding.root.removeView(mBinding.rvMenu);
        mBinding.rvMenu.setAdapter(new OptionMenuAdapter());
    }

    public void showOptionMenu() {
        Transition transition = TransitionInflater.from(requireContext()).inflateTransition(R.transition.chat_option_menu_show);
        TransitionManager.beginDelayedTransition(mBinding.root, transition);
        UiUtils.visible(mBinding.mask);
        mBinding.root.removeView(mBinding.ibOption);
        mBinding.root.addView(mBinding.rvMenu);
    }

    public void hideOptionMenu() {
        Transition transition = TransitionInflater.from(requireContext()).inflateTransition(R.transition.chat_option_menu_show);
        transition.addListener(new UiUtils.TransitionListenerAdapter() {
            @Override
            public void onTransitionEnd(@NonNull Transition transition) {
                mBinding.root.removeView(mBinding.rvMenu);
                mBinding.root.addView(mBinding.ibOption);
            }
        });
        TransitionManager.beginDelayedTransition(mBinding.root, transition);
        View fab = mBinding.ibOption;
        //将我们的RV设置为FAB的位置和大小
        UiUtils.setLeftTopRightBottom(mBinding.rvMenu, fab.getLeft(), fab.getTop(), fab.getRight(), fab.getBottom());
        UiUtils.invisible(mBinding.mask);
    }

    private class OptionMenuAdapter extends RecyclerView.Adapter<OptionMenuAdapter.OptionMenuItemViewHolder> {
        private final Menu mMenu;

        public OptionMenuAdapter() {
            PopupMenu menu = new PopupMenu(requireContext(), null);
            menu.getMenuInflater().inflate(R.menu.chat_options, menu.getMenu());
            mMenu = menu.getMenu();
        }

        @NonNull
        @Override
        public OptionMenuItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new OptionMenuItemViewHolder(ItemChatOptionMenuBinding.inflate(getLayoutInflater()));
        }

        @Override
        public void onBindViewHolder(@NonNull OptionMenuItemViewHolder holder, int position) {
            holder.binding.setItem(mMenu.getItem(position));
            holder.binding.setDividerVisible(position != 0);
            holder.binding.executePendingBindings();
        }

        @Override
        public int getItemCount() {
            return mMenu.size();
        }

        public class OptionMenuItemViewHolder extends RecyclerView.ViewHolder {
            private final ItemChatOptionMenuBinding binding;

            public OptionMenuItemViewHolder(@NonNull ItemChatOptionMenuBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
                this.binding.setHost(mHost);
            }
        }
    }
}
