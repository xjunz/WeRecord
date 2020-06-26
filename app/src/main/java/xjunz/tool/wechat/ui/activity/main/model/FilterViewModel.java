package xjunz.tool.wechat.ui.activity.main.model;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import static xjunz.tool.wechat.ui.activity.main.model.FilterConfiguration.VICTIM_CONTACT;
import static xjunz.tool.wechat.ui.activity.main.model.FilterConfiguration.VICTIM_TALKER;

public class FilterViewModel extends ViewModel {
    private MutableLiveData<FilterConfiguration> mTalkerConfig = new MutableLiveData<>();
    private MutableLiveData<FilterConfiguration> mContactConfig = new MutableLiveData<>();
    private MutableLiveData<FilterConfiguration> mCurrentConfig = new MutableLiveData<>();


    public void config(FilterConfiguration configuration, int payload) {
        configuration.payload = payload;
        if (configuration.filterVictim == VICTIM_CONTACT) {
            mContactConfig.setValue(configuration);
            mCurrentConfig.setValue(configuration);
        } else if (configuration.filterVictim == VICTIM_TALKER) {
            mTalkerConfig.setValue(configuration);
            mCurrentConfig.setValue(configuration);
        } else {
            throw new IllegalArgumentException("Current config has no victim! ");
        }
    }


    public LiveData<FilterConfiguration> getTalkerConfiguration() {
        return mTalkerConfig;
    }

    public LiveData<FilterConfiguration> getContactConfiguration() {
        return mContactConfig;
    }


    public LiveData<FilterConfiguration> getCurrentConfig() {
        return mCurrentConfig;
    }


}
