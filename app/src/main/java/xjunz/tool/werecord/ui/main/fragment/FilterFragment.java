/*
 * Copyright (c) 2021 xjunz. 保留所有权利
 */

package xjunz.tool.werecord.ui.main.fragment;

import android.animation.ObjectAnimator;
import android.graphics.Path;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.ListPopupWindow;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import java.lang.reflect.Field;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import io.reactivex.Flowable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import xjunz.tool.werecord.R;
import xjunz.tool.werecord.data.viewmodel.PageViewModel;
import xjunz.tool.werecord.databinding.FragmentFilterBinding;
import xjunz.tool.werecord.util.RxJavaUtils;

public class FilterFragment extends Fragment {
    private PageViewModel mModel;
    private int[] mSelection;
    private Spinner[] mSpinnerList;
    private FragmentFilterBinding mBinding;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mModel = new ViewModelProvider(requireActivity(), new ViewModelProvider.NewInstanceFactory()).get(PageViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = FragmentFilterBinding.inflate(inflater);
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mBinding.spOrderBy.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, getResources().getStringArray(R.array.orders)));
        mBinding.setModel(mModel);
        mSpinnerList = new Spinner[]{mBinding.spName, mBinding.spType, mBinding.spTime, mBinding.spMsgCount, mBinding.spSortBy, mBinding.spOrderBy};
        mSelection = new int[mSpinnerList.length];
        mModel.addEventHandler(mEventHandler);
    }

    private final PageViewModel.EventHandler mEventHandler = new PageViewModel.EventHandler() {
        /**
         * 筛选取消时，恢复{@link Spinner}的{@code selection}
         */
        @Override
        public void onCancelFilter() {
            for (int i = 0; i < mSelection.length; i++) {
                mSpinnerList[i].setSelection(mSelection[i]);
            }
        }

        /**
         * 当用户打开筛选面板准备筛选时，调用此方法，备份当前配置。
         * 因为用户可能改变了筛选配置但是没有点“确认”，就会导致
         * 实际数据与配置不一致。当用户取消筛选时，恢复备份的配置。
         *
         */
        @Override
        public void onPrepareFilter() {
            for (int i = 0; i < mSpinnerList.length; i++) {
                mSelection[i] = mSpinnerList[i].getSelectedItemPosition();
            }
            //展示type
            if (mModel.needDemonstrationAllContactTypes()) {
                mModel.requestDemonstrateAllContactTypes(false);
                mBinding.spType.performClick();
                try {
                    Class<?> clazz = mBinding.spType.getClass();
                    Field fieldPopup = clazz.getDeclaredField("mPopup");
                    fieldPopup.setAccessible(true);
                    Path path = new Path();
                    path.moveTo(1, 1);
                    path.lineTo(1.2f, 1.2f);
                    path.lineTo(1, 1);
                    ListPopupWindow spinnerPopup = Objects.requireNonNull((ListPopupWindow) fieldPopup.get(mBinding.spType));
                    ListView listView = spinnerPopup.getListView();
                    Flowable.intervalRange(0, mBinding.spType.getCount(), 200, 61, TimeUnit.MILLISECONDS)
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(new RxJavaUtils.FlowableSubscriberAdapter<Long>() {
                                @Override
                                public void onNext(Long aLong) {
                                    super.onNext(aLong);
                                    if (spinnerPopup.isShowing()) {
                                        View view = listView.getChildAt(aLong.intValue());
                                        ObjectAnimator animator = ObjectAnimator.ofFloat(view, View.SCALE_X, View.SCALE_Y, path);
                                        animator.setDuration(100);
                                        animator.start();
                                    } else {
                                        mSubscription.cancel();
                                    }
                                }
                            });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    };
}
