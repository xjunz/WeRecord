/*
 * Copyright (c) 2020 xjunz. 保留所有权利
 */

package xjunz.tool.wechat.util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.jaredrummler.android.shell.BuildConfig;
import com.jaredrummler.android.shell.CommandResult;
import com.jaredrummler.android.shell.Shell;

import org.apaches.commons.codec.binary.Hex;

public class ShellUtils {

    public static class ShellException extends Exception {
        ShellException(String message) {
            super(BuildConfig.DEBUG ? message : Hex.encodeHexString(message.getBytes()));
        }
    }

    /**
     * 使用{@code cp}指令复制某一文件到指定位置
     *
     * @param srcPath     源文件路径
     * @param tarPath     目标路径
     * @param msgWhenFail 操作失败时打印的信息
     * @throws ShellException 指令运行失败时抛出的异常
     */
    public static void cp(String srcPath, String tarPath, String msgWhenFail) throws ShellException {
        sudo(msgWhenFail, "cp", srcPath, tarPath);
    }

    /**
     * 使用{@code cp}指令复制某一文件到指定位置，不抛出异常
     *
     * @param srcPath 源文件路径
     * @param tarPath 目标路径
     */
    public static void cpNoError(String srcPath, String tarPath, String msgWhenFail) {
        try {
            sudo(msgWhenFail, "cp", srcPath, tarPath);
        } catch (ShellException e) {
            e.printStackTrace();
        }
    }

    public static void rmIfExists(String filePath, String msgWhenFail) throws ShellException {
        String rmUnless = "if [ -e \"" + filePath + "\" ];then\n rm -f " + filePath + "\nfi";
        ShellUtils.sudo(msgWhenFail, rmUnless);
    }

    /**
     * 使用{@code cat}指令获取指定文件的文本内容
     *
     * @param path        指定文件路径
     * @param msgWhenFail 操作失败时打印的文本
     * @return 文本内容
     * @throws ShellException 指令运行失败时抛出的异常
     */
    public static String cat(String path, String msgWhenFail) throws ShellException {
        return sudo(msgWhenFail, "cat", path).getStdout();
    }

    @WorkerThread
    public static void restartWechat() {
        try {
            sudo("restartWechat,1", "am", "kill", "com.tencent.mm");
            sudo("restartWechat,2", "am", "start", "com.tencent.mm/.ui.LauncherUI");
        } catch (ShellException e) {
            e.printStackTrace();
        }
    }

    /**
     * 以root用户身份运行一段{@code shell}指令
     *
     * @param msgWhenFail    指令运行失败时需要加入{@link ShellException#ShellException(String)}构造器的信息
     *                       <p>
     *                       此信息用于提醒开发者程序的何处出错，一般格式为{@code [类名(可省)],[方法名],[此方法的第几处sudo调用(唯一的sudo调用可省略)]}。
     *                       也可以使用其他显式提醒或为空。
     * @param commandSegment 指令段，每一个指令段之间会被加上空格并拼接成最后的指令
     * @return 返回指令运行的 {@link CommandResult}
     * @throws ShellException 指令运行失败时抛出的异常
     */
    @NonNull
    public static CommandResult sudo(@Nullable String msgWhenFail, @NonNull String... commandSegment) throws ShellException {
        StringBuilder command = new StringBuilder();
        for (String segment : commandSegment) {
            command.append(segment).append(" ");
        }
        CommandResult result = Shell.SU.run(command.toString());
        if (!result.isSuccessful()) {
            throw new ShellException((msgWhenFail == null ? "" : msgWhenFail + ":\n") + result.getStderr());
        }
        return result;
    }


}
