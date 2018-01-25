package com.miittech.you.global;

/**
 * Created by Administrator on 2017/10/20.
 */

public class IntentExtras {
    public static final String FROM="from";
    public static class FRIEND{
        public static final String DATA="friend_data";
    }
    public static class DEVICE{
        public static final String DATA="data";
        public static final String CLASSIFY="classify";
        public static final String ID="devId";
        public static final String NAME="devName";
        public static final String IMAGE="image";
    }

    public static class SOURND{
        public static final String ID = "id";
        public static final String NAME="name";
    }

    public static class IGNORE{
        public static final String DATA="ignore_data";
    }

    public static class ACTION{
        public final static String ACTION_BLE_COMMAND="com.mittech.you.device.service.cmd";
        public final static String ACTION_CMD_RESPONSE="com.mittech.you.device.service.response";
        public final static String ACTION_SOUND_PLAY_ONCLICK="com.mittech.you.device.sound.onclick";
        public final static String ACTION_SOUND_PLAY_DIALOG="com.mittech.you.device.sound.dialog";
        public final static String ACTION_TASK_SEND="com.mittech.you.task.send";
        public final static String ACTION_RECEIVE_MESSAGE="com.mittech.you.receive.msg";
    }
    public static class CMD{
        public final static int CMD_DEVICE_LIST_ADD = 0x10;
        public final static int CMD_DEVICE_CONNECT_BIND = 0x11;
        public final static int CMD_DEVICE_ALERT_START=0x12;
        public final static int CMD_DEVICE_ALERT_STOP=0x13;
        public final static int CMD_DEVICE_UNBIND=0x14;
        public final static int CMD_DEVICE_LIST_CLEAR=0x15;
//        public final static int CMD_TASK_EXCE=0x16;
        public final static int CMD_DEVICE_BIND_SCAN=0x17;
        public final static int CMD_DEVICE_UNBIND_ERROR=0x18;
        public final static int CMD_DEVICE_SCANING=0x19;
    }
    public static class RET{
        public final static int RET_BLE_SCAN_START=0x20;
//        public final static int RET_BLE_SCANING=0x21;
        public final static int RET_BLE_CONNECT_START=0x22;
        public final static int RET_BLE_CONNECT_SUCCESS=0x023;
        public final static int RET_BLE_CONNECT_FAILED=0x024;
        public final static int RET_BLE_DISCONNECT=0x025;
        public final static int RET_BLE_FIND_BIND_DEVICE=0x26;
        public final static int RET_BLE_MODE_BIND_SUCCESS=0x27;
        public final static int RET_BLE_MODE_BIND_FAIL=0x28;
        public final static int RET_BLE_MODE_WORK_SUCCESS=0x29;
        public final static int RET_BLE_MODE_WORK_FAIL=0x30;
        public final static int RET_BLE_ALERT_STARTED=0x31;
        public final static int RET_BLE_ALERT_STOPED=0x32;
        public final static int RET_BLE_UNBIND_COMPLETE=0x33;
        public final static int RET_BLE_READ_RSSI=0x04;
        public final static int RET_BLE_READ_BATTERY=0x35;
        public final static int LOCATION=0x36;
        public final static int RET_BLE_STATE_ON=0x37;
        public final static int RET_BLE_STATE_OFF=0x38;
    }
    public static class HANDLER{
        public final static int MSG_HANDLER_DEVECE_LIST = 0x50;
        public final static int MSG_HANDLER_FRIEND_LIST = 0x51;
    }
}
