package com.miittech.you.entity;

import java.io.Serializable;

/**
 * Created by Administrator on 2017/12/5.
 */

public class JpushFriend implements Serializable {
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
        private String nsgid;
        private String eventid;
        private String sourceid;
        private String targetid;
        private String addtime;
        private int state;//0/1/-1 分别是等待确认/已分享/已拒绝或取消

        public String getNsgid() {
            return nsgid;
        }

        public void setNsgid(String nsgid) {
            this.nsgid = nsgid;
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

        public String getAddtime() {
            return addtime;
        }

        public void setAddtime(String addtime) {
            this.addtime = addtime;
        }

        public int getState() {
            return state;
        }

        public void setState(int state) {
            this.state = state;
        }
    }

}
