package com.clj.fastble.scan;


import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import com.clj.fastble.BleManager;
import com.clj.fastble.data.BleDevice;
import com.clj.fastble.utils.BleLog;
import com.clj.fastble.utils.HexUtil;

import java.sql.Time;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

@SuppressLint("NewApi")
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public abstract class BleScanPresenter extends ScanCallback implements BluetoothAdapter.LeScanCallback {

    private String[] mDeviceNames = null;
    private String mDeviceMac = null;
    private boolean mFuzzy = false;
    private boolean mNeedConnect = false;
    private Timer timer = new Timer();
    private List<BleDevice> mBleDeviceList = new ArrayList<>();
    private long mScanTimeout = BleManager.DEFAULT_SCAN_TIME;

    public BleScanPresenter(String[] names, String mac, boolean fuzzy, boolean needConnect, long timeOut) {
        this.mDeviceNames = names;
        this.mDeviceMac = mac;
        this.mFuzzy = fuzzy;
        this.mNeedConnect = needConnect;
        this.mScanTimeout = timeOut;
    }

    @Override
    public void onScanResult(int callbackType, ScanResult result) {
        super.onScanResult(callbackType, result);
        onLeScan(result.getDevice(),result.getRssi(),result.getScanRecord().getBytes());
    }

    @Override
    public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
        if (device == null)
            return;

        BleDevice scanResult = new BleDevice(device, rssi, scanRecord, System.currentTimeMillis());

        synchronized (this) {
            if (TextUtils.isEmpty(mDeviceMac) && (mDeviceNames == null || mDeviceNames.length < 1)) {
                next(scanResult);
                return;
            }

            if (!TextUtils.isEmpty(mDeviceMac)) {
                if (!mDeviceMac.equalsIgnoreCase(device.getAddress()))
                    return;
            }

            if (mDeviceNames != null && mDeviceNames.length > 0) {
                AtomicBoolean equal = new AtomicBoolean(false);
                for (String name : mDeviceNames) {
                    String remoteName = device.getName();
                    if (remoteName == null)
                        remoteName = "";
                    if (mFuzzy ? remoteName.contains(name) : remoteName.equalsIgnoreCase(name)) {
                        equal.set(true);
                    }
                }
                if (!equal.get()) {
                    return;
                }
            }

            next(scanResult);
        }
    }

    private void next(BleDevice bleDevice) {
        if (mNeedConnect) {
            BleLog.i("onScanning--------"
                    + "  name:" + bleDevice.getName()
                    + "  mac:" + bleDevice.getMac()
                    + "  Rssi:" + bleDevice.getRssi()
                    + "  scanRecord:" + HexUtil.formatHexString(bleDevice.getScanRecord()));

            mBleDeviceList.add(bleDevice);
            BleManager.getInstance().getBleScanner().stopLeScan();

        } else {
            BleLog.i("onScanning  ------"
                    + "  name: " + bleDevice.getName()
                    + "  mac: " + bleDevice.getMac()
                    + "  Rssi: " + bleDevice.getRssi()
                    + "  scanRecord: " + HexUtil.formatHexString(bleDevice.getScanRecord()));

            mBleDeviceList.add(bleDevice);
            onScanning(bleDevice);
        }
    }

    public final void notifyScanStarted(boolean success) {
        mBleDeviceList.clear();
        timer.cancel();
        if (success && mScanTimeout > 0) {
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    BleManager.getInstance().getBleScanner().stopLeScan();
                }
            },mScanTimeout);
        }

        onScanStarted(success);
    }

    public final void notifyScanStopped() {
        timer.cancel();
        onScanFinished(mBleDeviceList);
    }

    public abstract void onScanStarted(boolean success);

    public abstract void onScanning(BleDevice bleDevice);

    public abstract void onScanFinished(List<BleDevice> bleDeviceList);


}
