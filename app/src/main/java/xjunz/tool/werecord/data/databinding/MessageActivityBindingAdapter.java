/*
 * Copyright (c) 2021 xjunz. 保留所有权利
 */

package xjunz.tool.werecord.data.databinding;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.os.Build;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.databinding.BindingAdapter;
import androidx.databinding.InverseBindingAdapter;
import androidx.databinding.InverseBindingListener;

import org.jetbrains.annotations.NotNull;

import de.hdodenhof.circleimageview.CircleImageView;
import io.reactivex.schedulers.Schedulers;
import xjunz.tool.werecord.R;
import xjunz.tool.werecord.impl.model.account.Account;
import xjunz.tool.werecord.impl.model.message.util.Edition;
import xjunz.tool.werecord.impl.repo.ContactRepository;
import xjunz.tool.werecord.impl.repo.RepositoryFactory;
import xjunz.tool.werecord.ui.customview.ElasticDragDismissFrameLayout;
import xjunz.tool.werecord.ui.customview.MasterToast;
import xjunz.tool.werecord.ui.message.fragment.SearchFragment;
import xjunz.tool.werecord.util.ActivityUtils;
import xjunz.tool.werecord.util.RxJavaUtils;
import xjunz.tool.werecord.util.UiUtils;

public class MessageActivityBindingAdapter {
    @BindingAdapter(value = {"android:editMode"})
    public static void setEditMode(ImageButton imageButton, boolean oldValue, boolean editMode) {
        if (oldValue != editMode) {
            imageButton.setTag(editMode);
            MasterToast.shortToast(editMode ? R.string.edit_mode_entered : R.string.edit_mode_exited);
            UiUtils.fadeSwitchImage(imageButton, editMode ? R.drawable.ic_baseline_eye_24 : R.drawable.ic_baseline_edit_24);
        }
    }

    @InverseBindingAdapter(attribute = "android:editMode", event = "android:editModeAttrChanged")
    public static boolean isEditMode(@NotNull ImageButton imageButton) {
        return imageButton.getTag() != null && (boolean) imageButton.getTag();
    }

    @BindingAdapter(value = {"android:editModeOnClick", "android:editModeAttrChanged"}, requireAll = false)
    public static void setEditModeChangeListener(@NotNull ImageButton imageButton, View.OnClickListener listener, InverseBindingListener editModeAttr) {
        imageButton.setOnClickListener(v -> {
            imageButton.setTag(!isEditMode(imageButton));
            editModeAttr.onChange();
            if (listener != null) {
                listener.onClick(v);
            }
        });
    }

    @BindingAdapter(value = "android:avatar")
    public static void setAvatar(CircleImageView imageView, Account oldAccount, Account newAccount) {
        if (oldAccount != null && oldAccount.equals(newAccount)) {
            return;
        }
        if (newAccount == null) {
            imageView.setImageResource(R.mipmap.avatar_default);
        } else {
            RxJavaUtils.maybe(newAccount::getAvatar).subscribeOn(Schedulers.io()).subscribe(new RxJavaUtils.MaybeObserverAdapter<Bitmap>() {
                @Override
                public void onComplete() {
                    imageView.setImageResource(R.mipmap.avatar_default);
                }

                @Override
                public void onSuccess(@NotNull Bitmap bitmap) {
                    imageView.setImageBitmap(bitmap);
                }
            });
        }
    }

    @BindingAdapter(value = "android:avatar")
    public static void setAvatar(CircleImageView imageView, String oldId, String newId) {
        if (oldId != null && oldId.equals(newId)) {
            return;
        }
        if (newId == null) {
            imageView.setImageResource(R.mipmap.avatar_default);
        } else {
            RxJavaUtils.maybe(() -> {
                ContactRepository repository = RepositoryFactory.get(ContactRepository.class);
                Account account = repository.get(newId);
                if (account == null) {
                    return null;
                } else {
                    return account.getAvatar();
                }
            }).subscribe(new RxJavaUtils.MaybeObserverAdapter<Bitmap>() {
                @Override
                public void onComplete() {
                    imageView.setImageResource(R.mipmap.avatar_default);
                }

                @Override
                public void onSuccess(@NotNull Bitmap bitmap) {
                    imageView.setImageBitmap(bitmap);
                }
            });
        }
    }

