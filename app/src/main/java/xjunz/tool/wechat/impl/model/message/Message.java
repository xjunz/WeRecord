/*
 * Copyright (c) 2020 xjunz. 保留所有权利
 */

package xjunz.tool.wechat.impl.model.message;

import android.content.ContentValues;
import android.os.Parcel;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import xjunz.tool.wechat.impl.Environment;
import xjunz.tool.wechat.impl.model.account.Account;
import xjunz.tool.wechat.impl.model.account.User;
import xjunz.tool.wechat.impl.repo.ContactRepository;
import xjunz.tool.wechat.impl.repo.RepositoryFactory;

public class Message {
    private ContentValues values;
    public static final Integer SEND = 1;
    public static final Integer RECEIVE_FROM_PEER = 0;
    public static final Integer RECEIVE_FROM_PEER_GLOBAL = 2;
    public static final Integer RECEIVE_FROM_SYSTEM = null;
    public static final String KEY_MSG_ID = "msgId";
    public static final String KEY_CONTENT = "content";
    public static final String KEY_TYPE = "type";
    public static final String KEY_IS_SEND = "isSend";
    public static final String KEY_CREATE_TIME = "createTime";
    public static final String KEY_STATUS = "status";
    public static final String KEY_IMG_PATH = "imgPath";
    public static final String KEY_TALKER = "talker";
    /**
     * 未经处理的消息内容
     * <p>
     * 群聊消息的{@link Message#getRawContent()}前会有发送消息的微信ID加":"前缀，或者系统消息前会有
     * 群聊ID加":"前缀，后面才是发送的消息内容。<b>注：这个前缀会被去除</b>
     * </p>
     */
    protected String content;
    /**
     * 从图片名{@link Message#getImgPath()}中解析得到的本地图片路径
     *
     * @see Message#getLocalImagePath()
     */
    protected String localImagePath;
    /**
     * 消息发送者ID，如果发送者为本人或者消息是系统发送的，此值为null。如果是单人聊天，发送者为{@link Message#getTalkerId()}，
     */
    @Nullable
    protected String senderId;
    /**
     * 是否已经尝试寻找发送者
     */
    private boolean hasTriedFindingSender;
    /**
     * 此消息发送者的{@link Account}对象
     */
    protected Account senderAccount;
    /**
     * 解析后的枚举类消息类型
     *
     * @see MessageFactory#judgeType(int)
     */
    @NonNull
    protected MessageFactory.Type type = MessageFactory.Type.OTHERS;

    public ContentValues getValues() {
        return values;
    }

    public String getTalkerId() {
        return values.getAsString(KEY_TALKER);
    }

    public Message(@NonNull ContentValues values) {
        this.values = values;
        String raw = getRawContent();
        //如果消息内容不为空、不是用户本人发送的消息且是群聊
        if (raw != null && !isSend() && isInGroupChat()) {
            //格式：[微信号:(\n)][内容]
            Pattern pattern = Pattern.compile("^([0-9|A-z_@]+?):\n?");
            Matcher matcher = pattern.matcher(raw);
            if (matcher.find()) {
                this.senderId = matcher.group(1);
                this.content = raw.substring(matcher.group().length());
                return;
            }
        }
        this.content = raw;
        if (!isSend()) {
            this.senderId = getTalkerId();
        }
    }


    public boolean isInGroupChat() {
        return getTalkerId().endsWith("@chatroom");
    }

    public int getMsgId() {
        return values.getAsInteger(KEY_MSG_ID);
    }


    public int getStatus() {
        return values.getAsInteger(KEY_STATUS);
    }

    public void setStatus(int status) {
        values.put(KEY_STATUS, status);
    }

    public String getImgPath() {
        return values.getAsString(KEY_IMG_PATH);
    }

    @Nullable
    public Account getSenderAccount() {
        if (senderAccount == null && !hasTriedFindingSender) {
            if (isSend()) {
                senderAccount = Environment.getInstance().getCurrentUser();
            } else {
                ContactRepository repository = RepositoryFactory.get(ContactRepository.class);
                senderAccount = repository.get(requireSenderId());
            }
            hasTriedFindingSender = true;
        }
        return senderAccount;
    }

