/*
 * Copyright (c) 2021 xjunz. 保留所有权利
 */

package xjunz.tool.werecord.impl.model.account;

import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.Nullable;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

import xjunz.tool.werecord.impl.repo.AvatarRepository;
import xjunz.tool.werecord.impl.repo.RepositoryFactory;


/**
 * 微信账号（个人用户、群聊、公众号）的抽象类
 */
public class Account implements Parcelable {
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

    public static final Creator<Account> CREATOR = new Creator<Account>() {
        @NotNull
        @Contract("_ -> new")
        @Override
        public Account createFromParcel(Parcel in) {
            return new Account(in);
        }

        @NotNull
        @Contract(value = "_ -> new", pure = true)
        @Override
        public Account[] newArray(int size) {
            return new Account[size];
        }
    };

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

    public String getAliasOrId() {
        return empty(alias) ? id : alias;
    }

    /**
     * 获取当前账号的头像
     *
     * @return 当前账号的头像
     */
    @Nullable
    public Bitmap getAvatar() {
        if (!mHasTryDecodeAvatar) {
            Bitmap bitmap = RepositoryFactory.get(AvatarRepository.class).getAvatar(id);
            mHasTryDecodeAvatar = true;
            mHasLocalAvatar = bitmap != null;
            return bitmap;
        } else {
            return mHasLocalAvatar ? RepositoryFactory.get(AvatarRepository.class).getAvatar(id) : null;
        }
    }


    /**
     * 比较两个账号的ID是否相等
     * <p>
     * 也可仅传入微信ID的{@link String}，如果此账号的ID与传入的ID一致，返回true
     */
    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj instanceof Account) {
            return ((Account) obj).id.equals(id);
        } else if (obj instanceof String) {
            return obj.toString().equals(id);
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

    public Account() {
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NotNull Parcel dest, int flags) {
        dest.writeString(this.nickname);
        dest.writeString(this.alias);
        dest.writeString(this.id);
        dest.writeByte(this.mHasTryDecodeAvatar ? (byte) 1 : (byte) 0);
        dest.writeByte(this.mHasLocalAvatar ? (byte) 1 : (byte) 0);
    }

    protected Account(@NotNull Parcel in) {
        this.nickname = in.readString();
        this.alias = in.readString();
        this.id = in.readString();
        this.mHasTryDecodeAvatar = in.readByte() != 0;
        this.mHasLocalAvatar = in.readByte() != 0;
    }

    public String getIdentifier() {
        if (Objects.equals(getName(), id)) {
            return id;
        }
        return getName() + "(" + id + ")";
    }

}
