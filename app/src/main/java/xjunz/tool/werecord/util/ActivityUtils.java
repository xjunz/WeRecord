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
import xjunz.tool.werecord.Constants;
import xjunz.tool.werecord.R;
import xjunz.tool.werecord.ui.customview.MasterToast;

/**
 * @author xjunz 2021/2/17 1:51
 */
public class ActivityUtils {
    public static void safeViewUri(@NotNull Context context, String uriString) {
        startActivityCreateChooser(context, new Intent(Intent.ACTION_VIEW, Uri.parse(uriString)));
    }

    private static boolean viewUri(@NotNull Context context, String uriString) {
        try {
            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(uriString)));
            return true;
        } catch (Exception e) {
            return false;
        }
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

    public static void feedbackEmail(@NotNull Context context, String msg) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(Constants.URI_FEEDBACK_EMAIL));
        intent.putExtra(Intent.EXTRA_SUBJECT, "WR-CRASH-REPORT: " + Utils.formatDateLocally(System.currentTimeMillis()));
        intent.putExtra(Intent.EXTRA_TEXT, msg);
        context.startActivity(Intent.createChooser(intent, App.getContext().getString(R.string.open_via)));
    }

    public static void feedbackAutoFallback(Context context, String errorLog) {
        if (Constants.USER_DEBUGGABLE) {
            if (!viewUri(context, Constants.URI_LAUNCH_QQ_TEMP_CHAT)) {
                feedbackEmail(context, errorLog);
            }
        } else {
            if (!viewUri(context, Constants.URI_JOIN_QQ_FEEDBACK_QQ_GROUP)) {
                feedbackEmail(context, errorLog);
            }
        }
    }

    public static void feedbackJoinQGroup(Context context) {
        try {
            context.startActivity(new Intent().setData(Uri.parse(Constants.URI_JOIN_QQ_FEEDBACK_QQ_GROUP)));
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
