/*
 * Copyright (c) 2021 xjunz. 保留所有权利
 */
package xjunz.tool.werecord.impl.model.message.util;

import android.annotation.SuppressLint;
import android.database.Cursor;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

import xjunz.tool.werecord.impl.Environment;
import xjunz.tool.werecord.impl.model.account.Account;
import xjunz.tool.werecord.impl.model.message.MessageFactory;
import xjunz.tool.werecord.impl.repo.ContactRepository;
import xjunz.tool.werecord.impl.repo.RepositoryFactory;
import xjunz.tool.werecord.ui.message.fragment.StatisticsFragment;

/**
 * @author xjunz 2021/1/31 1:12
 */
public class MessageAnalyzer {
    private String mTalkerId;
    private final Environment mEnvironment;

    private MessageAnalyzer() {
        mEnvironment = Environment.getInstance();
    }

    @NotNull
    public static MessageAnalyzer analyze(String talkerId) {
        MessageAnalyzer analyzer = new MessageAnalyzer();
        analyzer.mTalkerId = talkerId;
        return analyzer;
    }

   /* public long getTotalCountExceptForSystemMessages() {
        StringBuilder sb = new StringBuilder();
        for (int i : MessageFactory.TYPE_ARRAY_SYSTEM) {
            sb.append(i).append(",");
        }
        sb.deleteCharAt(sb.length() - 1);
        try (Cursor cursor = mEnvironment.getWorkerDatabase()
                .rawQuery(String.format("select count(msgId) from message where talker='%s' and not in (%s)", mTalkerId, sb.toString()), null)) {
            if (cursor.moveToNext()) {
                return cursor.getLong(0);
            }
        }
        return 0;
    }*/

    public MsgTypeStat[] analyzeType() {
        try (Cursor cursor = mEnvironment.getWorkerDatabase()
                .rawQuery(String.format("select type,count(type) from message where talker='%s' group by type order by count(type) desc", mTalkerId), null)) {
            MsgTypeStat[] stats = new MsgTypeStat[cursor.getCount()];
            while (cursor.moveToNext()) {
                MsgTypeStat stat = new MsgTypeStat(MessageFactory.getTypeFromRaw(cursor.getInt(0)));
                stat.setCount(cursor.getInt(1));
                stats[cursor.getPosition()] = stat;
            }
            return stats;
        }
    }

    private static abstract class MsgStat implements Comparable<MsgStat> {
        public int getCount() {
            return count;
        }

        protected int count = 1;

        public abstract String getName();

        protected abstract int getTotalCount();

        public float getFraction() {
            return (float) count / getTotalCount();
        }

        @SuppressLint("DefaultLocale")
        public String getPercentage() {
            return String.format("%.2f%%", getFraction() * 100);
        }

        @Override
        public int compareTo(@NotNull MsgStat o) {
            //降序，加负号
            return -Integer.compare(this.count, o.count);
        }
    }

    private static class MsgCountStat extends MsgStat {
        private final String sender;
        private static int totalCount;

        public MsgCountStat(String sender) {
            this.sender = sender;
        }

        private void setCount(int count) {
            this.count = count;
            totalCount += count;
        }

        public Account getSender() {
            return RepositoryFactory.get(ContactRepository.class).get(sender);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MsgCountStat stat = (MsgCountStat) o;
            return Objects.equals(sender, stat.sender);
        }

        @Override
        public int hashCode() {
            return Objects.hash(sender);
        }


        @Override
        public String getName() {
            return getSender().getName();
        }

        @Override
        protected int getTotalCount() {
            return totalCount;
        }
    }

    private static class MsgTypeStat extends StatisticsFragment.MsgStat {
        public MessageFactory.Type type;
        private static int totalCount;

        public MsgTypeStat(MessageFactory.Type type) {
            this.type = type;
        }

        private void setCount(int count) {
            this.count = count;
            totalCount += count;
        }

        @Override
        public String getName() {
            return type.getCaption();
        }

        @Override
        protected int getTotalCount() {
            return totalCount;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MsgTypeStat stat = (MsgTypeStat) o;
            return Objects.equals(type, stat.type);
        }

        @Override
        public int hashCode() {
            return Objects.hash(type);
        }
    }

}
