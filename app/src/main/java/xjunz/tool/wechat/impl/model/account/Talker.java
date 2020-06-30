package xjunz.tool.wechat.impl.model.account;

import androidx.annotation.NonNull;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import xjunz.tool.wechat.App;
import xjunz.tool.wechat.R;
import xjunz.tool.wechat.impl.repo.TalkerRepository;
import xjunz.tool.wechat.ui.activity.main.model.SortBy;
import xjunz.tool.wechat.util.UniUtils;

import static xjunz.tool.wechat.util.UniUtils.arabicDigit2HanDigit;

/**
 * Talker是什么？{@link Contact} and a {@code Talker}的区别是什么?
 * <p>继承自微信的设定，有当前用户参与的会话（群聊、私聊）都可以称为{@code Talker}。本质上，可以查询到聊天记录的会话主体就叫所谓{@code Talker}
 * 如果没有聊天记录，就退化为{@code Contact}。 所有Talker的信息都可以在"rconversation"表中查询到，详见{@link xjunz.tool.wechat.impl.repo.TalkerRepository}</p>
 */

public class Talker extends Contact {
    public static final SortBy DEFAULT_SORT_BY = SortBy.TIMESTAMP;
    public static final boolean DEFAULT_IS_ASCENDING = true;
    private static SortBy sSortBy = DEFAULT_SORT_BY;
    private static boolean sAscending = DEFAULT_IS_ASCENDING;
    /**
     * 聊天消息数量
     */
    public int messageCount;
    /**
     * 上次聊天时间，是对{@link Talker#lastMsgTimestamp}的{@code 格式化}
     * 详见{@link Talker#setLastMsgTimestamp(long)}
     */
    public String lastMessageTime;
    /**
     * 上次聊天的时间戳，精确到毫秒
     */
    private long lastMsgTimestamp;
    /**
     * 当前{@code Talker}的类型
     */
    public Type type;
    /**
     * 考虑到时间描述的获取可能比较耗时，我们建立一个缓存变量
     * 如果这个变量不为空，直接返回即可,详见{@link Talker#describe(SortBy)}
     */
    private String timestampDes;

    /**
     * {@code Talker}的枚举类
     * 包括三种类型：{@link Type#FRIEND}好友、{@link Type#GROUP}群聊、{@link Type#GZH}公众号
     */
    public enum Type {
        FRIEND(R.string.friend), GROUP(R.string.group), GZH(R.string.gzh);
        public String caption;

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
     * 设置排序依据和顺序，用于{@link Talker#compareTo(Contact)}
     * 这个方法必须在每次调用{@link Talker#compareTo(Contact)}之前调用，否则可能会使用上一次的配置
     *
     * @param by          排序依据
     * @param isAscending 是否升序
     */
    public static void setSortByAndOrderBy(SortBy by, boolean isAscending) {
        sSortBy = by;
        sAscending = isAscending;
    }

    public void setLastMsgTimestamp(long timestamp) {
        this.lastMsgTimestamp = timestamp;
        this.lastMessageTime = UniUtils.formatDate(timestamp);
    }

    public long getLastMsgTimestamp() {
        return lastMsgTimestamp;
    }

    /**
     * 判断当前Talker的类型，此方法应该在{@link TalkerRepository#queryAll()} 中调用，
     * 获取当前{@code Talker}类型直接访问{@link Talker#type}即可
     */
    @Override
    public void judgeType() {
        if (isGroup()) {
            this.type = Type.GROUP;
        } else if (isGZH()) {
            this.type = Type.GZH;
        } else {
            this.type = Type.FRIEND;
        }
    }


    /**
     * 获取某特定排序规则下的描述，描述的用途在于为数据分类
     * 相同描述的数据可视为一类，方便区分和筛选
     *
     * @param sortBy 排序依据
     * @return 获取当前数据的描述性文字，用于排序和分类
     */
    @NonNull
    @Override
    public String describe(@NonNull SortBy sortBy) {
        switch (sortBy) {
            case NAME:
                String comparator = getComparatorPyAbbr();
                return comparator.substring(0, 1);
            case MSG_COUNT:
                if (messageCount <= 10) {
                    return "<10";
                } else if (messageCount <= 50) {
                    return "<50";
                } else if (messageCount <= 100) {
                    return "<100";
                } else if (messageCount <= 500) {
                    return "<500";
                } else if (messageCount <= 1000) {
                    return "<1000";
                } else {
                    return ">1000";
                }
            case TIMESTAMP:
                if (timestampDes == null) {
                    Calendar lastMsg = Calendar.getInstance();
                    lastMsg.setTime(new Date(lastMsgTimestamp));
                    Calendar now = Calendar.getInstance();
                    now.setTime(new Date(System.currentTimeMillis()));
                    int yearGap = now.get(Calendar.YEAR) - lastMsg.get(Calendar.YEAR);
                    if (yearGap == 0) {
                        int dayGap = now.get(Calendar.DAY_OF_YEAR) - lastMsg.get(Calendar.DAY_OF_YEAR);
                        int weekGap = now.get(Calendar.WEEK_OF_YEAR) - lastMsg.get(Calendar.WEEK_OF_YEAR);
                        int monthGap = now.get(Calendar.MONTH) - lastMsg.get(Calendar.MONTH);
                        if (dayGap == 0) {
                            timestampDes = App.getStringOf(R.string.today);
                        } else if (dayGap == 1) {
                            timestampDes = App.getStringOf(R.string.yesterday);
                        } else if (weekGap == 0) {
                            timestampDes = App.getStringOf(R.string.format_days_ago, arabicDigit2HanDigit(dayGap));
                        } else if (monthGap == 0) {
                            if (weekGap == 1) {
                                timestampDes = App.getStringOf(R.string.last_week);
                            }
                            timestampDes = App.getStringOf(R.string.format_weeks_ago, arabicDigit2HanDigit(weekGap));
                        } else {
                            if (monthGap == 1) {
                                timestampDes = App.getStringOf(R.string.last_month);
                            }
                            timestampDes = App.getStringOf(R.string.format_months_ago, arabicDigit2HanDigit(monthGap));
                        }
                    } else {
                        if (yearGap == 1) {
                            timestampDes = App.getStringOf(R.string.last_year);
                        }
                        timestampDes = App.getStringOf(R.string.format_years_ago, arabicDigit2HanDigit(yearGap));
                    }
                }
                return timestampDes;

        }
        return "-";
    }

    /**
     * 返回当前{@link Talker#sSortBy}下的描述
     *
     * @return 描述
     */
    @Override
    public String describe() {
        return describe(sSortBy);
    }

    @Override
    public int compareTo(@NotNull Contact o) {
        if (o instanceof Talker) {
            Talker talker = (Talker) o;
            switch (sSortBy) {
                case NAME:
                    return (sAscending ? 1 : -1) * getComparatorPyAbbr().compareTo(o.getComparatorPyAbbr());
                case MSG_COUNT:
                    return (sAscending ? 1 : -1) * Integer.compare(messageCount, talker.messageCount);
                case TIMESTAMP:
                    //时间戳越大离现在越近
                    return (sAscending ? -1 : 1) * Long.compare(lastMsgTimestamp, talker.lastMsgTimestamp);
            }
        } else {
            return (sAscending ? 1 : -1) * super.compareTo(o);
        }
        return 0;
    }
}
