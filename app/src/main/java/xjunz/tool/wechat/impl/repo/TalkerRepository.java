package xjunz.tool.wechat.impl.repo;

import android.text.TextUtils;

import androidx.annotation.NonNull;

import net.sqlcipher.Cursor;
import net.sqlcipher.database.SQLiteDatabase;

import java.util.ArrayList;

import xjunz.tool.wechat.impl.model.account.Talker;

public class TalkerRepository extends LifecyclePerceptiveRepository {
    private static TalkerRepository sInstance;
    private ArrayList<Talker> mAll;

    public static TalkerRepository getInstance() {
        sInstance = sInstance == null ? new TalkerRepository() : sInstance;
        return sInstance;
    }

    @NonNull
    public ArrayList<Talker> getAll() {
        if (mAll == null) {
            throw new RuntimeException("Please call queryAll() first. ");
        }
        return mAll;
    }

    /**
     * 从数据库中查询字段并复制给相应的属性
     *
     * @param database 数据库对象
     */
    public void enrich(@NonNull SQLiteDatabase database, @NonNull String id) {

    }

    /**
     * 查询所有聊天数据
     */
    public void queryAll() {
        mAll = new ArrayList<>();
        SQLiteDatabase database = getDatabase();
        Cursor talkerQueryCursor = database.rawQuery("select username,conversationTime,msgCount from rconversation", null);
        while (!talkerQueryCursor.isClosed() && talkerQueryCursor.moveToNext()) {
            String id = talkerQueryCursor.getString(0);
            if (!TextUtils.isEmpty(id)) {
                Talker talker = new Talker();
                talker.endowIdentity(id);
                Cursor contactQueryCursor = database.rawQuery("select alias,conRemark,nickname,pyInitial,conRemarkPYShort,type from rcontact where username='" + id + "'", null);
                if (contactQueryCursor.moveToNext()) {
                    talker.alias = contactQueryCursor.getString(0);
                    talker.remark = contactQueryCursor.getString(1);
                    talker.nickname = contactQueryCursor.getString(2);
                    talker.nicknamePyAbbr = contactQueryCursor.getString(3);
                    talker.remarkPyAbbr = contactQueryCursor.getString(4);
                    talker.rawType = contactQueryCursor.getInt(5);
                    talker.judgeType();
                    contactQueryCursor.close();
                }
                talker.setLastMsgTimestamp(talkerQueryCursor.getLong(1));
                talker.messageCount = talkerQueryCursor.getInt(2);
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
