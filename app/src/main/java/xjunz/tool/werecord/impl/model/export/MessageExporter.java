/*
 * Copyright (c) 2021 xjunz. 保留所有权利
 */
package xjunz.tool.werecord.impl.model.export;

import android.content.ContentValues;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.databinding.Observable;
import androidx.databinding.library.baseAdapters.BR;

import net.sqlcipher.Cursor;
import net.sqlcipher.DatabaseUtils;
import net.sqlcipher.database.SQLiteDatabase;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
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
import xjunz.tool.werecord.impl.model.account.Talker;
import xjunz.tool.werecord.impl.model.message.Message;
import xjunz.tool.werecord.impl.repo.MessageRepository;
import xjunz.tool.werecord.impl.repo.RepositoryFactory;
import xjunz.tool.werecord.impl.repo.AvatarRepository;
import xjunz.tool.werecord.util.DbUtils;
import xjunz.tool.werecord.util.IoUtils;
import xjunz.tool.werecord.util.RxJavaUtils;
import xjunz.tool.werecord.util.UiUtils;
import xjunz.tool.werecord.util.Utils;

import static xjunz.tool.werecord.util.DbUtils.buildValuesFromCursorReuse;

/**
 * @author xjunz 2021/1/29 12:55
 */
public class MessageExporter extends Exporter {
    private Config<Boolean> mCustomTimeSpanConfig;
    private Config<Long> mStartTimeConfig;
    private Config<Long> mStopTimeConfig;
    private final List<Talker> mTalkers;

    public MessageExporter(List<Talker> talkers) {
        super();
        mTalkers = talkers;
        getFormatConfig().setHelpTextRes(R.string.help_message_export_format);
    }

    @Override
    public List<? extends Account> getSourceList() {
        return mTalkers;
    }

    public MessageExporter(Talker talker) {
        super();
        mTalkers = new ArrayList<>();
        mTalkers.add(talker);
        getFormatConfig().setHelpTextRes(R.string.help_message_export_format);
    }

    @Override
    public Format[] getSupportFormats() {
        return new Format[]{Format.TXT, Format.HTML, Format.CIPHER_DB};
    }


    @NotNull
    private String generateQuerySqlFromConfig() {
        StringBuilder clause = new StringBuilder();
        if (mCustomTimeSpanConfig.getValue()) {
            long startLimit = mStartTimeConfig.getValue();
            if (startLimit != -1) {
                clause.append(" and createTime > ").append(startLimit);
            }
            long stopLimit = mStopTimeConfig.getValue();
            if (stopLimit != -1) {
                clause.append(" and createTime < ").append(stopLimit);
            }
        }
        clause.append(" order by createTime");
        return clause.toString();
    }

