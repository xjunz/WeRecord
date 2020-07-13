/*
 * Copyright (c) 2020 xjunz. 保留所有权利
 */

package xjunz.tool.wechat.ui.intro.fragment;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import io.reactivex.CompletableObserver;
import io.reactivex.disposables.Disposable;
import xjunz.tool.wechat.App;
import xjunz.tool.wechat.BuildConfig;
import xjunz.tool.wechat.R;
import xjunz.tool.wechat.impl.Environment;
import xjunz.tool.wechat.ui.outer.DebugActivity;
import xjunz.tool.wechat.util.UiUtils;
import xjunz.tool.wechat.util.UniUtils;

public class IntroAvailabilityFragment extends IntroFragment implements View.OnClickListener {
    private Dialog mProgressDialog;
    private Button mBtnCheck;

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        if (Environment.getInstance().initialized() && savedInstanceState != null) {
            mBtnCheck.setText(R.string.check_succeeded);
            mBtnCheck.setEnabled(false);
            notifyStepDone();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_intro_availability, container, false);
        mBtnCheck = view.findViewById(R.id.btn_check);
        mBtnCheck.setOnClickListener(this);
        mProgressDialog = UiUtils.createProgressDialog(requireActivity(), R.string.checking);
        return view;
    }

    private CompletableObserver mObserver = new CompletableObserver() {
        @Override
        public void onSubscribe(Disposable d) {
            mProgressDialog.show();
        }

        @Override
        public void onComplete() {
            mProgressDialog.dismiss();
            mBtnCheck.setText(R.string.check_succeeded);
            App.getSharedPrefsManager().putIMEI(Environment.getInstance().getIMEI());
            notifyStepDone();
        }

        @Override
        public void onError(final Throwable e) {
            mBtnCheck.setEnabled(true);
            mProgressDialog.dismiss();
            final String log = e.getClass().getName() + ": " + e.getMessage() + "\n" + "Serial: " + Environment.getInstance().serialize();
            AlertDialog.Builder builder = UiUtils.createDialog(requireActivity(), R.string.error_occurred, log)
                    .setPositiveButton(R.string.feedback, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (UniUtils.feedbackTempQChat(requireActivity())) {
                                UniUtils.copyPlainText("feedback message", log);
                                UiUtils.toast(R.string.qchat_feedback_tip);
                            }
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
    public void onClick(View v) {
        v.setEnabled(false);
        Environment.getInstance().init(mObserver);
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
        return 3;
    }
}
