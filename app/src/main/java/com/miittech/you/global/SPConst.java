package com.miittech.you.global;

/**
 * Created by Administrator on 2017/11/7.
 */

public class SPConst{
    public static final String LOC_INFO="locInfo";
    public static final String USER_INFO="userInfo";
    public static final String IS_DEVICE_REDISCOVER="isfirst_after_login";
    public static class USER{
        public static final String SP_NAME="session";
        public static final String KEY_TOCKEN="tocken";
        public static final String KEY_USERID="userId";
        public static final String KEY_UNAME="uname";
    }


    public static class ALET_STATUE{
        public static final String SP_NAME="alert_status";
        public static final String KEY_STATUS="status";
        public static final int STATUS_BELLING=1;
        public static final int STATUS_UNBELL=2;
    }

    public static class DATA{
        public static final String DEVICELIST = "deviceList";
    }
}
