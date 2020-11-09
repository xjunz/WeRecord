 /*
 * Copyright (c) 2020 xjunz. 保留所有权利
 */

package xjunz.tool.wechat.ui;

 import android.app.Instrumentation;
 import android.content.Context;
 import android.content.Intent;
 import android.os.Build;
 import android.os.Bundle;
 import android.os.ResultReceiver;
 import android.view.KeyEvent;
 import android.view.View;
 import android.view.inputmethod.InputMethodManager;

 import androidx.appcompat.app.AppCompatActivity;

 import org.jetbrains.annotations.NotNull;

 import java.lang.reflect.Method;

 import io.reactivex.Completable;
import io.reactivex.schedulers.Schedulers;
import xjunz.tool.wechat.impl.Environment;
import xjunz.tool.wechat.impl.model.account.User;
import xjunz.tool.wechat.ui.outer.DebugActivity;

 public abstract class BaseActivity extends AppCompatActivity {


     public static void hideIme(@NotNull View view) {
         InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
         if (imm != null) {
             imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
         }
     }

     @Override
     protected void onStop() {
         //修复return transition bug
         //https://stackoverflow.com/questions/59261601/shared-element-transition-not-working-in-android-10q-while-returning-to-called?r=SearchResults
         if (!isFinishing() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
             new Instrumentation().callActivityOnSaveInstanceState(this, new Bundle());
         }
         super.onStop();
     }

     @Override
     public boolean onKeyDown(int keyCode, KeyEvent event) {
         if (keyCode == KeyEvent.KEYCODE_MENU) {
             //MasterToast.shortToast("菜单键按下");
             Intent intent = new Intent(this, DebugActivity.class);
             startActivity(intent);
         }
         return super.onKeyDown(keyCode, event);
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
