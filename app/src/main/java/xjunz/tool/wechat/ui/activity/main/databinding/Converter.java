package xjunz.tool.wechat.ui.activity.main.databinding;

import androidx.databinding.BindingConversion;

public class Converter {
    @BindingConversion()
    public static int ascendingToSelection(boolean ascending) {
        return ascending ? 0 : 1;
    }
}
