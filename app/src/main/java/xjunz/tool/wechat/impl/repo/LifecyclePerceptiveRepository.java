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

abstract class LifecyclePerceptiveRepository implements LifecycleObserver {
    private SQLiteDatabase mDb;
    private Environment mEnv;
    private User mCurrentUser;

    LifecyclePerceptiveRepository() {
        mEnv = Environment.getInstance();
        mEnv.getLifecycle().addObserver(this);
        mDb = mEnv.getDatabaseOfCurrentUser();
        mCurrentUser = mEnv.getCurrentUser();
    }


    SQLiteDatabase getDatabase() {
        return mDb;
    }

    User getCurrentUser() {
        return mCurrentUser;
    }

    Environment getEnvironment() {
        return mEnv;
    }


    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    public void purge() {
        RepositoryFactory.remove(getClass());
    }
}
