/*
 * Copyright (c) 2020 xjunz. 保留所有权利
 */
package xjunz.tool.wechat.ui.message;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.jetbrains.annotations.NotNull;

import de.hdodenhof.circleimageview.CircleImageView;
import xjunz.tool.wechat.R;
import xjunz.tool.wechat.ui.customview.MasterToast;
import xjunz.tool.wechat.util.UiUtils;

/**
 * 处理消息列表滑动时头像的滑动、滞留逻辑。
 * 模仿自Telegram安卓客户端。
 *
 * @author xjunz 2020/11/16 23:23
 */
public abstract class AvatarScrollHandler extends RecyclerView.OnScrollListener {


    //todo:快速划一下有两个头像
    //todo:不能直接在OnScroll里调用notifyItemChanged
    /**
     * 假头像，被添加到{@link android.view.ViewGroupOverlay}之内，实现头像跨Item滞留。
     */
    private ImageView mFakeAvatar;
    /**
     * 上一次滑动获取的最底部Item的索引记录，如果此记录和当前我们获取到的最底部Item的索引不一致，
     * 开始我们的处理逻辑。
     */
    private int mPreviousDownScrollIndex = -1;
    private int mPreviousUpScrollIndex = -1;
    private int mBaseMargin = -1;
    private boolean mHasFakeAvatar;
    private View mRealAvatar;
    /**
     * 临界top，头像的top不能超过此值，否则头像跟随此视图一起滑动
     */
    private Integer mCriticalTop;
    private Integer mCriticalBottom;
    @NonNull
    private final LinearLayoutManager mLayoutManager;

    /**
     * 构造{@link AvatarScrollHandler#mFakeAvatar}
     *
     * @param origin 原头像视图
     */
    private void buildFakeAvatar(@NotNull ImageView origin) {
        if (mFakeAvatar == null) {
            mFakeAvatar = new CircleImageView(origin.getContext());
            ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(origin.getLayoutParams());
            mFakeAvatar.setLayoutParams(lp);
            mFakeAvatar.setFocusable(true);
            mFakeAvatar.setClickable(true);
            mFakeAvatar.setOnClickListener(v -> MasterToast.shortToast(String.valueOf(mHasFakeAvatar)));
        }
        mFakeAvatar.setImageDrawable(origin.getDrawable());
    }

    public AvatarScrollHandler(@NonNull LinearLayoutManager layoutManager) {
        this.mLayoutManager = layoutManager;
    }

    public void reset() {
        mHasFakeAvatar = false;
        mCriticalTop = null;
        mCriticalBottom = null;
        notifyAvatarVisible();
    }

