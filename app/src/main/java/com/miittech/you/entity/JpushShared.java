package com.miittech.you.entity;

import java.io.Serializable;

/**
 * Created by Administrator on 2017/12/5.
 */

public class JpushShared implements Serializable {
    /**
     * content_type : shared
     * msg_content : 18722423376分享了有物贴片给您
     * extras : {"sourceid":"15","devid":"CF0000000000","eventid":"41","locinfo":{"lng":0,"lat":0},"targetid":"16","addtime":"20171205113134","msgid":"000000161512444694536995a436cead4e0fe87752b5098d8c664dfda9f71","state":1}
     * title : 贴片分享
     */

    private String content_type;
    private String msg_content;
    private ExtrasBean extras;
    private String title;

    public String getContent_type() {
        return content_type;
    }

    public void setContent_type(String content_type) {
        this.content_type = content_type;
    }

    public String getMsg_content() {
        return msg_content;
    }

    public void setMsg_content(String msg_content) {
        this.msg_content = msg_content;
    }

    public ExtrasBean getExtras() {
        return extras;
    }

    public void setExtras(ExtrasBean extras) {
        this.extras = extras;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public static class ExtrasBean implements Serializable {
        /**
         * sourceid : 15
         * devid : CF0000000000
         * eventid : 41
         * locinfo : {"lng":0,"lat":0}
         * targetid : 16
         * addtime : 20171205113134
         * msgid : 000000161512444694536995a436cead4e0fe87752b5098d8c664dfda9f71
         * state : 1
         */

        private String sourceid;
        private String devid;
        private String eventid;
        private Locinfo locinfo;
        private String targetid;
        private String addtime;
        private String msgid;
        private int state;//1：等待确认  -1：已取消

        public String getSourceid() {
            return sourceid;
        }

        public void setSourceid(String sourceid) {
            this.sourceid = sourceid;
        }

        public String getDevid() {
            return devid;
        }

        public void setDevid(String devid) {
            this.devid = devid;
        }

        public String getEventid() {
            return eventid;
        }

        public void setEventid(String eventid) {
            this.eventid = eventid;
        }

        public Locinfo getLocinfo() {
            return locinfo;
        }

        public void setLocinfo(Locinfo locinfo) {
            this.locinfo = locinfo;
        }

        public String getTargetid() {
            return targetid;
        }

        public void setTargetid(String targetid) {
            this.targetid = targetid;
        }

        public String getAddtime() {
            return addtime;
        }

        public void setAddtime(String addtime) {
            this.addtime = addtime;
        }

        public String getMsgid() {
            return msgid;
        }

        public void setMsgid(String msgid) {
            this.msgid = msgid;
        }

        public int getState() {
            return state;
        }

        public void setState(int state) {
            this.state = state;
        }
    }

}
