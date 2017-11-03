package com.miittech.you.service;


import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

public class BluetoothService extends Service {

    public BluetoothBinder mBinder = new BluetoothBinder();

    @Override
    public void onCreate() {
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {

        return super.onUnbind(intent);
    }

    public class BluetoothBinder extends Binder {
        public BluetoothService getService() {
            return BluetoothService.this;
        }
    }


}
