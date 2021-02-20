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

import xjunz.tool.werecord.App;
import xjunz.tool.werecord.R;
import xjunz.tool.werecord.util.Utils;

/**
 * 暂不支持预览的消息类型，如推送消息和一些暂未识别的消息类型。
 */
public class UnpreviewableMessage extends ComplexMessage {

    public UnpreviewableMessage(ContentValues values, MessageFactory.Type type) {
        super(values, type);
    }

    @NonNull
    @Override
    public String getTitle() {
        return App.getStringOf(R.string.unpreviewable);
    }

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public String getCaption() {
        return null;
    }

    @NonNull
    @Override
    public String getParsedContent() {
        return getType().getCaption() + "\n" + getTitle();
    }

    @NonNull
    @Override
    public CharSequence getSpannedContent() {
        return HtmlCompat.fromHtml("<i>&lt;" + getParsedContent() + "&gt;</i>", HtmlCompat.FROM_HTML_MODE_LEGACY);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NotNull Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
    }

    protected UnpreviewableMessage(Parcel in) {
        super(in);
    }

    public static final Creator<UnpreviewableMessage> CREATOR = new Creator<UnpreviewableMessage>() {
        @NotNull
        @Contract("_ -> new")
        @Override
        public UnpreviewableMessage createFromParcel(Parcel source) {
            return new UnpreviewableMessage(source);
        }

        @NotNull
        @Contract(value = "_ -> new", pure = true)
        @Override
        public UnpreviewableMessage[] newArray(int size) {
            return new UnpreviewableMessage[size];
        }
    };

    /**
     * 格式为:
     * \@发送者名称 (时间)
     * [类型]
     * 如：
     * \@xjunz（2020.1.12 16:54:01:000）
     * [图片]
     */
    @Override
    public String exportAsPlainText() {
        return String.format("<%s> %s \n[%s]",
                requireSenderName(),
                Utils.formatDateLocally(getCreateTimeStamp()),
                getType().getCaption());
    }
}
