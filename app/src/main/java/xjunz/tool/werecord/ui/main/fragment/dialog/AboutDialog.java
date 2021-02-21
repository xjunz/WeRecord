/*
 * Copyright (c) 2021 xjunz. 保留所有权利
 */
package xjunz.tool.werecord.ui.main.fragment.dialog;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.graphics.Path;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.BounceInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import xjunz.tool.werecord.Constants;
import xjunz.tool.werecord.R;
import xjunz.tool.werecord.databinding.DialogAboutBinding;
import xjunz.tool.werecord.ui.customview.MasterToast;
import xjunz.tool.werecord.util.ActivityUtils;
import xjunz.tool.werecord.util.UiUtils;

/**
 * @author xjunz 2021/2/20 0:20
 */
public class AboutDialog extends DialogFragment {
    private ImageView mIcon;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NO_FRAME, R.style.Base_Dialog_Translucent);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        DialogAboutBinding binding = DialogAboutBinding.inflate(inflater, container, false);
        binding.setHost(this);
        mIcon = binding.ivAppIcon;
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        //todo: AdaptiveIconDrawable 实现parallax 效果
        mIcon.postDelayed(() -> {
            mIcon.setVisibility(View.VISIBLE);
            mIcon.setTranslationY(-200);
            Path scalePath = new Path();
            scalePath.moveTo(0, 0);
            scalePath.lineTo(1, 1);
            Animator scale = ObjectAnimator.ofFloat(mIcon, View.SCALE_X, View.SCALE_Y, scalePath).setDuration(200);
            scale.setInterpolator(new OvershootInterpolator(4.0f));
            ObjectAnimator transY = ObjectAnimator.ofFloat(mIcon, View.TRANSLATION_Y, -200, 0).setDuration(200);
            transY.setInterpolator(new BounceInterpolator());
            AnimatorSet set = new AnimatorSet();
            set.playSequentially(scale, transY);
            set.start();
        }, 200);
    }

    public void gotoDonate() {
        ActivityUtils.safeViewUri(requireContext(), Constants.URI_DONATE_ALIPAY);
    }

    private long lastTapTimestamp;
    private int legalTapTimes;

    public void switchUserDebuggable() {
        if (legalTapTimes == 4) {
            UiUtils.swing(mIcon);
            Constants.USER_DEBUGGABLE = !Constants.USER_DEBUGGABLE;
            MasterToast.shortToast("USER_DEBUGGABLE = " + Constants.USER_DEBUGGABLE);
            legalTapTimes = 0;
        } else {
            long cur = System.currentTimeMillis();
            if (cur - lastTapTimestamp <= 300) {
                legalTapTimes++;
            } else {
                legalTapTimes = 1;
            }
            lastTapTimestamp = cur;
        }
    }
}
