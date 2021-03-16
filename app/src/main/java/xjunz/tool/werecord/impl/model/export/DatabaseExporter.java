/*
 * Copyright (c) 2021 xjunz. 保留所有权利
 */
package xjunz.tool.werecord.impl.model.export;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;

import net.sqlcipher.database.SQLiteDatabase;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import io.reactivex.Completable;
import xjunz.tool.werecord.App;
import xjunz.tool.werecord.R;
import xjunz.tool.werecord.impl.Environment;
import xjunz.tool.werecord.impl.model.account.Account;
import xjunz.tool.werecord.impl.model.account.User;
import xjunz.tool.werecord.util.IoUtils;
import xjunz.tool.werecord.util.RxJavaUtils;
import xjunz.tool.werecord.util.ShellUtils;

/**
 * @author xjunz 2021/2/17 0:09
 */
public class DatabaseExporter extends Exporter {
    private File mTempPlainTextDbFile;
    @Mode
    private final int mMode;
    public static final int MODE_DECRYPT = 0;
    public static final int MODE_KEEP = 1;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({MODE_DECRYPT, MODE_KEEP})
    @interface Mode {

    }

    public DatabaseExporter(@Mode int mode) {
        super();
        mMode = mode;
    }

    @Override
    public List<? extends Account> getSourceList() {
        return null;
    }

    @Override
    public Format[] getSupportFormats() {
        return new Format[]{Format.CIPHER_DB};
    }

    @Override
    protected Completable exportAsToAsync(Format format, @NonNull File outputFile, @NonNull OnProgressListener listener) {
        if (mMode == MODE_DECRYPT) {
            String name = "plaintext";
            SQLiteDatabase workerDb = Environment.getInstance().getWorkerDatabase();
            return RxJavaUtils.complete(() -> {
                mTempPlainTextDbFile = File.createTempFile("decrypted", null);
                workerDb.rawExecSQL(String.format("ATTACH DATABASE '%s' AS %s KEY '';", mTempPlainTextDbFile.getAbsolutePath(), name));
                workerDb.rawExecSQL(String.format("SELECT sqlcipher_export('%s');", name));
                workerDb.rawExecSQL(String.format("DETACH DATABASE %s;", name));
                //打包readme和数据库
                ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(outputFile, true));
                zipOutputStream.putNextEntry(new ZipEntry("readme.html"));
                zipOutputStream.write(getDbExportReadme(null).getBytes());
                zipOutputStream.closeEntry();
                zipOutputStream.putNextEntry(new ZipEntry("decrypted.db"));
                IoUtils.transferStreamNoCloseOutStream(new FileInputStream(mTempPlainTextDbFile), zipOutputStream);
                zipOutputStream.closeEntry();
                zipOutputStream.flush();
                zipOutputStream.close();
            }).doFinally(() -> IoUtils.deleteFile(mTempPlainTextDbFile));
        } else if (mMode == MODE_KEEP) {
            User currentUser = Environment.getInstance().getCurrentUser();
            return RxJavaUtils.complete(() -> {
                mTempPlainTextDbFile = File.createTempFile("backup", null);
                ShellUtils.cp(currentUser.originalDatabaseFilePath, mTempPlainTextDbFile.getPath());
                getPasswordConfig().setValue(currentUser.databasePassword);
                //打包readme和数据库
                ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(outputFile, true));
                zipOutputStream.putNextEntry(new ZipEntry("readme.html"));
                zipOutputStream.write(getDbExportReadme(String.format("<b>UIN</b>: %s<br>\n<b>Path</b>: %s<br>", currentUser.uin, currentUser.originalDatabaseFilePath)).getBytes());
                zipOutputStream.closeEntry();
                zipOutputStream.putNextEntry(new ZipEntry("EnMicroMsg.db"));
                IoUtils.transferStreamNoCloseOutStream(new FileInputStream(mTempPlainTextDbFile), zipOutputStream);
                zipOutputStream.closeEntry();
                zipOutputStream.flush();
                zipOutputStream.close();
            }).doFinally(() -> IoUtils.deleteFile(mTempPlainTextDbFile));
        }
        throw new IllegalArgumentException("Unknown mode: " + mMode);
    }

    @Override
    public String getExportFileName() {
        if (mMode == MODE_DECRYPT) {
            return App.getStringOf(R.string.format_export_db_name, getCurrentDate()).concat(".zip");
        } else if (mMode == MODE_KEEP) {
            return String.format("backup_%s", getCurrentDate()).concat(".zip");
        }
        throw new IllegalArgumentException("Unknown mode: " + mMode);

    }

    @Override
    public String getExportableName() {
        if (mMode == MODE_DECRYPT) {
            return App.getStringOf(R.string.decrypted_database);
        } else if (mMode == MODE_KEEP) {
            return App.getStringOf(R.string.original_database);
        }
        throw new IllegalArgumentException("Unknown mode: " + mMode);
    }

    @Override
    protected void initCustomConfigs(@NonNull List<Config<?>> configList) {
        //do nothing
    }
}
