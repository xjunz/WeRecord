/*
 * Copyright (c) 2021 xjunz. 保留所有权利
 */
package xjunz.tool.werecord.util;

import android.content.ContentValues;
import android.util.Pair;

import androidx.annotation.NonNull;

import net.sqlcipher.Cursor;
import net.sqlcipher.database.SQLiteDatabase;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import io.reactivex.Flowable;
import io.reactivex.schedulers.Schedulers;
import xjunz.tool.werecord.impl.Environment;

public class DbUtils {
    @NotNull
    @Contract("_ -> new")
    public static ContentValues buildValuesFromCursor(@NotNull Cursor cursor) {
        ContentValues values = new ContentValues();
        for (int i = 0; i < cursor.getColumnCount(); i++) {
            if (cursor.getType(i) == Cursor.FIELD_TYPE_BLOB) {
                values.put(cursor.getColumnName(i), cursor.getBlob(i));
            } else {
                values.put(cursor.getColumnName(i), cursor.getString(i));
            }
        }
        return values;
    }

    @NotNull
    public static ContentValues buildValuesFromCursorReuse(@NotNull Cursor cursor, @NonNull ContentValues values) {
        values.clear();
        for (int i = 0; i < cursor.getColumnCount(); i++) {
            if (cursor.getType(i) == Cursor.FIELD_TYPE_BLOB) {
                values.put(cursor.getColumnName(i), cursor.getBlob(i));
            } else {
                values.put(cursor.getColumnName(i), cursor.getString(i));
            }
        }
        return values;
    }

    @Nullable
    public static String getTableCreateSql(String tableName) {
        SQLiteDatabase db = Environment.getInstance().getWorkerDatabase();
        try (Cursor cursor = db.rawQuery(String.format("select sql from sqlite_master where name='%s'", tableName), null)) {
            if (cursor.moveToNext()) {
                return cursor.getString(0);
            }
            return null;
        }
    }

    public static void putObject(ContentValues values, String key, Object value) {
        if (value == null) {
            values.putNull(key);
        } else if (value instanceof String) {
            values.put(key, (String) value);
        } else if (value instanceof Integer || value instanceof Long) {
            values.put(key, ((Number) value).longValue());
        } else if (value instanceof Boolean) {
            values.put(key, (Boolean) value);
        } else if (value instanceof byte[]) {
            values.put(key, (byte[]) value);
        } else {
            throw new IllegalArgumentException("Unsupported type " + value.getClass());
        }
    }

    public static void rawQuery(String sql, Passable<Cursor> action) {
        try (Cursor cursor = Environment.getInstance().getWorkerDatabase().rawQuery(sql, null)) {
            while (cursor.moveToNext()) {
                action.pass(cursor);
            }
        }
    }

    public static void autoParseQueryParallel(String querySql, Passable<ContentValues> action, Passable<Throwable> onError, Runnable onFinish) {
        SQLiteDatabase db = Environment.getInstance().getWorkerDatabase();
        try (Cursor cursor = db.rawQuery(querySql, null)) {
            if (cursor.moveToNext()) {
                //总数
                long size = cursor.getCount();
                LogUtils.debug("总数量为：" + size);
                int parallelism = Runtime.getRuntime().availableProcessors();
                long unit = size / parallelism;
                long start = System.currentTimeMillis();
                Flowable.range(0, parallelism).map(ordinal -> {
                    //返回每个线程需要加载的消息起点和消息数
                    //最后一个分组需要多加载平均分后剩余的消息数
                    long offset = unit * ordinal;
                    long count = unit;
                    if (ordinal == parallelism - 1) {
                        count = size - ordinal * unit;
                    }
                    return new Pair<>(offset, count);
                })
                        .parallel()
                        .runOn(Schedulers.io())
                        .doOnNext(pair -> {
                            try (Cursor innerCursor = db.rawQuery(querySql + " limit " + pair.second + " offset " + pair.first, null)) {
                                LogUtils.debug(Thread.currentThread() + "开始于:" + System.currentTimeMillis());
                                LogUtils.debug(Thread.currentThread() + "数据为:from " + pair.first + " to " + (pair.second + pair.first - 1));
                                while (innerCursor.moveToNext()) {
                                    action.pass(buildValuesFromCursor(innerCursor));
                                }
                                LogUtils.debug(Thread.currentThread() + "结束于:" + System.currentTimeMillis());
                            }
                        }).sequential().doOnError(passed -> {
                    if (onError != null) {
                        onError.pass(passed);
                    }
                }).doOnComplete(() -> {
                    if (onFinish != null) {
                        onFinish.run();
                    }
                    LogUtils.debug("OK:" + (System.currentTimeMillis() - start));
                }).subscribe();
            }
        }
    }

    public static void autoParseQuery(String sql, Passable<ContentValues> action) {
        rawQuery(sql, cursor -> action.pass(buildValuesFromCursor(cursor)));
    }
}
