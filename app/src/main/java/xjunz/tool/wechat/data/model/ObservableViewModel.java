package xjunz.tool.wechat.data.model;

import androidx.databinding.Observable;
import androidx.databinding.PropertyChangeRegistry;
import androidx.lifecycle.ViewModel;

/**
 * 用于{@code DataBinding}的[可观察]的{@link ViewModel}
 * <p>
 * 参考自：
 *
 * @see <a href=https://github.com/android/databinding-samples/blob/master/TwoWaySample/app/src/main/java/com/example/android/databinding/twowaysample/util/ObservableViewModel.kt>
 * </p>
 */
public class ObservableViewModel extends ViewModel implements Observable {
    private PropertyChangeRegistry mCallbacks = new PropertyChangeRegistry();

    @Override
    public void addOnPropertyChangedCallback(OnPropertyChangedCallback callback) {
        mCallbacks.add(callback);
    }

    @Override
    public void removeOnPropertyChangedCallback(OnPropertyChangedCallback callback) {
        mCallbacks.remove(callback);
    }


    /**
     * Notifies observers that all properties of this instance have changed.
     */
    void notifyChange() {
        mCallbacks.notifyCallbacks(this, 0, null);
    }

    /**
     * Notifies observers that a specific property has changed. The getter for the
     * property that changes should be marked with the @Bindable annotation to
     * generate a field in the BR class to be used as the fieldId parameter.
     *
     * @param fieldId The generated BR id for the Bindable field.
     */
    void notifyPropertyChanged(int fieldId) {
        mCallbacks.notifyCallbacks(this, fieldId, null);
    }
}
