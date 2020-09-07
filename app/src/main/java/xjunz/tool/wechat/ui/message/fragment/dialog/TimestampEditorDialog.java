/*
 * Copyright (c) 2020 xjunz. 保留所有权利
 */
package xjunz.tool.wechat.ui.message.fragment.dialog;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import xjunz.tool.wechat.R;
import xjunz.tool.wechat.data.viewmodel.EditorViewModel;
import xjunz.tool.wechat.databinding.DialogTimestampEditorBinding;

public class TimestampEditorDialog extends DialogFragment {
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, R.style.Base_Dialog_SenderChooser);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        DialogTimestampEditorBinding binding = DataBindingUtil.inflate(getLayoutInflater(), R.layout.dialog_timestamp_editor, container, false);
        EditorViewModel model = new ViewModelProvider(requireActivity(), new ViewModelProvider.NewInstanceFactory()).get(EditorViewModel.class);
        binding.setModel(model);
        binding.setDialog(this);
        return binding.getRoot();
    }

}
