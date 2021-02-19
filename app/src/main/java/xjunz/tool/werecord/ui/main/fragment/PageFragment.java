/*
 * Copyright (c) 2021 xjunz. 保留所有权利
 */
package xjunz.tool.werecord.ui.main.fragment;

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
     * 初始化配置
     *
     * @return 当前配置
     */
    public abstract PageConfig getInitialConfig();

    public PageConfig getCurrentConfig() {
        return mConfig = (mConfig == null ? getInitialConfig() : mConfig);
    }
}
