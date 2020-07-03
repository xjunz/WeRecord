package xjunz.tool.wechat.data.databinding;

import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.databinding.BindingAdapter;
import androidx.databinding.BindingMethod;
import androidx.databinding.BindingMethods;
import androidx.databinding.InverseBindingAdapter;
import androidx.databinding.InverseBindingListener;
import androidx.databinding.InverseBindingMethod;
import androidx.databinding.InverseBindingMethods;
import androidx.viewpager2.widget.ViewPager2;

import xjunz.tool.wechat.R;
import xjunz.tool.wechat.ui.customview.BottomBar;
import xjunz.tool.wechat.util.UiUtils;

@BindingMethods({
        @BindingMethod(type = BottomBar.class, attribute = "android:onItemSelect", method = "setOnItemSelectListener"),
        @BindingMethod(type = BottomBar.class, attribute = "android:selection", method = "setSelection"),
        @BindingMethod(type = ViewPager2.class, attribute = "android:currentItem", method = "setCurrentItem"),
})
@InverseBindingMethods({
        @InverseBindingMethod(type = ViewPager2.class, attribute = "android:currentItem"),
        @InverseBindingMethod(type = BottomBar.class, attribute = "android:selection"),
})

public class MainActivityBindingAdapter {
    @BindingAdapter(value = "android:title")
    public static void setTitle(TextView textView, CharSequence title) {
        if (!textView.getText().equals(title)) {
            UiUtils.fadeSwitchText(textView, title);
        }
    }

    @BindingAdapter(value = {"android:currentItemAttrChanged"})
    public static void setCurrentItemChangeListener(ViewPager2 pager, InverseBindingListener currentItemAttr) {
        pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                currentItemAttr.onChange();
            }
        });
    }

    @BindingAdapter(value = {"android:onItemSelect", "android:selectionAttrChanged"}, requireAll = false)
    public static void setSelectionChangeListener(BottomBar bottomBar, BottomBar.OnItemSelectListener listener, InverseBindingListener selectionAttr) {
        bottomBar.setOnItemSelectListener((position, caption, unchanged) -> {
            if (listener != null) {
                listener.onItemSelect(position, caption, unchanged);
            }
            if (!unchanged) {
                selectionAttr.onChange();
            }
        });
    }

    @BindingAdapter(value = {"android:searchMode"})
    public static void setSearchMode(ImageButton imageButton, boolean searchMode) {
        UiUtils.fadeSwitchImage(imageButton, searchMode ? R.drawable.ic_close_24dp : R.drawable.ic_search_24dp);
    }

    @InverseBindingAdapter(attribute = "android:searchMode", event = "android:searchModeAttrChanged")
    public static boolean isSearchMode(ImageButton imageButton) {
        return imageButton.getTag() != null && (boolean) imageButton.getTag();
    }

    @BindingAdapter(value = {"android:onClick", "android:searchModeAttrChanged"}, requireAll = false)
    public static void setSearchModeChangeListener(ImageButton imageButton, View.OnClickListener listener, InverseBindingListener searchModeAttr) {
        imageButton.setOnClickListener(v -> {
            imageButton.setTag(!isSearchMode(imageButton));
            searchModeAttr.onChange();
            if (listener != null) {
                listener.onClick(v);
            }
        });
    }
}
