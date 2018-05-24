package com.miittech.you.net.response;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import com.google.gson.annotations.SerializedName;
import com.miittech.you.App;
import com.miittech.you.R;
import com.miittech.you.activity.user.LoginRegisteActivity;
import com.miittech.you.utils.Common;
import com.miittech.you.global.IntentExtras;
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
    private int configid;
    private String devid;//蓝牙有物的出厂id
    private int valid;//1:可用，0：不可用(已被其他绑int定),-1：不合法有物id
    private int bindstate;//1：已绑定 0：未绑定


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

    public int getConfigid() {
        return configid;
    }

    public void setConfigid(int configid) {
        this.configid = configid;
    }

    public String getDevid() {
        return devid;
    }

    public void setDevid(String devid) {
        this.devid = devid;
    }

    public int getValid() {
        return valid;
    }

    public void setValid(int valid) {
        this.valid = valid;
    }

    public int getBindstate() {
        return bindstate;
    }

    public void setBindstate(int bindstate) {
        this.bindstate = bindstate;
    }

    @Override
    public String toString() {
        return "BaseResponse{" +
                "errcodeX=" + errcodeX +
                ", clientid='" + clientid + '\'' +
                ", url='" + url + '\'' +
                ", errmsg='" + errmsg + '\'' +
                ", configid='" + configid + '\'' +
                '}';
    }

    public boolean isSuccessful(){
        return this.errcodeX== ResponseCode.successful;
    }

    public boolean unExsitUser(){
        return this.errcodeX==ResponseCode.user_notexist;
    }

    public void onError(Context context){
        ToastUtils.showShort(errmsg);
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
                if(!TextUtils.isEmpty(Common.getTocken())) {
                    Common.ValidToken(context);
                }
                break;
            case ResponseCode.invalid_token:
            case ResponseCode.invalid_sign_data:
                ToastUtils.showShort("异地登陆执行退出");
                Intent cmd= new Intent(IntentExtras.ACTION.ACTION_BLE_COMMAND);
                cmd.putExtra("cmd",IntentExtras.CMD.CMD_DEVICE_LIST_CLEAR);
                App.getInstance().getLocalBroadCastManager().sendBroadcast(cmd);
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

    public boolean isVerSuccessful() {
        return (isSuccessful()&&this.valid==1);
    }

    public boolean isBindSuccessful(){
        return (isSuccessful()&&this.bindstate==1);
    }

    public void onVerError(){
        switch (this.valid){
            case 1:
                break;
            case 0:
                ToastUtils.showShort("设备已被绑定或者设备不可用！");
                break;
            case -1:
                ToastUtils.showShort("不合法设备！");
                break;
        }
    }
}
