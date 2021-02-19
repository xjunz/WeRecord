/*
 * Copyright (c) 2021 xjunz. 保留所有权利
 */
package xjunz.tool.werecord.ui.message;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import xjunz.tool.werecord.R;
import xjunz.tool.werecord.databinding.ActivityEditorBinding;
import xjunz.tool.werecord.impl.model.message.Message;
import xjunz.tool.werecord.ui.base.EditorFragment;
import xjunz.tool.werecord.ui.base.RecycleSensitiveActivity;
import xjunz.tool.werecord.ui.customview.MasterToast;
import xjunz.tool.werecord.ui.message.fragment.AdvancedEditorFragment;
import xjunz.tool.werecord.ui.message.fragment.SimpleEditorFragment;
import xjunz.tool.werecord.util.UiUtils;

public class EditorActivity extends RecycleSensitiveActivity {
    public static final String EXTRA_EDIT_MODE = "EditorActivity.extra.EditMode";
    /**
     * 发送时间的起始时限，添加模式传入
     */
    public static final String EXTRA_SEND_TIMESTAMP_START = "EditorActivity.extra.SendTimestamp.START";
    /**
     * 发送时间的结束时限，添加模式传入
     */
    public static final String EXTRA_SEND_TIMESTAMP_STOP = "EditorActivity.extra.SendTimestamp.STOP";
    public static final String EXTRA_MESSAGE_ORIGIN = "EditorActivity.extra.message.ORIGIN";
    private EditorFragment[] mEditorPages;
    /**
     * 编辑模式：编辑原消息
     */
    public static final int EDIT_MODE_EDIT = 1;
    /**
     * 编辑模式：在前面添加消息
     */
    public static final int EDIT_MODE_ADD_BEFORE = 3;
    /**
     * 编辑模式：在后面添加消息
     */
    public static final int EDIT_MODE_ADD_AFTER = 5;
    private boolean mHasShowAdvancedEditorWarning = false;
    private ActivityEditorBinding mBinding;

    @Override
    protected void onCreateNormally(@Nullable Bundle savedInstanceState) {
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_editor);
        Intent intent = getIntent();
        int editionMode = intent.getIntExtra(EXTRA_EDIT_MODE, -1);
        Message origin = intent.getParcelableExtra(EXTRA_MESSAGE_ORIGIN);
        if (editionMode != EDIT_MODE_EDIT) {
            long def = generateDefaultSendTimestamp(intent.getLongExtra(EXTRA_SEND_TIMESTAMP_START, -1), intent.getLongExtra(EXTRA_SEND_TIMESTAMP_STOP, -1), editionMode);
            origin.setCreateTimeStamp(def);
        }
        initPages(origin, editionMode);
    }

    private void initPages(Message message, int editionMode) {
        mEditorPages = new EditorFragment[2];
        mEditorPages[0] = new SimpleEditorFragment().setEditionMode(editionMode).setOrigin(message);
        mEditorPages[1] = new AdvancedEditorFragment().setEditionMode(editionMode).setOrigin(message);
        mBinding.vpEditor.setAdapter(new EditorPageAdapter());
        mBinding.tvTabAdvancedEditor.getBackground().setAlpha(0);
        mBinding.vpEditor.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                if (mEditorPages[position] instanceof AdvancedEditorFragment && !mHasShowAdvancedEditorWarning) {
                    mHasShowAdvancedEditorWarning = true;
                    UiUtils.createCaveat(EditorActivity.this, R.string.warning_advanced_editor).show();
                }
                mBinding.setEdited(mEditorPages[position].isEdited());
            }

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels);
                if (positionOffset != 0) {
                    mBinding.tvTabAdvancedEditor.getBackground().setAlpha((int) (positionOffset * 255 + .5));
                    mBinding.tvTabSimpleEditor.getBackground().setAlpha((int) ((1 - positionOffset) * 255 + .5));
                }
            }
        });
    }

    private class EditorPageAdapter extends FragmentStateAdapter {

        public EditorPageAdapter() {
            super(EditorActivity.this);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            return mEditorPages[position];
        }

        @Override
        public int getItemCount() {
            return mEditorPages.length;
        }
    }

    public void resetClone(View view) {
        mEditorPages[mBinding.vpEditor.getCurrentItem()].reset();
        MasterToast.shortToast(R.string.reset_completed);
    }

    /**
     * 确认更改
     */
    public void confirmEdition(View view) {
        EditorFragment editor = mEditorPages[mBinding.vpEditor.getCurrentItem()];
        if (!editor.isEdited().get()) {
            MasterToast.shortToast(R.string.no_change_was_made);
        } else {
            Intent intent = new Intent();
            intent.putExtra(EXTRA_MESSAGE_ORIGIN, editor.getEditedMessage());
            setResult(RESULT_OK, intent);
        }
        finishAfterTransition();
    }

    /**
     * 默认添加的消息的时间差，默认值为一秒。即若“在前面添加”，则默认在目标消息
     * 前一秒添加消息，相反，在其后一秒添加消息。
     */
    public static final long DEFAULT_TIME_OFFSET = 1000L;

    private long generateDefaultSendTimestamp(long startLimit, long endLimit, int editionMode) {
        //如果起始和结束都有时限
        if (startLimit != -1 && endLimit != -1) {
            //且两者时间差超过默认时间差
            if (endLimit - startLimit >= DEFAULT_TIME_OFFSET) {
                //根据是否为在前面添加消息，向前或向后调整发送时间
                return editionMode == EDIT_MODE_ADD_BEFORE ? endLimit - DEFAULT_TIME_OFFSET : startLimit + DEFAULT_TIME_OFFSET;
            } else {
                //否则，选择两者中间的时间点
                return (endLimit + startLimit) / 2;
            }
        } else if (startLimit == -1) {
            //如果没有起始时限，则返回结束时限前的默认时间差的时间戳
            return endLimit - DEFAULT_TIME_OFFSET;
        } else {
            //如果没有结束时限，返回起始时限后的默认时间差的时间戳
            return startLimit + DEFAULT_TIME_OFFSET;
        }
    }


}
