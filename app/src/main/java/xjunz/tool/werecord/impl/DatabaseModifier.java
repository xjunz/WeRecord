/*
 * Copyright (c) 2021 xjunz. 保留所有权利
 */
package xjunz.tool.werecord.impl;

import android.content.ContentValues;
import android.util.ArrayMap;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;
import androidx.databinding.BaseObservable;
import androidx.databinding.Bindable;

import net.sqlcipher.Cursor;
import net.sqlcipher.database.SQLiteDatabase;

import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import xjunz.tool.werecord.App;
import xjunz.tool.werecord.BR;
import xjunz.tool.werecord.impl.model.account.Talker;
import xjunz.tool.werecord.impl.model.message.Message;
import xjunz.tool.werecord.impl.model.message.util.Edition;
import xjunz.tool.werecord.impl.repo.MessageRepository;
import xjunz.tool.werecord.impl.repo.RepositoryFactory;
import xjunz.tool.werecord.util.IoUtils;
import xjunz.tool.werecord.util.ShellUtils;

import static java.lang.String.format;
import static xjunz.tool.werecord.impl.model.message.util.Edition.FLAG_INSERTION;
import static xjunz.tool.werecord.impl.model.message.util.Edition.FLAG_REMOVAL;
import static xjunz.tool.werecord.impl.model.message.util.Edition.FLAG_REPLACEMENT;
import static xjunz.tool.werecord.impl.repo.ContactRepository.TABLE_CONTACT;
import static xjunz.tool.werecord.impl.repo.MessageRepository.TABLE_MESSAGE;
import static xjunz.tool.werecord.impl.repo.MessageRepository.TABLE_MESSAGE_BACKUP;
import static xjunz.tool.werecord.impl.repo.TalkerRepository.TABLE_CONVERSATION;

/**
 * 数据库修改器，实现了数据库修改的各个方法
 * <p>
 * 通过{@link Environment#modifyDatabase()}获取实例
 * </p>
 */
public class DatabaseModifier extends BaseObservable {
    private final String workerDatabasePath;
    private final String originDatabasePath;
    /**
     * 对于数据库的更改是否已同步到微信的数据库
     */
    private boolean thereAnyPendingEdition;
    private final ArrayMap<Long, Edition> mPendingEditions = new ArrayMap<>();

    @NotNull
    private SQLiteDatabase getDb() {
        return Environment.getInstance().getWorkerDatabase();
    }

    DatabaseModifier(@NotNull Environment environment) {
        this.originDatabasePath = environment.getCurrentUser().originalDatabaseFilePath;
        this.workerDatabasePath = environment.getCurrentUser().workerDatabaseFilePath;
    }

    @Bindable
    public boolean isThereAnyPendingEdition() {
        return thereAnyPendingEdition;
    }

    public void setThereAnyPendingEdition(boolean has) {
        this.thereAnyPendingEdition = has;
        notifyPropertyChanged(BR.thereAnyPendingEdition);
    }

    public void putPendingEdition(Edition edition) {
        mPendingEditions.put(edition.getTargetMsgId(), edition);
        setThereAnyPendingEdition(true);
    }

    public void removePendingEdition(long msgId) {
        mPendingEditions.remove(msgId);
        setThereAnyPendingEdition(mPendingEditions.size() != 0);
    }

    public void removeAllPendingEditions() {
        mPendingEditions.clear();
        setThereAnyPendingEdition(false);
    }

    public ArrayMap<Long, Edition> getAllPendingEditions() {
        return mPendingEditions;
    }

    @WorkerThread
    public void applyAllPendingEditions() throws ShellUtils.ShellException {
        for (int i = 0; i < mPendingEditions.size(); i++) {
            Edition edition = mPendingEditions.valueAt(i);
            switch (edition.getFlag()) {
                case FLAG_REMOVAL:
                    deleteMessage(edition.getVictim());
                    break;
                case FLAG_INSERTION:
                    insertMessage(edition.getFiller());
                    break;
                case FLAG_REPLACEMENT:
                    replaceMessage(edition.getFiller());
                    break;
            }
        }
        apply();
        removeAllPendingEditions();
    }

