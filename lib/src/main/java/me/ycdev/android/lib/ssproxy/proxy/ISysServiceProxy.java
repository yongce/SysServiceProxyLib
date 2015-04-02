package me.ycdev.android.lib.ssproxy.proxy;

import android.os.IBinder;
import android.os.IInterface;

/**
 * Basic interface for finding and publishing system services.
 *
 * <p>Note: Based on the code of android.os.IServiceManager.</p>
 */
public interface ISysServiceProxy extends IInterface {
    static final String SSP_DESCRIPTOR = "me.ycdev.android.lib.ssproxy.proxy.ISysServiceProxy";

    static final int SSP_VERSION = 1;

    static final int GET_SSP_VERSION_TRANSACTION = IBinder.FIRST_CALL_TRANSACTION;
    static final int GET_SERVICE_TRANSACTION = IBinder.FIRST_CALL_TRANSACTION + 1;
    static final int CHECK_SERVICE_TRANSACTION = IBinder.FIRST_CALL_TRANSACTION + 2;
    static final int ADD_SERVICE_TRANSACTION = IBinder.FIRST_CALL_TRANSACTION + 3;
    static final int LIST_SERVICES_TRANSACTION = IBinder.FIRST_CALL_TRANSACTION + 4;

    /**
     * Get version of the SSP binder.
     */
    public int getSspVersion();

    /**
     * Retrieve an existing service called @a name from the
     * service manager.  Blocks for a few seconds waiting for it to be
     * published if it does not already exist.
     */
    public IBinder getService(String name);

    /**
     * Retrieve an existing service called @a name from the service manager. Non-blocking.
     */
    public IBinder checkService(String name);

    /**
     * Place a new @a service called @a name into the service manager.
     */
    public void addService(String name, IBinder service);

    /**
     * Return a list of all currently running services.
     */
    public String[] listServices();

}
