/*
 * Copyright (c) 2020 xjunz. 保留所有权利
 */

package xjunz.tool.wechat.impl.repo;

import net.sqlcipher.Cursor;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import xjunz.tool.wechat.impl.model.account.Group;

public class GroupRepository extends AccountRepository<Group> {
    private static final int CACHE_CAPACITY = 20;

    GroupRepository() {
    }

    @Override
    public int getCacheCapacity() {
        return CACHE_CAPACITY;
    }

    @Override
    protected void queryAllInternal(@NotNull List<Group> all) {
        Cursor cursor = getDatabase().rawQuery("select chatroomname,memberList,displayname,roomowner,memberCount from chatroom", null);
        while (cursor.moveToNext()) {
            Group group = new Group(cursor.getString(0));
            group.setMemberIdSerial(cursor.getString(1));
            group.memberDisplayName = cursor.getString(2);
            group.groupOwnerId = cursor.getString(3);
            group.memberCount = cursor.getInt(4);
            all.add(group);
        }
        cursor.close();
    }

    @Override
    protected Group query(String id) {
        Cursor cursor = getDatabase().rawQuery("select memberList,displayname,roomowner,memberCount from chatroom where chatroomname='" + id + "'", null);
        Group group = new Group(id);
        if (cursor.moveToNext()) {
            group.setMemberIdSerial(cursor.getString(0));
            group.memberDisplayName = cursor.getString(1);
            group.groupOwnerId = cursor.getString(2);
            group.memberCount = cursor.getInt(3);
        }
        cursor.close();
        return group;
    }
}
