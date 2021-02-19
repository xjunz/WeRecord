/*
 * Copyright (c) 2021 xjunz. 保留所有权利
 */
package xjunz.tool.werecord.ui.message.fragment;

import android.os.Bundle;
import android.text.TextUtils;
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

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import xjunz.tool.werecord.R;
import xjunz.tool.werecord.data.viewmodel.MessageViewModel;
import xjunz.tool.werecord.databinding.FragmentSearchBinding;
import xjunz.tool.werecord.databinding.ItemMessageSearchResultBinding;
import xjunz.tool.werecord.impl.model.message.Message;
import xjunz.tool.werecord.ui.message.MessageActivity;
import xjunz.tool.werecord.util.UiUtils;

/**
 * 承载消息搜索功能和显示搜索结果的{@link Fragment}
 */
public class SearchFragment extends Fragment implements MessageViewModel.SearchDelegate {
    /**
     * 当前搜索结果
     */
    private final List<MessageItem> mSearchResult = new ArrayList<>();
    private Disposable mSearchDisposable;
    private MessageViewModel mModel;
    private FragmentSearchBinding mBinding;
    private MessageResultAdapter mAdapter;
    private MessageActivity mHost;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mModel = new ViewModelProvider(requireActivity(), new ViewModelProvider.NewInstanceFactory()).get(MessageViewModel.class);
        mModel.delegateSearch(this);
        mHost = (MessageActivity) requireActivity();
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_search, container, false);
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        //mHost.loadAllMessages();
    }

    /**
     * 并发搜索包含关键词{@param keyword}的所有消息记录
     *
     * @param keyword 关键词
     */
    @Override
    public void search(String keyword) {
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
        mSearchDisposable = Flowable.fromIterable(mModel.currentLoadedMessages)
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
            this.binding.setHost(mHost);
        }
    }
}
