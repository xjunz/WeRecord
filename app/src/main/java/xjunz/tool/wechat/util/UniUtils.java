/*
 * Copyright (c) 2020 xjunz. 保留所有权利
 */

package xjunz.tool.wechat.util;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.net.Uri;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;

import com.github.promeg.pinyinhelper.Pinyin;

import org.jetbrains.annotations.Nullable;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import xjunz.tool.wechat.App;

public class UniUtils {
    private static final String feedbackQGroupNum = "561721325";
    private static final String feedbackQNum = "3285680362";

    public static boolean feedbackJoinQGroup(Activity context) {
        try {
            context.startActivity(new Intent().setData(Uri.parse("mqqapi://card/show_pslcard?src_type=internal&version=1&uin=" + feedbackQGroupNum + "&card_type=group&source=qrcode")));
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    /**
     * 返回原字符串的拼音缩写
     * <p>通过对原字符串进行逐个{@link Character}（字符）的遍历，
     * 如果某个字符为汉字则获取其拼音的首字母的大写形式，如果不是汉字，
     * 则保留源字符，最后将它们拼接并返回。</p>
     * <p>如：“Hello, 你好”会返回“Hello，NH”</p>
     *
     * @param src 源字符串
     * @return 拼音缩写
     */
    public static String getPinYinAbbr(String src) {
        StringBuilder abbr = new StringBuilder();
        for (char c : src.toCharArray()) {
            abbr.append(Pinyin.toPinyin(c).charAt(0));
        }
        return abbr.toString();
    }

    private static final String[] HAN_DIGITS = {"零", "一", "两", "三", "四", "五", "六", "七", "八", "九", "十", "十一"};

    /**
     * 将阿拉伯数字转为汉字
     *
     * @param digit 阿拉伯数字
     * @return 汉字
     */
    public static String arabicDigit2HanDigit(int digit) {
        return HAN_DIGITS[digit];
    }

    /**
     * 根据一周内的某一天的序号获取汉字
     * <br/><b>注：</b>周日为一周的第一天
     *
     * @param dayOfWeek 一周内的某一天
     * @return 汉字周几
     */
    public static String dayOfWeek2Han(@IntRange(from = 1, to = 7) int dayOfWeek) {
        return dayOfWeek == 1 ? "日" : arabicDigit2HanDigit(dayOfWeek - 1);
    }

    public static void copyPlainText(String label, CharSequence msg) {
        ClipboardManager clipboardManager = (ClipboardManager) App.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboardManager != null) {
            clipboardManager.setPrimaryClip(new ClipData(label, new String[]{"text/plain"}, new ClipData.Item(msg)));
        }
    }

    public static boolean feedbackTempQChat(Activity context) {
        try {
            context.startActivity(new Intent().setData(Uri.parse("mqqwpa://im/chat?chat_type=wpa&uin=" + feedbackQNum)));
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    /**
     * 返回目标字符串的正则捕获结果，匹配模式为{@link Pattern#DOTALL}
     *
     * @param src   源字符串
     * @param regex 正则表达式（包含捕获组）
     * @return 匹配到的捕获组集合
     */
    public static List<String> extract(String src, String regex) {
        Pattern p = Pattern.compile(regex, Pattern.DOTALL);
        Matcher m = p.matcher(src);
        ArrayList<String> strs = null;
        while (m.find()) {
            if (strs == null) {
                strs = new ArrayList<>();
            }
            strs.add(m.group(1));
        }
        return strs;
    }

    /**
     * 提取出符合正则的第一个捕获组
     */
    @Nullable
    public static String extractFirst(String src, String regex) {
        Pattern p = Pattern.compile(regex, Pattern.DOTALL);
        Matcher m = p.matcher(src);
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }

    public static String extractAndConcatHans(String src) {
        StringBuilder builder = new StringBuilder();
        //Sadly, Android does not support Java 7:(, so below codes don't make sense
        //List<String> hans=extract(src,"(\\p{IsHan}+)");
        List<String> hans = extract(src, "([\\u4E00-\\u9FFF]+)");
        for (String han : hans) {
            builder.append(han);
        }
        return builder.toString();
    }


    /**
     * 返回介于{@param min}(包含)和{@param max}（包含）之间的一个随机数,
     *
     * @param min 随机数约束的最小值
     * @param max 随机数约束的最大值
     * @return 随机数
     */
    public static int random(int min, int max) {
        Random random = new Random();
        return random.nextInt(max - min + 1) + min;
    }


    public static String formatToPercent(float num, int digits) {
        NumberFormat nt = NumberFormat.getPercentInstance();
        nt.setMinimumFractionDigits(digits);
        return nt.format(num);
    }

    public static String formatDate(long timestamp) {
        Date date = new Date(timestamp);
        return String.format(App.getContext().getResources().getConfiguration().locale, "%tF %tT", date, date);
    }

    /**
     * 从{@link Context}中获取其宿主{@link Activity}，即{@code unwrap}{@link ContextWrapper}
     *
     * @param context 被{@code wrap}的{@link Context}
     * @return 宿主 {@link Activity}
     */
    @NonNull
    public static Activity getHostActivity(@NonNull Context context) {
        if (context instanceof Activity) {
            return (Activity) context;
        } else if (context instanceof ContextWrapper) {
            return getHostActivity(((ContextWrapper) context).getBaseContext());
        }
        throw new IllegalArgumentException("The context passes in must be an Activity or a ContextWrapper! ");
    }
}
