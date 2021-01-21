/*
 * Copyright (c) 2021 xjunz. 保留所有权利
 */

package xjunz.tool.wechat.impl.model.message;

import android.content.ContentValues;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import xjunz.tool.wechat.App;
import xjunz.tool.wechat.impl.Environment;
import xjunz.tool.wechat.impl.model.account.Account;
import xjunz.tool.wechat.impl.model.account.Group;
import xjunz.tool.wechat.impl.model.account.User;
import xjunz.tool.wechat.impl.model.message.util.Edition;
import xjunz.tool.wechat.impl.model.message.util.LvBufferUtils;
import xjunz.tool.wechat.impl.repo.ContactRepository;
import xjunz.tool.wechat.impl.repo.GroupRepository;
import xjunz.tool.wechat.impl.repo.RepositoryFactory;
import xjunz.tool.wechat.util.Utils;

/**
 * 消息对象，是数据库中"message"表的数据的封装。所有消息来源于{@link MessageFactory#createMessage(ContentValues)}。
 * 需要注意的是，消息中所有数字类型的字段都是{@link Long}类型。
 *
 * @see MessageFactory
 * @see xjunz.tool.wechat.impl.repo.MessageRepository
 */
public class Message implements Parcelable {
    private final ContentValues values;
    /**
     * 发送
     */
    public static final Integer SEND = 1;
    /**
     * 对点接收
     */
    public static final Integer RECEIVE_FROM_PEER = 0;
    /**
     * 全局对点接收
     */
    public static final Integer RECEIVE_FROM_PEER_GLOBAL = 2;
    /**
     * 系统消息接收
     */
    public static final Integer RECEIVE_FROM_SYSTEM = null;
    /**
     * 发送成功
     */
    public static final Integer STATUS_SEND_SUC = 2;
    /**
     * 接收成功
     */
    public static final Integer STATUS_RECEIVE_SUC = 3;
    /**
     * 全体可见的系统消息
     */
    public static final Integer STATUS_SYSTEM = 4;
    /**
     * 发送失败
     */
    public static final Integer STATUS_SEND_FAILED = 5;
    /**
     * 仅自己可见的一些系统消息
     */
    public static final Integer STATUS_LOCAL = null;
    public static final String KEY_MSG_ID = "msgId";
    public static final String KEY_CONTENT = "content";
    public static final String KEY_TYPE = "type";
    public static final String KEY_IS_SEND = "isSend";
    public static final String KEY_CREATE_TIME = "createTime";
    public static final String KEY_STATUS = "status";
    public static final String KEY_IMG_PATH = "imgPath";
    public static final String KEY_TALKER = "talker";
    public static final String KEY_LV_BUFFER = "lvbuffer";
    //我们自定义的edition
    public static final String KEY_EDITION = "edition";
    //不存在于数据库中的KEY，为了统一设置的
    public static final String PREFIX_ABSTRACT = "abstracted_";
    //发送者
    public static final String ABSTRACT_KEY_SENDER_ID = PREFIX_ABSTRACT + "sender_id";
    //解析后的内容
    public static final String ABSTRACT_KEY_CONTENT = PREFIX_ABSTRACT + "content";
    //解析后的LvBuffer
    public static final String ABSTRACT_KEY_LVBUFFER = PREFIX_ABSTRACT + "lvbuffer";
    /**
     * 未经处理的消息内容
     * <p>
     * 群聊消息的{@link Message#getRawContent()}前会有发送消息的微信ID加":"前缀，后面才是发送的消息内容。此字段不包含这些前缀。
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
     * 解析后的枚举类消息类型
     *
     * @see MessageFactory#createMessage(ContentValues)
     */
    protected MessageFactory.Type type;
    protected int editionFlag = Edition.FLAG_NONE;
    protected CharSequence spannedContent;
    protected String parsedContent;
    /**
     * 解析错误码，当消息解析遇到已知的错误时，会
     */
    protected int parseErrorCode = -1;
    protected Object[] parsedLvBuffer;
    public static final int[] LV_BUFFER_READ_SERIAL = {0, 1, 0, 1, 1, 1, 1, 1, 1, 0, 0, 0, 1, 0, 2, 0, 0, 1, 1};
    public static int PARSE_ERROR_SENDER_ID = 1;
    public static int PARSE_ERROR_LV_BUFFER = 2;

