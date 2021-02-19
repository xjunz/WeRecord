 /*
 * Copyright (c) 2021 xjunz. 保留所有权利
 */

 package xjunz.tool.werecord.ui.base;

 import android.app.Instrumentation;
 import android.content.Context;
 import android.content.Intent;
 import android.os.Build;
 import android.os.Bundle;
 import android.view.KeyEvent;
 import android.view.View;
 import android.view.inputmethod.InputMethodManager;

 import androidx.appcompat.app.AppCompatActivity;
 import androidx.lifecycle.ViewModel;
 import androidx.lifecycle.ViewModelProvider;

 import org.jetbrains.annotations.NotNull;

 import xjunz.tool.werecord.impl.Environment;
 import xjunz.tool.werecord.impl.model.account.User;
 import xjunz.tool.werecord.ui.outer.DebugActivity;
 import xjunz.tool.werecord.util.Utils;

 public abstract class BaseActivity extends AppCompatActivity {

     public static void hideIme(@NotNull View view) {
         InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
         if (imm != null) {
             view.clearFocus();
             imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
         }
     }

     @Override
     public void finish() {
         super.finish();
      //   overridePendingTransition(R.anim.dialog_enter, R.anim.dialog_exit);
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

     public void showImeFor(@NotNull View view) {
         Utils.showImeFor(view);
     }

     public Environment getEnvironment() {
         return Environment.getInstance();
     }

     public User getCurrentUser() {
         return getEnvironment().getCurrentUser();
     }

     @NotNull
     public <T extends ViewModel> T getViewModel(Class<T> t) {
         return new ViewModelProvider(this, new ViewModelProvider.NewInstanceFactory()).get(t);
     }
 }
