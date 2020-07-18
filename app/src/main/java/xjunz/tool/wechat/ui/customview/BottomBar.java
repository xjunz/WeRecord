/*
 * Copyright (c) 2020 xjunz. 保留所有权利
 */

package xjunz.tool.wechat.ui.customview;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.TooltipCompat;

import xjunz.tool.wechat.R;

/**
 * 通过{@link LinearLayout}实现一个非常简单的BottomBar
 */
public class BottomBar extends LinearLayout {
    private OnItemSelectListener mListener;
    private CharSequence[] mCaptionEntries;
    private ViewGroup[] mItemViewList;
    private CharSequence mCurrentCaption;
    private int mSelection = -1;

    public BottomBar(@NonNull Context context) {
        super(context);
    }

    public BottomBar(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.BottomBar);
        int imgEntriesID = ta.getResourceId(R.styleable.BottomBar_imageEntries, View.NO_ID);
        mCaptionEntries = ta.getTextArray(R.styleable.BottomBar_captionEntries);
        ta.recycle();
        TypedArray ta2 = getResources().obtainTypedArray(imgEntriesID);
        int[] imageEntriesRes = new int[ta2.length()];
        for (int i = 0; i < imageEntriesRes.length; i++) {
            imageEntriesRes[i] = ta2.getResourceId(i, View.NO_ID);
        }
        ta2.recycle();
        this.setOrientation(HORIZONTAL);
        this.setGravity(Gravity.CENTER_VERTICAL);
        mItemViewList = new ViewGroup[imageEntriesRes.length];
        for (int i = 0; i < imageEntriesRes.length; i++) {
            ViewGroup child = (ViewGroup) ((Activity) getContext()).getLayoutInflater().inflate(R.layout.item_bottom_bar, this, false);
            mItemViewList[i] = child;
            child.setTag(i);
            child.setOnClickListener(view -> setSelection((int) view.getTag()));
            TooltipCompat.setTooltipText(child, mCaptionEntries[(int) child.getTag()]);
            ImageView image = (ImageView) child.getChildAt(0);
            image.setImageResource(imageEntriesRes[i]);
            image.setContentDescription(mCaptionEntries[i]);
            addView(child);

        }
    }


    public BottomBar(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setOnItemSelectListener(@Nullable OnItemSelectListener listener) {
        mListener = listener;
    }

    /**
     * 设置当前选中项为{@param index}
     *
     * @param index 欲选中项的索引
     */

    public void setSelection(int index) {
        if (index != mSelection) {
            mSelection = index;
            mCurrentCaption = mCaptionEntries[index];
            for (int i = 0; i < mItemViewList.length; i++) {
                mItemViewList[i].setSelected(i == index);
            }
            if (mListener != null) {
                mListener.onItemSelect(index, mCurrentCaption, false);
            }
        } else {
            if (mListener != null) {
                mListener.onItemSelect(index, mCurrentCaption, true);
            }
        }

    }

    public int getSelection() {
        return mSelection;
    }


    public interface OnItemSelectListener {
        void onItemSelect(int position, CharSequence caption, boolean unchanged);
    }

    public CharSequence getCurrentCaption() {
        return mCurrentCaption;
    }

}
