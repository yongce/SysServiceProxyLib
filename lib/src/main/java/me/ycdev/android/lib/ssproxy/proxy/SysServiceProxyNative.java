package me.ycdev.android.lib.ssproxy.proxy;

import android.os.*;
import android.os.Process;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import me.ycdev.android.lib.common.internalapi.android.os.ServiceManagerIA;
import me.ycdev.android.lib.ssproxy.utils.LibConfigs;
import me.ycdev.android.lib.ssproxy.utils.LibLogger;

/**
 * Native implementation of ISysServiceProxy.
 * <p>Note: Based on the code of android.os.ServiceManagerNative.</p>
 */
public class SysServiceProxyNative extends Binder implements ISysServiceProxy {
    private static final String TAG = "SysServiceProxyNative";
    private static final boolean DEBUG = LibConfigs.DEBUG_LOG;

    private final HashMap<String, BinderWrapper> mCachedServices = new HashMap<>();

    private int mOwnerUid;
    private int mSspVersion;

    /**
     * Cast a Binder object into a service manager interface, generating
     * a proxy if needed.
     */
    @Nullable
    public static ISysServiceProxy asInterface(@Nullable IBinder obj) {
        if (obj == null) {
            return null;
        }

        ISysServiceProxy in = (ISysServiceProxy) obj.queryLocalInterface(SSP_DESCRIPTOR);
        if (in != null) {
            return in;
        }

        return new SysServiceProxyProxy(obj);
    }

    public SysServiceProxyNative(int ownerUid, int sspVersion) {
        attachInterface(this, SSP_DESCRIPTOR);
        mOwnerUid = ownerUid;
        mSspVersion = sspVersion;
    }

    @Override
    public IBinder asBinder() {
        return this;
    }

    @Override
    public boolean onTransact(int code, @NonNull Parcel data, @NonNull Parcel reply,
            int flags) throws RemoteException {
        if (DEBUG) LibLogger.d(TAG, "onTransact: " + code + ", caller uid: " + getCallingUid());
        checkCallerPermission(code);
        switch (code) {
            case GET_SSP_VERSION_TRANSACTION: {
                data.enforceInterface(ISysServiceProxy.SSP_DESCRIPTOR);
                int version = getSspVersion();
                reply.writeNoException();
                reply.writeInt(version);
                return true;
            }

            case GET_SERVICE_TRANSACTION: {
                data.enforceInterface(ISysServiceProxy.SSP_DESCRIPTOR);
                String name = data.readString();
                IBinder service = getService(name);
                reply.writeNoException();
                reply.writeStrongBinder(service);
                return true;
            }

            case CHECK_SERVICE_TRANSACTION: {
                data.enforceInterface(ISysServiceProxy.SSP_DESCRIPTOR);
                String name = data.readString();
                IBinder service = checkService(name);
                reply.writeNoException();
                reply.writeStrongBinder(service);
                return true;
            }

            case ADD_SERVICE_TRANSACTION: {
                data.enforceInterface(ISysServiceProxy.SSP_DESCRIPTOR);
                String name = data.readString();
                IBinder service = data.readStrongBinder();
                addService(name, service);
                reply.writeNoException();
                return true;
            }

            case LIST_SERVICES_TRANSACTION: {
                data.enforceInterface(ISysServiceProxy.SSP_DESCRIPTOR);
                String[] list = listServices();
                reply.writeNoException();
                reply.writeStringArray(list);
                return true;
            }
        }
        return super.onTransact(code, data, reply, flags);
    }

    private void checkCallerPermission(int code) {
        int uid = getCallingUid();
        switch (code) {
            case GET_SSP_VERSION_TRANSACTION:
            case GET_SERVICE_TRANSACTION:
            case CHECK_SERVICE_TRANSACTION:
            case ADD_SERVICE_TRANSACTION:
            case LIST_SERVICES_TRANSACTION: {
                if (uid != mOwnerUid) {
                    throw new SecurityException("Unknown caller uid: " + uid + ", != " + mOwnerUid);
                }
                break;
            }

            default: {
                if (uid != Process.SYSTEM_UID && uid != 0 /* root */ && uid != 2000 /* SHELL UID */) {
                    throw new SecurityException("Unknown caller uid: " + uid + ", != " + mOwnerUid);
                }
                break;
            }
        }
    }

    @Override
    public int getSspVersion() {
        return mSspVersion;
    }

    @Override
    public IBinder getService(String name) {
        BinderWrapper binder;
        synchronized (mCachedServices) {
            binder = mCachedServices.get(name);
            if (binder == null || !binder.isBinderAlive()) {
                IBinder targetBinder = ServiceManagerIA.getService(name); // #getService()
                if (targetBinder == null) {
                    binder = null;
                } else {
                    binder = new BinderWrapper(targetBinder);
                    mCachedServices.put(name, binder);
                }
            }
        }
        return binder;
    }

