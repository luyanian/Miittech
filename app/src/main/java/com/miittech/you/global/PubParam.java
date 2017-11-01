package com.miittech.you.global;

import com.miittech.you.global.Params;

import java.io.Serializable;

/**
 * Created by Administrator on 2017/9/12.
 */

public class PubParam implements Serializable {
    private String appid="YOU";
    private String userid;
    private Long timestamp = Params.timeStamp();
    private String version="1.0";
    private String format="json";
    private String sign_method="sha1";

    public PubParam(String userid) {
        this.userid = userid;
    }

    public String toValueString(){
        return appid+userid+timestamp+version+format+sign_method;
    }

    public String toUrlParam(String sign){
        return "?appid="+appid+"&userid="+userid+"&timestamp="+timestamp+"&version="+version+"&format="+format+"&sign_method="+sign_method+"&sign="+sign;
    }

}
