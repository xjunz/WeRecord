/*
 * Copyright (c) 2020 xjunz. 保留所有权利
 */
package xjunz.tool.wechat.impl;

import android.content.ContentValues;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import net.sqlcipher.Cursor;
import net.sqlcipher.database.SQLiteDatabase;

import org.apaches.commons.codec.digest.DigestUtils;
import org.jetbrains.annotations.NotNull;

import xjunz.tool.wechat.impl.model.account.User;
import xjunz.tool.wechat.impl.model.message.Message;
import xjunz.tool.wechat.util.ShellUtils;

/**
 * 数据库修改器，实现了数据库修改的各个方法
 * <p>
 * 请通过{@link Environment#modifyDatabase()}获取实例
 * </p>
 */
public class DatabaseModifier {
    private static DatabaseModifier sInstance;
    private static final String TABLE_MESSAGE = "message";
    /**
     * 备份修改后的原消息的表名，该表用于判断某条消息是否被修改以及用于消息的恢复。
     * 我们采用{@code md5}摘要，为了规避今后可能的撞表以及微信的检测。
     */
    private String TABLE_ORIGINAL_MESSAGE_BACKUP;
    private SQLiteDatabase db;
    private String databaseBackupPath;
    private String databaseOriginalPath;

    static DatabaseModifier getInstance(SQLiteDatabase database, User user) {
        if (sInstance == null) {
            synchronized (DatabaseModifier.class) {
                if (sInstance == null) {
                    sInstance = new DatabaseModifier();
                    sInstance.db = database;
                    sInstance.databaseOriginalPath = user.originalDatabaseFilePath;
                    sInstance.databaseBackupPath = user.backupDatabaseFilePath;
                    //w-b= we-backup
                    sInstance.TABLE_ORIGINAL_MESSAGE_BACKUP = DigestUtils.md5Hex("w-b" + user.uin).substring(0, 8);
                    sInstance.createBackupTable();
                }
            }
        }
        return sInstance;
    }


    /**
     * 创建消息备份表，该表会复制{@code message}表的结构
     *
     * @see DatabaseModifier#TABLE_ORIGINAL_MESSAGE_BACKUP
     */
    private void createBackupTable() {
        //判断备份表是否存在
        Cursor cursor = db.rawQuery("select name from sqlite_master where type='table' and name='" + TABLE_ORIGINAL_MESSAGE_BACKUP + "'", null);
        //如果不存在
        if (cursor.getCount() == 0) {
            //创建表
            db.execSQL("create table " + TABLE_ORIGINAL_MESSAGE_BACKUP + " as select * from message where 1<>1");
            //以msgId为标的创建唯一索引
            db.execSQL("create unique index indexMsgId on " + TABLE_ORIGINAL_MESSAGE_BACKUP + "(msgId)");
            cursor.close();
        }
    }


    /**
     * 备份某条消息
     *
     * @param msgId 指定消息ID
     */
    private void backupMessage(int msgId) {
        db.execSQL("replace into " + TABLE_ORIGINAL_MESSAGE_BACKUP + " select * from message where msgId=" + msgId);
    }

    /**
     * 恢复某条消息
     *
     * @param msgId 指定消息ID
     */
    private void restoreMessage(int msgId) {
        //先从备份表恢复
        db.execSQL("replace into message select * from " + TABLE_ORIGINAL_MESSAGE_BACKUP + " where msgId=" + msgId);
        //然后备份表中已恢复的记录
        db.execSQL("delete from " + TABLE_ORIGINAL_MESSAGE_BACKUP + " where msgId = " + msgId);
    }

    /**
     * 将{@link Message}转换为{@link ContentValues}
     * <p>
     * 返回的{@link ContentValues}会用于数据库的增删改查，
     * 因为我们需要混淆源码，因此不建议采用反射的方法实现
     * </p>
     *
     * @param message 欲转换的{@link Message}
     * @return 转换后的 {@link ContentValues}
     */
    @NotNull
    private ContentValues buildContentValuesFromMessage(@NonNull Message message) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("type", message.getRawType());
        contentValues.put("status", message.getStatus());
        contentValues.put("isSend", message.isSend() ? 1 : 0);
        contentValues.put("createTime", message.getCreateTimeStamp());
        contentValues.put("talker", message.getTalkerId());
        contentValues.put("content", message.getRawContent());
        contentValues.put("imgPath", message.getImgPath());
        contentValues.put("bizChatId", -1);
        return contentValues;
    }

    /**
     * 向数据库中的message表中插入一条{@link Message}
     *
     * @param msg 欲插入的数据
     * @return 当前对象，便于链式调用
     */
    public DatabaseModifier insert(@NotNull Message msg) {
        backupMessage(msg.getMsgId());
        db.beginTransaction();
        db.insert(TABLE_MESSAGE, "NULL", buildContentValuesFromMessage(msg));
        return this;
    }

    ;

    /**
     * 确认数据库的所有更改，并替换微信的原数据库
     * <p>
     * 这是个耗时操作，应当在工作线程中执行
     *
     * @throws ShellUtils.ShellException shell指令执行异常
     */
    @WorkerThread
    public void apply() throws ShellUtils.ShellException {
        //确认所有更改
        while (db.inTransaction()) {
            db.setTransactionSuccessful();
            db.endTransaction();
        }
        //替换微信的原数据库为修改过的数据库
        ShellUtils.cp(databaseBackupPath, databaseOriginalPath, "apply,1");
        //删除原数据库运行时文件
        //如不删除，微信会检测到数据库损坏，并执行数据库修复，修复数据可能导致数据丢失
        ShellUtils.rmIfExists(databaseOriginalPath + "-shm", "apply,2");
        ShellUtils.rmIfExists(databaseOriginalPath + "-wal", "apply,3");
        ShellUtils.rmIfExists(databaseOriginalPath + ".ini", "apply,4");
        ShellUtils.rmIfExists(databaseOriginalPath + ".sm", "apply,5");
    }
}