    @Override
    public IBinder checkService(String name) {
        BinderWrapper binder;
        synchronized (mCachedServices) {
            binder = mCachedServices.get(name);
            if (binder == null || !binder.isBinderAlive()) {
                IBinder targetBinder = ServiceManagerIA.checkService(name); // #checkService()
                if (targetBinder == null) {
                    binder = null;
                } else {
                    binder = new BinderWrapper(targetBinder);
                    mCachedServices.put(name, binder);
                }
            }
        }
        return binder;
    }

    @Override
    public void addService(String name, IBinder service) {
        ServiceManagerIA.addService(name, service);
    }

    @Override
    public String[] listServices() {
        return ServiceManagerIA.listServices();
    }

    @Override
    protected void dump(FileDescriptor fd, PrintWriter fout, String[] args) {
        super.dump(fd, fout, args);
        fout.println("Service: " + SSP_DESCRIPTOR);
        fout.println("Version: " + mSspVersion);
        fout.println("Owner: " + mOwnerUid);
        fout.println("Caches: ");
        synchronized (mCachedServices) {
            for (Map.Entry<String, BinderWrapper> entry : mCachedServices.entrySet()) {
                fout.println("\t" + entry.getKey() + ": " + entry.getValue().isBinderAlive());
            }
        }
    }
}

class BinderWrapper extends Binder {
    private IBinder mTarget;

    public BinderWrapper(@NonNull IBinder target) {
        mTarget = target;
    }

    @Override
    public boolean isBinderAlive() {
        return super.isBinderAlive() && mTarget.isBinderAlive();
    }

    @Override
    protected boolean onTransact(int code, @NonNull Parcel data, @NonNull Parcel reply, int flags)
            throws RemoteException {
        return mTarget.transact(code, data, reply, flags);
    }
}

class SysServiceProxyProxy implements ISysServiceProxy {
    private static final String TAG = "SysServiceProxyProxy";

    private IBinder mRemote;

    public SysServiceProxyProxy(IBinder remote) {
        mRemote = remote;
    }

    @Override
    public IBinder asBinder() {
        return mRemote;
    }

    @Override
    public int getSspVersion() {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            data.writeInterfaceToken(ISysServiceProxy.SSP_DESCRIPTOR);
            mRemote.transact(GET_SSP_VERSION_TRANSACTION, data, reply, 0);
            reply.readException();
            return reply.readInt();
        } catch (RemoteException e) {
            LibLogger.w(TAG, "ssproxy died?", e);
        } finally {
            reply.recycle();
            data.recycle();
        }
        return -1;
    }

    @Override
    public IBinder getService(String name) {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            data.writeInterfaceToken(ISysServiceProxy.SSP_DESCRIPTOR);
            data.writeString(name);
            mRemote.transact(GET_SERVICE_TRANSACTION, data, reply, 0);
            reply.readException();
            return reply.readStrongBinder();
        } catch (RemoteException e) {
            LibLogger.w(TAG, "ssproxy died?", e);
        } finally {
            reply.recycle();
            data.recycle();
        }
        return null;
    }

    @Override
    public IBinder checkService(String name) {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            data.writeInterfaceToken(ISysServiceProxy.SSP_DESCRIPTOR);
            data.writeString(name);
            mRemote.transact(CHECK_SERVICE_TRANSACTION, data, reply, 0);
            reply.readException();
            return reply.readStrongBinder();
        } catch (RemoteException e) {
            LibLogger.w(TAG, "ssproxy died?", e);
        } finally {
            reply.recycle();
            data.recycle();
        }
        return null;
    }

    @Override
    public void addService(String name, IBinder service) {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            data.writeInterfaceToken(ISysServiceProxy.SSP_DESCRIPTOR);
            data.writeString(name);
            data.writeStrongBinder(service);
            mRemote.transact(ADD_SERVICE_TRANSACTION, data, reply, 0);
            reply.readException();
        } catch (RemoteException e) {
            LibLogger.w(TAG, "ssproxy died?", e);
        } finally {
            reply.recycle();
            data.recycle();
        }
    }

    @Override
    public String[] listServices() {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            data.writeInterfaceToken(ISysServiceProxy.SSP_DESCRIPTOR);
            mRemote.transact(LIST_SERVICES_TRANSACTION, data, reply, 0);
            reply.readException();
            return reply.createStringArray();
        } catch (RemoteException e) {
            LibLogger.w(TAG, "ssproxy died?", e);
        } finally {
            reply.recycle();
            data.recycle();
        }
        return null;
    }
}

