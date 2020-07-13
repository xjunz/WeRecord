/*
 * Copyright (c) 2020 xjunz. 保留所有权利
 */

package xjunz.tool.wechat.impl.repo;

import android.text.TextUtils;
import android.util.LruCache;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.sqlcipher.Cursor;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import xjunz.tool.wechat.impl.model.account.Contact;

public class ContactRepository extends AccountRepository<Contact> {
    private static ContactRepository sInstance;
    public static int sMaxCacheContactCount = 100;
    private LruCache<String, Contact> mContactCache;

    public ContactRepository() {
        this.mContactCache = new LruCache<String, Contact>(sMaxCacheContactCount);
    }

    @Override
    protected void queryAll(@NotNull List<Contact> all) {
        Cursor cursor = getDatabase().rawQuery("select username,alias,conRemark,nickname,type from rcontact where not type in (0,4,33)", null);
        while (cursor.moveToNext() || !cursor.isClosed()) {
            String id = cursor.getString(0);
            if (!TextUtils.isEmpty(id) && !id.startsWith("fake_")) {
                Contact contact = new Contact();
                contact.id = id;
                contact.alias = cursor.getString(1);
                contact.remark = cursor.getString(2);
                contact.nickname = cursor.getString(3);
                contact.rawType = cursor.getInt(4);
                contact.judgeType();
                get(contact.type).add(contact);
                all.add(contact);
            }
            if (cursor.isLast()) {
                cursor.close();
            }
        }
    }

    @Nullable
    public Contact query(@NonNull String id) {
        Cursor cursor = getDatabase().rawQuery("select alias,conRemark,nickname,type from rcontact where username='" + id + "'", null);
        if (cursor.moveToNext() || !cursor.isClosed()) {
            Contact contact = new Contact();
            contact.id = id;
            contact.alias = cursor.getString(0);
            contact.remark = cursor.getString(1);
            contact.nickname = cursor.getString(2);
            contact.rawType = cursor.getInt(3);
            contact.judgeType();
            get(contact.type).add(contact);
            if (cursor.isLast()) {
                cursor.close();
            }
            return contact;
        }
        return null;
    }

    @Override
    public Contact get(String id) {
        Contact mock = Contact.mockAccount(id);
        int index;
        if ((index = getAll().indexOf(mock)) >= 0) {
            return getAll().get(index);
        } else {
            Contact cache = mContactCache.get(id);
            if (cache == null) {
                Contact contact = query(id);
                if (contact != null) {
                    mContactCache.put(id, contact);
                    return contact;
                }
            } else {
                return cache;
            }
        }
        return null;
    }


    @Override
    public void purge() {
        sInstance = null;
    }

    public static ContactRepository getInstance() {
        return sInstance = sInstance == null ? new ContactRepository() : sInstance;
    }
}
