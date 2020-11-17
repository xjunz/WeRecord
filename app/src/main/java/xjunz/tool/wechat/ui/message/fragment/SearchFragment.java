/*
 * Copyright (c) 2020 xjunz. 保留所有权利
 */
package xjunz.tool.wechat.ui.message.fragment;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import org.reactivestreams.Publisher;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import xjunz.tool.wechat.R;
import xjunz.tool.wechat.data.viewmodel.MessageViewModel;
import xjunz.tool.wechat.databinding.FragmentSearchBinding;
import xjunz.tool.wechat.databinding.ItemMessageSearchResultBinding;
import xjunz.tool.wechat.impl.model.message.Message;
import xjunz.tool.wechat.impl.repo.MessageRepository;
import xjunz.tool.wechat.impl.repo.RepositoryFactory;
import xjunz.tool.wechat.util.UiUtils;

/**
 * 承载消息搜索功能和显示搜索结果的{@link Fragment}
 */
public class SearchFragment extends Fragment {
    /**
     * 所有消息列表，此变量仅持有{@link MessageViewModel#currentLoadedMessages}的引用，请勿给此变量复制
     */
    private List<Message> mAllMessages;
    /**
     * 当前搜索结果
     */
    private final List<MessageItem> mSearchResult = new ArrayList<>();
    /**
     * 加载所有消息的{@link Disposable}
     */
    private Disposable mLoadAllDisposable;
    /**
     * 搜索消息的{@link Disposable}
     */
    private Disposable mSearchDisposable;
    /**
     * 当前功能模块的数据模型对象
     */
    private MessageViewModel mModel;
    /**
     * 当前页面的{@link androidx.databinding.ViewDataBinding}实例
     */
    private FragmentSearchBinding mBinding;
    /**
     * 搜索列表的{@link androidx.recyclerview.widget.RecyclerView.Adapter}
     */
    private MessageResultAdapter mAdapter;
    /**
     * {@link xjunz.tool.wechat.ui.message.MessageActivity}发送的事件的处理者
     */
    private final MessageViewModel.EventHandler mHandler = new MessageViewModel.EventHandler() {
        @Override
        public void onSearch(String keyword) {
            super.onSearch(keyword);
            doSearch(keyword);
        }

    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mModel = new ViewModelProvider(requireActivity(), new ViewModelProvider.NewInstanceFactory()).get(MessageViewModel.class);
        mModel.addEventHandler(mHandler);
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_search, container, false);
        loadAllMessages();
        return mBinding.getRoot();
    }


    /**
     * 并发加载所有的消息记录，默认的并发线程数为CPU核心数+1
     */
    private void loadAllMessages() {
        //获取已加载的消息列表
        mAllMessages = mModel.currentLoadedMessages;
        //如果以及全部加载完，直接返回
        if (mModel.hasLoadAll.get()) {
            return;
        }
        //否则设置并发数
        int defaultGroupCount = Runtime.getRuntime().availableProcessors() + 1;
        //获取已经加载的消息数
        int preloadedCount = mModel.currentLoadedMessages.size();
        //获取需要加载的消息数（总消息数减去已加载的消息数）
        int msgCount = mModel.actualMessageCount - preloadedCount;
        //每个线程平均要加载的消息数
        int unitCount = msgCount / defaultGroupCount;
        //初始化ExecutorService
        ExecutorService executor = Executors.newFixedThreadPool(defaultGroupCount);
        //显示进度条
        if (mBinding.pbLoad.getVisibility() != View.VISIBLE) {
            UiUtils.fadeIn(mBinding.pbLoad);
        }
        mLoadAllDisposable = Flowable.range(0, defaultGroupCount).map(groupOrdinal -> {
            //返回每个线程需要加载的消息起点和消息数
            //最后一个分组需要多加载平均分后剩余的消息数
            int offset = unitCount * groupOrdinal;
            int count = unitCount;
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
                    mAllMessages.addAll(messages);
                }, throwable -> {
                    throwable.printStackTrace();
                    executor.shutdown();
                    UiUtils.fadeOut(mBinding.pbLoad);
                }, () -> {
                    //通知更新
                    mModel.notifyAllLoaded(preloadedCount);
                    executor.shutdown();
                    UiUtils.fadeOut(mBinding.pbLoad);
                });
    }

    /**
     * 并发搜索包含关键词{@param keyword}的所有消息记录
     *
     * @param keyword 关键词
     */
    private void doSearch(String keyword) {
        //如果上一个搜索任务还没完成，直接取消
        if (mSearchDisposable != null && !mSearchDisposable.isDisposed()) {
            mSearchDisposable.dispose();
        }
        //清空搜索结果
        mSearchResult.clear();
        //如果关键词是空的，清除列表，显示占位图
        if (TextUtils.isEmpty(keyword)) {
            mAdapter.notifyDataSetChanged();
            UiUtils.fadeIn(mBinding.ivNoResult);
            return;
        }
        //显示进度条
        if (mBinding.pbLoad.getVisibility() != View.VISIBLE) {
            UiUtils.fadeIn(mBinding.pbLoad);
        }
        //并行发射所有消息
        mSearchDisposable = Flowable.fromIterable(mAllMessages)
                .parallel()
                .runOn(Schedulers.computation())
                .flatMap((Function<Message, Publisher<MessageItem>>) message -> Flowable.create((FlowableOnSubscribe<MessageItem>) emitter -> {
                    //检查是否还有关键词，有的话通过筛选
                    String content = message.getParsedContent();
                    int index;
                    if ((index = content.indexOf(keyword)) >= 0) {
                        MessageItem item = new MessageItem(message);
                        item.spanLength = keyword.length();
                        item.spanStartIndex = index;
                        emitter.onNext(item);
                    }
                    emitter.onComplete();
                }, BackpressureStrategy.BUFFER))
                .sequential()
                //对过滤出的消息进行排序（按时间逆序->从新到旧）
                .sorted((o1, o2) -> -Long.compare(o1.message.getCreateTimeStamp(), o2.message.getCreateTimeStamp()))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(mSearchResult::add, Throwable::printStackTrace, () -> {
                    //更新列表
                    if (mAdapter == null) {
                        mAdapter = new MessageResultAdapter();
                        mBinding.rvResult.setAdapter(mAdapter);
                    } else {
                        mAdapter.notifyDataSetChanged();
                    }
                    //如果没搜到东西，显示展占位图
                    if (mAdapter.getItemCount() == 0) {
                        UiUtils.fadeIn(mBinding.ivNoResult);
                    } else {
                        UiUtils.invisible(mBinding.ivNoResult);
                    }
                    //隐藏进度条
                    UiUtils.fadeOut(mBinding.pbLoad);
                });
    }

    /**
     * 用于显示搜索结果的的数据项实体类，包装了{@link Message}和搜索
     * 关键字的所在的索引和长度，用于显示{@link android.text.Spanned}
     */
    public static class MessageItem {
        public Message message;
        public int spanStartIndex, spanLength;

        public MessageItem(Message message) {
            this.message = message;
            this.spanStartIndex = -1;
            this.spanLength = -1;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mLoadAllDisposable != null && !mLoadAllDisposable.isDisposed()) {
            mLoadAllDisposable.dispose();
        }
        if (mSearchDisposable != null && !mSearchDisposable.isDisposed()) {
            mSearchDisposable.dispose();
        }
    }

    private class MessageResultAdapter extends RecyclerView.Adapter<MessageResultViewHolder> {

        @NonNull
        @Override
        public MessageResultViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemMessageSearchResultBinding binding = DataBindingUtil.inflate(getLayoutInflater(), R.layout.item_message_search_result, parent, false);
            return new MessageResultViewHolder(binding);
        }

        @Override
        public void onBindViewHolder(@NonNull MessageResultViewHolder holder, int position) {
            holder.binding.setItem(mSearchResult.get(position));
            holder.binding.executePendingBindings();
        }

        @Override
        public int getItemCount() {
            return mSearchResult.size();
        }
    }

    private class MessageResultViewHolder extends RecyclerView.ViewHolder {
        ItemMessageSearchResultBinding binding;

        public MessageResultViewHolder(@NonNull ItemMessageSearchResultBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            this.binding.setModel(mModel);
        }
    }
}
