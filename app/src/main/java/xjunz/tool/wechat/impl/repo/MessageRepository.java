/*
 * Copyright (c) 2020 xjunz. 保留所有权利
 */

package xjunz.tool.wechat.impl.repo;

import android.content.ContentValues;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.sqlcipher.Cursor;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import xjunz.tool.wechat.impl.DatabaseModifier;
import xjunz.tool.wechat.impl.model.message.BackupMessage;
import xjunz.tool.wechat.impl.model.message.Message;
import xjunz.tool.wechat.impl.model.message.MessageFactory;

public class MessageRepository extends LifecyclePerceptiveRepository {


    MessageRepository() {
    }

    /**
     * 获得指定{@link xjunz.tool.wechat.impl.model.account.Talker}的ID的实际消息数
     * <p>
     * {@link xjunz.tool.wechat.impl.model.account.Talker#messageCount}获得的消息数
     * 来自"rconversation"表，是微信剔除了一些系统消息的消息数，不一定是"message"
     * 表里实际的消息数。
     *
     * @return 实际消息数
     * @see TalkerRepository#queryAll()
     * </p>
     */
    public int getActualMessageCountOf(String id) {
        Cursor cursor = getDatabase().rawQuery("select msgId from message where talker=" + "'" + id + "'", null);
        int actualCount = cursor.getCount();
        cursor.close();
        return actualCount;
    }

    /**
     * 查询指定微信ID的部分消息记录
     *
     * <p>此方法仅返回以{@param formerMsgList}的{@code size}为起始点的后{@param limitCount}条消息记录，不足则返回全部。
     * 记录以发送时间戳为排序依据，升序的形式排序。查询到的数据会追加进{@param formerMsgList}中。
     * </p>
     *
     * @param id            指定{@link xjunz.tool.wechat.impl.model.account.Talker}的微信ID
     * @param limitCount    查询的消息数量，不足则全部查询
     * @param formerMsgList 储存数据的{@link List}，数据会被追加到此{@link List}中
     * @return 查询到的实际消息数
     */
    public int queryMessageByTalkerLimit(@NonNull String id, int limitCount, @NonNull List<Message> formerMsgList) {
        Cursor cursor = getDatabase().rawQuery("select type,isSend,createTime,content,imgPath,msgId,status,talker from message where talker=" + "'"
                + id + "'" + " order by createTime desc" + " limit " + limitCount + " offset " + formerMsgList.size(), null);
        while (cursor.moveToNext()) {
            formerMsgList.add(buildMessageFromCursor(cursor));
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
    public List<Message> queryMessageByTalkerLimit(@NonNull String id, int offset, int limitCount) {
        List<Message> queried = new ArrayList<>();
        Cursor cursor = getDatabase().rawQuery("select type,isSend,createTime,content,imgPath,msgId,status,talker from message where talker=" + "'"
                + id + "'" + " order by createTime desc" + " limit " + limitCount + " offset " + offset, null);
        while (cursor.moveToNext()) {
            queried.add(buildMessageFromCursor(cursor));
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
    public Message queryMessageByMsgId(int msgId) {
        Cursor cursor = getDatabase().rawQuery("select * from message where msgId=" + msgId, null);
        if (cursor.moveToNext()) {
            return buildMessageFromCursor(cursor);
        }
        cursor.close();
        return null;
    }

    public List<BackupMessage> queryBackupMessages(String id) {
        List<BackupMessage> queried = new ArrayList<>();
        Cursor cursor = getDatabase().rawQuery("select type,isSend,createTime,content,imgPath,msgId,status,talker,edition from " + DatabaseModifier.TABLE_ORIGINAL_MESSAGE_BACKUP + " where talker=" + "'"
                + id + "'", null);
        while (cursor.moveToNext()) {
            queried.add(new BackupMessage(buildMessageFromCursor(cursor).getValues()));
        }
        cursor.close();
        return queried;
    }

    @NotNull
    @Contract("_ -> new")
    private Message buildMessageFromCursor(@NotNull Cursor cursor) {
        ContentValues values = new ContentValues();
        for (int i = 0; i < cursor.getColumnCount(); i++) {
            switch (cursor.getType(i)) {
                case Cursor.FIELD_TYPE_STRING:
                    values.put(cursor.getColumnName(i), cursor.getString(i));
                    break;
                case Cursor.FIELD_TYPE_BLOB:
                    values.put(cursor.getColumnName(i), cursor.getBlob(i));
                    break;
                case Cursor.FIELD_TYPE_FLOAT:
                    //使用getDouble()可同时兼容Float类型和Double类型，防止Double类型被转为Float导致溢出
                    values.put(cursor.getColumnName(i), cursor.getDouble(i));
                    break;
                case Cursor.FIELD_TYPE_INTEGER:
                    //使用getLong()可同时兼容Long类型和Integer类型，防止Long类型被转为Integer导致溢出
                    values.put(cursor.getColumnName(i), cursor.getLong(i));
                    break;
                case Cursor.FIELD_TYPE_NULL:
                    values.putNull(cursor.getColumnName(i));
                    break;
            }
        }
        return MessageFactory.createMessage(values);
    }
}
