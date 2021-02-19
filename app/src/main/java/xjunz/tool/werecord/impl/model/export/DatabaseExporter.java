/*
 * Copyright (c) 2021 xjunz. 保留所有权利
 */
package xjunz.tool.werecord.impl.model.export;

import android.text.TextUtils;

import androidx.annotation.NonNull;

import net.sqlcipher.database.SQLiteDatabase;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import io.reactivex.Completable;
import xjunz.tool.werecord.App;
import xjunz.tool.werecord.R;
import xjunz.tool.werecord.impl.Environment;
import xjunz.tool.werecord.impl.model.account.Account;
import xjunz.tool.werecord.util.IoUtils;
import xjunz.tool.werecord.util.RxJavaUtils;

/**
 * @author xjunz 2021/2/17 0:09
 */
public class DatabaseExporter extends Exporter {
    private File mTempPlainTextDbFile;

    public DatabaseExporter() {
        super();
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
            zipOutputStream.write(getDbExportReadme().getBytes());
            zipOutputStream.closeEntry();
            zipOutputStream.putNextEntry(new ZipEntry("decrypted.db"));
            IoUtils.transferStreamNoCloseOutStream(new FileInputStream(mTempPlainTextDbFile), zipOutputStream);
            zipOutputStream.closeEntry();
            zipOutputStream.flush();
            zipOutputStream.close();
        }).doFinally(() -> IoUtils.deleteFile(mTempPlainTextDbFile));
    }

    @Override
    public String getExportFileName() {
        return App.getStringOf(R.string.format_export_db_name, getCurrentDate()).concat(".zip");
    }

    @Override
    public String getExportableName() {
        if (TextUtils.isEmpty(getExportDbPassword())) {
            return App.getStringOf(R.string.decrypted_database);
        } else {
            return App.getStringOf(R.string.original_database);
        }
    }

    @Override
    protected void initCustomConfigs(@NonNull List<Config<?>> configList) {
        //do nothing
    }
}
