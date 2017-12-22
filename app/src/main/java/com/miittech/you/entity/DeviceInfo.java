package com.miittech.you.entity;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

/**
 * Created by Administrator on 2017/12/22.
 */

public class DeviceInfo implements Serializable {
    /**
     * devid : CF0000000000
     * owneruser : 0
     * devname : 6ZKl5YyZ
     * devimg : https://openservice.wisdomsky.cn/qianji/imgs/YOU151512918659419UGYboLD4UmOEDhc8VyMlk2jBrCPmyFG4.png
     * devtype : 1
     * groupid : 1
     * groupname : 6ZKl5YyZ
     * devbattery : 100
     * devstate : 1
     * devposstate : -1
     * lasttime : 20171209231546
     * usedstate : 100
     * bindstate : 1
     * bindtime : 20171204120517
     * friendid : 0
     * friendname :
     * alertinfo : {"vol":31,"isShake":1,"isRepeat":1,"isReconnect":1,"duration":30,"id":3,"name":"Blues for Slim","url":"native://bluesforslim.mp3"}
     * locinfo : {"lat":39.07744,"lng":117.03072,"addr":"5Lit5Zu95aSp5rSl5biC6KW/6Z2S5Yy65YWw6aao6YGT","lasttime":"20171209231546"}
     */

    @SerializedName("devid")
    private String devidX;
    private String owneruser;
    private String devname;
    private String devimg;
    private int devtype;
    private int groupid;
    private String groupname;
    private int devbattery;
    private int devstate;
    private int devposstate;
    private String lasttime;
    private int usedstate;
    @SerializedName("bindstate")
    private int bindstateX;
    private String bindtime;
    private String friendid;
    private String friendname;
    private AlertinfoBean alertinfo;
    private LocinfoBean locinfo;

    public String getDevidX() {
        return devidX;
    }

    public void setDevidX(String devidX) {
        this.devidX = devidX;
    }

    public String getOwneruser() {
        return owneruser;
    }

    public void setOwneruser(String owneruser) {
        this.owneruser = owneruser;
    }

    public String getDevname() {
        return devname;
    }

    public void setDevname(String devname) {
        this.devname = devname;
    }

    public String getDevimg() {
        return devimg;
    }

    public void setDevimg(String devimg) {
        this.devimg = devimg;
    }

    public int getDevtype() {
        return devtype;
    }

    public void setDevtype(int devtype) {
        this.devtype = devtype;
    }

    public int getGroupid() {
        return groupid;
    }

    public void setGroupid(int groupid) {
        this.groupid = groupid;
    }

    public String getGroupname() {
        return groupname;
    }

    public void setGroupname(String groupname) {
        this.groupname = groupname;
    }

    public int getDevbattery() {
        return devbattery;
    }

    public void setDevbattery(int devbattery) {
        this.devbattery = devbattery;
    }

    public int getDevstate() {
        return devstate;
    }

    public void setDevstate(int devstate) {
        this.devstate = devstate;
    }

    public int getDevposstate() {
        return devposstate;
    }

    public void setDevposstate(int devposstate) {
        this.devposstate = devposstate;
    }

    public String getLasttime() {
        return lasttime;
    }

    public void setLasttime(String lasttime) {
        this.lasttime = lasttime;
    }

    public int getUsedstate() {
        return usedstate;
    }

    public void setUsedstate(int usedstate) {
        this.usedstate = usedstate;
    }

    public int getBindstateX() {
        return bindstateX;
    }

    public void setBindstateX(int bindstateX) {
        this.bindstateX = bindstateX;
    }

    public String getBindtime() {
        return bindtime;
    }

    public void setBindtime(String bindtime) {
        this.bindtime = bindtime;
    }

    public String getFriendid() {
        return friendid;
    }

    public void setFriendid(String friendid) {
        this.friendid = friendid;
    }

    public String getFriendname() {
        return friendname;
    }

    public void setFriendname(String friendname) {
        this.friendname = friendname;
    }

    public AlertinfoBean getAlertinfo() {
        return alertinfo;
    }

    public void setAlertinfo(AlertinfoBean alertinfo) {
        this.alertinfo = alertinfo;
    }

    public LocinfoBean getLocinfo() {
        return locinfo;
    }

    public void setLocinfo(LocinfoBean locinfo) {
        this.locinfo = locinfo;
    }

    public static class AlertinfoBean implements Serializable {
        /**
         * vol : 31
         * isShake : 1
         * isRepeat : 1
         * isReconnect : 1
         * duration : 30
         * id : 3
         * name : Blues for Slim
         * url : native://bluesforslim.mp3
         */

        private int vol;
        private int isShake;
        private int isRepeat;
        private int isReconnect;
        private int duration;
        private int id;
        private String name;
        @SerializedName("url")
        private String urlX;

        public int getVol() {
            return vol;
        }

        public void setVol(int vol) {
            this.vol = vol;
        }

        public int getIsShake() {
            return isShake;
        }

        public void setIsShake(int isShake) {
            this.isShake = isShake;
        }

        public int getIsRepeat() {
            return isRepeat;
        }

        public void setIsRepeat(int isRepeat) {
            this.isRepeat = isRepeat;
        }

        public int getIsReconnect() {
            return isReconnect;
        }

        public void setIsReconnect(int isReconnect) {
            this.isReconnect = isReconnect;
        }

        public int getDuration() {
            return duration;
        }

        public void setDuration(int duration) {
            this.duration = duration;
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getUrlX() {
            return urlX;
        }

        public void setUrlX(String urlX) {
            this.urlX = urlX;
        }
    }

    public static class LocinfoBean implements Serializable {
        /**
         * lat : 39.07744
         * lng : 117.03072
         * addr : 5Lit5Zu95aSp5rSl5biC6KW/6Z2S5Yy65YWw6aao6YGT
         * lasttime : 20171209231546
         */

        private double lat;
        private double lng;
        private String addr;
        private String lasttime;

        public double getLat() {
            return lat;
        }

        public void setLat(double lat) {
            this.lat = lat;
        }

        public double getLng() {
            return lng;
        }

        public void setLng(double lng) {
            this.lng = lng;
        }

        public String getAddr() {
            return addr;
        }

        public void setAddr(String addr) {
            this.addr = addr;
        }

        public String getLasttime() {
            return lasttime;
        }

        public void setLasttime(String lasttime) {
            this.lasttime = lasttime;
        }
    }
}
