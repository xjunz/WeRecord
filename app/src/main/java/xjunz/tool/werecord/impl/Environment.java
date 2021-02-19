/*
 * Copyright (c) 2021 xjunz. 保留所有权利
 */

package xjunz.tool.werecord.impl;

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

import org.apaches.commons.codec.binary.Base64;
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
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import io.reactivex.CompletableObserver;
import xjunz.tool.werecord.App;
import xjunz.tool.werecord.BuildConfig;
import xjunz.tool.werecord.impl.model.account.User;
import xjunz.tool.werecord.util.IoUtils;
import xjunz.tool.werecord.util.RxJavaUtils;
import xjunz.tool.werecord.util.ShellUtils;
import xjunz.tool.werecord.util.Utils;

/**
 * An entity represents the fundamental functional environment of this application, which defines all kinds of constants,
 * initializes databases and creates {@link User} instances.
 * <p>
 * This entity is designed in single-instance mode. Please call {@link Environment#getInstance()} to obtain the instance.
 * Don't expect the instance returned is non-null, because the system would recycle this instance anytime when this app is
 * running in the background after home key is pressed.
 * <p>
 * In normal case, this instance's lifecycle is across the lifecycle of the application from {@link Environment#init(CompletableObserver)} is finished util
 * the user manually exits the app.
 */
public class Environment implements Serializable, LifecycleOwner {
    private final LifecycleRegistry mLifecycle;
    private static Environment sEnvironment;
    private transient String mWechatDataPath;
    private transient String mWechatSharedPrefsPath;
    private transient String mWechatMicroMsgPath;
    private transient final String separator = File.separator;
    private String mDatabaseBackupDirPath;
    private String mImei;
    private List<User> mUserList;
    private String mAvatarBackupPath;
    private String mLastUsedUin;
    private String mLastLoginUin;
    @Keep
    private String mBasicEnvironmentInfo;
    private String mAppFilesDir;
    private List<String> mUinList;
    private SQLiteDatabase mDatabaseOfCurUser;
    private User mCurrentUser;
    private DatabaseModifier mModifier;

    /**
     * @return 当前工作数据库
     */
    @NonNull
    public SQLiteDatabase getWorkerDatabase() {
        if (!initialized()) {
            throw new IllegalStateException("Environment is not initialized successfully! Current state is " + getLifecycle().getCurrentState().toString());
        }
        return mDatabaseOfCurUser;
    }

