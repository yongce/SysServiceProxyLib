package me.ycdev.android.lib.ssproxy.proxy;

import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.HashMap;

import me.ycdev.android.lib.common.internalapi.android.os.ServiceManagerIA;
import me.ycdev.android.lib.ssproxy.utils.LibLogger;

/**
 * Native implementation of ISysServiceProxy.
 * <p>Note: Based on the code of android.os.ServiceManagerNative.</p>
 */
public class SysServiceProxyNative extends Binder implements ISysServiceProxy {
    private final HashMap<String, BinderWrapper> mCachedServices = new HashMap<>();

    private int mOwnerUid;

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

    public SysServiceProxyNative() {
        attachInterface(this, SSP_DESCRIPTOR);
    }

    public void setOwnerUid(int ownerUid) {
        mOwnerUid = ownerUid;
    }

    @Override
    public IBinder asBinder() {
        return this;
    }

    @Override
    public boolean onTransact(int code, @NonNull Parcel data, @NonNull Parcel reply,
            int flags) throws RemoteException {
        checkCallerPermission();
        switch (code) {
            case ISysServiceProxy.GET_SERVICE_TRANSACTION: {
                data.enforceInterface(ISysServiceProxy.SSP_DESCRIPTOR);
                String name = data.readString();
                IBinder service = getService(name);
                reply.writeNoException();
                reply.writeStrongBinder(service);
                return true;
            }

            case ISysServiceProxy.CHECK_SERVICE_TRANSACTION: {
                data.enforceInterface(ISysServiceProxy.SSP_DESCRIPTOR);
                String name = data.readString();
                IBinder service = checkService(name);
                reply.writeNoException();
                reply.writeStrongBinder(service);
                return true;
            }

            case ISysServiceProxy.ADD_SERVICE_TRANSACTION: {
                data.enforceInterface(ISysServiceProxy.SSP_DESCRIPTOR);
                String name = data.readString();
                IBinder service = data.readStrongBinder();
                addService(name, service);
                reply.writeNoException();
                return true;
            }

            case ISysServiceProxy.LIST_SERVICES_TRANSACTION: {
                data.enforceInterface(ISysServiceProxy.SSP_DESCRIPTOR);
                String[] list = listServices();
                reply.writeNoException();
                reply.writeStringArray(list);
                return true;
            }
        }
        return super.onTransact(code, data, reply, flags);
    }

    private void checkCallerPermission() {
        int uid = getCallingUid();
        if (uid != mOwnerUid) {
            throw new SecurityException("Unknown caller uid: " + uid + ", != " + mOwnerUid);
        }
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

