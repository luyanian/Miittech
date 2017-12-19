package com.miittech.you.net.response;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import com.google.gson.annotations.SerializedName;
import com.miittech.you.App;
import com.miittech.you.R;
import com.miittech.you.activity.user.LoginRegisteActivity;
import com.miittech.you.activity.user.UserCenterActivity;
import com.miittech.you.common.Common;
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
                context.sendBroadcast(cmd);
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