    /**
     * 删除备份表
     */
    public void dropMessageBackupTables() {
        getDb().execSQL("drop table " + TABLE_MESSAGE_BACKUP);
    }

    public boolean isMessageBackupTableExists() {
        return queryExistence("select name from sqlite_master where type='table' and name='" + TABLE_MESSAGE_BACKUP + "'");
    }

    /**
     * 创建消息备份表(如果不存在)，该表会复制{@code message}表的结构，但不会复制其内容。
     */
    public void createBackupTableIfNotExists() {
        //如果备份表不存在
        if (!queryExistence("select name from sqlite_master where type='table' and name='" + TABLE_MESSAGE_BACKUP + "'")) {
            //创建备份表并复制message的表结构
            getDb().execSQL("create table " + TABLE_MESSAGE_BACKUP + " as select * from " + TABLE_MESSAGE + " where 1<>1");
            //创建"edition"字段
            getDb().execSQL("alter table " + TABLE_MESSAGE_BACKUP + " add " + Message.KEY_EDITION + " int default 1");
            //为msgId创建唯一索引
            getDb().execSQL("create unique index index" + TABLE_MESSAGE_BACKUP + "MsgId on " + TABLE_MESSAGE_BACKUP + " (msgId)");
        }
    }


    /**
     * 备份某条消息，如果备份不存在的话
     *
     * @param msg 指定消息
     */
    private void backupMessageIfNotExists(@NotNull Message msg, int editionFlag) {
        long msgId = msg.getMsgId();
        createBackupTableIfNotExists();
        //如果备份消息已存在，更新edition字段
        if (queryExistence(format("select * from %s where msgId=%s", TABLE_MESSAGE_BACKUP, msgId))) {
            getDb().execSQL(format("update %s set edition=%s where msgId=%s", TABLE_MESSAGE_BACKUP, editionFlag, msgId));
        }
        //如果不存在
        else {
            //备份原消息
            getDb().execSQL(format("insert into %s select *,%s from %s where msgId=%s", TABLE_MESSAGE_BACKUP, editionFlag, TABLE_MESSAGE, msgId));
        }
    }


    /**
     * 恢复某条消息
     *
     * @param msg 指定消息
     */
    public void restoreMessage(@NotNull Message msg) {
        long msgId = msg.getMsgId();
        ContentValues values = Objects.requireNonNull(RepositoryFactory.get(MessageRepository.class).queryBackupContentValuesByMsgId(msgId), "Cannot find backup for message " + msgId);
        //去除edition标签
        values.remove(Message.KEY_EDITION);
        //恢复记录,注：如果记录不存在（被删除），replace方法就会插入消息，因此不必加以判断
        getDb().replace(TABLE_MESSAGE, "content", values);
        //然后删除备份表中已恢复的记录
        getDb().execSQL(format("delete from %s where msgId = %s", TABLE_MESSAGE_BACKUP, msgId));
    }

    /**
     * 向数据库中的message表中插入一条{@link Message}
     *
     * @param msg 欲插入的消息
     */
    public void insertMessage(@NotNull Message msg) {
        //msgId字段是自增的，我们不需要也不能手动赋值，因此将其删除
        msg.getValues().remove(Message.KEY_MSG_ID);
        //插入后我们才能知道该消息的ID
        long msgId = getDb().insert(TABLE_MESSAGE, "content", msg.getValues());
        //设置ID（后面备份要用到）
        msg.getValues().put(Message.KEY_MSG_ID, msgId);
        //对其进行备份
        backupMessageIfNotExists(msg, FLAG_INSERTION);
    }

    private boolean queryExistence(String query) {
        Cursor cursor = getDb().rawQuery(query, null);
        int count = cursor.getCount();
        cursor.close();
        return count > 0;
    }

    ;

