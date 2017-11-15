package com.miittech.you.common;

import android.content.Context;
import android.util.Base64;

import com.google.gson.Gson;
import com.miittech.you.App;
import com.miittech.you.R;
import com.miittech.you.entity.Detailinfo;
import com.miittech.you.entity.Locinfo;
import com.miittech.you.entity.Repdata;
import com.miittech.you.net.ApiServiceManager;
import com.miittech.you.global.HttpUrl;
import com.miittech.you.global.Params;
import com.miittech.you.global.PubParam;
import com.miittech.you.net.response.BaseResponse;
import com.miittech.you.net.response.DeviceResponse;
import com.ryon.mutils.ConvertUtils;
import com.ryon.mutils.EncryptUtils;
import com.ryon.mutils.LogUtils;
import com.ryon.mutils.RegexUtils;
import com.ryon.mutils.StringUtils;
import com.ryon.mutils.ToastUtils;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import okhttp3.MediaType;
import okhttp3.RequestBody;

/**
 * Created by Administrator on 2017/9/21.
 */

public class Common {
    public static void getMsgCode(Context context, String phone, final OnGetVerCodeComplete onGetVerCodeComplete) {

        if(!RegexUtils.isMobileSimple(phone)){
            ToastUtils.showShort(R.string.tip_ver_phone_faild);
            return;
        }
        Map param = new HashMap();
        param.put("vtype", Params.VTYPE.PHONELOGIN);
        param.put("phone", phone);
        String json = new Gson().toJson(param);
        LogUtils.d("vcodeb", json);
        PubParam pubParam = new PubParam(Params.userid_unlogin);
        String sign = EncryptUtils.encryptSHA1ToString(pubParam.toValueString() + json + Params.signkey_unlogin).toLowerCase();
        LogUtils.d("sign", sign);
        String path = HttpUrl.Api + "vcodeb/" + pubParam.toUrlParam(sign);
        RequestBody requestBody = RequestBody.create(MediaType.parse(HttpUrl.MediaType_Json), json);

        ApiServiceManager.getInstance().buildApiService(context).postNetRequest(path, requestBody)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<BaseResponse>() {
                    @Override
                    public void accept(BaseResponse response) throws Exception {
                        if (response.isSuccessful()) {
                            onGetVerCodeComplete.onSuccessful(response.getClientid());
                        }
                    }
                });
    }
    public static void doCommitEvents(Context context,String devId, String eventTime, int eventType, Locinfo locinfo, Detailinfo detailinfo){
        Map param = new HashMap();
        param.put("devid", devId);
        param.put("eventtime", eventTime);
        param.put("eventype", eventType);
        if(locinfo!=null) {
            param.put("locinfo", locinfo);
        }
        if(detailinfo!=null) {
            param.put("detailinfo", detailinfo);
        }
        String json = new Gson().toJson(param);
        PubParam pubParam = new PubParam(App.getInstance().getUserId());
        String sign_unSha1 = pubParam.toValueString() + json + App.getInstance().getTocken();
        LogUtils.d("sign_unsha1", sign_unSha1);
        String sign = EncryptUtils.encryptSHA1ToString(sign_unSha1).toLowerCase();
        LogUtils.d("sign_sha1", sign);
        String path = HttpUrl.Api + "devevent/" + pubParam.toUrlParam(sign);
        final RequestBody requestBody = RequestBody.create(MediaType.parse(HttpUrl.MediaType_Json), json);
        ApiServiceManager.getInstance().buildApiService(context).postDeviceOption(path, requestBody)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<DeviceResponse>() {
                    @Override
                    public void accept(DeviceResponse response) throws Exception {
                        if(response.isSuccessful()) {

                        }else{

                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        throwable.printStackTrace();
                    }
                });
    }
    public static void doCommitDeviceStatus(Context context,int method, Repdata repData){

        Map param = new HashMap();
        param.put("method", method);
        param.put("repdata", repData);
        String json = new Gson().toJson(param);
        PubParam pubParam = new PubParam(App.getInstance().getUserId());
        String sign_unSha1 = pubParam.toValueString() + json + App.getInstance().getTocken();
        LogUtils.d("sign_unsha1", sign_unSha1);
        String sign = EncryptUtils.encryptSHA1ToString(sign_unSha1).toLowerCase();
        LogUtils.d("sign_sha1", sign);
        String path = HttpUrl.Api + "userreport/" + pubParam.toUrlParam(sign);
        final RequestBody requestBody = RequestBody.create(MediaType.parse(HttpUrl.MediaType_Json), json);
        ApiServiceManager.getInstance().buildApiService(context).postDeviceOption(path, requestBody)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<DeviceResponse>() {
                    @Override
                    public void accept(DeviceResponse response) throws Exception {
                        if(response.isSuccessful()) {

                        }else{

                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        throwable.printStackTrace();
                    }
                });
    }
    public static String formatMac2DevId(String address){
        String[] temp = address.toUpperCase().split(":");
        StringBuilder builder = new StringBuilder();
        for (int i=0;i<temp.length;i++){
            builder.append(temp[temp.length-1-i]);
        }
        return builder.toString();
    }
    public static String formatDevId2Mac(String devId){
        StringBuilder builder = new StringBuilder();
        for(int i=0;i<devId.length();i++){
            if(i%2==1){
                builder.append(devId.length()-1-i);
                builder.append(devId.length()-i);
                builder.append(":");
            }
        }
        String temp = builder.toString();
        return temp.substring(0,temp.length()-2);
    }
    public static String decodeBase64(String text){
        return new String(Base64.decode(text, Base64.DEFAULT));
    }
    public static String encodeBase64(String text){
        return  Base64.encodeToString(text.getBytes(),Base64.DEFAULT);
    }
    public static String getCurrentTime(){
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");
        return format.format(new Date());
    }

    public static String formatTimeStr(String time){
        String temp = time;

        return temp;
    }
    public static byte[] formatBleMsg(int modeBind, String msg){
        byte[] temp = ConvertUtils.hexString2Bytes(msg);
        byte[] data = new byte[temp.length+1];
        if(modeBind==Params.BLEMODE.MODE_BIND){
            data[0] = 02;
        }else if(modeBind==Params.BLEMODE.MODE_WORK) {
            data[0] = 01;
        }else if(modeBind==Params.BLEMODE.MODE_UNBIND){
            data[0] = 04;
        }
        for (int i=0;i<temp.length;i++){
            data[i+1]=temp[i];
        }
        return  data;
    }
}
