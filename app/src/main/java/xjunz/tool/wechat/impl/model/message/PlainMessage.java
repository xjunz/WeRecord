/*
 * Copyright (c) 2021 xjunz. 保留所有权利
 */
package xjunz.tool.wechat.impl.model.message;

import android.content.ContentValues;
import android.os.Parcel;

import androidx.annotation.NonNull;
import androidx.core.text.HtmlCompat;

import org.jetbrains.annotations.NotNull;

/**
 * 普通文本消息类型
 */
public class PlainMessage extends Message {

    public PlainMessage(ContentValues values) {
        super(values, MessageFactory.Type.PLAIN);
    }

    @NonNull
    @Override
    public String getParsedContent() {
        if (parsedContent == null) {
            parseSpan();
        }
        return parsedContent;
    }

    private void parseSpan() {
        spannedContent = HtmlCompat.fromHtml(content.replace("\n", "<br>"), HtmlCompat.FROM_HTML_MODE_LEGACY);
        parsedContent = spannedContent.toString();
    }

    @NonNull
    @Override
    public CharSequence getSpannedContent() {
        if (spannedContent == null) {
            parseSpan();
        }
        return spannedContent;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NotNull Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(this.parsedContent);
    }

    protected PlainMessage(Parcel in) {
        super(in);
        this.parsedContent = in.readString();
    }

    public static final Creator<PlainMessage> CREATOR = new Creator<PlainMessage>() {
        @Override
        public PlainMessage createFromParcel(Parcel source) {
            return new PlainMessage(source);
        }

        @Override
        public PlainMessage[] newArray(int size) {
            return new PlainMessage[size];
        }
    };
}
