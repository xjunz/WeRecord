package xjunz.tool.wechat.ui.activity.main.model;

import xjunz.tool.wechat.App;
import xjunz.tool.wechat.R;

public enum SortBy {
    //改变这几个值的顺序时，注意同时修改FilterFragment中mSpList内元素顺序
    NAME(R.string.name), TIMESTAMP(R.string.last_msg_time), MSG_COUNT(R.string.msg_count);
    String caption;

    SortBy(int captionRes) {
        this.caption = App.getStringOf(captionRes);
    }
}