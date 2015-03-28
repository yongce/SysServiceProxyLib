package me.ycdev.android.lib.ssproxy;

import android.app.AlarmManager;
import android.content.Context;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;
import android.test.AndroidTestCase;

import java.util.Arrays;

import eu.chainfire.libsuperuser.Shell;
import me.ycdev.android.lib.common.compat.PowerManagerCompat;
import me.ycdev.android.lib.common.internalapi.android.os.PowerManagerIA;
import me.ycdev.android.lib.ssproxy.utils.TestLogger;

public class SysServiceProxyTest extends AndroidTestCase {
    private static final String TAG = "SysServiceProxyTest";

    public void test_startDaemon() {
        assertTrue("wasn't granted root permission", Shell.SU.available());

        SysServiceProxy ssp = SysServiceProxy.getInstance(getContext());
        ssp.stopDaemon();
        assertFalse("failed to stop daemon", ssp.isDaemonAlive());

        ssp.startDaemon();
        assertTrue("failed to start daemon", ssp.isDaemonAlive());

        ssp.stopDaemon();
        assertFalse("failed to stop daemon", ssp.isDaemonAlive());
    }

    public void test_stopDaemon() {
        assertTrue("wasn't granted root permission", Shell.SU.available());

        SysServiceProxy ssp = SysServiceProxy.getInstance(getContext());
        ssp.startDaemon();
        assertTrue("failed to start daemon", ssp.isDaemonAlive());

        ssp.stopDaemon();
        assertFalse("failed to stop daemon", ssp.isDaemonAlive());
    }

    public void test_listServices() {
        SysServiceProxy ssp = SysServiceProxy.getInstance(getContext());
        ssp.startDaemon();
        assertTrue("failed to start daemon", ssp.isDaemonAlive());

        String[] services = ssp.listServices();
        assertNotNull("failed to get services", services);

        Arrays.sort(services);
        TestLogger.d(TAG, "all services: " + Arrays.toString(services));
        String sspName = SysServiceProxy.getSspServiceName(getContext().getPackageName());
        assertTrue("ssp not found", Arrays.binarySearch(services, sspName) >= 0);
        assertTrue("power not found", Arrays.binarySearch(services, "power") >= 0);
        assertTrue("batterystats not found", Arrays.binarySearch(services, "batteryinfo") >= 0
                || Arrays.binarySearch(services, "batterystats") >= 0);

        ssp.stopDaemon();
        assertFalse("failed to stop daemon", ssp.isDaemonAlive());
    }

    public void test_getService() {
        SysServiceProxy ssp = SysServiceProxy.getInstance(getContext());
        ssp.startDaemon();
        assertTrue("failed to start daemon", ssp.isDaemonAlive());

        IBinder powerBinder = ssp.getService(Context.POWER_SERVICE);
        doTestScreenOnOff(powerBinder);

        ssp.stopDaemon();
        assertFalse("failed to stop daemon", ssp.isDaemonAlive());
    }

    private void doTestScreenOnOff(IBinder powerBinder) {
        assertNotNull("failed to get power binder", powerBinder);

        Object powerService = PowerManagerIA.asInterface(powerBinder);
        assertNotNull("failed to get power service", powerService);

        PowerManager pm = (PowerManager) getContext().getSystemService(Context.POWER_SERVICE);
        if (!PowerManagerCompat.isScreenOn(pm)) {
            //noinspection deprecation
            PowerManager.WakeLock wakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK
                    | PowerManager.ACQUIRE_CAUSES_WAKEUP, "test");
            try {
                wakeLock.acquire();
                SystemClock.sleep(1000);
                assertTrue("failed to turn of the screen", PowerManagerCompat.isScreenOn(pm));
            } finally {
                wakeLock.release();
            }
        }

        PowerManagerIA.goToSleep(powerService, SystemClock.uptimeMillis());
        SystemClock.sleep(1000);
        assertFalse("failed to turn off the screen", PowerManagerCompat.isScreenOn(pm));

        AlarmManager alarmMgr = (AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE);
        alarmMgr.set(AlarmManager.RTC_WAKEUP, 0, null);
        //noinspection deprecation
        PowerManager.WakeLock wakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK
                | PowerManager.ACQUIRE_CAUSES_WAKEUP, "test");
        try {
            wakeLock.acquire();
            SystemClock.sleep(1000);
            assertTrue("failed to turn on the screen", PowerManagerCompat.isScreenOn(pm));
        } finally {
            wakeLock.release();
        }
    }

    public void test_checkService() {
        SysServiceProxy ssp = SysServiceProxy.getInstance(getContext());
        ssp.startDaemon();
        assertTrue("failed to start daemon", ssp.isDaemonAlive());

        IBinder powerBinder = ssp.checkService(Context.POWER_SERVICE);
        doTestScreenOnOff(powerBinder);

        ssp.stopDaemon();
        assertFalse("failed to stop daemon", ssp.isDaemonAlive());
    }

}