    @Override
    public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
        int curIndex = mLayoutManager.findFirstVisibleItemPosition();
        if (curIndex < 0) {
            return;
        }
        //如果临界值存在，更新临界值
        if (mCriticalBottom != null) {
            mCriticalBottom -= dy;
        }
        if (mCriticalTop != null) {
            mCriticalTop -= dy;
        }
        //向下滑（前进）
        if (dy < 0) {
            //当某个新的消息正好位于最底部时并且此时Overlay中没有假头像
            if (mPreviousDownScrollIndex != curIndex && !mHasFakeAvatar) {
                mPreviousDownScrollIndex = curIndex;
                //获取此消息的视图
                View cur = mLayoutManager.findViewByPosition(curIndex);
                ImageView curAvatar = cur.findViewById(R.id.iv_avatar);
                //如果不可见，可能是列表滑动过快，导致跳过了上一个
                //我们直接忽略就行了
                //TODO:不忽略
                if (curAvatar == null || curAvatar.getVisibility() == View.INVISIBLE) {
                    return;
                }
                View msg = cur.findViewById(R.id.msg_container);
                if (msg == null) {
                    return;
                }
                //获取该消息的组分割线
                View divider = cur.findViewById(R.id.divider_center);
                if (divider == null) {
                    return;
                }
                //如果这条消息和最前一条消息是同一组的，或者当前消息框高度大于头像高度，
                if (divider.getVisibility() == View.VISIBLE || msg.getHeight() > curAvatar.getHeight()) {
                    //那么，我们向overlay原头像的位置添加一个假头像
                    buildFakeAvatar(curAvatar);
                    mBaseMargin = UiUtils.getBottomMargin(cur) + UiUtils.getBottomMargin(msg);
                    mCriticalBottom = recyclerView.getHeight() - mBaseMargin;
                    mFakeAvatar.layout(curAvatar.getLeft(), mCriticalBottom - curAvatar.getHeight(), curAvatar.getRight(), mCriticalBottom);
                    recyclerView.getOverlay().add(mFakeAvatar);
                    mHasFakeAvatar = true;
                    //并隐藏真实的头像
                    curAvatar.setVisibility(View.INVISIBLE);
                    mRealAvatar = curAvatar;
                    notifyAvatarInvisible(curIndex);
                }
            } else if (mHasFakeAvatar) {
                mPreviousDownScrollIndex = curIndex;
                //当滚动时并且Overlay中有假头像
                //获取该消息的前一条消息的视图
                if (mCriticalTop == null) {
                    View pre = mLayoutManager.findViewByPosition(curIndex + 1);
                    if (pre == null) {
                        return;
                    }
                    View preAvatar = pre.findViewById(R.id.iv_avatar);
                    //如果前一条消息和当前消息不是一组的
                    if (preAvatar == null || preAvatar.getVisibility() == View.VISIBLE) {
                        //获取此消息的视图
                        View cur = mLayoutManager.findViewByPosition(curIndex);
                        int[] msgPos = new int[2];
                        UiUtils.getLocationCoordinateTo(cur, recyclerView, msgPos);
                        mCriticalTop = msgPos[1];
                    }
                } else {
                    //如果消息框的上边缘碰到假头像的上边缘
                    if (mCriticalTop > mFakeAvatar.getTop()) {
                        mFakeAvatar.offsetTopAndBottom(-dy);
                    }
                    //如果我们的假头像不再可见，重置逻辑
                    if (mFakeAvatar.getTop() >= recyclerView.getHeight()) {
                        recyclerView.getOverlay().remove(mFakeAvatar);
                        mPreviousUpScrollIndex = -1;
                        if (mRealAvatar != null) {
                            mRealAvatar.setVisibility(View.VISIBLE);
                        }
                        reset();
                    }
                }
            }
        } else {
            //后退
            //如果有假头像
            if (mHasFakeAvatar) {
                mPreviousDownScrollIndex = curIndex;
                if (mCriticalBottom == null) {
                    View cur = mLayoutManager.findViewByPosition(curIndex);
                    View curAvatar = cur.findViewById(R.id.iv_avatar);
                    //如果前一条消息和当前消息不是一组的
                    if (curAvatar == null || curAvatar.getVisibility() == View.VISIBLE) {
                        //获取此消息的视图
                        int[] msgPos = new int[2];
                        UiUtils.getLocationCoordinateTo(cur, recyclerView, msgPos);
                        mCriticalBottom = msgPos[1] + cur.getHeight();
                        mRealAvatar = curAvatar;
                        //并隐藏真实的头像
                        notifyAvatarInvisible(curIndex);
                        if (curAvatar != null) {
                            curAvatar.setVisibility(View.INVISIBLE);
                        }
                    }
                }
                //且假头像已经触顶
                if (mFakeAvatar.getBottom() > recyclerView.getHeight() - mBaseMargin) {
                    //此时列表后退时假头像跟着后退
                    //有时候dy可能过大（滑动过快），控制住
                    mFakeAvatar.offsetTopAndBottom(Math.max(-dy, recyclerView.getHeight() - mBaseMargin - mFakeAvatar.getBottom()));
                }
                //假如触底
                if (mCriticalBottom != null && mFakeAvatar.getBottom() >= mCriticalBottom) {
                    //重置
                    mFakeAvatar.offsetTopAndBottom(-dy);
                    recyclerView.getOverlay().remove(mFakeAvatar);
                    mPreviousDownScrollIndex = -1;
                   /* View critical = mLayoutManager.findViewByPosition(mAvatarHiddenIndex);
                    if (critical != null) {
                        View criticalAvatar = critical.findViewById(R.id.iv_avatar);
                        if (criticalAvatar != null) {
                            criticalAvatar.setVisibility(View.VISIBLE);
                        }
                    }*/
                    if (mRealAvatar != null) {
                        mRealAvatar.setVisibility(View.VISIBLE);
                    }
                    reset();
                }
            }
            //如果没有假头像，并且某个新的消息滑到最底部
            else if (mPreviousUpScrollIndex != curIndex && !mHasFakeAvatar) {
                mPreviousUpScrollIndex = curIndex;
                //获取此消息的视图
                ViewGroup cur = (ViewGroup) mLayoutManager.findViewByPosition(curIndex);
                View divider = cur.findViewById(R.id.divider_center);
                if (divider == null) {
                    return;
                }
                ImageView curAvatar = cur.findViewById(R.id.iv_avatar);
                if (curAvatar == null) {
                    return;
                }
                View msg = cur.findViewById(R.id.msg_container);
                if (msg == null) {
                    return;
                }
                if (divider.getVisibility() == View.VISIBLE || msg.getHeight() > curAvatar.getHeight()) {
                    buildFakeAvatar(curAvatar);
                    int[] curPos = new int[2];
                    UiUtils.getLocationCoordinateTo(cur, recyclerView, curPos);
                    mCriticalTop = curPos[1];
                    mFakeAvatar.layout(curAvatar.getLeft(), mCriticalTop, curAvatar.getRight(), mCriticalTop + curAvatar.getHeight());
                    recyclerView.getOverlay().add(mFakeAvatar);
                    mHasFakeAvatar = true;
                }
            }
        }
        //如果滑到了底部，重置
        //最后检查，否则mPreviousIndex会被覆盖
        if (!recyclerView.canScrollVertically(1)) {
            mPreviousDownScrollIndex = -1;
        }
    }


    public abstract void notifyAvatarInvisible(int position);

    public abstract void notifyAvatarVisible();

}
