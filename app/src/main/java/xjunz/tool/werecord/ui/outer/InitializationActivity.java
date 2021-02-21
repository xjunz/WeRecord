/*
 * Copyright (c) 2021 xjunz. 保留所有权利
 */

package xjunz.tool.werecord.ui.outer;

import android.app.KeyguardManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.CompletableObserver;
import io.reactivex.disposables.Disposable;
import xjunz.tool.werecord.App;
import xjunz.tool.werecord.Constants;
import xjunz.tool.werecord.R;
import xjunz.tool.werecord.impl.Environment;
import xjunz.tool.werecord.impl.model.message.util.TemplateManager;
import xjunz.tool.werecord.impl.repo.ContactRepository;
import xjunz.tool.werecord.impl.repo.MessageRepository;
import xjunz.tool.werecord.impl.repo.RepositoryFactory;
import xjunz.tool.werecord.impl.repo.TalkerRepository;
import xjunz.tool.werecord.impl.repo.WxAppRepository;
import xjunz.tool.werecord.ui.customview.MasterToast;
import xjunz.tool.werecord.ui.intro.IntroActivity;
import xjunz.tool.werecord.ui.main.MainActivity;
import xjunz.tool.werecord.util.ActivityUtils;
import xjunz.tool.werecord.util.IoUtils;
import xjunz.tool.werecord.util.LogUtils;
import xjunz.tool.werecord.util.RxJavaUtils;
import xjunz.tool.werecord.util.UiUtils;
import xjunz.tool.werecord.util.Utils;

/**
 * 启动活动，主要执行初始化工作
 */
public class InitializationActivity extends AppCompatActivity implements CompletableObserver {
    public static final String EXTRA_RECOVERY_LAUNCH = "InitializationActivity.extra.RecoveryLaunch";
    private static final AtomicBoolean sNoVerificationLaunch = new AtomicBoolean(false);
    private boolean mIsRecoveryLaunch;

    public static void notifyNoVerificationLaunch() {
        sNoVerificationLaunch.set(true);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!App.getSharedPrefsManager().isAppIntroDone()) {
            Intent i = new Intent(this, IntroActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            return;
        }
        if (mIsRecoveryLaunch = getIntent() != null && getIntent().getBooleanExtra(EXTRA_RECOVERY_LAUNCH, false)) {
            RepositoryFactory.purge();
        }
        boolean needVerify = App.config().isVerifyDeviceCredentialEnabled() && !sNoVerificationLaunch.getAndSet(false);
        if (needVerify) {
            KeyguardManager manager = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
            if (manager == null || !manager.isDeviceSecure()) {
                MasterToast.shortToast(R.string.unable_to_verify_device_credential);
                finish();
            } else {
                ActivityResultLauncher<Void> launcher = ActivityUtils.registerDeviceCredentialConfirmationLauncher(this, R.string.verify_owner_title, R.string.verify_owner_des, this::initIfNecessary, () -> {
                    MasterToast.shortToast(R.string.unable_to_verify_device_credential);
                    finish();
                });
                if (launcher == null) {
                    MasterToast.shortToast(R.string.unable_to_verify_device_credential);
                    finish();
                } else {
                    launcher.launch(null);
                }
            }
        } else {
            initIfNecessary();
        }
    }

    private void initIfNecessary() {
        setContentView(R.layout.activity_splash);
        //初始化环境
        Environment env = Environment.getInstance();
        if (env != null && env.initialized()) {
            onComplete();
        } else {
            Environment.create().init(this);
        }
    }

    @Override
    public void onSubscribe(@NotNull Disposable d) {
        LogUtils.debug("===start init environment===");
        if (mIsRecoveryLaunch) {
            MasterToast.shortToast(R.string.relaunching_from_recycle);
        }
    }

    @Override
    public void onComplete() {
        RxJavaUtils.complete(() -> {
            //查询所有聊天对象
            RepositoryFactory.get(TalkerRepository.class).queryAll();
            //查询所有联系人信息
            RepositoryFactory.get(ContactRepository.class).queryAll();
            //查询所有App信息
            RepositoryFactory.get(WxAppRepository.class).queryAll();
            //初始化类型表
            RepositoryFactory.get(MessageRepository.class).initTypeMap();
            //初始化模板
            TemplateManager.init(this);
        }).subscribe(new RxJavaUtils.CompletableObservableAdapter() {
            @Override
            public void onComplete() {
                Intent i = new Intent(InitializationActivity.this, MainActivity.class);
                //清除当前任务
                i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(i);
            }

            @Override
            public void onError(@NotNull Throwable e) {
                InitializationActivity.this.onError(e);
            }
        });
    }

    @Override
    public void onError(@NotNull Throwable e) {
        App.getSharedPrefsManager().setIsAppIntroDone(false);
        String stacktrace = IoUtils.readStackTraceFromThrowable(e);
        if (Constants.USER_DEBUGGABLE) {
            stacktrace = Environment.getBasicEnvInfo() + "\n\n" + stacktrace;
        }
        String finalStacktrace = stacktrace;
        AlertDialog alert = UiUtils.createDialog(this, R.string.init_failed, stacktrace)
                .setNeutralButton(R.string.copy, null)
                .setPositiveButton(R.string.feedback, null).setNegativeButton(R.string.quit, (a, b) -> finish())
                .setCancelable(false).show();
        alert.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(v -> ActivityUtils.feedbackAutoFallback(InitializationActivity.this, finalStacktrace));
        alert.getButton(DialogInterface.BUTTON_NEUTRAL).setOnClickListener(v -> {
            Utils.copyPlainText("WR-ERROR-LOG", finalStacktrace);
            MasterToast.shortToast(R.string.has_copied_to_clipboard);
        });
    }

    @Override
    public void onBackPressed() {
        //block
    }
}
