package com.miittech.you.fragment;

import android.Manifest;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.CircleOptions;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.map.Stroke;
import com.baidu.mapapi.model.LatLng;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;
import com.google.gson.Gson;
import com.luck.picture.lib.permissions.RxPermissions;
import com.miittech.you.App;
import com.miittech.you.R;
import com.miittech.you.activity.device.DeviceDetailActivity;
import com.miittech.you.dialog.DialogUtils;
import com.miittech.you.dialog.MapDeviceUsersListDialog;
import com.miittech.you.glide.GlideApp;
import com.miittech.you.global.HttpUrl;
import com.miittech.you.global.IntentExtras;
import com.miittech.you.global.Params;
import com.miittech.you.global.PubParam;
import com.miittech.you.impl.OnListItemClick;
import com.miittech.you.location.LocationClient;
import com.miittech.you.net.ApiServiceManager;
import com.miittech.you.net.response.DeviceResponse;
import com.miittech.you.net.response.FriendLocInfoResponse;
import com.miittech.you.net.response.FriendsResponse;
import com.ryon.mutils.EncryptUtils;
import com.ryon.mutils.LogUtils;
import com.ryon.mutils.ToastUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import okhttp3.MediaType;
import okhttp3.RequestBody;

/**
 * Created by Administrator on 2017/9/14.
 */

