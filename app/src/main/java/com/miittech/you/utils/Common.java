package com.miittech.you.utils;

import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.support.v4.app.NotificationManagerCompat;
import android.text.TextUtils;
import android.util.Base64;

import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.utils.DistanceUtil;
import com.google.gson.Gson;
import com.miittech.you.App;
import com.miittech.you.R;
import com.miittech.you.dialog.DialogUtils;
import com.miittech.you.dialog.UpdateDialog;
import com.miittech.you.entity.Detailinfo;
import com.miittech.you.entity.DeviceInfo;
import com.miittech.you.entity.Locinfo;
import com.miittech.you.global.HttpUrl;
import com.miittech.you.global.Params;
import com.miittech.you.global.PubParam;
import com.miittech.you.global.SPConst;
import com.miittech.you.impl.OnGetVerCodeComplete;
import com.miittech.you.impl.OnMsgTipOptions;
import com.miittech.you.impl.OnNetRequestCallBack;
import com.miittech.you.net.ApiServiceManager;
import com.miittech.you.net.response.AppVersionResponse;
import com.miittech.you.net.response.BaseResponse;
import com.miittech.you.net.response.DeviceDetailResponse;
import com.miittech.you.net.response.DeviceListResponse;
import com.miittech.you.net.response.FriendsResponse;
import com.miittech.you.net.response.UserInfoResponse;
import com.miittech.you.service.BleService;
import com.ryon.mutils.AppUtils;
import com.ryon.mutils.EncryptUtils;
import com.ryon.mutils.LogUtils;
import com.ryon.mutils.NetworkUtils;
import com.ryon.mutils.RegexUtils;
import com.ryon.mutils.SPUtils;
import com.ryon.mutils.TimeUtils;
import com.ryon.mutils.ToastUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import okhttp3.MediaType;
import okhttp3.RequestBody;

import static android.content.Context.DOWNLOAD_SERVICE;

/**
 * Created by Administrator on 2017/9/21.
 */

