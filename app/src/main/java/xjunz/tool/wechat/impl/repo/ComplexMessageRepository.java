/*
 * Copyright (c) 2020 xjunz. 保留所有权利
 */
package xjunz.tool.wechat.impl.repo;

import androidx.annotation.CheckResult;

import net.sqlcipher.Cursor;

import org.jetbrains.annotations.NotNull;

import xjunz.tool.wechat.impl.model.message.ComplexMessage;

public class ComplexMessageRepository extends LifecyclePerceptiveRepository {
    private static final String TABLE_APP_MESSAGE = "AppMessage";

    ComplexMessageRepository() {
    }

    @CheckResult
    public boolean fulfillComplexMessage(@NotNull ComplexMessage message) {
        boolean found = false;
        Cursor cursor = getDatabase().rawQuery("select title,description,source,type from " + TABLE_APP_MESSAGE + " where msgId=" + message.getMsgId(), null);
        if (cursor.moveToNext() && !cursor.isClosed()) {
            message.setTitle(cursor.getString(0));
            message.setDescription(cursor.getString(1));
            message.setSource(cursor.getString(2));
            message.setRawSubtype(cursor.getInt(3));
            found = true;
        }
        cursor.close();
        return found;
    }
}
