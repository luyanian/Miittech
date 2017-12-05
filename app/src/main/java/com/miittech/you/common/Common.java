package com.miittech.you.common;

import android.content.Context;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Base64;

import com.baidu.location.BDLocation;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.utils.DistanceUtil;
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
import com.miittech.you.net.response.FriendsResponse;
import com.miittech.you.net.response.UserInfoResponse;
import com.miittech.you.service.BleService;
import com.miittech.you.utils.HexUtil;
import com.ryon.mutils.ConvertUtils;
import com.ryon.mutils.EncodeUtils;
import com.ryon.mutils.EncryptUtils;
import com.ryon.mutils.LogUtils;
import com.ryon.mutils.NetworkUtils;
import com.ryon.mutils.RegexUtils;
import com.ryon.mutils.SPUtils;
import com.ryon.mutils.SpanUtils;
import com.ryon.mutils.StringUtils;
import com.ryon.mutils.TimeUtils;
import com.ryon.mutils.ToastUtils;

import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
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
    public synchronized static void getMsgCode(Context context, String phone, final OnGetVerCodeComplete onGetVerCodeComplete) {

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
    public synchronized static void doCommitEvents(Context context,String devId, int eventType,Detailinfo detailinfo){
        Map param = new HashMap();
        param.put("devid", devId);
        param.put("eventtime", Common.getCurrentTime());
        param.put("eventype", eventType);
        Map locinfo = new HashMap();
        Locinfo location = (Locinfo) SPUtils.getInstance().readObject(SPConst.LOC_INFO);
        if(location!=null) {
            locinfo.put("addr", Common.encodeBase64(location.getAddr()));
            locinfo.put("lat", location.getLat());
            locinfo.put("lng", location.getLng());
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
    public synchronized static void eventConfirm(final Context context, String eventId, String method){
        Map param = new HashMap();
        param.put("eventid", eventId);
        param.put("method", method);
        String json = new Gson().toJson(param);
        PubParam pubParam = new PubParam(App.getInstance().getUserId());
        String sign_unSha1 = pubParam.toValueString() + json + App.getInstance().getTocken();
        LogUtils.d("sign_unsha1", sign_unSha1);
        String sign = EncryptUtils.encryptSHA1ToString(sign_unSha1).toLowerCase();
        LogUtils.d("sign_sha1", sign);
        String path = HttpUrl.Api + "eventconfirm/" + pubParam.toUrlParam(sign);
        final RequestBody requestBody = RequestBody.create(MediaType.parse(HttpUrl.MediaType_Json), json);
        ApiServiceManager.getInstance().buildApiService(context).postNetRequest(path, requestBody)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<BaseResponse>() {
                    @Override
                    public void accept(BaseResponse response) throws Exception {
                        if(response.isSuccessful()) {

                        }else{
                            response.onError(context);
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        throwable.printStackTrace();
                    }
                });
    }
    public synchronized static void AddFriendConfirm(final Context context, String friendId, String method) {
        Map param = new HashMap();
        param.put("method", method);
        param.put("friended", friendId);
        String json = new Gson().toJson(param);
        PubParam pubParam = new PubParam(App.getInstance().getUserId());
        String sign_unSha1 = pubParam.toValueString() + json + App.getInstance().getTocken();
        LogUtils.d("sign_unsha1", sign_unSha1);
        String sign = EncryptUtils.encryptSHA1ToString(sign_unSha1).toLowerCase();
        LogUtils.d("sign_sha1", sign);
        String path = HttpUrl.Api + "friend/" + pubParam.toUrlParam(sign);
        final RequestBody requestBody = RequestBody.create(MediaType.parse(HttpUrl.MediaType_Json), json);

        ApiServiceManager.getInstance().buildApiService(context).postToGetFriendList(path, requestBody)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<FriendsResponse>() {
                    @Override
                    public void accept(FriendsResponse response) throws Exception {
                        if(response.isSuccessful()){

                        }else{
                            response.onError(context);
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
//        byte[] temp = ConvertUtils.str2HexStr1(msg);
        byte[] temp = HexUtil.decodeHex(HexUtil.encodeHex(msg.getBytes()));
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

    public static boolean isUserContainDevice(String address) {
        DeviceResponse response = (DeviceResponse) SPUtils.getInstance().readObject(SPConst.DATA.DEVICELIST);
        if(response!=null){
            List<DeviceResponse.DevlistBean> list = response.getDevlist();
            if(list==null||list.size()<=0){
                return false;
            }
            for (DeviceResponse.DevlistBean devlistBean : list){
                if(address.equals(Common.formatDevId2Mac(devlistBean.getDevidX()))&&TextUtils.isEmpty(devlistBean.getFriendname())){
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isIgnoreBell() {
        UserInfoResponse response = (UserInfoResponse) SPUtils.getInstance().readObject(SPConst.USER_INFO);
        if(response!=null&&response.getConfig()!=null){
            UserInfoResponse.ConfigBean configBean = response.getConfig();
            if(configBean.getDonotdisturb()!=null){
                UserInfoResponse.ConfigBean.DonotdisturbBean donotdisturbBean = configBean.getDonotdisturb();
                if(isAreaIgnore(donotdisturbBean.getArealist())||isTimeIgnore(donotdisturbBean.getTimelist())){
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isAreaIgnore(List<UserInfoResponse.ConfigBean.DonotdisturbBean.ArealistBean> arealist){
        if(arealist!=null&&arealist.size()>0){
            for (UserInfoResponse.ConfigBean.DonotdisturbBean.ArealistBean arealistBean : arealist){
                if(arealistBean!=null&&!TextUtils.isEmpty(arealistBean.getSsid())&&arealistBean.getSsid().equals(NetworkUtils.getSsidOfConnectWifi())){
                    return true;
                }
                UserInfoResponse.ConfigBean.DonotdisturbBean.ArealistBean.AreaBean areaBean = arealistBean.getArea();
                if(areaBean!=null&&areaBean.getLat()<=0&&areaBean.getLat()<=0){
                    Locinfo locinfo = (Locinfo) SPUtils.getInstance().readObject(SPConst.LOC_INFO);
                    LatLng latLng1 = new LatLng(areaBean.getLat(),areaBean.getLng());
                    LatLng latLng2 = new LatLng(locinfo.getLat(),locinfo.getLng());
                    if(DistanceUtil.getDistance(latLng1, latLng2)<areaBean.getR()){
                        return true;
                    }
                }
            }
        }
        return false;
    }
    public static boolean isTimeIgnore(List<UserInfoResponse.ConfigBean.DonotdisturbBean.TimelistBean> timelist){
        if(timelist==null||timelist.size()<=0){
            return false;
        }
        for(UserInfoResponse.ConfigBean.DonotdisturbBean.TimelistBean timelistBean : timelist){
            int index = TimeUtils.getWeekIndex(new Date());
            String week = timelistBean.getDayofweek();
            if(TextUtils.isEmpty(week)){
                return false;
            }
            index = (index+7-1)%7;

            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd");
            Date date = new Date();

            String ymd = simpleDateFormat.format(date);
            SimpleDateFormat format = new SimpleDateFormat("yyyyMMddhhmmss");
            long stime = TimeUtils.string2Millis(ymd+timelistBean.getStime(),format);
            long etime = TimeUtils.string2Millis(ymd+timelistBean.getEtime(),format);
            long curTime = TimeUtils.getNowMills();

            if(curTime>stime&&curTime<etime&&week.contains(index+"")){
                return true;
            }
        }
        return false;
    }
}
