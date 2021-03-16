/*
 * Copyright (c) 2021 xjunz. 保留所有权利
 */

package xjunz.tool.werecord.impl.model.account;

import android.os.Parcel;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import org.apaches.commons.codec.digest.DigestUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.lang.reflect.Field;

import xjunz.tool.werecord.impl.Environment;
import xjunz.tool.werecord.util.IoUtils;


public class User extends Account {
    public final String dirPath;
    public final String originalDatabaseFilePath;
    /**
     * 工作数据库: 即我们用于各种数据库操作的数据库，替换微信的源数据库以实现功能
     */
    public String workerDatabaseFilePath;
    /**
     * //todo
     * 备份数据库: 备份的微信源数据库，用于出现意外时的还原
     */
    public String backupDatabaseFilePath;
    public String databasePassword;
    public final String imageCachePath;
    public final String videoCachePath;
    public boolean isLastLogin;
    public String uin;
    public String phoneNum;
    public boolean isCurrentUsed;

    public User(String uin) {
        this.uin = uin;
        String pathIdentifier = DigestUtils.md5Hex("mm" + uin);
        this.dirPath = Environment.getInstance().getVictimMicroMsgPath() + File.separator + pathIdentifier;
        this.originalDatabaseFilePath = dirPath + File.separator + "EnMicroMsg.db";
        this.imageCachePath = android.os.Environment.getExternalStorageDirectory().getPath() + File.separator + "tencent"
                + File.separator + "MicroMsg" + File.separator + pathIdentifier + File.separator + "image2";
        this.videoCachePath = android.os.Environment.getExternalStorageDirectory().getPath() + File.separator + "tencent"
                + File.separator + "MicroMsg" + File.separator + pathIdentifier + File.separator + "video";
    }

    public void deleteWorkerDatabase() {
        final File backup = new File(workerDatabaseFilePath);
        if (backup.exists()) {
            IoUtils.deleteFileSync(backup);
        }
    }

    public String getBlurredPhoneNumber() {
        if (!TextUtils.isEmpty(phoneNum) && phoneNum.length() == 11) {
            return phoneNum.substring(0, 3) + "****" + phoneNum.substring(7);
        }
        return phoneNum;
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
                    str = str == null ? "<null>" : str;
                    output.append(field.getName()).append(": ").append(str).append("\n");
                }
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return output.toString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NotNull Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(this.dirPath);
        dest.writeString(this.originalDatabaseFilePath);
        dest.writeString(this.workerDatabaseFilePath);
        dest.writeString(this.backupDatabaseFilePath);
        dest.writeString(this.databasePassword);
        dest.writeString(this.imageCachePath);
        dest.writeString(this.videoCachePath);
        dest.writeByte(this.isLastLogin ? (byte) 1 : (byte) 0);
        dest.writeString(this.uin);
    }

    protected User(Parcel in) {
        super(in);
        this.dirPath = in.readString();
        this.originalDatabaseFilePath = in.readString();
        this.workerDatabaseFilePath = in.readString();
        this.backupDatabaseFilePath = in.readString();
        this.databasePassword = in.readString();
        this.imageCachePath = in.readString();
        this.videoCachePath = in.readString();
        this.isLastLogin = in.readByte() != 0;
        this.uin = in.readString();
    }

    public static final Creator<User> CREATOR = new Creator<User>() {
        @Override
        public User createFromParcel(Parcel source) {
            return new User(source);
        }

        @Override
        public User[] newArray(int size) {
            return new User[size];
        }
    };
}
