package xjunz.tool.wechat.data.viewmodel;

import java.util.ArrayList;
import java.util.List;

import xjunz.tool.wechat.App;
import xjunz.tool.wechat.R;

public enum SortBy {
    //改变这几个值的顺序时，注意同时修改FilterFragment中mSpList内元素顺序
    NAME(R.string.by_name), TIMESTAMP(R.string.by_last_msg_time), MSG_COUNT(R.string.by_msg_count);
    public String caption;

    SortBy(int captionRes) {
        this.caption = App.getStringOf(captionRes);
    }

    public static List<String> getCaptionList() {
        List<String> captions = new ArrayList<>();
        for (SortBy by : SortBy.values()) {
            captions.add(by.caption);
        }
        return captions;
    }
}