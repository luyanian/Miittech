package com.miittech.you.net.response;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Created by Administrator on 2017/9/18.
 */
public class UserInfoResponse extends BaseResponse{
    /**
     * userinfo : {"username":"","nickname":"cnlvbg==\n","headimg":"https://openservice.wisdomsky.cn/qianji/imgs/YOU151505984053662U7IlWDloZym451XI8e3w9Xrt7hRLYDRA.jpg","phone":"18722423376","email":"","isBindEmail":0,"isBindWx":0,"isBindQQ":0,"isShareLocation":0,"isAreaDisturb":0,"isTimeDisturb":0,"alertinfo":{"vol":15,"isShake":0,"isRepeat":1,"isReconnect":1,"duration":30,"id":1,"name":"","url":""},"locinfo":{"lat":0,"lng":0,"addr":"","lasttime":""}}
     * config : {"workconf":{"isalarm":1,"nearrssi":-60,"farrssi":-75,"alarmrssi":-85,"lossrssi":-95},"donotdisturb":{"arealist":[{"id":25,"title":"5a62","inout":0,"area":{"lat":0,"lng":0,"addr":"","R":0}},{"id":26,"title":"5YWs5Y+4","inout":0,"area":{"lat":0,"lng":0,"addr":"","R":0}},{"id":134,"title":"���j","inout":1,"ssid":"lan-hi"},{"id":136,"title":"L","inout":1,"area":{"lat":0,"lng":0,"addr":"","R":0}}],"timelist":[{"id":135,"title":"u001c","dayofweek":"1267","repeat":1,"stime":51635,"etime":111637}]}}
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
         * nickname : cnlvbg==
         * headimg : https://openservice.wisdomsky.cn/qianji/imgs/YOU151505984053662U7IlWDloZym451XI8e3w9Xrt7hRLYDRA.jpg
         * phone : 18722423376
         * email :
         * isBindEmail : 0
         * isBindWx : 0
         * isBindQQ : 0
         * isShareLocation : 0
         * isAreaDisturb : 0
         * isTimeDisturb : 0
         * alertinfo : {"vol":15,"isShake":0,"isRepeat":1,"isReconnect":1,"duration":30,"id":1,"name":"","url":""}
         * locinfo : {"lat":0,"lng":0,"addr":"","lasttime":""}
         */

        @SerializedName("username")
        private String usernameX;
        private String nickname;
        private String headimg;
        private String phone;
        private String email;
        private int isBindEmail;
        private int isBindWx;
        private int isBindQQ;
        private int isShareLocation;
        private int isAreaDisturb;
        private int isTimeDisturb;
        private AlertinfoBean alertinfo;
        private LocinfoBean locinfo;

        public String getUsernameX() {
            return usernameX;
        }

        public void setUsernameX(String usernameX) {
            this.usernameX = usernameX;
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

        public int getIsBindEmail() {
            return isBindEmail;
        }

        public void setIsBindEmail(int isBindEmail) {
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

        public static class LocinfoBean {
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

    public static class ConfigBean {
        /**
         * workconf : {"isalarm":1,"nearrssi":-60,"farrssi":-75,"alarmrssi":-85,"lossrssi":-95}
         * donotdisturb : {"arealist":[{"id":25,"title":"5a62","inout":0,"area":{"lat":0,"lng":0,"addr":"","R":0}},{"id":26,"title":"5YWs5Y+4","inout":0,"area":{"lat":0,"lng":0,"addr":"","R":0}},{"id":134,"title":"���j","inout":1,"ssid":"lan-hi"},{"id":136,"title":"L","inout":1,"area":{"lat":0,"lng":0,"addr":"","R":0}}],"timelist":[{"id":135,"title":"u001c","dayofweek":"1267","repeat":1,"stime":51635,"etime":111637}]}
         */

        private WorkconfBean workconf;
        private DonotdisturbBean donotdisturb;

        public WorkconfBean getWorkconf() {
            return workconf;
        }

        public void setWorkconf(WorkconfBean workconf) {
            this.workconf = workconf;
        }

        public DonotdisturbBean getDonotdisturb() {
            return donotdisturb;
        }

        public void setDonotdisturb(DonotdisturbBean donotdisturb) {
            this.donotdisturb = donotdisturb;
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

        public static class DonotdisturbBean {
            private List<ArealistBean> arealist;
            private List<TimelistBean> timelist;

            public List<ArealistBean> getArealist() {
                return arealist;
            }

            public void setArealist(List<ArealistBean> arealist) {
                this.arealist = arealist;
            }

            public List<TimelistBean> getTimelist() {
                return timelist;
            }

            public void setTimelist(List<TimelistBean> timelist) {
                this.timelist = timelist;
            }

            public static class ArealistBean {
                /**
                 * id : 25
                 * title : 5a62
                 * inout : 0
                 * area : {"lat":0,"lng":0,"addr":"","R":0}
                 * ssid : lan-hi
                 */

                private int id;
                private String title;
                private int inout;
                private AreaBean area;
                private String ssid;

                public int getId() {
                    return id;
                }

                public void setId(int id) {
                    this.id = id;
                }

                public String getTitle() {
                    return title;
                }

                public void setTitle(String title) {
                    this.title = title;
                }

                public int getInout() {
                    return inout;
                }

                public void setInout(int inout) {
                    this.inout = inout;
                }

                public AreaBean getArea() {
                    return area;
                }

                public void setArea(AreaBean area) {
                    this.area = area;
                }

                public String getSsid() {
                    return ssid;
                }

                public void setSsid(String ssid) {
                    this.ssid = ssid;
                }

                public static class AreaBean {
                    /**
                     * lat : 0
                     * lng : 0
                     * addr :
                     * R : 0
                     */

                    private int lat;
                    private int lng;
                    private String addr;
                    private int R;

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

                    public int getR() {
                        return R;
                    }

                    public void setR(int R) {
                        this.R = R;
                    }
                }
            }

            public static class TimelistBean {
                /**
                 * id : 135
                 * title : u001c
                 * dayofweek : 1267
                 * repeat : 1
                 * stime : 51635
                 * etime : 111637
                 */

                private int id;
                private String title;
                private String dayofweek;
                private int repeat;
                private int stime;
                private int etime;

                public int getId() {
                    return id;
                }

                public void setId(int id) {
                    this.id = id;
                }

                public String getTitle() {
                    return title;
                }

                public void setTitle(String title) {
                    this.title = title;
                }

                public String getDayofweek() {
                    return dayofweek;
                }

                public void setDayofweek(String dayofweek) {
                    this.dayofweek = dayofweek;
                }

                public int getRepeat() {
                    return repeat;
                }

                public void setRepeat(int repeat) {
                    this.repeat = repeat;
                }

                public int getStime() {
                    return stime;
                }

                public void setStime(int stime) {
                    this.stime = stime;
                }

                public int getEtime() {
                    return etime;
                }

                public void setEtime(int etime) {
                    this.etime = etime;
                }
            }
        }
    }
}
