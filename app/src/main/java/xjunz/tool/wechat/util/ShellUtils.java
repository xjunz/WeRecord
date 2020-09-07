/*
 * Copyright (c) 2020 xjunz. 保留所有权利
 */

package xjunz.tool.wechat.util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.jaredrummler.android.shell.BuildConfig;
import com.jaredrummler.android.shell.CommandResult;
import com.jaredrummler.android.shell.Shell;

import org.apaches.commons.codec.binary.Hex;

import java.io.File;
import java.io.IOException;

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
     * @param tarPath     目标路径，原则上，此文件应当存在
     * @param msgWhenFail 操作失败时打印的信息
     * @throws ShellException 指令运行失败时抛出的异常
     */
    public static void cp(String srcPath, String tarPath, String msgWhenFail) throws ShellException {
        sudo(msgWhenFail, "cp", srcPath, tarPath);
    }

    /**
     * 使用{@code cp}指令复制某一文件到应用的数据文件夹内
     * <p>
     * 此方法会判断目标文件是否存在，因为如果目标文件不存在，
     * cp指令会使用默认属性（权限、所有者、用户组），导致我们使用
     * {@link File} API无法访问此文件。但是如果目标文件存在，就会
     * 沿用目标文件的属性。因此，如果目标文件不存在，先用{@code Java API}
     * 创建空文件
     * </p>
     *
     * @param srcPath 源文件路径
     * @param tarPath 目标路径
     */
    public static void cp2data(String srcPath, String tarPath, boolean overwriteIfExists, String msgWhenFail) throws IOException, ShellException {
        File target = new File(tarPath);
        if (!target.exists()) {
            if (target.createNewFile()) {
                sudo(msgWhenFail, "cp", srcPath, tarPath);
            } else {
                throw new IOException("Fail to create " + tarPath);
            }
        } else if (overwriteIfExists) {
            sudo(msgWhenFail, "cp", srcPath, tarPath);
        }
    }

    /**
     * 如果目标文件存在，删除之
     *
     * @param filePath    目标文件路径
     * @param msgWhenFail 失败后打印的信息
     * @throws ShellException Shell指令执行异常
     */
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

    /**
     * 重启微信
     */
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
     * @param msgWhenFail     指令运行失败时需要加入{@link ShellException#ShellException(String)}构造器的信息
     *                        <p>
     *                        此信息用于提醒开发者程序的何处出错，一般格式为{@code [类名(可省)],[方法名],[此方法的第几处sudo调用(唯一的sudo调用可省略)]}。
     *                        也可以使用其他显式提醒或为空。
     * @param commandSegments 指令段，每一个指令段之间会被加上空格并拼接成最后的指令
     * @return 返回指令运行的 {@link CommandResult}
     * @throws ShellException 指令运行失败时抛出的异常
     */
    @NonNull
    public static CommandResult sudo(@Nullable String msgWhenFail, @NonNull String... commandSegments) throws ShellException {
        StringBuilder command = new StringBuilder();
        for (String segment : commandSegments) {
            command.append(segment).append(" ");
        }
        CommandResult result = Shell.SU.run(command.toString());
        if (!result.isSuccessful()) {
            throw new ShellException((msgWhenFail == null ? "" : msgWhenFail + ":\n") + result.getStderr());
        }
        return result;
    }


}
