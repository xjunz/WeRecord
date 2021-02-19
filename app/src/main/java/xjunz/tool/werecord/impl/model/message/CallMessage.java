/*
 * Copyright (c) 2021 xjunz. 保留所有权利
 */
package xjunz.tool.werecord.impl.model.message;

import android.content.ContentValues;
import android.os.Parcel;

import androidx.annotation.Nullable;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public class CallMessage extends ComplexMessage {

    private static final String VOICE_CALL_CONTENT = "voip_content_voice";
    private static final String VIDEO_CALL_CONTENT = "voip_content_video";
    public static final String ABSTRACT_KEY_CALL_CONTENT = "call_content";
    private static final int INDEX_OF_CALL_CONTENT = 2;

    public CallMessage(ContentValues values) {
        super(values, judgeType(values.getAsString(KEY_CONTENT)));
    }

    private static MessageFactory.Type judgeType(@Nullable String content) {
        if (VOICE_CALL_CONTENT.equals(content)) {
            return MessageFactory.Type.VOICE_CALL;
        } else if (VIDEO_CALL_CONTENT.equals(content)) {
            return MessageFactory.Type.VIDEO_CALL;
        } else {
            return MessageFactory.Type.CALL;
        }
    }

    @Nullable
    @Override
    public String getTitle() {
        return parsedLvBuffer == null ? null : (String) parsedLvBuffer[INDEX_OF_CALL_CONTENT];
    }

    @SuppressWarnings("unchecked")
    @Nullable
    @Override
    public <T> T get(@NotNull String key) {
        if (ABSTRACT_KEY_CALL_CONTENT.equals(key)) {
            return (T) getTitle();
        }
        return super.get(key);
    }

    @Override
    public void modify(@NotNull String key, Object content) {
        //改变内容可能改变内容
        if (KEY_CONTENT.equals(key) || ABSTRACT_KEY_CONTENT.equals(key)) {
            super.modify(key, content);
            this.type = judgeType((String) content);
        } else if (ABSTRACT_KEY_CALL_CONTENT.equals(key)) {
            //修改内容
            parsedLvBuffer[INDEX_OF_CALL_CONTENT] = content;
            super.modify(ABSTRACT_KEY_LVBUFFER, parsedLvBuffer);
        } else {
            super.modify(key, content);
        }
    }

    @Nullable
    @Override
    public String getDescription() {
        return null;
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
    }

    protected CallMessage(Parcel in) {
        super(in);
    }

    public static final Creator<CallMessage> CREATOR = new Creator<CallMessage>() {
        @NotNull
        @Contract("_ -> new")
        @Override
        public CallMessage createFromParcel(Parcel source) {
            return new CallMessage(source);
        }

        @NotNull
        @Contract(value = "_ -> new", pure = true)
        @Override
        public CallMessage[] newArray(int size) {
            return new CallMessage[size];
        }
    };
}
