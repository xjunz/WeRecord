package xjunz.tool.wechat.util;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.AttrRes;
import androidx.annotation.ColorInt;
import androidx.annotation.Dimension;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import xjunz.tool.wechat.R;
import xjunz.tool.wechat.ui.customview.MasterToast;

import static xjunz.tool.wechat.App.getContext;

public class UiUtils {

    public static AlertDialog.Builder createRationale(Context context, Object msg) {
        return createDialog(context, R.string.rationale, msg).setPositiveButton(android.R.string.ok, null);
    }

    public static AlertDialog.Builder createAlert(Context context, Object msg) {
        return createDialog(context, R.string.alert, msg).setPositiveButton(android.R.string.ok, null);
    }

    public static AlertDialog.Builder createDialog(Context context, @Nullable Object title, @Nullable Object msg) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
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

    public static void fadeSwitchText(@NonNull final TextView target, final Object text) {
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
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float alpha = (float) animation.getAnimatedValue();
                target.setAlpha(alpha);
                drawingCache.setAlpha((int) ((1f - alpha) * 255));
            }
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
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float alpha = (float) animation.getAnimatedValue();
                target.setAlpha(alpha);
                drawingCache.setAlpha((int) ((1f - alpha) * 255));
            }
        });
        valueAnimator.setInterpolator(AnimUtils.getFastOutSlowInInterpolator());
        valueAnimator.start();
    }


    public static void translateY(View target, int transY) {
        target.animate().translationY(transY).setInterpolator(AnimUtils.getFastOutSlowInInterpolator()).start();
    }

    public static Dialog createProgressDialog(Context context, int titleRes) {
        @SuppressLint("InflateParams") View content = LayoutInflater.from(context).inflate(R.layout.dialog_progress, null);
        TextView tvTitle = content.findViewById(R.id.tv_title);
        tvTitle.setText(titleRes);
        return new AlertDialog.Builder(context).setView(content).setCancelable(false).create();
    }

    @ColorInt
    public static int getAttrColor(@NonNull Context context, @AttrRes int attrRes) {
        TypedArray typedArray = context.obtainStyledAttributes(new int[]{attrRes});
        int color = typedArray.getColor(0, 0);
        typedArray.recycle();
        return color;
    }

    public static @Dimension
    int getAttrDimension(@NonNull Context context, @AttrRes int attrRes) {
        TypedArray typedArray = context.obtainStyledAttributes(new int[]{attrRes});
        int dimen = typedArray.getDimensionPixelSize(0, 0);
        typedArray.recycle();
        return dimen;
    }

    public static int dip2px(float dipValue) {
        return (int) (dipValue * getContext().getResources().getDisplayMetrics().density + .5);
    }


    public static void visible(View... views) {
        for (View v : views) {
            v.setVisibility(View.VISIBLE);
        }
    }

    public static void invisible(View... views) {
        for (View v : views) {
            v.setVisibility(View.INVISIBLE);
        }
    }

    public static void disable(View... views) {
        for (View v : views) {
            v.setEnabled(false);
        }
    }

    public static void enable(View... views) {
        for (View v : views) {
            v.setEnabled(true);
        }
    }

    public static void gone(View... views) {
        for (View v : views) {
            v.setVisibility(View.GONE);
        }
    }


    public static void toast(@Nullable Object msg) {
        if (msg instanceof Integer) {
            MasterToast.shortToast((Integer) msg);
        } else {
            if (msg == null) {
                msg = "<null>";
            }
            MasterToast.shortToast(msg.toString());
        }

    }

}
