package com.miittech.you.service;
import android.app.Service;  
import android.content.Intent;  
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;  
import android.widget.Toast;

import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.utils.DistanceUtil;
import com.google.gson.Gson;
import com.inuker.bluetooth.library.Constants;
import com.inuker.bluetooth.library.connect.response.BleReadResponse;
import com.inuker.bluetooth.library.connect.response.BleReadRssiResponse;
import com.miittech.you.App;
import com.miittech.you.ble.ClientManager;
import com.miittech.you.common.BleCommon;
import com.miittech.you.common.Common;
import com.miittech.you.dialog.DialogUtils;
import com.miittech.you.dialog.MapDeviceUsersListDialog;
import com.miittech.you.global.HttpUrl;
import com.miittech.you.global.Params;
import com.miittech.you.global.PubParam;
import com.miittech.you.impl.OnListItemClick;
import com.miittech.you.net.ApiServiceManager;
import com.miittech.you.net.response.FriendsResponse;
import com.ryon.constant.TimeConstants;
import com.ryon.mutils.ConvertUtils;
import com.ryon.mutils.EncryptUtils;
import com.ryon.mutils.LogUtils;
import com.ryon.mutils.TimeUtils;
import com.ryon.mutils.ToastUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import okhttp3.MediaType;
import okhttp3.RequestBody;

public class ReportService extends Service {
    public LocationClient mLocationClient = null;
    private MyLocationListener myListener = new MyLocationListener();
    private BDLocation lastLocation;
    private long lastMillins=0;
    @Override  
    public IBinder onBind(Intent intent) {  
        return null;  
    }  
  
    @Override  
    public void onCreate() {  
        Toast.makeText(this, "ReportService Service created", Toast.LENGTH_LONG).show();
        mLocationClient = new LocationClient(getApplicationContext());
        mLocationClient.registerLocationListener(myListener);
        LocationClientOption option = new LocationClientOption();
        option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);
        option.setCoorType("bd09ll");
        option.setScanSpan(60000);
        option.setOpenGps(true);
        option.setIgnoreKillProcess(false);
        option.setWifiCacheTimeOut(5*60*1000);
        mLocationClient.setLocOption(option);
        mLocationClient.start();
    }  
  
    @Override  
    public void onDestroy() {  
        Toast.makeText(this, "ReportService Service Stoped", Toast.LENGTH_LONG).show();
        if(mLocationClient!=null){
            mLocationClient.stop();
        }
    }
    public class MyLocationListener extends BDAbstractLocationListener {
        @Override
        public void onReceiveLocation(BDLocation location){
            if(lastLocation!=null) {
                LatLng last = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
                LatLng current = new LatLng(location.getLatitude(), location.getLongitude());
                long curMillis = TimeUtils.getNowDate().getTime();
                if(DistanceUtil. getDistance(last, current)>200||TimeUtils.getTimeSpan(curMillis,lastMillins, TimeConstants.MIN)>30){
                    reportUserLocation(curMillis,location);
                }
            }
        }
    }

    private void reportUserLocation(final long millis, final BDLocation location) {
        Map user_loc = new HashMap();
        user_loc.put("lat",location.getLatitude());
        user_loc.put("lng",location.getLongitude());
        user_loc.put("addr",location.getAddrStr());
        List<String> macs = ClientManager.getInstance().getMacList();
        List<Map> devlist = new ArrayList<>();
        for(final String mac:macs){
            final Map devItem = new HashMap();
            devItem.put("devid", Common.formatMac2DevId(mac));
            ClientManager.getInstance().getClient().read(mac, BleCommon.batServiceUUID, BleCommon.batCharacteristicUUID, new BleReadResponse() {
                @Override
                public void onResponse(int code, byte[] data) {
                    if(code== Constants.REQUEST_SUCCESS){
                        devItem.put("devbattery", ConvertUtils.bytes2HexString(data));
                    }
                }
            });
            ClientManager.getInstance().getClient().readRssi(mac, new BleReadRssiResponse() {
                @Override
                public void onResponse(int code, Integer data) {
                    if(code== Constants.REQUEST_SUCCESS){
                        if(data<-85) {
                            devItem.put("devposstate",3);
                        }
                        if(data>-85&&data<-70){
                            devItem.put("devposstate",2);
                        }
                        if(data>-70){
                            devItem.put("devposstate",1);
                        }
                    }
                }
            });
            devItem.put("devstate", 1);
            devItem.put("usedstate", 1);
            devItem.put("bindstate", 1);
            devlist.add(devItem);
        }
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Map repdata = new HashMap();
        repdata.put("reptime",TimeUtils.millis2String(millis,new SimpleDateFormat("yyyymmddhhmmss")));
        repdata.put("user_loc",user_loc);
        repdata.put("devlist",devlist);
        Map param = new HashMap();
        param.put("method", 1);
        param.put("repdata", repdata);
        String json = new Gson().toJson(param);
        PubParam pubParam = new PubParam(App.getInstance().getUserId());
        String sign_unSha1 = pubParam.toValueString() + json + App.getInstance().getTocken();
        LogUtils.d("sign_unsha1", sign_unSha1);
        String sign = EncryptUtils.encryptSHA1ToString(sign_unSha1).toLowerCase();
        LogUtils.d("sign_sha1", sign);
        String path = HttpUrl.Api + "userreport/" + pubParam.toUrlParam(sign);
        final RequestBody requestBody = RequestBody.create(MediaType.parse(HttpUrl.MediaType_Json), json);

        ApiServiceManager.getInstance().buildApiService(App.getInstance().getApplicationContext()).postToGetFriendList(path,
                requestBody)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<FriendsResponse>() {
                    @Override
                    public void accept(FriendsResponse response) throws Exception {
                        if (response.isSuccessful()) {
                            lastLocation = location;
                            lastMillins = millis;
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        throwable.printStackTrace();
                    }
                });
    }
}  