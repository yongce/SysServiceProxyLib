# SysServiceProxyLib

## Library usage

Please refer code of "demo" project.

### Start daemon

```
SysServiceProxy.getInstance(appContext).startDaemon();
```
### Check if the daemon is running

```
SysServiceProxy.getInstance(appContext).isDaemonAlive();
```

### Invoke system services
For example, make the device goto sleep:
```
    private void gotoSleep() {
        IBinder powerBinder = SysServiceProxy.getInstance(this).getService(Context.POWER_SERVICE);
        AppLogger.d(TAG, "power binder: " + powerBinder);
        if (powerBinder != null) {
            Object powerService = PowerManagerIA.asInterface(powerBinder);
            AppLogger.d(TAG, "power service: " + powerService);
            if (powerService != null) {
                PowerManagerIA.goToSleep(powerService, SystemClock.uptimeMillis());
            }
        }
    }
```

### Stop the daemon
```
SysServiceProxy.getInstance(appContext).stopDaemon();
```

## Library development

### Unit testing
Execute all test cases and create only one merged report:
```
$ ./gradlew mergeAndroidReports
```
View the test report at location "build/reports/androidTests/index.html".
