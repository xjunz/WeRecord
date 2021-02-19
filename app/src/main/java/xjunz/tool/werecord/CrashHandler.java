/*
 * Copyright (c) 2021 xjunz. 保留所有权利
 */
package xjunz.tool.werecord;

import android.os.Looper;

import androidx.annotation.NonNull;

import xjunz.tool.werecord.ui.customview.MasterToast;
import xjunz.tool.werecord.util.IoUtils;
import xjunz.tool.werecord.util.Utils;

public class CrashHandler implements Thread.UncaughtExceptionHandler {
    @Override
    public void uncaughtException(@NonNull Thread t, @NonNull Throwable e) {
        //这个错误不需要处理，它来自我们的RecycleSensitiveActivity
        if (!e.getClass().getSimpleName().equals("SuperNotCalledException")) {
            //TODO:更优雅的处理方式
            String stacktrace = IoUtils.readStackTraceFromThrowable(e);
            Utils.copyPlainText("WeRecord Error Msg", stacktrace);
            new Thread(() -> {
                Looper.prepare();
                MasterToast.shortToast("很抱歉，应用崩溃了，正在收集错误信息...");
                Looper.loop();
            }).start();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException interruptedException) {
                interruptedException.printStackTrace();
            }
        }
        System.exit(1);
    }
}
