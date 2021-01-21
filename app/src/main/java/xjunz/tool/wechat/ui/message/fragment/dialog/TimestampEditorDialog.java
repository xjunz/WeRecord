/*
 * Copyright (c) 2021 xjunz. 保留所有权利
 */
package xjunz.tool.wechat.ui.message.fragment.dialog;

import android.os.Bundle;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ObservableField;
import androidx.databinding.ObservableLong;

import org.jetbrains.annotations.NotNull;

import xjunz.tool.wechat.R;
import xjunz.tool.wechat.databinding.DialogTimestampEditorBinding;
import xjunz.tool.wechat.ui.base.ConfirmationDialog;
import xjunz.tool.wechat.util.Utils;

public class TimestampEditorDialog extends ConfirmationDialog<Long> {
    private final ObservableField<String> hint = new ObservableField<>();
    private final ObservableLong parsed = new ObservableLong(-1L);

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        DialogTimestampEditorBinding binding = DataBindingUtil.inflate(getLayoutInflater(), R.layout.dialog_timestamp_editor, container, false);
        binding.setHost(this);
        return binding.getRoot();
    }

    public void reset() {
        parsed.set(def);
        parsed.notifyChange();
    }

    @Override
    public Long getResult() {
        return parsed.get();
    }

    public ObservableField<String> getHint() {
        return hint;
    }

    public ObservableLong getParsed() {
        return parsed;
    }


    public void notifyTextChanged(@NotNull Editable editable) {
        String text = editable.toString();
        long timestamp = Utils.parseDate(text);
        parsed.set(-1L);
        if (timestamp < 0) {
            hint.set(getString(R.string.timestamp_parse_failed));
        } else {
            if (timestamp > System.currentTimeMillis()) {
                hint.set(getString(R.string.error_time_traveller));
            } else {
                parsed.set(timestamp);
                hint.set(Utils.formatDateChinese(timestamp));
            }
        }
    }
}
