package xjunz.tool.wechat.data.databinding;

import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.annotation.Nullable;
import androidx.databinding.BindingAdapter;
import androidx.databinding.InverseBindingAdapter;
import androidx.databinding.InverseBindingListener;

import java.util.Arrays;
import java.util.List;

import xjunz.tool.wechat.data.model.SortBy;

/**
 * {@link xjunz.tool.wechat.data.model.FilterConfig}实体类相关的自定义{@link BindingAdapter}声明类
 */
//TODO: 将'android:'改为'filter:'
public class FilterFragmentBindingAdapter {

    /**
     * 为自定义属性{@code android:sortBy}设置自定义{@link BindingAdapter}
     * 实现通过改变{@link xjunz.tool.wechat.data.model.FilterConfig#sortBy}改变{@link Spinner}的{@code selection}
     *
     * @param spinner 当前{@link Spinner}
     * @param what    当前{@link Spinner}的{@code android:sortBy}属性设置的值
     */
    @BindingAdapter(value = {"android:sortBy"})
    public static void setSortBy(Spinner spinner, SortBy what) {
        spinner.setSelection(Arrays.asList(SortBy.values()).indexOf(what), true);
    }


    /**
     * 为自定义属性{@code android:sortBy}设置自定义{@link InverseBindingAdapter}
     * 实现通过改变{@link Spinner}的{@code selection}改变{@link xjunz.tool.wechat.data.model.FilterConfig#sortBy}
     *
     * @param spinner 当前{@link Spinner}
     * @return 根据当前{@link Spinner}的{@code selection}获取到的{@link SortBy}
     */
    @InverseBindingAdapter(attribute = "android:sortBy", event = "android:sortByAttrChanged")
    public static SortBy getSortBy(Spinner spinner) {
        return SortBy.values()[spinner.getSelectedItemPosition()];
    }


    /**
     * 实现{@code android:sortByAttrChanged}事件，这个事件用于判断何时{@link Spinner}的{@code selection}改变，
     * 从而调用{@link InverseBindingListener#onChange()}方法通知数据更新。
     * <b>注意：</b>{@code android:sortByAttrChanged}属性不可用于XML中，此方法由框架调用
     *
     * @param spinner      当前{@link Spinner}
     * @param itemSelected 用户设置的{@link android.widget.AdapterView.OnItemSelectedListener}，此参数可为空
     * @param sortByAttr   系统传入的{@link InverseBindingListener}
     */
    @BindingAdapter(value = {"android:onItemSelected", "android:sortByAttrChanged"}, requireAll = false)
    public static void setSortByChangeListener(Spinner spinner, @Nullable final AdapterView.OnItemSelectedListener itemSelected, final InverseBindingListener sortByAttr) {
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (itemSelected != null) {
                    itemSelected.onItemSelected(parent, view, position, id);
                }
                if (sortByAttr != null) {
                    sortByAttr.onChange();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                if (itemSelected != null) {
                    itemSelected.onNothingSelected(parent);
                }
            }
        });
    }

    /**
     * 为{@code android:entries}设置自定义{@link BindingAdapter}
     * 允许传入{@link androidx.databinding.ObservableList<String>}，并自动设置适配器，从而实现数据绑定
     *
     * @param spinner 当前{@link Spinner}
     * @param entries 当前{@link Spinner}的{@code android:entries}属性设置的值
     */
    @BindingAdapter(value = {"android:entries"})
    public static void setEntries(Spinner spinner, @Nullable List<String> entries) {
        if (entries != null) {
            spinner.setAdapter(new ArrayAdapter<>(spinner.getContext(), android.R.layout.simple_spinner_dropdown_item, entries));
        }
    }


}