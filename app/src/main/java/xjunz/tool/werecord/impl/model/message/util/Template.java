/*
 * Copyright (c) 2021 xjunz. 保留所有权利
 */
package xjunz.tool.werecord.impl.model.message.util;

import android.content.ContentValues;
import android.os.Parcel;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.StringDef;

import org.intellij.lang.annotations.RegExp;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import xjunz.tool.werecord.impl.model.message.Message;
import xjunz.tool.werecord.impl.model.message.MessageFactory;
import xjunz.tool.werecord.impl.model.message.SystemMessage;
import xjunz.tool.werecord.impl.repo.MessageRepository;
import xjunz.tool.werecord.impl.repo.RepositoryFactory;

/**
 * 新增消息时使用的消息模板，所有消息模板都储存在数据库中，且由{@link TemplateManager}进行管理。
 * <p>
 * 消息模板包含一个{@link Message}的完整的{@link ContentValues}、名称、自定义标志以及从{@link ContentValues}
 * 里的{@link Message#KEY_CONTENT}内容中通过{@link RepGroup#MATCH_PATTERN}捕获的替换模板组。
 * </p>
 *
 * @author xjunz 2020/12/30
 * @see TemplateManager
 */
public class Template {
    public static final String KEY_NAME = "name";
    public static final String KEY_IS_CUSTOM = "isCustom";
    public static final String KEY_ID = "id";
    private final ContentValues mSource;
    private final List<RepGroup> mRepGroups = new ArrayList<>();

    public long getId() {
        Long id = mSource.getAsLong(KEY_ID);
        return id == null ? -1L : id;
    }

    public ContentValues getValues() {
        return mSource;
    }

    public String getName() {
        return mSource.getAsString(KEY_NAME);
    }

    public List<RepGroup> getRepGroups() {
        return mRepGroups;
    }

    public boolean isCustom() {
        return mSource.getAsBoolean(KEY_IS_CUSTOM);
    }

    @NotNull
    @Contract("_ -> new")
    public static Template fromLocal(ContentValues values) {
        Template template = new Template(values, values.getAsString(KEY_NAME), values.getAsBoolean(KEY_IS_CUSTOM));
        template.compat();
        return template;
    }

    /**
     * 从{@link Message}中生成自定义模板，名称默认为其{@link xjunz.tool.werecord.impl.model.message.MessageFactory.Type}的{@code caption}
     *
     * @param message 源消息
     * @return 生成的模板
     */
    @NotNull
    @Contract("_ -> new")
    public static Template fromMessage(@NotNull Message message) {
        ContentValues source = message.deepClone().getValues();
        ContentValues values = new ContentValues();
        for (String key : source.keySet()) {
            switch (key) {
                case Message.KEY_CONTENT:
                    //将content字段设为不包含sender id的纯内容，因为sender id需要手动设置，不能包含在模板里
                    values.put(key, message.getContent());
                    break;
                case Message.KEY_TYPE:
                    //必须保留type字段
                    values.put(key, source.getAsLong(key));
                    break;
                case "bizChatId":
                    //这个东西默认值是-1，设为默认值
                    values.put("bizChatId", -1);
                    break;
                case Message.KEY_LV_BUFFER:
                    //TODO:暂且保留LvBuffer
                    values.put(key, source.getAsByteArray(key));
                    break;
                case Message.KEY_IS_SEND:
                    if (!(message instanceof SystemMessage)) {
                        //如果不是系统消息，重置为发送
                        values.put(key, Message.SEND);
                    } else {
                        //否则保留IS_SEND字段内容
                        values.put(key, source.getAsLong(key));
                    }
                    break;
                case Message.KEY_STATUS:
                    if (!(message instanceof SystemMessage)) {
                        //如果不是系统消息，重置为发送成功
                        values.put(key, Message.STATUS_SEND_SUC);
                    } else {
                        //否则保留STATUS字段内容
                        values.put(key, source.getAsLong(key));
                    }
                    break;
                case Message.KEY_MSG_ID:
                    //消息ID设为-1
                    values.put(key, -1);
                    break;
                default:
                    //其他字段，全部置空
                    values.putNull(key);
                    break;
            }
        }
        return new Template(values, message.getType().getCaption(), true);
    }

    public String getContent() {
        return mSource.getAsString(Message.KEY_CONTENT);
    }

    public void setContent(String content) {
        mSource.put(Message.KEY_CONTENT, content);
    }

