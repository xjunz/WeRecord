/*
 * Copyright (c) 2020 xjunz. 保留所有权利
 */

package xjunz.tool.wechat.impl.model.account;

import android.util.Log;

import androidx.annotation.NonNull;

import org.apaches.commons.codec.digest.DigestUtils;

import java.io.File;
import java.lang.reflect.Field;

import xjunz.tool.wechat.impl.Environment;


public class User extends Account {
    public final String dirPath;
    public final String originalDatabaseFilePath;
    public String backupDatabaseFilePath;
    public String databasePragmaKey;
    public final String imageCachePath;
    public final String videoCachePath;
    public boolean isLastLogin;
    public String uin;

    public User(String uin) {
        this.uin = uin;
        String pathIdentifier = DigestUtils.md5Hex("mm" + uin);
        this.dirPath = Environment.getInstance().getWechatMicroMsgPath() + File.separator + pathIdentifier;
        this.originalDatabaseFilePath = dirPath + File.separator + "EnMicroMsg.db";
        this.imageCachePath = android.os.Environment.getExternalStorageDirectory().getPath() + File.separator + "tencent"
                + File.separator + "MicroMsg" + File.separator + pathIdentifier + File.separator + "image2";
        this.videoCachePath = android.os.Environment.getExternalStorageDirectory().getPath() + File.separator + "tencent"
                + File.separator + "MicroMsg" + File.separator + pathIdentifier + File.separator + "video";
    }

    public String getMsgDatabasePath() {
        return originalDatabaseFilePath;
    }

    public void deleteBackupDatabase() {
        final File backup = new File(backupDatabaseFilePath);
        if (backup.exists()) {
            new Thread(() -> {
                if (!backup.delete()) {
                    Log.wtf("?o?", "Failed to delete backup files");
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
