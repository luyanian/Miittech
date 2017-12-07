package com.miittech.you.net.response;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.List;

/**
 * Created by Administrator on 2017/11/7.
 */

public class DeviceInfoResponse extends BaseResponse {

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

        private DevinfoBean devinfo;

        public DevinfoBean getDevinfo() {
            return devinfo;
        }

        public void setDevinfo(DevinfoBean devinfo) {
            this.devinfo = devinfo;
        }

        public static class DevinfoBean implements Serializable{
            /**
             * devid : 92EACA00AB06
             * owneruser : 0
             * devname : 5omL5o+Q5YyF

             * devimg : https://openservice.wisdomsky.cn/qianji/imgs/YOU161510034575823CtXjtvD8EZiSz1oIl8FDdqMgHVHDV347.jpg
             * devtype : 1
             * groupid : 0
             * groupname :
             * devbattery : 100
             * devstate : 1
             * devposstate : -1
             * lasttime : 20171107140241
             * usedstate : 1
             * bindstate : 1
             * bindtime : 20171107140241
             * alertinfo : {"vol":15,"isShake":0,"isRepeat":1,"isReconnect":1,"duration":30,"id":1,"name":"","url":""}
             * locinfo : {"lat":0,"lng":0,"addr":"","lasttime":"20171107140241"}
             */

            private String devid;
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
            private int bindstate;
            private String bindtime;
            private AlertinfoBean alertinfo;
            private LocinfoBean locinfo;

            public String getDevid() {
                return devid;
            }

            public void setDevid(String devid) {
                this.devid = devid;
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

            public int getBindstate() {
                return bindstate;
            }

            public void setBindstate(int bindstate) {
                this.bindstate = bindstate;
            }

            public String getBindtime() {
                return bindtime;
            }

            public void setBindtime(String bindtime) {
                this.bindtime = bindtime;
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
                 * vol : 15
                 * isShake : 0
                 * isRepeat : 1
                 * isReconnect : 1
                 * duration : 30
                 * id : 1
                 * name :
                 * url :
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
                 * lat : 0
                 * lng : 0
                 * addr :
                 * lasttime : 20171107140241
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