    public static final Creator<Message> CREATOR = new Creator<Message>() {
        @NotNull
        @Contract("_ -> new")
        @Override
        public Message createFromParcel(Parcel in) {
            return new Message(in);
        }

        @NotNull
        @Contract(value = "_ -> new", pure = true)
        @Override
        public Message[] newArray(int size) {
            return new Message[size];
        }
    };

    public ContentValues getValues() {
        return values;
    }

    public String getTalkerId() {
        return values.getAsString(KEY_TALKER);
    }

    public Message(@NonNull ContentValues values, MessageFactory.Type type) {
        this.values = values;
        this.type = type;
        if (isBackup()) {
            //如果是备份消息，读取其编辑标志
            this.editionFlag = values.getAsInteger(KEY_EDITION);
        }
        parseSenderIdAndContent(values.getAsString(KEY_CONTENT));
        readLvBuffer();
    }

    public void readLvBuffer() {
        byte[] buffer = values.getAsByteArray(KEY_LV_BUFFER);
        try {
            LvBufferUtils utils = new LvBufferUtils();
            parsedLvBuffer = utils.readLvBuffer(buffer, LV_BUFFER_READ_SERIAL);
        } catch (Exception e) {
            //e.printStackTrace();
            parseErrorCode = PARSE_ERROR_LV_BUFFER;
        }
    }

    @Nullable
    public Object[] getParsedLvBuffer() {
        return parsedLvBuffer;
    }

    /**
     * @return 消息是否解析失败
     */
    public boolean isParseError() {
        return parseErrorCode != -1;
    }

    /**
     * @return 当前消息解析失败的错误码，无错误返回-1
     */
    public int getParseErrorCode() {
        return parseErrorCode;
    }

