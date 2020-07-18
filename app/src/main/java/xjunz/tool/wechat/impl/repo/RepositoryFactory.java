/*
 * Copyright (c) 2020 xjunz. 保留所有权利
 */
package xjunz.tool.wechat.impl.repo;

import java.util.HashMap;

public class RepositoryFactory {
    private static HashMap<Class<? extends LifecyclePerceptiveRepository>, LifecyclePerceptiveRepository> sInstanceMap = new HashMap<>();

    public static <T extends LifecyclePerceptiveRepository> T singleton(Class<T> tClass) {
        LifecyclePerceptiveRepository singleton = sInstanceMap.get(tClass);
        if (singleton == null) {
            synchronized (LifecyclePerceptiveRepository.class) {
                try {
                    singleton = tClass.newInstance();
                    sInstanceMap.put(tClass, singleton);
                } catch (IllegalAccessException | InstantiationException e) {
                    e.printStackTrace();
                }
            }
        }
        return tClass.cast(singleton);
    }

    static void remove(Class<? extends LifecyclePerceptiveRepository> tClass) {
        sInstanceMap.remove(tClass);
    }

}
