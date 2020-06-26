package xjunz.tool.wechat.impl.repo;

import android.graphics.Bitmap;
import android.util.LruCache;

public class AvatarRepository extends LifecyclePerceptiveRepository {
    private static AvatarRepository sInstance;
    private static final int DEFAULT_CACHE_SIZE = 5 * 1024 * 1024;
    //Create a LruCache with 5MB of opacity
    private LruCache<String, Bitmap> sAvatarCache;

    private AvatarRepository() {
        this.sAvatarCache = new LruCache<String, Bitmap>(DEFAULT_CACHE_SIZE) {
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getByteCount();
            }
        };
    }

    public Bitmap getAvatarOf(String id) {
        return sAvatarCache.get(id);
    }

    public void putAvatarOf(String id, Bitmap bitmap) {
        sAvatarCache.put(id, bitmap);
    }

    public static AvatarRepository getInstance() {
        sInstance = sInstance == null ? new AvatarRepository() : sInstance;
        return sInstance;
    }

    @Override
    public void purge() {
        sInstance = null;
    }

}
