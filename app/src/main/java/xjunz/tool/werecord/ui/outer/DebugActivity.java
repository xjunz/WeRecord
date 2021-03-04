/*
 * Copyright (c) 2021 xjunz. 保留所有权利
 */

package xjunz.tool.werecord.ui.outer;

import android.app.Dialog;
import android.os.Bundle;
import android.os.Process;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import java.io.File;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import xjunz.tool.werecord.R;
import xjunz.tool.werecord.impl.Environment;
import xjunz.tool.werecord.impl.model.account.User;
import xjunz.tool.werecord.impl.model.message.util.TemplateManager;
import xjunz.tool.werecord.ui.base.BaseActivity;
import xjunz.tool.werecord.ui.customview.MasterToast;
import xjunz.tool.werecord.util.IoUtils;
import xjunz.tool.werecord.util.RxJavaUtils;
import xjunz.tool.werecord.util.ShellUtils;
import xjunz.tool.werecord.util.UiUtils;

public class DebugActivity extends BaseActivity {
    private EditText mEtOutput;
    private Environment mEnv;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debug);
        mEnv = Environment.getInstance();
        mEtOutput = findViewById(R.id.et_output);

    }


    public void parseEnvInfo(View view) {
      /*  if (mEtInput.getText() != null)
            try {
                String input = mEtInput.getText().toString();
                if (input.contains("Serial")) {
                    input = Objects.requireNonNull(Utils.extractFirst(input, "Serial:(.+)$"), "error extract serial").trim();
                    UiUtils.toast(String.valueOf(input.length()));
                }
                mEtOutput.setText(Html.fromHtml(Objects.requireNonNull(Environment.deserialize(input), "error parse environment")));
            } catch (Exception e) {
                mEtOutput.setText(String.format("%s: %s", e.getClass().getName(), e.getMessage()));
            }*/
    }

    public void deleteBackupTable(View view) {
        RxJavaUtils.complete(() -> mEnv.modifyDatabase().dropMessageBackupTables()).subscribe(new RxJavaUtils.CompletableObservableAdapter() {
            @Override
            public void onComplete() {
                super.onComplete();
                UiUtils.toast("已删除");
            }

            @Override
            public void onError(@NotNull Throwable e) {
                super.onError(e);
                mEtOutput.setText(IoUtils.readStackTraceFromThrowable(e));
            }
        });
    }

    public void showEnvInfo(View view) {
        mEtOutput.setText(Environment.getBasicEnvInfo());
    }

    public void exportDatabase(View view) {
        Dialog dialog = UiUtils.createProgress(this, R.string.please_wait);
        dialog.show();
        if (mEnv.initialized() && mEnv.getCurrentUser() != null) {
            String src = mEnv.getCurrentUser().workerDatabaseFilePath;
            String tar = android.os.Environment.getExternalStorageDirectory() + File.separator + mEnv.getCurrentUser().databasePassword + ".db";
            if (new File(src).exists()) {
                RxJavaUtils.complete(() -> {
                    ShellUtils.cp(src, tar);
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
                        mEtOutput.setText(IoUtils.readStackTraceFromThrowable(e));
                    }
                });
            }
        }
    }

    public void simulateSysRecycle(View view) {
        MasterToast.shortToast("五秒后回收App进程");
        RxJavaUtils.complete(() -> {
            Thread.sleep(5000);
            Log.i("werecord", "simulate system recycle pid_" + Process.myPid());
            ShellUtils.sudo("kill -9 " + Process.myPid());
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .subscribe(new RxJavaUtils.CompletableObservableAdapter() {
                    @Override
                    public void onError(@NotNull Throwable e) {
                        super.onError(e);
                        mEtOutput.setText(IoUtils.readStackTraceFromThrowable(e));
                    }
                });
    }

    public void exportTemplateDb(View view) {
        Dialog dialog = UiUtils.createProgress(this, R.string.please_wait);
        if (mEnv.initialized()) {
            mEnv.getCurrentUser();
            File src = getDatabasePath(TemplateManager.TEMPLATE_DB_NAME);
            String tar = android.os.Environment.getExternalStorageDirectory() + File.separator + TemplateManager.TEMPLATE_DB_PWD + ".db";
            if (src.exists()) {
                dialog.show();
                RxJavaUtils.complete(() -> {
                    ShellUtils.cp(src.getPath(), tar);
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
                        mEtOutput.setText(IoUtils.readStackTraceFromThrowable(e));
                    }
                });
            }
        }
    }

    public void deleteTemplateDb(View view) {
        RxJavaUtils.complete(() -> {
            //noinspection ResultOfMethodCallIgnored
            getDatabasePath(TemplateManager.TEMPLATE_DB_NAME).delete();
        }).subscribe(new RxJavaUtils.CompletableObservableAdapter() {
            @Override
            public void onComplete() {
                super.onComplete();
                UiUtils.toast("已删除");
            }

            @Override
            public void onError(@NotNull Throwable e) {
                super.onError(e);
                mEtOutput.setText(IoUtils.readStackTraceFromThrowable(e));
            }
        });
    }

    public void restoreMsgDatabaseBackup(View view) {
        User currentUser = getEnvironment().getCurrentUser();
        Dialog progress = UiUtils.createProgress(this, R.string.please_wait);
        if (currentUser.backupDatabaseFilePath == null) {
            MasterToast.shortToast("备份不存在");
            return;
        }
        File backup = new File(currentUser.backupDatabaseFilePath);
        if (!backup.exists()) {
            MasterToast.shortToast("备份不存在");
            return;
        }
        progress.show();
        RxJavaUtils.complete(() -> {
            String databaseOriginalPath = currentUser.originalDatabaseFilePath;
            //先强行停止微信，否则可能导致数据库损坏
            ShellUtils.forceStop("com.tencent.mm");
            ShellUtils.cp(currentUser.backupDatabaseFilePath, currentUser.originalDatabaseFilePath);
            //删除原数据库运行时文件
            //如不删除，微信会检测到数据库损坏，并执行数据库修复，修复数据可能导致数据丢失
            ShellUtils.rmIfExists(databaseOriginalPath + "-shm");
            ShellUtils.rmIfExists(databaseOriginalPath + "-wal");
            ShellUtils.rmIfExists(databaseOriginalPath + ".ini");
            ShellUtils.rmIfExists(databaseOriginalPath + ".sm");
        }).subscribe(new RxJavaUtils.CompletableObservableAdapter() {
            @Override
            public void onComplete() {
                super.onComplete();
                progress.dismiss();
                UiUtils.createLaunch(DebugActivity.this).show();
            }

            @Override
            public void onError(@NotNull Throwable e) {
                super.onError(e);
                progress.dismiss();
                mEtOutput.setText(IoUtils.readStackTraceFromThrowable(e));
            }
        });
    }

    public void backupMsgDatabase(View view) {
        Dialog progress = UiUtils.createProgress(this, R.string.please_wait);
        progress.show();
        RxJavaUtils.complete(() -> getEnvironment().backupOriginDatabaseOf(getEnvironment().getCurrentUser()))
                .subscribe(new RxJavaUtils.CompletableObservableAdapter() {
                    @Override
                    public void onComplete() {
                        super.onComplete();
                        progress.dismiss();
                        MasterToast.shortToast("备份完成");
                    }

                    @Override
                    public void onError(@NotNull Throwable e) {
                        super.onError(e);
                        progress.dismiss();
                        mEtOutput.setText(IoUtils.readStackTraceFromThrowable(e));
                    }
                });
    }

    public void caughtException(View view) {
        try {
            throw new RuntimeException("An exception thrown in codes explicitly.");
        } catch (Exception e) {
            UiUtils.showError(this, e);
        }
    }

    public void uncaughtException(View view) {
        throw new RuntimeException("An exception thrown in codes explicitly.");
    }
}
