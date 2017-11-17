package com.miittech.you.global;

/**
 * Created by Administrator on 2017/10/20.
 */

public class IntentExtras {
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

    public static class ACTION{
        public final static String ACTION_BLE_COMMAND="com.mittech.you.device.service.cmd";
        public final static String ACTION_CMD_RESPONSE="com.mittech.you.device.service.response";
    }
    public static class CMD{
        public final static int CMD_DEVICE_CONNECT_BIND = 0x010;
        public final static int CMD_DEVICE_CONNECT_WORK = 0x011;
        public final static int CMD_DEVICE_ALERT_START=0x012;
        public final static int CMD_DEVICE_ALERT_STOP=0x013;
        public final static int CMD_DEVICE_UNBIND=0x014;
    }
    public static class RET{
        public final static int RET_DEVICE_CONNECT_SUCCESS=0x020;
        public final static int RET_DEVICE_CONNECT_FAILED=0x021;
        public final static int RET_DEVICE_CONNECT_BIND_SUCCESS=0x022;
        public final static int RET_DEVICE_CONNECT_BIND_FAIL=0x023;
        public final static int RET_DEVICE_CONNECT_WORK_SUCCESS=0x024;
        public final static int RET_DEVICE_CONNECT_WORK_FAILED=0x025;
        public final static int RET_DEVICE_CONNECT_ALERT_START_SUCCESS=0x026;
        public final static int RET_DEVICE_CONNECT_ALERT_STOP_SUCCESS=0x027;
        public final static int RET_DEVICE_UNBIND_SUCCESS=0x028;
    }
}
