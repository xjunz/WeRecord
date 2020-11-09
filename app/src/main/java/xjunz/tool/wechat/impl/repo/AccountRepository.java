/*
 * Copyright (c) 2020 xjunz. 保留所有权利
 */

package xjunz.tool.wechat.impl.repo;

import android.util.LruCache;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import xjunz.tool.wechat.impl.model.account.Account;

abstract class AccountRepository<T extends Account> extends LifecyclePerceptiveRepository {
    private List<T> mAll = new ArrayList<>();
    private final LruCache<String, T> mCache;

    AccountRepository() {
        mCache = new LruCache<>(getCacheCapacity());
    }

    public abstract int getCacheCapacity();

    public void resizeCache(int capacity) {
        mCache.resize(capacity);
    }

    @NonNull
    public List<T> getAll() {
        if (mAll == null) {
            throw new NullPointerException("Please call queryAll() first! ");
        }
        return mAll;
    }

    protected abstract void queryAllInternal(@NonNull List<T> all);

    public void queryAll() {
        if (mAll == null) {
            mAll = new ArrayList<>();
        }
        queryAllInternal(mAll);
    }

    /**
     * 从数据库查询一个{@link Account}
     *
     * @param id {@link Account}的ID
     * @return 查询到的 {@link Account}。如果查询不到，返回{@code null}
     */
    protected abstract T query(String id);

    /**
     * 获取某个{@param id}的{@link Account}实例
     * <p>
     * 此方法会先从{@link LruCache}缓存中获取，如果不存在，尝试从全部已查询到的
     * {@link Account}列表({@link AccountRepository#getAll()})中获取，如果不存在，再尝试从数据库获取，
     * 即调用{@link AccountRepository#query(String)}获取，如果取得，存入缓存，
     * 如果未取得，返回{@code null}
     * </p>
     *
     * @param id 与获取的{@link Account}的ID
     * @return 获取到的 {@link Account}，如果数据库中查询不到此ID，返回{@code null}
     */
    @SuppressWarnings("SuspiciousMethodCalls")
    @Nullable
    public T get(String id) {
        //先从缓存中获取
        T t = mCache.get(id);
        //如果缓存中不存在
        if (t == null) {
            int index;
            //从全部中获取
            if (mAll != null && (index = mAll.indexOf(id)) >= 0) {
                t = mAll.get(index);
                mCache.put(id, t);
            } else {
                //如果都不存在，从数据库中获取
                t = query(id);
                if (t != null) {
                    mCache.put(id, t);
                } else {
                    return null;
                }
            }
        }
        return t;
    }
}
