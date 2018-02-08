package com.miittech.you.ble.scan;

import android.bluetooth.le.ScanCallback;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.text.TextUtils;

import com.miittech.you.ble.BleClient;

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
            ScanResult scanResult = new ScanResult();
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
