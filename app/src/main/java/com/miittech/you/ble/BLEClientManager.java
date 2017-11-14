package com.miittech.you.ble;

import com.inuker.bluetooth.library.BluetoothClient;
import com.miittech.you.App;

/**
 * Created by ryon on 2017/11/13.
 */

public class BLEClientManager {
    private static BluetoothClient mClient;
    public static BluetoothClient getClient() {
        if (mClient == null) {
            synchronized (BLEClientManager.class) {
                if (mClient== null) {
                    mClient = new BluetoothClient(App.getInstance());
                }
            }
        }
        return mClient;
    }
}