    @NonNull
    public String requireSenderId() {
        return Objects.requireNonNull(getSenderId(), "Got null senderId, this message may be sent by the user. ");
    }

    /**
     * 获取本条消息发送者ID，如果发送者为用户本人或者未找到发送者（如某些系统消息），返回NULL
     *
     * @return 消息发送者ID
     */
    @Nullable
    public String getSenderId() {
        return senderId;
    }


    public String getLocalImagePath() {
        String imgPath = getImgPath();
        User user = Environment.getInstance().getCurrentUser();
        if (localImagePath == null) {
            if (!TextUtils.isEmpty(imgPath)) {
                if (imgPath.startsWith("T")) {
                    int index = imgPath.lastIndexOf("_");
                    String md5 = imgPath.substring(index + 1);
                    localImagePath = user.imageCachePath + File.separator
                            + md5.substring(0, 2) + File.separator
                            + md5.substring(2, 4) + File.separator
                            + "th_" + md5;
                }
            }
        }
        return localImagePath;
    }

    public String getRawContent() {
        return values.getAsString(KEY_CONTENT);
    }


    public boolean isSend() {
        Integer isSend = values.getAsInteger(KEY_IS_SEND);
        return isSend != null && isSend.equals(SEND);
    }


    public long getCreateTimeStamp() {
        return values.getAsLong(KEY_CREATE_TIME);
    }

    public void setImgPath(String imgPath) {
        values.put(KEY_IMG_PATH, imgPath);
    }

    public void setSend(int send) {
        values.put(KEY_IS_SEND, send);
    }

    public void setCreateTimeStamp(long createTimeStamp) {
        values.put(KEY_CREATE_TIME, createTimeStamp);
    }


    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
        if (isInGroupChat()) {
            if (senderId != null) {
                values.put(KEY_CONTENT, senderId + ":\n" + content);
            }
        } else {
            values.put(KEY_CONTENT, content);
        }
    }

    public int getRawType() {
        return values.getAsInteger(KEY_TYPE);
    }

    /**
     * 返回解析后的用于比较、查找的用户所关心的消息内容。
     * 这些内容反映真实的消息信息，不包含各种富文本标签
     *
     * @return 内容
     */
    @NonNull
    public String getParsedContent() {
        return content;
    }


    /**
     * 返回用于显示的 {@link CharSequence}，此方法会解析{@code Html}富文本。
     * 暂不支持解析显示的消息，会返回其消息类型。
     *
     * @return 用于显示的 {@link CharSequence}
     */
    @NonNull
    public CharSequence getSpannedContent() {
        return content;
    }


    @NotNull
    public MessageFactory.Type getType() {
        return type;
    }

    public void setType(@NonNull MessageFactory.Type type) {
        this.type = type;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Message message = (Message) o;
        return getMsgId() == message.getMsgId();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getMsgId());
    }

    @NotNull
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (String name : values.keySet()) {
            String value = values.getAsString(name);
            if (sb.length() > 0) {
                sb.append("\n");
            }
            sb.append(name).append("=").append(value);
        }
        return sb.toString();
    }

    @NonNull
    public Message deepClone() {
        Parcel parcel = Parcel.obtain();
        parcel.writeParcelable(values, 0);
        parcel.setDataPosition(0);
        Message clone = new Message(Objects.requireNonNull(parcel.readParcelable(values.getClass().getClassLoader())));
        parcel.recycle();
        return clone;
    }

    @NonNull
    public Message deepFactoryClone() {
        Parcel parcel = Parcel.obtain();
        parcel.writeParcelable(values, 0);
        parcel.setDataPosition(0);
        Message clone = MessageFactory.createMessage((Objects.requireNonNull(parcel.readParcelable(values.getClass().getClassLoader()))));
        parcel.recycle();
        return clone;
    }
}
