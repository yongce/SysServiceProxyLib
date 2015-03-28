package me.ycdev.android.lib.ssproxy;

import android.content.Context;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import me.ycdev.android.lib.common.internalapi.android.os.ServiceManagerIA;
import me.ycdev.android.lib.ssproxy.proxy.ISysServiceProxy;
import me.ycdev.android.lib.ssproxy.proxy.SysServiceProxyNative;
import me.ycdev.android.lib.ssproxy.utils.LibConfigs;
import me.ycdev.android.lib.ssproxy.utils.LibLogger;

/**
 * This class can be used to invoke services in system service manager.
 * It provides similar methods with android.os.ServiceManager.
 * <p/>
 * When {@link #startDaemon()} is invoked and the daemon is started,
 * a new service "ssproxy_<i>xxxxxx</i>" will be added into the system service manager,
 * and a new process "ssproxy_<i>yyyyyy</i>" will be started.
 */
public class SysServiceProxy {
    private static final String TAG = "SysServiceProxy";
    private static final boolean DEBUG = LibConfigs.DEBUG_LOG;

    static final String SSP_NAME_PREFIX = "ssproxy_";

    private Context mAppContext;
    private int mUid;
    private String mPkgName;
    private String mSspName;

    private static volatile SysServiceProxy sInstance;
    private SysServiceProxy(Context cxt) {
        mAppContext = cxt.getApplicationContext();
        mUid = android.os.Process.myUid();
        mPkgName = cxt.getPackageName();
        mSspName = getSspServiceName(mPkgName);
    }

    public static SysServiceProxy getInstance(Context cxt) {
        if (sInstance == null) {
            synchronized (SysServiceProxy.class) {
                if (sInstance == null) {
                    sInstance = new SysServiceProxy(cxt);
                }
            }
        }
        return sInstance;
    }

    static String getSspServiceName(String pkgName) {
        return SSP_NAME_PREFIX + pkgName;
    }

    public boolean isDaemonAlive() {
        if (DEBUG) LibLogger.d(TAG, "check daemon alive...");
        IBinder sspBinder = ServiceManagerIA.getService(mSspName);
        if (sspBinder != null) {
            if (DEBUG) LibLogger.d(TAG, "ssp added");
            if (sspBinder.isBinderAlive()) {
                if (DEBUG) LibLogger.d(TAG, "ssp alive");
                return true;
            }
        }
        return false;
    }

    public boolean startDaemon() {
        if (DEBUG) LibLogger.d(TAG, "start daemon...");
        if (Looper.myLooper() == Looper.getMainLooper()) {
            throw new RuntimeException("Cannot be invoked in UI thread");
        }

        IBinder sspBinder = ServiceManagerIA.getService(mSspName);
        if (sspBinder != null) {
            if (DEBUG) LibLogger.d(TAG, "service already added");
            if (sspBinder.isBinderAlive()) {
                if (DEBUG) LibLogger.d(TAG, "ssp alive");
                return true;
            }
        }

        String[] cmds = RootPermUtils.getRootJarCommand(mAppContext,
                SysServiceProxyDaemon.CMD_START, String.valueOf(mUid), mPkgName);
        RootPermUtils.runSuCommand(cmds, "u:object_r:default_android_service:s0");
        if (DEBUG) LibLogger.d(TAG, "start daemon done");
        return ServiceManagerIA.getService(mSspName) != null;
    }

    public boolean stopDaemon() {
        if (DEBUG) LibLogger.d(TAG, "stop daemon...");
        if (Looper.myLooper() == Looper.getMainLooper()) {
            throw new RuntimeException("Cannot be invoked in UI thread");
        }

        IBinder sspBinder = ServiceManagerIA.getService(mSspName);
        if (sspBinder == null) {
            if (DEBUG) LibLogger.d(TAG, "service is not running");
            return true;
        }

        String[] cmds = RootPermUtils.getRootJarCommand(mAppContext,
                SysServiceProxyDaemon.CMD_STOP, String.valueOf(mUid), mPkgName);
        RootPermUtils.runSuCommand(cmds, "u:object_r:default_android_service:s0");
        if (DEBUG) LibLogger.d(TAG, "stop daemon done");
        return ServiceManagerIA.getService(mSspName) == null;
    }

    @Nullable
    public IBinder checkService(@NonNull String name) {
        IBinder sspBinder = ServiceManagerIA.getService(mSspName);
        ISysServiceProxy sspService = SysServiceProxyNative.asInterface(sspBinder);
        if (sspService != null) {
            return sspService.checkService(name);
        }
        return null;
    }

    @Nullable
    public IBinder getService(@NonNull String name) {
        IBinder sspBinder = ServiceManagerIA.getService(mSspName);
        ISysServiceProxy sspService = SysServiceProxyNative.asInterface(sspBinder);
        if (sspService != null) {
            return sspService.getService(name);
        }
        return null;
    }

    // hidden
    @SuppressWarnings("unused")
    void addService(@NonNull String name, @NonNull IBinder service) {
        IBinder sspBinder = ServiceManagerIA.getService(mSspName);
        ISysServiceProxy sspService = SysServiceProxyNative.asInterface(sspBinder);
        if (sspService != null) {
            sspService.addService(name, service);
        }
    }

    @Nullable
    public String[] listServices() {
        IBinder sspBinder = ServiceManagerIA.getService(mSspName);
        ISysServiceProxy sspService = SysServiceProxyNative.asInterface(sspBinder);
        if (sspService != null) {
            return sspService.listServices();
        }
        return null;
    }
}
