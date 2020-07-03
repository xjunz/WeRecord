package xjunz.tool.wechat.ui.customview;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import xjunz.tool.wechat.App;
import xjunz.tool.wechat.R;

/**
 * 简单地实现一个更美观的Toast，暂且命名为<b>MasterToast</b>
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
     * 显示一个{@link MasterToast}
     *
     * @param msg    要显示的信息
     * @param length 时长
     */
    private static void toast(@NonNull String msg, int length) {
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
    public static void shortToast(@NonNull String msg) {
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
    public static void longToast(@NonNull String msg) {
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
