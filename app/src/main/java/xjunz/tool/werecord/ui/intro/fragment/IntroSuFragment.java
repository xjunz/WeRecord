/*
 * Copyright (c) 2021 xjunz. 保留所有权利
 */

package xjunz.tool.werecord.ui.intro.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.jaredrummler.android.shell.Shell;

import xjunz.tool.werecord.R;

public class IntroSuFragment extends IntroFragment implements View.OnClickListener {
    private static final String TAG_SU_UNKNOWN = "SU";
    private static final String TAG_SU_FAIL = "SF";
    private static final String TAG_SU_SUCCESS = "SS";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_intro_su, container, false);
        view.findViewById(R.id.btn_check).setOnClickListener(this);
        return view;
    }


    @Override
    public int getIconResource() {
        return R.drawable.ic_hexagon_144dp;
    }

    @Override
    public int getTitleResource() {
        return R.string.super_user;
    }

    @Override
    public int getStepIndex() {
        return 1;
    }


    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btn_check) {
            Button btn = (Button) v;
            String tag = (String) btn.getTag();
            if (tag == null) {
                tag = TAG_SU_UNKNOWN;
            }
            switch (tag) {
                case TAG_SU_FAIL:
                    //just fall through
                case TAG_SU_UNKNOWN:
                    if (Shell.SU.available()) {
                        btn.setText(R.string.check_succeeded);
                        v.setEnabled(false);
                        v.setTag(TAG_SU_SUCCESS);
                        notifyStepDone();
                    } else {
                        btn.setText(R.string.fail_and_click_to_retry);
                        v.setEnabled(true);
                        v.setTag(TAG_SU_FAIL);
                    }
                    break;
            }
        }
    }
}
