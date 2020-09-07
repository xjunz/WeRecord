/*
 * Copyright (c) 2020 xjunz. 保留所有权利
 */

package xjunz.tool.wechat.ui.customview;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import xjunz.tool.wechat.App;
import xjunz.tool.wechat.R;

/**
 * 简单地实现一个更美观的Toast，美其名曰<b>MasterToast</b>
 */
public class MasterToast extends android.widget.Toast {
    /**
     * Construct an empty Toast object.  You must call {@link #setView} before you
     * can call {@link #show}.
     *
     * @param context The context to use.  Usually your {@link Application}
     *                or {@link Activity} object.
     */

    public MasterToast(Context context) {
        super(context);
    }

    /**
     * 创建一个{@link MasterToast}
     *
     * @param msg 要显示的信息
     * @return 创建的{@link MasterToast}
     */
    public static MasterToast make(@Nullable CharSequence msg) {
        MasterToast toast = new MasterToast(App.getContext());
        @SuppressLint("InflateParams") TextView content = (TextView) LayoutInflater.from(App.getContext()).inflate(R.layout.widget_toast, null);
        toast.setView(content);
        content.setText(msg);
        return toast;
    }

    /**
     * 显示一个{@link MasterToast}
     *
     * @param msg    要显示的信息
     * @param length 时长
     */
    private static void toast(@Nullable CharSequence msg, int length) {
        MasterToast toast = new MasterToast(App.getContext());
        @SuppressLint("InflateParams") TextView content = (TextView) LayoutInflater.from(App.getContext()).inflate(R.layout.widget_toast, null);
        toast.setView(content);
        toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 200);
        content.setText(msg);
        toast.show();
    }

    /**
     * 显示时长为{@link android.widget.Toast#LENGTH_SHORT}的{@link MasterToast}
     *
     * @param msg 要显示的消息
     */
    public static void shortToast(@Nullable CharSequence msg) {
        toast(msg, Toast.LENGTH_SHORT);
    }

    /**
     * 显示时长为{@link android.widget.Toast#LENGTH_SHORT}的{@link MasterToast}
     *
     * @param msgRes 要显示的消息的资源ID
     */
    public static void shortToast(@StringRes int msgRes) {
        shortToast(App.getStringOf(msgRes));
    }

    /**
     * 显示时长为{@link android.widget.Toast#LENGTH_LONG}的{@link MasterToast}
     *
     * @param msg 要显示的消息
     */
    public static void longToast(@Nullable String msg) {
        toast(msg, Toast.LENGTH_LONG);
    }

    /**
     * 显示时长为{@link android.widget.Toast#LENGTH_LONG}的{@link MasterToast}
     *
     * @param msgRes 要显示的消息的资源ID
     */
    public static void longToast(@StringRes int msgRes) {
        longToast(App.getStringOf(msgRes));
    }
}
