package com.miittech.you.ble.gatt;

import android.bluetooth.BluetoothDevice;

/**
 * Created by ryon on 2018/1/13.
 */

public class BleWriteCallback extends BaseOptionCallback {
    public synchronized void onWriteSuccess(BluetoothDevice device){};
    public synchronized void onWriteFialed(BluetoothDevice device){};
}
