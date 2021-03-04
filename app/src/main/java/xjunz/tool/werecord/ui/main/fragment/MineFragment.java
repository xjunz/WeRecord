/*
 * Copyright (c) 2021 xjunz. 保留所有权利
 */
package xjunz.tool.werecord.ui.main.fragment;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.google.android.gms.oss.licenses.OssLicensesMenuActivity;

import net.sqlcipher.database.SQLiteDatabase;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;

import xjunz.tool.werecord.App;
import xjunz.tool.werecord.BuildConfig;
import xjunz.tool.werecord.Constants;
import xjunz.tool.werecord.R;
import xjunz.tool.werecord.Settings;
import xjunz.tool.werecord.databinding.FragmentMineBinding;
import xjunz.tool.werecord.impl.Environment;
import xjunz.tool.werecord.impl.model.export.DatabaseExporter;
import xjunz.tool.werecord.ui.base.ProgressDialog;
import xjunz.tool.werecord.ui.customview.MasterToast;
import xjunz.tool.werecord.ui.export.ExportShowcaseDialog;
import xjunz.tool.werecord.ui.main.fragment.dialog.AboutDialog;
import xjunz.tool.werecord.ui.main.fragment.dialog.SwitchAccountDialog;
import xjunz.tool.werecord.ui.viewmodel.PageConfig;
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
    private ActivityResultLauncher<Void> mDeviceCredentialConfirmationLauncher;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mDeviceCredentialConfirmationLauncher = ActivityUtils.registerDeviceCredentialConfirmationLauncher(this, R.string.verify_owner_title, -1,
                () -> mSettings.verifyDeviceCredential.toggleValue(), () -> MasterToast.shortToast(R.string.unable_to_verify_device_credential));
    }

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
    }

    @Override
    public PageConfig getInitialConfig() {
        PageConfig pageConfig = new PageConfig();
        pageConfig.filterEnabled = false;
        pageConfig.caption = App.getStringOf(R.string.mine);
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
        new SwitchAccountDialog().show(getParentFragmentManager(), "switch_account");
    }

    public void toggleEnableVerifyDeviceCredential() {
        if (mSettings.isVerifyDeviceCredentialEnabled()) {
            if (mDeviceCredentialConfirmationLauncher != null) {
                mDeviceCredentialConfirmationLauncher.launch(null);
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
                            UiUtils.showError(requireContext(), e);
                        }

                        @Override
                        public void onComplete() {
                            new ExportShowcaseDialog().setFile(mDatabaseExportTempFile)
                                    .setFilename(exporter.getExportFileName())
                                    .show(getParentFragmentManager(), "decrypted_db_export_showcase");
                        }
                    });
        } catch (IOException e) {
            MasterToast.shortToast(e.getMessage());
        }
    }

    public void getLatestVersion() {
        MasterToast.longToast(getString(R.string.format_current_app_version, BuildConfig.VERSION_NAME));
        ActivityUtils.safeViewUri(requireContext(), Constants.URL_APP_DOWNLOAD_PAGE);
    }

    public void showAppInfo() {
        new AboutDialog().show(getParentFragmentManager(), "about");
    }

    public void gotoFeedback(View view) {
        ActivityUtils.feedbackAutoFallback(requireContext(), Environment.getBasicEnvInfo());
    }
}
