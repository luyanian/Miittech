package com.miittech.you.ble.gatt;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;

/**
 * Created by ryon on 2018/1/13.
 */

public class BleReadCallback extends BaseOptionCallback{
    public synchronized void onReadResponse(BluetoothDevice device,BluetoothGattCharacteristic characteristic, byte[] data){};

}
