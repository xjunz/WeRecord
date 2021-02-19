/*
 * Copyright (c) 2021 xjunz. 保留所有权利
 */

package xjunz.tool.werecord.ui.intro.fragment;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import org.jetbrains.annotations.NotNull;

import io.reactivex.CompletableObserver;
import io.reactivex.disposables.Disposable;
import xjunz.tool.werecord.BuildConfig;
import xjunz.tool.werecord.R;
import xjunz.tool.werecord.impl.Environment;
import xjunz.tool.werecord.ui.outer.DebugActivity;
import xjunz.tool.werecord.util.ActivityUtils;
import xjunz.tool.werecord.util.UiUtils;
import xjunz.tool.werecord.util.Utils;

public class IntroAvailabilityFragment extends IntroFragment implements View.OnClickListener {
    private Dialog mProgressDialog;
    private Button mBtnCheck;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_intro_availability, container, false);
        mBtnCheck = view.findViewById(R.id.btn_check);
        mBtnCheck.setOnClickListener(this);
        mProgressDialog = UiUtils.createProgress(requireActivity(), R.string.checking);
        return view;
    }

    private final CompletableObserver mObserver = new CompletableObserver() {
        @Override
        public void onSubscribe(@NotNull Disposable d) {
            mProgressDialog.show();
        }

        @Override
        public void onComplete() {
            mProgressDialog.dismiss();
            mBtnCheck.setText(R.string.check_succeeded);
            // App.getSharedPrefsManager().putIMEI(Environment.getInstance().getIMEI());
            notifyStepDone();
        }

        @Override
        public void onError(@NotNull final Throwable e) {
            mBtnCheck.setEnabled(true);
            mProgressDialog.dismiss();
            final String log = e.getClass().getName() + ": " + e.getMessage() + "\n" + "Serial: " + Environment.getInstance().serialize();
            AlertDialog.Builder builder = UiUtils.createDialog(requireActivity(), R.string.error_occurred, log)
                    .setPositiveButton(R.string.feedback, (dialog, which) -> {
                        if (ActivityUtils.feedbackTempQChat(requireActivity())) {
                            Utils.copyPlainText("WeRecord feedback message", log);
                            UiUtils.toast(R.string.q_chat_feedback_tip);
                        }
                    });
            if (BuildConfig.DEBUG) {
                builder.setNegativeButton(R.string.debug, (dialog, which) -> {
                    Intent i = new Intent(requireActivity(), DebugActivity.class);
                    i.putExtra(DebugActivity.EXTRA_ENV_SERIAL, log);
                    startActivity(i);
                });
            }
            builder.show();
        }
    };

    @Override
    public void onClick(@NotNull View v) {
        v.setEnabled(false);
        Environment.newInstance().init(mObserver);
    }


    @Override
    public int getIconResource() {
        return R.drawable.ic_report_144dp;
    }

    @Override
    public int getTitleResource() {
        return R.string.availability;
    }

    @Override
    public int getStepIndex() {
        return 2;
    }
}
