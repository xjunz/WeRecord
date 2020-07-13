/*
 * Copyright (c) 2020 xjunz. 保留所有权利
 */

package xjunz.tool.wechat.ui.main;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;

import androidx.annotation.Nullable;

import com.bm.library.PhotoView;

import xjunz.tool.wechat.R;
import xjunz.tool.wechat.impl.model.account.Contact;
import xjunz.tool.wechat.ui.BaseActivity;
import xjunz.tool.wechat.ui.customview.MasterToast;

public class ImageViewerActivity extends BaseActivity {
    public static final String EXTRA_DATA = "ImageViewerActivity.extra.data";
    PhotoView photoView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_viewer);
        photoView = findViewById(R.id.photo_view);
        Intent intent = getIntent();
        if (intent != null) {
            Contact contact = (Contact) intent.getSerializableExtra(EXTRA_DATA);
            if (contact != null) {
                Bitmap bitmap = contact.getAvatar();
                if (bitmap == null) {
                    MasterToast.shortToast("No avatar");
                } else {
                    photoView.setImageBitmap(bitmap);
                    photoView.enable();

                }
            }
        }
    }
}
