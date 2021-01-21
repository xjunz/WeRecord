/*
 * Copyright (c) 2021 xjunz. 保留所有权利
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
    public static final String TABLE_CONTACT = "rcontact";
    private final SimpleArrayMap<Contact.Type, List<Contact>> mMap = new SimpleArrayMap<>();

    public boolean isNonFriendsLoaded() {
        return mNonFriendsLoaded;
    }

    private boolean mNonFriendsLoaded;

    ContactRepository() {
    }

    public Contact.Type[] getAllTypes() {
        Contact.Type[] types = new Contact.Type[mMap.size()];
        for (int i = 0; i < types.length; i++) {
            types[i] = mMap.keyAt(i);
        }
        return types;
    }

    @Override
    public int getCacheCapacity() {
        return CACHE_CAPACITY;
    }

    /**
     * 默认只加载好友，不加载全部联系人，原因：
     * 1、多数情况下用不到全部联系人，需要陌生人信息的时候单独{@link ContactRepository#query(String)}就行
     * 2、全部联系人的数量可能是十分庞大的，甚至可能有成千上万条，全部查询的时间成本和内存开销都是很大的
     */
    @Override
    protected void queryAllInternal(@NotNull List<Contact> all) {
        Cursor cursor = getDatabase().rawQuery("select username,alias,conRemark,nickname,type,lvbuff from rcontact where not type in (0,4,33)", null);
        while (cursor.moveToNext()) {
            String id = cursor.getString(0);
            if (!TextUtils.isEmpty(id) && !id.startsWith("fake_")) {
                Contact contact = new Contact(id);
                contact.alias = cursor.getString(1);
                contact.remark = cursor.getString(2);
                contact.nickname = cursor.getString(3);
                contact.rawType = cursor.getInt(4);
                contact.judgeType();
                contact.setLvBuffer(cursor.getBlob(5));
                getAllOfType(contact.type).add(contact);
                all.add(contact);
            }
        }
        cursor.close();
    }

    /**
     * 查询全部非好友联系人
     */
    public void queryNonFriends() {
        Cursor cursor = getDatabase().rawQuery("select username,alias,conRemark,nickname,type from rcontact where type in (0,4,33)", null);
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
                mAll.add(contact);
            }
        }
        mNonFriendsLoaded = true;
        cursor.close();
    }

    @Nullable
    protected Contact query(@NonNull String id) {
        try (Cursor cursor = getDatabase().rawQuery("select alias,conRemark,nickname,type,lvbuff from rcontact where username='" + id + "'", null)) {
            if (cursor.moveToNext()) {
                Contact contact = new Contact(id);
                contact.alias = cursor.getString(0);
                contact.remark = cursor.getString(1);
                contact.nickname = cursor.getString(2);
                contact.rawType = cursor.getInt(3);
                contact.judgeType();
                contact.setLvBuffer(cursor.getBlob(4));
                return contact;
            }
        }
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
