/*
 * Copyright (c) 2021 xjunz. 保留所有权利
 */
package xjunz.tool.werecord.util;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.net.Uri;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCaller;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import org.jetbrains.annotations.NotNull;

import xjunz.tool.werecord.App;
import xjunz.tool.werecord.R;
import xjunz.tool.werecord.ui.customview.MasterToast;

/**
 * @author xjunz 2021/2/17 1:51
 */
public class ActivityUtils {
    public static void viewUri(@NotNull Context context, String uriString) {
        startActivityCreateChooser(context, new Intent(Intent.ACTION_VIEW, Uri.parse(uriString)));
    }

    public static void startActivityCreateChooser(@NotNull Context context, Intent intent) {
        String title = context.getString(R.string.open_via);
        Intent chooser = Intent.createChooser(intent, title);
        context.startActivity(chooser);
    }

    /**
     * 从{@link Context}中获取其宿主{@link Activity}，即{@code unwrap} {@link ContextWrapper}
     *
     * @param context 被{@code wrap}的{@link Context}
     * @return 宿主 {@link Activity}
     */
    @NonNull
    public static Activity getHostActivity(@NonNull Context context) {
        if (context instanceof Activity) {
            return (Activity) context;
        } else if (context instanceof ContextWrapper) {
            return getHostActivity(((ContextWrapper) context).getBaseContext());
        }
        throw new IllegalArgumentException("The context passed in must be an Activity or a ContextWrapper wrapping an Activity! ");
    }

    private static final String feedbackQGroupNum = "602611929";
    private static final String feedbackQNum = "3285680362";

    public static boolean feedbackTempQChat(Context context) {
        try {
            context.startActivity(new Intent().setData(Uri.parse("mqqwpa://im/chat?chat_type=wpa&uin=" + feedbackQNum)));
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public static void feedbackJoinQGroup(Context context) {
        try {
            context.startActivity(new Intent().setData(Uri.parse("mqqapi://card/show_pslcard?src_type=internal&version=1&uin=" + feedbackQGroupNum + "&card_type=group&source=qrcode")));
        } catch (Exception e) {
            MasterToast.shortToast(R.string.operation_failed);
        }
    }

    public static void launchVictim(@NotNull Context context) {
        context.startActivity(context.getPackageManager().getLaunchIntentForPackage("com.tencent.mm"));
    }

    @Nullable
    public static ActivityResultLauncher<Void> registerDeviceCredentialConfirmationLauncher(@NonNull ActivityResultCaller caller, @StringRes int title, @StringRes int des, @NonNull Runnable onSucceed, @NonNull Runnable onFail) {
        KeyguardManager manager = (KeyguardManager) App.getContext().getSystemService(Context.KEYGUARD_SERVICE);
        if (manager != null && manager.isDeviceSecure()) {
            return caller.registerForActivityResult(new ActivityResultContract<Void, ActivityResult>() {
                @NonNull
                @Override
                public Intent createIntent(@NonNull Context context, Void input) {
                    return manager.createConfirmDeviceCredentialIntent(context.getText(title), des == -1 ? null : context.getText(des));
                }

                @Override
                public ActivityResult parseResult(int resultCode, @Nullable Intent intent) {
                    return new ActivityResult(resultCode, intent);
                }
            }, result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    onSucceed.run();
                } else {
                    onFail.run();
                }
            });
        }
        return null;
    }
}
