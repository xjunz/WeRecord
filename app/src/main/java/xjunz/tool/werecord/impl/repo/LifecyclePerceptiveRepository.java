/*
 * Copyright (c) 2021 xjunz. 保留所有权利
 */

package xjunz.tool.werecord.impl.repo;


import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;

import net.sqlcipher.database.SQLiteDatabase;

import xjunz.tool.werecord.impl.Environment;
import xjunz.tool.werecord.impl.model.account.User;

class LifecyclePerceptiveRepository implements LifecycleObserver {
    private final Environment mEnv;

    protected LifecyclePerceptiveRepository() {
        mEnv = Environment.getInstance();
        mEnv.addLifecycleObserver(this);
    }

    protected SQLiteDatabase getDatabase() {
        return mEnv.getWorkerDatabase();
    }

    protected User getCurrentUser() {
        return mEnv.getCurrentUser();
    }

    protected Environment getEnvironment() {
        return mEnv;
    }


    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    public void purge() {
        RepositoryFactory.remove(getClass());
    }
}
