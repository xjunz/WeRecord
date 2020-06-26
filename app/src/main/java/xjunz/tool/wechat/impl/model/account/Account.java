package xjunz.tool.wechat.impl.model.account;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.LruCache;

import androidx.annotation.Keep;
import androidx.annotation.Nullable;

import org.apaches.commons.codec.digest.DigestUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.Serializable;

import xjunz.tool.wechat.impl.Environment;
import xjunz.tool.wechat.util.ShellUtils;


/**
 * 微信账号的抽象类
 */
public abstract class Account implements Serializable {
    /**
     * 微信的昵称
     */
    public String nickname;
    /**
     * 微信号，是微信账号或有的唯一标识
     */
    public String alias;
    private static final LruCache<String, Bitmap> avatarBitmapCache = new LruCache<String, Bitmap>(10 * 1024 * 1024) {
        @Override
        protected int sizeOf(String key, Bitmap value) {
            return value.getByteCount();
        }
    };
    private String originalAvatarPath;
    private String backupAvatarPath;
    /**
     * 微信ID，通常的形式是"wxid_xxxxx"，是微信账号必有的唯一标识
     */
    public String id;
    private String pathIdentifier;
    private String ownerDirPath;
    /**
     * 当前用户的UIN，是当前用户的唯一标识
     */
    private String ownerUin;

    String getPathIdentifier() {
        return pathIdentifier;
    }

    String getOwnerDirPath() {
        return ownerDirPath;
    }


    protected boolean isGZH() {
        return id.startsWith("gh_");
    }

    @Keep
    protected boolean mayUser() {
        return id.startsWith("wxid_");
    }

    protected boolean isGroup() {
        return id.endsWith("@chatroom");
    }

    Account(String uin) {
        this.ownerUin = uin;
        this.pathIdentifier = DigestUtils.md5Hex("mm" + ownerUin);
        this.ownerDirPath = Environment.getInstance().getWechatMicroMsgPath() + File.separator + pathIdentifier;
    }


    public void endowIdentity(String id) {
        this.id = id;
        String idMd5 = DigestUtils.md5Hex(id);
        this.backupAvatarPath = Environment.getInstance().getAvatarBackupPath() + File.separator + idMd5;
        this.originalAvatarPath = this.ownerDirPath + File.separator + "avatar" + File.separator
                + idMd5.substring(0, 2) + File.separator
                + idMd5.substring(2, 4) + File.separator
                + "user_" + idMd5 + ".png";
    }

    boolean empty(String str) {
        return str == null || str.length() == 0;
    }


    public String getName() {
        return empty(nickname) ? (empty(alias) ? (empty(id) ? "<unknown>" : id) : alias) : nickname;
    }

    public String getOwnerUin() {
        return ownerUin;
    }

    public Bitmap decodeAvatar() {
        Bitmap bitmap;
        File backup = new File(backupAvatarPath);
        if (!backup.exists()) {
            ShellUtils.cpNoError(originalAvatarPath, backupAvatarPath);
        }
        bitmap = BitmapFactory.decodeFile(backupAvatarPath);
        if (bitmap == null) {
            return null;
        }
        synchronized (avatarBitmapCache) {
            avatarBitmapCache.put(id, bitmap);
        }
        return bitmap;
    }


    public Bitmap getAvatar() {
        Bitmap bitmap = avatarBitmapCache.get(id);
        if (bitmap == null) {
            bitmap = decodeAvatar();
        }
        return bitmap;
    }


    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj instanceof Account) {
            return ((Account) obj).id.equals(id);
        }
        return super.equals(obj);
    }

    @NotNull
    @Override
    public String toString() {
        return "Account{" +
                "nickname='" + nickname + '\'' +
                ", alias='" + alias + '\'' +
                ", id='" + id + '\'' +
                '}';
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }


}
