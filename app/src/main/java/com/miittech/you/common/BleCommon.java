package com.miittech.you.common;

import java.util.UUID;

/**
 * Created by Administrator on 2017/11/1.
 */

public class BleCommon {
    public final static UUID serviceUUID= UUID.fromString("00001802-0000-1000-8000-00805f9b34fb");
    public final static UUID userServiceUUID= UUID.fromString("edfec62e-9910-0bac-5241-d8bda6932a2f");

    public final static UUID characteristicUUID= UUID.fromString("00002a06-0000-1000-8000-00805f9b34fb");
    public final static UUID userCharacteristicLogUUID = UUID.fromString("772ae377-b3d2-4f8e-4042-5481d1e0098c");
    public final static UUID userCharactButtonStateUUID = UUID.fromString("6c290d2e-1c03-aca1-ab48-a9b908bae79e");

//    public final static UUID batServiceUUID= UUID.fromString("00002800-0000-1000-8000-00805f9b34fb");
    public final static UUID batServiceUUID= UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb");
    public final static UUID batCharacteristicUUID= UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb");

}
