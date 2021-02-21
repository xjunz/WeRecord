/*
 * Copyright (c) 2021 xjunz. 保留所有权利
 */
package xjunz.tool.werecord.ui.message.fragment;

import android.content.ContentValues;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextUtils;
import android.text.method.DigitsKeyListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.BaseObservable;
import androidx.databinding.Bindable;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;
import androidx.recyclerview.widget.RecyclerView;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import xjunz.tool.werecord.BR;
import xjunz.tool.werecord.R;
import xjunz.tool.werecord.databinding.FragmentAdvancedEditorBinding;
import xjunz.tool.werecord.databinding.ItemAdvancedEditorBinding;
import xjunz.tool.werecord.databinding.ItemSeparatorBinding;
import xjunz.tool.werecord.impl.model.message.Message;
import xjunz.tool.werecord.impl.model.message.util.LvBufferUtils;
import xjunz.tool.werecord.impl.repo.MessageRepository;
import xjunz.tool.werecord.impl.repo.RepositoryFactory;
import xjunz.tool.werecord.impl.util.ContentType;
import xjunz.tool.werecord.ui.base.ContentEditorDialog;
import xjunz.tool.werecord.ui.base.EditorFragment;
import xjunz.tool.werecord.ui.base.SingleLineEditorDialog;
import xjunz.tool.werecord.ui.customview.MasterToast;
import xjunz.tool.werecord.ui.message.fragment.dialog.LvBufferEditorDialog;
import xjunz.tool.werecord.util.UiUtils;
import xjunz.tool.werecord.util.Utils;