    @BindingAdapter(value = "android:avatarIfExists")
    public static void setAvatarIfExists(CircleImageView imageView, Account oldAccount, Account newAccount) {
        if (oldAccount != null && oldAccount.equals(newAccount)) {
            return;
        }
        if (newAccount == null) {
            imageView.setVisibility(View.GONE);
        } else {
            imageView.setVisibility(View.VISIBLE);
            RxJavaUtils.maybe(newAccount::getAvatar).subscribeOn(Schedulers.io()).subscribe(new RxJavaUtils.MaybeObserverAdapter<Bitmap>() {
                @Override
                public void onComplete() {
                    imageView.setImageResource(R.mipmap.avatar_default);
                }

                @Override
                public void onSuccess(@NotNull Bitmap bitmap) {
                    imageView.setImageBitmap(bitmap);
                }
            });
        }
    }

    @BindingAdapter(value = "android:layout_marginTop")
    public static void setMarginTop(@NotNull View view, float marginTop) {
        ConstraintLayout.LayoutParams lp = (ConstraintLayout.LayoutParams) view.getLayoutParams();
        lp.topMargin = (int) marginTop;
        view.setLayoutParams(lp);
    }

    @SuppressLint("ClickableViewAccessibility")
    @BindingAdapter(value = {"android:contextMenu"})
    public static void setContextMenu(@NotNull View view, Runnable onClick) {
        Activity activity = ActivityUtils.getHostActivity(view.getContext());
        activity.registerForContextMenu(view);
        float[] touchPos = new float[2];
        view.setOnTouchListener((v, event) -> {
            view.onTouchEvent(event);
            touchPos[0] = event.getX();
            touchPos[1] = event.getY();
            return false;
        });
        view.setOnClickListener(v -> {
            if (onClick != null) {
                onClick.run();
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                v.showContextMenu(touchPos[0], touchPos[1]);
            } else {
                v.showContextMenu();
            }
        });
        view.setOnLongClickListener(v -> {
            if (onClick != null) {
                onClick.run();
            }
            return false;
        });
    }

    @BindingAdapter(value = "android:onDismiss")
    public static void setOnDismissCallback(ElasticDragDismissFrameLayout layout, Runnable onDismiss) {
        layout.addListener(new ElasticDragDismissFrameLayout.ElasticDragDismissCallback() {
            @Override
            public void onDragDismissed() {
                super.onDragDismissed();
                if (onDismiss != null) {
                    onDismiss.run();
                }
            }
        });
    }

    @BindingAdapter(value = "android:keywordHighlight")
    public static void setKeywordHighlight(TextView textView, SearchFragment.MessageItem item) {
        if (item.spanStartIndex >= 0 && item.spanLength > 0) {
            //设置高亮
            ForegroundColorSpan foregroundColorSpan = new ForegroundColorSpan(UiUtils.getColorAccent());
            BackgroundColorSpan backgroundColorSpan = new BackgroundColorSpan(UiUtils.getColorControlHighlight());
            SpannableString span = new SpannableString(item.message.getParsedContent());
            span.setSpan(foregroundColorSpan, item.spanStartIndex, item.spanStartIndex + item.spanLength, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            span.setSpan(backgroundColorSpan, item.spanStartIndex, item.spanStartIndex + item.spanLength, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
            textView.setText(span);
        } else {
            //清除样式
            textView.setText(item.message.getParsedContent());
        }
    }

    @BindingAdapter(value = "android:editionColor")
    public static void setEditionColor(@NotNull View view, int edition) {
        Context context = view.getContext();
        switch (edition) {
            case Edition.FLAG_REMOVAL:
                view.setBackgroundTintList(ColorStateList.valueOf(context.getColor(R.color.colorDel)));
                break;
            case Edition.FLAG_INSERTION:
                view.setBackgroundTintList(ColorStateList.valueOf(context.getColor(R.color.colorIns)));
                break;
            case Edition.FLAG_REPLACEMENT:
                view.setBackgroundTintList(ColorStateList.valueOf(context.getColor(R.color.colorRep)));
                break;
        }
    }


}
