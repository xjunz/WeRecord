/*
 * Copyright (c) 2021 xjunz. 保留所有权利
 */
package xjunz.tool.werecord.ui.message.fragment;

import android.os.Bundle;
import android.text.InputType;
import android.util.ArrayMap;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.databinding.BaseObservable;
import androidx.databinding.Bindable;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;
import androidx.recyclerview.widget.RecyclerView;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import xjunz.tool.werecord.BR;
import xjunz.tool.werecord.R;
import xjunz.tool.werecord.databinding.FragmentSimpleEditorBinding;
import xjunz.tool.werecord.databinding.ItemPreviewBinding;
import xjunz.tool.werecord.databinding.ItemSeparatorBinding;
import xjunz.tool.werecord.databinding.ItemSimpleEditorBinding;
import xjunz.tool.werecord.impl.Environment;
import xjunz.tool.werecord.impl.model.account.Account;
import xjunz.tool.werecord.impl.model.account.Group;
import xjunz.tool.werecord.impl.model.message.AppMessage;
import xjunz.tool.werecord.impl.model.message.CallMessage;
import xjunz.tool.werecord.impl.model.message.ComplexMessage;
import xjunz.tool.werecord.impl.model.message.Message;
import xjunz.tool.werecord.impl.model.message.PlainMessage;
import xjunz.tool.werecord.impl.model.message.SystemMessage;
import xjunz.tool.werecord.impl.model.message.UnpreviewableMessage;
import xjunz.tool.werecord.impl.model.message.util.Edition;
import xjunz.tool.werecord.impl.model.message.util.Template;
import xjunz.tool.werecord.impl.repo.RepositoryFactory;
import xjunz.tool.werecord.impl.repo.WxAppRepository;
import xjunz.tool.werecord.ui.base.ContentEditorDialog;
import xjunz.tool.werecord.ui.base.EditorFragment;
import xjunz.tool.werecord.ui.base.SingleLineEditorDialog;
import xjunz.tool.werecord.ui.customview.MasterToast;
import xjunz.tool.werecord.ui.message.fragment.dialog.SenderChooserDialog;
import xjunz.tool.werecord.ui.message.fragment.dialog.TemplateShowcaseDialog;
import xjunz.tool.werecord.ui.message.fragment.dialog.TimestampEditorDialog;
import xjunz.tool.werecord.util.Returnable;
import xjunz.tool.werecord.util.Utils;

