/*
 * Copyright (c) 2020 xjunz. 保留所有权利
 */
package xjunz.tool.wechat.impl.model.message;

import android.content.ContentValues;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class BackupMessage extends Message {

    public static final String KEY_EDITION = "edition";

    public BackupMessage(@NonNull ContentValues values) {
        super(values);
    }

    @Override
    public int getEditionFlag() {
        return getValues().getAsInteger(KEY_EDITION);
    }

    public void removeEditionFlag() {
        getValues().remove(KEY_EDITION);
    }

    @NonNull
    public Edition toEdition(@Nullable Message rep) {
        switch (this.getEditionFlag()) {
            case Edition.FLAG_REMOVAL:
                return Edition.remove(this);
            case Edition.FLAG_INSERTION:
                return Edition.insert(this);
            case Edition.FLAG_REPLACEMENT:
                return Edition.replace(this, rep);
        }
        throw new IllegalArgumentException("Unexpected edition flag: " + getEditionFlag());
    }
}
