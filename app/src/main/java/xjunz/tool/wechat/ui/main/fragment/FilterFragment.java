package xjunz.tool.wechat.ui.main.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import xjunz.tool.wechat.R;
import xjunz.tool.wechat.data.viewmodel.PageViewModel;
import xjunz.tool.wechat.databinding.FragmentFilterBinding;

public class FilterFragment extends Fragment implements PageViewModel.EventHandler {
    private PageViewModel mModel;
    private int[] mSelection;
    private Spinner[] mSpinnerList;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mModel = new ViewModelProvider(requireActivity(), new ViewModelProvider.NewInstanceFactory()).get(PageViewModel.class);
        mModel.addEventHandler(this);
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        FragmentFilterBinding binding = FragmentFilterBinding.inflate(inflater);
        binding.spOrderBy.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, getResources().getStringArray(R.array.orders)));
        binding.setModel(mModel);
        mSpinnerList = new Spinner[]{binding.spName, binding.spType, binding.spTime, binding.spMsgCount, binding.spSortBy, binding.spOrderBy};
        mSelection = new int[mSpinnerList.length];
        return binding.getRoot();
    }

    @Override
    public void onConfirmFilter() {

    }

    @Override
    public void onResetFilter() {

    }

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
     * @see FilterFragment#onCancelFilter()
     */
    @Override
    public void onPrepareFilter() {
        for (int i = 0; i < mSpinnerList.length; i++) {
            mSelection[i] = mSpinnerList[i].getSelectedItemPosition();
        }
    }
}
