/*
 * Copyright (c) 2020 xjunz. 保留所有权利
 */

package xjunz.tool.wechat.impl;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.os.Process;
import android.text.TextUtils;

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
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

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
 * An entity represents the fundamental functional environment of this application, which defines all kinds of constants,
 * initializes databases and creates {@link User} instances.
 * <p>
 * This entity is designed in single-instance mode. Please call {@link Environment#getInstance()} to obtain the instance.
 * </p>
 */
public class Environment implements SQLiteDatabaseHook, Serializable, LifecycleOwner {
    private final LifecycleRegistry mLifecycle;
    private static Environment sEnvironment;
    private transient final String DEF_IMEI = "1234567890ABCDEF";
    private transient String mWechatDataPath;
    private transient String mWechatSharedPrefsPath;
    private transient String mWechatMicroMsgPath;
    private transient final String separator = File.separator;
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
        if (!initialized()) {
            throw new IllegalStateException("Environment is not initialized successfully!");
        }
        return mDatabaseOfCurUser;
    }

    public User getCurrentUser() {
        if (!initialized()) {
            throw new IllegalStateException("Environment is not initialized successfully!");
        }
        return mCurrentUser;
    }

    public String getWechatMicroMsgPath() {
        return mWechatMicroMsgPath;
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
            PackageInfo packageInfo = packageManager.getPackageInfo("com.tencent.mm", 0);
            mBasicEnvironmentInfo += "<br/><b>wechat_version_code</b>: " + packageInfo.versionCode + "<br/><b>wechat_version_name</b>: " + packageInfo.versionName + "<br/>]";
            mWechatDataPath = packageInfo.applicationInfo.dataDir;
            mWechatMicroMsgPath = mWechatDataPath + separator + "MicroMsg";
            mWechatSharedPrefsPath = mWechatDataPath + separator + "shared_prefs";
            mImei = readImei();
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
        return sEnvironment = (sEnvironment == null ? new Environment() : sEnvironment);
    }


    private void backupDatabaseOf(@NotNull User user) throws IOException, ShellUtils.ShellException {
        user.backupDatabaseFilePath = mDatabaseBackupDirPath + File.separator + DigestUtils.md5Hex(user.uin);
        ShellUtils.cp2data(user.originalDatabaseFilePath, user.backupDatabaseFilePath, true, "backupDatabaseOf");
    }


    private void initUins() throws ShellUtils.ShellException {
        String out = ShellUtils.cat(mWechatSharedPrefsPath + separator + "app_brand_global_sp.xml", "initUins,1");
        // /data/user/0/com.tencent.mm/shared_prefs/app_brand_global_sp.xml
        mUinList = UniUtils.extract(out, ">(\\d+)<");
        if (mUinList.size() == 0) {
            throw new RuntimeException("No uin set found");
        }
        out = ShellUtils.cat(mWechatSharedPrefsPath + separator + "com.tencent.mm_preferences.xml", "initUins,2");
        mCurrentUin = UniUtils.extractFirst(out, "last_login_uin\">(\\d+)<");
        //out = ShellUtils.cat(mWechatSharedPrefsPath + separator + "system_config_prefs.xml", "initUins,2");
        if (TextUtils.isEmpty(mCurrentUin)) {
            throw new RuntimeException("No last login uin found");
        }
    }

    @SuppressWarnings("unchecked")
    @Deprecated
    private void initImei() {
        //fetch from SP first
        mImei = App.getSharedPrefsManager().getIMEI();
        if (mImei != null) {
            return;
        }
        //otherwise get from file
        String temp = mAppFilesDir + separator + "temp.cfg";
        File tempFile = new File(temp);
        try {
            ShellUtils.cp2data(mWechatMicroMsgPath + File.separator + "CompatibleInfo.cfg", temp, false, "initImei");
            FileInputStream fis = new FileInputStream(tempFile);
            ObjectInputStream ois = new ObjectInputStream(fis);
            HashMap<Integer, String> map = (HashMap<Integer, String>) ois.readObject();
            //258
            mImei = map.get(258);
            fis.close();
            ois.close();
        } catch (IOException | ShellUtils.ShellException | ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            //noinspection ResultOfMethodCallIgnored
            tempFile.delete();
        }

        if (mImei == null) {
            mImei = DEF_IMEI;
        }
    }

    private String readImei() throws NoSuchPaddingException, NoSuchAlgorithmException, IOException, ShellUtils.ShellException, InvalidKeyException {
        String keyInfoPath = mWechatDataPath + separator + "files" + separator + "KeyInfo.bin";
        SecretKeySpec secretKeySpec = new SecretKeySpec("_wEcHAT_".getBytes(), "RC4");
        Cipher cipher = Cipher.getInstance("RC4");
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
        String temp = mAppFilesDir + separator + "temp.bin";
        File tempFile = new File(temp);
        ShellUtils.cp2data(keyInfoPath, temp, false, "readImei");
        BufferedReader reader = new BufferedReader(new InputStreamReader(new CipherInputStream(new FileInputStream(temp), cipher)));
        String key = reader.readLine();
        reader.close();
        //noinspection ResultOfMethodCallIgnored
        tempFile.delete();
        return key;
    }

    private void tryOpenDatabaseOf(@NonNull User user, @NonNull String imei) {
        String possibleKey = DigestUtils.md5Hex(imei + user.uin).substring(0, 7).toLowerCase();
        mDatabaseOfCurUser = SQLiteDatabase.openDatabase(user.backupDatabaseFilePath, possibleKey, null, SQLiteDatabase.OPEN_READWRITE, this);
        user.databasePragmaKey = possibleKey;
        //UniUtils.copyPlainText("key",possibleKey);
    }

    public DatabaseModifier modifyDatabase() {
        return DatabaseModifier.getInstance(this);
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

    @Nullable
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


    @NotNull
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
                "<b>version_code</b>: " + App.VERSION_CODE + "<br/>" + "<b>pid:</>" + Process.myPid()
                + "<br/>" + "<b>uid:</b>" + Process.myUid();
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
