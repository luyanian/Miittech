package com.miittech.you.global;

import java.util.Date;

/**
 * Created by Administrator on 2017/9/11.
 */

public class Params {

    public static final String userid_unlogin="00000000";
    public static final String signkey_unlogin="TRACOIN2017APP";

    public static Long timeStamp(){
        return new Date().getTime();
    }

    public static class VTYPE{
        public static final String PHONELOGIN="PHONELOGIN";
        public static final String FORGETPWD="FORGETPWD";
    }
    public static class QRY_TYPE {
        public static final String ALL="ALL";//全部明细信息(不含轨迹)
        public static final String BASE="BASE";//基本信息
        public static final String CONF="CONF";//勿扰等配置
        public static final String EVENTLOG="EVENTLOG";//历史事件（实际为用户下所有贴片的事件信息，按事件时间倒排）
        public static final String TRACE="TRACE";//历史轨迹
        public static final String USED="USED";//当前账户绑定的贴片列表
        public static final String SHARED="SHARED";//分享给好有的贴片列表
    }

    public static class METHOD{
        public static final String PHONE="PHONE";
        public static final String EMAIL="EMAIL";
        public static final String UNAME="UNAME";
        public static final String QQSSO="QQSSO";
        public static final String WECHART="WECHART";
        public static final String IGNORE_ADD="ADD";//添加申请
        public static final String IGNORE_UPD="UPD";//添加申请
        public static final String IGNORE_DEL="DEL";//添加申请
        public static final String FRIEND_ADD="ADD";//添加申请
        public static final String FRIEND_CONFIRM="CONFIRM";//确认添加
        public static final String FRIEND_REFUSE="REFUSE";//拒绝添加
        public static final String FRIEND_DELETE="DELETE";//解除好友关系
        public static final String FRIEND_UPDATE="UPDATE";//编辑好友信息
        public static final String BINGD = "Bind";//绑定设备
        public static final String UNBIND = "Unbind";//解除绑定
    }

    public static class FRIEND_STATUS{
        public final static int FRIEND_AREADY_ADD=1;//已添加
        public final static int FRIEND_APPLYING=2;//申请中
        public final static int FRIEND_BE_INVITED=4;//被邀请
        public final static int FRIEND_REFUSED=8;//已拒绝
        public final static int FRIEND_BE_DELETE=6;//已被对方删除

    }
    public static class EVENT_TYPE{
        public final static int DEVICE_ADD=1;//添加贴片
        public final static int DEVICE_CONNECT=2;//建立连接
        public final static int DEVICE_FAR_FROM=3;//远离
        public final static int DEVICE_LOSE=4;//丢失
        public final static int DEVICE_REDISCOVER=5;//重新找回
        public final static int DEVICE_HIDE_ON=6;//设置隐藏
        public final static int DEVICE_HIDE_OFF=7;//恢复隐藏
        public final static int DEVICE_ALERM_DISABLE=8;//禁用告警
        public final static int DEVICE_ALERM_ENABLE=9;//恢复告警
        public final static int DEVICE_SHARE=10;//贴片分享
        public final static int DEVICE_SHARE_SUCCESSFUL=11;//分享成功
        public final static int DEVICE_SHARE_REFUSE=12;//分项拒绝
        public final static int DEVICE_SHARE_TACKBACK=13;//收回分享贴片
        public final static int DEVICE_FIND=10101;//手机查找贴片
    }
    //当前贴片状态值
    public static class DEVICE_STATUS{
        public final static int DIABLE=-100;//禁用
        public final static int LOSE=-99;//丢失
        public final static int SHUTDOWN=-2;//关机（cube）
        public final static int STOP_USE_TEMPORARILY=-1;//暂停使用
        public final static int NORMAL=1;//正常使用

    }
    //与当前使用者的距离
    public static class DEVICE_POSS_STATE{
        public final static int UNKNOWN=-1;// <0  未知
        public final static int CLOSE=1;//很近
        public final static int FAR=2;//较远
        public final static int FAR_AWAY=3;//远离
        public final static int LOSE = 100;//丢失？
    }
    //使用状态
    public static class USED_STATE{
        public static final int OWN=1;//自己使用
        public static final int SHARE=100;//已分享
    }
    //绑定状态
    public static class BIND_STATE{
        public static final int BIND=1;//正常绑定
        public static final int UNBIND=0;//已解绑（已解绑的贴片自动隐藏）
    }


    public static class BLEMODE{
        public static final int MODE_WORK=0;
        public static final int MODE_BIND=1;
        public static final int MODE_UNBIND=2;
    }
    public static class BLE_CMD{
        public static final byte[] CMD_ALERT_START = new byte[]{0x02};
        public static final byte[] CMD_ALERT_END = new byte[]{0x00};
    }

}
