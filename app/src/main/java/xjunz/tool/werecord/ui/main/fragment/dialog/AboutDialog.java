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
import androidx.core.view.OneShotPreDrawListener;
import androidx.fragment.app.DialogFragment;

import xjunz.tool.werecord.R;
import xjunz.tool.werecord.databinding.DialogAboutBinding;
import xjunz.tool.werecord.util.ActivityUtils;

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
        OneShotPreDrawListener.add(mIcon, () -> {
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
        });
    }

    public void gotoDonate() {
        ActivityUtils.viewUri(requireContext(), "alipayqr://platformapi/startapp?saId=10000007&qrcode=https://qr.alipay.com/fkx154567xmljmwmkmchc65?t=1606376518084");
    }
}