public class MapFragment extends Fragment implements BaiduMap.OnMyLocationClickListener {
    Unbinder unbinder;
    @BindView(R.id.map_view)
    MapView mMapView;
    @BindView(R.id.img_device)
    ImageView imgDevice;
    private BaiduMap mBaiduMap;
    private Object currentObject;
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle
            savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_map, container, false);
        unbinder = ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initMapView();
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            // 相当于onResume()方法
            if(mMapView!=null) {
                mMapView.onResume();
            }
        } else {
            // 相当于onpause()方法
            if(mMapView!=null) {
                mMapView.onPause();
            }
        }
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        if (hidden) {
            //相当于Fragment的onPause
            if(mMapView!=null) {
                mMapView.onPause();
            }
        } else {
            // 相当于Fragment的onResume
            if(mMapView!=null) {
                mMapView.onResume();
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if(mMapView!=null) {
            mMapView.onDestroy();
        }
        unbinder.unbind();
    }

    @OnClick(R.id.img_device)
    public void onClickDevice(){
        getDeviceList();
    }
    @OnClick(R.id.img_users)
    public void onClickFriends(){
        getFrentList();
    }

    private BDAbstractLocationListener listener = new BDAbstractLocationListener() {
        @Override
        public void onReceiveLocation(BDLocation location){
            //此处的BDLocation为定位结果信息类，通过它的各种get方法可获取定位相关的全部结果
            //以下只列举部分获取经纬度相关（常用）的结果信息
            //更多结果信息获取说明，请参照类参考中BDLocation类中的说明
            // 构造定位数据
            MyLocationData locData = new MyLocationData.Builder()
                    .accuracy(location.getRadius())
                    // 此处设置开发者获取到的方向信息，顺时针0-360
                    .direction(0).latitude(location.getLatitude())
                    .longitude(location.getLongitude()).build();

            // 设置定位数据
            mBaiduMap.setMyLocationData(locData);

            LatLng ll = new LatLng(location.getLatitude(),location.getLongitude());
            MapStatus.Builder builder = new MapStatus.Builder();
            builder.target(ll).zoom(18.0f);
            mBaiduMap.animateMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()));
        }
    };

    private void getFrentList(){
        Map param = new HashMap();
        param.put("state", 1);
        String json = new Gson().toJson(param);
        PubParam pubParam = new PubParam(App.getInstance().getUserId());
        String sign_unSha1 = pubParam.toValueString() + json + App.getInstance().getTocken();
        LogUtils.d("sign_unsha1", sign_unSha1);
        String sign = EncryptUtils.encryptSHA1ToString(sign_unSha1).toLowerCase();
        LogUtils.d("sign_sha1", sign);
        String path = HttpUrl.Api + "friendslist/" + pubParam.toUrlParam(sign);
        final RequestBody requestBody = RequestBody.create(MediaType.parse(HttpUrl.MediaType_Json), json);

        ApiServiceManager.getInstance().buildApiService(getActivity()).postToGetFriendList(path,
                requestBody)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<FriendsResponse>() {
                    @Override
                    public void accept(FriendsResponse response) throws Exception {
                        if (response.isSuccessful()) {
                            if(response.getFriendlist()!=null&&response.getFriendlist().size()>0) {
                                final MapDeviceUsersListDialog mapDialog = DialogUtils.getInstance
                                        ().createDevicesUsersDialog(getActivity());
                                mapDialog.initData(response.getFriendlist(),new OnListItemClick(){
                                    @Override
                                    public void onItemClick(Object o) {
                                        super.onItemClick(o);
                                        FriendsResponse.FriendlistBean friend =
                                                (FriendsResponse.FriendlistBean) o;
                                        getFriendLocation(friend);
                                        if(mapDialog!=null&&mapDialog.isShowing()){
                                            mapDialog.dismiss();
                                        }
                                    }
                                });
                                mapDialog.show();
                            }else{
                                ToastUtils.showShort("您还没有添加好友！");
                            }
                        } else {
                            response.onError(getActivity());
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        throwable.printStackTrace();
                    }
                });
    }
    public void initMapView(){

        mBaiduMap = mMapView.getMap();
        //普通地图
        mBaiduMap.setMapType(BaiduMap.MAP_TYPE_NORMAL);
        // 开启定位图层
        mBaiduMap.setMyLocationEnabled(true);
        MapView.setMapCustomEnable(true);

        RxPermissions permissions = new RxPermissions(getActivity());
        permissions.request
                (Manifest.permission.READ_PHONE_STATE,Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACCESS_FINE_LOCATION
                        ,Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .subscribe(new Consumer<Boolean>() {
                    @Override
                    public void accept(Boolean aBoolean) throws Exception {
                        if(aBoolean){
                            //注册监听函数
                            LocationClientOption option = new LocationClientOption();
                            option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);
                            option.setCoorType("bd09ll");
                            option.setScanSpan(0);//只定位一次
                            option.setOpenGps(true);
                            option.setLocationNotify(true);
                            option.setIgnoreKillProcess(false);
                            option.SetIgnoreCacheException(false);
                            option.setWifiCacheTimeOut(5*60*1000);
                            option.setEnableSimulateGps(false);
                            LocationClient.getInstance().initLocation().startLocation(option,listener);
                        }
                    }
                });
        mBaiduMap.setOnMyLocationClickListener(this);
    }
    private void initMapView(FriendsResponse.FriendlistBean friend, List<FriendLocInfoResponse.FriendInfo> friendInfos) {
        boolean isContain = false;
        if(friendInfos!=null&&friendInfos.size()>0){
            for (final FriendLocInfoResponse.FriendInfo friendInfo : friendInfos){
                if(friend.getFriendid().equals(friendInfo.getFriendid())){
                    isContain = true;
                    currentObject = friendInfo;
                    GlideApp.with(this).asBitmap().centerCrop().override(100,100)
                            .load(friend.getHeadimg()).transform(new CircleCrop()).into(new SimpleTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(Bitmap resource, Transition<? super Bitmap> transition) {
                            BitmapDescriptor mCurrentMarker = BitmapDescriptorFactory.fromBitmap(resource);
                            MyLocationConfiguration config = new MyLocationConfiguration(MyLocationConfiguration.LocationMode.NORMAL,true, mCurrentMarker,0xAAFFFF88,0xAA00FF00);
                            mBaiduMap.setMyLocationConfiguration(config);
                            MyLocationData locData = new MyLocationData.Builder()
                                    .latitude(friendInfo.getLat())
                                    .longitude(friendInfo.getLng()).build();
                            mBaiduMap.setMyLocationData(locData);
                        }
                    });

                    LatLng llCircle = new LatLng(friendInfo.getLat(),friendInfo.getLng());
                    MapStatus.Builder builder = new MapStatus.Builder();
                    builder.target(llCircle).zoom(18.0f);
                    mBaiduMap.animateMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()));
                }
            }
        }
        if(!isContain){
            ToastUtils.showShort("该好友还没有位置信息！");
        }
    }

    private void initMapView(DeviceResponse.DevlistBean device){
        final DeviceResponse.DevlistBean.LocinfoBean locInfo = device.getLocinfo();
        currentObject = device;
        if(locInfo==null){
            return;
        }
        GlideApp.with(this).asBitmap().centerCrop().override(100,100)
                .load(device.getDevimg()).transform(new CircleCrop()).into(new SimpleTarget<Bitmap>() {
            @Override
            public void onResourceReady(Bitmap resource, Transition<? super Bitmap> transition) {
                BitmapDescriptor mCurrentMarker = BitmapDescriptorFactory.fromBitmap(resource);
                MyLocationConfiguration config = new MyLocationConfiguration(MyLocationConfiguration.LocationMode.NORMAL,true, mCurrentMarker,0xAAFFFF88,0xAA00FF00);
                mBaiduMap.setMyLocationConfiguration(config);
                MyLocationData locData = new MyLocationData.Builder()
                        .latitude(locInfo.getLat())
                        .longitude(locInfo.getLng()).build();
                // 设置定位数据
                mBaiduMap.setMyLocationData(locData);
            }
        });

        LatLng llCircle = new LatLng(locInfo.getLat(),locInfo.getLng());
        MapStatus.Builder builder = new MapStatus.Builder();
        builder.target(llCircle).zoom(18.0f);
        mBaiduMap.animateMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()));
    }

    private void getFriendLocation(final FriendsResponse.FriendlistBean friend) {
        List<Map> friendlist = new ArrayList<>();
        Map item = new HashMap();
        item.put("friendid",friend.getFriendid());
        friendlist.add(item);
        Map param = new HashMap();
        param.put("qrytype", Params.QRY_TYPE.BASE);
        param.put("friendlist", friendlist);
        String json = new Gson().toJson(param);
        PubParam pubParam = new PubParam(App.getInstance().getUserId());
        String sign_unSha1 = pubParam.toValueString() + json + App.getInstance().getTocken();
        LogUtils.d("sign_unsha1", sign_unSha1);
        String sign = EncryptUtils.encryptSHA1ToString(sign_unSha1).toLowerCase();
        LogUtils.d("sign_sha1", sign);
        String path = HttpUrl.Api + "friendslocation/" + pubParam.toUrlParam(sign);
        final RequestBody requestBody = RequestBody.create(MediaType.parse(HttpUrl.MediaType_Json), json);


        ApiServiceManager.getInstance().buildApiService(getActivity()).postToGetFriendLocList(path,
                requestBody)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<FriendLocInfoResponse>() {
                    @Override
                    public void accept(FriendLocInfoResponse response) throws Exception {
                        if (response.isSuccessful()) {
                            if(response.getFriendlist()!=null&&response.getFriendlist().size()>0) {
                                initMapView(friend,response.getFriendlist());
                            }else{
                                response.onError(getActivity());
                            }
                        } else {
                            response.onError(getActivity());
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        throwable.printStackTrace();
                    }
                });
    }

    private void getDeviceList() {
        Map param = new LinkedHashMap();
        param.put("qrytype", Params.QRY_TYPE.USED);
        String json = new Gson().toJson(param);
        PubParam pubParam = new PubParam(App.getInstance().getUserId());
        String sign_unSha1 = pubParam.toValueString() + json + App.getInstance().getTocken();
        LogUtils.d("sign_unsha1", sign_unSha1);
        String sign = EncryptUtils.encryptSHA1ToString(sign_unSha1).toLowerCase();
        LogUtils.d("sign_sha1", sign);
        String path = HttpUrl.Api + "userdevicelist/" + pubParam.toUrlParam(sign);
        final RequestBody requestBody = RequestBody.create(MediaType.parse(HttpUrl.MediaType_Json), json);

        ApiServiceManager.getInstance().buildApiService(getActivity()).postDeviceOption(path, requestBody)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<DeviceResponse>() {
                    @Override
                    public void accept(DeviceResponse response) throws Exception {
                        if(response.isSuccessful()){
                            if(response.getDevlist()!=null&&response.getDevlist().size()>0) {
                                final MapDeviceUsersListDialog mapDialog = DialogUtils.getInstance().createDevicesUsersDialog(getActivity());
                                mapDialog.initData(response.getDevlist(),new OnListItemClick(){
                                    @Override
                                    public void onItemClick(Object o) {
                                        super.onItemClick(o);
                                        DeviceResponse.DevlistBean device = (DeviceResponse.DevlistBean) o;
                                        initMapView(device);
                                        if(mapDialog!=null&&mapDialog.isShowing()){
                                            mapDialog.dismiss();
                                        }
                                    }
                                });
                                mapDialog.show();
                            }
                        }else{
                            response.onError(getActivity());
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        throwable.printStackTrace();
                    }
                });
    }

    @Override
    public boolean onMyLocationClick() {
        if(currentObject==null){
            return false;
        }
        Intent intent;
        if(currentObject instanceof FriendsResponse.FriendlistBean){

        }
        if(currentObject instanceof DeviceResponse.DevlistBean){
            intent = new Intent(getActivity(), DeviceDetailActivity.class);
            intent.putExtra(IntentExtras.DEVICE.DATA,(DeviceResponse.DevlistBean)currentObject);
            startActivity(intent);
        }
        return false;
    }
}
