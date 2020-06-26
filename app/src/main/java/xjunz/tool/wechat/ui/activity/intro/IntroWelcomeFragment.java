package xjunz.tool.wechat.ui.activity.intro;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import xjunz.tool.wechat.R;

public class IntroWelcomeFragment extends IntroFragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        notifyStepDone();
        return inflater.inflate(R.layout.fragment_intro_welcome, container, false);
    }

    @Override
    public int getIconResource() {
        return R.drawable.ic_favorite_144dp;
    }

    @Override
    public int getTitleResource() {
        return R.string.welcome;
    }


    @Override
    public int getStepIndex() {
        return 0;
    }
}