public class SimpleEditorFragment extends EditorFragment {
    private FragmentSimpleEditorBinding mBinding;
    private List<EditorItem> mItems;
    private SimpleEditorAdapter mAdapter;
    private EditorItem mStatusItem, mContentItem;
    private int mRepGroupItemStartIndex;
    private ArrayMap<Template.RepGroup, Object> mRepGroupValues;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initItems();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_simple_editor, container, false);
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mAdapter = new SimpleEditorAdapter();
        mBinding.rvEditor.setAdapter(mAdapter);
        initPreview(mBinding.msgPreviewContainer);
    }

    private boolean isApp() {
        return mVictim instanceof AppMessage;
    }

    private boolean unpreviewable() {
        return mVictim instanceof UnpreviewableMessage;
    }

    private boolean isSystem() {
        return mVictim instanceof SystemMessage;
    }

    private void initItems() {
        mItems = new ArrayList<>();
        //通用项目
        mItems.add(new EditorItem(EditorItem.KEY_SEPARATOR, R.string.universal));
        if (!isInEditionMode()) {
            //模板
            mItems.add(new EditorItem(EditorItem.KEY_TEMPLATE, R.string.template, () -> mTemplate == null ? getString(R.string.bracketed_none) : mTemplate.getName()));
        }
        mItems.add(mContentItem = new EditorItem(Message.ABSTRACT_KEY_CONTENT, R.string.msg_content));
        mItems.add(new EditorItem(Message.KEY_CREATE_TIME, R.string.send_time, () -> Utils.formatDate(mVictim.getCreateTimeStamp())));
        mItems.add(mStatusItem = new EditorItem(Message.KEY_STATUS, R.string.send_status,
                () -> getString(mVictim.supportModifySendStatus() ? mVictim.sendFailed() ? R.string.status_send_failed : R.string.status_send_suc : R.string.modify_send_status_not_supported), () -> mVictim.supportModifySendStatus()) {
            @Override
            public boolean isChanged() {
                return mOrigin.isSend() && mVictim.isSend() && super.isChanged();
            }
        });
        if (!isSystem()) {
            mItems.add(new EditorItem(Message.ABSTRACT_KEY_SENDER_ID, R.string.sender, () -> {
                Account account = mVictim.getSenderAccount();
                return account == null ? mVictim.getSenderId() : account.getIdentifier();
            }).setShowSender(true));
        }
        if (mVictim instanceof CallMessage) {
            mItems.add(new EditorItem(EditorItem.KEY_SEPARATOR, R.string.msg_type_call));
            mItems.add(new EditorItem(CallMessage.ABSTRACT_KEY_CALL_CONTENT, R.string.msg_content));
        }
        //有模板的话
        if (mTemplate != null) {
            List<Template.RepGroup> reps = mTemplate.getRepGroups();
            //如果有替换组的话
            if (reps.size() != 0) {
                mItems.add(new EditorItem(EditorItem.KEY_SEPARATOR, R.string.rep_group).setNote(getString(R.string.note_from_template)));
                mRepGroupItemStartIndex = mItems.size();
                for (Template.RepGroup rep : reps) {
                    mItems.add(new EditorItem(EditorItem.KEY_REP_GROUP, rep.name, () -> {
                        Object value = mRepGroupValues.get(rep);
                        if (value == null) {
                            return null;
                        }
                        switch (rep.getType()) {
                            case Template.RepGroup.TYPE_TIMESTAMP:
                                return Utils.formatDate(Long.parseLong((String) value));
                            case Template.RepGroup.TYPE_ACCOUNT_ID:
                                return ((Account) value).id;
                            case Template.RepGroup.TYPE_ACCOUNT_NAME:
                                return ((Account) value).getName();
                        }
                        return value.toString();
                    }) {
                        @Override
                        public void setValue(Object newValue) {
                            mRepGroupValues.put(rep, newValue);
                            applyRepGroupValues();
                            mContentItem.notifyPropertyChanged(BR.contentPreview);
                            notifyPropertyChanged(BR.contentPreview);
                            refreshPreview();
                        }
                    }.setShowSender(rep.isTypeOf(Template.RepGroup.TYPE_ACCOUNT_ID) || rep.isTypeOf(Template.RepGroup.TYPE_ACCOUNT_NAME)));
                }
            }
        }
        mItems.add(new EditorItem(EditorItem.KEY_SEPARATOR, R.string.preview).setHideDivider().setNote(isApp() || unpreviewable() ? getString(R.string.note_imperfect_preview) : null));
        mItems.add(new EditorItem(EditorItem.KEY_PREVIEW, R.string.preview));
    }

    @Override
    public void onMessageReset() {
        mRepGroupValues = new ArrayMap<>();
        if (mTemplate != null) {
            applyRepGroupValues();
        }
        initItems();
        mAdapter.notifyDataSetChanged();
    }

    private void applyRepGroupValues() {
        String content = mTemplate.getContent();
        for (Template.RepGroup repGroup : mTemplate.getRepGroups()) {
            Object value = mRepGroupValues.get(repGroup);
            String replacement;
            if (value == null) {
                replacement = "";
            } else {
                switch (repGroup.getType()) {
                    case Template.RepGroup.TYPE_ACCOUNT_ID:
                        replacement = ((Account) value).id;
                        break;
                    case Template.RepGroup.TYPE_ACCOUNT_NAME:
                        replacement = ((Account) value).getName();
                        break;
                    default:
                        replacement = value.toString();
                        break;
                }
            }
            for (String p : repGroup.getReplacementPattern()) {
                content = content.replace(p, replacement);
            }
        }
        mVictim.modifyContent(content);
    }

    public class EditorItem extends BaseObservable {
        public final String caption;
        private final String key;
        public static final String KEY_SEPARATOR = "separator";
        public static final String KEY_TEMPLATE = "template";
        public static final String KEY_PREVIEW = "preview";
        public static final String KEY_REP_GROUP = "repGroup";
        private Returnable<String> previewable;
        private Returnable<Boolean> editable;

        private EditorItem(@NonNull String key, @StringRes int captionRes) {
            this.key = key;
            this.caption = getString(captionRes);
            if (!key.equals(KEY_SEPARATOR)) {
                this.previewable = () -> {
                    Object value = getValue();
                    return value == null ? null : value.toString();
                };
            }
        }

        private EditorItem(@NonNull String key, @StringRes int captionRes,
                           @Nullable Returnable<String> previewable, @NonNull Returnable<Boolean> editable) {
            this(key, getString(captionRes), previewable, editable);
        }

        private EditorItem(@NonNull String key, String caption, @Nullable Returnable<String> previewable, @NonNull Returnable<Boolean> editable) {
            this.key = key;
            this.caption = caption;
            if (!key.equals(KEY_SEPARATOR)) {
                this.previewable = previewable;
                this.editable = editable;
            }
        }

        private boolean isSeparator() {
            return KEY_SEPARATOR.equals(key);
        }

        private boolean isPreview() {
            return KEY_PREVIEW.equals(key);
        }

        private EditorItem(@NonNull String key, @StringRes int captionRes,
                           @Nullable Returnable<String> previewable) {
            this(key, captionRes, previewable, () -> true);
        }

        private EditorItem(@NonNull String key, String caption, @Nullable Returnable<String> previewable) {
            this(key, caption, previewable, () -> true);
        }

        private Object getValue() {
            return mVictim.get(key);
        }

        public void setValue(Object newValue) {
            mVictim.modify(key, newValue);
            notifyMessageChanged();
            notifyPropertyChanged(BR.contentPreview);
            refreshPreview();
        }

        public void reset() {
            setValue(mOrigin.get(key));
        }

        @Bindable
        public String getContentPreview() {
            return previewable.get();
        }

        private boolean showSender;

        EditorItem setShowSender(boolean show) {
            showSender = show;
            return this;
        }

        private boolean hideDivider;

        EditorItem setHideDivider() {
            this.hideDivider = true;
            return this;
        }

        private String note;

        EditorItem setNote(String note) {
            this.note = note;
            return this;
        }

        @Bindable("contentPreview")
        public Account getSender() {
            return showSender ? mVictim.getSenderAccount() : null;
        }

        @Bindable("contentPreview")
        public boolean isChanged() {
            return isInEditionMode() && !Objects.equals(mOrigin.get(key), mVictim.get(key));
        }

        @Bindable("contentPreview")
        public boolean isEditable() {
            return editable == null ? true : editable.get();
        }
    }


    private class SimpleEditorAdapter extends RecyclerView.Adapter<SimpleEditorViewHolder> {
        static final int TYPE_ITEM = 3;
        static final int TYPE_SEPARATOR = 5;
        static final int TYPE_PREVIEW = 7;

        @Override
        public int getItemViewType(int position) {
            EditorItem item = mItems.get(position);
            return item.isSeparator() ? TYPE_SEPARATOR : item.isPreview() ? TYPE_PREVIEW : TYPE_ITEM;
        }

        @NonNull
        @Override
        public SimpleEditorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new SimpleEditorViewHolder(DataBindingUtil.inflate(getLayoutInflater()
                    , viewType == TYPE_ITEM ? R.layout.item_simple_editor : viewType == TYPE_PREVIEW ? R.layout.item_preview : R.layout.item_separator, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull SimpleEditorViewHolder holder, int position) {
            EditorItem item = mItems.get(position);
            int type = holder.getItemViewType();
            if (type == TYPE_ITEM) {
                ItemSimpleEditorBinding binding = (ItemSimpleEditorBinding) holder.binding;
                binding.setItem(item);
            } else if (type == TYPE_SEPARATOR) {
                ItemSeparatorBinding binding = (ItemSeparatorBinding) holder.binding;
                binding.setCaption(item.caption);
                binding.setHideDivider(item.hideDivider);
                binding.setNote(item.note);
            } else {
                ItemPreviewBinding binding = (ItemPreviewBinding) holder.binding;
                initPreview(binding.previewContainer);
            }
            holder.binding.executePendingBindings();
        }

        @Override
        public int getItemCount() {
            return mItems.size();
        }
    }


    private class SimpleEditorViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private final ViewDataBinding binding;

        SimpleEditorViewHolder(@NonNull ViewDataBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            if (binding instanceof ItemSimpleEditorBinding) {
                ItemSimpleEditorBinding itemSimpleEditorBinding = (ItemSimpleEditorBinding) binding;
                itemSimpleEditorBinding.btnReset.setOnClickListener(this);
                itemSimpleEditorBinding.getRoot().setOnClickListener(this);
            }
        }


        @Override
        public void onClick(@NotNull View v) {
            EditorItem item = mItems.get(getAdapterPosition());
            if (v.getId() == R.id.btn_reset) {
                if (item.key.equals(Message.ABSTRACT_KEY_SENDER_ID)) {
                    changeSenderConsideringStatus(item, mOrigin.getSenderId());
                } else {
                    item.reset();
                }
                return;
            }
            switch (item.key) {
                case Message.ABSTRACT_KEY_CONTENT:
                case CallMessage.ABSTRACT_KEY_CALL_CONTENT:
                    new ContentEditorDialog().setLabel(item.caption)
                            .setDefault(mVictim.get(item.key))
                            .setPassableCallback(item::setValue).show(getParentFragmentManager(), item.key);
                    break;
                case Message.KEY_STATUS:
                    item.setValue(mVictim.sendFailed() ? Message.STATUS_SEND_SUC : Message.STATUS_SEND_FAILED);
                    break;
                case Message.ABSTRACT_KEY_SENDER_ID:
                    String[] senders = getOptionalSenderIds();
                    if (senders == null || senders.length < 2) {
                        return;
                    }
                    if (senders.length == 2) {
                        changeSenderConsideringStatus(item, item.getValue().equals(senders[0]) ? senders[1] : senders[0]);
                        return;
                    }
                    new SenderChooserDialog().setSenderIds(getOptionalSenderIds())
                            .setDefault(mVictim.getSenderAccount())
                            .setPassableCallback(result -> {
                                changeSenderConsideringStatus(item, result.id);
                            }).show(getParentFragmentManager(), item.key);
                    break;
                case Message.KEY_CREATE_TIME:
                    new TimestampEditorDialog().setHelpTextRes(R.string.help_send_timestamp).setLabelRes(R.string.send_time).setDefault(mVictim.getCreateTimeStamp()).setPassableCallback(item::setValue).show(getParentFragmentManager(), item.key);
                    break;
                case EditorItem.KEY_TEMPLATE:
                    new TemplateShowcaseDialog().setCallback(SimpleEditorFragment.this::setTemplate).show(getParentFragmentManager(), "template_showcase");
                    break;
                case EditorItem.KEY_REP_GROUP:
                    Template.RepGroup repGroup = mTemplate.getRepGroups().get(getAdapterPosition() - mRepGroupItemStartIndex);
                    String type = repGroup.getType();
                    Object value = mRepGroupValues.get(repGroup);
                    switch (type) {
                        case Template.RepGroup.TYPE_STRING:
                            new ContentEditorDialog().setLabel(repGroup.name).setDefault(Utils.stringValueOf(value))
                                    .setPassableCallback(item::setValue).show(getParentFragmentManager(), item.caption);
                            break;
                        case Template.RepGroup.TYPE_DECIMAL:
                            new SingleLineEditorDialog().setLabel(item.caption).setEditorTag(type).setConfig(et -> {
                                et.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                            }).setDefault(Utils.stringValueOf(value)).setPredicateCallback(result -> {
                                try {
                                    item.setValue(Double.parseDouble(result));
                                } catch (NumberFormatException e) {
                                    MasterToast.shortToast(R.string.error_number_format);
                                    return false;
                                }
                                return true;
                            }).show(getParentFragmentManager(), item.caption);
                        case Template.RepGroup.TYPE_LONG:
                            new SingleLineEditorDialog().setLabel(item.caption).setEditorTag(type).setConfig(et -> {
                                et.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED);
                            }).setDefault(Utils.stringValueOf(value)).setPredicateCallback(result -> {
                                try {
                                    item.setValue(Long.parseLong(result));
                                } catch (NumberFormatException e) {
                                    MasterToast.shortToast(R.string.error_number_format);
                                    return false;
                                }
                                return true;
                            }).show(getParentFragmentManager(), item.caption);
                            break;
                        case Template.RepGroup.TYPE_TIMESTAMP:
                            new TimestampEditorDialog().setLabel(item.caption).setDefault(mVictim.getCreateTimeStamp()).setAllowUnchanged(true)
                                    .setPassableCallback(item::setValue).show(getParentFragmentManager(), item.caption);
                            break;
                        case Template.RepGroup.TYPE_ACCOUNT_ID:
                        case Template.RepGroup.TYPE_ACCOUNT_NAME:
                            new SenderChooserDialog().setSenderIds(getOptionalSenderIds()).setDefault((Account) value)
                                    .setPassableCallback(item::setValue).show(getParentFragmentManager(), item.caption);
                            break;
                        case Template.RepGroup.TYPE_APP_ID:
                            PopupMenu menu = new PopupMenu(requireContext(), itemView);
                            WxAppRepository repository = RepositoryFactory.get(WxAppRepository.class);
                            ArrayMap<String, String> all = repository.getAll();
                            if (all.isEmpty()) {
                                MasterToast.shortToast(R.string.none);
                                return;
                            }
                            for (int i = 0; i < all.size(); i++) {
                                menu.getMenu().add(0, i, 0, all.valueAt(i));
                            }
                            menu.setOnMenuItemClickListener(menuItem -> {
                                item.setValue(all.keyAt(menuItem.getItemId()));
                                return true;
                            });
                            menu.setGravity(Gravity.END);
                            menu.show();
                            break;
                    }
                    break;
            }
        }

        /**
         * 修改发送者(考虑发送状态)
         */
        private void changeSenderConsideringStatus(@NotNull EditorItem item, String newSenderId) {
            item.setValue(newSenderId);
            //变成发送消息
            if (mVictim.isSend()) {
                //且原消息也是发送消息
                if (mOrigin.isSend()) {
                    //重置
                    mStatusItem.reset();
                }
                //但原消息是接收消息
                else {
                    //设为发送成功
                    mStatusItem.setValue(Message.STATUS_SEND_SUC);
                }
            }
            //变成接收消息
            else {
                //且原消息是发送
                if (mOrigin.isSend()) {
                    //fixme:可能有问题,原:status.setValue(Message.STATUS_SEND_SUC);
                    //设为接收成功
                    mStatusItem.setValue(Message.STATUS_RECEIVE_SUC);
                }
                //原消息也是接收
                else {
                    //重置为原来的状态
                    mStatusItem.reset();
                }
            }
        }
    }

    @NotNull
    private Account getCurrentUser() {
        return Environment.getInstance().getCurrentUser();
    }

    @Nullable
    public String[] getOptionalSenderIds() {
        if (mVictim.isInGroupChat()) {
            Group group = mVictim.getGroupTalker();
            return group != null ? group.getMemberIdList() : null;
        } else {
            //搁着和自己聊天呢
            if (getCurrentUser().id.equals(mVictim.getTalkerId())) {
                return null;
            }
            return new String[]{getCurrentUser().id, mVictim.getTalkerId()};
        }
    }

    private void refreshPreview() {
        mAdapter.notifyItemChanged(mItems.size() - 1);
    }

    private void initPreview(@NotNull ViewGroup previewContainer) {
        previewContainer.removeAllViews();
        Message noEditionClone = mVictim.deepClone();
        //去除编辑标志，因为预览的消息不需要显示编辑标签
        noEditionClone.setEditionFlag(Edition.FLAG_NONE);
        boolean left = !noEditionClone.isSend();
        int layoutRes = 0;
        if (noEditionClone instanceof ComplexMessage) {
            layoutRes = left ? R.layout.item_bubble_complex_left : R.layout.item_bubble_complex_right;
        } else if (noEditionClone instanceof PlainMessage) {
            layoutRes = left ? R.layout.item_bubble_plain_left : R.layout.item_bubble_plain_right;
        } else if (noEditionClone instanceof SystemMessage) {
            layoutRes = R.layout.item_bubble_system;
        }
        ViewDataBinding binding = DataBindingUtil.inflate(getLayoutInflater(), layoutRes, previewContainer, true);
        binding.setVariable(BR.msg, noEditionClone);
        if (!(noEditionClone instanceof SystemMessage)) {
            binding.setVariable(BR.account, noEditionClone.getSenderAccount());
        }
        binding.executePendingBindings();
    }
}
