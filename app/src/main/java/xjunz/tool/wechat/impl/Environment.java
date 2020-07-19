/*
 * Copyright (c) 2020 xjunz. 保留所有权利
 */

package xjunz.tool.wechat.impl;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;

import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteDatabaseHook;

import org.apaches.commons.codec.DecoderException;
import org.apaches.commons.codec.binary.Hex;
import org.apaches.commons.codec.digest.DigestUtils;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import io.reactivex.Completable;
import io.reactivex.CompletableObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import xjunz.tool.wechat.App;
import xjunz.tool.wechat.impl.model.account.User;
import xjunz.tool.wechat.util.IOUtils;
import xjunz.tool.wechat.util.ShellUtils;
import xjunz.tool.wechat.util.UniUtils;

/**
 * An entity represents the functional environment of this application which defines all kinds of constants,
 * initializes databases and creates {@link User} instances.
 */
public class Environment implements SQLiteDatabaseHook, Serializable, LifecycleOwner {
    private LifecycleRegistry mLifecycle;
    private static Environment sEnvironment;
    private transient final String WECHAT_PACKAGE_NAME = "com.tencent.mm";
    private String mWechatSharedPrefsUinSetPath;
    private String mWechatSharedPrefsCurUinPath;
    private String mWechatImeiPath;
    private String mWechatMicroMsgPath;
    private transient final String separator = File.separator;
    public transient static final String DATABASE_EN_MICRO_MSG_NAME = "EnMicroMsg.db";
    private String mDatabaseBackupDirPath;
    private String mImei;
    private List<User> mUserList;
    private String mAvatarBackupPath;
    private String mCurrentUin;
    @Keep
    private String mBasicEnvironmentInfo;
    private String mAppFilesDir;
    private List<String> mUinList;
    private SQLiteDatabase mDatabaseOfCurUser;
    private User mCurrentUser;

