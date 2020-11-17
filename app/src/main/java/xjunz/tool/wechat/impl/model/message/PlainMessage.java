/*
 * Copyright (c) 2020 xjunz. 保留所有权利
 */
package xjunz.tool.wechat.impl.model.message;

import android.content.ContentValues;
import android.text.Html;

import androidx.annotation.NonNull;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 普通文本消息类型
 * <p>
 * 微信会解析{@code &lt;a/&gt;}标签，因此我们需要判断文本内容里是否有该标签
 * </p>
 */
public class PlainMessage extends Message {
    private boolean isSpanned;
    private String parsedContent;
    private CharSequence spannedContent;


    public PlainMessage(ContentValues values) {
        super(values);
    }

    @NonNull
    @Override
    public String getParsedContent() {
        if (parsedContent == null) {
            Pattern pattern = Pattern.compile("<a .+?=\".+?\">(.+?)</a>");
            Matcher matcher = pattern.matcher(content);
            this.parsedContent = content;
            while (matcher.find()) {
                isSpanned = true;
                parsedContent = parsedContent.replace(matcher.group(), Objects.requireNonNull(matcher.group(1)));
            }
        }
        return parsedContent;
    }

    @NonNull
    @Override
    public CharSequence getSpannedContent() {
        if (spannedContent == null) {
            getParsedContent();
            spannedContent = isSpanned ? Html.fromHtml(content.replaceAll("\n", "<br>")) : content;
        }
        return spannedContent;
    }
}
