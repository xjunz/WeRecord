/*
 * Copyright (c) 2021 xjunz. 保留所有权利
 */
package xjunz.tool.werecord;

import android.app.ApplicationErrorReport;
import android.content.Intent;
import android.util.PrintStreamPrinter;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import xjunz.tool.werecord.impl.Environment;
import xjunz.tool.werecord.ui.outer.CrashReportActivity;
import xjunz.tool.werecord.util.IoUtils;
import xjunz.tool.werecord.util.Utils;

public class CrashHandler implements Thread.UncaughtExceptionHandler {
    @Override
    public void uncaughtException(@NonNull Thread t, @NonNull Throwable e) {
        //这个错误不需要处理，它来自我们的RecycleSensitiveActivity
        if (!e.getClass().getSimpleName().equals("SuperNotCalledException")) {
            if (BuildConfig.DEBUG) {
                e.printStackTrace();
            }
            ApplicationErrorReport.CrashInfo info = new ApplicationErrorReport.CrashInfo(e);
            Environment environment = Environment.getInstance();
            if (environment != null && environment.initialized()) {
                environment.getCurrentUser().deleteWorkerDatabase();
            }
            String date = Utils.formatDate(System.currentTimeMillis());
            File logDir = new File(App.getContext().getCacheDir().getPath() + File.separator + "crash");
            File logFile = null;
            try {
                if (logDir.exists() || (!logDir.exists() && logDir.mkdirs())) {
                    logFile = new File(logDir.getPath() + File.separator + date + ".txt");
                    if (logFile.createNewFile()) {
                        FileOutputStream out = new FileOutputStream(logFile, true);
                        out.write(date.concat("\n").getBytes());
                        out.write(Environment.getBasicEnvInfo().concat("\n").getBytes());
                        out.write(Environment.infoBlockHeader("S").concat("\n").getBytes());
                        PrintStreamPrinter printer = new PrintStreamPrinter(new PrintStream(out, true));
                        info.dump(printer, "");
                        out.close();
                    }
                }
            } catch (IOException ioException) {
                //fallback
                Utils.copyPlainText("WR-ERROR-LOG", IoUtils.readStackTraceFromThrowable(e));
            }
            Intent launchCrashReport = new Intent(App.getContext(), CrashReportActivity.class);
            launchCrashReport.putExtra(CrashReportActivity.EXTRA_USER_DEBUGGABLE, Constants.USER_DEBUGGABLE);
            launchCrashReport.putExtra(CrashReportActivity.EXTRA_LOG_FILE_PATH, logFile == null ? null : logFile.getPath())
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            App.getContext().startActivity(launchCrashReport);
        }
        System.exit(1);
    }
}
