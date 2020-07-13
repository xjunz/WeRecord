/*
 * Copyright (c) 2020 xjunz. 保留所有权利
 */

package xjunz.tool.wechat.ui.main;

import android.os.Bundle;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import xjunz.tool.wechat.BR;
import xjunz.tool.wechat.R;
import xjunz.tool.wechat.data.viewmodel.MessageViewModel;
import xjunz.tool.wechat.databinding.ActivityMessageBinding;
import xjunz.tool.wechat.impl.Environment;
import xjunz.tool.wechat.impl.model.account.Talker;
import xjunz.tool.wechat.impl.model.message.Message;
import xjunz.tool.wechat.impl.repo.ContactRepository;
import xjunz.tool.wechat.impl.repo.MessageRepository;
import xjunz.tool.wechat.ui.BaseActivity;
import xjunz.tool.wechat.ui.customview.MasterToast;
import xjunz.tool.wechat.util.RxJavaUtils;
import xjunz.tool.wechat.util.UiUtils;

public class MessageActivity extends BaseActivity {
    public static final String EXTRA_DATA = "MessageActivity.extra.data";
    private static final int INITIAL_LOAD_COUNT = 50;
    private static final int CONSEQUENT_LOAD_COUNT = 100;
    private Talker mTalker;
    private RecyclerView mRvMessage;
    private List<Message> mMessageList;
    private MessageRepository mMessageRepo;
    private ContactRepository mContactRepo;
    private MessageAdapter mAdapter;
    private ActivityMessageBinding mBinding;
    private MessageViewModel mModel;
    private int mItemShowingContextMenu = -1;
    private boolean mHasLoadAllMessages;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mTalker = (Talker) getIntent().getSerializableExtra(EXTRA_DATA);
        if (mTalker == null) {
            MasterToast.shortToast("No data passed in, who are u?");
            finish();
        } else {
            mMessageRepo = MessageRepository.getInstance();
            mContactRepo = ContactRepository.getInstance();
            mBinding = DataBindingUtil.setContentView(this, R.layout.activity_message);
            mModel = new ViewModelProvider(this, new ViewModelProvider.NewInstanceFactory()).get(MessageViewModel.class);
            mBinding.setTalker(mTalker);
            mBinding.setModel(mModel);
            mRvMessage = mBinding.rvMessage;
            initList();
        }
    }


    private void initList() {
        mMessageList = new ArrayList<>();
        RxJavaUtils.single(() -> mMessageRepo.queryMessageByTalkerLimit(mTalker.id, INITIAL_LOAD_COUNT, mMessageList)).subscribe(new RxJavaUtils.SingleObserverAdapter<Integer>() {
            @Override
            public void onSuccess(Integer o) {
                mHasLoadAllMessages = mMessageList.size() >= mTalker.messageCount;
                mAdapter = new MessageAdapter();
                mRvMessage.setAdapter(mAdapter);
                //回到最底部，显示最新的消息记录（因为是reverseLayout的）
                mRvMessage.scrollToPosition(0);
            }
        });
        mBinding.rvMessage.addOnScrollListener(new RecyclerView.OnScrollListener() {
            int formerState = RecyclerView.SCROLL_STATE_IDLE;

            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                formerState = newState;
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                //当用户滑动到顶部的时候
                if (formerState != RecyclerView.SCROLL_STATE_IDLE && !mBinding.rvMessage.canScrollVertically(-1)) {
                    //如果已经数据已经全部加载完
                    if (mHasLoadAllMessages) {
                        //提示
                        UiUtils.toast(R.string.has_loaded_all_msg);
                        return;
                    }
                    //否则显示加载进度条
                    UiUtils.fadeIn(mBinding.pbLoad);
                    RxJavaUtils.single(() -> {
                        //为了加载动画不一闪而过造成不好的用户体验，
                        //假装加载500毫秒，让动画跑一会儿，其实秒加载。
                        Thread.sleep(500);
                        return mMessageRepo.queryMessageByTalkerLimit(mTalker.id, INITIAL_LOAD_COUNT, mMessageList);
                    }).subscribe(new RxJavaUtils.SingleObserverAdapter<Integer>() {
                        @Override
                        public void onSuccess(Integer count) {
                            super.onSuccess(count);
                            //关闭加载进度条并更新数据
                            UiUtils.fadeOut(mBinding.pbLoad);
                            mHasLoadAllMessages = mMessageList.size() >= mTalker.messageCount;
                            mAdapter.notifyItemInserted(mMessageList.size() - count);
                            mBinding.rvMessage.smoothScrollBy(0, -100, new AccelerateDecelerateInterpolator());
                        }
                    });
                }
            }
        });
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        getMenuInflater().inflate(R.menu.message, menu);
    }

    public void onMessageClicked(@NonNull Message message) {
        UiUtils.toast(message.getRawContent());
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        if (mItemShowingContextMenu != -1) {
            switch (item.getItemId()) {
                case R.id.item_add_before:
                    break;
                case R.id.item_add_after:
                    break;
                case R.id.item_delete:
                    break;
                case R.id.item_edit:
                    break;
                case R.id.item_check:
                    break;
            }
        }
        return super.onContextItemSelected(item);
    }


    private class MessageAdapter extends RecyclerView.Adapter<MessageViewHolder> {
        private final int FLAG_IS_SEND = 13;
        private final int ITEM_TYPE_PLAIN = 1;
        private final int ITEM_TYPE_SYSTEM = 2;
        private final int ITEM_TYPE_COMPLEX = 3;

        private boolean isSend(int compositeType) {
            return compositeType % FLAG_IS_SEND == 0;
        }

        private int getType(int compositeType) {
            return compositeType / (isSend(compositeType) ? FLAG_IS_SEND : 1);
        }

        private int getResOf(int compositeType) {
            boolean left = !isSend(compositeType);
            int type = getType(compositeType);
            switch (type) {
                case ITEM_TYPE_SYSTEM:
                    return R.layout.item_bubble_system;
                case ITEM_TYPE_COMPLEX:
                    return left ? R.layout.item_bubble_complex_left : R.layout.item_bubble_complex_right;
                case ITEM_TYPE_PLAIN:
                    return left ? R.layout.item_bubble_plain_left : R.layout.item_bubble_plain_right;

            }
            throw new IllegalArgumentException("No such type: " + compositeType);
        }

        @Override
        public int getItemViewType(int position) {
            Message message = mMessageList.get(position);
            Message.Type type = message.getType();
            if (message.getType() == Message.Type.SYSTEM) {
                return ITEM_TYPE_SYSTEM;
            } else {
                if (type == null || type.isComplex()) {
                    return (message.isSend() ? FLAG_IS_SEND : 1) * ITEM_TYPE_COMPLEX;
                } else {
                    return (message.isSend() ? FLAG_IS_SEND : 1) * ITEM_TYPE_PLAIN;
                }
            }
        }

        @NonNull
        @Override
        public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ViewDataBinding binding = DataBindingUtil.inflate(getLayoutInflater(), getResOf(viewType), parent, false);
            return new MessageViewHolder(binding);
        }

        @Override
        public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
            Message message = mMessageList.get(position);
            holder.binding.setVariable(BR.message, message);
            if (message.getType() != Message.Type.SYSTEM) {
                if (message.isSend()) {
                    holder.binding.setVariable(BR.account, Environment.getInstance().getCurrentUser());
                } else {
                    holder.binding.setVariable(BR.account, message.getGroupTalkerId() == null ? mTalker : ContactRepository.getInstance().get(message.requireSenderId()));
                }
            }
            holder.binding.executePendingBindings();
        }

        @Override
        public int getItemCount() {
            return mMessageList.size();
        }
    }

    private class MessageViewHolder extends RecyclerView.ViewHolder {
        ViewDataBinding binding;

        public MessageViewHolder(@NonNull ViewDataBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            this.binding.setVariable(BR.activity, MessageActivity.this);
        }
    }

    @Override
    public void onBackPressed() {
        if (mBinding.messagePanel.isOpen()) {
            mBinding.messagePanel.closePanel();
        } else {
            super.onBackPressed();
        }
    }

}
