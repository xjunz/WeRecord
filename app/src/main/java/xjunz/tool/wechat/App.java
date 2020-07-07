package xjunz.tool.wechat;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;

import com.github.promeg.pinyinhelper.Pinyin;

import net.sqlcipher.database.SQLiteDatabase;

import org.apaches.commons.codec.digest.DigestUtils;
import org.jetbrains.annotations.Contract;

import java.io.File;
import java.lang.ref.WeakReference;

import xjunz.tool.wechat.util.IOUtils;

public class App extends Application {
    private static WeakReference<Context> sApplicationContext;
    private static final String DATA_SHARED_PREFS_NAME = "data";
    public static int VERSION_CODE;
    public static String VERSION_NAME;
    private static SharedPreferences gSharedPrefs;
    private static SharedPrefsManager gSharedPrefsManager;
    public static String DATA_PATH;

    public static String getStringOf(int strRes) {
        return getContext().getString(strRes);
    }

    public static String getStringOf(int strRes, Object... args) {
        return getContext().getString(strRes, args);
    }


    @Override
    public void onCreate() {
        super.onCreate();
        //初始化Cipher SQL
        SQLiteDatabase.loadLibs(this);
        //初始化拼音库
        Pinyin.init(null);
        sApplicationContext = new WeakReference<>(getApplicationContext());
        gSharedPrefs = getApplicationContext().getSharedPreferences(DATA_SHARED_PREFS_NAME, Context.MODE_PRIVATE);
        gSharedPrefsManager = new SharedPrefsManager();
        DATA_PATH = getContext().getFilesDir().getPath();
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_ACTIVITIES);
            VERSION_CODE = packageInfo.versionCode;
            VERSION_NAME = packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            if (BuildConfig.DEBUG)
                e.printStackTrace();
        }
    }

    public static Context getContext() {
        return sApplicationContext.get();
    }

    @ColorInt
    public static int getColorOf(@ColorRes int colorRes) {
        return getContext().getResources().getColor(colorRes);
    }

    @Contract(pure = true)
    public static SharedPrefsManager getSharedPrefsManager() {
        return gSharedPrefsManager;
    }

    public static class SharedPrefsManager {
        private static final String key_recorded_version_code = "rvc";
        private static final String key_is_first_run = "ifr";
        private static final String key_is_app_intro_done = "iaid";
        private static final String key_imei = "imei";

        public boolean isFirstRun() {
            if (gSharedPrefs.getBoolean(key_is_first_run, true)) {
                gSharedPrefs.edit().putBoolean(key_is_first_run, false).apply();
                return true;
            }
            return false;
        }

        public String getIMEI() {
            return gSharedPrefs.getString(key_imei, null);
        }

        public void putIMEI(String imei) {
            gSharedPrefs.edit().putString(key_imei, imei).apply();
        }

        public void setIsAppIntroDone(boolean done) {
            gSharedPrefs.edit().putBoolean(key_is_app_intro_done, done).apply();
        }

        public boolean isAppIntroDone() {
            return gSharedPrefs.getBoolean(key_is_app_intro_done, false);
        }

        private int getRecordedVersionCode() {
            return gSharedPrefs.getInt(key_recorded_version_code, -1);
        }

        private void setRecordedVersionCode(int versionCode) {
            gSharedPrefs.edit().putInt(key_recorded_version_code, versionCode).apply();
        }

        public boolean isAppUpdated() {
            if (VERSION_CODE > getRecordedVersionCode()) {
                setRecordedVersionCode(VERSION_CODE);
                return true;
            }
            return false;
        }
    }

    public class SerializationManager {
        private SerializationManager() {
        }

        private String generatePathFor(String name) {
            return DATA_PATH + File.separator + DigestUtils.md5Hex(name);
        }

        public void write(Object object, String filename) {
            IOUtils.serializeToStorage(object, generatePathFor(filename));
        }

        public <T> T read(String filename, Class<T> clazz) {
            return IOUtils.deserializeFromStorage(generatePathFor(filename), clazz);
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
