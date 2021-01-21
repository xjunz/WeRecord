/*
 * Copyright (c) 2021 xjunz. 保留所有权利
 */
package xjunz.tool.wechat.util;

import android.content.ContentValues;

import net.sqlcipher.Cursor;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public class DbUtils {
    @NotNull
    @Contract("_ -> new")
    public static ContentValues buildValuesFromCursor(@NotNull Cursor cursor) {
        ContentValues values = new ContentValues();
        for (int i = 0; i < cursor.getColumnCount(); i++) {
            switch (cursor.getType(i)) {
                case Cursor.FIELD_TYPE_STRING:
                    values.put(cursor.getColumnName(i), cursor.getString(i));
                    break;
                case Cursor.FIELD_TYPE_INTEGER:
                    values.put(cursor.getColumnName(i), cursor.getLong(i));
                    break;
                case Cursor.FIELD_TYPE_NULL:
                    values.putNull(cursor.getColumnName(i));
                    break;
                case Cursor.FIELD_TYPE_BLOB:
                    values.put(cursor.getColumnName(i), cursor.getBlob(i));
                    break;
                case Cursor.FIELD_TYPE_FLOAT:
                    values.put(cursor.getColumnName(i), cursor.getDouble(i));
                    break;
            }
        }
        return values;
    }
}
