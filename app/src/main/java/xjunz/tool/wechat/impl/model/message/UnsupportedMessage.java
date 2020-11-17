/*
 * Copyright (c) 2020 xjunz. 保留所有权利
 */
package xjunz.tool.wechat.impl.model.message;

import android.content.ContentValues;
import android.text.Html;

import androidx.annotation.NonNull;

public class UnsupportedMessage extends Message {

    public UnsupportedMessage(ContentValues values) {
        super(values);
    }

    @NonNull
    @Override
    public String getParsedContent() {
        return getType().getCaption();
    }

    @NonNull
    @Override
    public CharSequence getSpannedContent() {
        return Html.fromHtml("<i>&lt;" + getParsedContent() + "&gt;</i>");
    }
}
