package me.ycdev.android.lib.ssproxy;

import android.os.IBinder;
import android.os.Process;
import android.os.SystemClock;

import java.util.Arrays;

import me.ycdev.android.lib.common.internalapi.android.os.ProcessIA;
import me.ycdev.android.lib.common.internalapi.android.os.ServiceManagerIA;
import me.ycdev.android.lib.common.utils.StringUtils;
import me.ycdev.android.lib.ssproxy.proxy.SysServiceProxyNative;
import me.ycdev.android.lib.ssproxy.utils.LibConfigs;
import me.ycdev.android.lib.ssproxy.utils.LibLogger;

class SysServiceProxyDaemon {
    private static final String TAG = "SysServiceProxyDaemon";
    private static final boolean DEBUG = LibConfigs.DEBUG_LOG;

    public static final String CMD_START = "cmd_start";
    public static final String CMD_STOP = "cmd_stop";

    public static void main(String[] args) {
        if (DEBUG) LibLogger.d(TAG, "Received params: " + Arrays.toString(args));
        if (args.length != 3) {
            if (DEBUG) LibLogger.e(TAG, "Usage: SysServiceProxyDaemon <cmd> <cmd parameters>");
            return;
        }

        // Check if we have the necessary permission.
        int uid = android.os.Process.myUid();
        if (uid != 0 && uid != Process.SYSTEM_UID) {
            if (DEBUG) LibLogger.e(TAG, "No permission! uid = " + uid);
            return;
        }

        String cmd = args[0];
        int ownerUid = StringUtils.parseInt(args[1], 0);
        String pkgName = args[2];
        if (CMD_START.equals(cmd)) {
            startDaemon(ownerUid, pkgName);
        } else if (CMD_STOP.equals(cmd)) {
            stopDaemon(ownerUid, pkgName);
        }
    }

    private static void startDaemon(int ownerUid, String pkgName) {
        // Check if the service is running
        String serviceName = SysServiceProxy.getSspServiceName(pkgName);
        if (ServiceManagerIA.checkService(serviceName) != null) {
            if (DEBUG) LibLogger.w(TAG, "SSP is running!");
            return;
        }

        // Add the service into ServiceManager
        SysServiceProxyNative sspBinder = new SysServiceProxyNative();
        sspBinder.setOwnerUid(ownerUid);
        ServiceManagerIA.addService(serviceName, sspBinder);

        if (DEBUG) LibLogger.d(TAG, "ssp is added");

        // Change the process name
        ProcessIA.setArgV0(SysServiceProxy.SSP_NAME_PREFIX + ownerUid);
        int ppid = ProcessIA.myPpid();
        android.os.Process.killProcess(ppid);

        // Keep the process running
        while (true) {
            if (DEBUG) {
                SystemClock.sleep(10 * 1000); // 10 seconds
            } else {
                SystemClock.sleep(60 * 1000); // 1 minute
            }

            if (DEBUG) LibLogger.d(TAG, "heart beat...");
            IBinder checkBinder = ServiceManagerIA.checkService(serviceName);
            if (checkBinder != sspBinder) {
                if (DEBUG) LibLogger.d(TAG, "bad binder: " + checkBinder);
                break; // quit
            }
        }

        LibLogger.w(TAG, "ssp service died: " + sspBinder);
    }

    private static void stopDaemon(int ownerUid, String pkgName) {
        // Check if the service is running
        String serviceName = SysServiceProxy.getSspServiceName(pkgName);
        if (ServiceManagerIA.checkService(serviceName) == null) {
            if (DEBUG) LibLogger.w(TAG, "SSP is not running!");
            return;
        }

        // Change the process name
        ProcessIA.setArgV0(SysServiceProxy.SSP_NAME_PREFIX + "stop-" + ownerUid);

        // Replace the service to make the old one to die
        SysServiceProxyNative sspBinder = new SysServiceProxyNative();
        sspBinder.setOwnerUid(ownerUid);
        ServiceManagerIA.addService(serviceName, sspBinder);

        if (DEBUG) LibLogger.d(TAG, "spp is replaced and should go to die");
        // Let the process to die to make the service to die
    }
}
