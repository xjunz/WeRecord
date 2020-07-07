package xjunz.tool.wechat.ui.intro.fragment;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;

public abstract class IntroFragment extends Fragment {
    private static OnStepDoneListener sListener;
    private boolean mIsDone;

    void notifyStepDone() {
        mIsDone = true;
        sListener.onStepDone(getStepIndex());
    }

    public interface OnStepDoneListener {
        void onStepDone(int step);
    }

    public abstract @DrawableRes
    int getIconResource();

    public abstract @StringRes
    int getTitleResource();

    public boolean isThisStepDone() {
        return mIsDone;
    }

    public abstract int getStepIndex();

    public static void setOnStepDoneListener(OnStepDoneListener listener) {
        sListener = listener;
    }
}