public class Common {
    public synchronized static void getMsgCode(final Context context, String phone, final OnGetVerCodeComplete onGetVerCodeComplete) {

        if(!RegexUtils.isMobileSimple(phone)){
            ToastUtils.showShort(R.string.tip_ver_phone_faild);
            if(onGetVerCodeComplete!=null) {
                onGetVerCodeComplete.onRequestStart();
            }
            return;
        }
        if(!NetworkUtils.isConnected()){
            ToastUtils.showShort(R.string.msg_net_error);
            if(onGetVerCodeComplete!=null) {
                onGetVerCodeComplete.onRequestStart();
            }
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
                            if(onGetVerCodeComplete!=null) {
                                onGetVerCodeComplete.onSuccessful(response.getClientid());
                            }
                        }else{
                            response.onError(context);
                        }
                    }
                });
    }
    public synchronized static void doCommitEvents(final Context context, String devId, int eventType, Detailinfo detailinfo){
        if(!NetworkUtils.isConnected()){
            return;
        }
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
        PubParam pubParam = new PubParam(Common.getUserId());
        String sign_unSha1 = pubParam.toValueString() + json + Common.getTocken();
        LogUtils.d("sign_unsha1", sign_unSha1);
        String sign = EncryptUtils.encryptSHA1ToString(sign_unSha1).toLowerCase();
        LogUtils.d("sign_sha1", sign);
        String path = HttpUrl.Api + "devevent/" + pubParam.toUrlParam(sign);
        final RequestBody requestBody = RequestBody.create(MediaType.parse(HttpUrl.MediaType_Json), json);
        ApiServiceManager.getInstance().buildApiService(context).postDeviceOption(path, requestBody)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<DeviceListResponse>() {
                    @Override
                    public void accept(DeviceListResponse response) throws Exception {
                        if(!response.isSuccessful()) {
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
    public synchronized static void eventConfirm(final Context context, String eventId, String method){
        if(!NetworkUtils.isConnected()){
            return;
        }
        Map param = new HashMap();
        param.put("eventid", eventId);
        param.put("method", method);
        String json = new Gson().toJson(param);
        PubParam pubParam = new PubParam(Common.getUserId());
        String sign_unSha1 = pubParam.toValueString() + json + Common.getTocken();
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
                        if(!response.isSuccessful()) {
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
    public synchronized static void getUserInfo(final Context context, final OnNetRequestCallBack onNetRequestCallBack){
        if(!NetworkUtils.isConnected()){
            if(onNetRequestCallBack!=null){
                onNetRequestCallBack.OnRequestComplete();
            }
            return;
        }
        Map param = new HashMap();
        param.put("qrytype", Params.QRY_TYPE.ALL);
        String json = new Gson().toJson(param);
        PubParam pubParam = new PubParam(Common.getUserId());
        String sign_unSha1 = pubParam.toValueString() + json + Common.getTocken();
        LogUtils.d("sign_unsha1", sign_unSha1);
        String sign = EncryptUtils.encryptSHA1ToString(sign_unSha1).toLowerCase();
        LogUtils.d("sign_sha1", sign);
        String path = HttpUrl.Api + "userinfo/" + pubParam.toUrlParam(sign);
        RequestBody requestBody = RequestBody.create(MediaType.parse(HttpUrl.MediaType_Json), json);
        ApiServiceManager.getInstance().buildApiService(context).postToGetUserInfo(path, requestBody)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new Consumer<UserInfoResponse>() {
                @Override
                public void accept(UserInfoResponse response) throws Exception {
                    if(response.isSuccessful()){
                        SPUtils.getInstance(SPConst.USER.SP_NAME).put(SPConst.USER.KEY_NIKENAME,response.getUserinfo().getNickname());
                        SPUtils.getInstance(SPConst.USER.SP_NAME).put(SPConst.USER.KEY_IMAGE,response.getUserinfo().getHeadimg());
                        SPUtils.getInstance().remove(SPConst.DATA.USERINFO);
                        SPUtils.getInstance().saveObject(SPConst.DATA.USERINFO,response);
                    }else{
                        response.onError(context);
                    }
                    if(onNetRequestCallBack!=null){
                        onNetRequestCallBack.OnRequestComplete();
                    }
                }
            }, new Consumer<Throwable>() {
                @Override
                public void accept(Throwable throwable) throws Exception {
                    throwable.printStackTrace();
                    if(onNetRequestCallBack!=null){
                        onNetRequestCallBack.OnRequestComplete();
                    }
                }
            });
    }
    public synchronized static void getDeviceDetailInfo(final Context context, final String devId, final OnNetRequestCallBack onNetRequestCallBack){
        if(!NetworkUtils.isConnected()){
            if(onNetRequestCallBack!=null){
                onNetRequestCallBack.OnRequestComplete();
            }
            ToastUtils.showShort("网络链接断开，请检查网络");
            return;
        }
        Map param = new HashMap();
        param.put("devid", devId);
        param.put("qrytype", Params.QRY_TYPE.ALL);
        String json = new Gson().toJson(param);
        PubParam pubParam = new PubParam(Common.getUserId());
        String sign_unSha1 = pubParam.toValueString() + json + Common.getTocken();
        LogUtils.d("sign_unsha1", sign_unSha1);
        String sign = EncryptUtils.encryptSHA1ToString(sign_unSha1).toLowerCase();
        LogUtils.d("sign_sha1", sign);
        String path = HttpUrl.Api + "deviceinfo/" + pubParam.toUrlParam(sign);
        final RequestBody requestBody = RequestBody.create(MediaType.parse(HttpUrl.MediaType_Json), json);

        ApiServiceManager.getInstance().buildApiService(context).postDeviceInfoOption(path, requestBody)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<DeviceDetailResponse>() {
                    @Override
                    public void accept(DeviceDetailResponse response) throws Exception {
                        if (response.isSuccessful()) {
                            SPUtils.getInstance().remove(Common.formatDevId2Mac(devId));
                            SPUtils.getInstance().saveObject(Common.formatDevId2Mac(devId),response.getUserinfo().getDevinfo());
                        } else {
                            response.onError(context);
                        }
                        if(onNetRequestCallBack!=null){
                            onNetRequestCallBack.OnRequestComplete();
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        throwable.printStackTrace();
                        if(onNetRequestCallBack!=null){
                            onNetRequestCallBack.OnRequestComplete();
                        }
                    }
                });
    }
    public synchronized static void getAppVersion(final Context context, final boolean isAuto){
        if(!NetworkUtils.isConnected()){
            ToastUtils.showShort("网络链接断开，请检查网络");
            return;
        }
        Map param = new HashMap();
        param.put("ostype", "android");
        param.put("ver", AppUtils.getAppVersionName());
        String json = new Gson().toJson(param);
        PubParam pubParam = new PubParam(Common.getUserId());
        String sign_unSha1 = pubParam.toValueString() + json + Common.getTocken();
        LogUtils.d("sign_unsha1", sign_unSha1);
        String sign = EncryptUtils.encryptSHA1ToString(sign_unSha1).toLowerCase();
        LogUtils.d("sign_sha1", sign);
        String path = HttpUrl.Api + "appversion/" + pubParam.toUrlParam(sign);
        final RequestBody requestBody = RequestBody.create(MediaType.parse(HttpUrl.MediaType_Json), json);

        ApiServiceManager.getInstance().buildApiService(App.getInstance()).postGetAppVersion(path, requestBody)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<AppVersionResponse>() {
                    @Override
                    public void accept(final AppVersionResponse response) throws Exception {
                        if (response.isSuccessful()) {
                            String curVersion = AppUtils.getAppVersionName();
                            if(curVersion.compareTo(response.getVersion().getMin())<0){//强制升级
                                DialogUtils.getInstance().showUpdateDialog(context,false)
                                    .setTitle("版本更新")
                                    .setMsg("检查到新的版本 v"+response.getVersion().getLast()+",请及时更新")
                                    .hideLeftBtn()
                                    .setRightBtnText("立即更新")
                                    .setOnMsgTipOptions(new OnMsgTipOptions(){
                                        @Override
                                        public void onSure() {
                                            super.onSure();
                                            Common.download(context,response.getVersion().getLasturl());
                                        }
                                    }).show();
                                return;
                            }
                            if(curVersion.compareTo(response.getVersion().getLast())<0&&isAuto){//不强制升级
                                final UpdateDialog updateDialog = DialogUtils.getInstance().showUpdateDialog(context,true);
                                updateDialog.setTitle("版本更新");
                                updateDialog.setMsg("检查到新的版本 v"+response.getVersion().getLast()+",请及时更新");
                                updateDialog.setLeftBtnText("取消");
                                updateDialog.setRightBtnText("更新");
                                updateDialog.setOnMsgTipOptions(new OnMsgTipOptions(){
                                            @Override
                                            public void onSure() {
                                                super.onSure();
                                                Common.download(context,response.getVersion().getLasturl());
                                                if(updateDialog!=null&&updateDialog.isShowing()){
                                                    updateDialog.dismiss();
                                                }
                                            }

                                            @Override
                                            public void onCancel() {
                                                super.onCancel();
                                                if(updateDialog!=null&&updateDialog.isShowing()){
                                                    updateDialog.dismiss();
                                                }
                                            }
                                        });
                                updateDialog.show();
                                return;
                            }
                            if(curVersion.compareTo(response.getVersion().getLast())>=0&&isAuto) {//不强制升级
                                ToastUtils.showShort("当前已是最新版本");
                            }
                        } else {
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

    private static void download(Context context,String lasturl) {
        DownloadManagerUtil downloadManagerUtil = new DownloadManagerUtil(context);
        downloadManagerUtil.download(lasturl, AppUtils.getAppName(), AppUtils.getAppPackageName());
    }

    public synchronized static void AddFriendConfirm(final Context context, String friendId, String method) {
        if(!NetworkUtils.isConnected()){
            ToastUtils.showShort(R.string.msg_net_error);
            return;
        }
        Map param = new HashMap();
        param.put("method", method);
        param.put("friended", friendId);
        String json = new Gson().toJson(param);
        PubParam pubParam = new PubParam(Common.getUserId());
        String sign_unSha1 = pubParam.toValueString() + json + Common.getTocken();
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
                        if(!response.isSuccessful()){
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
    public synchronized static void initDeviceList(final Context context, final OnNetRequestCallBack onNetRequestCallBack) {
        if(!NetworkUtils.isConnected()){
            if(onNetRequestCallBack!=null){
                onNetRequestCallBack.OnRequestComplete();
            }
            ToastUtils.showShort("网络链接断开，请检查网络");
            return;
        }
        Map param = new LinkedHashMap();
        param.put("qrytype", Params.QRY_TYPE.ALL);
        String json = new Gson().toJson(param);
        PubParam pubParam = new PubParam(Common.getUserId());
        String sign_unSha1 = pubParam.toValueString() + json + Common.getTocken();
        LogUtils.d("sign_unsha1", sign_unSha1);
        String sign = EncryptUtils.encryptSHA1ToString(sign_unSha1).toLowerCase();
        LogUtils.d("sign_sha1", sign);
        String path = HttpUrl.Api + "userdevicelist/" + pubParam.toUrlParam(sign);
        final RequestBody requestBody = RequestBody.create(MediaType.parse(HttpUrl.MediaType_Json), json);

        ApiServiceManager.getInstance().buildApiService(context).postDeviceOption(path, requestBody)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<DeviceListResponse>() {
                    @Override
                    public void accept(DeviceListResponse response) throws Exception {
                        SPUtils.getInstance().remove(SPConst.DATA.DEVICELIST);
                        SPUtils.getInstance().saveObject(SPConst.DATA.DEVICELIST, response);
                        if(!response.isSuccessful()) {
                            response.onError(context);
                        }
                        if(onNetRequestCallBack!=null){
                            onNetRequestCallBack.OnRequestComplete();
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        throwable.printStackTrace();
                        if(onNetRequestCallBack!=null){
                            onNetRequestCallBack.OnRequestComplete();
                        }
                    }
                });
    }
    public static synchronized void updateIngnoreSettingValid() {
        Map userattr = new HashMap();
        userattr.put("isAreaDisturb", SPUtils.getInstance().getInt(SPConst.DISTURB.ISAREADISTURB,1));
        userattr.put("isTimeDisturb", SPUtils.getInstance().getInt(SPConst.DISTURB.ISTIMEDISTURB,1));

        Map param = new HashMap();
        param.put("method", "EF");
        param.put("userattr", userattr);
        if(!NetworkUtils.isConnected()){
            return;
        }
        String json = new Gson().toJson(param);
        PubParam pubParam = new PubParam(Common.getUserId());
        String sign_unSha1 = pubParam.toValueString() + json + Common.getTocken();
        LogUtils.d("sign_unsha1", sign_unSha1);
        String sign = EncryptUtils.encryptSHA1ToString(sign_unSha1).toLowerCase();
        LogUtils.d("sign_sha1", sign);
        String path = HttpUrl.Api + "userattr/" + pubParam.toUrlParam(sign);
        RequestBody requestBody = RequestBody.create(MediaType.parse(HttpUrl.MediaType_Json), json);

        ApiServiceManager.getInstance().buildApiService(App.getInstance()).postNetRequest(path, requestBody)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<BaseResponse>() {
                    @Override
                    public void accept(BaseResponse response) throws Exception {
                        if (response.isSuccessful()) {

                        } else {
                            ToastUtils.showShort(response.getErrmsg());
                        }
                    }
                });
    }
    public static void ValidToken(final Context context) {
        if(!NetworkUtils.isConnected()){
            return;
        }
        String json = "";
        PubParam pubParam = new PubParam(Common.getUserId());
        String sign_unSha1 = pubParam.toValueString() + json + Common.getTocken();
        LogUtils.d("sign_unsha1", sign_unSha1);
        String sign = EncryptUtils.encryptSHA1ToString(sign_unSha1).toLowerCase();
        LogUtils.d("sign_sha1", sign);
        String path = HttpUrl.Api + "tokenstate/" + pubParam.toUrlParam(sign);
        final RequestBody requestBody = RequestBody.create(MediaType.parse(HttpUrl.MediaType_Json), json);
        ApiServiceManager.getInstance().buildApiService(context).postNetRequest(path, requestBody)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<BaseResponse>() {
                    @Override
                    public void accept(BaseResponse response) throws Exception {
                        if(response.isSuccessful()){
                            ToastUtils.showShort("系统异常，请稍后重试");
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
        DeviceListResponse response = (DeviceListResponse) SPUtils.getInstance().readObject(SPConst.DATA.DEVICELIST);
        if(response!=null){
            List<DeviceInfo> list = response.getDevlist();
            if(list==null||list.size()<=0){
                return false;
            }
            for (DeviceInfo devlistBean : list){
                if(address.equals(Common.formatDevId2Mac(devlistBean.getDevidX()))&&TextUtils.isEmpty(devlistBean.getFriendname())){
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isBell() {
        if(Common.isAreaIgnore()||Common.isTimeIgnore()){
            return false;
        }
        return true;
    }

    public static boolean isAreaIgnore(){
        boolean isPointIgnore = (SPUtils.getInstance().getInt(SPConst.DISTURB.ISAREADISTURB,1)==1);
        if(!isPointIgnore){
            return false;
        }
        UserInfoResponse response = (UserInfoResponse) SPUtils.getInstance().readObject(SPConst.DATA.USERINFO);
        if(response!=null&&response.getConfig()!=null){
            UserInfoResponse.ConfigBean configBean = response.getConfig();
            if(configBean.getDonotdisturb()!=null){
                UserInfoResponse.ConfigBean.DonotdisturbBean donotdisturbBean = configBean.getDonotdisturb();
                if(donotdisturbBean.getArealist()!=null&&donotdisturbBean.getArealist().size()>0){
                    for (UserInfoResponse.ConfigBean.DonotdisturbBean.ArealistBean arealistBean : donotdisturbBean.getArealist()){
                        if(!TextUtils.isEmpty(arealistBean.getSsid())&&Common.decodeBase64(arealistBean.getSsid()).equals(NetworkUtils.getSsidOfConnectWifi())){
                            return true;
                        }
                        UserInfoResponse.ConfigBean.DonotdisturbBean.ArealistBean.AreaBean areaBean = arealistBean.getArea();
                        if(areaBean!=null){
                            Locinfo locinfo = (Locinfo) SPUtils.getInstance().readObject(SPConst.LOC_INFO);
                            LatLng latLng1 = new LatLng(areaBean.getLat(),areaBean.getLng());
                            LatLng latLng2 = new LatLng(locinfo.getLat(),locinfo.getLng());
                            double distance = DistanceUtil.getDistance(latLng1, latLng2);
                            if(distance<areaBean.getR()){
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }
    public static boolean isTimeIgnore(){
        boolean isTimeIgnore = (SPUtils.getInstance().getInt(SPConst.DISTURB.ISTIMEDISTURB,1)==1);
        if(!isTimeIgnore){
            return false;
        }
        UserInfoResponse response = (UserInfoResponse) SPUtils.getInstance().readObject(SPConst.DATA.USERINFO);
        if(response!=null&&response.getConfig()!=null) {
            UserInfoResponse.ConfigBean configBean = response.getConfig();
            if (configBean.getDonotdisturb() != null) {
                UserInfoResponse.ConfigBean.DonotdisturbBean donotdisturbBean = configBean.getDonotdisturb();
                if(donotdisturbBean.getTimelist()!=null&&donotdisturbBean.getTimelist().size()>0){
                    for(UserInfoResponse.ConfigBean.DonotdisturbBean.TimelistBean timelistBean : donotdisturbBean.getTimelist()){
                        int index = TimeUtils.getWeekIndex(new Date());
                        index = (index+7-1)%7;
                        if(index==0){
                            index+=7;
                        }
                        String week = timelistBean.getDayofweek();
                        if(!TextUtils.isEmpty(week)&&week.contains(index+"")){
                            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd");
                            Date date = new Date();
                            String ymd = simpleDateFormat.format(date);

                            String sstime = Common.repairStrLen(timelistBean.getStime());
                            String eetime = Common.repairStrLen(timelistBean.getEtime());

                            SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");
                            long stime = TimeUtils.string2Millis(ymd+sstime,format);
                            long etime = TimeUtils.string2Millis(ymd+eetime,format);
                            long curTime = TimeUtils.getNowMills();
                            if(etime<stime){
                                if(curTime>stime||curTime<etime){
                                    return true;
                                }
                            }else{
                                if(curTime>stime&&curTime<etime){
                                    return true;
                                }
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    public static int getDefaultDevImgResouceId(String classfy){
        if("钥匙".equals(classfy)) {
            return R.drawable.ic_yaoshi;
        }
        if("钱包".equals(classfy)) {
            return R.drawable.ic_qianbao;
        }
        if("手提包".equals(classfy)) {
            return R.drawable.ic_shoutibao;
        }
        if("电脑".equals(classfy)) {
            return R.drawable.ic_diannao;
        }
        if("自行车".equals(classfy)) {
            return R.drawable.ic_zixingche;
        }
        if("汽车".equals(classfy)) {
            return R.drawable.ic_qiche;
        }
        if("相机".equals(classfy)) {
            return R.drawable.ic_xiangji;
        }
        if("雨伞".equals(classfy)) {
            return R.drawable.ic_yusan;
        }
        if("衣服".equals(classfy)) {
            return R.drawable.ic_yifu;
        }
        if("身份证".equals(classfy)) {
            return R.drawable.ic_shenfenzheng;
        }
        if("护照".equals(classfy)) {
            return R.drawable.ic_huzhao;
        }
        if("行李箱".equals(classfy)) {
            return R.drawable.ic_xinglixiang;
        }
        if("背包".equals(classfy)) {
            return R.drawable.ic_shubao;
        }
        if("手提箱".equals(classfy)) {
            return R.drawable.ic_shoutixiang;
        }
        if("其他".equals(classfy)) {
            return R.drawable.ic_qita;
        }
        return R.drawable.ic_qita;
    }


    public static String getTocken(){
        return SPUtils.getInstance(SPConst.USER.SP_NAME).getString(SPConst.USER.KEY_TOCKEN);
    }
    public static String getUserId(){
        return SPUtils.getInstance(SPConst.USER.SP_NAME).getString(SPConst.USER.KEY_USERID);
    }
    public static String getNikeName(){
        return SPUtils.getInstance(SPConst.USER.SP_NAME).getString(SPConst.USER.KEY_NIKENAME);
    }
    public static String getUserHeadImage(){
        return SPUtils.getInstance(SPConst.USER.SP_NAME).getString(SPConst.USER.KEY_IMAGE);
    }

    public static String formatWeekRepeat(String dayofweek) {
        if(dayofweek.contains("1234567")){
            return "每天";
        }
        StringBuilder value = new StringBuilder();
        if(dayofweek.contains("1")){
            value.append(",星期一");
        }
        if(dayofweek.contains("2")){
            value.append(",星期二");
        }
        if(dayofweek.contains("3")){
            value.append(",星期三");
        }
        if(dayofweek.contains("4")){
            value.append(",星期四");
        }
        if(dayofweek.contains("5")){
            value.append(",星期五");
        }
        if(dayofweek.contains("6")){
            value.append(",星期六");
        }
        if(dayofweek.contains("7")){
            value.append(",星期日");
        }
        String keyStr = value.toString();
        if(TextUtils.isEmpty(keyStr)){
            return "";
        }else{
            return keyStr.substring(1);
        }
    }

    public static String repairStrLen(String str) {
        if(TextUtils.isEmpty(str)){
            return "";
        }
        int ne = 6-str.length();
        for(int i=0;i<ne;i++){
            str = "0"+str;
        }
        return str;
    }

    public static boolean isLocationEnabled() {
        int locationMode = 0;
        String locationProviders;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            try {
                locationMode = Settings.Secure.getInt(App.getInstance().getContentResolver(), Settings.Secure.LOCATION_MODE);
            } catch (Settings.SettingNotFoundException e) {
                e.printStackTrace();
                return false;
            }
            return locationMode != Settings.Secure.LOCATION_MODE_OFF;
        } else {
            locationProviders = Settings.Secure.getString(App.getInstance().getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
            return !TextUtils.isEmpty(locationProviders);
        }
    }
    public static boolean isNotificationEnabled(){
        NotificationManagerCompat manager = NotificationManagerCompat.from(App.getInstance().getApplicationContext());
        return manager.areNotificationsEnabled();
    }
}
