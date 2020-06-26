package xjunz.tool.wechat.impl.model.account;

import android.util.Log;

import androidx.annotation.NonNull;

import java.io.File;
import java.lang.reflect.Field;

import xjunz.tool.wechat.impl.Environment;


public class User extends Account {

    private String originalDatabaseFilePath;
    public String backupDatabaseFilePath;
    public transient String databasePragmaKey;
    public String imageCachePath;
    public String videoCachePath;
    public boolean isLastLogin;


    public User(String uin) {
        super(uin);
        this.originalDatabaseFilePath = this.getOwnerDirPath() + File.separator + Environment.DATABASE_EN_MICRO_MSG_NAME;
        this.imageCachePath = android.os.Environment.getExternalStorageDirectory().getPath() + File.separator + "tencent"
                + File.separator + "MicroMsg" + File.separator + getPathIdentifier() + File.separator + "image2";
        this.videoCachePath = android.os.Environment.getExternalStorageDirectory().getPath() + File.separator + "tencent"
                + File.separator + "MicroMsg" + File.separator + getPathIdentifier() + File.separator + "video";
    }

    public String getMsgDatabasePath() {
        return originalDatabaseFilePath;
    }

    public void deleteBackupDatabase() {
        final File backup = new File(backupDatabaseFilePath);
        if (backup.exists()) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    if (!backup.delete()) {
                        Log.wtf("?o?", "Failed to delete backup files");
                    }
                }
            }).start();
        }
    }

    @NonNull
    @Override
    public String toString() {
        StringBuilder output = new StringBuilder();
        Field[] fields = getClass().getFields();
        try {
            for (Field field : fields) {
                if (field.getGenericType() == String.class) {
                    String str = (String) field.get(this);
                    str = str == null ? "<i>null</i>" : str;
                    output.append("<b>").append(field.getName()).append("</b>").append(": ").append(str).append("<br/>");
                }
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return output.toString();
    }
}
