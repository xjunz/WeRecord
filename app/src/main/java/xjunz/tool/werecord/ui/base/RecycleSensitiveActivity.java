/*
 * Copyright (c) 2021 xjunz. 保留所有权利
 */
package xjunz.tool.werecord.ui.base;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.concurrent.atomic.AtomicBoolean;

import xjunz.tool.werecord.impl.Environment;
import xjunz.tool.werecord.ui.outer.InitializationActivity;

/**
 * 一个抽象{@link android.app.Activity}用于处理应用进入后台后意外被系统回收导致{@link Environment#getInstance()}为null的情况。
 * 因为我们的{@link Environment}包含大型的数据库对象且无法被序列化，因此，我们不能通过{@link android.app.Activity#onSaveInstanceState(Bundle)}
 * 保存数据，我们的处理方式是直接重启APP重新初始化我们的{@link Environment}。所有需要使用{@link Environment}的{@link android.app.Activity}都应
 * 继承此类并在{@link RecycleSensitiveActivity#onCreateNormally(Bundle)}里执行正常的{@link android.app.Activity#onCreate(Bundle)}逻辑。
 */
public abstract class RecycleSensitiveActivity extends BaseActivity {
    /**
     * 判断是否已经在重启恢复了，防止多个{@link android.app.Activity}同时调用重启
     */
    private static final AtomicBoolean sIsLaunchingRecovery = new AtomicBoolean(false);
    private static final String TAG_RECYCLED = "RecycleSensitiveActivity.tag.recycled";

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(TAG_RECYCLED, true);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        //如果应用被系统回收了，重启我们的APP，且标志为“恢复”
        if (Environment.getInstance() == null) {
           // LogUtils.debug(savedInstanceState.get(TAG_RECYCLED));
            if (!sIsLaunchingRecovery.getAndSet(true)) {
                Intent launch = getPackageManager().getLaunchIntentForPackage(getPackageName());
                launch.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                launch.putExtra(InitializationActivity.EXTRA_RECOVERY_LAUNCH, true);
                startActivity(launch);
            }
        } else {
            super.onCreate(savedInstanceState);
            onCreateNormally(savedInstanceState);
        }
    }

    protected abstract void onCreateNormally(@Nullable Bundle savedInstanceState);
}
