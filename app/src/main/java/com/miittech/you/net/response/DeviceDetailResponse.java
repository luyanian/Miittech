package com.miittech.you.net.response;

import com.google.gson.annotations.SerializedName;
import com.miittech.you.entity.DeviceInfo;

import java.io.Serializable;
import java.util.List;

/**
 * Created by Administrator on 2017/11/7.
 */

public class DeviceDetailResponse extends BaseResponse {

    /**
     * userinfo : {"devinfo":{"devid":"92EACA00AB06","owneruser":"0","devname":"5omL5o+Q5YyF\n","devimg":"https://openservice.wisdomsky.cn/qianji/imgs/YOU161510034575823CtXjtvD8EZiSz1oIl8FDdqMgHVHDV347.jpg","devtype":1,"groupid":0,"groupname":"","devbattery":100,"devstate":1,"devposstate":-1,"lasttime":"20171107140241","usedstate":1,"bindstate":1,"bindtime":"20171107140241","alertinfo":{"vol":15,"isShake":0,"isRepeat":1,"isReconnect":1,"duration":30,"id":1,"name":"","url":""},"locinfo":{"lat":0,"lng":0,"addr":"","lasttime":"20171107140241"}}}
     * config : {}
     */

    private UserinfoBean userinfo;
    private ConfigBean config;
    private List<TracelistBean> tracelist;

    public UserinfoBean getUserinfo() {
        return userinfo;
    }

    public void setUserinfo(UserinfoBean userinfo) {
        this.userinfo = userinfo;
    }

    public ConfigBean getConfig() {
        return config;
    }

    public void setConfig(ConfigBean config) {
        this.config = config;
    }

    public List<TracelistBean> getTracelist() {
        return tracelist;
    }

    public void setTracelist(List<TracelistBean> tracelist) {
        this.tracelist = tracelist;
    }

    public static class UserinfoBean implements Serializable{
        /**
         * devinfo : {"devid":"92EACA00AB06","owneruser":"0","devname":"5omL5o+Q5YyF\n","devimg":"https://openservice.wisdomsky.cn/qianji/imgs/YOU161510034575823CtXjtvD8EZiSz1oIl8FDdqMgHVHDV347.jpg","devtype":1,"groupid":0,"groupname":"","devbattery":100,"devstate":1,"devposstate":-1,"lasttime":"20171107140241","usedstate":1,"bindstate":1,"bindtime":"20171107140241","alertinfo":{"vol":15,"isShake":0,"isRepeat":1,"isReconnect":1,"duration":30,"id":1,"name":"","url":""},"locinfo":{"lat":0,"lng":0,"addr":"","lasttime":"20171107140241"}}
         */

        private DeviceInfo devinfo;

        public DeviceInfo getDevinfo() {
            return devinfo;
        }

        public void setDevinfo(DeviceInfo devinfo) {
            this.devinfo = devinfo;
        }
    }


    public static class ConfigBean  implements Serializable{
    }

    public static class TracelistBean implements Serializable {
        /**
         * ttime : 20171204155433
         * traceid : 42943
         * ttype : 0
         * locinfo : {"lat":39.101208,"lng":117.13795,"addr":"5Lit5Zu95aSp5rSl5biC6KW/6Z2S5Yy656u56IuR6LevNuWPtzUwNzYwNw=="}
         */

        private String ttime;
        private String traceid;
        private int ttype;
        private LocinfoBean locinfo;

        public String getTtime() {
            return ttime;
        }

        public void setTtime(String ttime) {
            this.ttime = ttime;
        }

        public String getTraceid() {
            return traceid;
        }

        public void setTraceid(String traceid) {
            this.traceid = traceid;
        }

        public int getTtype() {
            return ttype;
        }

        public void setTtype(int ttype) {
            this.ttype = ttype;
        }

        public LocinfoBean getLocinfo() {
            return locinfo;
        }

        public void setLocinfo(LocinfoBean locinfo) {
            this.locinfo = locinfo;
        }

        public static class LocinfoBean implements Serializable {
            /**
             * lat : 39.101208
             * lng : 117.13795
             * addr : 5Lit5Zu95aSp5rSl5biC6KW/6Z2S5Yy656u56IuR6LevNuWPtzUwNzYwNw==
             */

            private double lat;
            private double lng;
            private String addr;

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
        }
    }
}
