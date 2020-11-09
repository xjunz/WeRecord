/*
 * Copyright (c) 2020 xjunz. 保留所有权利
 */
package xjunz.tool.wechat.impl;

import android.util.Log;
import android.util.SparseArray;

import androidx.annotation.CheckResult;
import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;
import androidx.databinding.ObservableBoolean;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;

import net.sqlcipher.Cursor;
import net.sqlcipher.database.SQLiteDatabase;

import org.jetbrains.annotations.NotNull;

import xjunz.tool.wechat.impl.model.message.BackupMessage;
import xjunz.tool.wechat.impl.model.message.Edition;
import xjunz.tool.wechat.impl.model.message.Message;
import xjunz.tool.wechat.impl.repo.MessageRepository;
import xjunz.tool.wechat.impl.repo.RepositoryFactory;
import xjunz.tool.wechat.util.ShellUtils;

import static xjunz.tool.wechat.impl.model.message.Edition.FLAG_DELETION;
import static xjunz.tool.wechat.impl.model.message.Edition.FLAG_INSERTION;
import static xjunz.tool.wechat.impl.model.message.Edition.FLAG_REPLACEMENT;

/**
 * 数据库修改器，实现了数据库修改的各个方法
 * <p>
 * 通过{@link Environment#modifyDatabase()}获取实例
 * </p>
 */
public class DatabaseModifier implements LifecycleObserver {
    private static DatabaseModifier sInstance;
    private static final String TABLE_MESSAGE = "message";
    /**
     * 备份修改后的原消息的表名，该表用于判断某条消息是否被修改以及用于消息的恢复。
     */
    public static final String TABLE_ORIGINAL_MESSAGE_BACKUP = "WeBackup";
    private final SQLiteDatabase db;
    private final String databaseBackupPath;
    private final String databaseOriginalPath;
    /**
     * 对于数据库的更改是否已同步到微信的数据库
     */
    private final ObservableBoolean mHasPendingChanges;


    private final SparseArray<Edition> mPendingEditions;

    private DatabaseModifier(@NotNull Environment environment) {
        this.db = environment.getDatabaseOfCurrentUser();
        this.databaseOriginalPath = environment.getCurrentUser().originalDatabaseFilePath;
        this.databaseBackupPath = environment.getCurrentUser().backupDatabaseFilePath;
        this.mHasPendingChanges = new ObservableBoolean(false);
        this.mPendingEditions = new SparseArray<>();
    }

    @CheckResult
    static DatabaseModifier getInstance(Environment environment) {
        if (sInstance == null) {
            synchronized (DatabaseModifier.class) {
                sInstance = new DatabaseModifier(environment);
                environment.getLifecycle().addObserver(sInstance);
            }
        }
        return sInstance;
    }

    public void putPendingEdition(Edition edition) {
        mPendingEditions.put(edition.getOrigin().getMsgId(), edition);
        mHasPendingChanges.set(true);
    }

    public void removeAllPendingEditions() {
        mPendingEditions.removeAtRange(0, mPendingEditions.size() - 1);
        mHasPendingChanges.set(false);
    }

    public SparseArray<Edition> getAllPendingEditions() {
        return mPendingEditions;
    }

    public void applyAllPendingEditions() throws ShellUtils.ShellException {
        for (int i = 0; i < mPendingEditions.size(); i++) {
            Edition edition = mPendingEditions.valueAt(i);
            switch (edition.getFlag()) {
                case FLAG_DELETION:
                    delete(edition.getReplacement());
                    break;
                case FLAG_INSERTION:
                    insert(edition.getReplacement());
                    break;
                case FLAG_REPLACEMENT:
                    replace(edition.getReplacement());
                    break;
            }
        }
        apply();
        removeAllPendingEditions();
    }

    /**
     * 删除备份表
     */
    public void dropBackupTable() {
        db.execSQL("drop table " + TABLE_ORIGINAL_MESSAGE_BACKUP);
    }

    /**
     * 创建消息备份表(如果不存在)，该表会复制{@code message}表的结构，但不会复制其内容。
     *
     * @see DatabaseModifier#TABLE_ORIGINAL_MESSAGE_BACKUP
     */
    public void createBackupTableIfNotExists() {
        //判断备份表是否存在
        Cursor cursor = db.rawQuery("select name from sqlite_master where type='table' and name='" + TABLE_ORIGINAL_MESSAGE_BACKUP + "'", null);
        //如果不存在
        if (cursor.getCount() == 0) {
            //以下的数据库操作不使用execSQL方法，而是使用
            //创建表
            db.execSQL("create table " + TABLE_ORIGINAL_MESSAGE_BACKUP + " as select * from message where 1<>1");
            //创建"edition"字段
            db.execSQL("alter table " + TABLE_ORIGINAL_MESSAGE_BACKUP + " add " + BackupMessage.KEY_EDITION + " int default 1");
            //为msgId创建唯一索引
            db.execSQL("create unique index index" + TABLE_ORIGINAL_MESSAGE_BACKUP + "MsgId on " + TABLE_ORIGINAL_MESSAGE_BACKUP + " (msgId)");
        }
        cursor.close();
    }


