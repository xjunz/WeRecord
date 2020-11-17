/*
 * Copyright (c) 2020 xjunz. 保留所有权利
 */

package xjunz.tool.wechat.ui.message;

import android.annotation.SuppressLint;
import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Bundle;
import android.transition.Transition;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import xjunz.tool.wechat.BR;
import xjunz.tool.wechat.R;
import xjunz.tool.wechat.data.viewmodel.EditorViewModel;
import xjunz.tool.wechat.data.viewmodel.MessageEditorViewModel;
import xjunz.tool.wechat.data.viewmodel.MessageViewModel;
import xjunz.tool.wechat.databinding.ActivityMessageBinding;
import xjunz.tool.wechat.impl.DatabaseModifier;
import xjunz.tool.wechat.impl.Environment;
import xjunz.tool.wechat.impl.model.account.Account;
import xjunz.tool.wechat.impl.model.account.Talker;
import xjunz.tool.wechat.impl.model.message.BackupMessage;
import xjunz.tool.wechat.impl.model.message.Edition;
import xjunz.tool.wechat.impl.model.message.Message;
import xjunz.tool.wechat.impl.model.message.MessageFactory;
import xjunz.tool.wechat.impl.repo.ContactRepository;
import xjunz.tool.wechat.impl.repo.MessageRepository;
import xjunz.tool.wechat.impl.repo.RepositoryFactory;
import xjunz.tool.wechat.ui.BaseActivity;
import xjunz.tool.wechat.ui.customview.MessagePanel;
import xjunz.tool.wechat.ui.main.DetailActivity;
import xjunz.tool.wechat.ui.message.fragment.EditionFragment;
import xjunz.tool.wechat.ui.message.fragment.SearchFragment;
import xjunz.tool.wechat.ui.message.fragment.StatisticsFragment;
import xjunz.tool.wechat.util.RxJavaUtils;
import xjunz.tool.wechat.util.UiUtils;

public class MessageActivity extends BaseActivity {
    public static final String EXTRA_TALKER = "MessageActivity.extra.talker";
    /**
     * 初始加载的消息数，不宜也没必要过大，否则可能造成卡顿和内存的浪费
     */
    private static final int INITIAL_LOAD_COUNT = 50;
    /**
     * 加载更多消息记录的消息数，没必要过多，浪费内存资源且用户一般不会查看这么多记录
     */
    private static final int CONSEQUENT_LOAD_COUNT = 100;
    /**
     * 加载更多消息时假装加载的时间
     * <p>因为读取消息记录实际上耗时很，为了防止加载进度条一闪而过的情况，设置
     * 假装加载时长</p>
     */
    private static final long FAKE_LOAD_MILLS = 500L;
    private Talker mTalker;
    private RecyclerView mRvMessage;
    /**
     * 当前显示的消息数据列表，仅持有{@link MessageViewModel#currentLoadedMessages}的引用，
     * 切勿赋值给此变量
     */
    private List<Message> mMessageList;
    private MessageRepository mMessageRepo;
    private ContactRepository mContactRepo;
    private MessageAdapter mAdapter;
    private ActivityMessageBinding mBinding;
    private MessageViewModel mModel;
    private Fragment[] mPages;
    /**
     * 当前需要被导航的消息，此消息来源于{@link SearchFragment}的搜索结果被点击时
     * 发送的{@link MessageViewModel#notifyNavigate(Message)}事件。接收到此事件后，
     * 当前消息列表会滑动到此消息的位置，予以导航。
     */
    private Message mMessageToNavigate;
    /**
     * 需要闪烁提醒的消息索引
     *
     * @see MessageAdapter#onBindViewHolder(MessageViewHolder, int, List)
     */
    private int mMessageIndexToBlink = -1;
    private DatabaseModifier mModifier;
    private MessageEditorViewModel mMessageEditorViewModel;
    private final MessageViewModel.EventHandler mHandler = new MessageViewModel.EventHandler() {
        @Override
        public void onNavigate(Message msg) {
            super.onNavigate(msg);
            mBinding.messagePanel.closePanel();
            mMessageToNavigate = msg;
        }

        @Override
        public void onAllLoaded(int preCount) {
            mAdapter.notifyItemInserted(preCount);
            if (mBinding.messagePanel.isOpen()) {
                mBinding.etSearch.requestFocus();
            }
        }
    };

