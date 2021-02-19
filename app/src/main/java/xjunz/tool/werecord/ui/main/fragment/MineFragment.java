/*
 * Copyright (c) 2021 xjunz. 保留所有权利
 */
package xjunz.tool.werecord.ui.main.fragment;

import android.app.Activity;
import android.app.Dialog;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.google.android.gms.oss.licenses.OssLicensesMenuActivity;

import net.sqlcipher.database.SQLiteDatabase;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

import xjunz.tool.werecord.App;
import xjunz.tool.werecord.BuildConfig;
import xjunz.tool.werecord.Constants;
import xjunz.tool.werecord.R;
import xjunz.tool.werecord.Settings;
import xjunz.tool.werecord.data.viewmodel.PageConfig;
import xjunz.tool.werecord.databinding.FragmentMineBinding;
import xjunz.tool.werecord.impl.Environment;
import xjunz.tool.werecord.impl.model.export.DatabaseExporter;
import xjunz.tool.werecord.ui.base.ProgressDialog;
import xjunz.tool.werecord.ui.customview.MasterToast;
import xjunz.tool.werecord.ui.main.fragment.dialog.SwitchAccountDialog;
import xjunz.tool.werecord.util.ActivityUtils;
import xjunz.tool.werecord.util.IoUtils;
import xjunz.tool.werecord.util.RxJavaUtils;
import xjunz.tool.werecord.util.UiUtils;

/**
 * @author xjunz 2021/2/11 12:01
 */
