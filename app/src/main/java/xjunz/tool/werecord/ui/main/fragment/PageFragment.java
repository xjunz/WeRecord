/*
 * Copyright (c) 2021 xjunz. 保留所有权利
 */
package xjunz.tool.werecord.ui.main.fragment;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import xjunz.tool.werecord.data.viewmodel.PageConfig;

/**
 * @author xjunz 2021/2/11 13:47
 */
public abstract class PageFragment extends Fragment {
    /**
     * 当前页面的配置信息
     */
    protected PageConfig mConfig;

    /**
     * 初始化配置，在{@link Fragment#onCreate(Bundle)}中执行
     *
     * @return 当前配置
     */
    public abstract PageConfig getInitialConfig();

    public PageConfig getCurrentConfig() {
        return mConfig;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mConfig = getInitialConfig();
    }
}
