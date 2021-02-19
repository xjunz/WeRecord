/*
 * Copyright (c) 2021 xjunz. 保留所有权利
 */
package xjunz.tool.werecord.impl.model.message;

import android.content.ContentValues;
import android.os.Parcel;

import androidx.annotation.NonNull;
import androidx.core.text.HtmlCompat;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import xjunz.tool.werecord.util.Utils;

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
        @NotNull
        @Contract("_ -> new")
        @Override
        public PlainMessage createFromParcel(Parcel source) {
            return new PlainMessage(source);
        }

        @NotNull
        @Contract(value = "_ -> new", pure = true)
        @Override
        public PlainMessage[] newArray(int size) {
            return new PlainMessage[size];
        }
    };

    @Override
    public String exportAsPlainText() {
        return String.format("<%s> %s \n%s",
                requireSenderName(),
                Utils.formatDateLocally(getCreateTimeStamp()),
                getParsedContent());
    }
}
