package com.miittech.you.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.text.TextUtils;

import java.util.List;

/**
 * Created by Administrator on 2018/1/12.
 */

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class BleScanCallback extends ScanCallback {
    private ScanResultCallback scanResultCallback;

    public BleScanCallback(ScanResultCallback scanResultCallback) {
        this.scanResultCallback = scanResultCallback;
    }
    @Override
    public void onScanResult(int callbackType, final android.bluetooth.le.ScanResult result) {
        super.onScanResult(callbackType, result);
        if(scanResultCallback!=null&&result.getDevice()!=null&& !TextUtils.isEmpty(result.getDevice().getAddress())) {
            com.miittech.you.ble.ScanResult scanResult = new com.miittech.you.ble.ScanResult();
            scanResult.setDevice(result.getDevice());
            scanResult.setMac(result.getDevice().getAddress());
            scanResult.setName(result.getDevice().getName());
            scanResult.setRssi(result.getRssi());
            scanResult.setScanRecord(result.getScanRecord().getBytes());
            scanResultCallback.onScaning(scanResult);
        }
    }

    @Override
    public void onBatchScanResults(List<android.bluetooth.le.ScanResult> results) {
        super.onBatchScanResults(results);
    }

    @Override
    public void onScanFailed(int errorCode) {
        super.onScanFailed(errorCode);
        if(errorCode==2) {
            BleClient.getInstance().restartBle();
        }
    }
}
