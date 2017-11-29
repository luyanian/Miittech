package com.miittech.you.common;

import android.content.Context;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Base64;

import com.google.gson.Gson;
import com.miittech.you.App;
import com.miittech.you.R;
import com.miittech.you.entity.Detailinfo;
import com.miittech.you.entity.Locinfo;
import com.miittech.you.entity.Repdata;
import com.miittech.you.global.SPConst;
import com.miittech.you.net.ApiServiceManager;
import com.miittech.you.global.HttpUrl;
import com.miittech.you.global.Params;
import com.miittech.you.global.PubParam;
import com.miittech.you.net.response.BaseResponse;
import com.miittech.you.net.response.DeviceResponse;
import com.miittech.you.service.BleService;
import com.ryon.mutils.ConvertUtils;
import com.ryon.mutils.EncodeUtils;
import com.ryon.mutils.EncryptUtils;
import com.ryon.mutils.LogUtils;
import com.ryon.mutils.RegexUtils;
import com.ryon.mutils.SPUtils;
import com.ryon.mutils.SpanUtils;
import com.ryon.mutils.StringUtils;
import com.ryon.mutils.ToastUtils;

import java.io.UnsupportedEncodingException;
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
    public static void doCommitEvents(Context context,String devId, int eventType,Detailinfo detailinfo){
        Map param = new HashMap();
        param.put("devid", devId);
        param.put("eventtime", Common.getCurrentTime());
        param.put("eventype", eventType);
        param.put("locinfo", Common.decodeBase64(SPUtils.getInstance().getString(SPConst.LOCATION_ADDRE)));
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
            builder.append(temp[temp.length - 1 - i]);

        }
        return builder.toString();
    }
    public static String formatDevId2Mac(String devId){
        StringBuilder builder = new StringBuilder();
        for(int i=0;i<devId.length();i++){
            if(i%2==1){
                builder.append(devId.charAt(devId.length()-1-i));
                builder.append(devId.charAt(devId.length()-i));
                builder.append(":");
            }
        }
        String temp = builder.toString();
        return temp.substring(0,temp.length()-1);
    }
    public static String decodeBase64(String text){
        if(TextUtils.isEmpty(text)){
            return "";
        }
        try {
            return new String(Base64.decode(text.getBytes("utf-8"), Base64.NO_WRAP));
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }
    public static String encodeBase64(String text){
        if(TextUtils.isEmpty(text)){
            return "";
        }
        try {
            return new String(Base64.encode(text.getBytes("utf-8"), Base64.NO_WRAP));
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }
    public static String getCurrentTime(){
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");
        return format.format(new Date());
    }

    public static byte[] formatBleMsg(int modeBind, String msg){
//        byte[] temp = ConvertUtils.hexString2Bytes(msg);
        byte[] temp = ConvertUtils.str2HexStr1(msg);
        byte[] data = new byte[temp.length+1];
        if(modeBind==Params.BLEMODE.MODE_BIND){
            data[0] = 0x02;
        }else if(modeBind==Params.BLEMODE.MODE_WORK) {
            data[0] = 0x01;
        }else if(modeBind==Params.BLEMODE.MODE_UNBIND){
            data[0] = 0x04;
        }
        for (int i=0;i<temp.length;i++){
            data[i+1]=temp[i];
        }
        return  data;
    }

    /**
     * 检测辅助功能是否开启<br>
     * 方 法 名：isAccessibilitySettingsOn <br>
     * @param mContext
     * @return boolean
     */
    public static boolean isAccessibilitySettingsOn(Context mContext) {
        int accessibilityEnabled = 0;
        final String service = mContext.getPackageName() + "/" + BleService.class.getCanonicalName();

        try {
            accessibilityEnabled = Settings.Secure.getInt(mContext.getApplicationContext().getContentResolver(),
                    android.provider.Settings.Secure.ACCESSIBILITY_ENABLED);
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }
        TextUtils.SimpleStringSplitter mStringColonSplitter = new TextUtils.SimpleStringSplitter(':');
        if (accessibilityEnabled == 1) {
            String settingValue = Settings.Secure.getString(mContext.getApplicationContext().getContentResolver(),Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (settingValue != null) {
                mStringColonSplitter.setString(settingValue);
                while (mStringColonSplitter.hasNext()) {
                    String accessibilityService = mStringColonSplitter.next();
                    if (accessibilityService.equalsIgnoreCase(service)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
