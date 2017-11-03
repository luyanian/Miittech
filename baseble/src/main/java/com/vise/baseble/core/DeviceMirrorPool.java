package com.vise.baseble.core;

import com.vise.baseble.common.BleConfig;
import com.vise.baseble.common.ConnectState;
import com.vise.baseble.model.BluetoothLeDevice;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * @Description: 设备镜像池，用来管理多个设备连接后的操作
 * @author: <a href="http://www.xiaoyaoyou1212.com">DAWI</a>
 * @date: 17/8/1 23:18.
 */
public class DeviceMirrorPool {
    private final LruHashMap<String, DeviceMirror> DEVICE_MIRROR_MAP;

    public DeviceMirrorPool() {
        DEVICE_MIRROR_MAP = new LruHashMap<>(BleConfig.getInstance().getMaxConnectCount());
    }

    public DeviceMirrorPool(int deviceMirrorSize) {
        DEVICE_MIRROR_MAP = new LruHashMap<>(deviceMirrorSize);
    }

    /**
     * 添加设备镜像
     *
     * @param bluetoothLeDevice
     */
    public synchronized void addDeviceMirror(BluetoothLeDevice bluetoothLeDevice) {
        if (bluetoothLeDevice == null) {
            return;
        }
        String key = bluetoothLeDevice.getAddress();
        if (!DEVICE_MIRROR_MAP.containsKey(key)) {
            DEVICE_MIRROR_MAP.put(key, new DeviceMirror(bluetoothLeDevice));
        }
    }

    /**
     * 添加设备镜像
     *
     * @param deviceMirror
     */
    public synchronized void addDeviceMirror(DeviceMirror deviceMirror) {
        if (deviceMirror == null) {
            return;
        }
        if (!DEVICE_MIRROR_MAP.containsKey(deviceMirror.getUniqueSymbol())) {
            DEVICE_MIRROR_MAP.put(deviceMirror.getUniqueSymbol(), deviceMirror);
        }
    }

    /**
     * 删除设备镜像
     *
     * @param devMac
     */
    public synchronized void removeDeviceMirror(String devMac) {
        if (DEVICE_MIRROR_MAP.containsKey(devMac)) {
            DEVICE_MIRROR_MAP.remove(devMac);
        }
    }

    /**
     * 删除设备镜像
     *
     * @param deviceMirror
     */
    public synchronized void removeDeviceMirror(DeviceMirror deviceMirror) {
        if (deviceMirror == null) {
            return;
        }
        if (DEVICE_MIRROR_MAP.containsKey(deviceMirror.getUniqueSymbol())) {
            DEVICE_MIRROR_MAP.remove(deviceMirror.getUniqueSymbol());
        }
    }

    /**
     * 判断是否包含设备镜像
     *
     * @param deviceMirror
     * @return
     */
    public synchronized boolean isContainDevice(DeviceMirror deviceMirror) {
        if (deviceMirror == null || !DEVICE_MIRROR_MAP.containsKey(deviceMirror.getUniqueSymbol())) {
            return false;
        }
        return true;
    }

    /**
     * 判断是否包含设备镜像
     *
     * @param devMac
     * @return
     */
    public synchronized boolean isContainDevice(String devMac) {
        return DEVICE_MIRROR_MAP.containsKey(devMac);
    }

    /**
     * 获取连接池中该设备镜像的连接状态，如果没有连接则返回CONNECT_DISCONNECT。
     *
     * @param devMac
     * @return
     */
    public synchronized ConnectState getConnectState(String devMac) {
        DeviceMirror deviceMirror = getDeviceMirror(devMac);
        if (deviceMirror != null) {
            return deviceMirror.getConnectState();
        }
        return ConnectState.CONNECT_DISCONNECT;
    }

    /**
     * 获取连接池中的设备镜像，如果没有连接则返回空
     *
     * @param devMac
     * @return
     */
    public synchronized DeviceMirror getDeviceMirror(String devMac) {

        if (DEVICE_MIRROR_MAP.containsKey(devMac)) {
            return DEVICE_MIRROR_MAP.get(devMac);
        }
        return null;
    }

    /**
     * 断开连接池中某一个设备
     *
     * @param devMac
     */
    public synchronized void disconnect(String devMac) {
        if (isContainDevice(devMac)) {
            getDeviceMirror(devMac).disconnect();
        }
    }

    /**
     * 断开连接池中所有设备
     */
    public synchronized void disconnect() {
        for (Map.Entry<String, DeviceMirror> stringDeviceMirrorEntry : DEVICE_MIRROR_MAP.entrySet()) {
            stringDeviceMirrorEntry.getValue().disconnect();
        }
        DEVICE_MIRROR_MAP.clear();
    }

    /**
     * 清除连接池
     */
    public synchronized void clear() {
        for (Map.Entry<String, DeviceMirror> stringDeviceMirrorEntry : DEVICE_MIRROR_MAP.entrySet()) {
            stringDeviceMirrorEntry.getValue().clear();
        }
        DEVICE_MIRROR_MAP.clear();
    }

    /**
     * 获取连接池设备镜像Map集合
     *
     * @return
     */
    public Map<String, DeviceMirror> getDeviceMirrorMap() {
        return DEVICE_MIRROR_MAP;
    }

    /**
     * 获取连接池设备镜像List集合
     *
     * @return
     */
    public synchronized List<DeviceMirror> getDeviceMirrorList() {
        final List<DeviceMirror> deviceMirrors = new ArrayList<>(DEVICE_MIRROR_MAP.values());
        Collections.sort(deviceMirrors, new Comparator<DeviceMirror>() {
            @Override
            public int compare(final DeviceMirror lhs, final DeviceMirror rhs) {
                return lhs.getUniqueSymbol().compareToIgnoreCase(rhs.getUniqueSymbol());
            }
        });
        return deviceMirrors;
    }

    /**
     * 获取连接池设备详细信息List集合
     *
     * @return
     */
    public synchronized List<BluetoothLeDevice> getDeviceList() {
        final List<BluetoothLeDevice> deviceList = new ArrayList<>();
        for (DeviceMirror deviceMirror : getDeviceMirrorList()) {
            if (deviceMirror != null) {
                deviceList.add(deviceMirror.getBluetoothLeDevice());
            }
        }
        return deviceList;
    }

}