    public SQLiteDatabase getDatabaseOfCurrentUser() {
        if (!getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED)) {
            throw new IllegalStateException("Environment is not initialized successfully! Please call init() first. ");
        }
        return mDatabaseOfCurUser;
    }

    public User getCurrentUser() {
        if (!getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED)) {
            throw new IllegalStateException("Environment is not initialized successfully! Please call init() first. ");
        }
        return mCurrentUser;
    }

    public String getWechatMicroMsgPath() {
        return mWechatMicroMsgPath;
    }

    public String getCurrentUin() {
        return mCurrentUin;
    }

    public String getAvatarBackupPath() {
        return mAvatarBackupPath;
    }

    @NonNull
    @Override
    public Lifecycle getLifecycle() {
        return mLifecycle;
    }


    /**
     * initiate the environment
     *
     * @param observer the callback
     */
    public void init(CompletableObserver observer) {
        Completable initTask = Completable.create(emitter -> {
            mBasicEnvironmentInfo = "<br/>[<br/>" + getBasicHardwareInfo() + "<br/>" + getVersionInfo();
            //create backup dirs
            mAppFilesDir = App.getContext().getFilesDir().getPath();
            mDatabaseBackupDirPath = mAppFilesDir + separator + DigestUtils.md5Hex("database_backup");
            File file = new File(mDatabaseBackupDirPath);
            if (!file.exists()) {
                if (!file.mkdir()) {
                    throw new RuntimeException("Failed to create db backup dir");
                }
            }
            mAvatarBackupPath = mAppFilesDir + separator + DigestUtils.md5Hex("avatar_backup");
            File file1 = new File(mAvatarBackupPath);
            if (!file1.exists()) {
                if (!file1.mkdir()) {
                    throw new RuntimeException("Failed to create avatar backup dir");
                }
            }
            PackageManager packageManager = App.getContext().getPackageManager();
            PackageInfo packageInfo = packageManager.getPackageInfo(WECHAT_PACKAGE_NAME, 0);
            mBasicEnvironmentInfo += "<br/><b>wechat_version_code</b>: " + packageInfo.versionCode + "<br/><b>wechat_version_name</b>: " + packageInfo.versionName + "<br/>]";
            String gWechatDataPath = packageInfo.applicationInfo.dataDir;
            mWechatMicroMsgPath = gWechatDataPath + separator + "MicroMsg";
            String sWechatSharedPrefsPath = gWechatDataPath + separator + "shared_prefs";
            mWechatSharedPrefsUinSetPath = sWechatSharedPrefsPath + separator + "app_brand_global_sp.xml";
            mWechatSharedPrefsCurUinPath = sWechatSharedPrefsPath + separator + "system_config_prefs.xml";
            mWechatImeiPath = mWechatMicroMsgPath + File.separator + "CompatibleInfo.cfg";
            initImei();
            initUins();
            initUsers();
            backupDatabaseOf(mCurrentUser);
            tryOpenDatabaseOf(mCurrentUser, mImei);
            fulfillUsers();
            mLifecycle.setCurrentState(Lifecycle.State.STARTED);
            emitter.onComplete();
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
        initTask.subscribe(observer);
    }

    private void initUsers() {
        mUserList = new ArrayList<>();
        for (String uin : mUinList) {
            User user = new User(uin);
            if (uin.equals(mCurrentUin)) {
                user.isLastLogin = true;
                mCurrentUser = user;
            }
            mUserList.add(user);
        }
    }

    private void fulfillUsers() {
        for (User user : mUserList) {
            Cursor cursor = mDatabaseOfCurUser.rawQuery("select id,value from userinfo where id in(2,4,42) ", null);
            while (cursor.moveToNext()) {
                int id = cursor.getInt(0);
                String value = cursor.getString(1);
                switch (id) {
                    case 2:
                        user.id = value;
                        break;
                    case 4:
                        user.nickname = value;
                        break;
                    case 42:
                        user.alias = value;
                        break;
                }
            }
            cursor.close();
        }
    }


    public boolean initialized() {
        return mLifecycle.getCurrentState().isAtLeast(Lifecycle.State.STARTED);
    }

    private Environment() {
        mLifecycle = new LifecycleRegistry(this);
        mLifecycle.setCurrentState(Lifecycle.State.CREATED);
    }

    public static Environment getInstance() {
        sEnvironment = sEnvironment == null ? new Environment() : sEnvironment;
        return sEnvironment;
    }


    public String getIMEI() {
        return mImei;
    }

    public List<User> getUserList() {
        return mUserList;
    }


    /*private void grantDatabaseAccessibility(User user) {
        int myUid = Process.myUid();
        //改变数据库文件所属用户组
        Shell.SU.run("chgrp " + myUid + " " + user.originalDatabaseFilePath);
        //允许用户组读写
        Shell.SU.run("chmod " + "660 " + user.originalDatabaseFilePath);
    }*/

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void backupDatabaseOf(@NotNull User user) throws ShellUtils.ShellException {
        user.backupDatabaseFilePath = mDatabaseBackupDirPath + File.separator + DigestUtils.md5Hex(user.uin);
        File backup = new File(user.backupDatabaseFilePath);
        try {
            backup.createNewFile();
            ShellUtils.cp(user.originalDatabaseFilePath, user.backupDatabaseFilePath, "backupDatabaseOf");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void initUins() throws ShellUtils.ShellException {
        String out = ShellUtils.cat(mWechatSharedPrefsUinSetPath, "Failed to read " + mWechatSharedPrefsUinSetPath);
        mUinList = UniUtils.extract(out, ">(\\d+)<");
        if (mUinList == null) {
            throw new RuntimeException("No uin found");
        }
        out = ShellUtils.cat(mWechatSharedPrefsCurUinPath, "Failed to read " + mWechatSharedPrefsCurUinPath);
        mCurrentUin = UniUtils.extractFirst(out, "default_uin\" value=\"(\\d+)");
        if (mCurrentUin == null) {
            throw new RuntimeException("No last login uin found");
        }
    }


    @SuppressWarnings("unchecked")
    private void initImei() {
        //fetch from SP first
        mImei = App.getSharedPrefsManager().getIMEI();
        if (mImei != null) {
            return;
        }
        //otherwise get from file
        String temp = mAppFilesDir + separator + "temp.cfg";
        try {
            ShellUtils.cp(mWechatImeiPath, temp, "Failed to backup CompatibleInfo.cfg");
        } catch (ShellUtils.ShellException e) {
            e.printStackTrace();
        }
        File tempFile = new File(temp);
        tempFile.deleteOnExit();
        try {
            FileInputStream fis = new FileInputStream(tempFile);
            ObjectInputStream ois = new ObjectInputStream(fis);
            HashMap<Integer, String> map = (HashMap<Integer, String>) ois.readObject();
            //258
            mImei = map.get(258);
            fis.close();
            ois.close();
            if (mImei == null) {
                throw new RuntimeException("Got null IMEI");
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }


    private void tryOpenDatabaseOf(@NonNull User user, @NonNull String imei) {
        user.databasePragmaKey = DigestUtils.md5Hex(imei + user.uin).substring(0, 7).toLowerCase();
        mDatabaseOfCurUser = SQLiteDatabase.openDatabase(user.backupDatabaseFilePath, user.databasePragmaKey, null, SQLiteDatabase.OPEN_READONLY, this);
    }

    public DatabaseModifier modifyDatabase() {
        return DatabaseModifier.getInstance(mDatabaseOfCurUser, mCurrentUser);
    }

    public String serialize() {
        ByteArrayInputStream in = new ByteArrayInputStream(toString().getBytes());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ZipOutputStream outputStream = new ZipOutputStream(out);
        try {
            outputStream.setLevel(9);
            outputStream.putNextEntry(new ZipEntry(""));
            IOUtils.transferStream(in, outputStream);
            return Hex.encodeHexString(out.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String deserialize(String serial) {
        try {
            ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(Hex.decodeHex(serial)));
            in.getNextEntry();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            IOUtils.transferStream(in, out);
            return out.toString();
        } catch (IOException | DecoderException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void purge() {
        if (mDatabaseOfCurUser != null) {
            mDatabaseOfCurUser.close();
        }
        if (mCurrentUser != null) {
            mCurrentUser.deleteBackupDatabase();
        }
        mLifecycle.setCurrentState(LifecycleRegistry.State.DESTROYED);
        sEnvironment = null;
    }


    private static String getBasicHardwareInfo() {
        return "<b>release</b>: " + Build.VERSION.RELEASE + "<br/>" +
                "<b>SDK</b>: " + Build.VERSION.SDK_INT + "<br/>" +
                "<b>brand</b>: " + Build.BRAND + "<br/>" +
                "<b>model</b>: " + Build.MODEL + "<br/>" +
                "<b>CPU_ABI</b>: " + Arrays.toString(Build.SUPPORTED_ABIS);
    }

    @NonNull
    @Contract(pure = true)
    private static String getVersionInfo() {
        return "<b>version_name</b>: " + App.VERSION_NAME + "<br/>" +
                "<b>version_code</b>: " + App.VERSION_CODE;
    }


    @NonNull
    @Override
    public String toString() {
        StringBuilder output = new StringBuilder();
        Field[] fields = getClass().getDeclaredFields();
        try {
            for (Field field : fields) {
                if (!Modifier.isTransient(field.getModifiers())) {
                    if (field.getGenericType() == String.class) {
                        output.append("<b>").append(field.getName()).append("</b>").append(": ").append(field.get(this)).append("<br/>");
                    } else if (field.getGenericType() == User.class) {
                        User user = (User) field.get(this);
                        output.append("<b>").append(field.getName()).append("</b>").append(": <br/>[<br/>").append(user == null ? "null" : user.toString()).append("<br/>]<br/>");
                    }
                }
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return output.toString();
    }

    @Override
    public void preKey(SQLiteDatabase database) {

    }

    @Override
    public void postKey(SQLiteDatabase database) {
        database.rawExecSQL("PRAGMA cipher_migrate;");
    }
}
