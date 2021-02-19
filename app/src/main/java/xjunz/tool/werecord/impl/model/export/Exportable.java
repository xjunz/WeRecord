/*
 * Copyright (c) 2021 xjunz. 保留所有权利
 */
package xjunz.tool.werecord.impl.model.export;

import android.content.ContentValues;

/**
 * @author xjunz 2021/1/26 22:47
 */
public interface Exportable {
    /**
     * 以纯文本形式导出
     */
    String exportAsPlainText();

    /**
     * 以{@link ContentValues}形式导出，用于数据库支持
     */
    ContentValues exportAsContentValues();

    /**
     * 以富文本形式导出
     */
    String exportAsHtml();
}
