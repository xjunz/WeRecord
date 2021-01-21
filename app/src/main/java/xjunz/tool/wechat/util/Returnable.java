/*
 * Copyright (c) 2021 xjunz. 保留所有权利
 */
package xjunz.tool.wechat.util;

import java.util.concurrent.Callable;

/**
 * 不抛出异常的{@link Callable}
 *
 * @param <R> {@code get}方法返回的结果类型
 */
public interface Returnable<R> {
    R get();
}
