
package com.clj.fastble.bluetooth;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.os.Build;
import android.os.Handler;
import android.os.Message;

import com.clj.fastble.BleManager;
import com.clj.fastble.callback.BleIndicateCallback;
import com.clj.fastble.callback.BleMtuChangedCallback;
import com.clj.fastble.callback.BleNotifyCallback;
import com.clj.fastble.callback.BleReadCallback;
import com.clj.fastble.callback.BleRssiCallback;
import com.clj.fastble.callback.BleWriteCallback;
import com.clj.fastble.data.BleDevice;
import com.clj.fastble.exception.OtherException;
import com.clj.fastble.exception.TimeoutException;

import java.util.UUID;

/**
 * Ble Device Connector.
 * be sure main thread
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class BleConnector {

    private static final String UUID_CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR = "00002902-0000-1000-8000-00805f9b34fb";
    private BluetoothGatt bluetoothGatt;
    private BluetoothGattService service;
    private BluetoothGattCharacteristic characteristic;
    private BleBluetooth bleBluetooth;
    private static BleDevice bleDevice;

    public BleConnector(BleBluetooth bleBluetooth, BleDevice bleDevice) {
        this.bleBluetooth = bleBluetooth;
        this.bluetoothGatt = bleBluetooth.getBluetoothGatt();
        this.bleDevice = bleDevice;
    }

    public BleConnector withUUID(UUID serviceUUID, UUID charactUUID) {

        if (serviceUUID != null && bluetoothGatt != null) {
            service = bluetoothGatt.getService(serviceUUID);
        }

        if (service != null && charactUUID != null) {
            characteristic = service.getCharacteristic(charactUUID);
        }

        return this;
    }

    public BleConnector withUUIDString(String serviceUUID, String charactUUID) {
        return withUUID(formUUID(serviceUUID), formUUID(charactUUID));
    }

    private UUID formUUID(String uuid) {
        return uuid == null ? null : UUID.fromString(uuid);
    }




     /*------------------------------- main operation ----------------------------------- */


    /**
     * notify
     */
    public void enableCharacteristicNotify(BleNotifyCallback bleNotifyCallback, String uuid_notify) {
        if (characteristic != null
                && (characteristic.getProperties() | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {

            handleCharacteristicNotifyCallback(bleNotifyCallback, uuid_notify);
            setCharacteristicNotification(bluetoothGatt, characteristic, true, bleNotifyCallback);
        } else {
            if (bleNotifyCallback != null)
                bleNotifyCallback.onNotifyFailure(new OtherException("this characteristic not support notify!"));
        }
    }

    /**
     * stop notify
     */
    public boolean disableCharacteristicNotify() {
        if (characteristic != null
                && (characteristic.getProperties() | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
            return setCharacteristicNotification(bluetoothGatt, characteristic, false, null);
        } else {
            return false;
        }
    }

    /**
     * notify setting
     */
    private boolean setCharacteristicNotification(BluetoothGatt gatt,
                                                  BluetoothGattCharacteristic characteristic,
                                                  boolean enable,
                                                  BleNotifyCallback bleNotifyCallback) {
        if (gatt == null || characteristic == null) {
            if (bleNotifyCallback != null)
                bleNotifyCallback.onNotifyFailure(new OtherException("gatt or characteristic equal null"));
            return false;
        }

        boolean success1 = gatt.setCharacteristicNotification(characteristic, enable);
        if (!success1) {
            if (bleNotifyCallback != null)
                bleNotifyCallback.onNotifyFailure(new OtherException("gatt setCharacteristicNotification fail"));
            return false;
        }

        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(formUUID(UUID_CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR));
        if (descriptor == null) {
            if (bleNotifyCallback != null)
                bleNotifyCallback.onNotifyFailure(new OtherException("descriptor equals null"));
            return false;
        } else {
            descriptor.setValue(enable ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE :
                    BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
            boolean success2 = gatt.writeDescriptor(descriptor);
            if (!success2) {
                if (bleNotifyCallback != null)
                    bleNotifyCallback.onNotifyFailure(new OtherException("gatt writeDescriptor fail"));
            }
            return success2;
        }
    }

    /**
     * indicate
     */
    public void enableCharacteristicIndicate(BleIndicateCallback bleIndicateCallback, String uuid_indicate) {
        if (characteristic != null
                && (characteristic.getProperties() | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
            handleCharacteristicIndicateCallback(bleIndicateCallback, uuid_indicate);
            setCharacteristicIndication(bluetoothGatt, characteristic, true, bleIndicateCallback);
        } else {
            if (bleIndicateCallback != null)
                bleIndicateCallback.onIndicateFailure(new OtherException("this characteristic not support indicate!"));
        }
    }


    /**
     * stop indicate
     */
    public boolean disableCharacteristicIndicate() {
        if (characteristic != null
                && (characteristic.getProperties() | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
            return setCharacteristicIndication(bluetoothGatt, characteristic, false, null);
        } else {
            return false;
        }
    }

    /**
     * indicate setting
     */
    private boolean setCharacteristicIndication(BluetoothGatt gatt,
                                                BluetoothGattCharacteristic characteristic,
                                                boolean enable,
                                                BleIndicateCallback bleIndicateCallback) {
        if (gatt == null || characteristic == null) {
            if (bleIndicateCallback != null)
                bleIndicateCallback.onIndicateFailure(new OtherException("gatt or characteristic equal null"));
            return false;
        }

        boolean success1 = gatt.setCharacteristicNotification(characteristic, enable);
        if (!success1) {
            if (bleIndicateCallback != null)
                bleIndicateCallback.onIndicateFailure(new OtherException("gatt setCharacteristicNotification fail"));
            return false;
        }

        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(formUUID(UUID_CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR));
        if (descriptor == null) {
            if (bleIndicateCallback != null)
                bleIndicateCallback.onIndicateFailure(new OtherException("descriptor equals null"));
            return false;
        } else {
            descriptor.setValue(enable ? BluetoothGattDescriptor.ENABLE_INDICATION_VALUE :
                    BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
            boolean success2 = gatt.writeDescriptor(descriptor);
            if (!success2) {
                if (bleIndicateCallback != null)
                    bleIndicateCallback.onIndicateFailure(new OtherException("gatt writeDescriptor fail"));
            }
            return success2;
        }
    }

    /**
     * write
     */
    public void writeCharacteristic(byte[] data, BleWriteCallback bleWriteCallback, String uuid_write) {
        if (data == null || data.length <= 0) {
            if (bleWriteCallback != null)
                bleWriteCallback.onWriteFailure(bleDevice,new OtherException("the data to be written is empty"));
            return;
        }

        if (characteristic == null
                || (characteristic.getProperties() & (BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) == 0) {
            if (bleWriteCallback != null)
                bleWriteCallback.onWriteFailure(bleDevice,new OtherException("this characteristic not support write!"));
            return;
        }

        if (characteristic.setValue(data)) {
            handleCharacteristicWriteCallback(bleWriteCallback, uuid_write);
            if (!bluetoothGatt.writeCharacteristic(characteristic)) {
                if (bleWriteCallback != null)
                    bleWriteCallback.onWriteFailure(bleDevice,new OtherException("gatt writeCharacteristic fail"));
            }
        } else {
            if (bleWriteCallback != null)
                bleWriteCallback.onWriteFailure(bleDevice,new OtherException("Updates the locally stored value of this characteristic fail"));
        }
    }

    /**
     * read
     */
    public void readCharacteristic(BleReadCallback bleReadCallback, String uuid_read) {
        if (characteristic != null
                && (characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_READ) > 0) {

            handleCharacteristicReadCallback(bleReadCallback, uuid_read);
            if (!bluetoothGatt.readCharacteristic(characteristic)) {
                if (bleReadCallback != null)
                    bleReadCallback.onReadFailure(new OtherException("gatt readCharacteristic fail"));
            }
        } else {
            if (bleReadCallback != null)
                bleReadCallback.onReadFailure(new OtherException("this characteristic not support read!"));
        }
    }

    /**
     * rssi
     */
    public void readRemoteRssi(BleRssiCallback bleRssiCallback) {
        handleRSSIReadCallback(bleRssiCallback);
        if (!bluetoothGatt.readRemoteRssi()) {
            if (bleRssiCallback != null)
                bleRssiCallback.onRssiFailure(bleDevice,new OtherException("gatt readRemoteRssi fail"));
        }
    }

    /**
     * set mtu
     */
    public void setMtu(int requiredMtu, BleMtuChangedCallback bleMtuChangedCallback) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            handleSetMtuCallback(bleMtuChangedCallback);
            if (!bluetoothGatt.requestMtu(requiredMtu)) {
                if (bleMtuChangedCallback != null)
                    bleMtuChangedCallback.onsetMTUFailure(new OtherException("gatt requestMtu fail"));
            }
        } else {
            if (bleMtuChangedCallback != null)
                bleMtuChangedCallback.onsetMTUFailure(new OtherException("API level lower than 21"));
        }
    }


    /**************************************** Handle call back ******************************************/

    /**
     * notify
     */
    private void handleCharacteristicNotifyCallback(BleNotifyCallback bleNotifyCallback,
                                                    String uuid_notify) {
        if (bleNotifyCallback != null) {
            bleNotifyCallback.setBleConnector(this);
            bleNotifyCallback.setKey(uuid_notify);
            bleBluetooth.addNotifyCallback(uuid_notify, bleNotifyCallback);
        }
    }

    /**
     * indicate
     */
    private void handleCharacteristicIndicateCallback(BleIndicateCallback bleIndicateCallback,
                                                      String uuid_indicate) {
        if (bleIndicateCallback != null) {
            bleIndicateCallback.setBleConnector(this);
            bleIndicateCallback.setKey(uuid_indicate);
            bleBluetooth.addIndicateCallback(uuid_indicate, bleIndicateCallback);
        }
    }

    /**
     * write
     */
    private void handleCharacteristicWriteCallback(BleWriteCallback bleWriteCallback,
                                                   String uuid_write) {
        if (bleWriteCallback != null) {
            bleWriteCallback.setBleConnector(this);
            bleWriteCallback.setKey(uuid_write);
            bleBluetooth.addWriteCallback(uuid_write, bleWriteCallback);
        }
    }

    /**
     * read
     */
    private void handleCharacteristicReadCallback(BleReadCallback bleReadCallback,
                                                  String uuid_read) {
        if (bleReadCallback != null) {
            bleReadCallback.setBleConnector(this);
            bleReadCallback.setKey(uuid_read);
            bleBluetooth.addReadCallback(uuid_read, bleReadCallback);
        }
    }

    /**
     * rssi
     */
    private void handleRSSIReadCallback(final BleRssiCallback bleRssiCallback) {
        if (bleRssiCallback != null) {
            bleRssiCallback.setBleConnector(this);
            bleBluetooth.addRssiCallback(bleRssiCallback);
        }
    }

    /**
     * set mtu
     */
    private void handleSetMtuCallback(final BleMtuChangedCallback bleMtuChangedCallback) {
        if (bleMtuChangedCallback != null) {
            bleMtuChangedCallback.setBleConnector(this);
            bleBluetooth.addMtuChangedCallback(bleMtuChangedCallback);
        }
    }

}