    private final MessageEditorViewModel.EditorEventHandler mEditorEventHandler = new MessageEditorViewModel.EditorEventHandler() {
        @Override
        public void onMessageChanged(boolean timestampChanged, Message changed) {
            mMessageList.remove(mModel.selectedMessagePosition);
            if (timestampChanged) {
                mAdapter.notifyItemRemoved(mModel.selectedMessagePosition);
                int index = Collections.binarySearch(mMessageList, changed, (o1, o2) -> -Long.compare(o1.getCreateTimeStamp(), o2.getCreateTimeStamp()));
                if (index >= 0) {
                    mMessageList.add(index + 1, changed);
                    mAdapter.notifyItemInserted(index + 1);
                } else {
                    index = -(index + 1);
                    mMessageList.add(index, changed);
                    mAdapter.notifyItemInserted(index);
                }
            } else {
                mMessageList.add(mModel.selectedMessagePosition, changed);
                mAdapter.notifyItemChanged(mModel.selectedMessagePosition);
            }
        }

        @Override
        public void onMessageInserted(boolean addBefore, Message inserted) {
            int insertion = addBefore ? mModel.selectedMessagePosition - 1 : mModel.selectedMessagePosition + 1;
            mMessageList.add(insertion, inserted);
            mAdapter.notifyItemInserted(insertion);
        }

        @Override
        public void onMessageDeleted() {
            mMessageList.remove(mModel.selectedMessagePosition);
            mAdapter.notifyItemRemoved(mModel.selectedMessagePosition);
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mTalker = (Talker) getIntent().getSerializableExtra(EXTRA_TALKER);
        if (mTalker == null) {
            // MasterToast.shortToast("No data passed in, who are u?");
            finish();
            return;
        }
        mMessageRepo = RepositoryFactory.get(MessageRepository.class);
        mContactRepo = RepositoryFactory.get(ContactRepository.class);
        mModifier = Environment.getInstance().modifyDatabase();
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_message);
        mRvMessage = mBinding.rvMessage;
        mModel = new ViewModelProvider(this, new ViewModelProvider.NewInstanceFactory()).get(MessageViewModel.class);
        mMessageEditorViewModel = MessageEditorViewModel.get(getApplication());
        mMessageEditorViewModel.registerEventHandler(mEditorEventHandler);
        mModel.currentTalker = mTalker;
        mModel.addEventHandler(mHandler);
        mMessageList = mModel.currentLoadedMessages;
        mBinding.setModel(mModel);
        initList();
        initPages();
        initPanel();
    }

    private void doNavigate(Message msg) {
        //查找需要导航的消息
        int index = mMessageList.indexOf(msg);
        //如果找到
        if (index >= 0) {
            //获取当前列表上下边缘的消息索引
            int f = UiUtils.getFirstVisibleItemIndexOfList(mRvMessage, false);
            int l = UiUtils.getLastVisibleItemIndexOfList(mRvMessage, false);
            if (f >= 0 && l >= 0) {
                //如果存在
                int span = l - f;
                if (f <= index && l >= index) {
                    //且欲导航的位置在这俩之间
                    //直接滑到该位置并闪烁提示
                    mRvMessage.smoothScrollToPosition(index);
                    mAdapter.notifyItemChanged(index, MessageAdapter.PAYLOAD_NAVIGATE);
                    return;
                } else if (Math.abs(f - index) <= span * 2 || Math.abs(l - index) <= span * 2) {
                    //如果欲导航的位置离上下边缘不超过两个屏幕
                    mMessageIndexToBlink = index;
                    //平滑地滑动导航到指定位置
                    mRvMessage.smoothScrollToPosition(index);
                    return;
                }
            }
            //否则
            //直接跳到指定位置（不执行滑动动作，太墨迹）
            mRvMessage.scrollToPosition(index);
            //在下一帧闪烁提示
            mRvMessage.post(() -> mAdapter.notifyItemChanged(index, MessageAdapter.PAYLOAD_NAVIGATE));
        } else {
            UiUtils.toast("找不到该消息~");
        }
    }

