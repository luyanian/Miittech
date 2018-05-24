package com.miittech.you.entity;

import java.io.Serializable;
import java.util.List;

/**
 * Created by Administrator on 2017/10/27.
 */

public class Repdata implements Serializable {

    private List<DataItem> Jsonitem;
    private List<DeviceItem> devlist;

    public List<DataItem> getJsonitem() {
        return Jsonitem;
    }

    public void setJsonitem(List<DataItem> jsonitem) {
        Jsonitem = jsonitem;
    }

    public List<DeviceItem> getDevlist() {
        return devlist;
    }

    public void setDevlist(List<DeviceItem> devlist) {
        this.devlist = devlist;
    }

    class DataItem implements Serializable{
        private String reptime;//上报时间yyyymmddhhmmss
        private Locinfo user_loc;

        public String getReptime() {
            return reptime;
        }

        public void setReptime(String reptime) {
            this.reptime = reptime;
        }

        public Locinfo getUser_loc() {
            return user_loc;
        }

        public void setUser_loc(Locinfo user_loc) {
            this.user_loc = user_loc;
        }
    }

    class DeviceItem implements Serializable{
        private String devid;//有物id
        private int devbattery;//百分比，0-100
        private int devstate;//当前有物状态值
        private int devposstate;//与当前使用者的距离
        private int usedstate;//使用状态
        private int bindstate;//绑定状态
        private String sourceid;//当有物为别人分享过来的归属者userid

        public String getDevid() {
            return devid;
        }

        public void setDevid(String devid) {
            this.devid = devid;
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

        public int getUsedstate() {
            return usedstate;
        }

        public void setUsedstate(int usedstate) {
            this.usedstate = usedstate;
        }

        public int getBindstate() {
            return bindstate;
        }

        public void setBindstate(int bindstate) {
            this.bindstate = bindstate;
        }

        public String getSourceid() {
            return sourceid;
        }

        public void setSourceid(String sourceid) {
            this.sourceid = sourceid;
        }
    }


}
