/*
 * Copyright (c) 2020 xjunz. 保留所有权利
 */
package xjunz.tool.wechat.impl.model.message;

import org.jetbrains.annotations.NotNull;

/**
 * 消息修改的抽象类
 *
 * @author xjunz 2020/11/5 22:52
 */
public class Edition {
    /**
     * 消息删除指令标志
     */
    public static final int FLAG_DELETION = 2;
    /**
     * 消息替换指令标志
     */
    public static final int FLAG_REPLACEMENT = 1;
    /**
     * 消息插入指令标志
     */
    public static final int FLAG_INSERTION = 3;

    /**
     * 消息编辑的指令
     */
    private final int flag;
    /**
     * 原消息
     */
    private final Message origin;
    /**
     * 替换后的消息
     */
    private final Message replacement;

    public int getFlag() {
        return flag;
    }

    public Message getOrigin() {
        return origin;
    }

    public Message getReplacement() {
        return replacement;
    }

    private Edition(int flag, Message origin, Message replacement) {
        this.flag = flag;
        this.replacement = replacement;
        this.origin = origin;
    }

    @NotNull
    public static Edition delete(Message message) {
        return new Edition(FLAG_DELETION, message, message);
    }

    @NotNull
    public static Edition replace(Message origin, Message replacement) {
        return new Edition(FLAG_REPLACEMENT, origin, replacement);
    }

    @NotNull
    public static Edition insert(Message message) {
        return new Edition(FLAG_INSERTION, message, message);
    }

    /**
     * @return 当前消息编辑指令的恢复指令
     */
    public Edition getReverseEdition() {
        switch (flag) {
            case FLAG_DELETION:
                return Edition.insert(origin);
            case FLAG_INSERTION:
                return Edition.delete(replacement);
            case FLAG_REPLACEMENT:
                return Edition.replace(replacement, origin);
        }
        throw new RuntimeException("unexpected instruction: " + flag);
    }
}
