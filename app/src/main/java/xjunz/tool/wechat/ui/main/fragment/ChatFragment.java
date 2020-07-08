package xjunz.tool.wechat.ui.main.fragment;

import android.text.Html;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import java.util.Arrays;
import java.util.List;

import xjunz.tool.wechat.R;
import xjunz.tool.wechat.data.viewmodel.PageConfig;
import xjunz.tool.wechat.data.viewmodel.SortBy;
import xjunz.tool.wechat.impl.model.account.Contact;
import xjunz.tool.wechat.impl.model.account.Talker;
import xjunz.tool.wechat.impl.repo.AccountRepository;
import xjunz.tool.wechat.impl.repo.TalkerRepository;
import xjunz.tool.wechat.ui.customview.MasterToast;


/**
 * 显示会话列表的{@link Fragment}
 */
public class ChatFragment extends ListPageFragment<Talker> {

    private ChatAdapter mAdapter;

    @Override
    public int getLayoutResource() {
        return R.layout.fragment_chat;
    }

    @Override
    public SortBy[] getSortByList() {
        return SortBy.values();
    }

    @Override
    public Contact.Type[] getTypeList() {
        //null表示全部匹配
        return new Contact.Type[]{null, Contact.Type.FRIEND, Contact.Type.JOINED_GROUP, Contact.Type.FOLLOWING_GZH, Contact.Type.SERVICE};
    }

    @Override
    public void initPageConfig(PageConfig config) {
        config.caption = getString(R.string.chat);
        config.isChat.set(true);
        config.sortBy.set(SortBy.TIMESTAMP);
        List<String> captionList = Arrays.asList(getResources().getStringArray(R.array.type_talker));
        config.typeList.addAll(captionList);
        config.sortByList.addAll(SortBy.getCaptionList());
        config.setEventHandler(this);
    }


    @Override
    public ChatAdapter getAdapter() {
        return mAdapter = mAdapter == null ? new ChatAdapter() : mAdapter;
    }

    @Override
    public void resetFilterConfig(PageConfig config) {
        config.sortBy.set(SortBy.TIMESTAMP);
        config.orderBy.set(PageConfig.ORDER_ASCENDING);
        config.typeSelection.set(0);
        config.descriptionSelectionMap.clear();
    }

    @Override
    public AccountRepository<Talker> getRepo() {
        return TalkerRepository.getInstance();
    }


    private class ChatAdapter extends ListPageAdapter<ChatAdapter.ChatViewHolder> {

        @NonNull
        @Override
        public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(viewType == Item.TYPE_DATA ? R.layout.item_chat : R.layout.item_separator, parent, false);
            return new ChatViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull final ChatViewHolder holder, int position) {
            super.onBindViewHolder(holder, position);
            Item item = mItemList.get(position);
            if (item.isData()) {
                //设置格式化后的时间
                holder.tvTime.setText(item.content.formatTimestamp);
                //设置记录数
                holder.tvMsgCount.setText(Html.fromHtml(getString(R.string.format_total_records, item.content.messageCount)));
            }
        }

        private class ChatViewHolder extends ListPageViewHolder {
            TextView tvMsgCount, tvTime;

            public ChatViewHolder(@NonNull View itemView) {
                super(itemView);
                tvMsgCount = itemView.findViewById(R.id.tv_msg_count);
                tvTime = itemView.findViewById(R.id.tv_time);
                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (getItemViewType() == Item.TYPE_DATA) {
                            MasterToast.shortToast("....");
                        }
                    }
                });
            }
        }
    }

}
