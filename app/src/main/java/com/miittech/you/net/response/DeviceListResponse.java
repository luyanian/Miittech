package com.miittech.you.net.response;

import com.google.gson.annotations.SerializedName;
import com.miittech.you.entity.DeviceInfo;

import java.io.Serializable;
import java.util.List;

/**
 * Created by Administrator on 2017/10/18.
 */

public class DeviceListResponse extends BaseResponse implements Serializable {
    private List<DeviceInfo> devlist;

    public List<DeviceInfo> getDevlist() {
        return devlist;
    }

    public void setDevlist(List<DeviceInfo> devlist) {
        this.devlist = devlist;
    }

}
