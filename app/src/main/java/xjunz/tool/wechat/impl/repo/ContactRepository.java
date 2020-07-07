package xjunz.tool.wechat.impl.repo;

import android.text.TextUtils;

import net.sqlcipher.Cursor;

import xjunz.tool.wechat.impl.model.account.Contact;

public class ContactRepository extends AccountRepository<Contact> {
    private static ContactRepository sInstance;

    @Override
    public void queryAll() {
        Cursor cursor = getDatabase().rawQuery("select username,alias,conRemark,nickname,type from rcontact where not type in (0,4,33)", null);
        while (cursor.moveToNext() || !cursor.isClosed()) {
            String id = cursor.getString(0);
            if (!TextUtils.isEmpty(id) && !id.startsWith("fake_")) {
                Contact contact = new Contact();
                contact.endowIdentity(id);
                contact.alias = cursor.getString(1);
                contact.remark = cursor.getString(2);
                contact.nickname = cursor.getString(3);
                contact.rawType = cursor.getInt(4);
                contact.judgeType();
                get(contact.type).add(contact);
                mAll.add(contact);
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
       return sInstance = sInstance == null ? new ContactRepository() : sInstance;
    }
}
