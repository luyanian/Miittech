package com.clj.fastble.callback;


import com.clj.fastble.bluetooth.BleConnector;
import com.clj.fastble.data.BleDevice;
import com.clj.fastble.exception.BleException;

public abstract class BleRssiCallback {

    public abstract void onRssiFailure(BleDevice bleDevice,BleException exception);

    public abstract void onRssiSuccess(BleDevice bleDevice,int rssi);

    private BleConnector bleConnector;

    public void setBleConnector(BleConnector bleConnector) {
        this.bleConnector = bleConnector;
    }

    public BleConnector getBleConnector() {
        return bleConnector;
    }
}