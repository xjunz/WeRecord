/*
 * Copyright (c) 2020 xjunz. 保留所有权利
 */

package xjunz.tool.wechat.util;

import com.jaredrummler.android.shell.CommandResult;
import com.jaredrummler.android.shell.Shell;

import java.io.File;
import java.io.IOException;

public class ShellUtils {

    public static class ShellException extends Exception {
        ShellException(String message) {
            super(message);
        }
    }


    /**
     * 使用{@code cp}指令复制某一文件到指定位置
     *
     * @param srcPath     源文件路径
     * @param tarPath     目标路径
     * @param msgWhenFail 操作失败时打印的信息
     * @throws ShellException Shell指令执行异常
     * @throws IOException    Java文件操作异常
     */
    public static void cp(String srcPath, String tarPath, String msgWhenFail) throws ShellException, IOException {
        File backup = new File(tarPath);
        if (backup.createNewFile()) {
            CommandResult result = Shell.SU.run("cp " + srcPath + " " + tarPath);
            if (!result.isSuccessful()) {
                throw new ShellException(formatError(result, msgWhenFail));
            }
        }
    }

    /**
     * 使用{@code cp}指令复制某一文件到指定位置，不抛出异常
     *
     * @param srcPath 源文件路径
     * @param tarPath 目标路径
     */
    public static void cpNoError(String srcPath, String tarPath) {
        File backup = new File(tarPath);
        try {
            if (backup.createNewFile()) {
                Shell.SU.run("cp " + srcPath + " " + tarPath);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 使用{@code cat}指令获取指定文件的文本内容
     *
     * @param path        指定文件路径
     * @param msgWhenFail 操作失败时打印的文本
     * @return 文本内容
     * @throws ShellException Shell指令执行异常
     */
    public static String cat(String path, String msgWhenFail) throws ShellException {
        CommandResult result = Shell.SU.run("cat " + path);
        if (!result.isSuccessful()) {
            throw new ShellException(formatError(result, msgWhenFail));
        } else {
            return result.getStdout();
        }
    }

    private static String formatError(CommandResult result, String cus) {
        return cus + ":" + result.getStderr() + ":" + result.exitCode;
    }

}
