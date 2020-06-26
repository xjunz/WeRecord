package xjunz.tool.wechat.ui.customview;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.widget.TextView;

import androidx.annotation.StringRes;

import xjunz.tool.wechat.App;
import xjunz.tool.wechat.R;

/**
 * 更美观的Toast
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


    public static void shortToast(CharSequence msg) {
        MasterToast toast = new MasterToast(App.getContext());
        @SuppressLint("InflateParams") TextView content = (TextView) LayoutInflater.from(App.getContext()).inflate(R.layout.widget_toast, null);
        toast.setView(content);
        toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 200);
        content.setText(msg);
        toast.show();
    }

    public static void shortToast(@StringRes int msgRes) {
        shortToast(App.getStringOf(msgRes));
    }
}
