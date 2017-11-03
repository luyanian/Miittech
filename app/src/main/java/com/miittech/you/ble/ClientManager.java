package com.miittech.you.ble;

import com.inuker.bluetooth.library.BluetoothClient;
import com.miittech.you.App;

public class ClientManager {

    private static BluetoothClient mClient;

    public static BluetoothClient getClient() {
        if (mClient == null) {
            synchronized (ClientManager.class) {
                if (mClient == null) {
                    mClient = new BluetoothClient(App.getInstance());
                }
            }
        }
        return mClient;
    }
}