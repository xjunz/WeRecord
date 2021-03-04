/*
 * Copyright (c) 2021 xjunz. 保留所有权利
 */

package xjunz.tool.werecord.ui.main;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.Nullable;

import java.util.Objects;

import xjunz.tool.werecord.R;
import xjunz.tool.werecord.impl.model.account.Account;
import xjunz.tool.werecord.ui.base.RecycleAwareActivity;
import xjunz.tool.werecord.util.UiUtils;

public class ImageViewerActivity extends RecycleAwareActivity {
    public static final String EXTRA_ACCOUNT = "ImageViewerActivity.extra.account";
    ImageView mIvAvatar;

    @Override
    protected void onCreateNormally(@Nullable Bundle savedInstanceState) {
        setContentView(R.layout.activity_image_viewer);
        mIvAvatar = findViewById(R.id.iv_avatar);
        setWithAndHeight();
        Intent intent = getIntent();
        Account contact = Objects.requireNonNull(intent.getParcelableExtra(EXTRA_ACCOUNT), "Got null account");
        Bitmap bitmap = contact.getAvatar();
        if (bitmap == null) {
            UiUtils.toast(R.string.no_local_avatar_found);
            mIvAvatar.setImageResource(R.mipmap.avatar_default);
        } else {
            mIvAvatar.setImageBitmap(bitmap);
        }
    }

    @SuppressWarnings("SuspiciousNameCombination")
    private void setWithAndHeight() {
        ViewGroup.LayoutParams lp = mIvAvatar.getLayoutParams();
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        lp.width = screenWidth;
        lp.height = screenWidth;
        mIvAvatar.setLayoutParams(lp);
    }
}
