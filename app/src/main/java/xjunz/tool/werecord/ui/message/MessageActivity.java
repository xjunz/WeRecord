/*
 * Copyright (c) 2021 xjunz. 保留所有权利
 */

package xjunz.tool.werecord.ui.message;

import android.annotation.SuppressLint;
import android.app.ActivityOptions;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.ArrayMap;
import android.util.Pair;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import xjunz.tool.werecord.App;
import xjunz.tool.werecord.BR;
import xjunz.tool.werecord.R;
import xjunz.tool.werecord.data.viewmodel.MessageViewModel;
import xjunz.tool.werecord.databinding.ActivityMessageBinding;
import xjunz.tool.werecord.impl.DatabaseModifier;
import xjunz.tool.werecord.impl.Environment;
import xjunz.tool.werecord.impl.model.account.Talker;
import xjunz.tool.werecord.impl.model.message.ComplexMessage;
import xjunz.tool.werecord.impl.model.message.Message;
import xjunz.tool.werecord.impl.model.message.SystemMessage;
import xjunz.tool.werecord.impl.model.message.util.Edition;
import xjunz.tool.werecord.impl.model.message.util.Template;
import xjunz.tool.werecord.impl.repo.MessageRepository;
import xjunz.tool.werecord.impl.repo.RepositoryFactory;
import xjunz.tool.werecord.ui.base.RecycleSensitiveActivity;
import xjunz.tool.werecord.ui.customview.MasterToast;
import xjunz.tool.werecord.ui.customview.MessagePanel;
import xjunz.tool.werecord.ui.main.DetailActivity;
import xjunz.tool.werecord.ui.message.fragment.EditionFragment;
import xjunz.tool.werecord.ui.message.fragment.SearchFragment;
import xjunz.tool.werecord.ui.message.fragment.StatisticsFragment;
import xjunz.tool.werecord.ui.message.fragment.dialog.MessageViewerDialog;
import xjunz.tool.werecord.ui.message.fragment.dialog.TemplateSetupDialog;
import xjunz.tool.werecord.util.RxJavaUtils;
import xjunz.tool.werecord.util.UiUtils;

public class MessageActivity extends RecycleSensitiveActivity {
    public static final String EXTRA_TALKER = "MessageActivity.extra.talker";
    /**
     * 初始加载的消息数，不宜也没必要过大，否则可能造成卡顿和内存的浪费
     */
    private static final int INITIAL_LOAD_COUNT = 50;
    /**
     * 加载更多消息记录的消息数，没必要过多，浪费内存资源且用户一般不会查看这么多记录
     */
    private static final int LATER_LOAD_COUNT = 100;
    /**
     * 加载更多消息时假装加载的时间
     * <p>因为读取消息记录实际上耗时很，为了防止加载进度条一闪而过的情况，设置
     * 假装加载时长</p>
     */
    private static final long MINIMUM_LOAD_MILLS = 500L;
    private Talker mTalker;
    private RecyclerView mRvMessage;
    /**
     * 当前显示的消息数据列表，仅持有{@link MessageViewModel#currentLoadedMessages}的引用，
     * 切勿赋值给此变量
     */
    private List<Message> mMessageList;
    private MessageRepository mMessageRepo;
    private MessageAdapter mAdapter;
    private ActivityMessageBinding mBinding;
    private MessageViewModel mModel;
    private Fragment[] mPages;
    /**
     * 当前需要被导航的消息。当前消息列表会滑动到此消息的位置，予以导航。
     */
    private Message mMessageToNavigate;
    /**
     * 需要闪烁提醒的消息索引
     *
     * @see MessageAdapter#onBindViewHolder(MessageViewHolder, int, List)
     */
    private int mMessageIndexToBlink = -1;
    private DatabaseModifier mModifier;
    private int mSelectedMsgIndex;
    private Message mSelectedMsg;
    private long mGeneratedMsgId = -1;

    private void startEditorForResult(int editMode, @NotNull Intent intent) {
        ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(this);
        intent.putExtra(EditorActivity.EXTRA_MESSAGE_ORIGIN, mSelectedMsg);
        intent.putExtra(EditorActivity.EXTRA_EDIT_MODE, editMode);
        startActivityForResult(intent, editMode, options.toBundle());
    }