public class AdvancedEditorFragment extends EditorFragment {
    private List<EditorItem> mEditorItems;
    private FragmentAdvancedEditorBinding mBinding;
    private MessageRepository mRepository;
    private EditorAdapter mAdapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mEditorItems = new ArrayList<>();
        mRepository = RepositoryFactory.get(MessageRepository.class);
        initItems();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_advanced_editor, container, false);
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mAdapter = new EditorAdapter();
        mBinding.rvEditor.setAdapter(mAdapter);
    }

    private void initItems() {
        mEditorItems.clear();
        //mEditorItems.add(new EditorItem(EditorItem.KEY_SEPARATOR, getString(R.string.universal)));
        ContentValues values = mVictim.getValues();
        for (String key : values.keySet()) {
            if (Message.KEY_LV_BUFFER.equals(key)) {
                mEditorItems.add(new EditorItem(key, Message.ABSTRACT_KEY_LVBUFFER, mVictim.getParsedLvBuffer(), true) {
                    @Nullable
                    @Override
                    public String getContentPreview() {
                        return this.getValue() == null ? null : Arrays.deepToString((Object[]) this.getValue());
                    }
                });
            } else if (Message.KEY_TALKER.equals(key) || Message.KEY_TYPE.equals(key) || Message.KEY_MSG_ID.equals(key)) {
                mEditorItems.add(new EditorItem(key, key, values.get(key), false));
            } else {
                mEditorItems.add(new EditorItem(key, values.get(key)));
            }
        }
        /*if (mVictim instanceof AppMessage) {
            mEditorItems.add(new EditorItem(EditorItem.KEY_SEPARATOR, getString(R.string.message_type_app_message)));
            ContentValues appValues = ((AppMessage) mVictim).getAppValues();
            for (String key : appValues.keySet()) {
                if (AppMessage.KEY_SUBTYPE.equals(key)) {
                    mEditorItems.add(new EditorItem(key, AppMessage.ABSTRACT_KEY_SUBTYPE, appValues.get(key), false));
                } else if (Message.KEY_MSG_ID.equals(key)) {
                    mEditorItems.add(new EditorItem(key, key, appValues.get(key), false));
                } else {
                    mEditorItems.add(new EditorItem(key, appValues.get(key)));
                }
            }
        }*/
    }

    @Override
    public void onMessageReset() {
        initItems();
        mAdapter.notifyDataSetChanged();
    }

    public class EditorItem extends BaseObservable {
        private final String key;
        private final String abstract_key;
        private Object value;
        private final boolean editable;
        private static final String KEY_SEPARATOR = "separator";

        EditorItem(@NonNull String key, @Nullable Object value) {
            this.key = key;
            this.value = value;
            this.abstract_key = key;
            this.editable = true;
        }

        EditorItem(@NonNull String key, @NonNull String abstract_key, @Nullable Object value, boolean editable) {
            this.key = key;
            this.abstract_key = abstract_key;
            this.value = value;
            this.editable = editable;
        }

        private boolean isSeparator() {
            return KEY_SEPARATOR.equals(key);
        }

        @Bindable("value")
        @Nullable
        public String getContentPreview() {
            return value == null ? null : value instanceof byte[] ? Arrays.toString((byte[]) value) : value.toString();
        }

        public void setValue(Object newValue) {
            if (Objects.equals(this.value, newValue)) {
                return;
            }
            this.value = newValue;
            mVictim.modify(abstract_key, value);
            notifyMessageChanged();
            notifyPropertyChanged(BR.value);
        }

        private boolean deepEquals(Object a, Object b) {
            if (a instanceof byte[] && b instanceof byte[]) {
                return Utils.byteArrayDeepEquals((byte[]) a, (byte[]) b);
            } else {
                return Objects.deepEquals(a, b);
            }
        }

        public void reset() {
            setValue(mOrigin.get(abstract_key));
        }

        @Bindable("value")
        public boolean isChanged() {
            return isInEditionMode() && !deepEquals(mOrigin.get(abstract_key), value);
        }

        @NonNull
        public String getKey() {
            return key;
        }

        @Nullable
        @Bindable
        public Object getValue() {
            return value;
        }

        @NonNull
        public String getType() {
            return mRepository.requireType(key);
        }

        public boolean isEditable() {
            return editable;
        }

    }

    private class EditorAdapter extends RecyclerView.Adapter<EditorViewHolder> {
        private static final int TYPE_ITEM = 3;
        private static final int TYPE_SEPARATOR = 5;

        @Override
        public int getItemViewType(int position) {
            return mEditorItems.get(position).getKey().equals(EditorItem.KEY_SEPARATOR) ? TYPE_SEPARATOR : TYPE_ITEM;
        }

        @NonNull
        @Override
        public EditorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new EditorViewHolder(DataBindingUtil.inflate(getLayoutInflater(),
                    viewType == TYPE_SEPARATOR ? R.layout.item_separator : R.layout.item_advanced_editor, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull EditorViewHolder holder, int position) {
            if (holder.getItemViewType() == TYPE_ITEM) {
                ItemAdvancedEditorBinding binding = (ItemAdvancedEditorBinding) holder.binding;
                binding.setItem(mEditorItems.get(position));
            } else if (holder.getItemViewType() == TYPE_SEPARATOR) {
                ItemSeparatorBinding binding = (ItemSeparatorBinding) holder.binding;
                binding.setCaption(mEditorItems.get(position).getContentPreview());
            }
        }

        @Override
        public int getItemCount() {
            return mEditorItems.size();
        }
    }

    private class EditorViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private final ViewDataBinding binding;

        public EditorViewHolder(@NonNull ViewDataBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            if (binding instanceof ItemAdvancedEditorBinding) {
                ItemAdvancedEditorBinding advancedEditorBinding = (ItemAdvancedEditorBinding) binding;
                this.itemView.setOnClickListener(this);
                advancedEditorBinding.btnReset.setOnClickListener(this);
            }
        }

        @Override
        public void onClick(@NotNull View v) {
            EditorItem item = mEditorItems.get(getAdapterPosition());
            if (v.getId() == R.id.btn_reset) {
                item.reset();
                return;
            }
            if (Message.KEY_LV_BUFFER.equals(item.key)) {
                new LvBufferEditorDialog().setTypeSerial(Message.LV_BUFFER_READ_SERIAL).setDefault(mVictim.getParsedLvBuffer()).setPassableCallback(item::setValue).show(getParentFragmentManager(), item.key);
                return;
            }
            int contentType = ContentType.getTypeFromRaw(item.getType());
            if (contentType == ContentType.INTEGER || contentType == ContentType.LONG) {
                final EditText[] editor = new EditText[1];
                new SingleLineEditorDialog().setLabel(item.getKey()).setEditorTag(item.getType())
                        .setConfig(et -> {
                            et.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED);
                            editor[0] = et;
                        }).setDefault(item.getContentPreview())
                        .setPredicateCallback(result -> {
                            if (TextUtils.isEmpty(result)) {
                                item.setValue(null);
                            } else {
                                try {
                                    item.setValue(Long.parseLong(result));
                                } catch (NumberFormatException e) {
                                    MasterToast.shortToast(R.string.error_number_format);
                                    UiUtils.swing(editor[0]);
                                    return false;
                                }
                            }
                            return true;
                        }).show(getParentFragmentManager(), item.key);
            } else {
                new ContentEditorDialog().configEditText(passed -> {
                    if (contentType == ContentType.BLOB) {
                        passed.setFilters(new InputFilter[]{new DigitsKeyListener(Utils.getCurrentLocale()) {
                            @Override
                            protected char[] getAcceptedChars() {
                                return new char[]{'-', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '[', ',', ']', ' '};
                            }
                        }});
                    }
                }).setLabel(item.getKey()).setDefault(item.getContentPreview()).setPredicateCallback(result -> {
                    if (contentType == ContentType.BLOB) {
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
                                item.setValue(bytes);
                            } catch (NumberFormatException e) {
                                MasterToast.shortToast(R.string.error_number_format);
                                return false;
                            }
                        } else {
                            MasterToast.shortToast(R.string.error_number_format);
                            return false;
                        }
                    } else {
                        item.setValue(result);
                    }
                    return true;
                }).show(getParentFragmentManager(), item.getKey());
            }
        }
    }
}
