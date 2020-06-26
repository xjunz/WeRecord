package xjunz.tool.wechat.ui.activity.main;

import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import java.util.Objects;

import xjunz.tool.wechat.R;
import xjunz.tool.wechat.databinding.FragmentFilterBinding;
import xjunz.tool.wechat.ui.activity.main.model.FilterConfiguration;
import xjunz.tool.wechat.ui.activity.main.model.FilterViewModel;
import xjunz.tool.wechat.ui.activity.main.model.SortBy;
import xjunz.tool.wechat.util.UiUtils;

public class FilterFragment extends Fragment implements AdapterView.OnItemSelectedListener, View.OnClickListener, Observer<FilterConfiguration> {
    private FilterConfiguration mCurConfig;
    private Spinner[] mSpList;
    private FilterViewModel mModel;
    private Button mBtnConfirm, mBtnReset;
    private FragmentFilterBinding mBinding;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mModel = new ViewModelProvider(requireActivity(), new ViewModelProvider.NewInstanceFactory()).get(FilterViewModel.class);
        mModel.getCurrentConfig().observe(requireActivity(), this);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = FragmentFilterBinding.inflate(inflater);
        ViewGroup root = mBinding.getRoot();
        initViews();
        return root;
    }


    private void initViews() {
        mBinding.spSortBy.setOnItemSelectedListener(this);
        mBinding.spOrderBy.setOnItemSelectedListener(this);
        mBinding.spCategory.setOnItemSelectedListener(this);
        mSpList = new Spinner[]{mBinding.spName, mBinding.spTime, mBinding.spMsgCount};
        for (int i = 0; i < mSpList.length; i++) {
            Spinner spinner = mSpList[i];
            spinner.setTag(SortBy.values()[i]);
            spinner.setOnItemSelectedListener(this);
        }
        mBinding.spOrderBy.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, getResources().getStringArray(R.array.orders)));
        mBinding.btnConfirmFilter.setOnClickListener(this);
        mBinding.btnResetFilter.setOnClickListener(this);
    }

    @Override
    public void onChanged(FilterConfiguration filterConfig) {
        mCurConfig = filterConfig;
        switch (filterConfig.getPayload()) {
            case FilterConfiguration.PAYLOAD_RESET_FILTER:
                mCurConfig.ascending = true;
                mCurConfig.categorySelection = 0;
                for (SortBy by : SortBy.values()) {
                    mCurConfig.selectionMap.put(by, 0);
                }
                if (mCurConfig.filterVictim == FilterConfiguration.VICTIM_TALKER) {
                    mCurConfig.sortBy = SortBy.TIMESTAMP;
                    mCurConfig.sortBySelection = 1;
                } else {
                    mCurConfig.sortBy = SortBy.NAME;
                    mCurConfig.sortBySelection = 0;
                }
                mModel.config(mCurConfig, FilterConfiguration.PAYLOAD_CONFIRM_FILTER);
                mModel.config(mCurConfig, FilterConfiguration.PAYLOAD_INIT);
                break;
            case FilterConfiguration.PAYLOAD_COUNT_CHANGED:
                mBinding.tvStats.setText(Html.fromHtml(getString(R.string.format_count_stats, filterConfig.totalCount, filterConfig.filteredCount)));
                break;
            case FilterConfiguration.PAYLOAD_INIT:
                if (mCurConfig.filterVictim == FilterConfiguration.VICTIM_CONTACT) {
                    UiUtils.disable(mBinding.spMsgCount, mBinding.spTime, mBinding.tvCaptionTime, mBinding.tvCaptionMsgCount);
                    mBinding.spName.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, Objects.requireNonNull(mCurConfig.descriptionListMap.get(SortBy.NAME))));
                    mBinding.spCategory.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, getResources().getStringArray(R.array.category_contact)));
                    mBinding.spSortBy.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, getResources().getStringArray(R.array.contact_sort)));
                    Integer selection = mCurConfig.selectionMap.get(SortBy.NAME);
                    if (selection != null) {
                        mBinding.spName.setSelection(selection);
                    }
                } else {
                    UiUtils.enable(mBinding.spMsgCount, mBinding.spTime, mBinding.tvCaptionTime, mBinding.tvCaptionMsgCount);
                    mBinding.spCategory.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, getResources().getStringArray(R.array.category_talker)));
                    mBinding.spSortBy.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, getResources().getStringArray(R.array.talker_sorts)));
                    for (Spinner spinner : mSpList) {
                        SortBy tag = (SortBy) spinner.getTag();
                        spinner.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, Objects.requireNonNull(mCurConfig.descriptionListMap.get(tag))));
                        Integer selection = mCurConfig.selectionMap.get(tag);
                        if (selection != null) {
                            spinner.setSelection(selection);
                        }
                    }
                }
                mBinding.spCategory.setSelection(mCurConfig.categorySelection);
                mBinding.spCategory.setSelection(mCurConfig.sortBySelection);
                mBinding.spOrderBy.setSelection(mCurConfig.ascending ? 0 : 1);
                break;
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (mCurConfig == null) {
            return;
        }
        switch (parent.getId()) {
            case R.id.sp_category:
                mCurConfig.categorySelection = position;
                break;
            case R.id.sp_order_by:
                mCurConfig.ascending = position == 0;
                break;
            case R.id.sp_sort_by:
                mCurConfig.sortBy = SortBy.values()[position];
                mCurConfig.sortBySelection = position;
                break;
            default:
                mCurConfig.selectionMap.put((SortBy) parent.getTag(), position);
                break;
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_confirm_filter:
                mModel.config(mCurConfig, FilterConfiguration.PAYLOAD_CONFIRM_FILTER);
                //mPanel.closePanel();
                break;
            case R.id.btn_reset_filter:
                mModel.config(mCurConfig, FilterConfiguration.PAYLOAD_RESET_FILTER);
                break;
        }
    }


}
