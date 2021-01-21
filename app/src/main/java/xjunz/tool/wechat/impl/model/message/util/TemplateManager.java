/*
 * Copyright (c) 2021 xjunz. 保留所有权利
 */
package xjunz.tool.wechat.impl.model.message.util;

import android.content.Context;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;

import net.sqlcipher.Cursor;
import net.sqlcipher.database.SQLiteDatabase;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import xjunz.tool.wechat.impl.Environment;
import xjunz.tool.wechat.util.DbUtils;
import xjunz.tool.wechat.util.IOUtils;

/**
 * 模板管理器，管理所有内置模板和自定义模板。
 *
 * @author xjunz 2020/12/30
 * @see Template
 */
public class TemplateManager implements LifecycleObserver {
    public static final String TEMPLATE_DB_NAME = "fed36e93a0509e20f2dc96cbbd85b678"; //DigestUtils.md5Hex("templates");
    public static final String TEMPLATE_DB_PWD = "66f6181";//DigestUtils.md5Hex("template").substring(0,7)
    public static final String TEMPLATE_TABLE_NAME = "template";
    /**
     * message表的创建sql为{@code msgId INTEGER, msgSvrId INTEGER , type INT, status INT, isSend INT, isShowTimer INTEGER, createTime INTEGER, talker TEXT,
     * content TEXT, imgPath TEXT, reserved TEXT, lvbuffer BLOB, transContent TEXT,transBrandWording TEXT ,talkerId INTEGER, bizClientMsgId TEXT,
     * bizChatId INTEGER DEFAULT -1, bizChatUserId TEXT, msgSeq INTEGER, flag INT, solitaireFoldInfo BLOB, historyId TEXT}。
     */
    public static final String SQL_CREATE_TABLE = String.format("CREATE TABLE IF NOT EXISTS %s(%s TEXT, %s INT, %s INTEGER PRIMARY KEY, msgId INTEGER, msgSvrId INTEGER , type INT, status INT, isSend INT, isShowTimer INTEGER, createTime INTEGER, talker TEXT," +
            "content TEXT, imgPath TEXT, reserved TEXT, lvbuffer BLOB, transContent TEXT,transBrandWording TEXT ,talkerId INTEGER, bizClientMsgId TEXT," +
            "bizChatId INTEGER DEFAULT -1, bizChatUserId TEXT, msgSeq INTEGER, flag INT, solitaireFoldInfo BLOB, historyId TEXT)", TEMPLATE_TABLE_NAME, Template.KEY_NAME, Template.KEY_IS_CUSTOM, Template.KEY_ID);
    private final SQLiteDatabase mDb;
    private final List<Template> mBuildInTemplates = new ArrayList<>();
    private final List<Template> mCustomTemplates = new ArrayList<>();
    private static TemplateManager sInstance;

    private TemplateManager(SQLiteDatabase db) {
        mDb = db;
        Environment.getInstance().getLifecycle().addObserver(this);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void init(@NotNull Context context) {
        File dbFile = context.getDatabasePath(TEMPLATE_DB_NAME);
        if (!dbFile.exists()) {
            if (dbFile.mkdirs()) {
                try {
                    dbFile.delete();
                    dbFile.createNewFile();
                    IOUtils.transferStream(context.getAssets().open("templates"), new FileOutputStream(dbFile));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(dbFile, TEMPLATE_DB_PWD, null);
        sInstance = new TemplateManager(db);
        sInstance.loadAllLocalTemplates();
    }

    /**
     * 从数据库中加载所有模板
     */
    private void loadAllLocalTemplates() {
        Cursor cursor = mDb.rawQuery("select * from " + TEMPLATE_TABLE_NAME, null);
        while (cursor.moveToNext()) {
            Template template = Template.fromLocal(DbUtils.buildValuesFromCursor(cursor));
            //LogUtils.debug(template.getSource());
            if (template.isCustom()) {
                mCustomTemplates.add(template);
            } else {
                mBuildInTemplates.add(template);
            }
        }
        cursor.close();
    }

    public boolean isLocalNameExists(String name) {
        for (Template template : mBuildInTemplates) {
            if (Objects.equals(template.getName(), name)) {
                return true;
            }
        }
        return false;
    }

    public boolean isCustomTemplateExists(Template template) {
        return mCustomTemplates.contains(template);
    }

    public boolean isCustomNameExists(String name) {
        for (Template template : mCustomTemplates) {
            if (Objects.equals(template.getName(), name)) {
                return true;
            }
        }
        return false;
    }

    public static TemplateManager getInstance() {
        if (sInstance == null) {
            throw new IllegalStateException("Please call init() first! ");
        }
        return sInstance;
    }

    public List<Template> getBuildInTemplates() {
        return mBuildInTemplates;
    }

    public List<Template> getCustomTemplates() {
        return mCustomTemplates;
    }

    public void replaceCustomTemplate(@NotNull Template template) {
        mDb.replace(TEMPLATE_TABLE_NAME, "name", template.getValues());
    }

    public void addCustomTemplate(Template template) {
        mCustomTemplates.add(template);
        template.getValues().put(Template.KEY_IS_CUSTOM, true);
        long id = mDb.replace(TEMPLATE_TABLE_NAME, "name", template.getValues());
        template.getValues().put(Template.KEY_ID, id);
    }

    public void convertCustomToBuildIn(@NotNull Template template) {
        //先从自定义模板中移除
        mCustomTemplates.remove(template);
        //然后新增到内置模板
        mBuildInTemplates.add(template);
        //更新到数据库
        template.getValues().put(Template.KEY_IS_CUSTOM, false);
        long id = mDb.replace(TEMPLATE_TABLE_NAME, "name", template.getValues());
        template.getValues().put(Template.KEY_ID, id);
    }

    public void removeCustomTemplate(Template template) {
        mCustomTemplates.remove(template);
        mDb.delete(TEMPLATE_TABLE_NAME, "id=" + template.getId(), null);
    }

    public void removeBuildInTemplate(Template template) {
        mBuildInTemplates.remove(template);
        mDb.delete(TEMPLATE_TABLE_NAME, "id=" + template.getId(), null);
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    public void purge() {
        if (mDb != null && mDb.isOpen()) {
            mDb.close();
        }
    }
}
