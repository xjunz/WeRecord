package xjunz.tool.wechat.ui.main.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import xjunz.tool.wechat.R;
import xjunz.tool.wechat.data.viewmodel.PageViewModel;
import xjunz.tool.wechat.databinding.FragmentFilterBinding;

public class FilterFragment extends Fragment {
    private PageViewModel mModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mModel = new ViewModelProvider(requireActivity(), new ViewModelProvider.NewInstanceFactory()).get(PageViewModel.class);
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        FragmentFilterBinding binding = FragmentFilterBinding.inflate(inflater);
        binding.spOrderBy.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, getResources().getStringArray(R.array.orders)));
        binding.setModel(mModel);
        return binding.getRoot();
    }
}
