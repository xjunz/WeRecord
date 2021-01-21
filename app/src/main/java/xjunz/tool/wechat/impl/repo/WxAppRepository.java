/*
 * Copyright (c) 2021 xjunz. 保留所有权利
 */
package xjunz.tool.wechat.impl.repo;

import android.util.ArrayMap;

import net.sqlcipher.Cursor;

import org.jetbrains.annotations.Nullable;

/**
 * @author xjunz 2021/1/10 21:52
 */
public class WxAppRepository extends LifecyclePerceptiveRepository {
    private final ArrayMap<String, String> mAll = new ArrayMap<>();
    public String TABLE_APP_INFO = "AppInfo";

    public void queryAll() {
        try (Cursor cursor = getDatabase().rawQuery(String.format("select appId,appName from %s", TABLE_APP_INFO), null)) {
            while (cursor.moveToNext()) {
                mAll.put(cursor.getString(0), cursor.getString(1));
            }
        }
    }

    @Nullable
    public String getNameOf(String appId) {
        return mAll.get(appId);
    }

    public ArrayMap<String, String> getAll() {
        return mAll;
    }
}
