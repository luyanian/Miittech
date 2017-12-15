package com.miittech.you.entity;

import java.io.Serializable;

/**
 * Created by Administrator on 2017/12/15.
 */

public class JpushMsg implements Serializable {
    private String msg_content;
    private String title;
    private String content_type;
    private Extras extras;

    public String getMsg_content() {
        return msg_content;
    }

    public void setMsg_content(String msg_content) {
        this.msg_content = msg_content;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent_type() {
        return content_type;
    }

    public void setContent_type(String content_type) {
        this.content_type = content_type;
    }

    public Extras getExtras() {
        return extras;
    }

    public void setExtras(Extras extras) {
        this.extras = extras;
    }

    public class Extras implements Serializable {
        private String msgid;
        private String eventid;
        private String sourceid;
        private String targetid;
        private String logintime;
        private String devid;
        private String sharetime;
        private String addtime;
        private String validstr;
        private String validtime;
        private Locinfo locinfo;
        private int state;

        public String getMsgid() {
            return msgid;
        }

        public void setMsgid(String msgid) {
            this.msgid = msgid;
        }

        public String getEventid() {
            return eventid;
        }

        public void setEventid(String eventid) {
            this.eventid = eventid;
        }

        public String getSourceid() {
            return sourceid;
        }

        public void setSourceid(String sourceid) {
            this.sourceid = sourceid;
        }

        public String getTargetid() {
            return targetid;
        }

        public void setTargetid(String targetid) {
            this.targetid = targetid;
        }

        public String getLogintime() {
            return logintime;
        }

        public void setLogintime(String logintime) {
            this.logintime = logintime;
        }

        public String getDevid() {
            return devid;
        }

        public void setDevid(String devid) {
            this.devid = devid;
        }

        public String getSharetime() {
            return sharetime;
        }

        public void setSharetime(String sharetime) {
            this.sharetime = sharetime;
        }

        public String getAddtime() {
            return addtime;
        }

        public void setAddtime(String addtime) {
            this.addtime = addtime;
        }

        public String getValidstr() {
            return validstr;
        }

        public void setValidstr(String validstr) {
            this.validstr = validstr;
        }

        public String getValidtime() {
            return validtime;
        }

        public void setValidtime(String validtime) {
            this.validtime = validtime;
        }

        public Locinfo getLocinfo() {
            return locinfo;
        }

        public void setLocinfo(Locinfo locinfo) {
            this.locinfo = locinfo;
        }

        public int getState() {
            return state;
        }

        public void setState(int state) {
            this.state = state;
        }
    }

}
