/*
 * Copyright (c) 2021 xjunz. 保留所有权利
 */
package xjunz.tool.werecord.ui.export;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;

import java.io.IOException;

import io.reactivex.CompletableObserver;
import io.reactivex.disposables.Disposable;
import xjunz.tool.werecord.R;
import xjunz.tool.werecord.databinding.ActivityContactExportBinding;
import xjunz.tool.werecord.impl.model.export.ContactExporter;
import xjunz.tool.werecord.impl.model.export.ExporterRegistry;
import xjunz.tool.werecord.ui.base.ProgressDialog;
import xjunz.tool.werecord.ui.customview.MasterToast;
import xjunz.tool.werecord.util.UiUtils;

/**
 * @author xjunz 2021/2/5 1:07
 */
public class ContactExportActivity extends ExporterActivity {
    public static final String EXTRA_SOURCE_COUNT = "ContactExportActivity.extra.SourceCount";

    @Override
    protected void onCreateNormally(@Nullable Bundle savedInstanceState) {
        super.onCreateNormally(savedInstanceState);
        ActivityContactExportBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_contact_export);
        binding.setCount(getIntent().getIntExtra(EXTRA_SOURCE_COUNT, 0));
        mExporter = (ContactExporter) ExporterRegistry.getInstance().obtain();
        if (mExporter != null) {
            binding.rvConfig.setAdapter(new ConfigItemAdapter());
        } else {
            finish();
            MasterToast.shortToast(R.string.error_occurred);
        }
    }

    public void cancelExport(View view) {
        finishAfterTransition();
    }

    public void confirmExport(View view) {
        try {
            ProgressDialog progress = ProgressDialog.build(this).setTitle(R.string.exporting);
            mExporter.exportToAsync(createTempOutputFile("contact"), null)
                    .doOnDispose(this::clearCacheIfExists)
                    .doOnError(e -> clearCacheIfExists())
                    .doFinally(progress::dismiss)
                    .subscribe(new CompletableObserver() {
                        @Override
                        public void onSubscribe(@NonNull Disposable d) {
                            progress.setCancelableAction(d::dispose).show();
                        }

                        @Override
                        public void onComplete() {
                            new ExportShowcaseDialog().setFilename(mExporter.getExportFileName()).setFile(getLatestOutputFile()).show(getSupportFragmentManager(), "export_showcase");
                        }

                        @Override
                        public void onError(@NonNull Throwable e) {
                            UiUtils.showError(ContactExportActivity.this, e);
                        }
                    });
        } catch (IOException e) {
            MasterToast.shortToast(R.string.error_occurred);
        }
    }
}
