/*
 * Copyright (c) 2021 xjunz. 保留所有权利
 */
package xjunz.tool.werecord.ui.export;

import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.PopupMenu;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.IOException;
import java.util.List;

import xjunz.tool.werecord.R;
import xjunz.tool.werecord.databinding.ItemExportConfigBinding;
import xjunz.tool.werecord.impl.model.export.Exporter;
import xjunz.tool.werecord.impl.model.export.Exporter.Config;
import xjunz.tool.werecord.impl.model.export.Exporter.DateConfig;
import xjunz.tool.werecord.impl.model.export.Exporter.EnumConfig;
import xjunz.tool.werecord.impl.model.export.Exporter.PasswordConfig;
import xjunz.tool.werecord.impl.model.export.Exporter.SwitchConfig;
import xjunz.tool.werecord.impl.model.export.ExporterRegistry;
import xjunz.tool.werecord.ui.base.RecycleAwareActivity;
import xjunz.tool.werecord.ui.base.SingleLineEditorDialog;
import xjunz.tool.werecord.ui.customview.MasterToast;
import xjunz.tool.werecord.ui.message.fragment.dialog.TimestampEditorDialog;
import xjunz.tool.werecord.util.IoUtils;

/**
 * @author xjunz 2021/2/8 13:56
 */
public class ExporterActivity extends RecycleAwareActivity {
    private File mLatestOutputFile;
    protected Exporter mExporter;

    @Override
    protected void onCreateNormally(@Nullable Bundle savedInstanceState) {
        mExporter = ExporterRegistry.getInstance().obtain();
        if (mExporter == null) {
            finish();
            MasterToast.shortToast(R.string.error_occurred);
        }
    }

    protected File getLatestOutputFile() {
        return mLatestOutputFile;
    }

    /**
     * 清理上次导出的文件缓存
     */
    protected synchronized void clearCacheIfExists() {
        if (mLatestOutputFile != null && mLatestOutputFile.exists()) {
            IoUtils.deleteFileSync(mLatestOutputFile);
        }
    }

    protected File createTempOutputFile(String prefix) throws IOException {
        clearCacheIfExists();
        return mLatestOutputFile = File.createTempFile(prefix, null);
    }

    protected class ConfigItemAdapter extends RecyclerView.Adapter<ConfigItemAdapter.ConfigItemViewHolder> {
        private final List<? extends Exporter.Config<?>> mConfigs;

        ConfigItemAdapter() {
            mConfigs = mExporter.getConfigs();
        }

        @NonNull
        @Override
        public ConfigItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ConfigItemViewHolder(ItemExportConfigBinding.inflate(getLayoutInflater(), parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ConfigItemViewHolder holder, int position) {
            holder.binding.setConfig(mConfigs.get(position));
            holder.binding.executePendingBindings();
        }

        @Override
        public int getItemCount() {
            return mConfigs.size();
        }

        protected class ConfigItemViewHolder extends RecyclerView.ViewHolder {
            private final ItemExportConfigBinding binding;

            public ConfigItemViewHolder(@NonNull ItemExportConfigBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
                this.itemView.setOnClickListener(v -> onItemClicked(getAdapterPosition()));
            }

            private void onItemClicked(int position) {
                Config<?> config = mConfigs.get(position);
                if (config instanceof Exporter.DateConfig) {
                    DateConfig dateConfig = (DateConfig) config;
                    PopupMenu popupMenu = new PopupMenu(ExporterActivity.this, itemView, Gravity.END);
                    for (int i = 0; i < dateConfig.getValueEnum().length; i++) {
                        popupMenu.getMenu().add(0, i, 0, dateConfig.getPreview(i));
                    }
                    popupMenu.setOnMenuItemClickListener(item -> {
                        if (item.getItemId() == 0) {
                            dateConfig.setValue(-1L);
                        } else {
                            new TimestampEditorDialog().setLabel(dateConfig.getTitle())
                                    .setDefault(dateConfig.getValue() < 0 ? System.currentTimeMillis() : dateConfig.getValue())
                                    .setPassableCallback(dateConfig::setValue).setAllowUnchanged(true).show(getSupportFragmentManager(), "date");
                        }
                        return true;
                    });
                    popupMenu.show();
                } else if (config instanceof Exporter.EnumConfig) {
                    EnumConfig<?> enumConfig = (EnumConfig<?>) config;
                    PopupMenu menu = new PopupMenu(ExporterActivity.this, itemView, Gravity.END);
                    for (int i = 0; i < enumConfig.getValueEnum().length; i++) {
                        menu.getMenu().add(0, i, 0, enumConfig.getPreview(i));
                    }
                    menu.setOnMenuItemClickListener(item -> {
                        enumConfig.setValueEnumSelection(item.getItemId());
                        return true;
                    });
                    menu.show();
                } else if (config instanceof Exporter.SwitchConfig) {
                    SwitchConfig switchConfig = (SwitchConfig) config;
                    switchConfig.switchValue();
                } else if (config instanceof Exporter.PasswordConfig) {
                    PasswordConfig pwdConfig = (PasswordConfig) config;
                    new SingleLineEditorDialog().setConfig(et -> et.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD)).setLabel(config.getTitle()).setEditorTag("PASSWORD")
                            .setDefault(pwdConfig.getValue()).setPassableCallback(pwdConfig::setValue).show(getSupportFragmentManager(), "password");
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        clearCacheIfExists();
    }
}
