//package com.miittech.you.manager;
//
//import android.bluetooth.BluetoothAdapter;
//import android.bluetooth.BluetoothDevice;
//import android.bluetooth.BluetoothGatt;
//import android.bluetooth.BluetoothGattCallback;
//import android.bluetooth.BluetoothGattCharacteristic;
//import android.bluetooth.BluetoothManager;
//import android.bluetooth.BluetoothProfile;
//import android.content.Context;
//import android.content.Intent;
//import android.content.pm.PackageManager;
//import android.os.Handler;
//import android.util.Log;
//
//import com.miittech.you.App;
//import com.ryon.mutils.LogUtils;
//
//public class BLEManager {
//
//    private BluetoothManager mBluetoothManager;
//    private String mBluetoothDeviceAddress;
//    private BluetoothGatt mBluetoothGatt;
//    private int mConnectionState = STATE_DISCONNECTED;
//
//    private static final int STATE_DISCONNECTED = 0;
//    private static final int STATE_CONNECTING = 1;
//    private static final int STATE_CONNECTED = 2;
//
//    public final static String ACTION_GATT_CONNECTED = "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
//    public final static String ACTION_GATT_DISCONNECTED = "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
//    public final static String ACTION_GATT_SERVICES_DISCOVERED = "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
//    public final static String ACTION_DATA_AVAILABLE = "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
//    public final static String EXTRA_DATA = "com.example.bluetooth.le.EXTRA_DATA";
//
//    private BluetoothAdapter mBluetoothAdapter;
//    private static BLEManager bleManager;
//    private boolean mScanning;
//    private Context context;
//    public static BLEManager getInstance(){
//        if(bleManager==null){
//            synchronized (BLEManager.class){
//                bleManager = new BLEManager();
//            }
//        }
//        return bleManager;
//    }
//
//    public void setContext(Context context){
//        this.context = context;
//    }
//
//    private boolean isSupportBle(){
//        // you can selectively disable BLE-related features.
//        return this.context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE))
//    }
//
//    private BluetoothAdapter getBluetoothAdapter(){
//        final BluetoothManager bluetoothManager =
//                (BluetoothManager)App.getInstance().getSystemService(Context.BLUETOOTH_SERVICE);
//        mBluetoothAdapter = bluetoothManager.getAdapter();
//        return mBluetoothAdapter;
//    }
//
//    private void scanLeDevice(final boolean enable) {
//        if (enable) {
//            // Stops scanning after a pre-defined scan period.
//            new Handler().postDelayed(new Runnable() {
//                @Override
//                public void run() {
//                    mScanning = false;
//                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
//                }
//            }, 1000);
//
//            mScanning = true;
//            mBluetoothAdapter.startLeScan(mLeScanCallback);
//        } else {
//            mScanning = false;
//            mBluetoothAdapter.stopLeScan(mLeScanCallback);
//        }
//    }
//    public void connectDevice(BluetoothDevice device, boolean autoConnect){
//        mBluetoothGatt = device.connectGatt(this.context,true, mGattCallback);
//    }
//
//    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
//        @Override
//        public void onLeScan(final BluetoothDevice device, int rssi,byte[] scanRecord) {
//            if(device.getName().contains("yoowoo")) {
//                connectDevice(device, true);
//            }
//        }
//    };
//    private final BluetoothGattCallback mGattCallback =  new BluetoothGattCallback() {
//        @Override
//        public void onConnectionStateChange(BluetoothGatt gatt, int status,
//                                            int newState) {
//            String intentAction;
//            if (newState == BluetoothProfile.STATE_CONNECTED) {
//                intentAction = ACTION_GATT_CONNECTED;
//                mConnectionState = STATE_CONNECTED;
//                broadcastUpdate(intentAction);
//                LogUtils.d("Connected to GATT server.");
//                LogUtils.d("Attempting to start service discovery:" + mBluetoothGatt.discoverServices());
//
//            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
//                intentAction = ACTION_GATT_DISCONNECTED;
//                mConnectionState = STATE_DISCONNECTED;
//                LogUtils.d("Disconnected from GATT server.");
//                broadcastUpdate(intentAction);
//            }
//        }
//
//        @Override
//        // New services discovered
//        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
//            if (status == BluetoothGatt.GATT_SUCCESS) {
//                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
//            } else {
//                LogUtils.d("onServicesDiscovered received: " + status);
//            }
//        }
//
//        @Override
//        // Result of a characteristic read operation
//        public void onCharacteristicRead(BluetoothGatt gatt,
//                                         BluetoothGattCharacteristic characteristic,
//                                         int status) {
//            if (status == BluetoothGatt.GATT_SUCCESS) {
//                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
//            }
//        }
//    };
//}