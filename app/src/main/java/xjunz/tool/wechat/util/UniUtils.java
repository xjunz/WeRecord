package xjunz.tool.wechat.util;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

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

    public static String getPinYinAbbr(String src) {
        StringBuilder abbr = new StringBuilder();
        for (char c : src.toCharArray()) {
            if (Pinyin.isChinese(c)) {
                abbr.append(Pinyin.toPinyin(c).charAt(0));
            } else {
                abbr.append(c);
            }
        }
        return abbr.toString();
    }

    private static final String[] HAN_DIGITS = {"零", "一", "两", "三", "四", "五", "六", "七", "八", "九", "十", "十一"};

    public static String arabicDigit2HanDigit(int digit) {
        return HAN_DIGITS[digit];
    }

    public static void copyPlainText(String label, String msg) {
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
        // List<String> hans=extract(src,"(\\p{IsHan}+)");
        List<String> hans = extract(src, "([\\u4E00-\\u9FFF]+)");
        for (String han : hans) {
            builder.append(han);
        }
        return builder.toString();
    }


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

}
