/*
 * Copyright (c) 2020 xjunz. 保留所有权利
 */

package xjunz.tool.wechat.impl.repo;

import androidx.annotation.NonNull;
import androidx.collection.SimpleArrayMap;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import xjunz.tool.wechat.impl.model.account.Account;
import xjunz.tool.wechat.impl.model.account.Contact;

public abstract class AccountRepository<T extends Account> extends LifecyclePerceptiveRepository {
    private SimpleArrayMap<Contact.Type, List<T>> mMap = new SimpleArrayMap<>();
    private List<T> mAll = new ArrayList<>();

    @NonNull
    public List<T> getAll() {
        if (mAll == null) {
            throw new IllegalStateException("Pls call queryAll first! ");
        }
        return mAll;
    }

    abstract void queryAll(@NonNull List<T> all);

    public void queryAll() {
        if (mAll == null) {
            mAll = new ArrayList<>();
        }
        queryAll(mAll);
    }

    @NonNull
    public List<T> get(Contact.Type type) {
        List<T> accounts = mMap.get(type);
        if (accounts == null) {
            accounts = new LinkedList<>();
            mMap.put(type, accounts);
        }
        return accounts;
    }

    public abstract T get(String id);
}
