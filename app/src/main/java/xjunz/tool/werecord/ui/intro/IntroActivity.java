/*
 * Copyright (c) 2021 xjunz. 保留所有权利
 */

package xjunz.tool.werecord.ui.intro;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.text.HtmlCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import java.io.IOException;

import xjunz.tool.werecord.App;
import xjunz.tool.werecord.R;
import xjunz.tool.werecord.ui.base.BaseActivity;
import xjunz.tool.werecord.ui.customview.MasterToast;
import xjunz.tool.werecord.ui.intro.fragment.IntroAvailabilityFragment;
import xjunz.tool.werecord.ui.intro.fragment.IntroFragment;
import xjunz.tool.werecord.ui.intro.fragment.IntroSuFragment;
import xjunz.tool.werecord.ui.intro.fragment.IntroWelcomeFragment;
import xjunz.tool.werecord.ui.outer.InitializationActivity;
import xjunz.tool.werecord.util.IoUtils;
import xjunz.tool.werecord.util.UiUtils;

public class IntroActivity extends BaseActivity implements IntroFragment.OnStepDoneListener {
    private ViewPager2 mViewPager;
    private IntroFragment[] mPages;
    private TextView mTvTitle, mTvIndicator;
    private ImageView mIvIcon;
    private ImageButton mIbNext, mIbPrevious;
    private final ViewPager2.OnPageChangeCallback mOnPageChangeCallback = new ViewPager2.OnPageChangeCallback() {
        @Override
        public void onPageSelected(int position) {
            IntroFragment fragment = mPages[position];
            UiUtils.fadeSwitchText(mTvTitle, fragment.getTitleResource());
            mIvIcon.setImageResource(fragment.getIconResource());
            mTvIndicator.setText(String.format("%s/%s", position + 1, mPages.length));
            if (position == 0) {
                mIbPrevious.setVisibility(View.INVISIBLE);
            } else {
                mIbPrevious.setVisibility(View.VISIBLE);
            }
            if (fragment.isThisStepDone()) {
                mIbNext.setVisibility(View.VISIBLE);
                if (position == mPages.length - 1) {
                    mIbNext.setImageResource(R.drawable.ic_check_24dp);
                } else {
                    mIbNext.setImageResource(R.drawable.ic_keyboard_arrow_right_24dp);
                }
            } else {
                mIbNext.setVisibility(View.INVISIBLE);
            }
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro);
        initPages();
        initViews();
        mViewPager.setAdapter(new IntroViewPagerAdapter(getSupportFragmentManager(), this.getLifecycle()));
    }

    private void initViews() {
        mTvTitle = findViewById(R.id.tv_title);
        mIvIcon = findViewById(R.id.iv_icon);
        mTvIndicator = findViewById(R.id.tv_indicator);
        mViewPager = findViewById(R.id.vp_intro);
        // Block swipe
        mViewPager.setUserInputEnabled(false);
        mViewPager.registerOnPageChangeCallback(mOnPageChangeCallback);
        mTvTitle.setText(mPages[0].getTitleResource());
        mIvIcon.setImageResource(mPages[0].getIconResource());
        mTvIndicator.setText(String.format("%s/%s", 1, mPages.length));
        mIbNext = findViewById(R.id.ib_next_step);
        mIbPrevious = findViewById(R.id.ib_previous_step);
        mIbPrevious.setVisibility(View.GONE);
    }

    private void initPages() {
        mPages = new IntroFragment[3];
        mPages[0] = new IntroWelcomeFragment();
        mPages[1] = new IntroSuFragment();
        //  mPages[2] = new IntroPermissionFragment();
        mPages[2] = new IntroAvailabilityFragment();
        IntroFragment.setOnStepDoneListener(this);
    }


    @Override
    public void onStepDone(int step) {
        mIbNext.setVisibility(View.VISIBLE);
        if (step == mPages.length - 1) {
            mIbNext.setImageResource(R.drawable.ic_check_24dp);
        }
    }

    private class IntroViewPagerAdapter extends FragmentStateAdapter {

        IntroViewPagerAdapter(@NonNull FragmentManager fragmentManager, @NonNull Lifecycle lifecycle) {
            super(fragmentManager, lifecycle);
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

    public void gotoPrevious(View view) {
        mViewPager.setCurrentItem(mViewPager.getCurrentItem() - 1);
    }


    public void gotoNext(View view) {
        if (mViewPager.getCurrentItem() == mPages.length - 1) {
            try {
                new AlertDialog.Builder(this, R.style.Theme_AppCompat_Light_DialogWhenLarge_Material).setTitle(R.string.declaration)
                        .setMessage(HtmlCompat.fromHtml(IoUtils.readAssetAsString("declaration.html"), HtmlCompat.FROM_HTML_MODE_LEGACY))
                        .setPositiveButton(R.string.read_and_approved, (dialog, which) -> {
                            App.getSharedPrefsManager().setIsAppIntroDone(true);
                            Intent i = new Intent(this, InitializationActivity.class);
                            i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(i);
                        })
                        .setNegativeButton(R.string.exit, (dialog, which) -> finish())
                        .show();
            } catch (IOException e) {
                MasterToast.shortToast(R.string.error_occurred);
                finish();
            }
        } else {
            mViewPager.setCurrentItem(mViewPager.getCurrentItem() + 1);
        }

    }
}
