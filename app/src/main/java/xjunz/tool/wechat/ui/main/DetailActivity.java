/*
 * Copyright (c) 2020 xjunz. 保留所有权利
 */

package xjunz.tool.wechat.ui.main;

import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;

import xjunz.tool.wechat.R;
import xjunz.tool.wechat.databinding.ActivityDetailBinding;
import xjunz.tool.wechat.impl.model.account.Contact;
import xjunz.tool.wechat.impl.model.account.Talker;
import xjunz.tool.wechat.ui.BaseActivity;

public class DetailActivity extends BaseActivity {
    public static final String EXTRA_DATA = "DetailActivity.extra.data";
    private ActivityDetailBinding mBinding;
    private Contact mData;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_detail);
        if (intent != null) {
            mData = (Contact) intent.getSerializableExtra(EXTRA_DATA);
            if (mData != null) {
                mBinding.setContact(mData);
                if (mData instanceof Talker) {
                    mBinding.setTalker((Talker) mData);
                }
            }
        }
    }


    public void viewImage(View view) {
        ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(this, view, view.getTransitionName());
        Intent i = new Intent(this, ImageViewerActivity.class);
        i.putExtra(ImageViewerActivity.EXTRA_DATA, mData);
        startActivity(i, options.toBundle());
    }

    public void checkMessages(View view) {
        Intent i = new Intent(this, MessageActivity.class);
        i.putExtra(MessageActivity.EXTRA_DATA, mData);
        startActivity(i);
        this.finish();
    }
}
