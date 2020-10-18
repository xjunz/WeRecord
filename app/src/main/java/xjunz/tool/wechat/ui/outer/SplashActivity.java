/*
 * Copyright (c) 2020 xjunz. 保留所有权利
 */

package xjunz.tool.wechat.ui.outer;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import io.reactivex.Completable;
import io.reactivex.CompletableObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import xjunz.tool.wechat.App;
import xjunz.tool.wechat.R;
import xjunz.tool.wechat.impl.Environment;
import xjunz.tool.wechat.impl.repo.ContactRepository;
import xjunz.tool.wechat.impl.repo.RepositoryFactory;
import xjunz.tool.wechat.impl.repo.TalkerRepository;
import xjunz.tool.wechat.ui.BaseActivity;
import xjunz.tool.wechat.ui.intro.IntroActivity;
import xjunz.tool.wechat.ui.main.MainActivity;
import xjunz.tool.wechat.util.UiUtils;

/**
 * 启动页活动，主要执行初始化工作
 */
public class SplashActivity extends BaseActivity implements CompletableObserver {

    private Disposable mQueryDisposable;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!App.getSharedPrefsManager().isAppIntroDone()) {
            Intent i = new Intent(this, IntroActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            return;
        }
        setContentView(R.layout.activity_splash);
        //初始化环境
        Environment env = getEnvironment();
        if (!env.initialized()) {
            env.init(this);
        } else {
            onComplete();
        }
    }

    @Override
    public void onSubscribe(@NotNull Disposable d) {

    }

    @Override
    public void onComplete() {
        mQueryDisposable = Completable.create(emitter -> {
            //查询所有聊天对象
            RepositoryFactory.get(TalkerRepository.class).queryAll();
            //查询所有联系人信息
            RepositoryFactory.get(ContactRepository.class).queryAll();
            emitter.onComplete();
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(() -> {
            Intent i = new Intent(SplashActivity.this, MainActivity.class);
            //清除当前任务
            i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        }, Throwable::printStackTrace);
    }

    @Override
    public void onError(@NotNull Throwable e) {
        App.getSharedPrefsManager().setIsAppIntroDone(false);
        UiUtils.createDialog(this, R.string.init_failed, R.string.msg_init_failed).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                recreate();
            }
        }).setNegativeButton(R.string.exit, (dialog, which) -> finish()).setCancelable(false).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mQueryDisposable != null && !mQueryDisposable.isDisposed()) {
            mQueryDisposable.dispose();
        }
    }

    @Override
    public void onBackPressed() {
        //block
    }
}
