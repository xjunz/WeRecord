/*
 * Copyright (c) 2021 xjunz. 保留所有权利
 */
package xjunz.tool.werecord.ui.main.fragment;

import android.content.Context;

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
     * 初始化配置，此方法可能在{@link Fragment#onAttach(Context)}之前执行
     *
     * @return 当前配置
     */
    public abstract PageConfig getInitialConfig();

    public PageConfig getCurrentConfig() {
        if (mConfig == null) {
            mConfig = getInitialConfig();
        }
        return mConfig;
    }
}
