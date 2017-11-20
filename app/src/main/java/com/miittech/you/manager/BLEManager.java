package com.miittech.you.manager;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.pm.PackageManager;

import com.miittech.you.App;

public class BLEManager {
    private static BLEManager bleManager;


    public static BLEManager getInstance(){
        if(bleManager==null){
            synchronized (BLEManager.class){
                bleManager = new BLEManager();
            }
        }
        return bleManager;
    }

    private boolean isSupportBle(Context context){
        // you can selectively disable BLE-related features.
        return App.getInstance().getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE))
    }

    private BluetoothAdapter getBluetoothAdapter(){
        final BluetoothManager bluetoothManager =
                (BluetoothManager)App.getInstance().getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        return mBluetoothAdapter;
    }
}