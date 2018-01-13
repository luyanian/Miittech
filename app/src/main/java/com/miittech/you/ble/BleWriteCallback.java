package com.miittech.you.ble;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;

/**
 * Created by ryon on 2018/1/13.
 */

public class BleWriteCallback {
    public synchronized void onWriteSuccess(BluetoothGatt bluetoothGatt){};
    public synchronized void onWriteFialed(BluetoothGatt bluetoothGatt){};
}
