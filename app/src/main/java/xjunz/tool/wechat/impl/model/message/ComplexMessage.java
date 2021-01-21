/*
 * Copyright (c) 2021 xjunz. 保留所有权利
 */
package xjunz.tool.wechat.impl.model.message;

import android.content.ContentValues;
import android.os.Parcel;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

/**
 * 复杂消息类型是一类以复杂消息模板（如下所示）显示的消息，其具体消息类型是不确定的。
 * |———————|
 * |Type   caption|
 * |TITLE         |
 * |description   |
 * |———————|
 */

public abstract class ComplexMessage extends Message {

    public ComplexMessage(ContentValues values, MessageFactory.Type superType) {
        super(values, superType);
    }

    @Nullable
    public abstract String getTitle();

    @Nullable
    public abstract String getDescription();

    /**
     * 返回解析后的内容
     * <p>
     * {@link ComplexMessage#getTitle()}+换行+{@link ComplexMessage#getDescription()}
     * </p>
     *
     * @return 解析后的内容
     */
    @NonNull
    @Override
    public String getParsedContent() {
        if (parsedContent == null) {
            StringBuilder sb = new StringBuilder();
            if (!TextUtils.isEmpty(getTitle())) {
                sb.append(getTitle());
                if (!TextUtils.isEmpty(getDescription())) {
                    sb.append("\n").append(getDescription());
                }
            } else {
                if (!TextUtils.isEmpty(getDescription())) {
                    sb.append(getDescription());
                }
            }
            parsedContent = sb.toString();
        }
        return parsedContent;
    }

    @NonNull
    @Override
    public CharSequence getSpannedContent() {
        return getParsedContent();
    }

    @Nullable
    public abstract String getCaption();

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NotNull Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
    }

    protected ComplexMessage(Parcel in) {
        super(in);
    }
}
