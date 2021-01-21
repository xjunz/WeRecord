/*
 * Copyright (c) 2021 xjunz. 保留所有权利
 */
package xjunz.tool.wechat.ui.message.fragment.dialog;

import android.content.ContentValues;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import xjunz.tool.wechat.R;
import xjunz.tool.wechat.databinding.ItemMessageViewerBinding;
import xjunz.tool.wechat.impl.model.message.Message;

public class MessageViewerDialog extends DialogFragment {
    private Message mVictim;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NO_TITLE, R.style.Base_Dialog_Translucent_NoDim);
    }

    public MessageViewerDialog setMessage(Message msg) {
        this.mVictim = msg;
        return this;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_message_viewer, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        RecyclerView rvViewer = view.findViewById(R.id.rv_message_viewer);
        rvViewer.setAdapter(new MessageViewAdapter());
    }

    private class MessageViewAdapter extends RecyclerView.Adapter<MessageViewerViewHolder> {
        private final List<String> keyList;
        private final ContentValues values;

        MessageViewAdapter() {
            values = mVictim.getValues();
            keyList = new ArrayList<>(values.keySet());
        }

        @NonNull
        @Override
        public MessageViewerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new MessageViewerViewHolder(DataBindingUtil.inflate(getLayoutInflater(), R.layout.item_message_viewer, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull MessageViewerViewHolder holder, int position) {
            String key = keyList.get(position);
            holder.binding.setKey(key);
            Object value = values.get(key);
            if (value instanceof byte[]) {
                holder.binding.setValue(new String((byte[]) value));
            } else {
                holder.binding.setValue(value == null ? null : value.toString());
            }
            holder.binding.setTag(value != null ? value.getClass().getSimpleName() : null);
            holder.binding.executePendingBindings();
        }

        @Override
        public int getItemCount() {
            return values.size();
        }
    }

    private static class MessageViewerViewHolder extends RecyclerView.ViewHolder {
        private final ItemMessageViewerBinding binding;

        public MessageViewerViewHolder(@NonNull ItemMessageViewerBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
