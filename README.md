# SysServiceProxyLib

SysServiceProxyLib (SSProxy for short) is an Android library for invoking system services in service manager. Root permission is required.

## Library User Guide

### Import library

View all released version at [Releases](https://github.com/yongce/SysServiceProxyLib/releases).

Either use the released tags or use the "release" branch to get a released version.

### Library usage

For a complete example, please refer code of the "demo" module.

#### Start daemon

First of all, you must start the SSProxy daemon before invoking any other methods. If successful, a new serivce "ssproxy_xxx" will be added into system service manager.

```
SysServiceProxy.getInstance(appContext).startDaemon();
```

### Check if the daemon is available

```
SysServiceProxy.getInstance(appContext).isDaemonAlive();
```

### Invoke system services via SSProxy

For example, make the device goto sleep:

```
IBinder powerBinder = SysServiceProxy.getInstance(this).getService(Context.POWER_SERVICE);
AppLogger.d(TAG, "power binder: " + powerBinder);
if (powerBinder != null) {
    Object powerService = PowerManagerIA.asInterface(powerBinder);
    AppLogger.d(TAG, "power service: " + powerService);
    if (powerService != null) {
        PowerManagerIA.goToSleep(powerService, SystemClock.uptimeMillis());
    }
}
```

### Stop the daemon

You can stop the daemon when you don't need it anymore.

```
SysServiceProxy.getInstance(appContext).stopDaemon();
```

## Library Development

### Branches and tags

There are two important branches: master and release. The "master" branch is for the purpose of library development; the "release" branch is for library release.

When a new version is ready for library users, a new release will be added and tagged at [Releases](https://github.com/yongce/SysServiceProxyLib/releases).

### Unit testing

This library should be well tested by unit test cases.
Execute all test cases and create only one merged report:
```
$ ./gradlew mergeAndroidReports
```
View the test report at location "build/reports/androidTests/index.html".

#### Test reports

To view all test reports of this library, please go to [Test Reports](https://github.com/yongce/SysServiceProxyLib/wiki/Test-Reports).

## License

   Copyright 2015 Yongce Tu (yongce.tu@gmail.com)

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
