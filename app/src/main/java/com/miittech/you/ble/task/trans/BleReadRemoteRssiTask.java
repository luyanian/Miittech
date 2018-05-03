package com.miittech.you.ble.task.trans;

import android.bluetooth.BluetoothGatt;
import android.text.TextUtils;

import com.miittech.you.ble.BleClient;
import com.miittech.you.ble.gatt.BleReadCallback;
import com.miittech.you.ble.task.Priority;
import com.miittech.you.utils.Common;
import com.ryon.mutils.LogUtils;

import java.util.UUID;

// 做一件打印自己的id的事。
public class BleReadRemoteRssiTask implements IBleTransTask {
    private String mac;
    // 默认优先级。
    private Priority priority = Priority.DEFAULT;
    private int sequence;
    private boolean isUpdate = false;

    public BleReadRemoteRssiTask(String mac) {
        this.mac = mac;
    }

    @Override
    public void run() {
        if (TextUtils.isEmpty(Common.getTocken())) {
            return;
        }
        synchronized (this) {
            LogUtils.d("bleService", "BleReadRemoteRssiTask----->" +mac);
            if (BleClient.getInstance().isConnected(mac)) {
                BleClient.getInstance().readRemoteRssiData(mac);
            }else{
                LogUtils.d("bleService", "getConnectState("+mac+") is not connect");
            }
        }

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
    public String getMacAddress() {
        return this.mac;
    }

    @Override
    public void setIsUpdate(boolean isUpdate) {
        this.isUpdate = isUpdate;
    }

    @Override
    public boolean isUpdate() {
        return isUpdate;
    }

    // 做优先级比较。
    @Override
    public int compareTo(IBleTransTask another) {
        final Priority me = this.getPriority();
        final Priority it = another.getPriority();
        return me == it ?  this.getSequence() - another.getSequence() :
                it.ordinal() - me.ordinal();
    }
}