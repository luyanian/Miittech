package com.miittech.you.ble.scan;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.text.TextUtils;

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
        if(scanResultCallback!=null&&device!=null&&!TextUtils.isEmpty(device.getAddress())){
            ScanResult scanResult = new ScanResult();
            scanResult.setDevice(device);
            scanResult.setMac(device.getAddress());
            scanResult.setName(device.getName());
            scanResult.setRssi(rssi);
            scanResult.setScanRecord(scanRecord);
            scanResultCallback.onScaning(scanResult);
        }
    }
}
