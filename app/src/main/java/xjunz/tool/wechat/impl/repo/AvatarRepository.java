package xjunz.tool.wechat.impl.repo;

import android.graphics.Bitmap;
import android.util.LruCache;

import androidx.annotation.Nullable;

public class AvatarRepository extends LifecyclePerceptiveRepository {
    private static AvatarRepository sInstance;
    private static final int DEFAULT_CACHE_SIZE = 20 * 1024 * 1024;
    //Create a LruCache with 20MB of opacity
    private final LruCache<String, Bitmap> sAvatarCache;

    private AvatarRepository() {
        this.sAvatarCache = new LruCache<String, Bitmap>(DEFAULT_CACHE_SIZE) {
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getByteCount();
            }
        };
    }


    @Nullable
    public Bitmap getAvatarOf(String id) {
        return sAvatarCache.get(id);
    }


    public void putAvatarOf(String id, Bitmap bitmap) {
        synchronized (sAvatarCache) {
            sAvatarCache.put(id, bitmap);
        }
    }

    public static AvatarRepository getInstance() {
        return sInstance = (sInstance == null ? new AvatarRepository() : sInstance);
    }

    @Override
    public void purge() {
        sInstance = null;
    }

}
