package com.miittech.you.net.response;

import java.util.List;

/**
 * Created by Administrator on 2017/11/9.
 */

public class FriendLocInfoResponse extends BaseResponse {
    private List<FriendInfo> friendlist;

    public List<FriendInfo> getFriendlist() {
        return friendlist;
    }

    public void setFriendlist(List<FriendInfo> friendlist) {
        this.friendlist = friendlist;
    }

    public static class FriendInfo{
        private String friendid;
        private String locinfo;
        private double lat;
        private double lng;
        private String addr;
        private String updtime;

        public String getFriendid() {
            return friendid;
        }

        public void setFriendid(String friendid) {
            this.friendid = friendid;
        }

        public String getLocinfo() {
            return locinfo;
        }

        public void setLocinfo(String locinfo) {
            this.locinfo = locinfo;
        }

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

        public String getUpdtime() {
            return updtime;
        }

        public void setUpdtime(String updtime) {
            this.updtime = updtime;
        }
    }
}