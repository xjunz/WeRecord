/*
 * Copyright (c) 2020 xjunz. 保留所有权利
 */

package xjunz.tool.wechat.impl.repo;

import net.sqlcipher.Cursor;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import xjunz.tool.wechat.impl.model.account.Group;

public class GroupRepository extends AccountRepository<Group> {

    GroupRepository() {
    }

    @Override
    protected void queryAll(@NotNull List<Group> all) {
        Cursor cursor = getDatabase().rawQuery("select chatroomname,memberList,displayname,roomowner,memberCount from chatroom", null);
        Group group = new Group();
        while (cursor.moveToNext() || !cursor.isClosed()) {
            group.id = cursor.getString(0);
            group.setMemberIDSerial(cursor.getString(1));
            group.memberDisplayName = cursor.getString(2);
            group.groupOwnerID = cursor.getString(3);
            group.memberCount = cursor.getInt(4);
            all.add(group);
        }
        cursor.close();
    }

    @Override
    public Group get(String id) {
        Cursor cursor = getDatabase().rawQuery("select memberList,displayname,roomowner,memberCount from chatroom where chatroomname='" + id + "'", null);
        Group group = new Group();
        group.id = id;
        if (cursor.moveToNext() || !cursor.isClosed()) {
            group.setMemberIDSerial(cursor.getString(0));
            group.memberDisplayName = cursor.getString(1);
            group.groupOwnerID = cursor.getString(2);
            group.memberCount = cursor.getInt(3);
        }
        cursor.close();
        return group;
    }
}
