package xjunz.tool.wechat.impl.repo;

import android.util.LruCache;

import net.sqlcipher.Cursor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
            Collections.sort(messages, new Comparator<Message>() {
                @Override
                public int compare(Message o1, Message o2) {
                    return Long.compare(o1.getCreateTimeStamp(), o2.getCreateTimeStamp());
                }
            });
            synchronized (sMessageCache) {
                sMessageCache.put(talkerId, messages);
            }
            return messages;
        }
    }

    public int getMessageCount(String talkerId) {
        if (sMessageCache.get(talkerId) != null) {
            return sMessageCache.get(talkerId).size();
        } else {
            return queryMessageCount(talkerId);
        }
    }


    private int queryMessageCount(final String id) {
        Cursor cursor = getDatabase().rawQuery("select rawType,isSend,createTime,content,imgPath,msgId from message where talker=" + "'" + id + "'", null);
        int count = cursor.getCount();
        cursor.close();
        return count;
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


    public List<Message> queryMessagesByTalker(String id) {
        ArrayList<Message> messages = new ArrayList<>();
        Cursor cursor = getDatabase().rawQuery("select rawType,isSend,createTime,content,imgPath,msgId from message where talker=" + "'" + id + "'", null);
        while (cursor.moveToNext()) {
            Message message = new Message(cursor.getString(3));
            message.setSend(cursor.getInt(1) == 1);
            message.setCreateTimeStamp(cursor.getLong(2));
            message.setTalkerId(id);
            message.setImgName(cursor.getString(4));
            message.setMsgId(cursor.getInt(5));
            messages.add(message);
        }
        cursor.close();
        return messages;
    }


}
