/*
 * Copyright (c) 2020 xjunz. 保留所有权利
 */

package xjunz.tool.wechat.impl.model.account;

import androidx.annotation.NonNull;

import org.jetbrains.annotations.NotNull;

import java.util.Calendar;
import java.util.Date;

import xjunz.tool.wechat.App;
import xjunz.tool.wechat.R;
import xjunz.tool.wechat.data.viewmodel.SortBy;
import xjunz.tool.wechat.util.UniUtils;

import static xjunz.tool.wechat.util.UniUtils.arabicDigit2HanDigit;

/**
 * Talker是什么？{@link Contact}和{@code Talker}的区别是什么?
 * <p>继承自微信的设定，有当前用户参与的会话（群聊、私聊）都可以称为{@code Talker}。本质上，可以查询到聊天记录的会话主体就叫所谓{@code Talker}
 * 如果没有聊天记录，就退化为{@code Contact}。 所有Talker的信息都可以在"rconversation"表中查询到。
 *
 * @see xjunz.tool.wechat.impl.repo.TalkerRepository </p>
 */

public class Talker extends Contact {
    /**
     * 聊天消息数量
     */
    public int messageCount;
    /**
     * 上次聊天的时间戳，精确到毫秒
     */
    public long lastMsgTimestamp;
    /**
     * 考虑到时间描述的获取可能比较耗时，我们建立一个缓存变量
     * 如果这个变量不为空，直接返回即可,
     *
     * @see Talker#describe(SortBy)
     */
    private String timestampDes;
    /**
     * 格式化后的时间戳，如: 2019/3/1
     */
    public String formatTimestamp;

    public Talker(String id) {
        super(id);
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
                if (messageCount < 10) {
                    return "1~10";
                } else if (messageCount <= 50) {
                    return "10~50";
                } else if (messageCount <= 100) {
                    return "50~100";
                } else if (messageCount <= 500) {
                    return "100~500";
                } else if (messageCount <= 1000) {
                    return "500~1000";
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
                    //刚刚更新的消息，时间戳还没同步被微信同步到数据库
                    if (yearGap < 0) {
                        this.lastMsgTimestamp = System.currentTimeMillis();
                        timestampDes = App.getStringOf(R.string.today);
                        formatTimestamp = App.getStringOf(R.string.not_long_ago);
                    } else if (yearGap == 0) {
                        int dayGap = now.get(Calendar.DAY_OF_YEAR) - lastMsg.get(Calendar.DAY_OF_YEAR);
                        int weekGap = now.get(Calendar.WEEK_OF_YEAR) - lastMsg.get(Calendar.WEEK_OF_YEAR);
                        int monthGap = now.get(Calendar.MONTH) - lastMsg.get(Calendar.MONTH);
                        if (dayGap == 0) {
                            timestampDes = App.getStringOf(R.string.today);
                            formatTimestamp = App.getStringOf(R.string.format_h_m, lastMsg.get(Calendar.HOUR_OF_DAY), lastMsg.get(Calendar.MINUTE));
                        } else if (dayGap == 1) {
                            timestampDes = App.getStringOf(R.string.yesterday);
                            formatTimestamp = timestampDes + App.getStringOf(R.string.format_h_m, lastMsg.get(Calendar.HOUR_OF_DAY), lastMsg.get(Calendar.MINUTE));
                        } else if (weekGap == 0) {
                            timestampDes = App.getStringOf(R.string.format_days_ago, arabicDigit2HanDigit(dayGap));
                            formatTimestamp = timestampDes;
                        } else if (monthGap == 0) {
                            if (weekGap == 1) {
                                timestampDes = App.getStringOf(R.string.last_week);
                                formatTimestamp = App.getStringOf(R.string.format_week_of_day, UniUtils.dayOfWeek2Han(lastMsg.get(Calendar.DAY_OF_WEEK)));
                            } else {
                                timestampDes = App.getStringOf(R.string.format_weeks_ago, arabicDigit2HanDigit(weekGap));
                                formatTimestamp = App.getStringOf(R.string.format_m_d, lastMsg.get(Calendar.MONTH) + 1, lastMsg.get(Calendar.DAY_OF_MONTH));
                            }
                        } else {
                            if (monthGap == 1) {
                                timestampDes = App.getStringOf(R.string.last_month);
                            } else {
                                timestampDes = App.getStringOf(R.string.format_months_ago, arabicDigit2HanDigit(monthGap));
                            }
                            formatTimestamp = App.getStringOf(R.string.format_m_d, lastMsg.get(Calendar.MONTH) + 1, lastMsg.get(Calendar.DAY_OF_MONTH));
                        }
                    } else {
                        if (yearGap == 1) {
                            timestampDes = App.getStringOf(R.string.last_year);
                        } else {
                            timestampDes = App.getStringOf(R.string.format_years_ago, arabicDigit2HanDigit(yearGap));
                        }
                        formatTimestamp = App.getStringOf(R.string.format_y_m_d, lastMsg.get(Calendar.YEAR), (lastMsg.get(Calendar.MONTH) + 1), lastMsg.get(Calendar.DAY_OF_MONTH));
                    }
                }
                return timestampDes;

        }
        return "-";
    }


    @Override
    public int compareTo(@NonNull Contact o, SortBy by, boolean isAscending) {
        if (o instanceof Talker) {
            Talker talker = (Talker) o;
            switch (by) {
                case NAME:
                    return (isAscending ? 1 : -1) * getComparatorPyAbbr().compareTo(o.getComparatorPyAbbr());
                case MSG_COUNT:
                    return (isAscending ? 1 : -1) * Integer.compare(messageCount, talker.messageCount);
                case TIMESTAMP:
                    //时间戳越大离现在越近
                    return (isAscending ? -1 : 1) * Long.compare(lastMsgTimestamp, talker.lastMsgTimestamp);
            }
        } else {
            return super.compareTo(o, by, isAscending);
        }
        return 0;
    }

    @NotNull
    @Override
    public String toString() {
        return "Talker{" +
                "remark='" + remark + '\'' +
                ", nickname='" + nickname + '\'' +
                ", alias='" + alias + '\'' +
                ", id='" + id + '\'' +
                '}';
    }
}
