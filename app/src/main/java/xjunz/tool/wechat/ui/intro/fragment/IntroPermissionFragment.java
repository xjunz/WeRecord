/*
 * Copyright (c) 2020 xjunz. 保留所有权利
 */

package xjunz.tool.wechat.ui.intro.fragment;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import xjunz.tool.wechat.R;
import xjunz.tool.wechat.util.Permissions;
import xjunz.tool.wechat.util.UiUtils;

public class IntroPermissionFragment extends IntroFragment implements View.OnClickListener {
    private Button mBtnPhone, mBtnStorage, mBtnSandbox;
    private Permissions mPermissions;
    private boolean mShouldRefresh;
    private View.OnClickListener mGotoSettings = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mShouldRefresh = true;
            mPermissions.gotoApplicationDetails();
        }
    };

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mPermissions = Permissions.of(this);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_intro_permission, container, false);
        mBtnPhone = view.findViewById(R.id.btn_phone_permission);
        mBtnStorage = view.findViewById(R.id.btn_storage_permission);
        mBtnSandbox = view.findViewById(R.id.btn_access_wx_sandbox);
        ImageButton ibHelp = view.findViewById(R.id.ib_help);
        mBtnPhone.setOnClickListener(this);
        mBtnStorage.setOnClickListener(this);
        mBtnSandbox.setOnClickListener(this);
        ibHelp.setOnClickListener(this);
        return view;
    }

    @Override
    public int getIconResource() {
        return R.drawable.ic_permission_144dp;
    }

    @Override
    public int getTitleResource() {
        return R.string.permission;
    }


    @Override
    public int getStepIndex() {
        return 2;
    }

    @Override
    public void onClick(@NotNull View v) {
        switch (v.getId()) {
            case R.id.btn_access_wx_sandbox:
                mBtnSandbox.setEnabled(false);
                mBtnSandbox.setText(R.string.approved);
                break;
            case R.id.btn_phone_permission:
                mPermissions.requestPhonePermission();
                /*mBtnPhone.setEnabled(false);
                mBtnPhone.setText(R.string.approved);*/
                break;
            case R.id.btn_storage_permission:
                mPermissions.requestStoragePermission();
                break;
            case R.id.ib_help:
                UiUtils.createRationale(getActivity(), R.string.rationale_imei_permission)
                        .setNegativeButton(android.R.string.cancel, null).show();
                break;
        }
    }


    private void refreshPermissionState() {
        if (mPermissions.hasPhonePermission()) {
            mBtnPhone.setText(R.string.permission_granted);
            mBtnPhone.setEnabled(false);
        } else if (mPermissions.isPhonePermissionBanned()) {
            mBtnStorage.setText(R.string.goto_settings);
            mBtnPhone.setOnClickListener(mGotoSettings);
        }
    }

    private boolean hasDone() {
        return !mBtnSandbox.isEnabled() && !mBtnPhone.isEnabled();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        refreshPermissionState();
        if (hasDone()) {
            notifyStepDone();
        }
    }


    @Override
    public void onResume() {
        super.onResume();
        if (mShouldRefresh) {
            refreshPermissionState();
            if (hasDone()) {
                notifyStepDone();
            }
            mShouldRefresh = false;
        }
    }
}

