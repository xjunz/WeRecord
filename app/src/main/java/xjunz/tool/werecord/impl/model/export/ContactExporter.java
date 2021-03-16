/*
 * Copyright (c) 2021 xjunz. 保留所有权利
 */
package xjunz.tool.werecord.impl.model.export;

import android.content.ContentValues;

import androidx.annotation.NonNull;

import net.sqlcipher.Cursor;
import net.sqlcipher.DatabaseUtils;
import net.sqlcipher.database.SQLiteDatabase;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import xjunz.tool.werecord.App;
import xjunz.tool.werecord.R;
import xjunz.tool.werecord.impl.Environment;
import xjunz.tool.werecord.impl.model.account.Account;
import xjunz.tool.werecord.impl.model.account.Contact;
import xjunz.tool.werecord.impl.repo.ContactRepository;
import xjunz.tool.werecord.util.DbUtils;
import xjunz.tool.werecord.util.IoUtils;
import xjunz.tool.werecord.util.Returnable;
import xjunz.tool.werecord.util.RxJavaUtils;
import xjunz.tool.werecord.util.Utils;

import static xjunz.tool.werecord.util.DbUtils.buildValuesFromCursorReuse;

/**
 * @author xjunz 2021/1/28 21:43
 */
public class ContactExporter extends Exporter {
    private final Returnable<List<Contact>> mSourceGetter;
    private List<Contact> mContactList;
    private static String sTemplateExportContactTable;

    public ContactExporter(Returnable<List<Contact>> sourceGetter) {
        mSourceGetter = sourceGetter;
        getFormatConfig().setHelpTextRes(R.string.help_contact_export_format);
    }

    @Override
    public List<? extends Account> getSourceList() {
        return mContactList;
    }

    @Override
    public Format[] getSupportFormats() {
        return new Format[]{Format.TXT, Format.CIPHER_DB, Format.TABLE};
    }

    private void loadSourceList() {
        if (mContactList == null) {
            mContactList = mSourceGetter.get();
        }
    }


    @Override
    protected Completable exportAsToAsync(@NotNull Format format, @NotNull File outputFile, @NotNull OnProgressListener listener) {
        switch (format) {
            case TXT:
                return RxJavaUtils.complete(() -> {
                    loadSourceList();
                    OutputStream outputStream = new FileOutputStream(outputFile);
                    String header = App.getStringOf(R.string.template_contact_export_header, getCurrentDate(), mContactList.size());
                    outputStream.write(header.getBytes());
                    //全部写入一个txt文件
                    for (int i = 0; i < mContactList.size(); i++) {
                        Contact contact = mContactList.get(i);
                        outputStream.write(("[" + (i + 1) + "]\n").getBytes());
                        outputStream.write(contact.exportAsPlainText().getBytes());
                    }
                    outputStream.flush();
                    outputStream.close();
                });
            case TABLE:
                return RxJavaUtils.complete(() -> {
                    loadSourceList();
                    OutputStream outputStream = new FileOutputStream(outputFile);
                    if (sTemplateExportContactTable == null) {
                        sTemplateExportContactTable = IoUtils.readAssetAsString("template_contact_export_table.html");
                    }
                    sTemplateExportContactTable = String.format(sTemplateExportContactTable, Utils.formatDateLocally(System.currentTimeMillis()));
                    String[] split = sTemplateExportContactTable.split("#s");
                    outputStream.write(split[0].getBytes());
                    //全部写入一个html文件
                    for (int i = 0; i < mContactList.size(); i++) {
                        Contact contact = mContactList.get(i);
                        outputStream.write((contact.exportAsTableElement(i + 1) + "\n").getBytes());
                    }
                    outputStream.write(split[1].getBytes());
                    outputStream.flush();
                    outputStream.close();
                });
            case CIPHER_DB:
                return Flowable.just(true).flatMapCompletable(yes -> {
                    loadSourceList();
                    String contactTableName = ContactRepository.TABLE_CONTACT;
                    File dbFile = File.createTempFile("database", null);
                    SQLiteDatabase exportDb = SQLiteDatabase.openOrCreateDatabase(dbFile, getExportDbPassword(), null);
                    SQLiteDatabase workerDb = Environment.getInstance().getWorkerDatabase();
                    exportDb.execSQL(DbUtils.getTableCreateSql(contactTableName));
                    exportDb.setLockingEnabled(false);
                    return Completable.create(emitter -> {
                        DatabaseUtils.InsertHelper helper = new DatabaseUtils.InsertHelper(exportDb, contactTableName);
                        ContentValues reuseValues = new ContentValues();
                        exportDb.beginTransaction();
                        int progress = 0;
                        listener.onGetTotalProgress(mContactList.size());
                        for (Contact talker : mContactList) {
                            try (Cursor cursor = workerDb.rawQuery(String.format("select * from %s where username='%s'", contactTableName, talker.id), null)) {
                                if (cursor.moveToNext()) {
                                    helper.insert(buildValuesFromCursorReuse(cursor, reuseValues));
                                    listener.onProgressUpdate(++progress);
                                }
                            }
                        }
                        exportDb.setTransactionSuccessful();
                        exportDb.endTransaction();
                        exportDb.close();
                        //打包readme和数据库
                        ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(outputFile, true));
                        zipOutputStream.putNextEntry(new ZipEntry("readme.html"));
                        zipOutputStream.write(getDbExportReadme(null).getBytes());
                        zipOutputStream.closeEntry();
                        zipOutputStream.putNextEntry(new ZipEntry("contacts.db"));
                        IoUtils.transferStreamNoCloseOutStream(new FileInputStream(dbFile), zipOutputStream);
                        zipOutputStream.closeEntry();
                        zipOutputStream.flush();
                        zipOutputStream.close();
                        emitter.onComplete();
                    }).doFinally(() -> IoUtils.deleteFile(dbFile));
                }).subscribeOn(Schedulers.newThread()).observeOn(AndroidSchedulers.mainThread());
        }
        throw new IllegalArgumentException("Unsupported export format: " + format.getName());
    }

    @Override
    public String getExportFileName() {
        return App.getStringOf(R.string.format_export_name, getExportFormat().getName(), getExportableName(), getCurrentDate()) + getExportFormat().getExportSuffix();
    }

    @Override
    public String getExportableName() {
        return App.getStringOf(R.string.contact);
    }

    @Override
    protected void initCustomConfigs(@NonNull List<Config<?>> configList) {
        //do nothing
    }
}
