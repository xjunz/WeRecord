/*
 * Copyright (c) 2020 xjunz. 保留所有权利
 */
package xjunz.tool.wechat.impl.model.message;

import android.content.ContentValues;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import xjunz.tool.wechat.R;

public class BackupMessage extends Message {

    public static final String KEY_EDITION = "edition";

    public enum EditionType {
        MODIFY(1, R.string.modified), DELETE(2, R.string.deleted), INSERT(3, R.string.added);

        public int flag;
        @StringRes
        public int captionRes;

        private EditionType(int flag, @StringRes int captionRes) {
            this.flag = flag;
            this.captionRes = captionRes;
        }
    }

    public BackupMessage(@NonNull ContentValues values) {
        super(values);
    }


    @NonNull
    public EditionType getEditionType() {
        int editionFlag = getValues().getAsInteger(KEY_EDITION);
        switch (getValues().getAsInteger(KEY_EDITION)) {
            case 1:
                return EditionType.MODIFY;
            case 2:
                return EditionType.DELETE;
            case 3:
                return EditionType.INSERT;
        }
        throw new IllegalStateException(getMsgId() + " has an unknown edition type with flag " + editionFlag);
    }

}
