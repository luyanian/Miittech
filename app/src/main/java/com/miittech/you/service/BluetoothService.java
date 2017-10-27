package com.miittech.you.service;


import android.app.Service;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import com.clj.fastble.BleManager;
import com.clj.fastble.conn.BleCharacterCallback;
import com.clj.fastble.conn.BleGattCallback;
import com.clj.fastble.conn.BleRssiCallback;
import com.clj.fastble.data.ScanResult;
import com.clj.fastble.exception.BleException;
import com.clj.fastble.scan.ListScanCallback;
import com.clj.fastble.utils.HexUtil;

public class BluetoothService extends Service {

    public BluetoothBinder mBinder = new BluetoothBinder();
    private BleManager bleManager;
    private Handler threadHandler = new Handler(Looper.getMainLooper());
    private Callback mCallback = null;
    private Callback2 mCallback2 = null;

    private String name;
    private String mac;
    private BluetoothGatt gatt;
    private BluetoothGattService service;
    private BluetoothGattCharacteristic characteristic;
    private int charaProp;

    @Override
    public void onCreate() {
        bleManager = new BleManager(this);
        bleManager.enableBluetooth();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        bleManager = null;
        mCallback = null;
        mCallback2 = null;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        bleManager.closeBluetoothGatt();
        return super.onUnbind(intent);
    }

    public class BluetoothBinder extends Binder {
        public BluetoothService getService() {
            return BluetoothService.this;
        }
    }

    public void setScanCallback(Callback callback) {
        mCallback = callback;
    }

    public void setConnectCallback(Callback2 callback) {
        mCallback2 = callback;
    }

    public interface Callback {

        void onStartScan();

        void onScanning(ScanResult scanResult);

        void onFoundDevice(ScanResult scanResult);

        void onScanComplete(ScanResult[] scanResults);

        void onConnecting();

        void onConnectFail(BleException exception);

        void onDisConnected(BluetoothGatt gatt, int status, BleException exception);

        void onServicesDiscovered(BluetoothGatt gatt, int status);

        void onConnectSuccess(BluetoothGatt gatt, int status);
    }

    public interface Callback2 {

        void onDisConnected(BluetoothGatt gatt, int status, BleException exception);
    }

