/*
 * Copyright (c) 2020 xjunz. 保留所有权利
 */

package xjunz.tool.wechat.ui.main.fragment;

import androidx.fragment.app.Fragment;

import xjunz.tool.wechat.data.viewmodel.PageConfig;

/**
 * 主页面{@code Page}的抽象类
 */
public abstract class PageFragment extends Fragment {
    public abstract PageConfig getCurrentConfig();
}
