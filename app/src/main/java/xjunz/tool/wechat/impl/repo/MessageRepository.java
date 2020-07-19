/*
 * Copyright (c) 2020 xjunz. 保留所有权利
 */

package xjunz.tool.wechat.impl.repo;

import android.util.LruCache;

import androidx.annotation.NonNull;

import net.sqlcipher.Cursor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import xjunz.tool.wechat.impl.model.message.Message;


public class MessageRepository extends LifecyclePerceptiveRepository {
    private static MessageRepository sInstance;
    private final static LruCache<String, List<Message>> sMessageCache = new LruCache<String, List<Message>>(1024 * 2) {
        @Override
        protected int sizeOf(String key, List<Message> value) {
            return value.size();
        }
    };


    public List<Message> getMessagesOf(String talkerId) {
        MessageRepository messageDao = MessageRepository.getInstance();
        if (sMessageCache.get(talkerId) != null) {
            return sMessageCache.get(talkerId);
        } else {
            List<Message> messages = messageDao.queryMessagesByTalker(talkerId);
            Collections.sort(messages, (o1, o2) -> Long.compare(o1.getCreateTimeStamp(), o2.getCreateTimeStamp()));
            synchronized (sMessageCache) {
                sMessageCache.put(talkerId, messages);
            }
            return messages;
        }
    }


    private MessageRepository() {
    }

    @Override
    public void purge() {
        sInstance = null;
    }


    public static MessageRepository getInstance() {
        sInstance = sInstance == null ? new MessageRepository() : sInstance;
        return sInstance;
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
        Cursor cursor = getDatabase().rawQuery("select type,isSend,createTime,content,imgPath,msgId,status from message where talker=" + "'"
                + id + "'" + " order by createTime desc" + " limit " + limitCount + " offset " + formerMsgList.size(), null);
        int i = 0;
        while (cursor.moveToNext()) {
            i++;
            Message message = new Message(cursor.getString(3), id);
            message.setRawType(cursor.getInt(0));
            message.setSend(cursor.getInt(1) == 1);
            message.setCreateTimeStamp(cursor.getLong(2));
            message.setImgPath(cursor.getString(4));
            message.setMsgId(cursor.getInt(5));
            message.setStatus(cursor.getInt(6));
            formerMsgList.add(message);
        }
        cursor.close();
        return i;
    }


    /**
     * 查询指定微信ID的全部消息记录
     *
     * @param id 指定{@link xjunz.tool.wechat.impl.model.account.Talker}的微信ID
     * @return 全部消息记录
     */
    public List<Message> queryMessagesByTalker(String id) {
        ArrayList<Message> messages = new ArrayList<>();
        Cursor cursor = getDatabase().rawQuery("select rawType,isSend,createTime,content,imgPath,msgId from message where talker=" + "'" + id + "'", null);
        while (cursor.moveToNext()) {
            Message message = new Message(cursor.getString(3), id);
            message.setSend(cursor.getInt(1) == 1);
            message.setCreateTimeStamp(cursor.getLong(2));
            message.setImgPath(cursor.getString(4));
            message.setMsgId(cursor.getInt(5));
            message.setStatus(cursor.getInt(6));
            messages.add(message);
        }
        cursor.close();
        return messages;
    }


}
