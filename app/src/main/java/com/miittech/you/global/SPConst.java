package com.miittech.you.global;

/**
 * Created by Administrator on 2017/11/7.
 */

public class SPConst{
    public static final String LOC_INFO="locInfo";
    public static final String isOpenNotification ="is_open_notification";
    public static class USER{
        public static final String SP_NAME="session";
        public static final String KEY_TOCKEN="tocken";
        public static final String KEY_USERID="userId";
        public static final String KEY_NIKENAME="uname";
        public static final String KEY_IMAGE="image";
    }


    public static class ALET_STATUE{
        public static final String SP_NAME="alert_status";
        public static final int STATUS_BELLING=1;
        public static final int STATUS_UNBELL=2;
    }

    public static class DATA{
        public static final String DEVICELIST = "deviceList";
        public static final String USERINFO = "userinfo";
    }
    public static class DISTURB{
        public static final String ISAREADISTURB="isAreaDisturb";
        public static final String ISTIMEDISTURB="isTimeDisturb";
    }
}
