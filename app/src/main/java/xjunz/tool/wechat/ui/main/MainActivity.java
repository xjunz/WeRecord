/*
 * Copyright (c) 2020 xjunz. 保留所有权利
 */

package xjunz.tool.wechat.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ObservableInt;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import xjunz.tool.wechat.App;
import xjunz.tool.wechat.R;
import xjunz.tool.wechat.data.viewmodel.PageConfig;
import xjunz.tool.wechat.data.viewmodel.PageViewModel;
import xjunz.tool.wechat.databinding.ActivityMainBinding;
import xjunz.tool.wechat.ui.BaseActivity;
import xjunz.tool.wechat.ui.customview.MasterToast;
import xjunz.tool.wechat.ui.main.fragment.ChatFragment;
import xjunz.tool.wechat.ui.main.fragment.ContactFragment;
import xjunz.tool.wechat.ui.main.fragment.PageFragment;
import xjunz.tool.wechat.ui.outer.DebugActivity;
import xjunz.tool.wechat.util.UiUtils;

public class MainActivity extends BaseActivity {
    /**
     * 当前{@link androidx.viewpager2.widget.ViewPager2}的选中项，支持数据绑定<br/>
     * <b>注意：</b>默认值不能为空或为0，否则不会触发数据绑定初始化
     */
    public ObservableInt mCurrentPageIndex = new ObservableInt(-1);
    /**
     * 填充{@link androidx.viewpager2.widget.ViewPager2}的{@link Fragment}集合
     */
    private PageFragment[] mPages;
    private PageViewModel mFilterModel;
    private ActivityMainBinding mBinding;
    private final PageViewModel.EventHandler mFilterEventHandler = new PageViewModel.EventHandler() {
        @Override
        public void onConfirmFilter() {
            mBinding.mainPanel.closePanel();
        }

        @Override
        public void onResetFilter() {
            MasterToast.shortToast(R.string.reset_completed);
        }

        @Override
        public void onCancelFilter() {
            mBinding.mainPanel.closePanel();
        }

        @Override
        public void onPrepareFilter() {

        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        mBinding.setActivity(this);
        initPages();
        mFilterModel = new ViewModelProvider(MainActivity.this, new ViewModelProvider.NewInstanceFactory()).get(PageViewModel.class);
        mBinding.setModel(mFilterModel);
        mFilterModel.addEventHandler(mFilterEventHandler);
        UiUtils.initColors(this);
    }


    private void initPages() {
        ChatFragment chat = new ChatFragment();
        ContactFragment contact = new ContactFragment();
        mPages = new PageFragment[]{chat, contact, new ContactFragment()};
        mBinding.vpMain.setAdapter(new MainFragmentAdapter(this));
    }


    public void autoSearchMode(View view) {
        //当数据变化后，数据绑定默认会在下一帧执行，导致requestFocus()时EditText实际上处于disabled的状态，
        //实际上并未获取到焦点，因此当EditText被设置为enabled时，就不会显示光标。可以调用此方法可以立即刷新数据绑定。
        mBinding.executePendingBindings();
        if (mFilterModel.getCurrentConfig().isInSearchMode.get()) {
            mBinding.etSearch.requestFocus();
            showIme(view);
        } else {
            mBinding.etSearch.getText().clear();
            hideIme(view);
        }
    }

    public void showFilterPanel(View view) {
        mBinding.mainPanel.openPanel();
        if (App.getSharedPrefsManager().isFirstRun()) {
            MasterToast.shortToast("下滑顶部工具栏也可直接调出筛选页面哦！");
        }
    }


    public void onItemSelected(int position, CharSequence caption, boolean unchanged) {
        if (!unchanged) {
            PageConfig config = mPages[position].getCurrentConfig();
            if (config != null) {
                mFilterModel.updateCurrentConfig(config);
            }
        }
    }

    public void gotoDebugActivity(View view) {
        startActivity(new Intent(this, DebugActivity.class));
    }


    private class MainFragmentAdapter extends FragmentStateAdapter {
        MainFragmentAdapter(FragmentActivity fragmentActivity) {
            super(fragmentActivity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            return mPages[position];
        }


        @Override
        public int getItemCount() {
            return mPages.length;
        }
    }

    private long lastPressedTimestamp;

    @Override
    public void onBackPressed() {
        if (mBinding.mainPanel.isOpen()) {
            mBinding.mainPanel.closePanel();
        } else if (mFilterModel.getCurrentConfig().isInSearchMode.get()) {
            mBinding.ibSearch.performClick();
        } else {
            if (lastPressedTimestamp != 0 && System.currentTimeMillis() - lastPressedTimestamp < 2000) {
                if (getEnvironment() != null) {
                    getEnvironment().purge();
                }
                super.onBackPressed();
            } else {
                MasterToast.longToast(R.string.press_again_to_quit);
                lastPressedTimestamp = System.currentTimeMillis();
            }
        }
    }
}
