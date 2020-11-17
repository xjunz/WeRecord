/*
 * Copyright (c) 2020 xjunz. 保留所有权利
 */
package xjunz.tool.wechat.ui.message.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;

import xjunz.tool.wechat.R;
import xjunz.tool.wechat.databinding.FragmentStatsBinding;

public class StatisticsFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        FragmentStatsBinding binding = DataBindingUtil.inflate(inflater, R.layout.fragment_stats, container, false);
        return binding.getRoot();
    }


}
