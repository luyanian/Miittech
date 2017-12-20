package com.miittech.you.net.response;

import java.io.Serializable;
import java.util.List;

/**
 * Created by Administrator on 2017/11/9.
 */

public class FriendLocInfoResponse extends BaseResponse {

    private List<FriendlistBean> friendlist;

    public List<FriendlistBean> getFriendlist() {
        return friendlist;
    }

    public void setFriendlist(List<FriendlistBean> friendlist) {
        this.friendlist = friendlist;
    }

    public static class FriendlistBean implements Serializable {
        /**
         * friendid : 7
         * locinfo : {"lat":39.141935,"lng":117.195131,"addr":"5aSp5rSl5biC5ZKM5bmz5Yy65Y2X6ams6LevMTHlj7c="}
         * updtime : 20171220090825
         */

        private String friendid;
        private LocinfoBean locinfo;
        private String updtime;

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

        public String getUpdtime() {
            return updtime;
        }

        public void setUpdtime(String updtime) {
            this.updtime = updtime;
        }

        public static class LocinfoBean implements Serializable {
            /**
             * lat : 39.141935
             * lng : 117.195131
             * addr : 5aSp5rSl5biC5ZKM5bmz5Yy65Y2X6ams6LevMTHlj7c=
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
