/*
 * Copyright (c) 2021 xjunz. 保留所有权利
 */

package xjunz.tool.werecord.impl.repo;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.LruCache;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.apaches.commons.codec.digest.DigestUtils;

import java.io.File;
import java.io.IOException;

import xjunz.tool.werecord.util.LogUtils;
import xjunz.tool.werecord.util.ShellUtils;

public class AvatarRepository extends LifecyclePerceptiveRepository {
    private static final int DEFAULT_CACHE_SIZE = 20 * 1024 * 1024;
    //Create a LruCache with 20MB of opacity
    private final LruCache<String, Bitmap> mAvatarCache;
    private static long sAvatarExpiredTime = 7 * 24 * 60 * 60 * 1000;

    AvatarRepository() {
        this.mAvatarCache = new LruCache<String, Bitmap>(DEFAULT_CACHE_SIZE) {
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getByteCount();
            }
        };
    }

    public void setAvatarExpiredTime(long timeInMills) {
        sAvatarExpiredTime = timeInMills;
    }

    /**
     * 从本地文件中解码指定微信ID的头像{@link Bitmap}
     *
     * @param id 微信ID
     * @return 该微信账号的头像，如果不存在则返回null
     */
    @Nullable
    private Bitmap decodeAvatar(@NonNull String id) {
        String idMd5 = DigestUtils.md5Hex(id);
        String backupAvatarPath = getEnvironment().getAvatarBackupPath() + File.separator + idMd5;
        String originalAvatarPath = getCurrentUser().dirPath + File.separator + "avatar" + File.separator
                + idMd5.substring(0, 2) + File.separator
                + idMd5.substring(2, 4) + File.separator
                + "user_" + idMd5 + ".png";
        try {
            File avatarFile = new File(backupAvatarPath);
            //如果头像不存在或已过期
            if (!avatarFile.exists() || (avatarFile.exists() && System.currentTimeMillis() - avatarFile.lastModified() > sAvatarExpiredTime)) {
                ShellUtils.cp2dataIfExists(originalAvatarPath, backupAvatarPath, true);
            }
        } catch (ShellUtils.ShellException | IOException e) {
            LogUtils.error("Failed to load avatar of " + id + " :" + e.getMessage());
        }
        return BitmapFactory.decodeFile(backupAvatarPath);
    }


    /**
     * 将指定微信ID的头像纳入缓存
     *
     * @param id     指定微信ID
     * @param bitmap 欲缓存的头像
     */
    public void putAvatarOf(@NonNull String id, @NonNull Bitmap bitmap) {
        synchronized (mAvatarCache) {
            mAvatarCache.put(id, bitmap);
        }
    }

    /**
     * 获取指定微信ID的微信头像
     * <p>先从缓存中获取，如果存在返回缓存的头像，如果不存在，再尝试本地解码
     * 文件，如果本地头像文件解码成功，纳入缓存并返回此头像，如果不存在，返回null
     * </p>
     *
     * @param id 微信ID
     * @return 该微信账号的头像，如果不存在则返回null
     */
    @Nullable
    public Bitmap getAvatar(@NonNull String id) {
        Bitmap cache = mAvatarCache.get(id);
        if (cache == null) {
            Bitmap bitmap = decodeAvatar(id);
            if (bitmap != null) {
                putAvatarOf(id, bitmap);
                return bitmap;
            } else {
                return null;
            }
        } else {
            return cache;
        }
    }

}
