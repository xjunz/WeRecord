package xjunz.tool.wechat.impl.repo;

import xjunz.tool.wechat.impl.model.account.User;

public class UserRepository extends LifecyclePerceptiveRepository {
    private static UserRepository sInstance;


    private UserRepository() {
    }

    public static UserRepository getInstance() {
        sInstance = sInstance == null ? new UserRepository() : sInstance;
        return sInstance;
    }

    @Override
    public void purge() {
        sInstance = null;
    }


    public void fulfill(User user) {

    }
}
