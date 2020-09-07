/*
 * Copyright (c) 2020 xjunz. 保留所有权利
 */

package xjunz.tool.wechat.impl.repo;


import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;

import net.sqlcipher.database.SQLiteDatabase;

import xjunz.tool.wechat.impl.Environment;
import xjunz.tool.wechat.impl.model.account.User;

class LifecyclePerceptiveRepository implements LifecycleObserver {
    private final SQLiteDatabase mDatabase;
    private final Environment mEnv;
    private final User mCurrentUser;

    protected LifecyclePerceptiveRepository() {
        mEnv = Environment.getInstance();
        mEnv.getLifecycle().addObserver(this);
        mDatabase = mEnv.getDatabaseOfCurrentUser();
        mCurrentUser = mEnv.getCurrentUser();
    }

    protected SQLiteDatabase getDatabase() {
        return mDatabase;
    }

    protected User getCurrentUser() {
        return mCurrentUser;
    }

    protected Environment getEnvironment() {
        return mEnv;
    }


    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    public void purge() {
        RepositoryFactory.remove(getClass());
    }
}
