/*
 * Copyright (c) 2021 xjunz. 保留所有权利
 */
package xjunz.tool.werecord.ui.message.fragment.dialog;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.DialogFragment;

import xjunz.tool.werecord.R;
import xjunz.tool.werecord.databinding.DialogTemplateSetupBinding;
import xjunz.tool.werecord.impl.model.message.util.Template;
import xjunz.tool.werecord.impl.model.message.util.TemplateManager;
import xjunz.tool.werecord.ui.base.SingleLineEditorDialog;
import xjunz.tool.werecord.ui.customview.MasterToast;

public class TemplateSetupDialog extends DialogFragment {
    private DialogTemplateSetupBinding mBinding;
    private Template mSource;
    private TemplateManager mManager;

    public TemplateSetupDialog setCallback(Runnable callback) {
        mCallback = callback;
        return this;
    }

    private Runnable mCallback;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NO_TITLE, R.style.Base_Dialog_Translucent_NoDim);
        mManager = TemplateManager.getInstance();
    }

    public TemplateSetupDialog setSourceTemplate(@NonNull Template source) {
        mSource = source;
        return this;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.dialog_template_setup, container, false);
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mBinding.lineCounter.bindTo(mBinding.etEditor);
        mBinding.setTemplate(mSource);
        mBinding.setHost(this);
    }


    public void confirm() {
        if (!mSource.isCustom()) {
            dismiss();
            return;
        }
        String label = getString(R.string.template_name);
        final EditText[] et = new EditText[1];
        new SingleLineEditorDialog().setLabel(label).setConfig(passed -> et[0] = passed).setAllowUnchanged(true).setDefault(mSource.getName())
                .setPredicateCallback(passed -> {
                    if (passed.length() == 0) {
                        MasterToast.shortToast(getString(R.string.format_require_nonnull, label));
                        return false;
                    }
                    boolean existed = mManager.isCustomTemplateExists(mSource);
                    if (!existed && mManager.isCustomNameExists(passed)) {
                        MasterToast.shortToast(getString(R.string.format_require_unique, passed));
                        return false;
                    }
                    mSource.setContent(mBinding.etEditor.getText().toString());
                    try {
                        mSource.initRepGroups();
                    } catch (Template.TypeNotConsistentException e) {
                        //如果检测到type不一致
                        MasterToast.shortToast(R.string.error_type_inconsistent);
                        et[0].setSelection(e.conflictSelection[0], e.conflictSelection[1]);
                        return false;
                    }
                    mSource.setName(passed);
                    if (existed) {
                        mManager.replaceCustomTemplate(mSource);
                        MasterToast.shortToast(getString(R.string.format_template_edited, passed));
                    } else {
                        mManager.addCustomTemplate(mSource);
                        MasterToast.shortToast(getString(R.string.format_template_added, passed));
                    }
                    if (mCallback != null) {
                        mCallback.run();
                    }
                    dismiss();
                    return true;
                }).show(requireFragmentManager(), "template_name");
    }
}
