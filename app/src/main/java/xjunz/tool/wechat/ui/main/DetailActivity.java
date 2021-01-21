/*
 * Copyright (c) 2021 xjunz. 保留所有权利
 */

package xjunz.tool.wechat.ui.main;

import android.annotation.SuppressLint;
import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;

import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;

import org.jetbrains.annotations.NotNull;

import xjunz.tool.wechat.BuildConfig;
import xjunz.tool.wechat.R;
import xjunz.tool.wechat.databinding.ActivityDetailBinding;
import xjunz.tool.wechat.impl.DatabaseModifier;
import xjunz.tool.wechat.impl.model.account.Contact;
import xjunz.tool.wechat.impl.model.account.Talker;
import xjunz.tool.wechat.impl.repo.ContactRepository;
import xjunz.tool.wechat.impl.repo.RepositoryFactory;
import xjunz.tool.wechat.ui.base.RecycleSensitiveActivity;
import xjunz.tool.wechat.ui.customview.MasterToast;
import xjunz.tool.wechat.ui.message.MessageActivity;
import xjunz.tool.wechat.ui.message.fragment.dialog.LvBufferEditorDialog;
import xjunz.tool.wechat.util.IOUtils;
import xjunz.tool.wechat.util.RxJavaUtils;
import xjunz.tool.wechat.util.UiUtils;

public class DetailActivity extends RecycleSensitiveActivity implements PopupMenu.OnMenuItemClickListener {
    public static final String EXTRA_CONTACT = "DetailActivity.extra.contact";
    public static final String EXTRA_CONTACT_ID = "DetailActivity.extra.ContactId";
    private Contact mData;
    private PopupMenu mPopupMenu;

    @Override
    protected void onCreateNormally(@Nullable Bundle savedInstanceState) {
        Intent intent = getIntent();
        ActivityDetailBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_detail);
        if (intent != null) {
            mData = intent.getParcelableExtra(EXTRA_CONTACT);
            if (mData == null) {
                String id = intent.getStringExtra(EXTRA_CONTACT_ID);
                if (id == null) {
                    MasterToast.shortToast("Got null data and null id.");
                    finish();
                    return;
                } else {
                    binding.setId(id);
                    mData = RepositoryFactory.get(ContactRepository.class).get(id);
                }
            }
            if (mData != null) {
                binding.setId(mData.id);
                binding.setContact(mData);
                boolean isTalker = mData instanceof Talker;
                if (isTalker) {
                    binding.setTalker((Talker) mData);
                }
                initPopupMenu(binding.ibMore, isTalker);
            } else {
                UiUtils.gone(binding.ibMore);
            }
        }
    }

    //todo
    private void initPopupMenu(View anchor, boolean talker) {
        mPopupMenu = new PopupMenu(this, anchor);
        mPopupMenu.getMenuInflater().inflate(R.menu.contact_detail, mPopupMenu.getMenu());
        mPopupMenu.getMenu().findItem(R.id.item_mark_as_unread).setVisible(talker);
        mPopupMenu.getMenu().findItem(R.id.item_lv_buffer).setVisible(BuildConfig.DEBUG);
        anchor.setOnTouchListener(mPopupMenu.getDragToOpenListener());
        mPopupMenu.setOnMenuItemClickListener(this);
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onMenuItemClick(@NotNull MenuItem item) {
        int id = item.getItemId();
        DatabaseModifier modifier = getEnvironment().modifyDatabase();
        switch (id) {
            case R.id.item_delete_locally:
                RxJavaUtils.complete(() -> {
                    if (modifier.deleteContactWithId(mData.id)) {
                        modifier.apply();
                    }
                }).subscribe(new RxJavaUtils.CompletableObservableAdapter() {
                    @Override
                    public void onComplete() {
                        super.onComplete();
                        MasterToast.shortToast("完成");
                    }

                    @Override
                    public void onError(@NotNull Throwable e) {
                        super.onError(e);
                        UiUtils.createError(DetailActivity.this, IOUtils.readStackTraceFromThrowable(e)).show();
                    }
                });
                break;
            case R.id.item_mark_as_unread:

                break;
            case R.id.item_lv_buffer:
                new LvBufferEditorDialog().setTypeSerial(Contact.LV_BUFFER_READ_SERIAL).setDefault(mData.getParsedLvBuffer()).show(getSupportFragmentManager(), "lv_buffer");
                break;
        }
        return true;
    }

    public void viewImage(View view) {
        ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(this, view, view.getTransitionName());
        Intent i = new Intent(this, ImageViewerActivity.class);
        i.putExtra(ImageViewerActivity.EXTRA_ACCOUNT, mData);
        startActivity(i, options.toBundle());
    }

    public void checkMessages(View view) {
        Intent i = new Intent(this, MessageActivity.class);
        i.putExtra(MessageActivity.EXTRA_TALKER, mData);
        startActivity(i, ActivityOptions.makeSceneTransitionAnimation(this, view, view.getTransitionName()).toBundle());
    }

    public void showMore(View view) {
        mPopupMenu.show();
    }


}
