package com.miittech.you.ble;

import java.util.UUID;

/**
 * Created by Administrator on 2017/11/1.
 */

public class BleUUIDS {
    public final static UUID serviceUUID= UUID.fromString("00001802-0000-1000-8000-00805f9b34fb");
    public final static UUID userServiceUUID= UUID.fromString("edfec62e-9910-0bac-5241-d8bda6932a2f");
    public final static UUID linkLossUUID= UUID.fromString("00001803-0000-1000-8000-00805f9b34fb");

    public final static UUID characteristicUUID= UUID.fromString("00002a06-0000-1000-8000-00805f9b34fb");
    public final static UUID userCharacteristicLogUUID = UUID.fromString("772ae377-b3d2-4f8e-4042-5481d1e0098c");
    public final static UUID userCharactButtonStateUUID = UUID.fromString("6c290d2e-1c03-aca1-ab48-a9b908bae79e");

    //    public final static UUID batServiceUUID= "00002800-0000-1000-8000-00805f9b34fb";
    public final static UUID batServiceUUID= UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb");
    public final static UUID batCharacteristicUUID= UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb");

    public final static UUID versionServiceUUID= UUID.fromString("0000180a-0000-1000-8000-00805f9b34fb");
    public final static UUID softwareVertionCharacteristicUUID= UUID.fromString("00002a28-0000-1000-8000-00805f9b34fb");
    public final static UUID firmwareVertionCharacteristicUUID= UUID.fromString("00002a26-0000-1000-8000-00805f9b34fb");


    public final static UUID CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    public static final UUID SPOTA_SERV_STATUS_UUID = UUID.fromString("5f78df94-798c-46f5-990a-b3eb6a065c88");
    public static final UUID SPOTA_DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    public static final UUID SPOTA_SERVICE_UUID = UUID.fromString("0000fef5-0000-1000-8000-00805f9b34fb");
    public static final UUID SPOTA_GPIO_MAP_UUID = UUID.fromString("724249f0-5eC3-4b5f-8804-42345af08651");
    public static final UUID SPOTA_PATCH_LEN_UUID = UUID.fromString("9d84b9a3-000c-49d8-9183-855b673fda31");
    public static final UUID SPOTA_PATCH_DATA_UUID = UUID.fromString("457871e8-d516-4ca1-9116-57d0b17b9cb2");
    public static final UUID SPOTA_MEM_DEV_UUID = UUID.fromString("8082caa8-41a6-4021-91c6-56f9b954cc34");

}
