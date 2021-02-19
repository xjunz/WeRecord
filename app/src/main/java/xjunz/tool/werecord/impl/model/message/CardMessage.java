/*
 * Copyright (c) 2021 xjunz. 保留所有权利
 */
package xjunz.tool.werecord.impl.model.message;


import android.content.ContentValues;
import android.os.Parcel;
import android.text.Html;

import androidx.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import xjunz.tool.werecord.util.Utils;

/**
 * 名片消息，其类型为42
 *
 * @see MessageFactory#TYPE_CARD
 */
public class CardMessage extends ComplexMessage {
    private final String nickname;
    private final String username;

    public CardMessage(ContentValues values) {
        super(values, MessageFactory.Type.CARD);
        //escape character entity reference (&#x)
        nickname = Html.fromHtml(Utils.extractFirst(getContent(), "nickname=\"(.+?)\"")).toString();
        username = Utils.extractFirst(getContent(), "username=\"(.+?)\"");
    }

    @Nullable
    @Override
    public String getTitle() {
        return nickname;
    }

    @Nullable
    @Override
    public String getDescription() {
        return username;
    }

    @Nullable
    @Override
    public String getCaption() {
        return null;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NotNull Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(this.nickname);
        dest.writeString(this.username);
    }

    protected CardMessage(Parcel in) {
        super(in);
        this.nickname = in.readString();
        this.username = in.readString();
    }

    public static final Creator<CardMessage> CREATOR = new Creator<CardMessage>() {
        @Override
        public CardMessage createFromParcel(Parcel source) {
            return new CardMessage(source);
        }

        @Override
        public CardMessage[] newArray(int size) {
            return new CardMessage[size];
        }
    };
}
