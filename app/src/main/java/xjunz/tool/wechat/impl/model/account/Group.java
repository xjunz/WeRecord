/*
 * Copyright (c) 2020 xjunz. 保留所有权利
 */

package xjunz.tool.wechat.impl.model.account;

public class Group extends Contact {
    private String[] mMemberIDList;
    public String memberDisplayName;
    public String groupOwnerID;
    public int memberCount;

    public void setMemberIDSerial(String serial) {
        mMemberIDList = serial.split(";");
    }

    public String[] getMemberIDList() {
        return mMemberIDList;
    }


}
