/*
 * Copyright (c) 2020 xjunz. 保留所有权利
 */

package xjunz.tool.wechat.impl.model.account;

import android.graphics.Bitmap;

import androidx.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

import xjunz.tool.wechat.impl.repo.AvatarRepository;


/**
 * 微信账号（个人用户、群聊、公众号）的抽象类
 */
public abstract class Account implements Serializable {
    /**
     * 微信的昵称
     */
    public String nickname;
    /**
     * 微信号，是微信账号或有的唯一标识
     */
    public String alias;
    /**
     * 微信ID，通常的形式是"wxid_xxxxx"，是微信账号必有的唯一标识
     */
    public String id;
    /**
     * 头像文件是否已经尝试解码过
     *
     * @see Account#getAvatar()
     */
    private boolean mHasTryDecodeAvatar;
    /**
     * 是否存在本地头像
     */
    private boolean mHasLocalAvatar;

    /**
     * @return 当前账号是否为公众号
     */
    public boolean isGZH() {
        return id.startsWith("gh_");
    }

    /**
     * 判断当前账号是否为个人用户号，此方法是充分不必要判断，
     * 充要判断请使用{@link Account#isUser()}
     *
     * @return 是否为个人用户号
     */
    protected boolean isUserUnnecessary() {
        return id.startsWith("wxid_");
    }

    /**
     * @return 当前账号是否为个人用户号
     */
    public boolean isUser() {
        return !isGZH() && !isGroup();
    }

    /**
     * @return 当前账号是否为群聊号
     */
    public boolean isGroup() {
        return id.endsWith("@chatroom");
    }


    protected boolean empty(String str) {
        return str == null || str.length() == 0;
    }


    public String getName() {
        return empty(nickname) ? (empty(alias) ? (empty(id) ? "<unknown>" : id) : alias) : nickname;
    }


    /**
     * 获取当前账号的头像
     *
     * @return 当前账号的头像
     */
    @Nullable
    public Bitmap getAvatar() {
        if (!mHasTryDecodeAvatar) {
            Bitmap bitmap = AvatarRepository.getInstance().getAvatar(id);
            mHasTryDecodeAvatar = true;
            mHasLocalAvatar = bitmap != null;
            return bitmap;
        } else {
            return mHasLocalAvatar ? AvatarRepository.getInstance().getAvatar(id) : null;
        }
    }


    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj instanceof Account) {
            return ((Account) obj).id.equals(id);
        }
        return super.equals(obj);
    }

    @NotNull
    @Override
    public String toString() {
        return "Account{" +
                "nickname='" + nickname + '\'' +
                ", alias='" + alias + '\'' +
                ", id='" + id + '\'' +
                '}';
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

}
