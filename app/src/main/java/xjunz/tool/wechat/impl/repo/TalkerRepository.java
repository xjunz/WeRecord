/*
 * Copyright (c) 2020 xjunz. 保留所有权利
 */

package xjunz.tool.wechat.impl.repo;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.sqlcipher.Cursor;
import net.sqlcipher.database.SQLiteDatabase;

import java.util.List;

import xjunz.tool.wechat.impl.model.account.Contact;
import xjunz.tool.wechat.impl.model.account.Talker;

public class TalkerRepository extends AccountRepository<Talker> {

    TalkerRepository() {
    }

    /**
     * 查询所有聊天数据
     */
    @Override
    protected void queryAll(@NonNull List<Talker> all) {
        SQLiteDatabase database = getDatabase();
        Cursor talkerQueryCursor = database.rawQuery("select username,conversationTime,msgCount from rconversation where not msgcount = 0", null);
        while (!talkerQueryCursor.isClosed() && talkerQueryCursor.moveToNext()) {
            String id = talkerQueryCursor.getString(0);
            if (!TextUtils.isEmpty(id)) {
                Talker talker = new Talker();
                talker.id = id;
                Cursor contactQueryCursor = database.rawQuery("select alias,conRemark,nickname,type from rcontact where username='" + id + "'", null);
                if (contactQueryCursor.moveToNext()) {
                    talker.alias = contactQueryCursor.getString(0);
                    talker.remark = contactQueryCursor.getString(1);
                    talker.nickname = contactQueryCursor.getString(2);
                    talker.rawType = contactQueryCursor.getInt(3);
                    talker.judgeType();
                    contactQueryCursor.close();
                }
                talker.lastMsgTimestamp = talkerQueryCursor.getLong(1);
                talker.messageCount = talkerQueryCursor.getInt(2);
                get(talker.type).add(talker);
                all.add(talker);
            }
            if (talkerQueryCursor.isLast()) {
                talkerQueryCursor.close();
            }
        }
    }

    @Nullable
    @SuppressWarnings("SuspiciousMethodCalls")
    @Override
    public Talker get(@NonNull String id) {
        int index = getAll().indexOf(Contact.mockAccount(id));
        if (index >= 0) {
            return getAll().get(index);
        } else {
            return null;
        }
    }


}