    /**
     * 备份某条消息
     *
     * @param msgId 指定消息ID
     */
    private void backupMessage(int msgId, int editionFlag) {
        createBackupTableIfNotExists();
        db.execSQL("insert or ignore into " + TABLE_ORIGINAL_MESSAGE_BACKUP + " select *," + editionFlag + " from message where msgId=" + msgId);
    }


    /**
     * 恢复某条消息
     *
     * @param msgId 指定消息ID
     */
    @CheckResult
    public DatabaseModifier restore(int msgId) {
        transactUnless();
        BackupMessage message = RepositoryFactory.get(MessageRepository.class).queryBackupMessageById(msgId);
        message.removeEditionFlag();
        //恢复记录
        db.replace(TABLE_MESSAGE, "content", message.getValues());
        //然后删除备份表中已恢复的记录
        db.execSQL("delete from " + TABLE_ORIGINAL_MESSAGE_BACKUP + " where msgId = " + msgId);
        return this;
    }

    /**
     * 恢复某条消息
     *
     * @param message 指定消息
     */
    @CheckResult
    public DatabaseModifier restore(@NotNull BackupMessage message) {
        transactUnless();
        message.removeEditionFlag();
        db.replace(TABLE_MESSAGE, "content", message.getValues());
        db.execSQL("delete from " + TABLE_ORIGINAL_MESSAGE_BACKUP + " where msgId = " + message.getMsgId());
        return this;
    }

    /**
     * @return 是否已经将当前数据库的更改同步到微信数据库
     */
    public ObservableBoolean hasPendingChanges() {
        return mHasPendingChanges;
    }

    /**
     * 向数据库中的message表中插入一条{@link Message}
     *
     * @param msg 欲插入的消息
     * @return 当前对象，便于链式调用
     */
    @CheckResult
    public DatabaseModifier insert(@NotNull Message msg) {
        transactUnless();
        backupMessage(msg.getMsgId(), FLAG_INSERTION);
        db.insert(TABLE_MESSAGE, "content", msg.getValues());
        return this;
    }

    @CheckResult
    public DatabaseModifier delete(@NonNull Message msg) {
        transactUnless();
        backupMessage(msg.getMsgId(), FLAG_DELETION);
        db.execSQL("delete from message where msgId=" + msg.getMsgId());
        return this;
    }


    /**
     * 替换数据库中的message表中的一条{@link Message}，
     * 以{@link Message#getMsgId()}为替换依据
     *
     * @param msg 用于替换的消息
     * @return 当前对象，便于链式调用
     */
    public DatabaseModifier replace(@NonNull Message msg) {
        transactUnless();
        backupMessage(msg.getMsgId(), FLAG_REPLACEMENT);
        db.replace(TABLE_MESSAGE, "content", msg.getValues());
        return this;
    }

    /**
     * 如果存在事务，确认数据库的所有更改，并替换微信的原数据库
     * <p>
     * 这是个耗时操作，应当在工作线程中执行
     *
     * @throws ShellUtils.ShellException shell指令执行异常，包括文件删除失败，数据库拷贝失败
     */
    @WorkerThread
    public void apply() throws ShellUtils.ShellException {
        if (db.inTransaction()) {
            throw new IllegalStateException("Changes are not confirmed, please call commit() first.");
        }
        Log.i("xjunz-", "========end transaction========");
        //替换微信的原数据库为修改过的数据库
        ShellUtils.cp(databaseBackupPath, databaseOriginalPath, "apply,1");
        //删除原数据库运行时文件
        //如不删除，微信会检测到数据库损坏，并执行数据库修复，修复数据可能导致数据丢失
        ShellUtils.rmIfExists(databaseOriginalPath + "-shm", "apply,2");
        ShellUtils.rmIfExists(databaseOriginalPath + "-wal", "apply,3");
        ShellUtils.rmIfExists(databaseOriginalPath + ".ini", "apply,4");
        ShellUtils.rmIfExists(databaseOriginalPath + ".sm", "apply,5");
    }

    public void commit() {
        if (db.inTransaction()) {
            db.setTransactionSuccessful();
            db.endTransaction();
        }
    }

    /**
     * 如果不存在事务，开启事务
     */
    private void transactUnless() {
        if (!db.inTransaction()) {
            Log.i("xjunz-", "========begin transaction========");
            db.beginTransaction();
        }
    }

    private void rollback() {
        while (db.inTransaction()) {
            db.endTransaction();
        }
        Log.i("xjunz-", "========end transaction========");
    }

    /**
     * 当{@link Environment}生命周期结束时，需要调用此方法将静态实例重置为null，
     * 否则该实例在仍会保留在应用的缓存之中（除非应用被强行停止）。当下次用户
     * 打开应用时，此实例会被继续使用，但是数据库实例已经关闭，从而导致查询数据库
     * 时闪退。
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    public void purge() {
        sInstance = null;
    }
}
