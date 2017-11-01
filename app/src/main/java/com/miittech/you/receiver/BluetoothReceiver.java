package com.miittech.you.receiver;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

/**
 * Created by Administrator on 2017/10/25.
 */

public class BluetoothReceiver extends BroadcastReceiver {

    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Bundle b = intent.getExtras();
        Object[] lstName = b.keySet().toArray();

        // 显示所有收到的消息及其细节
        for (int i = 0; i < lstName.length; i++) {
            String keyName = lstName[i].toString();
            Log.e(keyName, String.valueOf(b.get(keyName)));
        }
        BluetoothDevice device = null;
        // 搜索设备时，取得设备的MAC地址
        if (BluetoothDevice.ACTION_FOUND.equals(action)) {
            device = intent
                    .getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if (device.getBondState() == BluetoothDevice.BOND_NONE) {
                String str = "未配对|" + device.getName() + "|"
                        + device.getAddress();
//                if(this.callBack!=null){
//                    this.callBack.findDevice(device);
            }
        }else if(BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)){
            device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            switch (device.getBondState()) {
                case BluetoothDevice.BOND_BONDING:
                    Log.d("BlueToothTestActivity", "正在配对......");
                    break;
                case BluetoothDevice.BOND_BONDED:
                    Log.d("BlueToothTestActivity", "完成配对");
//                    this.callBack.bonded(device);
                    break;
                case BluetoothDevice.BOND_NONE:
                    Log.d("BlueToothTestActivity", "取消配对");
                default:
                    break;
            }
        }

    }
}
