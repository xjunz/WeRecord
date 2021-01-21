/*
 * Copyright (c) 2021 xjunz. 保留所有权利
 */
package xjunz.tool.wechat.ui.base;

import androidx.databinding.ObservableBoolean;
import androidx.fragment.app.Fragment;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import xjunz.tool.wechat.impl.model.message.Message;
import xjunz.tool.wechat.impl.model.message.util.Template;
import xjunz.tool.wechat.ui.message.EditorActivity;

public abstract class EditorFragment extends Fragment {
    protected Message mVictim;
    protected Message mOrigin;
    private int mEditionMode;
    private final ObservableBoolean mEdited = new ObservableBoolean(false);
    protected Template mTemplate;
    private static final List<EditorFragment> mGlobalListeners = new ArrayList<>();

    public EditorFragment() {
        mGlobalListeners.add(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mGlobalListeners.remove(this);
    }

    public EditorFragment setOrigin(@NotNull Message origin) {
        this.mOrigin = origin;
        this.mVictim = origin.deepClone();
        return this;
    }

    public void setTemplate(@Nullable Template template) {
        mTemplate = template;
        if (template != null) {
            for (EditorFragment fragment : mGlobalListeners) {
                fragment.mVictim = template.toMessage(mOrigin.getTalkerId(), mOrigin.getCreateTimeStamp());
                if (fragment.isAdded()) {
                    fragment.onMessageReset();
                }
            }
        } else {
            reset();
        }
    }

    public EditorFragment setEditionMode(int editionMode) {
        mEditionMode = editionMode;
        return this;
    }

    protected void notifyMessageChanged() {
        //我以为，只有编辑模式下才有消息改变这一说
        mEdited.set(isInEditionMode() && !mOrigin.deepEquals(mVictim));
    }

    public ObservableBoolean isEdited() {
        return mEdited;
    }

    public Message getEditedMessage() {
        return mVictim;
    }

    public boolean isInEditionMode() {
        return mEditionMode == EditorActivity.EDIT_MODE_EDIT;
    }

    public void reset() {
        mVictim = mOrigin.deepClone();
        mEdited.set(false);
        onMessageReset();
    }

    protected abstract void onMessageReset();
}
