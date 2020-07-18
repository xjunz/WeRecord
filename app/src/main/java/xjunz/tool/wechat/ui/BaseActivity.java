/*
 * Copyright (c) 2020 xjunz. 保留所有权利
 */

package xjunz.tool.wechat.ui;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.lang.reflect.Method;

import io.reactivex.Completable;
import io.reactivex.schedulers.Schedulers;
import xjunz.tool.wechat.R;
import xjunz.tool.wechat.impl.Environment;
import xjunz.tool.wechat.impl.model.account.User;
import xjunz.tool.wechat.util.UiUtils;

public abstract class BaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window window = getWindow();
        if (Build.VERSION.SDK_INT >= 27) {
            //在N及以后的设备上，我们可以使用亮色导航栏，导航栏按钮自动变为深色
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        } else {
            //在N以前的设备，为了防止导航栏按钮和背景合为一体，我们将导航栏背景填充暗色
            window.setNavigationBarColor(UiUtils.getAttrColor(this, R.attr.colorPrimaryDark));
        }
    }


    public static void hideIme(View view) {
        InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    @SuppressWarnings("JavaReflectionMemberAccess")
    public static void showIme(View view) {
        Completable.create(emitter -> {
            InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService
                    (Context.INPUT_METHOD_SERVICE);
            // the public methods don't seem to work for me, so try reflection.
            try {
                Method showSoftInputUnchecked = InputMethodManager.class.getMethod(
                        "showSoftInputUnchecked", int.class, ResultReceiver.class);
                showSoftInputUnchecked.setAccessible(true);
                showSoftInputUnchecked.invoke(imm, 0, null);
            } catch (Exception e) {
                // ho hum
            }
        }).subscribeOn(Schedulers.newThread()).subscribe();

    }

    public Environment getEnvironment() {
        return Environment.getInstance();
    }

    public User getCurrentUser() {
        return getEnvironment().getCurrentUser();
    }
}
