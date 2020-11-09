/*
 * Copyright (c) 2020 xjunz. 保留所有权利
 */
package xjunz.tool.wechat.util;

public class CaughtException extends Exception {

    public CaughtException(String message) {

    }

    @Override
    public void printStackTrace() {
        super.printStackTrace();
    }
}
