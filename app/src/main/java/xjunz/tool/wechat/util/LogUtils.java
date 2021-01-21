/*
 * Copyright (c) 2021 xjunz. 保留所有权利
 */
package xjunz.tool.wechat.util;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.concurrent.Callable;

import xjunz.tool.wechat.BuildConfig;

public class LogUtils {
    private static final String TAG = "xjunz";
    private static final String TAG_DEBUG = "xjunz-debug";

    public static void log(@NonNull String tag, @Nullable Object object) {
        Log.i(tag, Objects.toString(object, "[NULL]"));
    }

    public static void debug(@Nullable Object object) {
        if (BuildConfig.DEBUG) {
            log(TAG_DEBUG, object);
        }
    }

    public static void info(@Nullable Object object) {
        log(TAG, object);
    }

    public static <T> T logThrough(@NotNull Callable<T> callable) {
        T t = null;
        try {
            t = callable.call();
        } catch (Exception e) {
            e.printStackTrace();
        }
        debug(t);
        return t;
    }
}
