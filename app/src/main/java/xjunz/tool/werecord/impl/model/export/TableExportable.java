/*
 * Copyright (c) 2021 xjunz. 保留所有权利
 */
package xjunz.tool.werecord.impl.model.export;

import androidx.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

/**
 * @author xjunz 2021/2/10 0:43
 */
public interface TableExportable extends Exportable {
    String exportAsTableElement(long ordinal);

    default void td(@NotNull StringBuilder builder, @Nullable String unitContent) {
        builder.append("<td>").append(unitContent == null ? "" : unitContent).append("</td>").append("\n");
    }
}
