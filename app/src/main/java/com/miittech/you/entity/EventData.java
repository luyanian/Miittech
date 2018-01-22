package com.miittech.you.entity;

import com.miittech.you.utils.Common;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Administrator on 2018/1/22.
 */

public class EventData implements Serializable {
    private String devid;
    private String eventtime;
    private int eventype;

    public String getDevid() {
        return devid;
    }

    public void setDevid(String devid) {
        this.devid = devid;
    }

    public String getEventtime() {
        return eventtime;
    }

    public void setEventtime(String eventtime) {
        this.eventtime = eventtime;
    }

    public int getEventype() {
        return eventype;
    }

    public void setEventype(int eventype) {
        this.eventype = eventype;
    }
}
