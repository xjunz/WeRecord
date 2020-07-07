package xjunz.tool.wechat.ui.main;

import android.content.DialogInterface;
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
    private ChatFragment mChatFragment;
    private ContactFragment mContactFragment;
    private ActivityMainBinding mBinding;
    private PageViewModel.EventHandler mFilterEventHandler = new PageViewModel.EventHandler() {
        @Override
        public void confirmFilter() {
            mBinding.mainPanel.closePanel();
        }

        @Override
        public void resetFilter() {
            MasterToast.shortToast(R.string.reset_completed);
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
    }


    private void initPages() {
        mChatFragment = new ChatFragment();
        mContactFragment = new ContactFragment();
        mPages = new PageFragment[]{mChatFragment, mContactFragment, new ContactFragment()};
        mBinding.vpMain.setAdapter(new MainFragmentAdapter(this));
    }


    public void enterMultiSelectionMode() {
        UiUtils.fadeSwitchText(mBinding.tvTopBarTitle, "已选择1项");
        UiUtils.fadeSwitchImage(mBinding.ibSearch, R.drawable.ic_close_24dp);
    }


    public void enterSearchMode(View view) {
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


    public void onItemSelect(int position, CharSequence caption, boolean unchanged) {
        if (!unchanged) {
            PageConfig config = mPages[position].getCurrentConfig();
            if (config != null) {
                mFilterModel.updateCurrentConfig(config);
            }
        }
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (getEnvironment() != null) {
            getEnvironment().purge();
        }
    }

    @Override
    public void onBackPressed() {
        UiUtils.createAlert(this, R.string.msg_exit_confirmation)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        MainActivity.super.onBackPressed();
                    }
                }).show();

    }
}