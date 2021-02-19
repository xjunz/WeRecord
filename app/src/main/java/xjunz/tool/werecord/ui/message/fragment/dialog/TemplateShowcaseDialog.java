/*
 * Copyright (c) 2021 xjunz. 保留所有权利
 */
package xjunz.tool.werecord.ui.message.fragment.dialog;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.RecyclerView;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import xjunz.tool.werecord.BuildConfig;
import xjunz.tool.werecord.R;
import xjunz.tool.werecord.databinding.DialogTemplateShowcaseBinding;
import xjunz.tool.werecord.databinding.ItemTemplateShowcaseBinding;
import xjunz.tool.werecord.impl.model.message.util.Template;
import xjunz.tool.werecord.impl.model.message.util.TemplateManager;
import xjunz.tool.werecord.util.Passable;

/**
 * @author xjunz 2021/1/3 19:55
 */
public class TemplateShowcaseDialog extends DialogFragment {
    private DialogTemplateShowcaseBinding mBinding;
    private TemplateManager mManager;
    private TemplateShowcaseAdapter mBuildInAdapter, mCustomAdapter;
    private Passable<Template> mCallback;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NO_TITLE, R.style.Base_Dialog_Normal);
        mManager = TemplateManager.getInstance();
    }

    public TemplateShowcaseDialog setCallback(Passable<Template> callback) {
        this.mCallback = callback;
        return this;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.dialog_template_showcase, container, false);
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mBuildInAdapter = new TemplateShowcaseAdapter(mManager.getBuildInTemplates());
        mCustomAdapter = new TemplateShowcaseAdapter(mManager.getCustomTemplates());
        mBinding.rvBuildIn.setAdapter(mBuildInAdapter);
        mBinding.rvCustom.setAdapter(mCustomAdapter);
        mBinding.setHost(this);
        mBinding.getRoot().post(() -> {
            mBinding.getRoot().requestLayout();
            mBinding.getRoot().requestApplyInsets();
        });
    }

    public void confirmTemplate(Template template) {
        if (mCallback != null) {
            mCallback.pass(template);
        }
        dismiss();
    }

    /**
     * 将自定义模板转为内置模板，仅限debug
     */
    public boolean convertToBuildInTemplate(@NotNull Template template) {
        if (template.isCustom() && BuildConfig.DEBUG) {
            mManager.convertCustomToBuildIn(template);
            mBuildInAdapter.notifyDataSetChanged();
            mCustomAdapter.notifyDataSetChanged();
        }
        return true;
    }

    public void editTemplate(Template template) {
        new TemplateSetupDialog().setSourceTemplate(template)
                .setCallback(() -> mCustomAdapter.notifyDataSetChanged()).show(requireFragmentManager(), "template_setup");
    }

    private class TemplateShowcaseAdapter extends RecyclerView.Adapter<TemplateShowcaseAdapter.CustomTemplateViewHolder> {
        List<Template> mTemplates;

        TemplateShowcaseAdapter(List<Template> templates) {
            mTemplates = templates;
        }

        @NonNull
        @Override
        public CustomTemplateViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new CustomTemplateViewHolder(DataBindingUtil.inflate(getLayoutInflater(), R.layout.item_template_showcase, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull CustomTemplateViewHolder holder, int position) {
            holder.binding.setTemplate(mTemplates.get(position));
            holder.binding.executePendingBindings();
        }

        @Override
        public int getItemCount() {
            return mTemplates.size();
        }

        private class CustomTemplateViewHolder extends RecyclerView.ViewHolder {
            ItemTemplateShowcaseBinding binding;

            public CustomTemplateViewHolder(@NonNull ItemTemplateShowcaseBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
                this.binding.setHost(TemplateShowcaseDialog.this);
                binding.ibDelete.setOnClickListener(v -> {
                    int position = getAdapterPosition();
                    Template template = mTemplates.get(position);
                    if (template.isCustom()) {
                        mManager.removeCustomTemplate(template);
                        mCustomAdapter.notifyItemRemoved(position);
                    } else {
                        mManager.removeBuildInTemplate(template);
                        mBuildInAdapter.notifyItemRemoved(position);
                    }
                });
                binding.getRoot().setOnLongClickListener(v -> {
                    int position = getAdapterPosition();
                    Template template = mTemplates.get(position);
                    if (template.isCustom() && BuildConfig.DEBUG) {
                        mManager.convertCustomToBuildIn(template);
                        mCustomAdapter.notifyItemRemoved(position);
                        mBuildInAdapter.notifyItemInserted(mBuildInAdapter.getItemCount() - 1);
                    }
                    return true;
                });
            }
        }
    }
}