    private void initPages() {
        mPages = new Fragment[3];
        mPages[0] = new StatisticsFragment();
        mPages[1] = new EditionFragment();
        mPages[2] = new SearchFragment();
        mBinding.vpMessage.setAdapter(new MessageFragmentAdapter(this));
    }

    private void initPanel() {
        mBinding.messagePanel.addOnPanelSlideListener(new MessagePanel.OnPanelSlideListener() {
            @Override
            public void onPanelSlideFinished(boolean isOpen) {
                if (!isOpen && mMessageToNavigate != null) {
                    doNavigate(mMessageToNavigate);
                    mMessageToNavigate = null;
                } else if (isOpen) {
                    if (mModel.hasLoadAll.get()) {
                        mBinding.etSearch.requestFocus();
                    }
                }
            }

            @Override
            public void onPanelSlide(float fraction) {
                //no-op
            }

            @Override
            public void onPanelSlideStart(boolean isToOpen) {
                if (!isToOpen) {
                    hideIme(mBinding.etSearch);
                }
            }
        });
    }

    private final Comparator<Message> mBackupComparator = (o1, o2) -> {
        int comparison = Long.compare(o1.getCreateTimeStamp(), o2.getCreateTimeStamp());
        return comparison == 0 ? Integer.compare(o1.getMsgId(), o2.getMsgId()) : comparison;
    };

    private void loadData() {
        RxJavaUtils.complete(() -> {
            mModel.actualMessageCount = mMessageRepo.getActualMessageCountOf(mTalker.id);
            mMessageRepo.queryMessageByTalkerLimit(mTalker.id, INITIAL_LOAD_COUNT, mMessageList);
            mModel.hasLoadAll.set(mMessageList.size() >= mModel.actualMessageCount);
            if (mModifier.backupTableExists()) {
                mMessageRepo.queryBackupMessagesByTalker(mTalker.id, mModel.allBackupMessages);
                int index;
                for (BackupMessage backup : mModel.allBackupMessages) {
                    if ((index = mMessageList.indexOf(backup)) >= 0) {
                        mMessageList.get(index).setEditionFlag(backup.getEditionFlag());
                    } else if (backup.getEditionFlag() == Edition.FLAG_REMOVAL) {
                        int i = Collections.binarySearch(mMessageList, backup, mBackupComparator);
                        if (i >= 0) {
                            throw new IllegalArgumentException("Found a deleted message(" + backup.getMsgId() + ") in message list.");
                        } else {
                            int insertion = -(i + 1);
                            if (insertion > 0 || (insertion == 0 && mModel.hasLoadAll.get())) {
                                mMessageList.add(insertion, backup);
                            }
                        }
                    }
                }
            }
        }).subscribe(new RxJavaUtils.CompletableObservableAdapter() {
            @Override
            public void onComplete() {
                mAdapter = new MessageAdapter();
                mRvMessage.setAdapter(mAdapter);
                //回到最底部，显示最新的消息记录（因为是reverseLayout的）
                mRvMessage.scrollToPosition(0);
            }
        });
    }

    private AvatarScrollHandler mScrollListener;

