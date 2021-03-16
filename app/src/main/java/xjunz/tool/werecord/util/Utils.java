/*
 * Copyright (c) 2021 xjunz. 保留所有权利
 */

package xjunz.tool.werecord.util;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Build;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;

import com.github.promeg.pinyinhelper.Pinyin;

import org.intellij.lang.annotations.RegExp;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import xjunz.tool.werecord.App;

/**
 * 通用工具类，包含各种UI无关的工具方法
 */
public class Utils {
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
    @NotNull
    public static String getPinYinAbbr(@NotNull String src) {
        StringBuilder abbr = new StringBuilder();
        for (char c : src.toCharArray()) {
            abbr.append(Pinyin.toPinyin(c).charAt(0));
        }
        return abbr.toString();
    }

    //到十一，因为可能用到"十一个月前"
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
        return dayOfWeek == 1 ? "日" : dayOfWeek == 3 ? "二" : arabicDigit2HanDigit(dayOfWeek - 1);
    }

    /**
     * 复制指定文本内容到剪切板
     *
     * @param label 文本内容的标签
     * @param msg   欲复制到剪切板的文本内容
     */
    public static void copyPlainText(String label, CharSequence msg) {
        ClipboardManager clipboardManager = (ClipboardManager) App.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboardManager != null) {
            clipboardManager.setPrimaryClip(new ClipData(label, new String[]{"text/plain"}, new ClipData.Item(msg)));
        }
    }

    public static void showImeFor(@NotNull View view) {
        view.post(() -> {
            view.requestFocus();
            InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService
                    (Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(view, 0);
            }
        });
    }

    /**
     * 返回目标字符串的正则捕获结果，匹配模式为{@link Pattern#DOTALL}
     *
     * @param src   源字符串
     * @param regex 正则表达式（包含捕获组）
     * @return 匹配到的捕获组集合
     */
    @NotNull
    public static List<String> extract(String src, @RegExp String regex) {
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(src);
        ArrayList<String> strs = new ArrayList<>();
        while (m.find()) {
            strs.add(m.group(1));
        }
        return strs;
    }

    /**
     * 提取出符合正则的第一个捕获组，如果不存在返回null
     */
    @Nullable
    public static String extractFirst(String src, @RegExp String regex) {
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(src);
        if (m.find()) {
            return m.group(1);
        }
        return null;
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


    @NotNull
    public static String formatToPercent(float fraction, int digits) {
        NumberFormat nt = NumberFormat.getPercentInstance();
        nt.setMinimumFractionDigits(digits);
        return nt.format(fraction);
    }

    public static long parseDate(String dateStr) {
        SimpleDateFormat format = (SimpleDateFormat) SimpleDateFormat.getDateTimeInstance();
        format.applyPattern("yyyy-MM-dd HH:mm:ss:SSS");
        try {
            Date date = format.parse(dateStr);
            if (date == null) {
                return -1L;
            } else {
                return date.getTime();
            }
        } catch (ParseException e) {
            return -1L;
        }
    }

    @NotNull
    public static String formatDate(long timestamp) {
        if (timestamp <= 0) {
            return "-";
        }
        Date date = new Date(timestamp);
        SimpleDateFormat format = (SimpleDateFormat) SimpleDateFormat.getDateTimeInstance();
        format.applyPattern("yyyy-MM-dd HH:mm:ss:SSS");
        return format.format(date);
    }

    @NotNull
    public static String formatDateIgnoreMills(long timestamp) {
        if (timestamp <= 0) {
            return "-";
        }
        Date date = new Date(timestamp);
        SimpleDateFormat format = (SimpleDateFormat) SimpleDateFormat.getDateTimeInstance();
        format.applyPattern("yyyy-MM-dd HH:mm:ss");
        return format.format(date);
    }

    @SuppressWarnings("deprecation")
    public static Locale getCurrentLocale() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return App.getContext().getResources().getConfiguration().getLocales().get(0);
        } else {
            return App.getContext().getResources().getConfiguration().locale;
        }
    }

    @NotNull
    public static String formatDateLocally(long timestamp) {
        if (timestamp < 0) {
            return "-";
        }
        Date date = new Date(timestamp);
        SimpleDateFormat format = (SimpleDateFormat) SimpleDateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM, getCurrentLocale());
        return format.format(date);
    }


    public static String fallback(@Nullable String nullable, @NonNull String fallback) {
        return TextUtils.isEmpty(nullable) ? fallback : nullable;
    }

    @NotNull
    public static <T extends ViewModel> T getViewModel(ViewModelStoreOwner owner, Class<T> t) {
        return new ViewModelProvider(owner, new ViewModelProvider.NewInstanceFactory()).get(t);
    }

    public static boolean byteArrayDeepEquals(@Nullable byte[] a, @Nullable byte[] b) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        if (a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            if (a[i] != b[i]) {
                return false;
            }
        }
        return true;
    }

    public static boolean arrayDeepEquals(@Nullable Object[] a, @Nullable Object[] b) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        if (a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            Object ai = a[i];
            Object bi = b[i];
            if (ai == bi) {
                continue;
            }
            if (ai == null || bi == null) {
                return false;
            }
            if (!ai.getClass().equals(bi.getClass())) {
                return false;
            } else if (ai instanceof byte[]) {
                if (!byteArrayDeepEquals((byte[]) ai, (byte[]) bi)) {
                    return false;
                }
            } else {
                if (!Objects.equals(ai, bi)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * @return obj == null ? null : obj.toString();
     */
    @Nullable
    public static String stringValueOf(@Nullable Object obj) {
        return obj == null ? null : obj.toString();
    }
}
