/*
 * Copyright (c) 2021 xjunz. 保留所有权利
 */

package xjunz.tool.werecord.util;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import com.jaredrummler.android.shell.CommandResult;
import com.jaredrummler.android.shell.Shell;
import com.jaredrummler.android.shell.ShellExitCode;
import com.jaredrummler.android.shell.ShellNotFoundException;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

public class ShellUtils {

    public static class ShellException extends Exception {
        ShellException(String message) {
            super(message);
        }
    }

    /**
     * 使用{@code cp}指令复制某一文件到指定位置
     *
     * @param srcPath 源文件路径
     * @param tarPath 目标路径，原则上，此文件应当存在
     * @throws ShellException 指令运行失败时抛出的异常
     */
    public static void cp(String srcPath, String tarPath) throws ShellException {
        sudo("cp", srcPath, tarPath);
    }

    /**
     * 使用{@code cp}指令复制某一文件到应用的数据文件夹内，此方法会先判断原文件是否存在，
     * 如果原文件不存在，不会抛出异常
     * <p>
     * 此方法会判断目标文件是否存在，因为如果目标文件不存在，
     * cp指令会使用默认属性（权限、所有者、用户组），导致我们使用
     * {@link File} API无法访问此文件。但是如果目标文件存在，就会
     * 沿用目标文件的属性。因此，如果目标文件不存在，先用{@code Java API}
     * 创建空文件
     * </p>
     *
     * @param srcPath           源文件路径
     * @param tarPath           目标路径
     * @param overwriteIfExists 如果目标文件已存在，是否重写
     */
    public static void cp2dataIfExists(String srcPath, String tarPath, boolean overwriteIfExists) throws IOException, ShellException {
        File target = new File(tarPath);
        String cpUnless = String.format("if [ -e \"%1$s\" ];then\n cp %1$s %2$s\nfi", srcPath, tarPath);
        if (!target.exists()) {
            if (target.createNewFile()) {
                sudo(cpUnless);
            } else {
                throw new IOException("Fail to create " + tarPath);
            }
        } else if (overwriteIfExists) {
            sudo(cpUnless);
        }
    }

    /**
     * 使用{@code cp}指令复制某一文件到应用的数据文件夹内，如果原文件不存在的话会抛出异常
     *
     * @param srcPath           源文件路径
     * @param tarPath           目标路径
     * @param overwriteIfExists 如果目标文件已存在，是否重写
     * @see ShellUtils#cp2dataIfExists(String, String, boolean)
     */
    public static void cp2data(String srcPath, String tarPath, boolean overwriteIfExists) throws IOException, ShellException {
        File target = new File(tarPath);
        if (!target.exists()) {
            if (target.createNewFile()) {
                cp(srcPath, tarPath);
            } else {
                throw new IOException("Fail to create " + tarPath);
            }
        } else if (overwriteIfExists) {
            cp(srcPath, tarPath);
        }
    }

    /**
     * 如果目标文件存在，删除之
     *
     * @param filePath 目标文件路径
     * @throws ShellException Shell指令执行异常
     */
    public static void rmIfExists(String filePath) throws ShellException {
        String rmUnless = "if [ -e \"" + filePath + "\" ];then\n rm -f " + filePath + "\nfi";
        ShellUtils.sudo(rmUnless);
    }

    /**
     * 使用{@code cat}指令获取指定文件的文本内容
     *
     * @param path 指定文件路径
     * @return 文本内容
     * @throws ShellException 指令运行失败时抛出的异常
     */
    public static String cat(String path) throws ShellException {
        return sudo("cat", path).getStdout();
    }

    public static void forceStop(String packageName) throws ShellException {
        sudo("am", "force-stop", packageName);
    }

    /**
     * 使用am指令集启动Activity
     */
    public static void startActivity(String activityName) throws ShellException {
        sudo("am", "start", activityName);
    }

    public static class SU_MM {
        private static volatile Shell.Console console;

        @WorkerThread
        public static Shell.Console getConsole() throws ShellNotFoundException {
            if (console == null || console.isClosed()) {
                synchronized (Shell.SH.class) {
                    if (console == null || console.isClosed()) {
                        console = new Shell.Console.Builder().setShell(Shell.SU.shellMountMaster()).setWatchdogTimeout(30).build();
                    }
                }
            }
            return console;
        }

        /**
         * Closes the console if open
         */
        public static void closeConsole() {
            if (console != null) {
                synchronized (Shell.SU.class) {
                    if (console != null) {
                        console.close();
                        console = null;
                    }
                }
            }
        }

        @WorkerThread
        public static CommandResult run(@NonNull String... commands) {
            try {
                return getConsole().run(commands);
            } catch (ShellNotFoundException e) {
                return new CommandResult(
                        Collections.<String>emptyList(), Collections.<String>emptyList(), ShellExitCode.SHELL_NOT_FOUND);
            }
        }
    }

    /**
     * 以root用户身份运行一段{@code shell}指令
     *
     * @param commandSegments 指令段，每一个指令段之间会被加上空格并拼接成最后的指令
     * @return 返回指令运行的 {@link CommandResult}
     * @throws ShellException 指令运行失败时抛出的异常
     */
    @NonNull
    public static CommandResult sudo(@NonNull String... commandSegments) throws ShellException {
        StringBuilder command = new StringBuilder();
        for (String segment : commandSegments) {
            command.append(segment).append(" ");
        }
        //在android q 以上，需使用挂载全局命名空间的su
        CommandResult result = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ?
                SU_MM.run(command.toString()) : Shell.SU.run(command.toString());
        if (!result.isSuccessful()) {
            throw new ShellException(result.getStderr());
        }
        return result;
    }
}
