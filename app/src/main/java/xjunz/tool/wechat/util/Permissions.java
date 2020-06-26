package xjunz.tool.wechat.util;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.PermissionChecker;
import androidx.fragment.app.Fragment;

import org.jetbrains.annotations.Contract;


public class Permissions {

    private static final String[] STORAGE = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};
    //  private static final String PHONE = Manifest.permission.READ_PHONE_STATE;
    //  public static final int REQUEST_CODE_PHONE = 0x1a;
    public static final int REQUEST_CODE_STORAGE = 0x1b;
    private static final int REQUEST_CODE_APPLICATION_DETAILS = 0x1c;
    private Activity mContext;
    private Fragment mFragment;

   /* public boolean hasPhoneRequested() {
        return mHasPhoneRequested;
    }*/

    public boolean hasStorageRequested() {
        return mHasStorageRequested;
    }

    private boolean mHasStorageRequested;


    private boolean hasPermission(@NonNull String permission) {
        int result = PermissionChecker.checkSelfPermission(mContext, permission);
        // Also check if denied by APP OPS
        if (result == PermissionChecker.PERMISSION_DENIED_APP_OP || result == PermissionChecker.PERMISSION_DENIED) {
            return false;
        } else {
            return result == PermissionChecker.PERMISSION_GRANTED;
        }

    }

    private Permissions(@NonNull Activity context, @Nullable Fragment fragment) {
        this.mContext = context;
        this.mFragment = fragment;
    }


    @Contract("_ -> new")
    public static Permissions of(@NonNull Activity context) {
        return new Permissions(context, null);
    }


    @Contract("_ -> new")
    public static Permissions of(@NonNull Fragment fragment) {
        return new Permissions(fragment.requireActivity(), fragment);
    }

    /*   public boolean hasPhonePermission() {
           return hasPermission(PHONE);
       }
   */
    public boolean hasStoragePermission() {
        return hasPermission(STORAGE[0]) && hasPermission(STORAGE[1]);
    }

/*
    public void requestPhonePermission() {
        if (mFragment != null) {
            mFragment.requestPermissions(new String[]{PHONE}, REQUEST_CODE_PHONE);
        } else {
            ActivityCompat.requestPermissions(mContext, new String[]{PHONE}, REQUEST_CODE_PHONE);
        }
        mHasPhoneRequested = true;
    }
*/

    public void requestStoragePermission() {
        if (mFragment != null) {
            mFragment.requestPermissions(STORAGE, REQUEST_CODE_STORAGE);
        } else {
            ActivityCompat.requestPermissions(mContext, STORAGE, REQUEST_CODE_STORAGE);
        }
        mHasStorageRequested = true;
    }


/*    public boolean isPhonePermissionBanned() {
        return mHasPhoneRequested && !ActivityCompat.shouldShowRequestPermissionRationale(mContext, PHONE);
    }*/

    public boolean isStoragePermissionBanned() {
        return mHasStorageRequested && !ActivityCompat.shouldShowRequestPermissionRationale(mContext, STORAGE[0]);
    }

    public void gotoApplicationDetails() {
        Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        i.setData(Uri.parse("package:" + mContext.getPackageName()));
        mContext.startActivityForResult(i, REQUEST_CODE_APPLICATION_DETAILS);
    }

}
