/*
 * Copyright (c) 2020 xjunz. 保留所有权利
 */
package xjunz.tool.wechat.impl.model.message;

import android.content.ContentValues;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import org.jetbrains.annotations.NotNull;

import xjunz.tool.wechat.App;
import xjunz.tool.wechat.R;

/**
 * 消息工厂，用于“生产”{@link Message}对象
 */
public final class MessageFactory {

    //普通文本
    protected static final int TYPE_PLAIN_MSG = 1;
    //普通图片
    protected static final int TYPE_IMAGE = 3;
    //语音
    protected static final int TYPE_VOICE = 34;
    //好友
    protected static final int TYPE_RECOMMEND = 42;
    //视频
    protected static final int TYPE_VIDEO = 43;
    //表情图片
    protected static final int TYPE_EMOJI = 47;
    //位置
    protected static final int TYPE_LOCATION = 48;
    //分享
    protected static final int TYPE_SHARE = 49;
    //语音通话
    protected static final int TYPE_CALL = 50;
    //语音通话相关系统通知（如语音通话已结束通知）
    protected static final int TYPE_SYSTEM_CALL = 64;
    //系统通知（如撤回通知）
    protected static final int TYPE_SYSTEM = 10000;
    //请求位置信息
    protected static final int TYPE_SYSTEM_POSITION_REQUEST = 10002;
    //GIF图片
    protected static final int TYPE_GIF = 0x100031;
    //todo:未知（分享的链接，比如youtube的链接
    //估计是无法预览的分享，仍然显示的是文字
    protected static final int TYPE_16777265 = 0x01000031;
    //公众号推送（图片配文字）
    protected static final int TYPE_PUSH = 0x11000031;
    //小程序支付相关通知
    protected static final int TYPE_WCX_PAY = 0x13000031;
    //转账
    protected static final int TYPE_TRANSFER = 0x19000031;
    //红包
    protected static final int TYPE_HB = 0x1a000031;
    //加群的系统通知
    protected static final int TYPE_SYSTEM_JOIN_GROUP = 0x22000031;
    //todo:未知（图片
    protected static final int TYPE_587202609 = 0x23000031;
    //todo:未知（图片
    protected static final int TYPE_687865905 = 0x29000031;
    //回复
    protected static final int TYPE_REPLY = 0x31000031;
    //拍一拍消息
    protected static final int TYPE_SYSTEM_PAT = 0x35000031;

    //=====SUBTYPE是复杂类型消息的亚类=====
    //todo:分享（未知类型）[待定]
    protected static final int SUBTYPE_1 = 1;
    //todo:图片（来自小程序）[待定]
    protected static final int SUBTYPE_IMAGE = 2;
    //音乐
    protected static final int SUBTYPE_MUSIC = 3;
    //视频
    protected static final int SUBTYPE_VIDEO = 4;
    //公众号的通知
    protected static final int SUBTYPE_URL = 5;
    //文件
    protected static final int SUBTYPE_FILE = 6;
    //游戏（暂定）
    protected static final int SUBTYPE_GAME = 7;
    //GIF图片
    protected static final int SUBTYPE_GIF = 8;
    //位置共享
    protected static final int SUBTYPE_POSITION_SHARE = 17;
    //消息转发
    protected static final int SUBTYPE_REPOST = 19;
    //链接（分享自小程序）
    protected static final int SUBTYPE_WCX = 33;
    //分享（来自其他程序的分享）
    protected static final int SUBTYPE_SHARE = 36;
    //todo:红包（来自小程序）[待定]
    protected static final int SUBTYPE_46 = 46;
    //回复消息
    protected static final int SUBTYPE_REPLY = 57;
    //转账消息
    protected static final int SUBTYPE_TRANSFER = 2000;
    //红包消息
    protected static final int SUBTYPE_HB = 2001;

    public enum Type {
        CALL(R.string.call),
        EMOJI(R.string.emoji),
        RECOMMEND(R.string.recommend_friend),
        PLAIN(R.string.plain_text),
        IMAGE(R.string.picture),
        GIF(R.string.gif),
        PUSH(R.string.push),
        VIDEO(R.string.video),
        VOICE(R.string.voice),

        SYSTEM(R.string.system_msg),

        LOCATION(R.string.location, true),

        WCX_SHARED(R.string.wcx, true),
        SHARED_URL(R.string.shared_url, true),
        SHARE(R.string.share, true),
        REPOST(R.string.repost, true),
        FILE(R.string.file, true),
        TRANSFER(R.string.transfer, true),
        HB(R.string.hongbao, true),
        NOTIFICATION(R.string.notification, true),
        REPLY(R.string.reply, true),
        MUSIC(R.string.music, true),
        POSITION_SHARE(R.string.position_share, true),
        OTHERS(R.string.others, true);
        String caption;
        private final boolean isComplex;

        Type(@StringRes int captionRes, boolean isComplex) {
            this.caption = App.getStringOf(captionRes);
            this.isComplex = isComplex;
        }

        Type(@StringRes int captionRes) {
            this.caption = App.getStringOf(captionRes);
            this.isComplex = false;
        }

        public boolean isComplex() {
            return isComplex;
        }

        public String getCaption() {
            return caption;
        }

        public boolean isSystem() {
            return this == SYSTEM;
        }

        public boolean isPlain() {
            return this == PLAIN;
        }

    }

    @NotNull
    public static Message createMessage(@NonNull ContentValues values) {
        Type type = judgeType(values.getAsInteger(Message.KEY_TYPE));
        Message msg;
        if (type.isComplex()) {
            msg = new ComplexMessage(values);
        } else if (type.isSystem()) {
            msg = new SystemMessage(values);
        } else if (type.isPlain()) {
            msg = new PlainMessage(values);
        } else {
            msg = new UnsupportedMessage(values);
        }
        msg.setType(type);
        return msg;
    }

    @NonNull
    static Type judgeType(int rawType) {
        switch (rawType) {
            case TYPE_PLAIN_MSG:
                return Type.PLAIN;
            case TYPE_IMAGE:
                return Type.IMAGE;
            case TYPE_EMOJI:
                return Type.EMOJI;
            case TYPE_TRANSFER:
                return Type.TRANSFER;
            case TYPE_HB:
                return Type.HB;
            case TYPE_GIF:
                return Type.GIF;
            case TYPE_VOICE:
                return Type.VOICE;
            case TYPE_CALL:
                return Type.CALL;
            case TYPE_VIDEO:
                return Type.VIDEO;
            case TYPE_RECOMMEND:
                return Type.RECOMMEND;
            case TYPE_LOCATION:
                return Type.LOCATION;
            case TYPE_PUSH:
                return Type.PUSH;
            case TYPE_16777265:
            case TYPE_SHARE:
                return Type.SHARE;
            case TYPE_REPLY:
                return Type.REPLY;
            case TYPE_WCX_PAY:
                return Type.NOTIFICATION;
            case TYPE_SYSTEM_POSITION_REQUEST:
            case TYPE_SYSTEM_CALL:
            case TYPE_SYSTEM_PAT:
            case TYPE_SYSTEM_JOIN_GROUP:
            case TYPE_SYSTEM:
                return Type.SYSTEM;
        }
        return Type.OTHERS;
    }

    /**
     * todo
     * 为未识别的消息类型寻找可能的类型
     *
     * @return 可能的消息类型
     */
    private Type fallback() {
        return null;
    }
}