    /**
     * 删除某条消息
     *
     * @param msg 指定消息
     */
    public void deleteMessage(@NonNull Message msg) {
        long msgId = msg.getMsgId();
        //假设这是一条新增的消息，先从备份表中删除这条消息（因为新增的消息是有备份的）
        int affected = getDb().delete(TABLE_MESSAGE_BACKUP, format("msgId=%s and edition=%s", msgId, FLAG_INSERTION), null);
        //如果什么都没被删除，说明不是新增的消息，那么我们先备份消息
        if (affected == 0) {
            backupMessageIfNotExists(msg, FLAG_REMOVAL);
        }
        //再将其删除
        getDb().delete(TABLE_MESSAGE, "msgId=" + msg.getMsgId(), null);
    }


    /**
     * 替换数据库中的message表中的一条{@link Message}，
     * 以{@link Message#getMsgId()}为替换依据
     *
     * @param msg 用于替换的消息
     */
    public void replaceMessage(@NonNull Message msg) {
        backupMessageIfNotExists(msg, FLAG_REPLACEMENT);
        //替换消息
        getDb().replace(TABLE_MESSAGE, "content", msg.getValues());
    }

    public void addVerifyMessageFromId(String id) throws IOException {
        String table_fmesaage = "fmessage_conversation";
        if (!queryExistence(format("select talker from %s where talker='%s'", table_fmesaage, id))) {
            InputStream in = App.getContext().getAssets().open("verify.xml");
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            IoUtils.transferStream(in, out);
            String content = String.format(out.toString(), id);
            ContentValues values = new ContentValues();
            values.put("talker", id);
            values.put("displayName", id);
            values.put("state", 0);
            values.put("contentFromUsername", id);
            values.put("lastModifiedTime", System.currentTimeMillis());
            //values.put("addScene", 0);
            values.put("isNew", 1);
            ContentValues values1 = new ContentValues();
            values1.put("msgContent", content);
            values1.put("talker", id);
            values1.put("type", 1);
            values1.put("createTime", System.currentTimeMillis());
            long row = getDb().replace("fmessage_msginfo", null, values1);
            if (row != -1) {
                values.put("fmsgSysRowId", row);
                getDb().replace(table_fmesaage, "talker", values);
            }
        }
    }

    /**
     * 新增一个本地联系人
     */
    public void addContactWithId(String id) {
        if (queryExistence(format("select username from %s where username='%s'", TABLE_CONTACT, id))) {
            getDb().execSQL(format("update %s set type=%s where username='%s'", TABLE_CONTACT, 3, id));
        } else {
            ContentValues values = new ContentValues();
            values.put("type", 3);
            values.put("username", id);
            values.put("conRemark", id);
            getDb().insert(TABLE_CONTACT, "username", values);
        }
    }

    public boolean deleteContactWithId(String id) {
        int affected = getDb().delete(TABLE_CONTACT, format("username='%s'", id), null);
        return affected != 0;
    }


    /**
     * 创建联系人标签
     *
     * @param name 标签名
     */
    public void createContactLabelIfNotExists(@NonNull String name) {
        //如果不存在，我们才能插入标签
        if (!queryExistence(format("select labelName from ContactLabel where labelName='%s'", name))) {
            ContentValues values = new ContentValues();
            values.put("labelName", name);
            values.put("createTime", System.currentTimeMillis());
            getDb().insert("ContactLabel", "labelName", values);
        }
    }

    /**
     * <b>危险</b>：删除所有联系人
     */
    public void clearAllFriends() {
        getDb().delete(TABLE_CONTACT, "not type in (0,4,33)", null);
    }

    /**
     * 为指定Contact添加标签
     */
    public void attachLabelToContact(String contactId, String labelName) {
        Cursor cursor = getDb().rawQuery(format("select labelID from ContactLabel where labelName='%s'", labelName), null);
        if (cursor.moveToNext()) {
            //先获取到labelName所对应的labelId
            long labelId = cursor.getLong(0);
            cursor.close();
            Cursor cursor1 = getDb().rawQuery(format("select contactLabelIds from %s where username='%s'", TABLE_CONTACT, contactId), null);
            if (cursor1.moveToNext()) {
                //获取该contact已有的labelIds
                String labelIds = cursor1.getString(0);
                //如果labelIds不为null
                if (labelIds != null) {
                    String[] ids = labelIds.split(",");
                    boolean contains = false;
                    //判断该contact已经在labelName中
                    for (String id : ids) {
                        if (Objects.equals(id, String.valueOf(labelId))) {
                            contains = true;
                            break;
                        }
                    }
                    //如果不在
                    if (!contains) {
                        //在其原标签上增加labelName
                        labelIds += ids.length == 0 ? labelId : ("," + labelId);
                        //更新数据库上的contactLabelIds字段
                        getDb().rawExecSQL(format("update %s set contactLabelIds='%s' where username='%s'", TABLE_CONTACT, labelIds, contactId));
                    }
                }
            }
            cursor1.close();
        }
    }

