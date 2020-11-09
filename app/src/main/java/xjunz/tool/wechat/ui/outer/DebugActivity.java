/*
 * Copyright (c) 2020 xjunz. 保留所有权利
 */

package xjunz.tool.wechat.ui.outer;

import android.app.Dialog;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.Keep;
import androidx.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Objects;

import xjunz.tool.wechat.R;
import xjunz.tool.wechat.impl.Environment;
import xjunz.tool.wechat.ui.BaseActivity;
import xjunz.tool.wechat.util.IOUtils;
import xjunz.tool.wechat.util.RxJavaUtils;
import xjunz.tool.wechat.util.ShellUtils;
import xjunz.tool.wechat.util.UiUtils;
import xjunz.tool.wechat.util.UniUtils;

public class DebugActivity extends BaseActivity {
    @Keep
    public static final String EXTRA_ENV_SERIAL = "xjunz.extra.EnvSerial";
    private EditText mEtInput, mEtOutput;
    private Environment mEnv;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debug);
        mEnv = Environment.getInstance();
        String serial = getIntent().getStringExtra(EXTRA_ENV_SERIAL);
        mEtInput = findViewById(R.id.et_serial);
        if (serial != null) {
            mEtInput.setText(serial);
        }
        mEtOutput = findViewById(R.id.et_output);

    }


    public void parseEnvInfo(View view) {
        if (mEtInput.getText() != null)
            try {
                String input = mEtInput.getText().toString();
                if (input.contains("Serial")) {
                    input = Objects.requireNonNull(UniUtils.extractFirst(input, "Serial:(.+)$"), "error extract serial").trim();
                    UiUtils.toast(String.valueOf(input.length()));
                }
                mEtOutput.setText(Html.fromHtml(Objects.requireNonNull(Environment.deserialize(input), "error parse environment")));
            } catch (Exception e) {
                mEtOutput.setText(String.format("%s: %s", e.getClass().getName(), e.getMessage()));
            }
    }

    public void deleteBackupTable(View view) {
        RxJavaUtils.complete(() -> mEnv.modifyDatabase().dropBackupTable()).subscribe(new RxJavaUtils.CompletableObservableAdapter() {
            @Override
            public void onComplete() {
                super.onComplete();
                UiUtils.toast("已删除");
            }

            @Override
            public void onError(@NotNull Throwable e) {
                super.onError(e);
                mEtOutput.setText(IOUtils.readExceptionStackTrace(e));
            }
        });
    }

    public void showEnvInfo(View view) {
        mEtOutput.setText(Html.fromHtml(mEnv.toString()));
    }

    public void exportDatabase(View view) {
        Dialog dialog = UiUtils.createProgressDialog(this, R.string.please_wait);
        dialog.show();
        if (mEnv.initialized() && mEnv.getCurrentUser() != null) {
            String src = mEnv.getCurrentUser().backupDatabaseFilePath;
            String tar = android.os.Environment.getExternalStorageDirectory() + File.separator + mEnv.getCurrentUser().databasePragmaKey + ".db";
            if (new File(src).exists()) {
                RxJavaUtils.complete(() -> {
                    ShellUtils.cp(src, tar, "exportDatabase");
                }).subscribe(new RxJavaUtils.CompletableObservableAdapter() {
                    @Override
                    public void onComplete() {
                        super.onComplete();
                        dialog.dismiss();
                        UiUtils.toast("已导出到" + tar);
                    }

                    @Override
                    public void onError(@NotNull Throwable e) {
                        super.onError(e);
                        dialog.dismiss();
                        UiUtils.createError(DebugActivity.this, IOUtils.readExceptionStackTrace(e)).show();
                    }
                });
            }
        }
    }
}