    @Override
    protected void onCreateNormally(@Nullable Bundle savedInstanceState) {
        mTalker = getIntent().getParcelableExtra(EXTRA_TALKER);
        if (mTalker == null) {
            finish();
            return;
        }
        mMessageRepo = RepositoryFactory.get(MessageRepository.class);
        mModifier = Environment.getInstance().modifyDatabase();
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_message);
        mRvMessage = mBinding.rvMessage;
        mModel = getViewModel(MessageViewModel.class);
        mModel.currentTalker = mTalker;
        mMessageList = mModel.currentLoadedMessages;
        mBinding.setModel(mModel);
        initList();
        initPages();
        initPanel();
    }

    public void restore(int index, @NotNull Message target) {
        Message backup = mModel.getUnconfirmedBackup(target.getMsgId());
        //删除的消息没有备份，加以判断
        //新增的消息禁用还原，不必判断
        if (backup == null) {
            target.removeEdition();
            mAdapter.notifyItemChanged(index);
        } else {
            mModel.removeUnconfirmedBackup(backup);
            notifyItemChangedConsideringTimestamp(backup, index);
        }
        mModifier.removePendingEdition(target.getMsgId());
    }


    public void navigate(Message msg) {
        mBinding.messagePanel.closePanel();
        mMessageToNavigate = msg;
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
            UiUtils.toast(R.string.message_not_found);
        }
    }

    private void initPages() {
        mPages = new Fragment[3];
        mPages[0] = new StatisticsFragment();
        mPages[1] = new EditionFragment();
        mPages[2] = new SearchFragment();
        mBinding.vpMessage.setAdapter(new MessageFragmentAdapter(this));
        mBinding.vpMessage.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            int cur;

            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                cur = position;
                if (mPages[position] instanceof SearchFragment && !mModel.isLoadingAll.get() && !mModel.hasLoadedAll.get()) {
                    loadAllMessages(null);
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                super.onPageScrollStateChanged(state);
                if (state == ViewPager2.SCROLL_STATE_IDLE && mPages[cur] instanceof SearchFragment && mModel.hasLoadedAll.get()) {
                    showImeFor(mBinding.etSearch);
                }
            }
        });
    }

    private void initPanel() {
        mBinding.messagePanel.addOnPanelSlideListener(new MessagePanel.OnPanelSlideListener() {
            @Override
            public void onPanelSlideFinished(boolean isOpen) {
                if (!isOpen && mMessageToNavigate != null) {
                    doNavigate(mMessageToNavigate);
                    mMessageToNavigate = null;
                } else if (isOpen) {
                    if (mModel.hasLoadedAll.get()) {
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

    private Disposable mLoadAllDisposable;

    /**
     * 并发加载所有的消息记录，默认的并发线程数为CPU核心数+1
     */
    public void loadAllMessages(@Nullable Runnable onSuccess) {
        //如果已经全部加载完，直接返回
        if (mModel.hasLoadedAll.get()) {
            return;
        }
        //否则设置并发数
        int defaultGroupCount = Runtime.getRuntime().availableProcessors() + 1;
        //获取已经加载的消息数
        int preloadedCount = mModel.currentLoadedMessages.size();
        //获取需要加载的消息数（总消息数减去已加载的消息数）
        long msgCount = mModel.actualMessageCount - preloadedCount;
        //每个线程平均要加载的消息数
        int unitCount = (int) (msgCount / defaultGroupCount);
        //初始化ExecutorService
        ExecutorService executor = Executors.newFixedThreadPool(defaultGroupCount);
        //显示进度条
        mModel.isLoadingAll.set(true);
        mLoadAllDisposable = Flowable.range(0, defaultGroupCount).map(groupOrdinal -> {
            //返回每个线程需要加载的消息起点和消息数
            //最后一个分组需要多加载平均分后剩余的消息数
            int offset = unitCount * groupOrdinal;
            long count = unitCount;
            if (groupOrdinal == defaultGroupCount - 1) {
                count = msgCount - (groupOrdinal) * unitCount;
            }
            return new Pair<>(offset + preloadedCount, count);
        })
                .parallel()
                .runOn(Schedulers.from(executor))
                .flatMap(offsetLimitPair -> Flowable.create((FlowableOnSubscribe<List<Message>>) emitter -> {
                    if (offsetLimitPair.second != 0) {
                        //从数据库加载消息
                        emitter.onNext(RepositoryFactory.get(MessageRepository.class).queryMessageByTalkerLimit(mModel.currentTalker.id, offsetLimitPair.first, offsetLimitPair.second));
                    }
                    emitter.onComplete();
                }, BackpressureStrategy.BUFFER))
                .sequential()
                .observeOn(AndroidSchedulers.mainThread())
                //对消息进行排序，并行加载的消息是无序的
                .sorted((o1, o2) -> -Long.compare(o1.get(0).getCreateTimeStamp(), o2.get(0).getCreateTimeStamp()))
                .subscribe(messages -> {
                    //添加进消息列表
                    mMessageList.addAll(messages);
                }, throwable -> {
                    throwable.printStackTrace();
                    mModel.isLoadingAll.set(false);
                    executor.shutdown();
                }, () -> {
                    //通知更新
                    mModel.hasLoadedAll.set(true);
                    mAdapter.notifyItemInserted(preloadedCount);
                    showImeFor(mBinding.etSearch);
                    mModel.isLoadingAll.set(false);
                    executor.shutdown();
                    if (onSuccess != null) {
                        onSuccess.run();
                    }
                });
    }

    private void initList() {
        RxJavaUtils.complete(() -> {
            mModel.actualMessageCount = mMessageRepo.getActualMessageCountOf(mTalker.id);
            mMessageRepo.queryMessageByTalkerLimit(mTalker.id, INITIAL_LOAD_COUNT, mMessageList);
            mModel.hasLoadedAll.set(mMessageList.size() >= mModel.actualMessageCount);
            if (mModifier.isMessageBackupTableExists()) {
                mMessageRepo.queryBackupMessagesByTalker(mTalker.id, mModel.confirmedBackups);
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
                    if (mModel.hasLoadedAll.get()) {
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
                        Thread.sleep(MINIMUM_LOAD_MILLS);
                        return mMessageRepo.queryMessageByTalkerLimit(mTalker.id, LATER_LOAD_COUNT, mMessageList);
                    }).subscribe(new RxJavaUtils.SingleObserverAdapter<Integer>() {
                        @Override
                        public void onSuccess(@NotNull Integer count) {
                            super.onSuccess(count);
                            //关闭加载进度条并更新数据
                            UiUtils.fadeOut(mBinding.pbLoad);
                            mModel.hasLoadedAll.set(mMessageList.size() >= mModel.actualMessageCount);
                            mAdapter.notifyItemInserted(mMessageList.size() - count);
                            mBinding.rvMessage.smoothScrollBy(0, -100, new AccelerateDecelerateInterpolator());
                            mBinding.rvMessage.setNestedScrollingEnabled(true);
                        }
                    });
                }
            }
        });
    }

    /**
     * 重新从数据库加载消息
     */
    public void reloadMessages() {
        //获取真实消息数（修改后）
        ArrayMap<Long, Edition> all = mModifier.getAllPendingEditions();
        int delCount = 0;
        int insCount = 0;
        for (int i = 0; i < all.size(); i++) {
            int flag = all.valueAt(i).getFlag();
            if (flag == Edition.FLAG_INSERTION) {
                insCount++;
            } else if (flag == Edition.FLAG_REMOVAL) {
                delCount++;
            }
        }
        int loadCount = mMessageList.size() - delCount + insCount;
        mModel.actualMessageCount = mMessageRepo.getActualMessageCountOf(mTalker.id);
        //清空
        mMessageList.clear();
        //重新查询消息
        mMessageRepo.queryMessageByTalkerLimit(mTalker.id, loadCount, mMessageList);
        mModel.hasLoadedAll.set(mMessageList.size() >= mModel.actualMessageCount);
    }

    private void reloadBackupMessages() {
        mModel.confirmedBackups.clear();
        //重新查询备份
        if (mModifier.isMessageBackupTableExists()) {
            mMessageRepo.queryBackupMessagesByTalker(mTalker.id, mModel.confirmedBackups);
        }
    }

    public void notifyMessageListChanged() {
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        getMenuInflater().inflate(R.menu.message, menu);
        menu.setHeaderTitle(R.string.operation);
        //如果消息未编辑
        if (!mSelectedMsg.isEdited()) {
            //禁用还原
            menu.findItem(R.id.item_restore).setEnabled(false);
        } else {
            //如果选中消息已删除
            if (mSelectedMsg.getEditionFlag() == Edition.FLAG_REMOVAL) {
                //禁用编辑和删除
                menu.findItem(R.id.item_edit).setEnabled(false);
                menu.findItem(R.id.item_delete).setEnabled(false);
            }
            //如果选中消息是新增的
            else if (mSelectedMsg.getEditionFlag() == Edition.FLAG_INSERTION) {
                //禁用还原（没有原消息，删除即还原）
                menu.findItem(R.id.item_restore).setEnabled(false);
            }
        }
    }


    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        //如果消息解析失败，除了“还原”，不允许其他操作
        if (mSelectedMsg.isParseError() && item.getItemId() != R.id.item_restore) {
            MasterToast.shortToast(getString(R.string.format_error_parse_message, mSelectedMsg.getParseErrorCode()));
            return super.onContextItemSelected(item);
        }
        mModifier = getEnvironment().modifyDatabase();
        mBinding.setModifier(mModifier);
        switch (item.getItemId()) {
            case R.id.item_add_before:
                if (!App.config().isEditModeEnabled()) {
                    MasterToast.shortToast(R.string.edit_mode_not_enabled);
                    return super.onContextItemSelected(item);
                }
                Intent intentAddBefore = new Intent(this, EditorActivity.class);
                if (mSelectedMsgIndex < mMessageList.size() - 1) {
                    Message before = mMessageList.get(mSelectedMsgIndex + 1);
                    intentAddBefore.putExtra(EditorActivity.EXTRA_SEND_TIMESTAMP_START, before.getCreateTimeStamp());
                }
                intentAddBefore.putExtra(EditorActivity.EXTRA_SEND_TIMESTAMP_STOP, mSelectedMsg.getCreateTimeStamp());
                startEditorForResult(EditorActivity.EDIT_MODE_ADD_BEFORE, intentAddBefore);
                break;
            case R.id.item_add_after:
                if (!App.config().isEditModeEnabled()) {
                    MasterToast.shortToast(R.string.edit_mode_not_enabled);
                    return super.onContextItemSelected(item);
                }
                Intent intentAddAfter = new Intent(this, EditorActivity.class);
                if (mSelectedMsgIndex > 0) {
                    Message after = mMessageList.get(mSelectedMsgIndex - 1);
                    intentAddAfter.putExtra(EditorActivity.EXTRA_SEND_TIMESTAMP_STOP, after.getCreateTimeStamp());
                }
                intentAddAfter.putExtra(EditorActivity.EXTRA_SEND_TIMESTAMP_START, mSelectedMsg.getCreateTimeStamp());
                startEditorForResult(EditorActivity.EDIT_MODE_ADD_AFTER, intentAddAfter);
                break;
            case R.id.item_edit:
                if (!App.config().isEditModeEnabled()) {
                    MasterToast.shortToast(R.string.edit_mode_not_enabled);
                    return super.onContextItemSelected(item);
                }
                Intent intentEdit = new Intent(this, EditorActivity.class);
                startEditorForResult(EditorActivity.EDIT_MODE_EDIT, intentEdit);
                break;
            case R.id.item_delete:
                if (!App.config().isEditModeEnabled()) {
                    MasterToast.shortToast(R.string.edit_mode_not_enabled);
                    return super.onContextItemSelected(item);
                }
                //如果是新增的消息，删除后直接从列表移除
                if (mSelectedMsg.getEditionFlag() == Edition.FLAG_INSERTION) {
                    mMessageList.remove(mSelectedMsg);
                    mAdapter.notifyItemRemoved(mSelectedMsgIndex);
                    mModifier.removePendingEdition(mSelectedMsg.getMsgId());
                    mModel.notifyMessageRestored(Edition.FLAG_INSERTION, EditionFragment.EDITION_SET_INDEX_UNCONFIRMED);
                }
                //否则
                else {
                    //添加未处理编辑
                    mModifier.putPendingEdition(Edition.remove(mSelectedMsg));
                    //设置编辑标志
                    mSelectedMsg.setEditionFlag(Edition.FLAG_REMOVAL);
                    //更新UI
                    mAdapter.notifyItemChanged(mSelectedMsgIndex);
                    //发布全局事件
                    mModel.notifyMessageDeleted();
                }
                break;
            case R.id.item_check:
                new MessageViewerDialog().setMessage(mSelectedMsg).show(getSupportFragmentManager(), "message_viewer");
                break;
            case R.id.item_restore:
                int editionFlag = mSelectedMsg.getEditionFlag();
                restore(mSelectedMsgIndex, mSelectedMsg);
                mModel.notifyMessageRestored(editionFlag, EditionFragment.EDITION_SET_INDEX_UNCONFIRMED);
                break;
            case R.id.item_set_as_template:
                new TemplateSetupDialog().setSourceTemplate(Template.fromMessage(mSelectedMsg)).show(getSupportFragmentManager(), "template");
                break;
        }
        return super.onContextItemSelected(item);
    }

    private void notifyItemChangedConsideringTimestamp(@NotNull Message edited, int editedIndex) {
        boolean timestampChanged = edited.getCreateTimeStamp() != mSelectedMsg.getCreateTimeStamp();
        if (timestampChanged) {
            mMessageList.remove(editedIndex);
            int index = Collections.binarySearch(mMessageList, edited, (o1, o2) -> -Long.compare(o1.getCreateTimeStamp(), o2.getCreateTimeStamp()));
            int insertion = index >= 0 ? index + 1 : -(index + 1);
            mMessageList.add(insertion, edited);
            mAdapter.notifyItemChanged(editedIndex);
            if (insertion != editedIndex) {
                mAdapter.notifyItemMoved(editedIndex, insertion);
            }
        } else {
            mMessageList.set(editedIndex, edited);
            mAdapter.notifyItemChanged(editedIndex);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data == null) {
            return;
        }
        Message returned = data.getParcelableExtra(EditorActivity.EXTRA_MESSAGE_ORIGIN);
        switch (requestCode) {
            case EditorActivity.EDIT_MODE_EDIT:
                Message backup = mModel.getUnconfirmedBackup(mSelectedMsg.getMsgId());
                //如果发送者不是用户本人，恢复消息状态，否则消息状态可能混乱（比如对方拥有发送失败标志）
                if (backup != null && returned.getSenderId() != null && !returned.getSenderId().equals(getCurrentUser().id)) {
                    returned.setStatus(backup.getStatus());
                }
                //如果更改后的内容和备份的内容一致，说明此次更改恢复了原来的消息
                if (backup != null && backup.deepEquals(returned)) {
                    MasterToast.shortToast(R.string.message_restored);
                    mModel.removeUnconfirmedBackup(backup);
                    mModifier.removePendingEdition(mSelectedMsg.getMsgId());
                    notifyItemChangedConsideringTimestamp(backup, mSelectedMsgIndex);
                    mModel.notifyMessageRestored(mSelectedMsg.getEditionFlag(), EditionFragment.EDITION_SET_INDEX_UNCONFIRMED);
                    return;
                }
                //备份...
                mModel.addUnconfirmedBackupIfNotExists(mSelectedMsg);
                //添加...
                mModifier.putPendingEdition(Edition.replace(mSelectedMsg, returned));
                //设置...
                if (mSelectedMsg.getEditionFlag() != Edition.FLAG_INSERTION) {
                    returned.setEditionFlag(Edition.FLAG_REPLACEMENT);
                }
                notifyItemChangedConsideringTimestamp(returned, mSelectedMsgIndex);
                mModel.notifyMessageChanged();
                break;
            case EditorActivity.EDIT_MODE_ADD_AFTER:
            case EditorActivity.EDIT_MODE_ADD_BEFORE:
                if (mGeneratedMsgId == -1) {
                    mGeneratedMsgId = mMessageRepo.getMaxMsgId();
                }
                //为它设置一个独一无二的ID
                returned.getValues().put(Message.KEY_MSG_ID, ++mGeneratedMsgId);
                returned.setEditionFlag(Edition.FLAG_INSERTION);
                mModifier.putPendingEdition(Edition.insert(returned));
                int insertion = requestCode == EditorActivity.EDIT_MODE_ADD_AFTER ? mSelectedMsgIndex : mSelectedMsgIndex + 1;
                mMessageList.add(insertion, returned);
                mAdapter.notifyItemInserted(insertion);
                mModel.notifyMessageInserted();
                break;
        }
    }

    public void checkParseFailedMessages(Runnable ok) {
        ArrayMap<Long, Edition> all = mModifier.getAllPendingEditions();
        for (int i = 0; i < all.size(); i++) {
            Message rep = all.valueAt(i).getFiller();
            if (rep != null && rep.isParseError()) {
                UiUtils.createCaveat(this, getString(R.string.warning_parse_failed_message)).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (ok != null) {
                            ok.run();
                        }
                    }
                }).setNegativeButton(android.R.string.cancel, null).show();
                return;
            }
        }
        ok.run();
    }

    public void applyChanges(View view) {
        checkParseFailedMessages(() -> UiUtils.createAlert(this, getString(R.string.alert_apply_changes))
                .setPositiveButton(android.R.string.ok, (dialog, which) -> RxJavaUtils.complete(() -> mModifier.applyAllPendingEditions()).subscribe(new RxJavaUtils.CompletableObservableAdapter() {
                    Dialog progress;

                    @Override
                    public void onSubscribe(@NotNull Disposable d) {
                        progress = UiUtils.createProgress(MessageActivity.this, R.string.applying_changes);
                        progress.show();
                    }

                    @Override
                    public void onComplete() {
                        RxJavaUtils.complete(() -> {
                            reloadMessages();
                            reloadBackupMessages();
                        }).doFinally(progress::dismiss).subscribe(new RxJavaUtils.CompletableObservableAdapter() {
                            @Override
                            public void onComplete() {
                                mModel.notifyEditionListChanged(EditionFragment.EDITION_SET_INDEX_CONFIRMED);
                                mAdapter.notifyDataSetChanged();
                                UiUtils.createLaunch(MessageActivity.this).show();
                            }

                            @Override
                            public void onError(@NotNull Throwable e) {
                                UiUtils.createError(MessageActivity.this, e).show();
                            }
                        });
                    }

                    @Override
                    public void onError(@NotNull Throwable e) {
                        progress.dismiss();
                        UiUtils.createError(MessageActivity.this, e).show();
                    }
                })).setNegativeButton(android.R.string.cancel, null).show());

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
        private static final int PAYLOAD_NAVIGATE = 0;

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
            if (message instanceof SystemMessage) {
                return ITEM_TYPE_SYSTEM;
            } else {
                if (message instanceof ComplexMessage) {
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
                if (payload == PAYLOAD_NAVIGATE) {
                    View container = holder.binding.getRoot().findViewById(R.id.msg_container);
                    container.setPressed(true);
                    container.setPressed(false);
                }
            } else {
                super.onBindViewHolder(holder, position, payloads);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
            Message message = mMessageList.get(position);
            holder.binding.setVariable(BR.msg, message);
            holder.binding.setVariable(BR.vh, holder);
            holder.binding.executePendingBindings();
        }

        @Override
        public int getItemCount() {
            return mMessageList.size();
        }
    }

    public class MessageViewHolder extends RecyclerView.ViewHolder {
        ViewDataBinding binding;

        public void gotoDetail(View view) {
            Intent intent = new Intent(MessageActivity.this, DetailActivity.class);
            intent.putExtra(DetailActivity.EXTRA_CONTACT_ID, mMessageList.get(getAdapterPosition()).getSenderId());
            startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(MessageActivity.this, view, view.getTransitionName()).toBundle());
        }

        public void setSelectedMsgIndex(int index) {
            mSelectedMsgIndex = index;
            mSelectedMsg = mMessageList.get(index);
        }

        public MessageViewHolder(@NonNull ViewDataBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

    }

    @Override
    public void onBackPressed() {
        if (mBinding.messagePanel.isOpen()) {
            mBinding.messagePanel.closePanel();
        } else {
            if (mModifier.isThereAnyPendingEdition()) {
                UiUtils.createAlert(this, R.string.alert_quit_discard_changes).setPositiveButton(R.string.quit, (dialog, which) -> {
                    MessageActivity.super.onBackPressed();
                }).show();
            } else {
                super.onBackPressed();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //清空所有未应用的编辑
        mModifier.discardAllPendingEditions();
        if (mLoadAllDisposable != null && !mLoadAllDisposable.isDisposed()) {
            mLoadAllDisposable.dispose();
        }
    }
}
