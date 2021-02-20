/*
 * Copyright (c) 2021 xjunz. 保留所有权利
 */

package xjunz.tool.werecord.ui.intro.fragment;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import io.reactivex.CompletableObserver;
import io.reactivex.disposables.Disposable;
import xjunz.tool.werecord.R;
import xjunz.tool.werecord.impl.Environment;
import xjunz.tool.werecord.util.UiUtils;

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
            notifyStepDone();
        }

        @Override
        public void onError(@NotNull final Throwable e) {
            mBtnCheck.setEnabled(true);
            mProgressDialog.dismiss();
            UiUtils.showError(requireContext(), e);
        }
    };

    @Override
    public void onClick(@NotNull View v) {
        v.setEnabled(false);
        Environment.create().init(mObserver);
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
