/*
 * Copyright (c) 2021 xjunz. 保留所有权利
 */

package xjunz.tool.werecord.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ObservableInt;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import java.io.IOException;

import xjunz.tool.werecord.App;
import xjunz.tool.werecord.R;
import xjunz.tool.werecord.data.viewmodel.PageViewModel;
import xjunz.tool.werecord.databinding.ActivityMainBinding;
import xjunz.tool.werecord.ui.base.RecycleSensitiveActivity;
import xjunz.tool.werecord.ui.customview.MasterToast;
import xjunz.tool.werecord.ui.main.fragment.ChatFragment;
import xjunz.tool.werecord.ui.main.fragment.ContactFragment;
import xjunz.tool.werecord.ui.main.fragment.ListPageFragment;
import xjunz.tool.werecord.ui.main.fragment.MineFragment;
import xjunz.tool.werecord.ui.main.fragment.MultiSelectionFragment;
import xjunz.tool.werecord.ui.main.fragment.PageFragment;
import xjunz.tool.werecord.ui.outer.DebugActivity;
import xjunz.tool.werecord.ui.outer.InitializationActivity;
import xjunz.tool.werecord.util.IoUtils;
import xjunz.tool.werecord.util.UiUtils;

public class MainActivity extends RecycleSensitiveActivity {
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
    private MultiSelectionFragment mMultiSelectionFragment;
    private ChatFragment mChatFragment;
    private final PageViewModel.EventHandler mFilterEventHandler = new PageViewModel.EventHandler() {
        @Override
        public void onConfirmFilter() {
            mBinding.mainPanel.closePanel();
        }

        @Override
        public void onCancelFilter() {
            mBinding.mainPanel.closePanel();
        }
    };
    @Override
    protected void onCreateNormally(@Nullable Bundle savedInstanceState) {
        //todo:新增消息定位问题（msgId）
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        mBinding.setActivity(this);
        initPages();
        initViews();
        mFilterModel = new ViewModelProvider(MainActivity.this, new ViewModelProvider.NewInstanceFactory()).get(PageViewModel.class);
        mBinding.setModel(mFilterModel);
        mFilterModel.addEventHandler(mFilterEventHandler);
        UiUtils.initColors(this);
    }

    private void initViews() {
        //有搜索结果时，点击Enter键，进入多选模式并全选
        mBinding.etSearch.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_UP) {
                if (mBinding.etSearch.getText().toString().length() != 0 && getListPageFragment().getCurrentListSize() != 0) {
                    enterMultiSelectionMode();
                    selectAll();
                    return true;
                }
            }
            return false;
        });
    }

    private void initPages() {
        mChatFragment = new ChatFragment();
        ContactFragment contact = new ContactFragment();
        mPages = new PageFragment[]{mChatFragment, contact, new MineFragment()};
        mBinding.vpMain.setAdapter(new MainFragmentAdapter(this));
    }

    public void openPanel() {
        mBinding.mainPanel.openPanel();
    }

    public void toggleSearchMode() {
        mFilterModel.getCurrentConfig().toggleSearchMode();
        //当数据变化后，数据绑定默认会在下一帧执行，导致requestFocus()时EditText实际上处于disabled的状态，
        //实际上并未获取到焦点，因此当EditText被设置为enabled时，就不会显示光标。可以调用此方法可以立即刷新数据绑定。
        mBinding.executePendingBindings();
        if (mFilterModel.getCurrentConfig().isInSearchMode.get()) {
            showImeFor(mBinding.etSearch);
        } else {
            mBinding.etSearch.getText().clear();
            hideIme(mBinding.etSearch);
        }
    }

    public void onChatOptionMenuClicked(MenuItem item, View itemView) {
        mChatFragment.onOptionMenuClicked(item, itemView);
    }

    public void enterMultiSelectionMode() {
        hideIme(mBinding.etSearch);
        getListPageFragment().getCurrentConfig().isInMultiSelectionMode.set(true);
        mMultiSelectionFragment = new MultiSelectionFragment();
        getSupportFragmentManager().beginTransaction()
                .add(R.id.main_panel, mMultiSelectionFragment)
                .addToBackStack("multi_selection_mode")
                .commit();
    }

    private ListPageFragment<?> getListPageFragment() {
        return (ListPageFragment<?>) mPages[mCurrentPageIndex.get()];
    }

    public void showFilterPanel() {
        mBinding.mainPanel.openPanel();
        if (App.getSharedPrefsManager().isFirstRun()) {
            MasterToast.shortToast("下滑顶部工具栏也可直接调出筛选页面哦！");
        }
    }

    public void quitMultiSelectionMode() {
        getSupportFragmentManager().popBackStack();
        Fragment fragment = mPages[mCurrentPageIndex.get()];
        if (fragment instanceof ListPageFragment) {
            ((ListPageFragment<?>) fragment).quitMultiSelectionMode();
        }
        if (getListPageFragment().getCurrentConfig().isInSearchMode.get()) {
            showImeFor(mBinding.etSearch);
        }
    }

    public void selectAll() {
        getListPageFragment().selectAll();
    }

    public void onItemSelected(int position, CharSequence caption, boolean unchanged) {
        if (!unchanged) {
            mFilterModel.updateCurrentConfig(mPages[position].getCurrentConfig());
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        try {
            new AlertDialog.Builder(this, R.style.Theme_AppCompat_Light_DialogWhenLarge_Material).setTitle(R.string.declaration)
                    .setMessage(Html.fromHtml(IoUtils.readAssetAsString("declaration.html")))
                    .setNegativeButton(R.string.close, null)
                    .show();
        } catch (IOException e) {
            MasterToast.shortToast(R.string.error_occurred);
        }
    }

    public boolean gotoDebugActivity(View view) {
        startActivity(new Intent(this, DebugActivity.class));
        return true;
    }

    public void restartWithoutVerification() {
        getEnvironment().purge();
        InitializationActivity.notifyNoVerificationLaunch();
        startActivity(new Intent(this, InitializationActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK));
    }

    public void restartToSync(View view) {
        UiUtils.createAlert(this, R.string.alert_restart_to_sync)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> restartWithoutVerification())
                .setNegativeButton(android.R.string.cancel, null).show();
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
        if (mMultiSelectionFragment != null && mMultiSelectionFragment.isAdded()) {
            if (mMultiSelectionFragment.isOptionMenuShown()) {
                mMultiSelectionFragment.hideOptionMenu();
            } else {
                quitMultiSelectionMode();
            }
        } else if (mBinding.mainPanel.isOpen()) {
            mBinding.mainPanel.closePanel();
        } else if (mFilterModel.getCurrentConfig().isInMultiSelectionMode.get()) {
            mBinding.ibFilter.performClick();
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
