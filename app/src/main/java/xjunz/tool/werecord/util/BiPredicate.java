/*
 * Copyright (c) 2021 xjunz. 保留所有权利
 */
package xjunz.tool.werecord.util;

import android.annotation.SuppressLint;


/**
 * @author xjunz 2021/1/29 13:15
 */
public interface BiPredicate<T, V> {
    @SuppressLint("UnknownNullness")
    boolean test(T t, V v);
}
