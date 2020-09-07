/*
 * Copyright (c) 2020 xjunz. 保留所有权利
 */

package xjunz.tool.wechat.impl.model.account;

import org.jetbrains.annotations.NotNull;

/**
 * 群聊的实体类，一般通过{@link xjunz.tool.wechat.impl.repo.GroupRepository}构造
 */
public class Group extends Contact {
    /**
     * 群聊所有成员的ID列表
     */
    private String[] mMemberIdList;
    /**
     * 群聊成员名称（在微信的设定中，如果群聊未命名，则会以此作为名称）
     */
    public String memberDisplayName;
    /**
     * 群聊所有者ID
     */
    public String groupOwnerId;
    /**
     * 成员数量
     */
    public int memberCount;

    public Group(String id) {
        super(id);
    }

    /**
     * @param serial 群聊成员ID序列，以“;”分隔
     */
    public void setMemberIdSerial(@NotNull String serial) {
        mMemberIdList = serial.split(";");
    }

    /**
     * @return 群聊所有成员的ID列表
     */
    public String[] getMemberIdList() {
        return mMemberIdList;
    }


}
