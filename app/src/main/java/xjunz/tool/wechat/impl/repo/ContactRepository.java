/*
 * Copyright (c) 2020 xjunz. 保留所有权利
 */

package xjunz.tool.wechat.impl.repo;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.SimpleArrayMap;

import net.sqlcipher.Cursor;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import xjunz.tool.wechat.impl.model.account.Contact;

public class ContactRepository extends AccountRepository<Contact> {
    private static final int CACHE_CAPACITY = 500;
    private final SimpleArrayMap<Contact.Type, List<Contact>> mMap = new SimpleArrayMap<>();

    ContactRepository() {
    }

    @Override
    public int getCacheCapacity() {
        return CACHE_CAPACITY;
    }

    @Override
    protected void queryAllInternal(@NotNull List<Contact> all) {
        Cursor cursor = getDatabase().rawQuery("select username,alias,conRemark,nickname,type from rcontact where not type in (0,4,33)", null);
        while (cursor.moveToNext()) {
            String id = cursor.getString(0);
            if (!TextUtils.isEmpty(id) && !id.startsWith("fake_")) {
                Contact contact = new Contact(id);
                contact.alias = cursor.getString(1);
                contact.remark = cursor.getString(2);
                contact.nickname = cursor.getString(3);
                contact.rawType = cursor.getInt(4);
                contact.judgeType();
                getAllOfType(contact.type).add(contact);
                all.add(contact);
            }
        }
        cursor.close();
    }

    @Nullable
    protected Contact query(@NonNull String id) {
        Cursor cursor = getDatabase().rawQuery("select alias,conRemark,nickname,type from rcontact where username='" + id + "'", null);
        if (cursor.moveToNext()) {
            Contact contact = new Contact(id);
            contact.alias = cursor.getString(0);
            contact.remark = cursor.getString(1);
            contact.nickname = cursor.getString(2);
            contact.rawType = cursor.getInt(3);
            contact.judgeType();
            cursor.close();
            return contact;
        }
        cursor.close();
        return null;
    }

    @NonNull
    public List<Contact> getAllOfType(Contact.Type type) {
        List<Contact> accounts = mMap.get(type);
        if (accounts == null) {
            accounts = new ArrayList<>();
            mMap.put(type, accounts);
        }
        return accounts;
    }
}
