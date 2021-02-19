/*
 * Copyright (c) 2021 xjunz. 保留所有权利
 */
package xjunz.tool.werecord.impl.util;

import org.jetbrains.annotations.NotNull;

public class ContentType {
    public static final int INTEGER = 0;
    public static final int LONG = 1;
    public static final int BLOB = 2;
    public static final int STRING = 3;
    private static final String RAW_INTEGER = "INTEGER";
    private static final String RAW_LONG = "LONG";
    private static final String RAW_INT = "INT";
    private static final String RAW_STRING = "TEXT";
    private static final String RAW_BLOB = "BLOB";

    public static int getTypeFromRaw(@NotNull String raw) {
        switch (raw.toUpperCase()) {
            case RAW_STRING:
                return STRING;
            case RAW_INTEGER:
            case RAW_LONG:
                return LONG;
            case RAW_INT:
                return INTEGER;
            case RAW_BLOB:
                return BLOB;
        }
        throw new IllegalArgumentException("Unknown raw type: " + raw);
    }
}
