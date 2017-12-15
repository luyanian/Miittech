package com.miittech.you.net.response;

import java.io.Serializable;
import java.util.List;

/**
 * Created by Administrator on 2017/12/15.
 */

public class FriendTraceResponse extends BaseResponse implements Serializable {

    private List<TracelistBean> tracelist;

    public List<TracelistBean> getTracelist() {
        return tracelist;
    }

    public void setTracelist(List<TracelistBean> tracelist) {
        this.tracelist = tracelist;
    }

    public static class TracelistBean implements Serializable {
        /**
         * reptime : 20171214221556
         * traceid : 2158
         * user_loc : {"lat":39.051765,"lng":117.124259,"addr":"5aSp5rSl5biC6KW/6Z2S5Yy65Y2X5rKz6Lev"}
         */

        private String reptime;
        private String traceid;
        private UserLocBean user_loc;

        public String getReptime() {
            return reptime;
        }

        public void setReptime(String reptime) {
            this.reptime = reptime;
        }

        public String getTraceid() {
            return traceid;
        }

        public void setTraceid(String traceid) {
            this.traceid = traceid;
        }

        public UserLocBean getUser_loc() {
            return user_loc;
        }

        public void setUser_loc(UserLocBean user_loc) {
            this.user_loc = user_loc;
        }

        public static class UserLocBean implements Serializable {
            /**
             * lat : 39.051765
             * lng : 117.124259
             * addr : 5aSp5rSl5biC6KW/6Z2S5Yy65Y2X5rKz6Lev
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
