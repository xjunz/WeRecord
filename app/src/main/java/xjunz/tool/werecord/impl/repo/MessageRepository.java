/*
 * Copyright (c) 2021 xjunz. 保留所有权利
 */

package xjunz.tool.werecord.impl.repo;

import android.content.ContentValues;
import android.util.ArrayMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.sqlcipher.Cursor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import xjunz.tool.werecord.impl.model.message.Message;
import xjunz.tool.werecord.impl.model.message.MessageFactory;

import static xjunz.tool.werecord.util.DbUtils.buildValuesFromCursor;

public class MessageRepository extends LifecyclePerceptiveRepository {
    public static final String TABLE_MESSAGE = "message";
    public static final String TABLE_APP_MESSAGE = "AppMessage";
    public static final String TABLE_MESSAGE_BACKUP = "MessageBackup";
    public static final String TABLE_APP_MESSAGE_BACKUP = "AppMessageBackup";


    private final ArrayMap<String, String> typeMap = new ArrayMap<>();

    MessageRepository() {
    }

    @Nullable
    public String getType(String colName) {
        return typeMap.get(colName);
    }


    @NonNull
    public String requireType(String colName) {
        return Objects.requireNonNull(typeMap.get(colName), "Could not find column: " + colName);
    }

    public void initTypeMap() {
        Cursor cursor = getDatabase().rawQuery("pragma table_info(" + TABLE_MESSAGE + ")", null);
        int nameIndex = cursor.getColumnIndex("name");
        int typeIndex = cursor.getColumnIndex("type");
        while (cursor.moveToNext()) {
            typeMap.put(cursor.getString(nameIndex), cursor.getString(typeIndex));
        }
        cursor.close();
        Cursor cursor2 = getDatabase().rawQuery("pragma table_info(" + TABLE_APP_MESSAGE + ")", null);
        while (cursor2.moveToNext()) {
            typeMap.put(cursor2.getString(nameIndex), cursor2.getString(typeIndex));
        }
        cursor2.close();
    }

    /**
     * 获得指定{@link xjunz.tool.werecord.impl.model.account.Talker}的ID的实际消息数
     * <p>
     * {@link xjunz.tool.werecord.impl.model.account.Talker#messageCount}获得的消息数
     * 来自"rconversation"表，是微信剔除了一些系统消息的消息数，不一定是"message"
     * 表里实际的消息数。
     *
     * @return 实际消息数
     * @see TalkerRepository#queryAll()
     * </p>
     */
    public long getActualMessageCountOf(String talkerId) {
        long count = 0;
        Cursor cursor = getDatabase().rawQuery("select count(msgId) from " + TABLE_MESSAGE + " where talker=" + "'" + talkerId + "'", null);
        if (cursor.moveToNext()) {
            count = cursor.getLong(0);
        }
        cursor.close();
        return count;
    }

    /**
     * 查询指定微信ID的部分消息记录
     *
     * <p>此方法仅返回以{@param formerMsgList}的{@code size}为起始点的后{@param limitCount}条消息记录，不足则返回全部。
     * 记录以发送时间戳为排序依据，升序的形式排序。查询到的数据会追加进{@param formerMsgList}中。
     * </p>
     *
     * @param id            指定{@link xjunz.tool.werecord.impl.model.account.Talker}的微信ID
     * @param limitCount    查询的消息数量，不足则全部查询
     * @param formerMsgList 储存数据的{@link List}，数据会被追加到此{@link List}中
     * @return 查询到的实际消息数
     */
    public int queryMessageByTalkerLimit(@NonNull String id, long limitCount, @NonNull List<Message> formerMsgList) {
        Cursor cursor = getDatabase().rawQuery("select * from " + TABLE_MESSAGE + " where talker=" + "'"
                + id + "'" + " order by createTime desc" + " limit " + limitCount + " offset " + formerMsgList.size(), null);
        while (cursor.moveToNext()) {
            formerMsgList.add(MessageFactory.createMessage(buildValuesFromCursor(cursor)));
        }
        int count = cursor.getCount();
        cursor.close();
        return count;
    }

