/*
 * Copyright (c) 2021 xjunz. 保留所有权利
 */
package xjunz.tool.werecord.impl.model.export;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.annotation.WorkerThread;
import androidx.arch.core.util.Function;
import androidx.databinding.BaseObservable;
import androidx.databinding.Bindable;
import androidx.databinding.Observable;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import io.reactivex.Completable;
import xjunz.tool.werecord.App;
import xjunz.tool.werecord.BR;
import xjunz.tool.werecord.R;
import xjunz.tool.werecord.impl.model.account.Account;
import xjunz.tool.werecord.util.BiPredicate;
import xjunz.tool.werecord.util.IoUtils;
import xjunz.tool.werecord.util.Utils;

/**
 * @author xjunz 2021/1/26 23:27
 */
public abstract class Exporter {
    private final Config<Format> mFormatConfig;
    private final Config<String> mPasswordConfig;
    private final List<Config<?>> mConfigs = new ArrayList<>();
    private static String sDbExportReadme;

    public Exporter() {
        initCustomConfigs(mConfigs);
        mFormatConfig = new EnumConfig<Format>(R.string.export_type).setValueEnum(getSupportFormats()).setPreview(Format::getNameAndFileSuffix);
        mPasswordConfig = new PasswordConfig(R.string.db_password).setHelpTextRes(R.string.help_sqlite_password).setVisible(false);
        mFormatConfig.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
            @Override
            public void onPropertyChanged(Observable sender, int propertyId) {
                if (propertyId == BR.value) {
                    mPasswordConfig.setVisible(getExportFormat() == Format.CIPHER_DB);
                }
            }
        });
        mConfigs.add(mPasswordConfig);
        mConfigs.add(mFormatConfig);
    }

    public Format getExportFormat() {
        return mFormatConfig.getValue();
    }

    public String getExportDbPassword() {
        return mPasswordConfig.getValue();
    }

    @NotNull
    protected String getCurrentDate() {
        return Utils.formatDateIgnoreMills(System.currentTimeMillis());
    }

    public Config<Format> getFormatConfig() {
        return mFormatConfig;
    }

    public Config<String> getPasswordConfig() {
        return mPasswordConfig;
    }

    public abstract List<? extends Account> getSourceList();

    public abstract Format[] getSupportFormats();

    protected final static OnProgressListener EMPTY_PROGRESS_LISTENER = new OnProgressListener() {
        @Override
        public void onGetTotalProgress(int total) {

        }

        @Override
        public void onProgressUpdate(int current) {

        }

    };

    @NotNull
    public String getDbExportReadme(@Nullable String appendage) throws IOException {
        if (appendage == null) {
            appendage = "";
        }
        if (sDbExportReadme == null) {
            sDbExportReadme = IoUtils.readAssetAsString("template_db_export_readme.html");
        }
        String password = TextUtils.isEmpty(getExportDbPassword()) ? String.format("<i>%s</i>", App.getStringOf(R.string.none)) : getExportDbPassword();
        return String.format(sDbExportReadme, getExportableName(), Utils.formatDate(System.currentTimeMillis()), password, appendage);
    }

    /**
     * 返回一个异步执行导出任务，主线程观察的{@link Completable}。
     */
    @WorkerThread
    public Completable exportToAsync(@NonNull File outputFile, @Nullable OnProgressListener listener) {
        return exportAsToAsync(getExportFormat(), outputFile, listener == null ? EMPTY_PROGRESS_LISTENER : listener);
    }

    @WorkerThread
    protected abstract Completable exportAsToAsync(Format format, @NonNull File outputFile, @NonNull OnProgressListener listener);

    public abstract String getExportFileName();

    public abstract String getExportableName();

    public enum Format {
        TXT(xjunz.tool.werecord.R.string.export_format_plain_text, ".txt"),
        HTML(xjunz.tool.werecord.R.string.export_format_html, ".html"),
        CIPHER_DB(R.string.export_format_db, ".db", ".zip"),
        TABLE(R.string.export_format_table, ".html");
        private final String name;
        /**
         * 最终导出的文件格式，如{@link Format#CIPHER_DB}导出的最终格式为.zip，其中
         * 包括一个readme文件和导出的db文件
         */
        private final String exportSuffix;
        /**
         * 导出主体文件的文件格式, 如{@link Format#CIPHER_DB}导出的主体文件格式为.db，但其最终
         * 导出的文件格式为.zip
         */
        private final String fileSuffix;

        Format(@StringRes int nameRes, String fileSuffix, String exportSuffix) {
            this.name = App.getStringOf(nameRes);
            this.fileSuffix = fileSuffix;
            this.exportSuffix = exportSuffix;
        }

        Format(@StringRes int nameRes, String fileSuffix) {
            this.name = App.getStringOf(nameRes);
            this.fileSuffix = fileSuffix;
            this.exportSuffix = fileSuffix;
        }

        public String getName() {
            return name;
        }

        @NotNull
        public String getNameAndFileSuffix() {
            return String.format("%s(%s)", name, fileSuffix);
        }

        public String getFileSuffix() {
            return fileSuffix;
        }

        public String getExportSuffix() {
            return exportSuffix;
        }
    }

    public List<Config<?>> getConfigs() {
        return mConfigs;
    }

    protected abstract void initCustomConfigs(@NonNull List<Config<?>> configList);

    public interface OnProgressListener {
        void onGetTotalProgress(int total);

        void onProgressUpdate(int current);

    }

    public abstract static class Config<V> extends BaseObservable {
        private final String mTitle;
        protected Function<V, CharSequence> mPreview;
        private BiPredicate<Exportable, V> mFilter;
        private V mValue;
        private boolean mEnabled = true;

        @StringRes
        private int mHelpTextRes = -1;

        private boolean mVisible = true;

        public boolean isFilter() {
            return mFilter != null;
        }

        public Config<V> setDefValue(V value) {
            mValue = value;
            return this;
        }

        public Config<V> setHelpTextRes(@StringRes int res) {
            mHelpTextRes = res;
            return this;
        }

        public int getHelpTextRes() {
            return mHelpTextRes;
        }

        @Bindable
        public boolean isVisible() {
            return mVisible;
        }

        public Config<V> setVisible(boolean visible) {
            mVisible = visible;
            notifyPropertyChanged(BR.visible);
            return this;
        }

        @Bindable
        public boolean isEnabled() {
            return mEnabled;
        }

        public Config<V> setEnabled(boolean enabled) {
            if (enabled != mEnabled) {
                mEnabled = enabled;
                notifyPropertyChanged(BR.enabled);
            }
            return this;
        }

        public Config(@StringRes int titleRes) {
            mTitle = App.getStringOf(titleRes);
        }

        public Config<V> setPreview(@Nullable Function<V, CharSequence> preview) {
            this.mPreview = preview;
            return this;
        }

        public Config<V> setFilter(@Nullable BiPredicate<Exportable, V> filter) {
            mFilter = filter;
            return this;
        }


        public String getTitle() {
            return mTitle;
        }

        public boolean test(Exportable t) {
            return mFilter == null || mFilter.test(t, mValue);
        }

        @Bindable("value")
        public CharSequence getPreview() {
            return mPreview == null ? Objects.toString(mValue) : mPreview.apply(mValue);
        }


        @Bindable
        public V getValue() {
            return mValue;
        }

        public void setValue(V value) {
            if (!Objects.equals(value, mValue)) {
                this.mValue = value;
                notifyPropertyChanged(BR.value);
            }
        }
    }

    public static class SwitchConfig extends Config<Boolean> {
        public SwitchConfig(int titleRes) {
            super(titleRes);
        }

        public void switchValue() {
            setValue(!getValue());
        }
    }

    public static class PasswordConfig extends Config<String> {
        public PasswordConfig(int titleRes) {
            super(titleRes);
            setPreview(str -> {
                if (TextUtils.isEmpty(str)) {
                    return App.getTextOf(R.string.bracketed_none);
                } else {
                    return str;
                }
            });
        }

        @Override
        public String getValue() {
            return super.getValue();
        }

        @Override
        public void setValue(String value) {
            super.setValue(value);
        }
    }

    public static class EnumConfig<V> extends Config<V> {
        private V[] mValueEnum;

        @SafeVarargs
        public final Config<V> setValueEnum(@NotNull V... values) {
            mValueEnum = values;
            setValue(values[0]);
            return this;
        }

        public EnumConfig(int titleRes) {
            super(titleRes);
        }

        public void setValueEnumSelection(int valueEnumSelection) {
            setValue(mValueEnum[valueEnumSelection]);
        }

        public V[] getValueEnum() {
            return mValueEnum;
        }

        public CharSequence getPreview(int position) {
            return mPreview == null ? Objects.toString(mValueEnum[position]) : mPreview.apply(mValueEnum[position]);
        }
    }

    public static class DateConfig extends EnumConfig<Long> {
        public DateConfig(int titleRes) {
            super(titleRes);
            setPreview(time -> {
                if (time < 0) {
                    return time == -1L ? App.getStringOf(R.string.no_limit) : App.getStringOf(R.string.edit);
                } else {
                    return Utils.formatDate(time);
                }
            });
        }

        @Override
        public Long getValue() {
            return super.getValue();
        }

        @Override
        public Long[] getValueEnum() {
            return super.getValueEnum();
        }

        @Override
        public void setValue(Long value) {
            super.setValue(value);
        }
    }
}
