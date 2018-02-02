package com.miittech.you.ble;

import android.bluetooth.BluetoothDevice;

/**
 * Created by ryon on 2018/1/13.
 */

public class BleReadCallback {
    public synchronized void onReadResponse(byte[] data){};
    public synchronized void onReadBleVertion(byte[] firmware,byte[] software){};
}
