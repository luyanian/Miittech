package com.miittech.you.net.response;

import android.content.Context;

import com.ryon.mutils.ToastUtils;

import java.io.Serializable;
import java.util.List;

/**
 * Created by Administrator on 2017/10/18.
 */

public class DeviceResponse extends BaseResponse {
    private String devid;//蓝牙贴片的出厂id
    private int valid;//1:可用，0：不可用(已被其他绑int定),-1：不合法贴片id
    private int bindstate;//1：已绑定 0：未绑定
    private List<DevlistBean> devlist;

    public String getDevid() {
        return devid;
    }

    public void setDevid(String devid) {
        this.devid = devid;
    }

    public int getValid() {
        return valid;
    }

    public void setValid(int valid) {
        this.valid = valid;
    }

    public int getBindstate() {
        return bindstate;
    }

    public void setBindstate(int bindstate) {
        this.bindstate = bindstate;
    }

    public List<DevlistBean> getDevlist() {
        return devlist;
    }

    public void setDevlist(List<DevlistBean> devlist) {
        this.devlist = devlist;
    }

    /**
     * devlist : [{"devid":"97EACA00AB06","devname":"","devimg":"","devtype":1,"groupid":0,"groupname":"","devbattery":100,"devstate":1,"devposstate":-1,"lasttime":"","usedstate":1,"bindstate":1,"bindtime":"20171019143917","friendid":"0","locinfo":{"lat":0,"lng":0,"addr":"","lasttime":""}}]
     * errcode : 0
     * errmsg : success
     */

    public static class DevlistBean implements Serializable{
        /**
         * devid : 97EACA00AB06
         * devname :
         * devimg :
         * devtype : 1
         * groupid : 0
         * groupname :
         * devbattery : 100
         * devstate : 1
         * devposstate : -1
         * lasttime :
         * usedstate : 1
         * bindstate : 1
         * bindtime : 20171019143917
         * friendid : 0
         * locinfo : {"lat":0,"lng":0,"addr":"","lasttime":""}
         */

        private String devid;
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
        private String friendid;
        private LocinfoBean locinfo;

        public String getDevid() {
            return devid;
        }

        public void setDevid(String devid) {
            this.devid = devid;
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

        public String getFriendid() {
            return friendid;
        }

        public void setFriendid(String friendid) {
            this.friendid = friendid;
        }

        public LocinfoBean getLocinfo() {
            return locinfo;
        }

        public void setLocinfo(LocinfoBean locinfo) {
            this.locinfo = locinfo;
        }

        public static class LocinfoBean implements Serializable{
            /**
             * lat : 0
             * lng : 0
             * addr :
             * lasttime :
             */

            private int lat;
            private int lng;
            private String addr;
            private String lasttime;

            public int getLat() {
                return lat;
            }

            public void setLat(int lat) {
                this.lat = lat;
            }

            public int getLng() {
                return lng;
            }

            public void setLng(int lng) {
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


    public boolean isVerSuccessful() {
        return (super.isSuccessful()&&this.valid==1);
    }

    public boolean isBindSuccessful(){
        return (super.isSuccessful()&&this.bindstate==1);
    }



    @Override
    public void onError(Context context) {
        super.onError(context);
        switch (this.valid){
            case 1:
                break;
            case 0:
                ToastUtils.showShort("设备已被绑定或者设备不可用！");
                break;
            case -1:
                ToastUtils.showShort("不合法设备！");
                break;
        }
    }
}
