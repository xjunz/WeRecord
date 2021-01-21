/*
 * Copyright (c) 2021 xjunz. 保留所有权利
 */
package xjunz.tool.wechat.impl.model.message;

import android.content.ContentValues;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import xjunz.tool.wechat.App;
import xjunz.tool.wechat.R;
import xjunz.tool.wechat.impl.repo.MessageRepository;
import xjunz.tool.wechat.impl.repo.RepositoryFactory;

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
    //好友推荐、名片
    protected static final int TYPE_CARD = 42;
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
    //通知（如小程序支付）
    protected static final int TYPE_NOTIFICATION = 0x13000031;
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
    //接龙消息
    protected static final int TYPE_SOLITAIRE = 0x30000031;
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
    //通知
    protected static final int SUBTYPE_NOTIFICATION = 46;
    //回复消息
    protected static final int SUBTYPE_REPLY = 57;
    //转账消息
    protected static final int SUBTYPE_TRANSFER = 2000;
    //红包消息
    protected static final int SUBTYPE_HB = 2001;

    public enum Type {
        /**
         * {@link PlainMessage}
         */
        PLAIN(R.string.msg_type_plain_text),
        /**
         * {@link SystemMessage}
         */
        SYSTEM(R.string.msg_type_system),
        /**
         * {@link CallMessage}
         */
        VOICE_CALL(R.string.msg_type_voice_call),
        VIDEO_CALL(R.string.msg_type_video_call),
        CALL(R.string.msg_type_call),
        /**
         * {@link UnpreviewableMessage}
         */
        EMOJI(R.string.msg_type_emoji),
        IMAGE(R.string.msg_type_picture),
        GIF(R.string.msg_type_gif),
        VIDEO(R.string.msg_type_video),
        VOICE(R.string.msg_type_voice),
        PUSH(R.string.msg_type_push),
        UNKNOWN(R.string.msg_type_unknown),
        /**
         * {@link CardMessage}
         */
        CARD(R.string.msg_type_card),
        /**
         * {@link AppMessage}
         */
        LOCATION(R.string.msg_type_location),
        WCX_SHARED(R.string.msg_type_wcx),
        SHARED_URL(R.string.msg_type_shared_url),
        SHARE(R.string.msg_type_share),
        REPOST(R.string.msg_type_repost),
        FILE(R.string.msg_type_file),
        TRANSFER(R.string.msg_type_transfer),
        HB(R.string.msg_type_hb),
        NOTIFICATION(R.string.msg_type_notification),
        REPLY(R.string.msg_type_reply),
        MUSIC(R.string.msg_type_music),
        POSITION_SHARE(R.string.msg_type_position_share),
        SOLITAIRE(R.string.msg_type_solitaire);
        String caption;


        Type(@StringRes int captionRes) {
            this.caption = App.getStringOf(captionRes);
        }

        public String getCaption() {
            return caption;
        }
    }


    private static final int[] TYPE_ARRAY_IMAGE = {TYPE_IMAGE, 13, 23, 33, 39};

    @NotNull
    public static Message createMessage(@NonNull ContentValues values) {
        int rawType = values.getAsInteger(Message.KEY_TYPE);
        switch (rawType) {
            case TYPE_PLAIN_MSG:
            case 11:
            case 21:
            case 31:
            case 36:
                return new PlainMessage(values);
            case TYPE_IMAGE:
                return new UnpreviewableMessage(values, Type.IMAGE);
            case TYPE_EMOJI:
                return new UnpreviewableMessage(values, Type.EMOJI);
            case TYPE_SYSTEM:
            case TYPE_SYSTEM_JOIN_GROUP:
            case TYPE_SYSTEM_CALL:
            case TYPE_SYSTEM_PAT:
            case TYPE_SYSTEM_POSITION_REQUEST:
            case 0x37000031:
                return new SystemMessage(values);
            case TYPE_TRANSFER:
                return new AppMessage(values, Type.TRANSFER);
            case TYPE_HB:
                return new AppMessage(values, Type.HB);
            case TYPE_GIF:
                return new UnpreviewableMessage(values, Type.GIF);
            case TYPE_VOICE:
                return new UnpreviewableMessage(values, Type.VOICE);
            case TYPE_CALL:
                return new CallMessage(values);
            case TYPE_VIDEO:
                return new UnpreviewableMessage(values, Type.VIDEO);
            case TYPE_CARD:
                return new CardMessage(values);
            case TYPE_LOCATION:
                return new AppMessage(values, Type.LOCATION);
            case TYPE_PUSH:
                return new UnpreviewableMessage(values, Type.PUSH);
            case TYPE_16777265:
            case TYPE_SHARE:
                return new AppMessage(values, Type.SHARE);
            case TYPE_REPLY:
                return new AppMessage(values, Type.REPLY);
            case TYPE_NOTIFICATION:
                return new AppMessage(values, Type.NOTIFICATION);
            case TYPE_SOLITAIRE:
                return new AppMessage(values, Type.SOLITAIRE);
        }
        return fallback(values);
    }

    /**
     * 为未识别的消息类型寻找可能的类型
     *
     * @return 可能的消息类型
     */
    @NotNull
    @Contract(pure = true)
    private static Message fallback(@NotNull ContentValues values) {
        MessageRepository repository = RepositoryFactory.get(MessageRepository.class);
        ContentValues appValues = repository.queryAppContentValuesByMsgId(values.getAsInteger(Message.KEY_MSG_ID));
        if (appValues != null) {
            return new AppMessage(values, Type.UNKNOWN);
        }
        return new UnpreviewableMessage(values, Type.UNKNOWN);
    }
}
