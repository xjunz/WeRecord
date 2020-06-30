package xjunz.tool.wechat.impl.model.account;

import android.text.TextUtils;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import xjunz.tool.wechat.App;
import xjunz.tool.wechat.R;
import xjunz.tool.wechat.impl.Environment;
import xjunz.tool.wechat.impl.repo.ContactRepository;
import xjunz.tool.wechat.ui.activity.main.model.SortBy;
import xjunz.tool.wechat.util.UniUtils;

public class Contact extends Account implements Comparable<Contact> {
    public static final boolean DEFAULT_IS_ASCENDING = true;
    private static boolean sAscending = DEFAULT_IS_ASCENDING;
    public String remark;
    /**
     * 从数据库查询得到的type字段
     */
    public int rawType;
    /**
     * 处理后的枚举类型
     */
    public Type type;
    /**
     * rawType: 微信官方账号
     */
    protected static final int RAW_TYPE_OFFICIAL = 33;
    /**
     * rawType: 陌生人
     */
    protected static final int RAW_TYPE_UNSAVED_FRIEND = 4;
    /**
     * rawType: 1或3是保存的公众号，好友，群聊；暂时不知道区别
     */
    @Keep
    protected static final int RAW_TYPE_SAVED_3 = 3;
    @Keep
    protected static final int RAW_TYPE_SAVED_1 = 1;
    /**
     * rawType: 我方删除的好友，公众号，群聊
     */
    protected static final int RAW_TYPE_DELETED = 0;
    /**
     * rawType: 未保存的群聊
     */
    protected static final int RAW_TYPE_UNSAVED_GROUP = 2;
    /**
     * 从数据库中查询得到的昵称拼音缩写
     */
    public String nicknamePyAbbr;
    /**
     * 从数据库中查询得到的备注拼音缩写
     */
    public String remarkPyAbbr;
    /**
     * 处理得到的用于排序的名称拼音缩写
     */
    private String comparatorPyAbbr;

    public Contact() {
        super(Environment.getInstance().getCurrentUin());
    }

    /**
     * 设置排序顺序（是否升序）
     * 用于{@link Contact#compareTo(Contact)}
     *
     * @param ascending 是否升序
     */
    public static void setOrderBy(boolean ascending) {
        sAscending = ascending;
    }


    /**
     * 获取用于排序的名称拼音缩写，名称的优先级为备注、昵称、微信号、微信ID
     * 首字母ASCII码小于‘0’或者大于‘9’且小于‘A’的字符前会加上"#",代表符号开头的名称，
     * 大于‘z’的字符前会加上"?",代表其他类型的字符开头的名称（非符号、数字、字母），
     * 这样它们就会被分类在一起进行排序
     *
     * @return 用于排序的名称拼音缩写
     */
    protected String getComparatorPyAbbr() {
        if (comparatorPyAbbr == null) {
            if (!empty(remark)) {
                comparatorPyAbbr = remarkPyAbbr;
            } else {
                if (!empty(nicknamePyAbbr)) {
                    comparatorPyAbbr = nicknamePyAbbr;
                } else {
                    comparatorPyAbbr = UniUtils.getPinYinAbbr(getName());
                }
            }
            char first = comparatorPyAbbr.charAt(0);
            if (first < '0' || (first > '9' && first < 'A') || (first > 'Z' && first < 'a')) {
                comparatorPyAbbr = "#" + comparatorPyAbbr;
            } else if (first > 'z') {
                comparatorPyAbbr = "?" + comparatorPyAbbr;
            }
        }
        return comparatorPyAbbr.toUpperCase();
    }

    @Override
    public int compareTo(@NotNull Contact o) {
        return (sAscending ? 1 : -1) * getComparatorPyAbbr().compareTo(o.getComparatorPyAbbr());
    }


    /**
     * 处理后的Contact类型枚举类
     */
    public enum Type {
        /**
         * 注意：请勿随意更改顺序！更改后同步更改R.array.category_contact
         * 未定义的根据id,分类到保存的联系人中
         * 好友: （1||3）&& !endsWith("@chatroom")&&!startWith("gh_")
         */
        FRIEND(R.string.friend),
        /**
         * 删除的好友: 0 && !endsWith("@chatroom")&&!startWith("gh_")
         */
        DELETED_FRIEND(R.string.deleted_friend),
        /**
         * 陌生人: 4
         */
        STRANGER(R.string.stranger),
        /**
         * 加入的群聊:2||（ 3 && endsWith("_chatroom")）
         */
        JOINED_GROUP(R.string.saved_group),
        /**
         * 退出的群聊: 0 && endsWith("chatroom")
         */
        QUITED_GROUP(R.string.quited_group),
        /**
         * 关注的公众号: （1||3）&& startsWith("gh_")
         */
        FOLLOWING_GZH(R.string.following_gzh),
        /**
         * 取消关注的公众号: 0 && startsWith("gh_")
         */
        UNFOLLOWED_GZH(R.string.unfollowed_gzh),
        /**
         * 微信官方账号: 33
         */
        OFFICIAL(R.string.official);
        /**
         * 类型的名称资源ID
         */
        String caption;


        Type(int captionRes) {
            this.caption = App.getStringOf(captionRes);
        }

        public static List<String> getCaptionList() {
            List<String> captions = new ArrayList<>();
            for (Type type : Type.values()) {
                captions.add(type.caption);
            }
            return captions;
        }
    }

    /**
     * 获取某特定排序规则下的描述，描述的用途在于为数据分类
     * 相同描述的数据可视为一类，方便区分和筛选
     * 例如，当排序规则为{@link SortBy#NAME}时，即按名称排序时，
     * 此时的描述为当前联系人名称的拼音缩写首字母
     *
     * @param sortBy 指定排序规则
     * @return 返回当前对象的描述
     */
    @NonNull
    public String describe(SortBy sortBy) {
        if (sortBy == SortBy.NAME) {
            return getComparatorPyAbbr().substring(0, 1);
        }
        throw new IllegalArgumentException("Contact can be described with SortBy.NAME only! ");
    }

    /**
     * @return 返回默认描述
     */
    public String describe() {
        return describe(SortBy.NAME);
    }

    /**
     * 获取当前{@code Contact}对象的类型，主要根据{@link Contact#rawType}进行判断
     * 具体判断规则见{@link Type}的注释
     * 此方法应当被{@link ContactRepository#queryAll()}中被调用，获取当前对象的类型
     * 直接访问{@link Contact#type}即可
     */
    public void judgeType() {
        switch (rawType) {
            case RAW_TYPE_OFFICIAL:
                this.type = Type.OFFICIAL;
                break;
            case RAW_TYPE_DELETED:
                if (isGZH()) {
                    this.type = Type.UNFOLLOWED_GZH;
                } else if (isGroup()) {
                    this.type = Type.QUITED_GROUP;
                } else {
                    this.type = Type.DELETED_FRIEND;
                }
                break;
            case RAW_TYPE_UNSAVED_FRIEND:
                this.type = Type.STRANGER;
                break;
            case RAW_TYPE_UNSAVED_GROUP:
                this.type = Type.JOINED_GROUP;
                break;
            default:
                if (isGZH()) {
                    this.type = Type.FOLLOWING_GZH;
                } else if (isGroup()) {
                    this.type = Type.JOINED_GROUP;
                } else {
                    this.type = Type.FRIEND;
                }
                break;
        }
    }


    @Override
    public String getName() {
        return TextUtils.isEmpty(remark) ? super.getName() : remark;
    }


}
