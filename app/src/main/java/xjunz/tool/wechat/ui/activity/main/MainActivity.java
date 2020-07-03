package xjunz.tool.wechat.ui.activity.main;

import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ObservableBoolean;
import androidx.databinding.ObservableField;
import androidx.databinding.ObservableInt;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import xjunz.tool.wechat.App;
import xjunz.tool.wechat.R;
import xjunz.tool.wechat.data.model.FilterConfig;
import xjunz.tool.wechat.data.model.FilterViewModel;
import xjunz.tool.wechat.databinding.ActivityMainBinding;
import xjunz.tool.wechat.ui.activity.BaseActivity;
import xjunz.tool.wechat.ui.customview.MasterToast;
import xjunz.tool.wechat.util.UiUtils;

public class MainActivity extends BaseActivity implements TextWatcher {
    /**
     * 当前{@link androidx.viewpager2.widget.ViewPager2}的选中项，支持数据绑定<br/>
     * <b>注意：</b>默认值不能为空或为0，否则不会触发数据绑定初始化
     */
    public ObservableInt mCurrentPageIndex = new ObservableInt(-1);
    /**
     * 当前数据顶栏标题，支持数据绑定
     */
    public ObservableField<CharSequence> mTitle = new ObservableField<>();
    /**
     * 是否处于搜索模式，支持数据绑定
     */
    public ObservableBoolean mIsInSearchMode = new ObservableBoolean(false);
    /**
     * 填充{@link androidx.viewpager2.widget.ViewPager2}的{@link Fragment}集合
     */
    private Fragment[] mPages;
    private FilterViewModel mFilterModel;
    private ChatFragment mChatFragment;
    private ContactFragment mContactFragment;
    private ActivityMainBinding mBinding;
    private FilterViewModel.EventHandler mFilterEventHandler = new FilterViewModel.EventHandler() {
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
        mFilterModel = new ViewModelProvider(MainActivity.this, new ViewModelProvider.NewInstanceFactory()).get(FilterViewModel.class);
        mFilterModel.addEventHandler(mFilterEventHandler);
    }


    private void initPages() {
        mChatFragment = new ChatFragment();
        mContactFragment = new ContactFragment();
        mPages = new Fragment[]{mChatFragment, mContactFragment, new ContactFragment()};
        mBinding.vpMain.setAdapter(new MainFragmentAdapter(this));
    }


    public void enterMultiSelectionMode() {
        UiUtils.fadeSwitchText(mBinding.tvTopBarTitle, "已选择1项");
        UiUtils.fadeSwitchImage(mBinding.ibSearch, R.drawable.ic_close_24dp);
    }


    public void enterSearchMode(View view) {
        //因为数据绑定会在下一帧执行，导致requestFocus()时EditText实际上处于disabled的状态，因而并未获取到焦点
        //当EditText被设置为enabled时，就不会显示光标
        //直接调用此方法可以立即刷新数据绑定
        mBinding.executePendingBindings();
        if (mIsInSearchMode.get()) {
            mTitle.set(getString(R.string.search));
            mBinding.etSearch.requestFocus();
            showIme(view);
        } else {
            mTitle.set(mBinding.mainBottomBar.getCurrentCaption());
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
        if (unchanged) {
            return;
        }
        mTitle.set(caption);
        FilterConfig config;
        if (position == 0) {
            //判断ChatFragment是否存在配置
            config = mChatFragment.getFilterConfig();
            if (config == null) {
                //如果不存在，抛异常，因为ChatFragment是第一个显示的Fragment,配置应该已经在onCreate中初始化
                throw new IllegalStateException("ChatFragment is not initialized! ");
            }
            //存在的话，应用之
            mFilterModel.updateCurrentConfig(config);
        } else if (position == 1) {
            //判断并应用配置
            config = mContactFragment.getFilterConfig();
            if (config != null) {
                mFilterModel.updateCurrentConfig(config);
            }
            //如果不存在配置，什么也不做，因为ContactFragment可能还没有初始化
        }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

    }

    @Override
    public void afterTextChanged(Editable s) {
        mFilterModel.search(s.toString());
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
