/*
 * Copyright (c) 2021 xjunz. 保留所有权利
 */
package xjunz.tool.werecord.ui.customview;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;

import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import xjunz.tool.werecord.R;
import xjunz.tool.werecord.databinding.ItemMoreOptionBinding;

/**
 * @author xjunz 2021/2/14 13:36
 */
public class OptionItemView extends ConstraintLayout {
    private ItemMoreOptionBinding mBinding;

    public OptionItemView(Context context) {
        super(context);
        init(context);
    }

    public OptionItemView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.OptionItemView);
        if (typedArray.hasValue(R.styleable.OptionItemView_optionIcon)) {
            mBinding.ivIcon.setImageDrawable(typedArray.getDrawable(R.styleable.OptionItemView_optionIcon));
        }
        if (typedArray.hasValue(R.styleable.OptionItemView_optionTitle)) {
            mBinding.tvTitle.setText(typedArray.getText(R.styleable.OptionItemView_optionTitle));
        }
        if (typedArray.hasValue(R.styleable.OptionItemView_optionDes)) {
            mBinding.tvDes.setText(typedArray.getText(R.styleable.OptionItemView_optionDes));
        } else {
            mBinding.tvDes.setVisibility(GONE);
        }
        mBinding.divider.setVisibility(typedArray.getBoolean(R.styleable.OptionItemView_optionShowTopDivider, true) ? VISIBLE : GONE);
        mBinding.switchMaterial.setVisibility(typedArray.getBoolean(R.styleable.OptionItemView_optionShowSwitch, false) ? VISIBLE : GONE);
        typedArray.recycle();
    }

    private void init(Context context) {
        mBinding = ItemMoreOptionBinding.inflate(LayoutInflater.from(context), this, true);
        setBackground(ContextCompat.getDrawable(context, R.drawable.bg_selectable_item_floating));
        setClickable(true);
        setFocusable(true);
        setDescendantFocusability(FOCUS_BEFORE_DESCENDANTS);
    }

    public void setChecked(boolean checked) {
        mBinding.switchMaterial.setChecked(checked);
    }
}
