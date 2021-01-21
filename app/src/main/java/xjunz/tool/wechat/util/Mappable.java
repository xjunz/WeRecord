/*
 * Copyright (c) 2021 xjunz. 保留所有权利
 */
package xjunz.tool.wechat.util;

import androidx.annotation.Nullable;

public interface Mappable<O, M> {
    @Nullable
    M map(O origin);
}
