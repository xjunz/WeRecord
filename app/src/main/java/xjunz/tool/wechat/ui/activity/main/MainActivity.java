package xjunz.tool.wechat.ui.activity.main;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import xjunz.tool.wechat.R;
import xjunz.tool.wechat.ui.activity.BaseActivity;
import xjunz.tool.wechat.ui.activity.main.model.FilterConfiguration;
import xjunz.tool.wechat.ui.activity.main.model.FilterViewModel;
import xjunz.tool.wechat.ui.customview.BottomBar;
import xjunz.tool.wechat.ui.customview.MainPanel;
import xjunz.tool.wechat.ui.transition.MorphDrawable;
import xjunz.tool.wechat.util.AnimUtils;
import xjunz.tool.wechat.util.UiUtils;

import static xjunz.tool.wechat.ui.activity.main.model.FilterConfiguration.PAYLOAD_CONFIRM_FILTER;
import static xjunz.tool.wechat.ui.activity.main.model.FilterConfiguration.PAYLOAD_INIT;


public class MainActivity extends BaseActivity implements BottomBar.OnBottomBarItemClickedListener, TextWatcher {

    private ViewPager2 mViewPager;
    private Fragment[] mPages;
    private ViewGroup mTopBar;
    private TextView mTvTitle;
    private EditText mEtSearch;
    private ImageButton mIbFilter;
    private ProgressBar mPbLoad;
    private BottomBar mBottomBar;
    private FilterViewModel mFilterModel;
    private MainPanel mPanel;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews();
        initPages();
        mFilterModel = new ViewModelProvider(MainActivity.this, new ViewModelProvider.NewInstanceFactory()).get(FilterViewModel.class);
        mFilterModel.getCurrentConfig().observe(this, new Observer<FilterConfiguration>() {
            @Override
            public void onChanged(FilterConfiguration configuration) {
                if (configuration.getPayload() == PAYLOAD_CONFIRM_FILTER) {
                    mPanel.closePanel();
                }
            }
        });
    }


    private void initViews() {
        mPanel = findViewById(R.id.main_panel);
        mTopBar = findViewById(R.id.top_bar);
        mBottomBar = findViewById(R.id.bottom_bar);
        mBottomBar.setOnBottomBarItemClickedListener(this);
        mViewPager = findViewById(R.id.vp_main);
        mViewPager.setUserInputEnabled(false);
        mTvTitle = findViewById(R.id.tv_top_bar_title);
        mIbFilter = findViewById(R.id.ib_filter);
        mPbLoad = findViewById(R.id.pb_load);
        mEtSearch = findViewById(R.id.et_search);
        mEtSearch.addTextChangedListener(this);
    }

    private void initPages() {
        mPages = new Fragment[3];
        ChatFragment chatFragment = new ChatFragment();
        mPages[0] = chatFragment;
        mPages[1] = new ContactFragment();
        mPages[2] = new ChatFragment();
        mViewPager.setAdapter(new MainFragmentAdapter(this));
    }


    public void switchLoadingMode(final boolean into) {
        mPbLoad.animate().alpha(into ? 1f : 0f).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                if (!into) {
                    mPbLoad.setVisibility(View.GONE);
                }
            }

            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                if (into) {
                    mPbLoad.setVisibility(View.VISIBLE);
                }
            }
        }).start();
    }

    public void enterMultiSelectionMode() {
        UiUtils.fadeSwitchText(mTvTitle, "已选择1项");
        UiUtils.fadeSwitchImage(mIbFilter, R.drawable.ic_close_24dp);
    }


    public void enterSearchMode(View view) {
        final int backColor = UiUtils.getAttrColor(MainActivity.this, R.attr.colorPrimary);
        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float fraction = animation.getAnimatedFraction();
                MorphDrawable morphDrawable = new MorphDrawable(backColor, fraction * 40 / 2f);
                mTopBar.setBackground(morphDrawable);
                LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) mTopBar.getLayoutParams();
                lp.leftMargin = UiUtils.dip2px(fraction * 8);
                lp.rightMargin = UiUtils.dip2px(fraction * 8);
                // lp.topMargin = (int) (fraction * (mInsetTop + UiUtils.dip2px(8)));
                // lp.height = (int) (mTopBarHeight - fraction * (mTopBarHeight - mActionBarHeight + UiUtils.dip2px(8)));
                mTopBar.setLayoutParams(lp);
                // mTopBar.setPadding(0, (int) ((1 - fraction) * mInsetTop), 0, 0);
            }
        });


        if (mTvTitle.getText().toString().equals(getString(R.string.search))) {
            //exit
            //mChatPage.enterSearchMode(false);
            UiUtils.enable(mIbFilter);
            UiUtils.disable(mEtSearch);
            hideIme(mEtSearch);
            UiUtils.translateY(mBottomBar, 0);
            animator.setInterpolator(AnimUtils.getFastOutLinearInInterpolator());
            // animator.reverse();
            UiUtils.fadeSwitchText(mTvTitle, R.string.chat);
            UiUtils.fadeSwitchImage((ImageView) view, R.drawable.ic_search_24dp);
        } else {
            //enter
            // mChatPage.enterSearchMode(true);
            UiUtils.disable(mIbFilter);
            UiUtils.enable(mEtSearch);
            mEtSearch.requestFocus();
            animator.setInterpolator(AnimUtils.getLinearOutSlowInInterpolator());
            UiUtils.fadeSwitchImage((ImageView) view, R.drawable.ic_close_24dp);
            UiUtils.fadeSwitchText(mTvTitle, R.string.search);
        }
    }

    public void showFilterPanel(View view) {
        View filterPanel = findViewById(R.id.fl_filter);
    }

    @Override
    public void onItemClicked(int position, CharSequence caption, boolean unchanged) {
        if (unchanged) {
            return;
        }
        UiUtils.fadeSwitchText(mTvTitle, caption);
        FilterConfiguration config;
        if (position == 0) {
            config = mFilterModel.getTalkerConfiguration().getValue();
            if (config == null) {
                throw new IllegalStateException("ChatFragment's config is not initialized yet!");
            }
            mFilterModel.config(config, PAYLOAD_INIT);
        } else if (position == 1) {
            config = mFilterModel.getContactConfiguration().getValue();
            if (config != null) {
                mFilterModel.config(config, PAYLOAD_INIT);
            }
        }
        mViewPager.setCurrentItem(position);
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

    }

    @Override
    public void afterTextChanged(Editable s) {

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
