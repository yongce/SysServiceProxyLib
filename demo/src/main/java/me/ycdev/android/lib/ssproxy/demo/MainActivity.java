package me.ycdev.android.lib.ssproxy.demo;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.SystemClock;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.util.Arrays;

import eu.chainfire.libsuperuser.Debug;
import me.ycdev.android.lib.common.internalapi.android.app.ActivityManagerIA;
import me.ycdev.android.lib.common.internalapi.android.os.PowerManagerIA;
import me.ycdev.android.lib.common.internalapi.android.os.ServiceManagerIA;
import me.ycdev.android.lib.common.utils.WeakHandler;
import me.ycdev.android.lib.ssproxy.SysServiceProxy;
import me.ycdev.android.lib.ssproxy.demo.utils.AppLogger;


public class MainActivity extends ActionBarActivity implements View.OnClickListener,
        WeakHandler.MessageHandler {
    private static final String TAG = "MainActivity";

    private static final int MSG_START_DAEMON_DONE = 100;
    private static final int MSG_STOP_DAEMON_DONE = 101;
    private static final int MSG_DAEMON_NOT_RUNNING = 102;

    private Handler mHandler = new WeakHandler(this);

    private Button mStartDaemonBtn;
    private Button mStopDaemonBtn;

    private Button mGotoSleepBtn;
    private Button mRebootBtn;
    private Button mListServicesBtn;
    private Button mTestRawInvokingBtn;

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case MSG_START_DAEMON_DONE: {
                boolean result = (Boolean) msg.obj;
                if (result) {
                    Toast.makeText(this, R.string.tips_start_daemon_success, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, R.string.tips_start_daemon_failure, Toast.LENGTH_LONG).show();
                }
                break;
            }

            case MSG_STOP_DAEMON_DONE: {
                boolean result = (Boolean) msg.obj;
                if (result) {
                    Toast.makeText(this, R.string.tips_stop_daemon_success, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, R.string.tips_stop_daemon_failure, Toast.LENGTH_LONG).show();
                }
                break;
            }

            case MSG_DAEMON_NOT_RUNNING: {
                Toast.makeText(this, R.string.tips_daemon_not_running, Toast.LENGTH_SHORT).show();
                break;
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        AppLogger.i(TAG, "#onCreate()");

        mStartDaemonBtn = (Button) findViewById(R.id.start_daemon);
        mStartDaemonBtn.setOnClickListener(this);
        mStopDaemonBtn = (Button) findViewById(R.id.stop_daemon);
        mStopDaemonBtn.setOnClickListener(this);

        mGotoSleepBtn = (Button) findViewById(R.id.goto_sleep);
        mGotoSleepBtn.setOnClickListener(this);
        mRebootBtn = (Button) findViewById(R.id.reboot);
        mRebootBtn.setOnClickListener(this);
        mListServicesBtn = (Button) findViewById(R.id.list_services);
        mListServicesBtn.setOnClickListener(this);
        mTestRawInvokingBtn = (Button) findViewById(R.id.test_raw_invoking);
        mTestRawInvokingBtn.setOnClickListener(this);

        Debug.setDebug(true);
    }

    private void startDaemon() {
        final Context appContext = getApplicationContext();
        new Thread() {
            @Override
            public void run() {
                Boolean result = SysServiceProxy.getInstance(appContext).startDaemon();
                mHandler.obtainMessage(MSG_START_DAEMON_DONE, result).sendToTarget();
            }
        }.start();
    }

    private void stopDaemon() {
        final Context appContext = getApplicationContext();
        new Thread() {
            @Override
            public void run() {
                Boolean result = SysServiceProxy.getInstance(appContext).stopDaemon();
                mHandler.obtainMessage(MSG_STOP_DAEMON_DONE, result).sendToTarget();
            }
        }.start();
    }

    @Override
    public void onClick(View v) {
        if (v == mStartDaemonBtn) {
            startDaemon();
        } else if (v == mStopDaemonBtn) {
            stopDaemon();
        } else if (v == mGotoSleepBtn) {
            gotoSleep();
        } else if (v == mRebootBtn) {
            reboot();
        } else if (v == mListServicesBtn) {
            listServices();
        } else if (v == mTestRawInvokingBtn) {
            testRawInvoking();
        }
    }

    private void gotoSleep() {
        if (SysServiceProxy.getInstance(this).isDaemonAlive()) {
            IBinder powerBinder = SysServiceProxy.getInstance(this).getService(Context.POWER_SERVICE);
            AppLogger.d(TAG, "power binder: " + powerBinder);
            if (powerBinder != null) {
                Object powerService = PowerManagerIA.asInterface(powerBinder);
                AppLogger.d(TAG, "power service: " + powerService);
                if (powerService != null) {
                    PowerManagerIA.goToSleep(powerService, SystemClock.uptimeMillis());
                }
                return;
            }
        }
        mHandler.obtainMessage(MSG_DAEMON_NOT_RUNNING).sendToTarget();
    }

    private void reboot() {
        if (SysServiceProxy.getInstance(this).isDaemonAlive()) {
            IBinder powerBinder = SysServiceProxy.getInstance(this).checkService(Context.POWER_SERVICE);
            AppLogger.d(TAG, "power binder: " + powerBinder);
            if (powerBinder != null) {
                Object powerService = PowerManagerIA.asInterface(powerBinder);
                AppLogger.d(TAG, "power service: " + powerService);
                if (powerService != null) {
                    PowerManagerIA.reboot(powerService, "reboot");
                }
                return;
            }
        }
        mHandler.obtainMessage(MSG_DAEMON_NOT_RUNNING).sendToTarget();
    }

    private void listServices() {
        if (SysServiceProxy.getInstance(this).isDaemonAlive()) {
            String[] services = ServiceManagerIA.listServices();
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.tips_service_list);
            builder.setMessage(Arrays.toString(services));
            builder.create().show();
        } else {
            mHandler.obtainMessage(MSG_DAEMON_NOT_RUNNING).sendToTarget();
        }
    }

    private void testRawInvoking() {
        IBinder amBinder = ServiceManagerIA.getService(Context.ACTIVITY_SERVICE);
        AppLogger.d(TAG, "am binder: " + amBinder);
        if (amBinder != null) {
            Object amService = ActivityManagerIA.asInterface(amBinder);
            AppLogger.d(TAG, "am service: " + amService);
            if (amService != null) {
                try {
                    ActivityManagerIA.forceStopPackage(amService, getPackageName());
                } catch (SecurityException e) {
                    AppLogger.d(TAG, "cannot invoke the sytem services: " + e);
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
