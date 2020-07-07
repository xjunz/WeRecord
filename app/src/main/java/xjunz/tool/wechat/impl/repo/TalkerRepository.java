package xjunz.tool.wechat.impl.repo;

import android.text.TextUtils;

import net.sqlcipher.Cursor;
import net.sqlcipher.database.SQLiteDatabase;

import java.util.ArrayList;

import xjunz.tool.wechat.impl.model.account.Talker;

public class TalkerRepository extends AccountRepository<Talker> {
    private static TalkerRepository sInstance;

    public static TalkerRepository getInstance() {
        return sInstance = sInstance == null ? new TalkerRepository() : sInstance;
    }

    /**
     * 查询所有聊天数据
     */
    public void queryAll() {
        mAll = new ArrayList<>();
        SQLiteDatabase database = getDatabase();
        Cursor talkerQueryCursor = database.rawQuery("select username,conversationTime,msgCount from rconversation where not msgcount = 0", null);
        while (!talkerQueryCursor.isClosed() && talkerQueryCursor.moveToNext()) {
            String id = talkerQueryCursor.getString(0);
            if (!TextUtils.isEmpty(id)) {
                Talker talker = new Talker();
                talker.endowIdentity(id);
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
                mAll.add(talker);
            }
            if (talkerQueryCursor.isLast()) {
                talkerQueryCursor.close();
            }
        }
    }

    @Override
    public void purge() {
        sInstance = null;
    }

}
