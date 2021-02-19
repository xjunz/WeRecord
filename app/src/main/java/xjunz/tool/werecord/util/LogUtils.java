/*
 * Copyright (c) 2021 xjunz. 保留所有权利
 */
package xjunz.tool.werecord.util;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.concurrent.Callable;

import xjunz.tool.werecord.BuildConfig;

public class LogUtils {
    private static final String TAG = "we-record";
    private static final String TAG_DEBUG = "wr-debug";
    private static final String TAG_ERROR = "wr-error";

    public static void log(@NonNull String tag, @Nullable Object object) {
        Log.i(tag, Objects.toString(object, "[NULL]"));
    }

    public static void debug(@Nullable Object object) {
        if (BuildConfig.DEBUG) {
            log(TAG_DEBUG, object);
        }
    }

    public static void error(@Nullable Object object) {
        Log.e(TAG_ERROR, Objects.toString(object, "[NULL]"));
    }

    public static long calculateExecutionTime(@NotNull Runnable runnable, String taskName) {
        long start = System.currentTimeMillis();
        runnable.run();
        long consumed = System.currentTimeMillis() - start;
        debug(taskName + "用时:" + consumed);
        return consumed;
    }

    @NotNull
    public static <T> T logTroughCalculateExecutionTime(@NotNull Callable<T> callable, String taskName) {
        long start = System.currentTimeMillis();
        T t = null;
        try {
            t = callable.call();
        } catch (Exception e) {
            e.printStackTrace();
        }
        long consumed = System.currentTimeMillis() - start;
        debug(taskName + "用时:" + consumed);
        return t;
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
