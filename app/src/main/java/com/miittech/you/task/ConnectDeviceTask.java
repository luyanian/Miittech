package com.miittech.you.task;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.miittech.you.App;
import com.miittech.you.ble.BleClient;
import com.miittech.you.ble.BleUUIDS;
import com.miittech.you.ble.gatt.GattCallback;
import com.miittech.you.entity.DeviceInfo;
import com.miittech.you.global.IntentExtras;
import com.miittech.you.global.Params;
import com.miittech.you.utils.BingGoPlayUtils;
import com.miittech.you.utils.Common;
import com.ryon.mutils.LogUtils;
import com.ryon.mutils.SPUtils;

// 做一件打印自己的id的事。
public class ConnectDeviceTask implements ITask {
    private BluetoothDevice bleDevice;
    private boolean isBind;
    private GattCallback gattCallback;
    // 默认优先级。
    private Priority priority = Priority.DEFAULT;
    private int sequence;

    public ConnectDeviceTask(BluetoothDevice device, boolean isBind,GattCallback gattCallback) {
        this.bleDevice = device;
        this.isBind = isBind;
        this.gattCallback = gattCallback;
    }

    @Override
    public void run() {
        if (TextUtils.isEmpty(Common.getTocken())) {
            return;
        }
        if (bleDevice == null) {
            return;
        }
        synchronized (this) {
            LogUtils.d("bleService", "ConnectDeviceTask----->" + isBind+"   mac-->"+bleDevice.getAddress());
            if (BleClient.getInstance().getConnectState(bleDevice.getAddress()) != BluetoothGatt.STATE_DISCONNECTED) {
                LogUtils.d("bleService", "getConnectState("+bleDevice.getAddress()+") is not disconnected");
                return;
            }
        }
        BleClient.getInstance().connectDevice(bleDevice, gattCallback);
    }

    @Override
    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    @Override
    public Priority getPriority() {
        return priority;
    }
    @Override
    public void setSequence(int sequence) {
        this.sequence = sequence;
    }

    @Override
    public int getSequence() {
        return sequence;
    }

    // 做优先级比较。
    @Override
    public int compareTo(ITask another) {
        final Priority me = this.getPriority();
        final Priority it = another.getPriority();
        return me == it ?  this.getSequence() - another.getSequence() :
                it.ordinal() - me.ordinal();
    }
}