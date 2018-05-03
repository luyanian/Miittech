package com.miittech.you.ble.task.connect;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.content.Intent;
import android.text.TextUtils;

import com.miittech.you.App;
import com.miittech.you.ble.BleClient;
import com.miittech.you.ble.BleService;
import com.miittech.you.ble.gatt.GattCallback;
import com.miittech.you.ble.task.Priority;
import com.miittech.you.global.IntentExtras;
import com.miittech.you.utils.Common;
import com.ryon.mutils.LogUtils;

import org.reactivestreams.Subscriber;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

// 做一件打印自己的id的事。
public class BleConnectTask implements IBleConnectTask {
    private BluetoothDevice bleDevice;
    private boolean isBind;
    private BleService bleService;
    // 默认优先级。
    private Priority priority = Priority.DEFAULT;
    private int sequence;

    public BleConnectTask(BluetoothDevice device, boolean isBind, BleService bleService) {
        this.bleDevice = device;
        this.isBind = isBind;
        this.bleService = bleService;
    }

    @Override
    public void run() {
        if (TextUtils.isEmpty(Common.getTocken())) {
            return;
        }
        if (bleDevice == null) {
            return;
        }
        Intent intent2 = new Intent(IntentExtras.ACTION.ACTION_BLE_COMMAND);
        intent2.putExtra("cmd", IntentExtras.CMD.CMD_DEVICE_CONNECT);
        intent2.putExtra("address", bleDevice.getAddress());
        intent2.putExtra("isBind", isBind);
        App.getInstance().getLocalBroadCastManager().sendBroadcast(intent2);
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

    @Override
    public String getMac() {
        return bleDevice.getAddress();
    }

    // 做优先级比较。
    @Override
    public int compareTo(IBleConnectTask another) {
        final Priority me = this.getPriority();
        final Priority it = another.getPriority();
        return me == it ?  this.getSequence() - another.getSequence() :
                it.ordinal() - me.ordinal();
    }
}