    @Override
    protected Completable exportAsToAsync(@NotNull Format format, @NotNull @NonNull File outputFile, @NotNull OnProgressListener listener) {
        if (mTalkers.isEmpty()) {
            return null;
        }
        MessageRepository repository = RepositoryFactory.get(MessageRepository.class);
        String clause = generateQuerySqlFromConfig();
        switch (format) {
            case TXT:
                if (mTalkers.size() == 1) {
                    Talker talker = mTalkers.get(0);
                    return RxJavaUtils.complete(() -> {
                        OutputStream outputStream = new FileOutputStream(outputFile);
                        String sql = String.format("talker='%s'", talker.id) + clause;
                        List<Message> messages = repository.rawQueryMessageByTalker(sql);
                        if (!messages.isEmpty()) {
                            listener.onGetTotalProgress(messages.size());
                            String header = App.getStringOf(R.string.template_message_export_header, talker.getIdentifier(),
                                    Utils.formatDate(System.currentTimeMillis()), messages.size());
                            outputStream.write(header.getBytes());
                            for (int i = 0; i < messages.size(); i++) {
                                Message message = messages.get(i);
                                listener.onProgressUpdate(i + 1);
                                outputStream.write((message.exportAsPlainText() + "\n").getBytes());
                            }
                            outputStream.flush();
                            outputStream.close();
                        }
                    });
                } else {
                    //多源的话，先并行导出为txt，然后全部压缩为zip
                    File[] txtFiles = new File[mTalkers.size()];
                    listener.onGetTotalProgress(txtFiles.length * 2);
                    AtomicInteger progress = new AtomicInteger();
                    return Flowable.range(0, mTalkers.size()).parallel().runOn(Schedulers.io())
                            .doOnNext(index -> {
                                Talker talker = mTalkers.get(index);
                                File txt = File.createTempFile(talker.id, null);
                                txtFiles[index] = txt;
                                FileOutputStream txtOut = new FileOutputStream(txt, true);
                                String sql = String.format("talker='%s'", talker.id) + clause;
                                List<Message> messages = repository.rawQueryMessageByTalker(sql);
                                String header = App.getStringOf(R.string.template_message_export_header, talker.getIdentifier(),
                                        Utils.formatDate(System.currentTimeMillis()), messages.size());
                                txtOut.write(header.getBytes());
                                for (Message message : messages) {
                                    txtOut.write((message.exportAsPlainText() + "\n").getBytes());
                                }
                                txtOut.flush();
                                txtOut.close();
                                listener.onProgressUpdate(progress.incrementAndGet());
                            }).sequential().doOnComplete(() -> {
                                OutputStream outputStream = new FileOutputStream(outputFile);
                                ZipOutputStream zip = new ZipOutputStream(outputStream);
                                for (int i = 0; i < txtFiles.length; i++) {
                                    zip.putNextEntry(new ZipEntry(mTalkers.get(i).getName() + format.getFileSuffix()));
                                    InputStream in = new FileInputStream(txtFiles[i]);
                                    byte[] buffer = new byte[10 * 1024];
                                    int count;
                                    while ((count = in.read(buffer)) != -1) {
                                        zip.write(buffer, 0, count);
                                    }
                                    in.close();
                                    zip.closeEntry();
                                    IoUtils.deleteFile(txtFiles[i]);
                                    listener.onProgressUpdate(progress.incrementAndGet());
                                }
                                zip.flush();
                                zip.close();
                            }).observeOn(AndroidSchedulers.mainThread()).ignoreElements();
                }
            case CIPHER_DB:
                return Flowable.just(true).flatMapCompletable(yes -> {
                    String msgTableName = MessageRepository.TABLE_MESSAGE;
                    File dbFile = File.createTempFile("database", null);
                    SQLiteDatabase exportDb = SQLiteDatabase.openOrCreateDatabase(dbFile, getExportDbPassword(), null);
                    SQLiteDatabase workerDb = Environment.getInstance().getWorkerDatabase();
                    exportDb.execSQL(DbUtils.getTableCreateSql(msgTableName));
                    exportDb.setLockingEnabled(false);
                    return Completable.create(emitter -> {
                        DatabaseUtils.InsertHelper helper = new DatabaseUtils.InsertHelper(exportDb, msgTableName);
                        ContentValues reuseValues = new ContentValues();
                        exportDb.beginTransaction();
                        for (Talker talker : mTalkers) {
                            int progress = 0;
                            String querySql = String.format("select * from %s where talker='%s'", msgTableName, talker.id) + clause;
                            try (Cursor cursor = workerDb.rawQuery(querySql, null)) {
                                long size = cursor.getCount();
                                listener.onGetTotalProgress((int) size);
                                while (cursor.moveToNext()) {
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
                        zipOutputStream.putNextEntry(new ZipEntry("messages.db"));
                        IoUtils.transferStreamNoCloseOutStream(new FileInputStream(dbFile), zipOutputStream);
                        zipOutputStream.closeEntry();
                        zipOutputStream.flush();
                        zipOutputStream.close();
                        emitter.onComplete();
                    }).doFinally(() -> IoUtils.deleteFile(dbFile));
                }).subscribeOn(Schedulers.newThread()).observeOn(AndroidSchedulers.mainThread());
            case HTML:
                if (mTalkers.size() == 1) {
                    Talker talker = mTalkers.get(0);
                    return RxJavaUtils.complete(() -> {
                        OutputStream outputStream = new FileOutputStream(outputFile);
                        String sql = String.format("talker='%s'", talker.id) + clause;
                        List<Message> messages = repository.rawQueryMessageByTalker(sql);
                        if (!messages.isEmpty()) {
                            listener.onGetTotalProgress(messages.size());
                            String header = String.format("%s;var msgData=\"[{\\\"ExportTime\\\":\\\"%s\\\"," +
                                    "\\\"Source\\\":\\\"%s\\\"," +
                                    "\\\"MsgSize\\\":\\\"%s\\\"},",
                                    IoUtils.readAssetAsString("exportHtmlTemplate.txt"),
                                    Utils.formatDate(System.currentTimeMillis()),
                                    talker.getIdentifier(),
                                    messages.size());
                            outputStream.write(header.getBytes());
                            for (int i = 0; i < messages.size(); i++) {
                                Message message = messages.get(i);
                                listener.onProgressUpdate(i + 1);
                                outputStream.write((message.exportAsHtml() + ((i != messages.size() - 1) ? "," : "")).getBytes());
                            }
                            String footer = "]\";" + IoUtils.readAssetAsString("exportJsTemplate.txt") + "</script></body></html>";
                            outputStream.write(footer.getBytes());

                            outputStream.flush();
                            outputStream.close();
                        }
                    });
                } else {
                    break;
                }
        }
        return null;
    }

    private boolean isSingleSource() {
        return mTalkers.size() == 1;
    }

    @Override
    public String getExportFileName() {
        Format format = getExportFormat();
        switch (getExportFormat()) {
            case CIPHER_DB:
                if (isSingleSource()) {
                    //[数据库]XJUNZ(2021-2-8 21_00_12).zip
                    return String.format("[%s]", format.getName()) + mTalkers.get(0).getName() + String.format("(%s)", getCurrentDate()) + format.getExportSuffix();
                }
                //[数据库]导出的消息(2021-2-8 21_00_00).zip
                return App.getStringOf(R.string.format_export_name, format.getName(), getExportableName(), getCurrentDate()) + format.getExportSuffix();
            case TXT:
                if (isSingleSource()) {
                    //[聊天记录]XJUNZ(2021-2-8 21_00_21).txt
                    return String.format("[%s]", getExportableName()) + mTalkers.get(0).getName() + String.format("(%s)", getCurrentDate()) + format.getExportSuffix();
                }
                //[文本]导出的聊天记录(2021-2-8 21_00_00).zip
                return App.getStringOf(R.string.format_export_name, format.getName(), getExportableName(), getCurrentDate()) + ".zip";
            case HTML:
                if (isSingleSource()) {
                    //[聊天记录]XJUNZ(2021-2-8 21_00_21).html
                    return String.format("[%s]", getExportableName()) + mTalkers.get(0).getName() + String.format("(%s)", getCurrentDate()) + format.getExportSuffix();
                }
        }
        throw new IllegalArgumentException("Unsupported format: " + format.getName());
    }

    @Override
    public String getExportableName() {
        return App.getStringOf(R.string.chat_records);
    }

    @Override
    protected void initCustomConfigs(@NonNull List<Config<?>> configs) {
        configs.add(mCustomTimeSpanConfig = new SwitchConfig(R.string.export_time_span).setDefValue(false)
                .setPreview(custom -> App.getStringOf(custom ? R.string.custom : R.string.all)));
        configs.add(mStartTimeConfig = new DateConfig(R.string.start_time).setValueEnum(-1L, -2L).setEnabled(false));
        configs.add(mStopTimeConfig = new DateConfig(R.string.stop_time).setValueEnum(-1L, -2L).setEnabled(false));
        mCustomTimeSpanConfig.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
            @Override
            public void onPropertyChanged(Observable sender, int propertyId) {
                if (propertyId == BR.value) {
                    mStartTimeConfig.setEnabled(mCustomTimeSpanConfig.getValue());
                    mStopTimeConfig.setEnabled(mCustomTimeSpanConfig.getValue());
                }
            }
        });
    }
}
