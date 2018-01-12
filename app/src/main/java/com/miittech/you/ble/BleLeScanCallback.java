package com.miittech.you.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

/**
 * Created by Administrator on 2018/1/12.
 */

public class BleLeScanCallback implements BluetoothAdapter.LeScanCallback {
    private ScanResultCallback scanResultCallback;

    public BleLeScanCallback(ScanResultCallback scanResultCallback) {
        this.scanResultCallback = scanResultCallback;
    }

    @Override
    public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
        if(scanResultCallback!=null){
            com.miittech.you.ble.ScanResult scanResult = new com.miittech.you.ble.ScanResult();
            scanResult.setDevice(device);
            scanResult.setMac(device.getAddress());
            scanResult.setName(device.getName());
            scanResult.setRssi(rssi);
            scanResult.setScanRecord(scanRecord);
            scanResultCallback.onScaning(scanResult);
        }
    }
}