    /**
     * 创建一个假的{@link Talker}
     *
     * @param talkerId Talker的ID
     * @param digest   消息摘要
     * @param latest   最近的一条消息
     */
    public void createConversationIfNotExists(String talkerId, String digest, Message latest) {
        if (!queryExistence(format("select username from %s where username='%s'", TABLE_CONVERSATION, talkerId))) {
            ContentValues values = new ContentValues();
            values.put("username", talkerId);
            values.put("msgCount", 1);
            values.put("chatmode", 1);
            values.put("unReadCount", 1);
            values.put("conversationTime", latest.getCreateTimeStamp());
            values.put("digest", digest);
            values.put("msgType", latest.getRawType());
            getDb().insert(TABLE_CONVERSATION, "digest", values);
        }
    }

    public void dropTable(String tableName) {
        getDb().execSQL("drop table " + tableName);
    }

    public void markAsRead(@NonNull Talker talker) {
        getDb().execSQL(format("update %s set unReadCount=0 where username='%s'", TABLE_CONVERSATION, talker.id));
    }

    public void markAsUnread(@NonNull Talker talker, int count) {
        getDb().execSQL(format("update %s set unReadCount=%s where username='%s'", TABLE_CONVERSATION, count, talker.id));
    }

    public void deleteConversationWithMessages(@NotNull Talker talker) {
        getDb().delete(TABLE_CONVERSATION, format("username='%s'", talker.id), null);
        getDb().delete(TABLE_MESSAGE, format("talker='%s'", talker.id), null);
        if (isMessageBackupTableExists()) {
            getDb().delete(TABLE_MESSAGE_BACKUP, format("talker='%s'", talker.id), null);
        }
    }

    public void reshowConversation(@NotNull Talker talker) {
        getDb().execSQL(format("update %s set parentRef=null where username='%s'", TABLE_CONVERSATION, talker.id));
    }

    public void hideConversation(@NotNull Talker talker) {
        getDb().execSQL(format("update %s set parentRef='%s' where username='%s'", TABLE_CONVERSATION, Talker.PARENT_REF_HIDDEN, talker.id));
    }

    public void beginTransactionUnless() {
        if (!getDb().inTransaction()) {
            getDb().beginTransaction();
        }
    }

    private void commit() {
        if (getDb().inTransaction()) {
            getDb().setTransactionSuccessful();
        }
    }

    public void rollback() {
        if (getDb().inTransaction()) {
            getDb().endTransaction();
        }
    }

    /**
     * 替换微信的原数据库文件
     * <p>
     * 这是个耗时操作，应当在工作线程中执行
     *
     * @throws ShellUtils.ShellException shell指令执行异常，包括文件删除失败，数据库拷贝失败
     */
    @WorkerThread
    public void apply() throws ShellUtils.ShellException {
        //先强行停止微信，否则可能导致数据库损坏
        ShellUtils.forceStop("com.tencent.mm");
        //提交所有更改
        commit();
        //替换微信的原数据库为修改过的数据库
        ShellUtils.cp(workerDatabasePath, originDatabasePath);
        //删除原数据库运行时文件
        //如不删除，微信会检测到数据库损坏，并执行数据库修复，修复数据可能导致数据丢失
        ShellUtils.rmIfExists(originDatabasePath + "-shm");
        ShellUtils.rmIfExists(originDatabasePath + "-wal");
        ShellUtils.rmIfExists(originDatabasePath + ".ini");
        ShellUtils.rmIfExists(originDatabasePath + ".sm");
    }
}
