package xjunz.tool.wechat.ui.activity.main.model;

import android.util.ArrayMap;

import java.util.List;

public class FilterConfiguration {
    public static final int PAYLOAD_COUNT_CHANGED = 0x10010;
    public static final int PAYLOAD_RESET_FILTER = 0x11000;
    public static final int PAYLOAD_CONFIRM_FILTER = 0x10100;
    public static final int PAYLOAD_INIT = -3;
    public static final int VICTIM_TALKER = 1;
    public static final int VICTIM_CONTACT = -1;
    public int filterVictim;
    protected int payload;
    public SortBy sortBy;
    public int totalCount, filteredCount;
    public boolean ascending;
    public ArrayMap<SortBy, Integer> selectionMap;
    public int categorySelection, sortBySelection;
    public ArrayMap<SortBy, List<String>> descriptionListMap;

    public int getPayload() {
        return payload;
    }

    public static FilterConfiguration getDefault() {
        FilterConfiguration defaultConfig = new FilterConfiguration();
        defaultConfig.descriptionListMap = new ArrayMap<>();
        defaultConfig.selectionMap = new ArrayMap<>();
        defaultConfig.ascending = true;
        defaultConfig.sortBy = SortBy.NAME;
        return defaultConfig;
    }

    public List<String> getCurrentDescriptors() {
        return descriptionListMap.get(sortBy);
    }


}