    @NonNull
    public User getCurrentUser() {
        if (!initialized()) {
            throw new IllegalStateException("Environment is not initialized successfully! Current state is " + getLifecycle().getCurrentState().toString());
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
        RxJavaUtils.complete(() -> {
            mLastUsedUin = App.config().lastUsedUin.getValue();
            mBasicEnvironmentInfo = "<br/>[<br/>" + getBasicHardwareInfo() + "<br/>" + getVersionInfo();
            //create backup dirs
            mAppFilesDir = App.getContext().getFilesDir().getPath();
            mDatabaseBackupDirPath = mAppFilesDir + separator + DigestUtils.md5Hex("database_backup");
            File dbBackupDir = new File(mDatabaseBackupDirPath);
            if (!dbBackupDir.exists() && !dbBackupDir.mkdir())
                throw new RuntimeException("Failed to create db backup dir");
            mAvatarBackupPath = mAppFilesDir + separator + DigestUtils.md5Hex("avatar_backup");
            File avatarBackupDir = new File(mAvatarBackupPath);
            if (!avatarBackupDir.exists() && !avatarBackupDir.mkdir())
                throw new RuntimeException("Failed to create avatar backup dir");
            PackageManager packageManager = App.getContext().getPackageManager();
            PackageInfo packageInfo = packageManager.getPackageInfo("com.tencent.mm", 0);
            mBasicEnvironmentInfo += "<br/><b>wechat_version_code</b>: " + packageInfo.versionCode + "<br/><b>wechat_version_name</b>: " + packageInfo.versionName + "<br/>]";
            mWechatDataPath = packageInfo.applicationInfo.dataDir;
            mWechatMicroMsgPath = mWechatDataPath + separator + "MicroMsg";
            mWechatSharedPrefsPath = mWechatDataPath + separator + "shared_prefs";
            readImei();
            loadUins();
            initUsers();
            copyWorkerDatabase(mCurrentUser);
            tryOpenDatabaseOf(mCurrentUser, mImei);
            fulfillCurrentUser();
            loadUserIds();
            mLifecycle.setCurrentState(Lifecycle.State.STARTED);
        }).subscribe(observer);
    }

    public List<User> getUserList() {
        return mUserList;
    }

    private void initUsers() {
        mUserList = new ArrayList<>();
        for (String uin : mUinList) {
            User user = new User(uin);
            if (Objects.equals(mLastLoginUin, uin)) {
                user.isLastLogin = true;
                if (mLastUsedUin == null) {
                    mCurrentUser = user;
                    mCurrentUser.isCurrentUsed = true;
                    App.config().lastUsedUin.setValue(uin);
                }
            }
            if (mLastUsedUin != null) {
                if (Objects.equals(mLastUsedUin, uin)) {
                    mCurrentUser = user;
                    mCurrentUser.isCurrentUsed = true;
                    App.config().lastUsedUin.setValue(uin);
                }
            }
            mUserList.add(user);
        }
    }

    private void fulfillCurrentUser() {
        try (Cursor cursor = mDatabaseOfCurUser.rawQuery("select id,value from userinfo where id in(2,4,6,42) ", null)) {
            while (cursor.moveToNext()) {
                int id = cursor.getInt(0);
                String value = cursor.getString(1);
                switch (id) {
                    case 2:
                        mCurrentUser.id = value;
                        break;
                    case 4:
                        mCurrentUser.nickname = value;
                        break;
                    case 6:
                        mCurrentUser.phoneNum = value;
                        break;
                    case 42:
                        mCurrentUser.alias = value;
                        break;
                }
            }
        }
    }

    public boolean initialized() {
        return mLifecycle.getCurrentState().isAtLeast(Lifecycle.State.STARTED);
    }

    private Environment() {
        mLifecycle = new LifecycleRegistry(this);
        mLifecycle.setCurrentState(Lifecycle.State.CREATED);
    }

    public static Environment newInstance() {
        sEnvironment = new Environment();
        return sEnvironment;
    }

    public static Environment getInstance() {
        return sEnvironment;
    }

    private void copyWorkerDatabase(@NotNull User user) throws IOException, ShellUtils.ShellException {
        user.workerDatabaseFilePath = mDatabaseBackupDirPath + File.separator + DigestUtils.md5Hex(user.uin);
        ShellUtils.cp2data(user.originalDatabaseFilePath, user.workerDatabaseFilePath, true);
    }

    /**
     * 备份原数据库
     */
    public void backupOriginDatabaseOf(@NotNull User user) throws IOException, ShellUtils.ShellException {
        user.backupDatabaseFilePath = mDatabaseBackupDirPath + File.separator + DigestUtils.md5Hex("backup");
        ShellUtils.cp2data(user.originalDatabaseFilePath, user.backupDatabaseFilePath, true);
    }


    private void loadUins() throws ShellUtils.ShellException {
        String out = ShellUtils.cat(mWechatSharedPrefsPath + separator + "app_brand_global_sp.xml");
        // /data/user/0/com.tencent.mm/shared_prefs/app_brand_global_sp.xml
        mUinList = Utils.extract(out, ">(\\d+)<");
        if (mUinList.isEmpty()) {
            throw new RuntimeException("No uin set found");
        }
        out = ShellUtils.cat(mWechatSharedPrefsPath + separator + "com.tencent.mm_preferences.xml");
        mLastLoginUin = Utils.extractFirst(out, "last_login_uin\">(\\d+)<");
        //out = ShellUtils.cat(mWechatSharedPrefsPath + separator + "system_config_prefs.xml", "initUins,2");
        if (TextUtils.isEmpty(mLastLoginUin)) {
            throw new RuntimeException("No last login uin found");
        }
    }

    private void loadUserIds() throws ShellUtils.ShellException {
        String out = ShellUtils.cat(mWechatSharedPrefsPath + separator + "com.tencent.mm_preferences_account_switch.xml");
        // /data/user/0/com.tencent.mm/shared_prefs/com.tencent.mm_preferences_account_switch.xml
        List<String> ids = Utils.extract(out, "string>(.+?)<");
        if (ids.size() == mUinList.size()) {
            for (int i = 0; i < ids.size(); i++) {
                User user = mUserList.get(i);
                if (user.id == null) {
                    user.id = ids.get(i);
                }
            }
        }
    }

    /**
     * Use {@link Environment#readImei()} instead.
     */
    @SuppressWarnings("unchecked")
    @Deprecated
    private void initImei() {
        //fetch from SP first
        mImei = App.getSharedPrefsManager().getImei();
        if (mImei != null) {
            return;
        }
        //otherwise get from file
        String temp = mAppFilesDir + separator + "temp.cfg";
        File tempFile = new File(temp);
        try {
            ShellUtils.cp2data(mWechatMicroMsgPath + File.separator + "CompatibleInfo.cfg", temp, false);
            FileInputStream fis = new FileInputStream(tempFile);
            ObjectInputStream ois = new ObjectInputStream(fis);
            HashMap<Integer, String> map = (HashMap<Integer, String>) ois.readObject();
            //258
            mImei = map.get(0x102);
            fis.close();
            ois.close();
        } catch (IOException | ShellUtils.ShellException | ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            //noinspection ResultOfMethodCallIgnored
            tempFile.delete();
        }

        if (mImei == null) {
            //def imei
            mImei = "1234567890ABCDEF";
        }
    }

    private void readImei() throws NoSuchPaddingException, NoSuchAlgorithmException, IOException, ShellUtils.ShellException, InvalidKeyException {
        App.SharedPrefsManager spm = App.getSharedPrefsManager();
        //先从缓存读取
        if (spm.isAppIntroDone() && (mImei = spm.getImei()) != null) {
            return;
        }
        //否则从文件读取
        String keyInfoPath = mWechatDataPath + separator + "files" + separator + "KeyInfo.bin";
        SecretKeySpec secretKeySpec = new SecretKeySpec(/*"_wEcHAT_"*/new byte[]{95, 119, 69, 99, 72, 65, 84, 95}, "RC4");
        Cipher cipher = Cipher.getInstance("RC4");
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
        File tempFile = File.createTempFile("KeyInfo", ".bin");
        ShellUtils.cp(keyInfoPath, tempFile.getPath());
        BufferedReader reader = new BufferedReader(new InputStreamReader(new CipherInputStream(new FileInputStream(tempFile), cipher)));
        mImei = reader.readLine();
        reader.close();
        IoUtils.deleteFile(tempFile);
    }

    private static final SQLiteDatabaseHook COMPATIBILITY_HOOK = new SQLiteDatabaseHook() {
        @Override
        public void preKey(SQLiteDatabase database) {

        }

        @Override
        public void postKey(@NotNull SQLiteDatabase database) {
            //微信的CipherDB版本还是1，可以用以下指令打开，但是实测，用这个指令打开，修改数据库不会同步到本地文件
            //database.rawExecSQL("PRAGMA cipher_compatibility = 1;");
            //也可以用下面这条指令打开，但是速度较慢
            //migrate this database to latest version
            database.rawExecSQL("PRAGMA cipher_migrate;");
        }
    };

    private void tryOpenDatabaseOf(@NonNull User user, @NonNull String imei) {
        String possibleKey = DigestUtils.md5Hex(imei + user.uin).substring(0, 7).toLowerCase();
        int flag = App.config().isEditModeEnabled() ? SQLiteDatabase.OPEN_READWRITE : SQLiteDatabase.OPEN_READONLY;
        mDatabaseOfCurUser = SQLiteDatabase.openDatabase(user.workerDatabaseFilePath, possibleKey, null, flag, COMPATIBILITY_HOOK);
        user.databasePassword = possibleKey;
        App.getSharedPrefsManager().putImei(imei);
    }

    public DatabaseModifier modifyDatabase() {
        synchronized (DatabaseModifier.class) {
            if (mModifier == null) {
                mModifier = new DatabaseModifier(this);
            }
            return mModifier;
        }
    }

    public void reopenDatabase(int mode) {
        mDatabaseOfCurUser.close();
        mDatabaseOfCurUser = SQLiteDatabase.openDatabase(mCurrentUser.workerDatabaseFilePath, mCurrentUser.databasePassword, null, mode, COMPATIBILITY_HOOK);
    }

    public String serialize() {
        ByteArrayInputStream in = new ByteArrayInputStream(toString().getBytes());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ZipOutputStream outputStream = new ZipOutputStream(out);
        try {
            outputStream.setLevel(9);
            outputStream.putNextEntry(new ZipEntry(""));
            IoUtils.transferStream(in, outputStream);
            return Base64.encodeBase64String(out.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Nullable
    public static String deserialize(String serial) {
        try {
            ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(Base64.decodeBase64(serial)));
            in.getNextEntry();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            IoUtils.transferStream(in, out);
            return out.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void purge() {
        if (mDatabaseOfCurUser != null) {
            mDatabaseOfCurUser.close();
        }
        if (mCurrentUser != null) {
            mCurrentUser.deleteWorkerDatabase();
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
        return "<b>version_name</b>: " + BuildConfig.VERSION_NAME + "<br/>" +
                "<b>version_code</b>: " + BuildConfig.VERSION_CODE + "<br/>" + "<b>pid:</>" + Process.myPid()
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


}