    private void initList() {
       /* RxJavaUtils.complete(() -> {
            mModel.actualMessageCount = mMessageRepo.getActualMessageCountOf(mTalker.id);
            mMessageRepo.queryMessageByTalkerLimit(mTalker.id, INITIAL_LOAD_COUNT, mMessageList);
        }).subscribe(new RxJavaUtils.CompletableObservableAdapter() {
            @Override
            public void onComplete() {
                mModel.hasLoadAll.set(mMessageList.size() >= mModel.actualMessageCount);
                mAdapter = new MessageAdapter();
                mRvMessage.setAdapter(mAdapter);
                //回到最底部，显示最新的消息记录（因为是reverseLayout的）
                mRvMessage.scrollToPosition(0);
            }
        });*/
        loadData();
        //处理消息的闪烁提示，消息的加载
        mBinding.rvMessage.addOnScrollListener(new RecyclerView.OnScrollListener() {
            int formerState = RecyclerView.SCROLL_STATE_IDLE;

            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    if (mMessageIndexToBlink >= 0) {
                        mAdapter.notifyItemChanged(mMessageIndexToBlink, MessageAdapter.PAYLOAD_NAVIGATE);
                        mMessageIndexToBlink = -1;
                    }
                }
                formerState = newState;
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                //当用户滑动到顶部的时候
                if (formerState != RecyclerView.SCROLL_STATE_IDLE && !mBinding.rvMessage.canScrollVertically(-1)) {
                    //如果已经数据已经全部加载完
                    if (mModel.hasLoadAll.get()) {
                        //直接返回
                        return;
                    }
                    //否则显示加载进度条
                    UiUtils.fadeIn(mBinding.pbLoad);
                    //禁止下拉关闭
                    mBinding.rvMessage.setNestedScrollingEnabled(false);
                    RxJavaUtils.single(() -> {
                        //为了防止加载动画一闪而过造成不好的用户体验，
                        //假装加载500毫秒，让动画跑一会儿，其实毫秒级加载。
                        Thread.sleep(FAKE_LOAD_MILLS);
                        return mMessageRepo.queryMessageByTalkerLimit(mTalker.id, CONSEQUENT_LOAD_COUNT, mMessageList);
                    }).subscribe(new RxJavaUtils.SingleObserverAdapter<Integer>() {
                        @Override
                        public void onSuccess(@NotNull Integer count) {
                            super.onSuccess(count);
                            //关闭加载进度条并更新数据
                            UiUtils.fadeOut(mBinding.pbLoad);
                            mModel.hasLoadAll.set(mMessageList.size() >= mModel.actualMessageCount);
                            mAdapter.notifyItemInserted(mMessageList.size() - count);
                            mBinding.rvMessage.smoothScrollBy(0, -100, new AccelerateDecelerateInterpolator());
                            mBinding.rvMessage.setNestedScrollingEnabled(true);
                        }
                    });
                }
            }
        });
        getWindow().getSharedElementEnterTransition().addListener(new UiUtils.TransitionListenerAdapter() {
            @Override
            public void onTransitionEnd(@NonNull Transition transition) {
                mScrollListener = new AvatarScrollHandler((LinearLayoutManager) Objects.requireNonNull(mRvMessage.getLayoutManager())) {
                    @Override
                    public void notifyAvatarInvisible(int position) {
                        mAdapter.mInvisibleAvatarIndex = position;
                    }

                    @Override
                    public void notifyAvatarVisible() {
                        mAdapter.mInvisibleAvatarIndex = -1;
                    }
                };
                mRvMessage.addOnScrollListener(mScrollListener);
            }
        });
        getWindow().getSharedElementReturnTransition().addListener(new UiUtils.TransitionListenerAdapter() {

            @Override
            public void onTransitionStart(@NonNull Transition transition) {
                mRvMessage.removeOnScrollListener(mScrollListener);
            }
        });
    }


    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        getMenuInflater().inflate(R.menu.message, menu);
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        mModifier = getEnvironment().modifyDatabase();
        mBinding.setModifier(mModifier);
        Message selectedMessage = mModel.getSelectedMessage();
        mMessageEditorViewModel.passMessageToEdit(selectedMessage);
        switch (item.getItemId()) {
            case R.id.item_add_before:
                Intent intentAddBefore = new Intent(this, EditorActivity.class);
                intentAddBefore.putExtra(EditorActivity.EXTRA_EDIT_MODE, EditorViewModel.EDIT_MODE_ADD_BEFORE);
                if (mModel.selectedMessagePosition < mMessageList.size() - 1) {
                    Message before = mMessageList.get(mModel.selectedMessagePosition + 1);
                    intentAddBefore.putExtra(EditorActivity.EXTRA_SEND_TIMESTAMP_START, before.getCreateTimeStamp());
                }
                intentAddBefore.putExtra(EditorActivity.EXTRA_SEND_TIMESTAMP_STOP, selectedMessage.getCreateTimeStamp());
                startActivity(intentAddBefore);
                break;
            case R.id.item_add_after:
                Intent intentAddAfter = new Intent(this, EditorActivity.class);
                intentAddAfter.putExtra(EditorActivity.EXTRA_EDIT_MODE, EditorViewModel.EDIT_MODE_ADD_AFTER);
                if (mModel.selectedMessagePosition > 0) {
                    Message after = mMessageList.get(mModel.selectedMessagePosition - 1);
                    intentAddAfter.putExtra(EditorActivity.EXTRA_SEND_TIMESTAMP_STOP, after.getCreateTimeStamp());
                }
                intentAddAfter.putExtra(EditorActivity.EXTRA_SEND_TIMESTAMP_START, selectedMessage.getCreateTimeStamp());
                startActivity(intentAddAfter);
                break;
            case R.id.item_delete:
                mModifier.putPendingEdition(Edition.remove(selectedMessage));
                mMessageEditorViewModel.notifyMessageDeleted();
                break;
            case R.id.item_edit:
                Intent intentEdit = new Intent(this, EditorActivity.class);
                intentEdit.putExtra(EditorActivity.EXTRA_EDIT_MODE, EditorViewModel.EDIT_MODE_EDIT);
                startActivity(intentEdit);
                break;
            case R.id.item_check:
                UiUtils.createDialog(this, mModel.selectedMessagePosition + "",
                        mModel.getSelectedMessage() == null ? "null" : mModel.getSelectedMessage().toSpannedString()).show();
                break;
        }
        return super.onContextItemSelected(item);
    }

    private class MessageFragmentAdapter extends FragmentStateAdapter {
        MessageFragmentAdapter(FragmentActivity fragmentActivity) {
            super(fragmentActivity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            return mPages[position];
        }


        @Override
        public int getItemCount() {
            return mPages.length;
        }
    }

    private class MessageAdapter extends RecyclerView.Adapter<MessageViewHolder> {
        private final int FLAG_IS_SEND = 13;
        private final int ITEM_TYPE_PLAIN = 1;
        private final int ITEM_TYPE_SYSTEM = 2;
        private final int ITEM_TYPE_COMPLEX = 3;
        private static final int POSITION_TYPE_TOP = 2;
        private static final int POSITION_TYPE_BOTTOM = 0;
        private static final int POSITION_TYPE_CENTER = 1;
        private static final int POSITION_TYPE_SINGLE = -1;
        private final int SEPARATOR_MILLS = 5 * 60 * 1000;
        private int mInvisibleAvatarIndex = -1;
        private static final int PAYLOAD_NAVIGATE = 0;
        private static final int PAYLOAD_HIDE_AVATAR = 1;
        private static final int PAYLOAD_SHOW_AVATAR = 2;

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
                    return left ? R.layout.item_bubble_cl : R.layout.item_bubble_cr;
                case ITEM_TYPE_PLAIN:
                    return left ? R.layout.item_bubble_pl : R.layout.item_bubble_pr;

            }
            throw new IllegalArgumentException("No such type: " + compositeType);
        }

        @Override
        public int getItemViewType(int position) {
            Message message = mMessageList.get(position);
            MessageFactory.Type type = message.getType();
            if (message.getType() == MessageFactory.Type.SYSTEM) {
                return ITEM_TYPE_SYSTEM;
            } else {
                if (type.isComplex()) {
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
        public void onBindViewHolder(@NonNull MessageViewHolder holder, int position, @NonNull List<Object> payloads) {
            if (payloads.size() > 0) {
                int payload = (int) payloads.get(0);
                switch (payload) {
                    case PAYLOAD_NAVIGATE:
                        View container = holder.binding.getRoot().findViewById(R.id.msg_container);
                        container.setPressed(true);
                        container.setPressed(false);
                        break;
                    case PAYLOAD_HIDE_AVATAR:
                       /* mInvisibleAvatarIndex = position;
                        holder.binding.getRoot().findViewById(R.id.iv_avatar).setVisibility(View.INVISIBLE);*/
                        break;
                    case PAYLOAD_SHOW_AVATAR:
                        /*mInvisibleAvatarIndex = -1;
                        holder.binding.getRoot().findViewById(R.id.iv_avatar).setVisibility(View.VISIBLE);
                        */
                        break;
                }
            } else {
                super.onBindViewHolder(holder, position, payloads);
            }
        }

        private boolean isLegalNeighborWithNext(int position, Message cur) {
            if (position != 0) {
                Message nex = mMessageList.get(position - 1);
                return (nex.getSenderId() != null && nex.getSenderId().equals(cur.getSenderId())) && Math.abs(nex.getCreateTimeStamp() - cur.getCreateTimeStamp()) <= SEPARATOR_MILLS;
            }
            return false;
        }

        private boolean isLegalNeighborWithPrevious(int position, Message cur) {
            if (position != mMessageList.size() - 1) {
                Message pre = mMessageList.get(position + 1);
                return (pre.getSenderId() != null && pre.getSenderId().equals(cur.getSenderId())) && Math.abs(pre.getCreateTimeStamp() - cur.getCreateTimeStamp()) <= SEPARATOR_MILLS;
            }
            return false;
        }


        private int getPositionType(int position, Message cur) {
            if (isLegalNeighborWithNext(position, cur)) {
                if (isLegalNeighborWithPrevious(position, cur)) {
                    return POSITION_TYPE_CENTER;
                } else {
                    return POSITION_TYPE_TOP;
                }
            } else {
                if (isLegalNeighborWithPrevious(position, cur)) {
                    return POSITION_TYPE_BOTTOM;
                } else {
                    return POSITION_TYPE_SINGLE;
                }
            }
        }

        @Nullable
        private Account getAccount(@NotNull Message message) {
            if (!message.getType().isSystem()) {
                if (message.isSend()) {
                    return getCurrentUser();
                } else {
                    return message.isInGroupChat() ? message.getSenderAccount() : mTalker;
                }
            }
            return null;
        }

        @Override
        public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
            Message message = mMessageList.get(position);
            holder.binding.setVariable(BR.message, message);
            holder.binding.setVariable(BR.positionType, getPositionType(position, message));
            holder.binding.setVariable(BR.account, getAccount(message));
            if (holder.ivAvatar != null) {
                holder.ivAvatar.setVisibility(position == mInvisibleAvatarIndex ? View.INVISIBLE : View.VISIBLE);
            }
            holder.binding.executePendingBindings();
        }

        @Override
        public int getItemCount() {
            return mMessageList.size();
        }
    }

    public class MessageViewHolder extends RecyclerView.ViewHolder {
        ViewDataBinding binding;
        ImageView ivAvatar;

        public void gotoDetail(View view) {
            Intent intent = new Intent(MessageActivity.this, DetailActivity.class);
            intent.putExtra(DetailActivity.EXTRA_CONTACT_ID, mMessageList.get(getAdapterPosition()).requireSenderId());
            startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(MessageActivity.this, view, view.getTransitionName()).toBundle());
        }

        public MessageViewHolder(@NonNull ViewDataBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            ivAvatar = itemView.findViewById(R.id.iv_avatar);
            this.binding.setVariable(BR.model, mModel);
            // this.itemView.findViewById(R.id.msg_container).setOnClickListener();
        }

    }

    @Override
    public void onBackPressed() {
        if (mBinding.messagePanel.isOpen()) {
            mBinding.messagePanel.closePanel();
        } else {
            mModifier.removeAllPendingEditions();
            mMessageEditorViewModel.purge();
            super.onBackPressed();
        }
    }
}
