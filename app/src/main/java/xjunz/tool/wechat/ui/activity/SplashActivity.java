package xjunz.tool.wechat.ui.activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;

import com.github.promeg.pinyinhelper.Pinyin;

import io.reactivex.Completable;
import io.reactivex.CompletableEmitter;
import io.reactivex.CompletableObserver;
import io.reactivex.CompletableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import xjunz.tool.wechat.App;
import xjunz.tool.wechat.R;
import xjunz.tool.wechat.impl.Environment;
import xjunz.tool.wechat.impl.repo.ContactRepository;
import xjunz.tool.wechat.impl.repo.TalkerRepository;
import xjunz.tool.wechat.ui.activity.intro.IntroActivity;
import xjunz.tool.wechat.ui.activity.main.MainActivity;
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
        //初始化拼音库
        Pinyin.init(null);
        //初始化环境
        Environment env = getEnvironment();
        if (!env.initialized()) {
            env.init(this);
        } else {
            onComplete();
        }
    }

    @Override
    public void onSubscribe(Disposable d) {

    }

    @Override
    public void onComplete() {
        mQueryDisposable = Completable.create(new CompletableOnSubscribe() {
            @Override
            public void subscribe(CompletableEmitter emitter) throws Exception {
                //查询所有聊天记录
                TalkerRepository.getInstance().queryAll();
                //查询所有联系人信息
                ContactRepository.getInstance().queryAll();
                emitter.onComplete();
            }
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(new Action() {
            @Override
            public void run() {
                Intent i = new Intent(SplashActivity.this, MainActivity.class);
                //清除当前任务
                i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(i);
            }
        }, new Consumer<Throwable>() {
            @Override
            public void accept(Throwable throwable) throws Exception {
                throwable.printStackTrace();
            }
        });

    }

    @Override
    public void onError(Throwable e) {
        App.getSharedPrefsManager().setIsAppIntroDone(false);
        UiUtils.createDialog(this, R.string.init_failed, R.string.msg_init_failed).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                recreate();
            }
        }).setNegativeButton(R.string.exit, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        }).setCancelable(false).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mQueryDisposable != null) {
            mQueryDisposable.dispose();
        }
    }
}
