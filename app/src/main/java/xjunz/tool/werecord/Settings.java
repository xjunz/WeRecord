/*
 * Copyright (c) 2021 xjunz. 保留所有权利
 */
package xjunz.tool.werecord;

import android.content.SharedPreferences;

import androidx.databinding.BaseObservable;
import androidx.databinding.Bindable;

import java.util.Set;

/**
 * @author xjunz 2021/2/14 23:11
 */
public class Settings {
    private final SharedPreferences mSP;
    public SwitchSetting verifyDeviceCredential = new SwitchSetting("verify_device_credential", true);
    public SwitchSetting editMode = new SwitchSetting("edit_mode", false);
    public StringSetting lastUsedUin = new StringSetting("last_used_uin", null);

    Settings(SharedPreferences sp) {
        this.mSP = sp;
    }

    public boolean isVerifyDeviceCredentialEnabled() {
        return verifyDeviceCredential.getValue();
    }

    public boolean isEditModeEnabled() {
        return editMode.getValue();
    }

    public static abstract class Setting<T> extends BaseObservable {
        String key;
        T defValue;


        Setting(String key, T defValue) {
            this.key = key;
            this.defValue = defValue;
        }

        public void restoreDefault() {
            setValue(defValue);
        }

        @Bindable
        abstract T getValue();


        abstract void persistValue(T value);

        public void setValue(T value) {
            persistValue(value);
            notifyPropertyChanged(BR.value);
        }
    }

    public class IntegerSetting extends Setting<Integer> {

        IntegerSetting(String key, Integer defValue) {
            super(key, defValue);
        }

        @Bindable
        public Integer getValue() {
            return mSP.getInt(key, defValue);
        }

        @Override
        public void setValue(Integer value) {
            super.setValue(value);
        }

        @Override
        void persistValue(Integer value) {
            mSP.edit().putInt(key, value).apply();
        }
    }

    public class StringSetting extends Setting<String> {

        StringSetting(String key, String defValue) {
            super(key, defValue);
        }

        @Bindable
        public String getValue() {
            return mSP.getString(key, defValue);
        }

        @Override
        void persistValue(String value) {
            mSP.edit().putString(key, value).apply();
        }

    }

    public class SwitchSetting extends Setting<Boolean> {

        SwitchSetting(String key, Boolean defValue) {
            super(key, defValue);
        }

        @Bindable
        public Boolean getValue() {
            return mSP.getBoolean(key, defValue);
        }

        @Override
        void persistValue(Boolean value) {
            mSP.edit().putBoolean(key, value).apply();
        }

        @Override
        public void setValue(Boolean value) {
            super.setValue(value);
        }

        public void toggleValue() {
            setValue(!getValue());
        }
    }

    public class StringSetSetting extends Setting<Set<String>> {
        StringSetSetting(String key, Set<String> defValue) {
            super(key, defValue);
        }

        @Override
        public Set<String> getValue() {
            return mSP.getStringSet(key, defValue);
        }

        @Override
        void persistValue(Set<String> value) {
            mSP.edit().putStringSet(key, value).apply();
        }
    }
}
