/*
 * Copyright (c) 2020 xjunz. 保留所有权利
 */
package xjunz.tool.wechat.ui.customview;

import android.content.Context;
import android.text.Editable;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.BackgroundColorSpan;
import android.text.style.StrikethroughSpan;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatEditText;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import xjunz.tool.wechat.util.UiUtils;

/**
 * 编辑可视化的{@link android.widget.EditText}
 * <p>
 * 删除的内容使用中划线({@link StrikethroughSpan})标注，新增的内容使用有颜色的背景({@link BackgroundColorSpan})标注，
 * 替换的内容被分解为原文本的删除和新文本的增加。被标注为删除的内容不能插入、替换，只能还原，且被标注为删除的内容再次
 * 删除后会被还原。
 * </p>
 */
public class EditionVisualizedEditText extends AppCompatEditText {
    private boolean mShouldNotifyTextChanged;
    private TextWatcher mTextWatcher = new TextWatcher() {
        /**
         * 被删除的字符串
         */
        private CharSequence deletion;
        /**
         * 原字符串的起始点
         */
        private int start;
        /**
         * 原字符串的长度
         */
        private int count;
        /**
         * 原字符串被替换后的长度
         */
        private int after;

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            if (mShouldNotifyTextChanged) {
                this.after = after;
                this.count = count;
                this.start = start;
                if (after == 0 && count > 0) {
                    deletion = s.subSequence(start, start + count);
                }
            }
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable s) {
            EditionProfile profile = new EditionProfile(s);
            if (mShouldNotifyTextChanged) {
                if (count == 0 && after > 0) {
                    //===添加===
                    for (int i = start; i < start + after; i++) {
                        profile.makeAddition(i);
                    }
                } else if (count > 0 && after > 0) {
                    //===替换===
                    if (after > count) {
                        //==增量替换==
                    } else if (after < count) {
                        //==减量替换==
                    } else {
                        //==等量替换==
                    }
                } else if (after == 0 && count > 0) {
                    //===删除===
                    mShouldNotifyTextChanged = false;
                    //先将被删除的文本添加添加回去
                    s.insert(start, deletion);
                    int end = start + count;
                    profile.build(start, end);
                    int deletedCount = 0;
                    //判断这段文本是否有编辑
                    boolean pure = profile.isPure(start, end);
                    //从起始到结束遍历
                    for (int i = start; i < end; i++) {
                        //如果没有编辑
                        if (pure) {
                            //全部标记为删除
                            profile.makeDeletion(i);
                        } else {
                            //否则
                            if (profile.isInAddition(i)) {
                                //如果当前位置被标记为添加，则取消标记
                                profile.removeAddition(i);
                                mShouldNotifyTextChanged = false;
                                //并删除添加的文本
                                s.delete(i - deletedCount, i - deletedCount + 1);
                                deletedCount++;
                            } else if (profile.isInDeletion(i)) {
                                //如果当前位置被标记为删除，则取消标记，恢复为原文本
                                profile.removeDeletion(i - deletedCount);
                            }
                        }
                    }
                    //光标前移一格
                    setSelection(start);
                }
            }
            mShouldNotifyTextChanged = true;
        }
    };


    public EditionVisualizedEditText(Context context) {
        super(context);
        init();
    }

    public EditionVisualizedEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public EditionVisualizedEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        addTextChangedListener(mTextWatcher);
    }


    private class EditionProfile {
        private static final int FLAG_DELETION = 2;
        private static final int FLAG_ADDITION = 3;
        private static final int FLAG_NONE = 5;
        private List<Integer> deletionSerial;
        private List<Integer> additionSerial;
        private Editable editable;

        private EditionProfile(Editable editable) {
            deletionSerial = new ArrayList<>();
            additionSerial = new ArrayList<>();
            this.editable = editable;
        }

        private boolean containsFlag(int composeFlag, int flag) {
            return composeFlag % flag == 0;
        }

        private boolean isAddition(int pos) {
            return getEditionFlag(pos) % FLAG_ADDITION == 0;
        }

        private boolean isPlain(int pos) {
            return getEditionFlag(pos) == FLAG_NONE;
        }

        /**
         * 判断某段文本是否不包含任何编辑
         */
        private boolean isPure(int start, int end) {
            Object[] spans = editable.getSpans(start, end, Object.class);
            for (Object span : spans) {
                if (span instanceof StrikethroughSpan) {
                    return false;
                } else if (span instanceof BackgroundColorSpan) {
                    return false;
                }
            }
            return true;
        }

        private boolean isDeletion(int pos) {
            int composeFlag = getEditionFlag(pos);
            if (composeFlag % FLAG_DELETION == 0 && composeFlag != FLAG_DELETION) {
                throw new IllegalArgumentException("Deletion is expected to be selected single");
            } else {
                return composeFlag == FLAG_DELETION;
            }
        }

        private void build(int start, int end) {
            Object[] spans = editable.getSpans(start, end, Object.class);
            for (Object span : spans) {
                if (span instanceof StrikethroughSpan) {
                    //如果是删除线span
                    StrikethroughSpan strikethroughSpan = (StrikethroughSpan) span;
                    int pos = editable.getSpanStart(strikethroughSpan);
                    int index = Collections.binarySearch(deletionSerial, pos);
                    if (index < 0) {
                        deletionSerial.add(-(index + 1), pos);
                    }
                } else if (span instanceof BackgroundColorSpan) {
                    //如果是背景span
                    BackgroundColorSpan backgroundColorSpan = (BackgroundColorSpan) span;
                    int pos = editable.getSpanStart(backgroundColorSpan);
                    int index = Collections.binarySearch(additionSerial, pos);
                    if (index < 0) {
                        additionSerial.add(-(index + 1), pos);
                    }
                }
            }
            //combinedDeletionPairs = combine(deletionPairs);
            //combinedAdditionPairs = combine(additionPairs);
        }


        private List<int[]> combine(@NotNull List<int[]> pairs) {
            List<int[]> combined = new ArrayList<>(pairs);
            if (combined.size() != 0) {
                StringBuilder serial = new StringBuilder();
                for (int[] pair : combined) {
                    if (serial.length() != 0) {
                        serial.append(",");
                    }
                    serial.append(pair[0]);
                    serial.append(",");
                    serial.append(pair[1]);
                }
                String deletionSerial = serial.toString().replaceAll("(,\\d+)\\1", "");
                combined.clear();
                String[] deletions = deletionSerial.split(",");
                for (int i = 0; i < deletions.length / 2; i++) {
                    combined.add(new int[]{Integer.parseInt(deletions[i * 2]), Integer.parseInt(deletions[i * 2 + 1])});
                }
            }
            return combined;
        }


        private int getEditionFlag(int pos) {
            Object[] spans = Objects.requireNonNull(editable).getSpans(pos, pos, Object.class);
            for (Object span : spans) {
                if (span instanceof StrikethroughSpan) {
                    return FLAG_DELETION;
                } else if (span instanceof BackgroundColorSpan) {
                    return FLAG_ADDITION;
                }
            }
            return FLAG_NONE;
        }

        private void makeDeletion(int pos) {
            StrikethroughSpan strikethroughSpan = new StrikethroughSpan();
            editable.setSpan(strikethroughSpan, pos, pos + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        private void makeAddition(int pos) {
            BackgroundColorSpan backgroundColorSpan = new BackgroundColorSpan(UiUtils.getColorControlHighlight());
            editable.setSpan(backgroundColorSpan, pos, pos + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        private boolean isSpanOf(@NonNull Object span, int flag) {
            switch (flag) {
                case FLAG_ADDITION:
                    return span instanceof StrikethroughSpan;
                case FLAG_DELETION:
                    return span instanceof BackgroundColorSpan;
                case FLAG_NONE:
                    return false;
            }
            return false;
        }

        private void removeDeletion(int pos) {
            Object[] spans = editable.getSpans(pos, pos + 1, Object.class);
            for (Object span : spans) {
                if (span instanceof StrikethroughSpan) {
                    editable.removeSpan(span);
                }
            }
        }

        private void removeAddition(int pos) {
            Object[] spans = editable.getSpans(pos, pos + 1, Object.class);
            for (Object span : spans) {
                if (span instanceof BackgroundColorSpan) {
                    editable.removeSpan(span);
                }
            }
        }

        private void remove(@NotNull List<int[]> list, int[] item) {
            for (int[] ints : list) {
                if (ints[0] == item[0] && ints[1] == item[1]) {
                    list.remove(ints);
                    return;
                }
            }
        }

        private boolean isInDeletion(int pos) {
            return Collections.binarySearch(deletionSerial, pos) >= 0;
        }

        private boolean isInAddition(int pos) {
            return Collections.binarySearch(additionSerial, pos) >= 0;
        }


    }


    @Override
    protected void onSelectionChanged(int selStart, int selEnd) {
        super.onSelectionChanged(selStart, selEnd);
    }

}
