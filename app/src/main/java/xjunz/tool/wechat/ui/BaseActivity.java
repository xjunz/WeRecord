/*
 * Copyright (c) 2020 xjunz. 保留所有权利
 */

package xjunz.tool.wechat.ui;

import android.content.Context;
import android.os.ResultReceiver;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import androidx.appcompat.app.AppCompatActivity;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;

import io.reactivex.Completable;
import io.reactivex.schedulers.Schedulers;
import xjunz.tool.wechat.impl.Environment;
import xjunz.tool.wechat.impl.model.account.User;

public abstract class BaseActivity extends AppCompatActivity {


    public static void hideIme(@NotNull View view) {
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
