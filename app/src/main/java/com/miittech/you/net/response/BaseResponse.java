package com.miittech.you.net.response;

import android.content.Context;
import android.content.Intent;

import com.google.gson.annotations.SerializedName;
import com.miittech.you.App;
import com.miittech.you.R;
import com.miittech.you.activity.user.LoginRegisteActivity;
import com.miittech.you.activity.user.UserCenterActivity;
import com.miittech.you.global.SPConst;
import com.miittech.you.net.code.ResponseCode;
import com.ryon.mutils.ActivityPools;
import com.ryon.mutils.SPUtils;
import com.ryon.mutils.ToastUtils;
import java.io.Serializable;

/**
 * Created by ryon on 2017/6/10.
 */

public class BaseResponse implements Serializable{

    /**
     * clientid : l60hbk
     * errcode : 0
     */
    @SerializedName("errcode")
    private int errcodeX;
    private String clientid;
    private String url;
    private String errmsg;
    private String username;
    private String token;
    private String userid;

    public String getClientid() {
        return clientid;
    }

    public void setClientid(String clientid) {
        this.clientid = clientid;
    }

    public int getErrcodeX() {
        return errcodeX;
    }

    public void setErrcodeX(int errcodeX) {
        this.errcodeX = errcodeX;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getErrmsg() {
        return errmsg;
    }

    public void setErrmsg(String errmsg) {
        this.errmsg = errmsg;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getUserid() {
        return userid;
    }

    public void setUserid(String userid) {
        this.userid = userid;
    }

    @Override
    public String toString() {
        return "BaseResponse{" +
                "errcodeX=" + errcodeX +
                ", clientid='" + clientid + '\'' +
                ", url='" + url + '\'' +
                ", errmsg='" + errmsg + '\'' +
                ", token='" + token + '\'' +
                ", userid='" + userid + '\'' +
                '}';
    }

    public boolean isSuccessful(){
        return this.errcodeX== ResponseCode.successful;
    }

    public void onError(Context context){
        switch (errcodeX){
            case ResponseCode.user_notexist:
                ToastUtils.showShort(R.string.msg_user_not_exist);
                break;
            case ResponseCode.user_invalidpasswd:
                ToastUtils.showShort(R.string.msg_user_invalid_password);
                break;
            case ResponseCode.user_account_already_bind:
                ToastUtils.showShort(R.string.msg_account_already_bind);
                break;
            case ResponseCode.invalid_sign://签名错误，请重新登录
                ToastUtils.showShort("登录信息已失效，请重新登录");
                SPUtils.getInstance(SPConst.USER.SP_NAME).clear();
                Intent intent = new Intent(context,LoginRegisteActivity.class);
                context.startActivity(intent);
                ActivityPools.finishAllExcept(LoginRegisteActivity.class);
                break;
            default:
                ToastUtils.showShort(errmsg);
                break;
        }
    }
}
