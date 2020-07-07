package xjunz.tool.wechat.ui.intro;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import java.io.IOException;
import java.io.InputStream;

import xjunz.tool.wechat.App;
import xjunz.tool.wechat.R;
import xjunz.tool.wechat.ui.BaseActivity;
import xjunz.tool.wechat.ui.intro.fragment.IntroAvailabilityFragment;
import xjunz.tool.wechat.ui.intro.fragment.IntroFragment;
import xjunz.tool.wechat.ui.intro.fragment.IntroPermissionFragment;
import xjunz.tool.wechat.ui.intro.fragment.IntroSuFragment;
import xjunz.tool.wechat.ui.intro.fragment.IntroWelcomeFragment;
import xjunz.tool.wechat.ui.outer.SplashActivity;

public class IntroActivity extends BaseActivity implements IntroFragment.OnStepDoneListener {
    private ViewPager2 mViewPager;
    private IntroFragment[] mPages;
    private TextView mTvTitle, mTvIndicator;
    private ImageView mIvIcon;
    private ImageButton mIbNext, mIbPrevious;
    private ViewPager2.OnPageChangeCallback mOnPageChangeCallback = new ViewPager2.OnPageChangeCallback() {
        @Override
        public void onPageSelected(int position) {
            IntroFragment fragment = mPages[position];
            mTvTitle.setText(fragment.getTitleResource());
            mIvIcon.setImageResource(fragment.getIconResource());
            mTvIndicator.setText(String.format(getString(R.string.indicator_text), position + 1, mPages.length));
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
        mTvIndicator.setText(String.format(getString(R.string.indicator_text), 1, mPages.length));
        mIbNext = findViewById(R.id.ib_next_step);
        mIbPrevious = findViewById(R.id.ib_previous_step);
        mIbPrevious.setVisibility(View.GONE);
    }

    private void initPages() {
        mPages = new IntroFragment[4];
        mPages[0] = new IntroWelcomeFragment();
        mPages[1] = new IntroSuFragment();
        mPages[2] = new IntroPermissionFragment();
        mPages[3] = new IntroAvailabilityFragment();
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
            App.getSharedPrefsManager().setIsAppIntroDone(true);
            Intent i = new Intent(this, SplashActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        }
        if (mViewPager.getCurrentItem() == 0) {
            try {
                InputStream inputStream = getAssets().open("dc-191013-01.html");
                byte[] all = new byte[inputStream.available()];
                if (inputStream.read(all) == all.length) {
                    new AlertDialog.Builder(this, R.style.Theme_AppCompat_Light_DialogWhenLarge_Material).setTitle(R.string.declaration)
                            .setMessage(Html.fromHtml(new String(all)))
                            .setPositiveButton(R.string.read_and_approved, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    mViewPager.setCurrentItem(mViewPager.getCurrentItem() + 1);
                                }
                            })
                            .setNegativeButton(android.R.string.cancel, null)
                            .show();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            mViewPager.setCurrentItem(mViewPager.getCurrentItem() + 1);
        }

    }
}
