package xjunz.tool.wechat.impl.repo;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.collection.SimpleArrayMap;

import net.sqlcipher.Cursor;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import xjunz.tool.wechat.impl.model.account.Contact;

public class ContactRepository extends LifecyclePerceptiveRepository {
    private static ContactRepository sInstance;
    private SimpleArrayMap<Contact.Type, LinkedList<Contact>> mMap;
    private LinkedList<Contact> mAll;

    public SimpleArrayMap<Contact.Type, LinkedList<Contact>> getMap() {
        if (mMap == null) {
            throw new RuntimeException("Please call queryAll() first. ");
        }
        return mMap;
    }

    @NonNull
    public List<Contact> getAll() {
        if (mAll == null) {
            throw new RuntimeException("Please call queryAll() first. ");
        }
        return mAll;
    }

    public void queryAll() {
        mMap = new SimpleArrayMap<>();
        mAll = new LinkedList<>();
        Cursor cursor = getDatabase().rawQuery("select username,alias,conRemark,nickname,pyInitial,conRemarkPYShort,type from rcontact", null);
        while (cursor.moveToNext() || !cursor.isClosed()) {
            String id = cursor.getString(0);
            if (!TextUtils.isEmpty(id) && !id.startsWith("fake_")) {
                Contact contact = new Contact();
                contact.endowIdentity(id);
                contact.alias = cursor.getString(1);
                contact.remark = cursor.getString(2);
                contact.nickname = cursor.getString(3);
                contact.nicknamePyAbbr = cursor.getString(4);
                contact.remarkPyAbbr = cursor.getString(5);
                contact.rawType = cursor.getInt(6);
                contact.judgeType();
                Contact.Type type = contact.type;
                if (type != null) {
                    if (mMap.get(type) == null) {
                        mMap.put(type, new LinkedList<Contact>());
                    }
                    Objects.requireNonNull(mMap.get(type)).add(contact);
                    mAll.add(contact);
                }
            }
            if (cursor.isLast()) {
                cursor.close();
            }
        }
    }


    @Override
    public void purge() {
        sInstance = null;
    }

    public static ContactRepository getInstance() {
        sInstance = sInstance == null ? new ContactRepository() : sInstance;
        return sInstance;
    }


}
