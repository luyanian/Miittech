package com.miittech.you.entity;

import java.io.Serializable;

/**
 * Created by Administrator on 2017/10/27.
 */

public class Detailinfo implements Serializable {
    private String fromuserid;//贴片拥有者userid
    private String fromnickname;//贴片拥有者nickname
    private String touserid;//被分享贴片的userid

    public String getFromuserid() {
        return fromuserid;
    }

    public void setFromuserid(String fromuserid) {
        this.fromuserid = fromuserid;
    }

    public String getFromnickname() {
        return fromnickname;
    }

    public void setFromnickname(String fromnickname) {
        this.fromnickname = fromnickname;
    }

    public String getTouserid() {
        return touserid;
    }

    public void setTouserid(String touserid) {
        this.touserid = touserid;
    }
}
