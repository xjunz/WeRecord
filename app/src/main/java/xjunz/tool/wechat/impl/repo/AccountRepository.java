package xjunz.tool.wechat.impl.repo;

import androidx.annotation.NonNull;
import androidx.collection.SimpleArrayMap;

import java.util.LinkedList;
import java.util.List;

import xjunz.tool.wechat.impl.model.account.Account;
import xjunz.tool.wechat.impl.model.account.Contact;

public abstract class AccountRepository<T extends Account> extends LifecyclePerceptiveRepository {
    private SimpleArrayMap<Contact.Type, List<T>> mMap = new SimpleArrayMap<>();
    protected List<T> mAll = new LinkedList<>();

    @NonNull
    public List<T> getAll() {
        return mAll;
    }

    public abstract void queryAll();

    @NonNull
    public List<T> get(Contact.Type type) {
        List<T> accounts = mMap.get(type);
        if (accounts == null) {
            accounts = new LinkedList<>();
            mMap.put(type, accounts);
        }
        return accounts;
    }
}
