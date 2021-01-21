/*
 * Copyright (c) 2021 xjunz. 保留所有权利
 */
package xjunz.tool.wechat.ui.main.fragment.dialog;

import android.os.Bundle;
import android.text.TextUtils;
import android.transition.AutoTransition;
import android.transition.Transition;
import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;

import org.jetbrains.annotations.NotNull;

import xjunz.tool.wechat.R;
import xjunz.tool.wechat.databinding.DialogAddContactByIdBinding;
import xjunz.tool.wechat.impl.DatabaseModifier;
import xjunz.tool.wechat.impl.Environment;
import xjunz.tool.wechat.impl.model.account.Contact;
import xjunz.tool.wechat.impl.repo.ContactRepository;
import xjunz.tool.wechat.impl.repo.RepositoryFactory;
import xjunz.tool.wechat.ui.customview.MasterToast;
import xjunz.tool.wechat.util.IOUtils;
import xjunz.tool.wechat.util.RxJavaUtils;
import xjunz.tool.wechat.util.UiUtils;

/**
 * @author xjunz 2021/1/15 21:17
 */
public class AddContactByIdDialog extends DialogFragment {
    private DialogAddContactByIdBinding mBinding;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NO_FRAME, R.style.Base_Dialog_Translucent);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = DialogAddContactByIdBinding.inflate(inflater);
        mBinding.setHost(this);
        return mBinding.getRoot();
    }

    public void confirm() {
        String wxid = mBinding.etWxid.getText().toString();
        if (TextUtils.isEmpty(wxid)) {
            UiUtils.swing(mBinding.etWxid);
            MasterToast.shortToast(getString(R.string.format_require_nonnull, getString(R.string.wxid)));
            return;
        }
        if (!wxid.matches("^[0-9A-z_@]+?$")) {
            UiUtils.swing(mBinding.etWxid);
            MasterToast.shortToast(R.string.error_format);
            return;
        }
        Contact contact = RepositoryFactory.get(ContactRepository.class).get(wxid);
        if (contact != null && contact.type == Contact.Type.FRIEND) {
            UiUtils.swing(mBinding.etWxid);
            MasterToast.shortToast(getString(R.string.format_require_unique, wxid));
            return;
        }
        DatabaseModifier modifier = Environment.getInstance().modifyDatabase();
        for (int i = 0; i < mBinding.container.getChildCount(); i++) {
            View view = mBinding.container.getChildAt(i);
            if (view.getId() == R.id.pb_load) {
                UiUtils.visible(view);
            } else {
                UiUtils.gone(view);
            }
        }
        ViewGroup.LayoutParams lp = mBinding.container.getLayoutParams();
        lp.width = ViewGroup.LayoutParams.WRAP_CONTENT;
        mBinding.container.setLayoutParams(lp);
        Transition transition = new AutoTransition();
        transition.setInterpolator(new FastOutSlowInInterpolator());
        transition.addListener(new UiUtils.TransitionListenerAdapter() {
            @Override
            public void onTransitionEnd(@NonNull Transition transition) {
                //IMPLEMENTATION
                RxJavaUtils.complete(() -> {
                    modifier.addVerifyMessageFromId(wxid);
                    modifier.apply();
                }).subscribe(new RxJavaUtils.CompletableObservableAdapter() {
                    @Override
                    public void onComplete() {
                        postConfirm(true);
                        UiUtils.createLaunch(requireContext()).show();
                    }

                    @Override
                    public void onError(@NotNull Throwable e) {
                        postConfirm(false);
                        UiUtils.createError(requireContext(), IOUtils.readStackTraceFromThrowable(e)).show();
                    }
                });
            }
        });
        TransitionManager.beginDelayedTransition((ViewGroup) mBinding.getRoot(), transition);
        setCancelable(false);
    }

    private void postConfirm(boolean clearText) {
        if (clearText) {
            mBinding.etWxid.getText().clear();
        }
        for (int i = 0; i < mBinding.container.getChildCount(); i++) {
            View view = mBinding.container.getChildAt(i);
            if (view.getId() == R.id.pb_load) {
                UiUtils.gone(view);
            } else {
                UiUtils.visible(view);
            }
        }
        ViewGroup.LayoutParams lp = mBinding.container.getLayoutParams();
        lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
        mBinding.container.setLayoutParams(lp);
        Transition transition = new AutoTransition();
        transition.setInterpolator(new FastOutSlowInInterpolator());
        TransitionManager.beginDelayedTransition((ViewGroup) mBinding.getRoot(), transition);
        setCancelable(true);
    }
}