    //TODO:解析错误调试
    private void parseSenderIdAndContent(String raw) {
        //如果消息内容不是用户本人发送的消息且是群聊
        //注意：系统消息也可能是有发送者的（Talker）
        if (raw != null && !isSend() && isInGroupChat()) {
            //格式：[微信号:(\n)][内容]
            Pattern pattern = Pattern.compile("^([0-9|A-z_@]+?):\n?");
            Matcher matcher = pattern.matcher(raw);
            if (matcher.find()) {
                this.senderId = matcher.group(1);
                this.content = raw.substring(matcher.group().length());
                return;
            } else if (type != MessageFactory.Type.SYSTEM) {
                //如果不是系统消息且未找到，标记为解析错误
                parseErrorCode = PARSE_ERROR_SENDER_ID;
            }
        }
        this.content = raw;
        if (!isSend()) {
            this.senderId = getTalkerId();
        } else {
            this.senderId = Environment.getInstance().getCurrentUser().id;
        }
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public <T> T get(@NotNull String key) {
        switch (key) {
            case ABSTRACT_KEY_CONTENT:
                return (T) getContent();
            case ABSTRACT_KEY_SENDER_ID:
                return (T) getSenderId();
            case ABSTRACT_KEY_LVBUFFER:
                return (T) getParsedLvBuffer();
            default:
                return (T) values.get(key);
        }
    }

    public void modify(@NotNull String key, Object content) {
        switch (key) {
            case ABSTRACT_KEY_CONTENT:
                modifyContent((String) content);
                break;
            case ABSTRACT_KEY_SENDER_ID:
                modifySenderId((String) content);
                break;
            case ABSTRACT_KEY_LVBUFFER:
                modifyLvBuffer((Object[]) content);
                break;
            case KEY_CONTENT:
                String rawContent = ((String) content);
                values.put(KEY_CONTENT, rawContent);
                //如果修改了raw content,我们需要重新解析senderId和content
                parseSenderIdAndContent(rawContent);
                break;
            default:
                if (values.containsKey(key)) {
                    putObject(values, key, content);
                }
                break;
        }
    }

    static void putObject(ContentValues values, String key, Object value) {
        if (value == null) {
            values.putNull(key);
        } else if (value instanceof String) {
            values.put(key, (String) value);
        } else if (value instanceof Integer || value instanceof Long) {
            values.put(key, ((Number) value).longValue());
        } else if (value instanceof Boolean) {
            values.put(key, (Boolean) value);
        } else if (value instanceof byte[]) {
            values.put(key, (byte[]) value);
        } else {
            throw new IllegalArgumentException("Unsupported type " + value.getClass());
        }
    }

    public void setEditionFlag(int flag) {
        if (isBackup()) {
            throw new RuntimeException("You cannot set the edition flag of a backup message: " + getMsgId());
        }
        this.editionFlag = flag;
    }

    public boolean isEdited() {
        return this.editionFlag != Edition.FLAG_NONE;
    }

    public int getEditionFlag() {
        return editionFlag;
    }

    /**
     * 去除编辑标记，如果是备份消息，删除{@link Message#KEY_EDITION}键及其值
     */
    public void removeEdition() {
        values.remove(KEY_EDITION);
        setEditionFlag(Edition.FLAG_NONE);
    }

    public boolean isInGroupChat() {
        return getTalkerId().endsWith("@chatroom");
    }

    @Nullable
    public Group getGroupTalker() {
        if (isInGroupChat()) {
            GroupRepository groupRepository = RepositoryFactory.get(GroupRepository.class);
            return groupRepository.get(getTalkerId());
        }
        return null;
    }

    public long getMsgId() {
        return values.getAsLong(KEY_MSG_ID);
    }

    public String getMsgIdAsString() {
        return values.getAsString(KEY_MSG_ID);
    }

    public boolean supportModifySendStatus() {
        return isSend() && (Objects.equals(getStatus(), STATUS_SEND_SUC) || Objects.equals(getStatus(), STATUS_SEND_FAILED));
    }

    public Integer getStatus() {
        return values.getAsInteger(KEY_STATUS);
    }

    public boolean sendFailed() {
        return Objects.equals(getStatus(), STATUS_SEND_FAILED);
    }

    public void setStatus(Integer status) {
        values.put(KEY_STATUS, (long) status);
    }

    public String getImgPath() {
        return values.getAsString(KEY_IMG_PATH);
    }

    public void setSendFlag(Integer sendFlag) {
        values.put(KEY_IS_SEND, (long) sendFlag);
    }

    public void modifyLvBuffer(@Nullable Object[] parsed) {
        parsedLvBuffer = parsed;
        if (parsed == null) {
            values.putNull(KEY_LV_BUFFER);
            return;
        }
        LvBufferUtils utils = new LvBufferUtils();
        values.put(KEY_LV_BUFFER, utils.generateLvBuffer(parsed, LV_BUFFER_READ_SERIAL));
    }

    public void modifySenderId(@NonNull String newSenderId) {
        if (senderId == null || newSenderId.equals(senderId)) {
            return;
        }
        //如果消息内容不为空、不是用户本人发送的消息且是群聊
        String raw = getRawContent();
        String userId = Environment.getInstance().getCurrentUser().id;
        //如果是群聊
        if (isInGroupChat()) {
            //如果是发送的消息
            if (isSend()) {
                //变成接收消息
                setSendFlag(RECEIVE_FROM_PEER);
                //在消息前加上ID和冒号
                values.put(KEY_CONTENT, newSenderId + ":\n" + raw);
            } else {
                //如果是接收的消息
                //且新的发送者为用户本身
                if (newSenderId.equals(userId)) {
                    //变成发送的消息
                    setSendFlag(SEND);
                    //删去消息前的ID和冒号
                    //如果有换行，去掉换行
                    String newMsg = raw.substring(requireSenderId().length() + 1);
                    if (newMsg.startsWith("\n")) {
                        values.put(KEY_CONTENT, newMsg.substring(1));
                    } else {
                        values.put(KEY_CONTENT, newMsg);
                    }
                } else {
                    //替换掉原来的ID
                    values.put(KEY_CONTENT, newSenderId + raw.substring(requireSenderId().length()));
                }
            }
        } else {
            //如果是单人聊天
            //如果是发送
            if (isSend()) {
                //变成接收
                setSendFlag(RECEIVE_FROM_PEER);
            } else {
                //否则变成发送
                setSendFlag(SEND);
            }
        }
        this.senderId = newSenderId;
    }

    /**
     * Optimized equal method between two {@link ContentValues}es. More concretely, this method will
     * compare byte array type(if there is) values with {@link Utils#byteArrayDeepEquals(byte[], byte[])} instead of
     * simply invoking {@link Object[]#equals(Object)}.
     */
    static boolean valuesDeepEqual(@NonNull ContentValues a, @NonNull ContentValues b) {
        if (a == b) {
            return true;
        }
        if (a.size() != b.size()) {
            return false;
        }
        for (String key : b.keySet()) {
            if (!a.containsKey(key)) {
                return false;
            }
            Object aValue = a.get(key);
            Object bValue = b.get(key);
            if (aValue == bValue) {
                continue;
            }
            if (aValue == null || bValue == null) {
                return false;
            }
            if (!aValue.getClass().equals(bValue.getClass())) {
                return false;
            } else if (aValue instanceof byte[]) {
                if (!Utils.byteArrayDeepEquals(((byte[]) aValue), ((byte[]) bValue))) {
                    return false;
                }
            } else if (!Objects.equals(aValue, bValue)) {
                return false;
            }
        }
        return true;
    }

    public boolean deepEquals(@NotNull Message message) {
        if (this == message) {
            return true;
        }
        return valuesDeepEqual(message.values, values);
    }

    @Nullable
    public Account getSenderAccount() {
        if (senderId == null) {
            return null;
        }
        if (isSend()) {
            return Environment.getInstance().getCurrentUser();
        } else {
            ContactRepository repository = RepositoryFactory.get(ContactRepository.class);
            return repository.get(requireSenderId());
        }
    }

    @NonNull
    public Account requireSenderAccount() {
        return Objects.requireNonNull(getSenderAccount(), "Got null senderId, this message might be a system message.");
    }

    @NonNull
    public String requireSenderId() {
        return Objects.requireNonNull(getSenderId(), "Got null senderId, this message might be a system message.");
    }


    /**
     * 获取本条消息发送者ID，如果未找到发送者（如某些系统消息），返回NULL
     * 注意，某些系统消息也会有发送者，发送者通常是群聊本身
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

    public void setCreateTimeStamp(long createTimeStamp) {
        values.put(KEY_CREATE_TIME, createTimeStamp);
    }

    /**
     * @return 消息内容，不包含前缀。有可能为Null，如某些图片消息。
     */
    @Nullable
    public String getContent() {
        return content;
    }

    public void modifyContent(String content) {
        this.parseErrorCode = -1;
        this.content = content;
        this.spannedContent = null;
        this.parsedContent = null;
        if (isInGroupChat() && !isSend() && senderId != null) {
            values.put(KEY_CONTENT, senderId + ":\n" + content);
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
        return parsedContent;
    }


    /**
     * 返回用于显示的 {@link CharSequence}，此方法会解析{@code Html}富文本。
     * 暂不支持解析显示的消息，会返回其消息类型。
     *
     * @return 用于显示的 {@link CharSequence}
     */
    @NonNull
    public CharSequence getSpannedContent() {
        return spannedContent;
    }


    @NotNull
    public MessageFactory.Type getType() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Message)) {
            return false;
        }
        Message message = (Message) o;
        return getMsgId() == message.getMsgId();
    }

    /**
     * @return 返回一个消息是否为备份消息，备份消息来自于"WeBackup"表而不是"message"表
     */
    public boolean isBackup() {
        return values.containsKey(KEY_EDITION);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getMsgId());
    }

    @NotNull
    @Override
    public String toString() {
        return values.toString();
    }


    @NonNull
    public Message deepClone() {
        Parcel parcel = Parcel.obtain();
        parcel.writeParcelable(this, 0);
        parcel.setDataPosition(0);
        Message message = parcel.readParcelable(getClass().getClassLoader());
        parcel.recycle();
        return message;
    }

    public String getEditionFlagCaption() {
        int res = Edition.getEditionFlagCaptionOf(getEditionFlag());
        if (res == -1) {
            return null;
        } else {
            return App.getStringOf(res);
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NotNull Parcel dest, int flags) {
        dest.writeParcelable(this.values, flags);
        dest.writeString(this.content);
        dest.writeString(this.localImagePath);
        dest.writeString(this.senderId);
        dest.writeInt(this.type == null ? -1 : this.type.ordinal());
        dest.writeInt(this.editionFlag);
        dest.writeString(this.parsedContent);
        dest.writeInt(this.parseErrorCode);
        dest.writeArray(this.parsedLvBuffer);
    }

    protected Message(@NotNull Parcel in) {
        this.values = in.readParcelable(ContentValues.class.getClassLoader());
        this.content = in.readString();
        this.localImagePath = in.readString();
        this.senderId = in.readString();
        int tmpType = in.readInt();
        this.type = tmpType == -1 ? null : MessageFactory.Type.values()[tmpType];
        this.editionFlag = in.readInt();
        this.parsedContent = in.readString();
        this.parseErrorCode = in.readInt();
        this.parsedLvBuffer = in.readArray(Object[].class.getClassLoader());
    }

    private Message(ContentValues values) {
        this.values = values;
    }

    @NotNull
    @Contract("_, _ -> new")
    public static Message createEmptyMessage(String talkerId, int type) {
        ContentValues values = new ContentValues();
        values.put(KEY_TALKER, talkerId);
        values.put(KEY_TYPE, type);
        return new Message(values);
    }

}
