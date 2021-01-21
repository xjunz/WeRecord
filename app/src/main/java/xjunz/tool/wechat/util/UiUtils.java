/*
 * Copyright (c) 2021 xjunz. 保留所有权利
 */

package xjunz.tool.wechat.util;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.transition.Transition;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.BounceInterpolator;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.AttrRes;
import androidx.annotation.CheckResult;
import androidx.annotation.ColorInt;
import androidx.annotation.Dimension;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

import xjunz.tool.wechat.App;
import xjunz.tool.wechat.R;
import xjunz.tool.wechat.ui.customview.MasterToast;

import static android.content.Context.LAYOUT_INFLATER_SERVICE;
import static xjunz.tool.wechat.App.getContext;

public class UiUtils {
    @CheckResult
    public static AlertDialog.Builder createRationale(Context context, Object msg) {
        return createDialog(context, R.string.rationale, msg).setPositiveButton(android.R.string.ok, null);
    }

    @CheckResult
    public static AlertDialog.Builder createCaveat(Context context, Object msg) {
        return createDialog(context, R.string.caveat, msg).setPositiveButton(android.R.string.ok, null);
    }

    @CheckResult
    public static AlertDialog.Builder createLaunch(Context context) {
        return UiUtils.createAlert(context, context.getString(R.string.alert_restart_after_changes_applied))
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, (dialog1, which1) -> {
                    Utils.openWeChat(context);
                });
    }

    @CheckResult
    public static AlertDialog.Builder createAlert(Context context, Object msg) {
        return createDialog(context, R.string.alert, msg).setPositiveButton(android.R.string.ok, null);
    }

    @CheckResult
    public static AlertDialog.Builder createHelp(Context context, Object msg) {
        return createDialog(context, R.string.help, msg).setPositiveButton(android.R.string.ok, null);
    }

    @CheckResult
    public static AlertDialog.Builder createError(Context context, Object msg) {
        return createDialog(context, R.string.error_occurred, msg).setPositiveButton(R.string.feedback, (dialog, which) -> {
            //TODO:反馈方式
            // Utils.feedbackJoinQGroup(context);
        }).setNegativeButton(android.R.string.ok, null);
    }

    /**
     * 显示一个可以[不再显示]的对话框
     *
     * @param context       上下文对象
     * @param title         标题
     * @param msg           信息
     * @param prefKey       从{@link xjunz.tool.wechat.App.SharedPrefsManager}中获取/保存是否显示的此对话框的键值
     * @param defNoMore     默认是否不再显示
     * @param runWhenNoMore 确认按钮点击事件，如果不再显示，直接执行此事件
     * @see R.layout#dialog_preference
     */
    public static void showPrefDialog(Context context, int title, int msg, String prefKey, boolean defNoMore, Runnable runWhenNoMore) {
        String finalPrefKey = "nms_" + prefKey;
        if (App.getSharedPrefsManager().noMore(finalPrefKey)) {
            runWhenNoMore.run();
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(context).setTitle(title);
            @SuppressLint("InflateParams") ViewGroup view = (ViewGroup) ((LayoutInflater) Objects.requireNonNull(context.getSystemService(LAYOUT_INFLATER_SERVICE))).inflate(R.layout.dialog_preference, null);
            TextView tvMsg = view.findViewById(R.id.fl_container);
            CheckBox cbNoMore = view.findViewById(R.id.cb_no_more);
            cbNoMore.setChecked(defNoMore);
            tvMsg.setText(msg);
            builder.setView(view);
            builder.show();
        }
    }

    @NotNull
    public static AlertDialog.Builder createDialog(Context context, @Nullable Object title, @Nullable Object msg) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.Theme_AppCompat_Light_Dialog_Alert_Material);
        if (title != null) {
            if (title instanceof Integer) {
                builder.setTitle((Integer) title);
            } else if (title instanceof CharSequence) {
                builder.setTitle((CharSequence) title);
            } else {
                builder.setTitle(title.toString());
            }
        }
        if (msg != null) {
            if (msg instanceof Integer) {
                builder.setMessage((Integer) msg);
            } else if (msg instanceof CharSequence) {
                builder.setMessage((CharSequence) msg);
            } else {
                builder.setMessage(msg.toString());
            }
        }
        return builder;
    }

    @CheckResult
    @NotNull
    public static Dialog createProgress(Context context, Object title) {
        @SuppressLint("InflateParams") View content = LayoutInflater.from(context).inflate(R.layout.dialog_progress, null);
        TextView tvTitle = content.findViewById(R.id.tv_msg);
        if (title != null) {
            if (title instanceof Integer) {
                tvTitle.setText((Integer) title);
            } else if (title instanceof CharSequence) {
                tvTitle.setText((CharSequence) title);
            } else {
                tvTitle.setText(title.toString());
            }
        }
        return new AlertDialog.Builder(context, R.style.Theme_AppCompat_Light_Dialog_Alert_Material).setView(content).setCancelable(false).create();
    }

    public static int getBottomMargin(@NotNull View view) {
        ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
        return lp.bottomMargin;
    }

    public static int getTopMargin(@NotNull View view) {
        ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
        return lp.topMargin;
    }

    @NotNull
    @Contract("_ -> new")
    public static Drawable getDrawingCacheOf(@NotNull View target) {
        Bitmap bitmap = Bitmap.createBitmap(target.getWidth(), target.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        target.draw(canvas);
        return new BitmapDrawable(target.getResources(), bitmap);
    }

    public static void fadeSwitchText(@NonNull final TextView target, final Object text) {
        if (target.getWidth() == 0 || target.getHeight() == 0) {
            if (text instanceof Integer) {
                target.setText((Integer) text);
            } else if (text instanceof CharSequence) {
                target.setText((CharSequence) text);
            }
            return;
        }
        final View parent = (View) target.getParent();
        Bitmap bitmap = Bitmap.createBitmap(target.getWidth(), target.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        target.draw(canvas);
        final Drawable drawingCache = new BitmapDrawable(target.getResources(), bitmap);
        ValueAnimator valueAnimator = ValueAnimator.ofFloat(0f, 1f);
        valueAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                parent.getOverlay().remove(drawingCache);
            }

            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                target.setAlpha(0);
                if (text instanceof Integer) {
                    target.setText((Integer) text);
                } else if (text instanceof CharSequence) {
                    target.setText((CharSequence) text);
                }
                Rect bounds = new Rect();
                target.getHitRect(bounds);
                drawingCache.setBounds(bounds);
                parent.getOverlay().add(drawingCache);
            }
        });
        valueAnimator.addUpdateListener(animation -> {
            float alpha = (float) animation.getAnimatedValue();
            target.setAlpha(alpha);
            drawingCache.setAlpha((int) ((1f - alpha) * 255));
        });
        valueAnimator.setInterpolator(AnimUtils.getFastOutSlowInInterpolator());
        valueAnimator.start();
    }


    public static void fadeSwitchImage(@NonNull final ImageView target, final Object drawable) {
        if (target.getWidth() == 0 || target.getHeight() == 0) {
            return;
        }
        final View parent = (View) target.getParent();
        Bitmap bitmap = Bitmap.createBitmap(target.getWidth(), target.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        target.draw(canvas);
        final Drawable drawingCache = new BitmapDrawable(target.getResources(), bitmap);
        ValueAnimator valueAnimator = ValueAnimator.ofFloat(0f, 1f);
        valueAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                parent.getOverlay().remove(drawingCache);
            }

            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                target.setAlpha(0f);
                if (drawable instanceof Integer) {
                    target.setImageResource((Integer) drawable);
                } else if (drawable instanceof Bitmap) {
                    target.setImageBitmap((Bitmap) drawable);
                } else if (drawable instanceof Drawable) {
                    target.setImageDrawable((Drawable) drawable);
                }
                Rect bounds = new Rect();
                target.getHitRect(bounds);
                drawingCache.setBounds(bounds);
                parent.getOverlay().add(drawingCache);
            }
        });
        valueAnimator.addUpdateListener(animation -> {
            float alpha = (float) animation.getAnimatedValue();
            target.setAlpha(alpha);
            drawingCache.setAlpha((int) ((1f - alpha) * 255));
        });
        valueAnimator.setInterpolator(AnimUtils.getFastOutSlowInInterpolator());
        valueAnimator.start();
    }

    public static void animateTranslateY(@NotNull View target, int transY) {
        target.animate().translationY(transY).setInterpolator(AnimUtils.getFastOutSlowInInterpolator()).start();
    }

    public static void animateTranslateX(@NotNull View target, int transX) {
        target.animate().translationX(transX).setInterpolator(AnimUtils.getFastOutSlowInInterpolator()).start();
    }


    @ColorInt
    public static int getAttrColor(@NonNull Context context, @AttrRes int attrRes) {
        TypedArray typedArray = context.obtainStyledAttributes(new int[]{attrRes});
        int color = typedArray.getColor(0, 0);
        typedArray.recycle();
        return color;
    }

    @Dimension
    public static int getAttrDimension(@NonNull Context context, @AttrRes int attrRes) {
        TypedArray typedArray = context.obtainStyledAttributes(new int[]{attrRes});
        int dimen = typedArray.getDimensionPixelSize(0, 0);
        typedArray.recycle();
        return dimen;
    }

    @Dimension
    public static int dip2px(float dipValue) {
        return (int) (dipValue * getContext().getResources().getDisplayMetrics().density + .5);
    }


    public static void visible(@NotNull View... views) {
        for (View v : views) {
            v.setVisibility(View.VISIBLE);
        }
    }

    public static void invisible(@NotNull View... views) {
        for (View v : views) {
            v.setVisibility(View.INVISIBLE);
        }
    }

    public static void disable(@NotNull View... views) {
        for (View v : views) {
            v.setEnabled(false);
        }
    }

    public static void enable(@NotNull View... views) {
        for (View v : views) {
            v.setEnabled(true);
        }
    }

    public static void gone(@NotNull View... views) {
        for (View v : views) {
            v.setVisibility(View.GONE);
        }
    }

    public static void swing(@NonNull View view) {
        ObjectAnimator objectAnimator = ObjectAnimator.ofFloat(view, View.TRANSLATION_X, -20F, 20F, 0F);
        objectAnimator.setInterpolator(new BounceInterpolator());
        objectAnimator.start();
    }

    public static void toast(@StringRes int msgRes) {
        MasterToast.shortToast(msgRes);
    }


    public static void toast(@Nullable Object... messages) {
        StringBuilder msg = new StringBuilder();
        if (messages == null) {
            msg.append("<null>");
        } else {
            for (Object obj : messages) {
                if (obj == null) {
                    msg.append("<null>");
                } else {
                    msg.append(obj.toString());
                }
            }
        }
        MasterToast.shortToast(msg);
    }

    public static int getFirstVisibleItemIndexOfList(@NotNull RecyclerView recyclerView, boolean completelyVisible) {
        LinearLayoutManager llm = (LinearLayoutManager) Objects.requireNonNull(recyclerView.getLayoutManager());
        return completelyVisible ? llm.findFirstCompletelyVisibleItemPosition() : llm.findFirstVisibleItemPosition();
    }

    public static int getLastVisibleItemIndexOfList(@NotNull RecyclerView recyclerView, boolean completelyVisible) {
        LinearLayoutManager llm = (LinearLayoutManager) Objects.requireNonNull(recyclerView.getLayoutManager());
        return completelyVisible ? llm.findLastCompletelyVisibleItemPosition() : llm.findLastVisibleItemPosition();
    }

    public static void fadeOut(@NotNull View... views) {
        for (View v : views) {
            v.animate().alpha(0f).withEndAction(() -> v.setVisibility(View.GONE)).start();
        }
    }

    public static void fadeIn(@NotNull View... views) {
        for (View v : views) {
            v.setAlpha(0f);
            v.animate().alpha(1f).withStartAction(() -> v.setVisibility(View.VISIBLE)).start();
        }
    }

    public static void getLocationCoordinateTo(@NotNull View src, @NotNull View anchor, @NotNull int[] out) {
        int[] srcPos = new int[2];
        src.getLocationInWindow(srcPos);
        int[] anchorPos = new int[2];
        anchor.getLocationInWindow(anchorPos);
        out[0] = srcPos[0] - anchorPos[0];
        out[1] = srcPos[1] - anchorPos[1];
    }

    public static void setTextKeepSelection(@NotNull EditText et, CharSequence text) {
        Editable editable = et.getText();
        editable.replace(0, editable.length(), text);
    }

    /**
     * Determine if the navigation bar will be on the bottom of the screen, based on logic in
     * PhoneWindowManager.
     */
    public static boolean isNavBarOnBottom(@NonNull Context context) {
        final Resources res = context.getResources();
        final Configuration cfg = context.getResources().getConfiguration();
        final DisplayMetrics dm = res.getDisplayMetrics();
        boolean canMove = (dm.widthPixels != dm.heightPixels &&
                cfg.smallestScreenWidthDp < 600);
        return (!canMove || dm.widthPixels < dm.heightPixels);
    }

    private static int sColorAccent = -1;
    private static int sColorControlHighlight = -1;
    private static int sTextColorSecondary = -1;

    public static void initColors(@NonNull Context context) {
        TypedArray typedArray = context.obtainStyledAttributes(new int[]{R.attr.colorAccent, R.attr.colorControlHighlight, android.R.attr.textColorSecondary});
        sColorAccent = typedArray.getColor(0, sColorAccent);
        sColorControlHighlight = typedArray.getColor(1, sColorControlHighlight);
        sTextColorSecondary = Objects.requireNonNull(typedArray.getColorStateList(2)).getDefaultColor();
        typedArray.recycle();
    }

    @ColorInt
    public static int getColorAccent() {
        return sColorAccent;
    }


    @ColorInt
    public static int getColorControlHighlight() {
        return sColorControlHighlight;
    }


    @ColorInt
    public static int getTextColorSecondary() {
        return sTextColorSecondary;
    }

    public static class TransitionListenerAdapter implements Transition.TransitionListener {


        @Override
        public void onTransitionStart(@NonNull Transition transition) {

        }

        @Override
        public void onTransitionEnd(@NonNull Transition transition) {

        }

        @Override
        public void onTransitionCancel(@NonNull Transition transition) {

        }

        @Override
        public void onTransitionPause(@NonNull Transition transition) {

        }

        @Override
        public void onTransitionResume(@NonNull Transition transition) {

        }
    }

}
