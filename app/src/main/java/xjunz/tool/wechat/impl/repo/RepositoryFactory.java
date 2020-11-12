/*
 * Copyright (c) 2020 xjunz. 保留所有权利
 */
package xjunz.tool.wechat.impl.repo;

import java.util.HashMap;

/**
 * 构造并管理单例{@link LifecyclePerceptiveRepository}的工厂类
 */
public final class RepositoryFactory {
    private static final HashMap<Class<? extends LifecyclePerceptiveRepository>, LifecyclePerceptiveRepository> sInstanceMap = new HashMap<>();

    public static <T extends LifecyclePerceptiveRepository> T get(Class<T> repoClass) {
        LifecyclePerceptiveRepository singleton = sInstanceMap.get(repoClass);
        if (singleton == null) {
            synchronized (sInstanceMap) {
                try {
                    singleton = repoClass.newInstance();
                    sInstanceMap.put(repoClass, singleton);
                } catch (IllegalAccessException | InstantiationException e) {
                    e.printStackTrace();
                }
            }
        }
        return repoClass.cast(singleton);
    }

    static void remove(Class<? extends LifecyclePerceptiveRepository> tClass) {
        synchronized (sInstanceMap) {
            sInstanceMap.remove(tClass);
        }
    }
}
