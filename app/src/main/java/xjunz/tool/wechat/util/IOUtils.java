package xjunz.tool.wechat.util;

import org.apaches.commons.codec.DecoderException;
import org.apaches.commons.codec.binary.Hex;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

public class IOUtils {

    public static void transferStream(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int count;
        while ((count = in.read(buffer, 0, buffer.length)) != -1) {
            out.write(buffer, 0, count);
        }
        out.flush();
        in.close();
        out.close();
    }


    public static void serializeToStorage(Object obj, String path) {
        try {
            FileOutputStream fos = new FileOutputStream(path);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(obj);
            oos.close();
            byte[] bytes = bos.toByteArray();
            String hex = String.valueOf(Hex.encodeHex(bytes));
            bos.close();
            ByteArrayInputStream bis = new ByteArrayInputStream(hex.getBytes());
            transferStream(bis, fos);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Nullable
    public static String serializeToString(Object obj) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(obj);
            oos.close();
            byte[] bytes = bos.toByteArray();
            String hex = String.valueOf(Hex.encodeHex(bytes));
            bos.close();
            return hex;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


    @Nullable
    public static <T> T deserializeFromStorage(String path, Class<T> t) {
        try {
            FileInputStream fis = new FileInputStream(path);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            transferStream(fis, bos);
            byte[] decoded = Hex.decodeHex(bos.toString());
            ByteArrayInputStream bis = new ByteArrayInputStream(decoded);
            ObjectInputStream ois = new ObjectInputStream(bis);
            bis.close();
            ois.close();
            try {
                return t.cast(ois.readObject());
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        } catch (IOException | DecoderException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Nullable
    public static <T> T deserializeFromString(String src, Class<T> t) {
        try {
            byte[] decoded = Hex.decodeHex(src);
            ByteArrayInputStream bis = new ByteArrayInputStream(decoded);
            ObjectInputStream ois = new ObjectInputStream(bis);
            bis.close();
            ois.close();
            try {
                return t.cast(ois.readObject());
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        } catch (IOException | DecoderException e) {
            e.printStackTrace();
        }
        return null;
    }


}
