package com.miittech.you.ble;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.ScanResult;

import java.util.List;

/**
 * Created by Administrator on 2018/1/10.
 */
public abstract class ScanResultCallback{
    public abstract void onScaning(com.miittech.you.ble.ScanResult scanResult);
}
