package me.ycdev.android.lib.ssproxy;

import android.content.Context;
import android.text.TextUtils;

import eu.chainfire.libsuperuser.Shell;

class RootPermUtils {
    public static String[] getRootJarCommand(Context cxt, String cmd, String... cmdParams) {
        String exportCmd = "export CLASSPATH=" + cxt.getPackageCodePath();
        String rootJarCmd = "/system/bin/app_process /system/bin "
                + SysServiceProxyDaemon.class.getName() + " " + cmd;
        if (cmdParams.length > 0) {
            for (String p : cmdParams) {
                rootJarCmd = rootJarCmd + " " + p;
            }
        }
        return new String[] { exportCmd, rootJarCmd };
    }

    public static void runSuCommand(String[] cmds, String selinuxContext) {
        if (!TextUtils.isEmpty(selinuxContext) && isSELinuxEnforced()) {
            String shell = Shell.SU.shell(0, selinuxContext);
            Shell.run(shell, cmds, null, false, false);
        } else {
            Shell.run("su", cmds, null, false, false);
        }
    }

    private static boolean isSELinuxEnforced() {
        return Shell.SU.isSELinuxEnforcing();
    }
}
