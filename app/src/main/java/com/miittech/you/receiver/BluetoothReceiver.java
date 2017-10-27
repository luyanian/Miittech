package com.miittech.you.receiver;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by Administrator on 2017/10/25.
 */

//public class BluetoothReceiver extends BroadcastReceiver {
//
//    public void onReceive(Context context, Intent intent) {
//        String action = intent.getAction();
//        // When discovery finds a device
//        if (BluetoothDevice.ACTION_FOUND.equals(action)) {
//            // Get the BluetoothDevice object from the Intent
//            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
//            // Add the name and address to an array adapter to show in a ListView
//            mArrayAdapter.add(device.getName() + "\n" + device.getAddress());
//        }
//    }
//}
