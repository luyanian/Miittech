package com.miittech.you.net.response;

/**
 * Created by Administrator on 2017/9/18.
 */

public class UserInfoResponse extends BaseResponse{
    /**
     * userinfo : {"username":"","nickname":"有物用户","headimg":"","phone":"","email":"","isBindWx":0,"isBindQQ":0,"isShareLocation":0,"isAreaDisturb":0,"isTimeDisturb":0,"alertinfo":{"vol":15,"id":1,"name":"","url":""},"locinfo":{"lat":0,"lng":0,"lasttime":""}}
     * config : {"workconf":{"isalarm":1,"nearrssi":-60,"farrssi":-75,"alarmrssi":-85,"lossrssi":-95}}
     * errcode : 0
     */

    private UserinfoBean userinfo;
    private ConfigBean config;

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

    public static class UserinfoBean {
        /**
         * username :
         * nickname : 有物用户
         * headimg :
         * phone :
         * email :
         * isBindEmail:0
         * isBindWx : 0
         * isBindQQ : 0
         * isShareLocation : 0
         * isAreaDisturb : 0
         * isTimeDisturb : 0
         * alertinfo : {"vol":15,"id":1,"name":"","url":""}
         * locinfo : {"lat":0,"lng":0,"lasttime":""}
         */

        private String username;
        private String nickname;
        private String headimg;
        private String phone;
        private String email;
        private String isBindEmail;
        private int isBindWx;
        private int isBindQQ;
        private int isShareLocation;
        private int isAreaDisturb;
        private int isTimeDisturb;
        private AlertinfoBean alertinfo;
        private LocinfoBean locinfo;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getNickname() {
            return nickname;
        }

        public void setNickname(String nickname) {
            this.nickname = nickname;
        }

        public String getHeadimg() {
            return headimg;
        }

        public void setHeadimg(String headimg) {
            this.headimg = headimg;
        }

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getIsBindEmail() {
            return isBindEmail;
        }

        public void setIsBindEmail(String isBindEmail) {
            this.isBindEmail = isBindEmail;
        }

        public int getIsBindWx() {
            return isBindWx;
        }

        public void setIsBindWx(int isBindWx) {
            this.isBindWx = isBindWx;
        }

        public int getIsBindQQ() {
            return isBindQQ;
        }

        public void setIsBindQQ(int isBindQQ) {
            this.isBindQQ = isBindQQ;
        }

        public int getIsShareLocation() {
            return isShareLocation;
        }

        public void setIsShareLocation(int isShareLocation) {
            this.isShareLocation = isShareLocation;
        }

        public int getIsAreaDisturb() {
            return isAreaDisturb;
        }

        public void setIsAreaDisturb(int isAreaDisturb) {
            this.isAreaDisturb = isAreaDisturb;
        }

        public int getIsTimeDisturb() {
            return isTimeDisturb;
        }

        public void setIsTimeDisturb(int isTimeDisturb) {
            this.isTimeDisturb = isTimeDisturb;
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

        public static class AlertinfoBean {
            /**
             * vol : 15
             * id : 1
             * name :
             * url :
             */

            private int vol;
            private int id;
            private String name;
            private String url;

            public int getVol() {
                return vol;
            }

            public void setVol(int vol) {
                this.vol = vol;
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

            public String getUrl() {
                return url;
            }

            public void setUrl(String url) {
                this.url = url;
            }
        }

        public static class LocinfoBean {
            /**
             * lat : 0
             * lng : 0
             * lasttime :
             */

            private int lat;
            private int lng;
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

            public String getLasttime() {
                return lasttime;
            }

            public void setLasttime(String lasttime) {
                this.lasttime = lasttime;
            }
        }
    }

    public static class ConfigBean {
        /**
         * workconf : {"isalarm":1,"nearrssi":-60,"farrssi":-75,"alarmrssi":-85,"lossrssi":-95}
         */

        private WorkconfBean workconf;

        public WorkconfBean getWorkconf() {
            return workconf;
        }

        public void setWorkconf(WorkconfBean workconf) {
            this.workconf = workconf;
        }

        public static class WorkconfBean {
            /**
             * isalarm : 1
             * nearrssi : -60
             * farrssi : -75
             * alarmrssi : -85
             * lossrssi : -95
             */

            private int isalarm;
            private int nearrssi;
            private int farrssi;
            private int alarmrssi;
            private int lossrssi;

            public int getIsalarm() {
                return isalarm;
            }

            public void setIsalarm(int isalarm) {
                this.isalarm = isalarm;
            }

            public int getNearrssi() {
                return nearrssi;
            }

            public void setNearrssi(int nearrssi) {
                this.nearrssi = nearrssi;
            }

            public int getFarrssi() {
                return farrssi;
            }

            public void setFarrssi(int farrssi) {
                this.farrssi = farrssi;
            }

            public int getAlarmrssi() {
                return alarmrssi;
            }

            public void setAlarmrssi(int alarmrssi) {
                this.alarmrssi = alarmrssi;
            }

            public int getLossrssi() {
                return lossrssi;
            }

            public void setLossrssi(int lossrssi) {
                this.lossrssi = lossrssi;
            }
        }
    }

    //{"userinfo":{"username":"","nickname":"有物用户","headimg":"","phone":"","email":"","isBindWx":0,"isBindQQ":0,"isShareLocation":0,"isAreaDisturb":0,"isTimeDisturb":0,"alertinfo":{"vol":15,"id":1,"name":"","url":""},"locinfo":{"lat":0,"lng":0,"lasttime":""}},"config":{"workconf":{"isalarm":1,"nearrssi":-60,"farrssi":-75,"alarmrssi":-85,"lossrssi":-95}},"errcode":0}

}