    public void scanDevice() {
        resetInfo();

        if (mCallback != null) {
            mCallback.onStartScan();
        }

        boolean b = bleManager.scanDevice(new ListScanCallback(5000) {

            @Override
            public void onScanning(final ScanResult result) {
                runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mCallback != null) {
                            mCallback.onScanning(result);
                        }
                    }
                });
            }

            @Override
            public void onScanComplete(final ScanResult[] results) {
                runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mCallback != null) {
                            mCallback.onScanComplete(results);
                        }
                    }
                });
            }
        });
        if (!b) {
            if (mCallback != null) {
                mCallback.onScanComplete(new ScanResult[]{});
            }
        }
    }

    public void cancelScan() {
        bleManager.cancelScan();
    }

    public void connectDevice(final ScanResult scanResult) {
        if (mCallback != null) {
            mCallback.onConnecting();
        }

        bleManager.connectDevice(scanResult, true, new BleGattCallback() {

            @Override
            public void onFoundDevice(final ScanResult scanResult) {
                BluetoothService.this.name = scanResult.getDevice().getName();
                BluetoothService.this.mac = scanResult.getDevice().getAddress();
                runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        if(mCallback!=null){
                            mCallback.onFoundDevice(scanResult);
                        }
                    }
                });

            }

            @Override
            public void onConnecting(BluetoothGatt gatt, int status) {

            }

            @Override
            public void onConnectError(final BleException exception) {
                runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mCallback != null) {
                            mCallback.onConnectFail(exception);
                        }
                    }
                });
            }

            @Override
            public void onConnectSuccess(final BluetoothGatt gatt, final int status) {
                runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        if(mCallback!=null){
                            mCallback.onConnectSuccess(gatt,status);
                        }
                    }
                });
            }

            @Override
            public void onServicesDiscovered(final BluetoothGatt gatt, final int status) {
                BluetoothService.this.gatt = gatt;
                runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mCallback != null) {
                            mCallback.onServicesDiscovered(gatt,status);
                        }
                    }
                });
            }

            @Override
            public void onDisConnected(final BluetoothGatt gatt, final int status, final BleException exception) {
                runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mCallback != null) {
                            mCallback.onDisConnected(gatt,status,exception);
                        }
                        if (mCallback2 != null) {
                            mCallback2.onDisConnected(gatt,status,exception);
                        }
                    }
                });
            }

        });
    }

    public void scanAndConnect1(String name) {
        resetInfo();

        if (mCallback != null) {
            mCallback.onStartScan();
        }

        bleManager.scanNameAndConnect(name, 10000, false, new BleGattCallback() {

            @Override
            public void onFoundDevice(final ScanResult scanResult) {
                runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mCallback != null) {
                            mCallback.onFoundDevice(scanResult);
                        }
                    }
                });
                BluetoothService.this.name = scanResult.getDevice().getName();
                BluetoothService.this.mac = scanResult.getDevice().getAddress();
                runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mCallback != null) {
                            mCallback.onConnecting();
                        }
                    }
                });
            }

            @Override
            public void onConnecting(BluetoothGatt gatt, int status) {

            }

            @Override
            public void onConnectError(final BleException exception) {
                runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mCallback != null) {
                            mCallback.onConnectFail(exception);
                        }
                    }
                });
            }

            @Override
            public void onConnectSuccess(final BluetoothGatt gatt, final int status) {
                runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        if(mCallback!=null){
                            mCallback.onConnectSuccess(gatt,status);
                        }
                    }
                });

            }

            @Override
            public void onServicesDiscovered(final BluetoothGatt gatt, final int status) {
                BluetoothService.this.gatt = gatt;
                runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mCallback != null) {
                            mCallback.onServicesDiscovered(gatt, status);
                        }
                    }
                });
            }

            @Override
            public void onDisConnected(final BluetoothGatt gatt, final int status, final BleException exception) {
                runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mCallback != null) {
                            mCallback.onDisConnected(gatt, status, exception);
                        }
                        if (mCallback2 != null) {
                            mCallback2.onDisConnected(gatt, status, exception);
                        }
                    }
                });
            }
        });
    }

    public void scanAndConnect2(String name) {
        resetInfo();

        if (mCallback != null) {
            mCallback.onStartScan();
        }

        bleManager.scanfuzzyNameAndConnect(name, 5000, false, new BleGattCallback() {

            @Override
            public void onFoundDevice(final ScanResult scanResult) {
                runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mCallback != null) {
                            mCallback.onFoundDevice(scanResult);
                        }
                    }
                });
                BluetoothService.this.name = scanResult.getDevice().getName();
                BluetoothService.this.mac = scanResult.getDevice().getAddress();
                runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mCallback != null) {
                            mCallback.onConnecting();
                        }
                    }
                });
            }

            @Override
            public void onConnecting(BluetoothGatt gatt, int status) {

            }

            @Override
            public void onConnectError(final BleException exception) {
                runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mCallback != null) {
                            mCallback.onConnectFail(exception);
                        }
                    }
                });
            }

            @Override
            public void onConnectSuccess(final BluetoothGatt gatt, final int status) {
                runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        if(mCallback!=null){
                            mCallback.onConnectSuccess(gatt,status);
                        }
                    }
                });

            }

            @Override
            public void onDisConnected(final BluetoothGatt gatt, final int status, final BleException exception) {
                runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mCallback != null) {
                            mCallback.onDisConnected(gatt, status, exception);
                        }
                        if (mCallback2 != null) {
                            mCallback2.onDisConnected(gatt, status, exception);
                        }
                    }
                });
            }

            @Override
            public void onServicesDiscovered(final BluetoothGatt gatt, final int status) {
                BluetoothService.this.gatt = gatt;
                runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mCallback != null) {
                            mCallback.onServicesDiscovered(gatt, status);
                        }
                    }
                });
            }
        });
    }

    public void scanAndConnect3(String[] names) {
        resetInfo();

        if (mCallback != null) {
            mCallback.onStartScan();
        }

        bleManager.scanNamesAndConnect(names, 5000, false, new BleGattCallback() {

            @Override
            public void onFoundDevice(final ScanResult scanResult) {
                runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mCallback != null) {
                            mCallback.onFoundDevice(scanResult);
                        }
                    }
                });
                BluetoothService.this.name = scanResult.getDevice().getName();
                BluetoothService.this.mac = scanResult.getDevice().getAddress();
                runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mCallback != null) {
                            mCallback.onConnecting();
                        }
                    }
                });
            }

            @Override
            public void onConnecting(BluetoothGatt gatt, int status) {

            }

            @Override
            public void onConnectError(final BleException exception) {
                runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mCallback != null) {
                            mCallback.onConnectFail(exception);
                        }
                    }
                });
            }

            @Override
            public void onConnectSuccess(final BluetoothGatt gatt, final int status) {
                runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        if(mCallback!=null){
                            mCallback.onConnectSuccess(gatt,status);
                        }
                    }
                });


            }

            @Override
            public void onDisConnected(final BluetoothGatt gatt, final int status, final BleException exception) {
                runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mCallback != null) {
                            mCallback.onDisConnected(gatt, status, exception);
                        }
                        if (mCallback2 != null) {
                            mCallback2.onDisConnected(gatt, status, exception);
                        }
                    }
                });
            }

            @Override
            public void onServicesDiscovered(final BluetoothGatt gatt, final int status) {
                BluetoothService.this.gatt = gatt;
                runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mCallback != null) {
                            mCallback.onServicesDiscovered(gatt, status);
                        }
                    }
                });
            }
        });

    }

    public void scanAndConnect4(String[] names) {
        resetInfo();

        if (mCallback != null) {
            mCallback.onStartScan();
        }

        bleManager.scanfuzzyNamesAndConnect(names, 5000, false, new BleGattCallback() {

            @Override
            public void onFoundDevice(final ScanResult scanResult) {
                runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mCallback != null) {
                            mCallback.onFoundDevice(scanResult);
                        }
                    }
                });
                BluetoothService.this.name = scanResult.getDevice().getName();
                BluetoothService.this.mac = scanResult.getDevice().getAddress();
                runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mCallback != null) {
                            mCallback.onConnecting();
                        }
                    }
                });
            }

            @Override
            public void onConnecting(BluetoothGatt gatt, int status) {

            }

            @Override
            public void onConnectError(final BleException exception) {
                runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mCallback != null) {
                            mCallback.onConnectFail(exception);
                        }
                    }
                });
            }

            @Override
            public void onConnectSuccess(final BluetoothGatt gatt, final int status) {
                runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        if(mCallback!=null){
                            mCallback.onConnectSuccess(gatt,status);
                        }
                    }
                });
            }

            @Override
            public void onDisConnected(final BluetoothGatt gatt, final int status, final BleException exception) {
                runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mCallback != null) {
                            mCallback.onDisConnected(gatt, status, exception);
                        }
                        if (mCallback2 != null) {
                            mCallback2.onDisConnected(gatt, status, exception);
                        }
                    }
                });
            }

            @Override
            public void onServicesDiscovered(final BluetoothGatt gatt, final int status) {
                BluetoothService.this.gatt = gatt;
                runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mCallback != null) {
                            mCallback.onServicesDiscovered(gatt, status);
                        }
                    }
                });
            }
        });
    }

    public void scanAndConnect5(String mac) {
        resetInfo();

        if (mCallback != null) {
            mCallback.onStartScan();
        }

        bleManager.scanMacAndConnect(mac, 5000, false, new BleGattCallback() {

            @Override
            public void onFoundDevice(final ScanResult scanResult) {
                runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mCallback != null) {
                            mCallback.onFoundDevice(scanResult);
                        }
                    }
                });
                BluetoothService.this.name = scanResult.getDevice().getName();
                BluetoothService.this.mac = scanResult.getDevice().getAddress();
                runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mCallback != null) {
                            mCallback.onConnecting();
                        }
                    }
                });
            }

            @Override
            public void onConnecting(BluetoothGatt gatt, int status) {

            }

            @Override
            public void onConnectError(final BleException exception) {
                runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mCallback != null) {
                            mCallback.onConnectFail(exception);
                        }
                    }
                });
            }

            @Override
            public void onConnectSuccess(final BluetoothGatt gatt, final int status) {
                runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        if(mCallback!=null){
                            mCallback.onConnectSuccess(gatt,status);
                        }
                    }
                });
            }

            @Override
            public void onDisConnected(final BluetoothGatt gatt, final int status, final BleException exception) {
                runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mCallback != null) {
                            mCallback.onDisConnected(gatt, status, exception);
                        }
                        if (mCallback2 != null) {
                            mCallback2.onDisConnected(gatt, status, exception);
                        }
                    }
                });
            }

            @Override
            public void onServicesDiscovered(final BluetoothGatt gatt, final int status) {
                BluetoothService.this.gatt = gatt;
                runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mCallback != null) {
                            mCallback.onServicesDiscovered(gatt, status);
                        }
                    }
                });
            }

        });
    }

    public boolean read(String uuid_service, String uuid_read, BleCharacterCallback callback) {
        return bleManager.readDevice(uuid_service, uuid_read, callback);
    }

    public boolean write(String uuid_service, String uuid_write, String hex, BleCharacterCallback callback) {
        return bleManager.writeDevice(uuid_service, uuid_write, HexUtil.hexStringToBytes(hex), callback);
    }

    public boolean notify(String uuid_service, String uuid_notify, BleCharacterCallback callback) {
        return bleManager.notify(uuid_service, uuid_notify, callback);
    }

    public boolean indicate(String uuid_service, String uuid_indicate, BleCharacterCallback callback) {
        return bleManager.indicate(uuid_service, uuid_indicate, callback);
    }

    public boolean stopNotify(String uuid_service, String uuid_notify) {
        return bleManager.stopNotify(uuid_service, uuid_notify);
    }

    public boolean stopIndicate(String uuid_service, String uuid_indicate) {
        return bleManager.stopIndicate(uuid_service, uuid_indicate);
    }

    public boolean readRssi(BleRssiCallback callback) {
        return bleManager.readRssi(callback);
    }

    public void closeConnect() {
        bleManager.closeBluetoothGatt();
    }


    private void resetInfo() {
        name = null;
        mac = null;
        gatt = null;
        service = null;
        characteristic = null;
        charaProp = 0;
    }

    public String getName() {
        return name;
    }

    public String getMac() {
        return mac;
    }

    public BluetoothGatt getGatt() {
        return gatt;
    }

    public void setService(BluetoothGattService service) {
        this.service = service;
    }

    public BluetoothGattService getService() {
        return service;
    }

    public void setCharacteristic(BluetoothGattCharacteristic characteristic) {
        this.characteristic = characteristic;
    }

    public BluetoothGattCharacteristic getCharacteristic() {
        return characteristic;
    }

    public void setCharaProp(int charaProp) {
        this.charaProp = charaProp;
    }

    public int getCharaProp() {
        return charaProp;
    }


    private void runOnMainThread(Runnable runnable) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            runnable.run();
        } else {
            threadHandler.post(runnable);
        }
    }


}
