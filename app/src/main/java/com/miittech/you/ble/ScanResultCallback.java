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

@SuppressLint("NewApi")
public class ScanResultCallback extends ScanCallback implements BluetoothAdapter.LeScanCallback {
    @Override
    public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
        com.miittech.you.ble.ScanResult scanResult = new com.miittech.you.ble.ScanResult();
        scanResult.setDevice(device);
        scanResult.setMac(device.getAddress());
        scanResult.setName(device.getName());
        scanResult.setRssi(rssi);
        scanResult.setScanRecord(scanRecord);
        onScaning(scanResult);
    }

    @Override
    public void onScanResult(int callbackType, ScanResult result) {
        super.onScanResult(callbackType, result);
        com.miittech.you.ble.ScanResult scanResult = new com.miittech.you.ble.ScanResult();
        scanResult.setDevice(result.getDevice());
        scanResult.setMac(result.getDevice().getAddress());
        scanResult.setName(result.getDevice().getName());
        scanResult.setRssi(result.getRssi());
        scanResult.setScanRecord(result.getScanRecord().getBytes());
        onScaning(scanResult);
    }

    @Override
    public void onBatchScanResults(List<ScanResult> results) {
        super.onBatchScanResults(results);
    }

    @Override
    public void onScanFailed(int errorCode) {
        super.onScanFailed(errorCode);
    }

    public void onScaning(com.miittech.you.ble.ScanResult scanResult){

    }
}