public class MineFragment extends PageFragment {
    private FragmentMineBinding mBinding;
    private Settings mSettings;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSettings = App.config();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return (mBinding = FragmentMineBinding.inflate(inflater, container, false)).getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mBinding.setUser(Environment.getInstance().getCurrentUser());
        mBinding.setSettings(mSettings);
        mBinding.setHost(this);
        mBinding.tvDc.setMovementMethod(LinkMovementMethod.getInstance());
    }

    @Override
    public PageConfig getInitialConfig() {
        PageConfig pageConfig = new PageConfig();
        pageConfig.filterEnabled = false;
        pageConfig.caption = getString(R.string.mine);
        return pageConfig;
    }

    public void toggleEditMode() {
        if (mSettings.isEditModeEnabled()) {
            mSettings.editMode.toggleValue();
            Environment.getInstance().reopenDatabase(SQLiteDatabase.OPEN_READONLY);
        } else {
            AlertDialog dialog = UiUtils.createCaveat(requireContext(), R.string.warning_edit_mode)
                    .setPositiveButton(android.R.string.ok, (dialog1, which) -> {
                        mSettings.editMode.toggleValue();
                        Environment.getInstance().reopenDatabase(SQLiteDatabase.OPEN_READWRITE);
                    })
                    .setNegativeButton(android.R.string.cancel, null).show();
            Button button = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
            button.setEnabled(false);
            new CountDownTimer(3000L, 1000L) {
                final CharSequence originalText = button.getText();

                @Override
                public void onTick(long millisUntilFinished) {
                    button.setText(String.format("%s (%s)", originalText, millisUntilFinished / 1000 + 1));
                }

                @Override
                public void onFinish() {
                    button.setText(originalText);
                    button.setEnabled(true);
                }
            }.start();
            dialog.show();
        }
    }

    public void showSwitchAccountDialog() {
        if (Environment.getInstance().getUserList().size() < 2) {
            MasterToast.shortToast(R.string.no_candidate_account);
            return;
        }
        new SwitchAccountDialog().show(requireFragmentManager(), "switch_account");
    }

    private static final int REQ_CODE_VERIFY = 3;

    public void toggleEnableVerifyDeviceCredential() {
        if (mSettings.isVerifyDeviceCredentialEnabled()) {
            KeyguardManager manager = (KeyguardManager) requireActivity().getSystemService(Context.KEYGUARD_SERVICE);
            if (manager != null) {
                startActivityForResult(manager.createConfirmDeviceCredentialIntent(getString(R.string.verify_owner_title), null)
                        , REQ_CODE_VERIFY);
            }
        } else {
            mSettings.verifyDeviceCredential.toggleValue();
        }
    }

    public void showOssLicenses() {
        startActivity(new Intent(requireContext(), OssLicensesMenuActivity.class));
    }

    private File mDatabaseExportTempFile;

    private void clearExportCacheIfExists() {
        if (mDatabaseExportTempFile != null && mDatabaseExportTempFile.exists()) {
            IoUtils.deleteFileSync(mDatabaseExportTempFile);
        }
    }

    private static final int REQ_CODE_SAVE_DB = 5;

    public void exportDecryptedDatabase() {
        if (!App.config().isEditModeEnabled()) {
            MasterToast.shortToast(R.string.edit_mode_not_enabled);
            return;
        }
        ProgressDialog dialog = ProgressDialog.build(requireContext()).setTitle(R.string.exporting);
        try {
            mDatabaseExportTempFile = File.createTempFile("database", null);
            DatabaseExporter exporter = new DatabaseExporter();
            exporter.getPasswordConfig().setValue(null);
            exporter.exportToAsync(mDatabaseExportTempFile, null)
                    .doOnSubscribe(d -> dialog.setCancelableAction(d::dispose).show())
                    .doFinally(dialog::dismiss)
                    .doOnError(e -> clearExportCacheIfExists())
                    .doOnDispose(this::clearExportCacheIfExists)
                    .subscribe(new RxJavaUtils.CompletableObservableAdapter() {
                        @Override
                        public void onError(@NotNull Throwable e) {
                            UiUtils.createError(requireContext(), e).show();
                        }

                        @Override
                        public void onComplete() {
                            MasterToast.shortToast(R.string.pls_select_export_dir);
                            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                            intent.addCategory(Intent.CATEGORY_OPENABLE);
                            intent.setType("*/*");
                            intent.putExtra(Intent.EXTRA_TITLE, exporter.getExportFileName());
                            startActivityForResult(intent, REQ_CODE_SAVE_DB);
                        }
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void getLatestVersion() {
        MasterToast.longToast(getString(R.string.format_current_app_version, BuildConfig.VERSION_NAME));
        ActivityUtils.viewUri(requireContext(), Constants.URL_APP_DOWNLOAD_PAGE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_CODE_VERIFY) {
            if (resultCode == Activity.RESULT_OK) {
                mSettings.verifyDeviceCredential.toggleValue();
            } else {
                MasterToast.shortToast(R.string.unable_to_verify_device_credential);
            }
        } else if (requestCode == REQ_CODE_SAVE_DB) {
            if (data == null || data.getData() == null) {
                return;
            }
            if (mDatabaseExportTempFile != null && mDatabaseExportTempFile.exists()) {
                Dialog progress = UiUtils.createProgress(requireContext(), R.string.writing);
                progress.show();
                RxJavaUtils.complete(() -> {
                    FileChannel inChannel = new FileInputStream(mDatabaseExportTempFile).getChannel();
                    FileOutputStream outputStream = (FileOutputStream) requireActivity().getContentResolver().openOutputStream(data.getData(), "wa");
                    inChannel.transferTo(0, mDatabaseExportTempFile.length(), outputStream.getChannel());
                    inChannel.close();
                    outputStream.close();
                }).doFinally(() -> {
                    progress.dismiss();
                    clearExportCacheIfExists();
                }).subscribe(new RxJavaUtils.CompletableObservableAdapter() {
                    @Override
                    public void onComplete() {
                        MasterToast.shortToast(R.string.export_successfully);
                    }

                    @Override
                    public void onError(@NotNull Throwable e) {
                        UiUtils.createError(requireContext(), e).show();
                    }
                });
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        clearExportCacheIfExists();
    }
}
