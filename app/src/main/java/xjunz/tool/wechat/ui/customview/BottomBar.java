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

import xjunz.tool.wechat.R;

/**
 * 一个非常简单的BottomBar实现
 */
public class BottomBar extends LinearLayout {
    private OnBottomBarItemClickedListener mListener;
    private CharSequence[] mCaptionEntries;
    private ViewGroup[] mItemViewList;

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
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        mItemViewList = new ViewGroup[imageEntriesRes.length];
        for (int i = 0; i < imageEntriesRes.length; i++) {
            final ViewGroup child = (ViewGroup) ((Activity) getContext()).getLayoutInflater().inflate(R.layout.item_bottom_bar, this, false);
            mItemViewList[i] = child;
            final ImageView image = (ImageView) child.getChildAt(0);
            image.setImageResource(imageEntriesRes[i]);
            addView(child);
            final int index = i;
            if (i == 0) {
                child.setSelected(true);
            }
            mItemViewList[i].setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mListener != null) {
                        mListener.onItemClicked(index, mCaptionEntries[index], mItemViewList[index].isSelected());
                    }
                    for (int i = 0; i < mItemViewList.length; i++) {
                        ViewGroup child = mItemViewList[i];
                        child.setSelected(i == index);
                    }
                }
            });
        }
    }

    public BottomBar(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setOnBottomBarItemClickedListener(@Nullable OnBottomBarItemClickedListener listener) {
        mListener = listener;
    }

    public interface OnBottomBarItemClickedListener {
        void onItemClicked(int position, CharSequence caption, boolean unchanged);
    }


}
