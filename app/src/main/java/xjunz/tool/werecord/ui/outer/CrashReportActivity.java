/*
 * Copyright (c) 2021 xjunz. 保留所有权利
 */
package xjunz.tool.werecord.ui.outer;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.format.Formatter;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.databinding.DataBindingUtil;

import java.io.File;

import xjunz.tool.werecord.App;
import xjunz.tool.werecord.BuildConfig;
import xjunz.tool.werecord.Constants;
import xjunz.tool.werecord.R;
import xjunz.tool.werecord.databinding.ActivityCrashReportBinding;
import xjunz.tool.werecord.util.ActivityUtils;
import xjunz.tool.werecord.util.IoUtils;
import xjunz.tool.werecord.util.Utils;

/**
 * @author xjunz 2021/2/20 19:44
 */
public class CrashReportActivity extends AppCompatActivity {
    public static final String EXTRA_LOG_FILE_PATH = "CrashReportActivity.extra.LogFilePath";
    public static final String EXTRA_USER_DEBUGGABLE = "CrashReportActivity.extra.UserDebuggable";
    private File mLogFile;
    private boolean mUserDebuggable;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityCrashReportBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_crash_report);
        String logPath = getIntent().getStringExtra(EXTRA_LOG_FILE_PATH);
        mUserDebuggable = getIntent().getBooleanExtra(Intent.EXTRA_USER, false);
        if (logPath == null) {
            binding.setFileSize(getText(R.string.not_found));
        } else {
            mLogFile = new File(logPath);
            if (!mLogFile.exists()) {
                binding.setFileSize(getText(R.string.not_found));
            } else {
                binding.setFileSize(Formatter.formatFileSize(this, mLogFile.length()));
            }
        }
    }

    public void restart(View view) {
        Intent intent = new Intent(this, InitializationActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    public void quit(View view) {
        this.finishAndRemoveTask();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mLogFile != null && mLogFile.exists()) {
            IoUtils.deleteFileSync(mLogFile);
        }
    }

    public void send(View view) {
        Uri uri = FileProvider.getUriForFile(this, "xjunz.tool.werecord.fileprovider", mLogFile);
        Intent intent;
        if (!BuildConfig.DEBUG) {
            if (mUserDebuggable) {
                intent = new Intent(Intent.ACTION_VIEW, Uri.parse(Constants.URI_FEEDBACK_EMAIL))
                        .putExtra(Intent.EXTRA_SUBJECT, Utils.formatDateLocally(System.currentTimeMillis()))
                        .putExtra(Intent.EXTRA_STREAM, uri);
                startActivity(Intent.createChooser(intent, App.getContext().getString(R.string.open_via)));
            } else {
                intent = new Intent(Intent.ACTION_SEND, uri)
                        .putExtra(Intent.EXTRA_STREAM, uri)
                        .setType("text/plain");
            }
        } else {
            intent = new Intent(Intent.ACTION_VIEW, uri);
        }
        ActivityUtils.startActivityCreateChooser(this, intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION));
    }

    public void joinFeedbackGroup(View view) {
        ActivityUtils.feedbackJoinQGroup(this);
    }
}