    public void setName(String name) {
        mSource.put(KEY_NAME, name);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Template template = (Template) o;
        return Objects.equals(getId(), template.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }

    @Nullable
    @Contract(pure = true)
    private RepGroup containsRepGroup(String name) {
        for (RepGroup repGroup : mRepGroups) {
            if (Objects.equals(repGroup.name, name)) {
                return repGroup;
            }
        }
        return null;
    }

    public static class TypeNotConsistentException extends Exception {
        public int[] conflictSelection;

        TypeNotConsistentException(int[] conflictSelection) {
            super("Type inconsistency detected. Make sure all rep-groups with the same name have the same type.");
            this.conflictSelection = conflictSelection;
        }
    }

    private void initRepGroupSafe() {
        try {
            initRepGroups();
        } catch (TypeNotConsistentException e) {
            Log.e(getClass().getSimpleName(), "This is not expected to happen.");
            e.printStackTrace();
        }
    }

    /**
     * 初始化所有替换组
     */
    public void initRepGroups() throws TypeNotConsistentException {
        mRepGroups.clear();
        Pattern p = Pattern.compile(RepGroup.MATCH_PATTERN, Pattern.DOTALL);
        Matcher m = p.matcher(mSource.getAsString(Message.KEY_CONTENT));
        while (m.find()) {
            String repName = m.group(2);
            //先判断同名替换组是已经存在
            RepGroup repGroup = containsRepGroup(repName);
            //如果不存在
            if (repGroup == null) {
                repGroup = new RepGroup();
                repGroup.indices.add(new int[]{m.start(), m.end()});
                repGroup.rawType = m.group(1);
                repGroup.name = repName;
                mRepGroups.add(repGroup);
            } else {
                //如果存在且类型不一样，抛出异常
                if (!repGroup.isTypeOf(m.group(1))) {
                    mRepGroups.clear();
                    throw new TypeNotConsistentException(new int[]{m.start(), m.end()});
                }
                //否则只是记录索引
                repGroup.indices.add(new int[]{m.start(), m.end()});
            }
        }
    }

    /**
     * 兼容：旧版本不兼容Template里面的一些字段，比如historyId在旧版本的message表中没有此字段
     */
    private void compat() {
        MessageRepository repository = RepositoryFactory.get(MessageRepository.class);
        List<String> keysToRemove = new ArrayList<>();
        for (String key : mSource.keySet()) {
            if (repository.getType(key) == null) {
                keysToRemove.add(key);
            }
        }
        keysToRemove.remove(KEY_ID);
        keysToRemove.remove(KEY_IS_CUSTOM);
        keysToRemove.remove(KEY_NAME);
        for (String key : keysToRemove) {
            mSource.remove(key);
        }
    }

    private Template(@NonNull ContentValues values, String name, boolean isCustom) {
        //设置name和isCustom
        values.put(KEY_IS_CUSTOM, isCustom);
        values.put(KEY_NAME, name);
        mSource = values;
        initRepGroupSafe();
    }


    public static class RepGroup {
        @RegExp
        private static final String MATCH_PATTERN = "\\$([SDLINTA]?)\\{(.+?)\\}";
        public String name;
        private String rawType;
        public List<int[]> indices = new ArrayList<>();
        //普通文本(默认)
        public static final String TYPE_STRING = "S";
        //整数
        public static final String TYPE_LONG = "L";
        //小数或整数
        public static final String TYPE_DECIMAL = "D";
        //账号ID（仅限当前范围）
        public static final String TYPE_ACCOUNT_ID = "I";
        //账号名（仅限当前范围）
        public static final String TYPE_ACCOUNT_NAME = "N";
        //时间戳
        public static final String TYPE_TIMESTAMP = "T";
        //APP ID
        public static final String TYPE_APP_ID = "A";

        @StringDef({TYPE_STRING, TYPE_LONG, TYPE_DECIMAL, TYPE_ACCOUNT_ID, TYPE_ACCOUNT_NAME, TYPE_TIMESTAMP, TYPE_APP_ID})
        @Retention(RetentionPolicy.SOURCE)
        public @interface RepType {
        }

        @NonNull
        @RepType
        public String getType() {
            if (TextUtils.isEmpty(rawType)) {
                return TYPE_STRING;
            }
            return rawType;
        }

        public boolean isTypeOf(@Nullable String type) {
            if (TextUtils.isEmpty(type)) {
                type = TYPE_STRING;
            }
            return Objects.equals(type, rawType);
        }


        public String[] getReplacementPattern() {
            return isTypeOf(TYPE_STRING) ? new String[]{"${" + name + "}", "$" + TYPE_STRING + "{" + name + "}"} : new String[]{"$" + rawType + "{" + name + "}"};
        }

    }

    private ContentValues cloneValues() {
        Parcel parcel = Parcel.obtain();
        parcel.writeParcelable(mSource, 0);
        parcel.setDataPosition(0);
        ContentValues values = parcel.readParcelable(ContentValues.class.getClassLoader());
        parcel.recycle();
        return values;
    }

    /**
     * 从Template生成{@link Message}，每次生成的Message都是互为深度克隆的，不必担心共用变量导致污染问题。
     * 因为模板的talkerId和时间戳是被抹掉的，但是这两个字段是必要的，因此必须提供这两个参数。
     */
    public Message toMessage(String talkerId, long defTimestamp) {
        ContentValues clonedValues = cloneValues();
        clonedValues.remove(KEY_ID);
        clonedValues.remove(KEY_NAME);
        clonedValues.remove(KEY_IS_CUSTOM);
        clonedValues.put(Message.KEY_TALKER, talkerId);
        clonedValues.put(Message.KEY_CREATE_TIME, defTimestamp);
        return MessageFactory.createMessage(clonedValues);
    }

}