    /**
     * 查询指定微信ID、指定消息起点和数量的消息记录，
     * 为了减少内存占用和查询时间，我们不会查询消息
     * 的所有字段，仅查询若干必要的字段
     * <p>
     * 记录以发送时间戳为排序依据，升序的形式排序
     * </p>
     *
     * @param id         微信ID
     * @param offset     起点
     * @param limitCount 消息数量
     * @return 查询到的消息列表
     */
    public List<Message> queryMessageByTalkerLimit(@NonNull String id, long offset, long limitCount) {
        List<Message> queried = new ArrayList<>();
        Cursor cursor = getDatabase().rawQuery("select * from " + TABLE_MESSAGE + " where talker=" + "'"
                + id + "'" + " order by createTime desc" + " limit " + limitCount + " offset " + offset, null);
        while (cursor.moveToNext()) {
            queried.add(MessageFactory.createMessage(buildValuesFromCursor(cursor)));
        }
        cursor.close();
        return queried;
    }

    public List<Message> rawQueryMessageByTalker(@NonNull String whereClause) {
        List<Message> queried = new ArrayList<>();
        Cursor cursor = getDatabase().rawQuery(String.format("select * from %s where %s", TABLE_MESSAGE, whereClause), null);
        while (cursor.moveToNext()) {
            queried.add(MessageFactory.createMessage(buildValuesFromCursor(cursor)));
        }
        cursor.close();
        return queried;
    }

    /**
     * 从数据库中查询某消息记录，且记录其所有字段
     *
     * @param msgId 消息ID
     * @return 返回查询到的消息，如果未查询到返回null
     */
    @Nullable
    public Message queryMessageByMsgId(long msgId) {
        Cursor cursor = getDatabase().rawQuery("select * from " + TABLE_MESSAGE + " where msgId=" + msgId, null);
        if (cursor.moveToNext()) {
            Message message = MessageFactory.createMessage(buildValuesFromCursor(cursor));
            cursor.close();
            return message;
        }
        cursor.close();
        return null;
    }

    @Nullable
    public ContentValues queryAppContentValuesByMsgId(long msgId) {
        ContentValues values = null;
        Cursor cursor = getDatabase().rawQuery("select * from " + TABLE_APP_MESSAGE + " where msgId=" + msgId, null);
        if (cursor.moveToNext()) {
            values = buildValuesFromCursor(cursor);
        }
        cursor.close();
        return values;
    }

    @Nullable
    public ContentValues queryBackupAppContentValuesByMsgId(long msgId) {
        ContentValues values = null;
        Cursor cursor = getDatabase().rawQuery("select * from " + TABLE_APP_MESSAGE_BACKUP + " where msgId=" + msgId, null);
        if (cursor.moveToNext()) {
            values = buildValuesFromCursor(cursor);
        }
        cursor.close();
        return values;
    }

    @Nullable
    public ContentValues queryBackupContentValuesByMsgId(long msgId) {
        Cursor cursor = getDatabase().rawQuery("select * from " + TABLE_MESSAGE_BACKUP + " where msgId=" + msgId, null);
        if (cursor.moveToNext()) {
            return buildValuesFromCursor(cursor);
        }
        cursor.close();
        return null;
    }

    public Message queryBackupMessageById(long msgId) {
        Cursor cursor = getDatabase().rawQuery("select * from " + TABLE_MESSAGE_BACKUP + " where msgId=" + msgId, null);
        if (cursor.moveToNext()) {
            Message message = MessageFactory.createMessage(buildValuesFromCursor(cursor));
            cursor.close();
            return message;
        }
        cursor.close();
        return null;
    }

    public void queryBackupMessagesByTalker(String id, @NonNull List<Message> backupMessages) {
        Cursor cursor = getDatabase().rawQuery("select * from " + TABLE_MESSAGE_BACKUP + " where talker=" + "'"
                + id + "'", null);
        while (cursor.moveToNext()) {
            backupMessages.add(MessageFactory.createMessage(buildValuesFromCursor(cursor)));
        }
        cursor.close();
    }

}
