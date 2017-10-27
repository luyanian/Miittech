package com.miittech.you.entity;

import com.miittech.you.common.Common;

import java.io.Serializable;

/**
 * Created by Administrator on 2017/10/27.
 */

public class Locinfo implements Serializable {
    private double lat;//事件的纬度值(WGS84坐标系)
    private double lng;//事件的经度值(WGS84坐标系)
    private String addr;//地址名称（base64）

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
