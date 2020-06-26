package xjunz.tool.wechat.impl.repo;

import androidx.annotation.Keep;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;

import net.sqlcipher.database.SQLiteDatabase;

import xjunz.tool.wechat.impl.Environment;
import xjunz.tool.wechat.impl.model.account.User;

public abstract class LifecyclePerceptiveRepository implements LifecycleObserver {
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

    @Keep
    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    public abstract void purge();

}
