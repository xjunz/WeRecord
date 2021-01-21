/*
 * Copyright (c) 2021 xjunz. 保留所有权利
 */
package xjunz.tool.wechat.ui.message.fragment.dialog;

import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.text.method.DigitsKeyListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.RecyclerView;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

import xjunz.tool.wechat.R;
import xjunz.tool.wechat.databinding.FragmentLvbufferEditorBinding;
import xjunz.tool.wechat.databinding.ItemLvbufferEditorBinding;
import xjunz.tool.wechat.impl.model.message.util.LvBufferUtils;
import xjunz.tool.wechat.ui.base.ConfirmationDialog;
import xjunz.tool.wechat.ui.base.ContentEditorDialog;
import xjunz.tool.wechat.ui.customview.MasterToast;
import xjunz.tool.wechat.util.Utils;

import static xjunz.tool.wechat.impl.model.message.util.LvBufferUtils.TYPE_BUFFER;
import static xjunz.tool.wechat.impl.model.message.util.LvBufferUtils.TYPE_INTEGER;
import static xjunz.tool.wechat.impl.model.message.util.LvBufferUtils.TYPE_LONG;
import static xjunz.tool.wechat.impl.model.message.util.LvBufferUtils.TYPE_STRING;

public class LvBufferEditorDialog extends ConfirmationDialog<Object[]> {
    private FragmentLvbufferEditorBinding mBinding;
    private int[] mTypeSerial;
    private Object[] datum;
    private LvBufferAdapter mAdapter;

    @Override
    public int getStyleRes() {
        return R.style.Base_Dialog_Translucent_NoDim;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public LvBufferEditorDialog setTypeSerial(int[] typeSerial) {
        mTypeSerial = typeSerial;
        return this;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_lvbuffer_editor, container, false);
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mAdapter = new LvBufferAdapter();
        mBinding.rvEditor.setAdapter(mAdapter);
        mBinding.setHost(this);
    }

    @Override
    public ConfirmationDialog<Object[]> setDefault(@Nullable Object[] def) {
        if (def != null) {
            datum = Arrays.copyOf(def, def.length);
        } else {
            datum = new Object[mTypeSerial.length];
            for (int i = 0; i < datum.length; i++) {
                if (mTypeSerial[i] == TYPE_INTEGER) {
                    datum[i] = 0;
                }
            }
        }
        return super.setDefault(def);
    }

    @Override
    public boolean isChanged(Object[] newValue) {
        //如果原消息是NULL
        if (def == null) {
            for (int i = 0; i < datum.length; i++) {
                if (mTypeSerial[i] == TYPE_INTEGER) {
                    if ((Integer) datum[i] != 0) {
                        return true;
                    }
                } else if (datum[i] != null) {
                    return true;
                }
            }
            return false;
        }
        return !Utils.arrayDeepEquals(def, newValue);
    }

    @Override
    public Object[] getResult() {
        return datum;
    }

    @NotNull
    private String getTypeName(int type) {
        switch (type) {
            case TYPE_INTEGER:
                return "INTEGER";
            case TYPE_STRING:
                return "STRING";
            case TYPE_BUFFER:
                return "BUFFER";
            case TYPE_LONG:
                return "LONG";
        }
        throw new IllegalArgumentException("No such type: " + type);
    }

    private String getPreview(Object data) {
        return data instanceof byte[] ? Arrays.toString((byte[]) data) : data == null ? null : data.toString();
    }

    ;

    private class LvBufferAdapter extends RecyclerView.Adapter<LvBufferViewHolder> {
        @NonNull
        @Override
        public LvBufferViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new LvBufferViewHolder(DataBindingUtil.inflate(getLayoutInflater(), R.layout.item_lvbuffer_editor, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull LvBufferViewHolder holder, int position) {
            Object data = datum[position];
            if (mTypeSerial != null) {
                int type = mTypeSerial[position];
                holder.binding.setType(getTypeName(type));
            } else {
                if (data != null) {
                    holder.binding.setType(data.getClass().getSimpleName());
                } else {
                    holder.binding.setType(null);
                }
            }
            holder.binding.setPreview(getPreview(datum[position]));
            holder.binding.setOrdinal(position + 1);
            holder.binding.executePendingBindings();
        }

        @Override
        public int getItemCount() {
            return datum.length;
        }
    }

    private class LvBufferViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private final ItemLvbufferEditorBinding binding;

        public LvBufferViewHolder(@NonNull ItemLvbufferEditorBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            this.binding.getRoot().setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            int pos = getAdapterPosition();
            Object data = datum[pos];
            int type = mTypeSerial[pos];
            new ContentEditorDialog().configEditText(et -> {
                switch (type) {
                    case TYPE_INTEGER:
                        et.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED);
                        break;
                    case TYPE_BUFFER:
                        et.setFilters(new InputFilter[]{new DigitsKeyListener(getResources().getConfiguration().locale) {
                            @Override
                            protected char[] getAcceptedChars() {
                                return new char[]{'-', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '[', ',', ']', ' '};
                            }
                        }});
                        break;
                }
            }).setDefault(getPreview(data)).setCallback(result -> {
                switch (type) {
                    case TYPE_STRING:
                        if (result != null && result.getBytes().length > LvBufferUtils.MAX_BYTES_LENGTH) {
                            MasterToast.shortToast(R.string.error_over_length);
                            return false;
                        }
                        datum[pos] = result;
                        mAdapter.notifyItemChanged(pos);
                        return true;
                    case TYPE_INTEGER:
                        try {
                            datum[pos] = Integer.parseInt(result);
                            mAdapter.notifyItemChanged(pos);
                            return true;
                        } catch (NumberFormatException e) {
                            MasterToast.shortToast(R.string.error_number_format);
                        }
                    case TYPE_BUFFER:
                        if (result.startsWith("[") && result.endsWith("]")) {
                            String[] raw = result.substring(1, result.length() - 1).split(",");
                            byte[] bytes = new byte[raw.length];
                            if (bytes.length > LvBufferUtils.MAX_BYTES_LENGTH) {
                                MasterToast.shortToast(R.string.error_over_length);
                                return false;
                            }
                            try {
                                for (int i = 0; i < raw.length; i++) {
                                    bytes[i] = Byte.parseByte(raw[i].trim());
                                }
                                datum[pos] = bytes;
                                mAdapter.notifyItemChanged(pos);
                                return true;
                            } catch (NumberFormatException e) {
                                MasterToast.shortToast(R.string.error_number_format);
                            }
                        } else {
                            MasterToast.shortToast(R.string.error_number_format);
                        }
                        break;
                }
                return false;
            }).show(requireFragmentManager(), "lvbuffer:" + pos);
        }
    }
}
