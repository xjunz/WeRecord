/*
 * Copyright (c) 2021 xjunz. 保留所有权利
 */
package xjunz.tool.werecord.ui.export;

import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.databinding.ObservableField;
import androidx.fragment.app.DialogFragment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Objects;

import io.reactivex.CompletableObserver;
import io.reactivex.disposables.Disposable;
import xjunz.tool.werecord.R;
import xjunz.tool.werecord.databinding.DialogExportShowcaseBinding;
import xjunz.tool.werecord.ui.base.SingleLineEditorDialog;
import xjunz.tool.werecord.ui.customview.MasterToast;
import xjunz.tool.werecord.util.ActivityUtils;
import xjunz.tool.werecord.util.IoUtils;
import xjunz.tool.werecord.util.RxJavaUtils;
import xjunz.tool.werecord.util.UiUtils;

import static xjunz.tool.werecord.ui.export.ExporterActivity.REQUEST_CODE_SAVE_EXPORT_FILE;

/**
 * @author xjunz 2021/2/18 18:33
 */
public class ExportShowcaseDialog extends DialogFragment {
    private DialogExportShowcaseBinding mBinding;
    private File mFile;
    private final ObservableField<String> mFilename = new ObservableField<>();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NO_FRAME, R.style.Base_Dialog_Normal);
    }

    public ExportShowcaseDialog setFile(File file) {
        mFile = file;
        return this;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return (mBinding = DialogExportShowcaseBinding.inflate(inflater, container, false)).getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mBinding.setHost(this);
        mBinding.setFilename(mFilename);
        mBinding.setFileSize(Formatter.formatFileSize(requireContext(), mFile.length()));
    }

    public ExportShowcaseDialog setFilename(String name) {
        mFilename.set(name);
        return this;
    }

    public void editFilename() {
        new SingleLineEditorDialog().setLabel(getString(R.string.filename)).setDefault(mFilename.get())
                .setAllowUnchanged(true)
                .setPassableCallback(mFilename::set)
                .show(requireFragmentManager(), "edit_file_name");
    }

    private void rename() {
        if (!Objects.equals(mFile.getName(), mFilename.get())) {
            File renamed = new File(mFile.getParent() + File.separator + mFilename.get());
            if (mFile.renameTo(renamed)) {
                mFile = renamed;
            } else {
                UiUtils.swing(mBinding.tvFilename);
                MasterToast.shortToast(R.string.error_occurred);
            }
        }
    }

    public void share() {
        rename();
        if (!Objects.equals(mFile.getName(), mFilename.get())) {
            File renamed = new File(mFile.getParent() + File.separator + mFilename.get());
            if (mFile.renameTo(renamed)) {
                mFile = renamed;
            } else {
                UiUtils.swing(mBinding.tvFilename);
                MasterToast.shortToast(R.string.error_occurred);
                return;
            }
        }
        Uri uri = FileProvider.getUriForFile(requireContext(), "xjunz.tool.werecord.fileprovider", mFile);
        Intent intent = new Intent(Intent.ACTION_SEND, uri)
                .putExtra(Intent.EXTRA_STREAM, uri)
                .setType(MimeTypeMap.getSingleton().getMimeTypeFromExtension(mFilename.get().substring(mFilename.get().lastIndexOf('.') + 1)))
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        ActivityUtils.startActivityCreateChooser(requireContext(), intent);
    }

    public void preview() {
        rename();
        Uri uri = FileProvider.getUriForFile(requireContext(), "xjunz.tool.werecord.fileprovider", mFile);
        Intent intent = new Intent(Intent.ACTION_VIEW, uri)
                .addCategory(Intent.CATEGORY_DEFAULT)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        ActivityUtils.startActivityCreateChooser(requireContext(), intent);
    }

    public void saveTo() {
        MasterToast.shortToast(R.string.pls_select_save_dir);
        try {
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            intent.putExtra(Intent.EXTRA_TITLE, mFilename.get());
            startActivityForResult(intent, REQUEST_CODE_SAVE_EXPORT_FILE);
        } catch (ActivityNotFoundException e) {
            MasterToast.shortToast(R.string.unable_to_open);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SAVE_EXPORT_FILE) {
            if (data != null) {
                Uri uri = data.getData();
                Dialog progress = UiUtils.createProgress(requireContext(), R.string.saving);
                RxJavaUtils.complete(() -> IoUtils.transferFileViaChannel(new FileInputStream(mFile), (FileOutputStream) requireActivity().getContentResolver().openOutputStream(uri))).doFinally(progress::dismiss).subscribe(new CompletableObserver() {
                    @Override
                    public void onSubscribe(@io.reactivex.annotations.NonNull Disposable d) {
                        progress.show();
                    }

                    @Override
                    public void onComplete() {
                        MasterToast.shortToast(R.string.export_successfully);
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        UiUtils.createError(requireContext(), e).show();
                    }
                });
            } else {
                MasterToast.shortToast(R.string.operation_cancelled);
            }
        }
    }

    /**
     * 清理上次导出的文件缓存
     */
    protected synchronized void clearCacheIfExists() {
        if (mFile != null && mFile.exists()) {
            IoUtils.deleteFileSync(mFile);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        clearCacheIfExists();
    }
}
