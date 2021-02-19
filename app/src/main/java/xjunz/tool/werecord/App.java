/*
 * Copyright (c) 2021 xjunz. 保留所有权利
 */

package xjunz.tool.werecord;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.lifecycle.ViewModelStore;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import com.github.promeg.pinyinhelper.Pinyin;

import net.sqlcipher.database.SQLiteDatabase;

import org.apaches.commons.codec.digest.DigestUtils;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.security.GeneralSecurityException;

import xjunz.tool.werecord.util.IoUtils;

public class App extends Application implements ViewModelStoreOwner {
    private static WeakReference<Context> sApplicationContext;
    private static final String DATA_SHARED_PREFS_NAME = "data";
    private static SharedPreferences sSharedPrefs;
    private static SharedPrefsManager sSharedPrefsManager;
    public static String DATA_PATH;
    private ViewModelStore mViewModelStore;
    private static Settings sSettings;

    @NotNull
    public static String getStringOf(@StringRes int strRes) {
        return getContext().getString(strRes);
    }

    @NotNull
    public static String getStringOf(@StringRes int strRes, Object... args) {
        return getContext().getString(strRes, args);
    }

    public static CharSequence getTextOf(@StringRes int textRes) {
        return getContext().getText(textRes);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Thread.setDefaultUncaughtExceptionHandler(new CrashHandler());
        //初始化CipherSQL
        SQLiteDatabase.loadLibs(this);
        //初始化拼音库
        Pinyin.init(null);
        sApplicationContext = new WeakReference<>(getApplicationContext());
        try {
            String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
            sSharedPrefs = EncryptedSharedPreferences.create(
                    DATA_SHARED_PREFS_NAME,
                    masterKeyAlias,
                    getContext(),
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
            sSettings = new Settings(sSharedPrefs);
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
        }
        sSharedPrefsManager = new SharedPrefsManager();
        DATA_PATH = getContext().getFilesDir().getPath();
        mViewModelStore = new ViewModelStore();
    }

    public static Context getContext() {
        return sApplicationContext.get();
    }

    @Contract(pure = true)
    public static SharedPrefsManager getSharedPrefsManager() {
        return sSharedPrefsManager;
    }

    @NonNull
    @Override
    public ViewModelStore getViewModelStore() {
        return mViewModelStore;
    }

    public static Settings config() {
        return sSettings;
    }

    public static class SharedPrefsManager {
        private static final String key_recorded_version_code = "recorded_version_code";
        private static final String key_is_first_run = "is_first_run";
        private static final String key_is_app_intro_done = "is_app_intro_done";
        private static final String key_imei = "imei";

        public boolean isFirstRun() {
            if (sSharedPrefs.getBoolean(key_is_first_run, true)) {
                sSharedPrefs.edit().putBoolean(key_is_first_run, false).apply();
                return true;
            }
            return false;
        }

        public String getImei() {
            return sSharedPrefs.getString(key_imei, null);
        }

        public void putImei(String imei) {
            sSharedPrefs.edit().putString(key_imei, imei).apply();
        }

        public void setIsAppIntroDone(boolean done) {
            sSharedPrefs.edit().putBoolean(key_is_app_intro_done, done).apply();
        }

        public boolean isAppIntroDone() {
            return sSharedPrefs.getBoolean(key_is_app_intro_done, false);
        }

        private int getRecordedVersionCode() {
            return sSharedPrefs.getInt(key_recorded_version_code, -1);
        }

        private void setRecordedVersionCode() {
            sSharedPrefs.edit().putInt(key_recorded_version_code, BuildConfig.VERSION_CODE).apply();
        }

        public boolean isAppUpdated() {
            if (BuildConfig.VERSION_CODE > getRecordedVersionCode()) {
                setRecordedVersionCode();
                return true;
            }
            return false;
        }

        public boolean noMore(String key) {
            return sSharedPrefs.getBoolean(key, false);
        }

        public void setNoMore(String key, boolean isNoMore) {
            sSharedPrefs.edit().putBoolean(key, true).apply();
        }
    }

    public class SerializationManager {
        private SerializationManager() {
        }

        private String generatePathFor(String name) {
            return DATA_PATH + File.separator + DigestUtils.md5Hex(name);
        }

        public void write(Object object, String filename) {
            IoUtils.serializeToStorage(object, generatePathFor(filename));
        }

        public <T> T read(String filename, Class<T> clazz) {
            return IoUtils.deserializeFromStorage(generatePathFor(filename), clazz);
        }

        public void delete(String filename) {
            File file = new File(generatePathFor(filename));
            if (file.exists()) {
                if (!file.delete()) {
                    throw new RuntimeException("can't delete " + filename);
                }
            }
        }
    }
}
