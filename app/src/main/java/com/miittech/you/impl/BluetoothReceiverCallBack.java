package com.miittech.you.impl;

import android.bluetooth.BluetoothDevice;

/**
 * Created by Administrator on 2017/10/30.
 */

public interface BluetoothReceiverCallBack {
    void findDevice(BluetoothDevice device);

    void bonded(BluetoothDevice device);
}
