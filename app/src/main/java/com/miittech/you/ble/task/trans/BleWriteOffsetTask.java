package com.miittech.you.ble.task.trans;

import android.bluetooth.BluetoothGatt;
import android.text.TextUtils;

import com.miittech.you.ble.BleClient;
import com.miittech.you.ble.gatt.BleWriteCallback;
import com.miittech.you.ble.task.Priority;
import com.miittech.you.utils.Common;
import com.ryon.mutils.LogUtils;

import java.util.UUID;

// 做一件打印自己的id的事。
public class BleWriteOffsetTask implements IBleTransTask {
    private String mac;
    private UUID serviceUUID;
    private UUID characteristicUUID;
    private int memType;
    private int formatUni32;
    private int offset;
    private BleWriteCallback bleWriteCallback;
    // 默认优先级。
    private Priority priority = Priority.DEFAULT;
    private int sequence;
    private boolean isUpdate = false;

    public BleWriteOffsetTask(String mac, UUID uuid_service, UUID uuid_write, int memType, int formatUint32, int offset, BleWriteCallback bleWriteCallback) {
        this.mac = mac;
        this.serviceUUID = uuid_service;
        this.characteristicUUID = uuid_write;
        this.memType = memType;
        this.formatUni32 = formatUint32;
        this.offset = offset;
        this.bleWriteCallback = bleWriteCallback;
    }

    @Override
    public void run() {
        if (TextUtils.isEmpty(Common.getTocken())) {
            return;
        }
        synchronized (this) {
            LogUtils.d("bleService", "BleWriteOffsetTask----->" +mac);
            if (BleClient.getInstance().getConnectState(mac) == BluetoothGatt.STATE_CONNECTED) {
                BleClient.getInstance().writeOffsetData(mac,serviceUUID,characteristicUUID,memType,formatUni32,offset,bleWriteCallback);
            }else{
                LogUtils.d("bleService", "getConnectState("+mac+") is not disconnected");
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