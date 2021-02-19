/*
 * Copyright (c) 2021 xjunz. 保留所有权利
 */
package xjunz.tool.werecord.impl.model.message.util;

import androidx.annotation.NonNull;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;

public class LvBufferUtils {
    public static final int MAX_BYTES_LENGTH = 0xc00;
    public static final int TYPE_BUFFER = 2;
    public static final int TYPE_INTEGER = 1;
    public static final int TYPE_STRING = 0;
    public static final int TYPE_LONG = 3;
    private ByteBuffer mByteBuffer;

    public static boolean isLegal(byte[] buffer) {
        return buffer != null && buffer.length != 0 && buffer[0] == '{' && buffer[buffer.length - 1] == '}';
    }

    @NotNull
    @Contract(pure = true)
    public static Object[] createEmptyLvBuffer(@NotNull int[] typeSerial) {
        Object[] objects = new Object[typeSerial.length];
        for (int i = 0; i < typeSerial.length; i++) {
            if (typeSerial[i] == TYPE_INTEGER || typeSerial[i] == TYPE_LONG) {
                objects[i] = 0;
            }
        }
        return objects;
    }

    private void wrap(byte[] buffer) {
        mByteBuffer = ByteBuffer.wrap(buffer);
        mByteBuffer.position(1);
    }

    public byte[] generateLvBuffer(@NotNull Object[] parsed, int[] serial) {
        start();
        for (int i = 0; i < parsed.length; i++) {
            Object obj = parsed[i];
            switch (serial[i]) {
                case TYPE_INTEGER:
                    if (obj != null) {
                        putInt(((Integer) obj));
                    }
                    break;
                case TYPE_STRING:
                    putString((String) obj);
                    break;
                case TYPE_BUFFER:
                    putBuffer((byte[]) obj);
                    break;
                case TYPE_LONG:
                    if (obj != null) {
                        putLong(((Integer) obj));
                    }
                    break;
            }
        }
        return end();
    }

    @Nullable
    public Object[] readLvBuffer(byte[] buffer, int[] serial) {
        if (isLegal(buffer)) {
            wrap(buffer);
            Object[] parsedLvBuffer = new Object[serial.length];
            for (int i = 0; i < serial.length; i++) {
                if (isLastPosition()) {
                    return parsedLvBuffer;
                }
                switch (serial[i]) {
                    case TYPE_STRING:
                        parsedLvBuffer[i] = getString();
                        break;
                    case TYPE_INTEGER:
                        parsedLvBuffer[i] = getInt();
                        break;
                    case TYPE_BUFFER:
                        parsedLvBuffer[i] = getBuffer();
                        break;
                    case TYPE_LONG:
                        parsedLvBuffer[i] = getLong();
                        break;
                }
            }
            return parsedLvBuffer;
        }
        return null;
    }

    private int getInt() {
        return mByteBuffer.getInt();
    }

    private long getLong() {
        return mByteBuffer.getLong();
    }

    @NonNull
    private byte[] getBuffer() {
        int i = mByteBuffer.getShort();
        if (i > MAX_BYTES_LENGTH) {
            throw new IllegalArgumentException("Unexpected buffer length: " + i);
        } else if (i == 0) {
            return new byte[0];
        } else {
            byte[] buffer2 = new byte[i];
            mByteBuffer.get(buffer2, 0, i);
            return buffer2;
        }
    }

    @NonNull
    private String getString() {
        int i = mByteBuffer.getShort();
        if (i > MAX_BYTES_LENGTH) {
            throw new IllegalArgumentException("Unexpected String length: " + i);
        } else if (i == 0) {
            return "";
        } else {
            byte[] buffer = new byte[i];
            mByteBuffer.get(buffer, 0, i);
            return new String(buffer);
        }
    }

    private void forward(int i) {
        mByteBuffer.position(mByteBuffer.position() + i);
    }

    private void leap() {
        short s = mByteBuffer.getShort();
        if (s > 3072) {
            mByteBuffer = null;
        } else {
            mByteBuffer.position(s + mByteBuffer.position());
        }
    }

    private boolean isLastPosition() {
        return mByteBuffer.remaining() <= 1;
    }

    private void start() {
        mByteBuffer = ByteBuffer.allocate(4096);
        mByteBuffer.put((byte) 123);
    }

    private void lengthenIfNecessary(int length) {
        if (mByteBuffer.remaining() <= length) {
            ByteBuffer allocate = ByteBuffer.allocate(mByteBuffer.limit() + 4096);
            allocate.put(mByteBuffer.array(), 0, mByteBuffer.position());
            mByteBuffer = allocate;
        }
    }

    private void putInt(int i) {
        lengthenIfNecessary(4);
        mByteBuffer.putInt(i);
    }

    private void putLong(long j) {
        lengthenIfNecessary(8);
        mByteBuffer.putLong(j);
    }

    private void putBuffer(byte[] buffer) {
        if (buffer == null) {
            buffer = new byte[0];
        }
        if (buffer.length > MAX_BYTES_LENGTH) {
            throw new IllegalArgumentException("Unexpected buffer length: " + buffer.length);
        }
        //2 more for short
        lengthenIfNecessary(buffer.length + 2);
        mByteBuffer.putShort((short) buffer.length);
        if (buffer.length > 0) {
            mByteBuffer.put(buffer);
        }
    }

    private void putString(String str) {
        byte[] buffer = null;
        if (str != null) {
            buffer = str.getBytes();
        }
        if (buffer == null) {
            buffer = new byte[0];
        }
        if (buffer.length > MAX_BYTES_LENGTH) {
            return;
        }
        lengthenIfNecessary(buffer.length + 2);
        mByteBuffer.putShort((short) buffer.length);
        if (buffer.length > 0) {
            mByteBuffer.put(buffer);
        }
    }

    @NotNull
    private byte[] end() {
        lengthenIfNecessary(1);
        mByteBuffer.put((byte) 125);
        byte[] buffer = new byte[mByteBuffer.position()];
        System.arraycopy(mByteBuffer.array(), 0, buffer, 0, buffer.length);
        return buffer;
    }
}
