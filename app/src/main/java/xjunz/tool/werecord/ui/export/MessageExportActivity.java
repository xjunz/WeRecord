/*
 * Copyright (c) 2021 xjunz. 保留所有权利
 */
package xjunz.tool.werecord.ui.export;

import android.graphics.Color;
import android.os.Bundle;
import android.transition.TransitionInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.RecyclerView;

import java.io.IOException;
import java.util.List;

import io.reactivex.CompletableObserver;
import io.reactivex.disposables.Disposable;
import xjunz.tool.werecord.R;
import xjunz.tool.werecord.databinding.ActivityMessageExportBinding;
import xjunz.tool.werecord.databinding.ItemExportBinding;
import xjunz.tool.werecord.impl.model.account.Account;
import xjunz.tool.werecord.impl.model.export.Exporter;
import xjunz.tool.werecord.ui.base.ProgressDialog;
import xjunz.tool.werecord.ui.customview.MasterToast;
import xjunz.tool.werecord.util.UiUtils;

/**
 * @author xjunz 2021/1/26 20:21
 */
public class MessageExportActivity extends ExporterActivity {
    private ActivityMessageExportBinding mBinding;
    public static final String EXTRA_FROM_MULTI_SELECTION = "ExportActivity.extra.FromMultiSelection";

    @Override
    protected void onCreateNormally(@Nullable Bundle savedInstanceState) {
        super.onCreateNormally(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_message_export);
        mBinding.setLabel(mExporter.getExportableName());
        mBinding.rvConfig.setAdapter(new ConfigItemAdapter());
        mBinding.rvSource.setAdapter(new SourceItemAdapter());
        if (getIntent().getBooleanExtra(EXTRA_FROM_MULTI_SELECTION, false)) {
            mBinding.background.setBackgroundColor(Color.TRANSPARENT);
            getWindow().setSharedElementEnterTransition(TransitionInflater.from(this).inflateTransition(R.transition.message_export_shared_enter_from_multi_selection));
        }
    }

    /**
     * 默认使用SAF导出文件，如果有错误（如某些辣鸡<s>兲朝</s>ROM可能禁用SAF），采用root fallback导出文件
     */
    public void export(View view) {
        if (mExporter.getSourceList().isEmpty()) {
            MasterToast.shortToast(R.string.no_data);
            return;
        }
        if (mExporter.getExportFormat() == Exporter.Format.HTML) {
            UiUtils.createAlert(this, R.string.under_construction).setPositiveButton(android.R.string.ok, null).show();
            return;
        }
        try {
            ProgressDialog progress = ProgressDialog.build(this).setTitle(R.string.exporting);
            mExporter.exportToAsync(createTempOutputFile("message"), new Exporter.OnProgressListener() {
                @Override
                public void onGetTotalProgress(int total) {
                    if (total != 0) {
                        progress.setDeterminate(true).setMaxProgress(total);
                    }
                }

                @Override
                public void onProgressUpdate(int current) {
                    progress.setProgress(current);
                }
            }).doOnError(t -> clearCacheIfExists()).doOnDispose(this::clearCacheIfExists).doFinally(progress::dismiss).subscribe(new CompletableObserver() {
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
                    UiUtils.showError(MessageExportActivity.this, e);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class SourceItemAdapter extends RecyclerView.Adapter<SourceItemAdapter.SourceItemViewHolder> {
        private final List<? extends Account> mSources;

        public SourceItemAdapter() {
            super();
            mSources = mExporter.getSourceList();
        }

        @NonNull
        @Override
        public SourceItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new SourceItemViewHolder(ItemExportBinding.inflate(getLayoutInflater(), parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull SourceItemViewHolder holder, int position) {
            holder.binding.setAccount(mSources.get(position));
            holder.binding.executePendingBindings();
        }

        @Override
        public int getItemCount() {
            return mSources.size();
        }

        private class SourceItemViewHolder extends RecyclerView.ViewHolder {
            private final ItemExportBinding binding;

            public SourceItemViewHolder(@NonNull ItemExportBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
                binding.ibDelete.setOnClickListener(v -> {
                    int position = getAdapterPosition();
                    mSources.remove(position);
                    mBinding.rvSource.getAdapter().notifyItemRemoved(position);
                });
            }
        }
    }


}
