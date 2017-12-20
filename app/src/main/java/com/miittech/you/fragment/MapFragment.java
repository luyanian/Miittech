package com.miittech.you.fragment;

import android.Manifest;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.InfoWindow;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.model.LatLng;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.luck.picture.lib.permissions.RxPermissions;
import com.miittech.you.App;
import com.miittech.you.R;
import com.miittech.you.activity.device.DeviceDetailActivity;
import com.miittech.you.activity.event.FriendTraceDetailActivity;
import com.miittech.you.common.Common;
import com.miittech.you.dialog.DialogUtils;
import com.miittech.you.dialog.MapDeviceUsersListDialog;
import com.miittech.you.entity.Locinfo;
import com.miittech.you.glide.GlideApp;
import com.miittech.you.global.HttpUrl;
import com.miittech.you.global.IntentExtras;
import com.miittech.you.global.Params;
import com.miittech.you.global.PubParam;
import com.miittech.you.global.SPConst;
import com.miittech.you.impl.OnListItemClick;
import com.miittech.you.location.LocationClient;
import com.miittech.you.net.ApiServiceManager;
import com.miittech.you.net.response.DeviceResponse;
import com.miittech.you.net.response.FriendLocInfoResponse;
import com.miittech.you.net.response.FriendsResponse;
import com.ryon.mutils.EncryptUtils;
import com.ryon.mutils.LogUtils;
import com.ryon.mutils.SPUtils;
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
    @BindView(R.id.img_users)
    ImageView imgUsers;
    private BaiduMap mBaiduMap;
    private Object currentObject;
//    private float radius = 200f;

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
            if (mMapView != null) {
                mMapView.onResume();
            }
        } else {
            // 相当于onpause()方法
            if (mMapView != null) {
                mMapView.onPause();
            }
        }
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        if (hidden) {
            //相当于Fragment的onPause
            if (mMapView != null) {
                mMapView.onPause();
            }
        } else {
            // 相当于Fragment的onResume
            if (mMapView != null) {
                mMapView.onResume();
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mMapView != null) {
            mMapView.onDestroy();
        }
        unbinder.unbind();
    }

    @OnClick(R.id.img_device)
    public void onClickDevice() {
        imgUsers.setImageResource(R.drawable.ic_users);
        imgDevice.setImageResource(R.drawable.ic_devece_hr);
        getDeviceList();
    }

    @OnClick(R.id.img_users)
    public void onClickFriends() {
        imgDevice.setImageResource(R.drawable.ic_devece);
        imgUsers.setImageResource(R.drawable.ic_users_hr);
        getFrentList();
    }

    private void getFrentList() {
        Map param = new HashMap();
        param.put("state", 1);
        String json = new Gson().toJson(param);
        PubParam pubParam = new PubParam(Common.getUserId());
        String sign_unSha1 = pubParam.toValueString() + json + Common.getTocken();
        LogUtils.d("sign_unsha1", sign_unSha1);
        String sign = EncryptUtils.encryptSHA1ToString(sign_unSha1).toLowerCase();
        LogUtils.d("sign_sha1", sign);
        String path = HttpUrl.Api + "friendslist/" + pubParam.toUrlParam(sign);
        final RequestBody requestBody = RequestBody.create(MediaType.parse(HttpUrl.MediaType_Json), json);

        ApiServiceManager.getInstance().buildApiService(getActivity()).postToGetFriendList(path,requestBody)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<FriendsResponse>() {
                    @Override
                    public void accept(FriendsResponse response) throws Exception {
                        if (response.isSuccessful()) {
                            if (response.getFriendlist() != null) {
                                final MapDeviceUsersListDialog mapDialog = DialogUtils.getInstance().createDevicesUsersDialog(getActivity());
                                mapDialog.initData("friend", response.getFriendlist(), new OnListItemClick() {
                                    @Override
                                    public void onItemClick(Object o) {
                                        super.onItemClick(o);
                                        FriendsResponse.FriendlistBean friend = (FriendsResponse.FriendlistBean) o;
                                        if (!Common.getUserId().equals(friend.getFriendid())) {
                                            getFriendLocation(friend);
                                        } else {
                                            initMapView(friend);
                                        }
                                        if (mapDialog != null && mapDialog.isShowing()) {
                                            mapDialog.dismiss();
                                        }
                                    }
                                });
                                mapDialog.show();
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


    public void initMapView() {

        mBaiduMap = mMapView.getMap();
        //普通地图
        mBaiduMap.setMapType(BaiduMap.MAP_TYPE_NORMAL);
        // 开启定位图层
        mBaiduMap.setMyLocationEnabled(true);
        MapView.setMapCustomEnable(true);
        mBaiduMap.setOnMyLocationClickListener(this);
        final Locinfo locinfo = (Locinfo) SPUtils.getInstance().readObject(SPConst.LOC_INFO);
        if (locinfo != null) {
//            MyLocationData locData = new MyLocationData.Builder()
//                    .accuracy(30)
//                    .direction(0).latitude(locinfo.getLat())
//                    .longitude(locinfo.getLng()).build();
//
//            // 设置定位数据
//            mBaiduMap.setMyLocationData(locData);
            LatLng ll = new LatLng(locinfo.getLat(), locinfo.getLng());
            MapStatus.Builder builder = new MapStatus.Builder();
            builder.target(ll).zoom(18.0f);
            mBaiduMap.animateMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()));
            return;
        }
    }

    private void initMapView(final FriendsResponse.FriendlistBean friend, List<FriendLocInfoResponse.FriendlistBean> friendInfos) {
        boolean isContain = false;
        if (friendInfos != null && friendInfos.size() > 0) {
            for (final FriendLocInfoResponse.FriendlistBean friendInfo : friendInfos) {
                if (friend.getFriendid().equals(friendInfo.getFriendid())) {
                    isContain = true;
                    currentObject = friendInfo;
                    GlideApp.with(this)
                            .asBitmap()
                            .centerCrop()
                            .override(100, 100)
                            .load(friend.getHeadimg())
                            .transform(new CircleCrop())
                            .into(new SimpleTarget<Bitmap>() {
                                @Override
                                public void onResourceReady(Bitmap resource, Transition<? super Bitmap> transition) {
                                    final BitmapDescriptor mCurrentMarker = BitmapDescriptorFactory.fromBitmap(resource);
                                    final LatLng llCircle = new LatLng(friendInfo.getLocinfo().getLat(), friendInfo.getLocinfo().getLng());
                                    MarkerOptions ooB = new MarkerOptions().position(llCircle).icon(mCurrentMarker).zIndex(5);
                                    Marker mMarkerB = (Marker) (mBaiduMap.addOverlay(ooB));
                                    MapStatusUpdate u = MapStatusUpdateFactory.newLatLng(llCircle);
                                    mBaiduMap.setMapStatus(u);

                                    mBaiduMap.setOnMarkerClickListener(new BaiduMap.OnMarkerClickListener() {
                                        @Override
                                        public boolean onMarkerClick(Marker marker) {
                                            Button button = new Button(App.getInstance().getApplicationContext());
                                            button.setBackgroundResource(R.drawable.popup);
                                            button.setText(Common.decodeBase64(friend.getNickname()));
                                            button.setTextColor(Color.BLACK);
                                            button.setWidth(300);
                                            InfoWindow.OnInfoWindowClickListener listener = new InfoWindow.OnInfoWindowClickListener() {
                                                public void onInfoWindowClick() {
                                                    if (!Common.getUserId().equals(friendInfo.getFriendid())) {
                                                        Intent intent = new Intent(getActivity(), FriendTraceDetailActivity.class);
                                                        intent.putExtra(IntentExtras.DEVICE.DATA, friend);
                                                        startActivity(intent);
                                                        mBaiduMap.hideInfoWindow();
                                                    }
                                                }
                                            };
                                            InfoWindow mInfoWindow = new InfoWindow(BitmapDescriptorFactory.fromView(button), llCircle, -47, listener);
                                            mBaiduMap.showInfoWindow(mInfoWindow);
                                            return false;
                                        }
                                    });
                                }
                            });


                }
            }
        }
        if (!isContain) {
            ToastUtils.showShort("该好友还没有位置信息！");
        }
    }

    private void initMapView(final FriendsResponse.FriendlistBean friend) {
        final Locinfo locinfo = (Locinfo) SPUtils.getInstance().readObject(SPConst.LOC_INFO);
        if (locinfo == null) {
            return;
        }
        GlideApp.with(this)
                .asBitmap()
                .centerCrop()
                .override(100, 100)
                .load(friend.getHeadimg())
                .transform(new CircleCrop())
                .into(new SimpleTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(Bitmap resource, Transition<? super Bitmap> transition) {
                        final BitmapDescriptor mCurrentMarker = BitmapDescriptorFactory.fromBitmap(resource);
                        final LatLng llCircle = new LatLng(locinfo.getLat(), locinfo.getLng());
                        MarkerOptions ooB = new MarkerOptions().position(llCircle).icon(mCurrentMarker).zIndex(5);
                        Marker mMarkerB = (Marker) (mBaiduMap.addOverlay(ooB));
                        MapStatusUpdate u = MapStatusUpdateFactory.newLatLng(llCircle);
                        mBaiduMap.setMapStatus(u);

                        mBaiduMap.setOnMarkerClickListener(new BaiduMap.OnMarkerClickListener() {
                            @Override
                            public boolean onMarkerClick(Marker marker) {
                                Button button = new Button(App.getInstance().getApplicationContext());
                                button.setBackgroundResource(R.drawable.popup);
                                button.setText(Common.decodeBase64(friend.getNickname()));
                                button.setTextColor(Color.BLACK);
                                button.setWidth(300);
                                InfoWindow.OnInfoWindowClickListener listener = new InfoWindow.OnInfoWindowClickListener() {
                                    public void onInfoWindowClick() {

                                    }
                                };
                                InfoWindow mInfoWindow = new InfoWindow(BitmapDescriptorFactory.fromView(button), llCircle, -47, listener);
                                mBaiduMap.showInfoWindow(mInfoWindow);
                                return false;
                            }
                        });
                    }
                });
    }

    private void initMapView(final DeviceResponse.DevlistBean device) {
        final DeviceResponse.DevlistBean.LocinfoBean locInfo = device.getLocinfo();
        currentObject = device;
        if (locInfo == null) {
            return;
        }
        GlideApp.with(this)
                .asBitmap()
                .centerCrop()
                .override(100, 100)
                .load(device.getDevimg())
                .transform(new CircleCrop())
                .into(new SimpleTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(Bitmap resource, Transition<? super Bitmap> transition) {
//                    MyLocationData locData = new MyLocationData.Builder()
//                            .accuracy(radius)
//                            .direction(0)
//                            .latitude(locInfo.getLat())
//                            .longitude(locInfo.getLng()).build();
//                    mBaiduMap.setMyLocationData(locData);

                        final BitmapDescriptor mCurrentMarker = BitmapDescriptorFactory.fromBitmap(resource);
                        final LatLng llCircle = new LatLng(locInfo.getLat(), locInfo.getLng());
                        MarkerOptions ooB = new MarkerOptions().position(llCircle).icon(mCurrentMarker).zIndex(5);
                        Marker mMarkerB = (Marker) (mBaiduMap.addOverlay(ooB));
                        MapStatusUpdate u = MapStatusUpdateFactory.newLatLng(llCircle);
                        mBaiduMap.setMapStatus(u);

                        mBaiduMap.setOnMarkerClickListener(new BaiduMap.OnMarkerClickListener() {
                            @Override
                            public boolean onMarkerClick(Marker marker) {
                                Button button = new Button(App.getInstance().getApplicationContext());
                                button.setBackgroundResource(R.drawable.popup);
                                button.setText(Common.decodeBase64(device.getDevname()));
                                button.setTextColor(Color.BLACK);
                                button.setWidth(300);
                                InfoWindow.OnInfoWindowClickListener listener = new InfoWindow.OnInfoWindowClickListener() {
                                    public void onInfoWindowClick() {
                                        Intent intent = new Intent(getActivity(), DeviceDetailActivity.class);
                                        intent.putExtra(IntentExtras.DEVICE.DATA, device);
                                        startActivity(intent);
                                        mBaiduMap.hideInfoWindow();
                                    }
                                };
                                InfoWindow mInfoWindow = new InfoWindow(BitmapDescriptorFactory.fromView(button), llCircle, -47, listener);
                                mBaiduMap.showInfoWindow(mInfoWindow);
                                return false;
                            }
                        });
                    }
                });

    }

    private void getFriendLocation(final FriendsResponse.FriendlistBean friend) {
        List<Map> friendlist = new ArrayList<>();
        Map item = new HashMap();
        item.put("friendid", friend.getFriendid());
        friendlist.add(item);
        Map param = new HashMap();
        param.put("qrytype", Params.QRY_TYPE.BASE);
        param.put("friendlist", friendlist);
        String json = new Gson().toJson(param);
        PubParam pubParam = new PubParam(Common.getUserId());
        String sign_unSha1 = pubParam.toValueString() + json + Common.getTocken();
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
        param.put("qrytype", Params.QRY_TYPE.ALL);
        String json = new Gson().toJson(param);
        PubParam pubParam = new PubParam(Common.getUserId());
        String sign_unSha1 = pubParam.toValueString() + json + Common.getTocken();
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
                        if (response.isSuccessful()) {
                            if (response.getDevlist() != null && response.getDevlist().size() > 0) {
                                final MapDeviceUsersListDialog mapDialog = DialogUtils.getInstance().createDevicesUsersDialog(getActivity());
                                mapDialog.initData("device", response.getDevlist(), new OnListItemClick() {
                                    @Override
                                    public void onItemClick(Object o) {
                                        super.onItemClick(o);
                                        DeviceResponse.DevlistBean device = (DeviceResponse.DevlistBean) o;
                                        initMapView(device);
                                        if (mapDialog != null && mapDialog.isShowing()) {
                                            mapDialog.dismiss();
                                        }
                                    }
                                });
                                mapDialog.show();
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

    @Override
    public boolean onMyLocationClick() {
        if (currentObject == null) {
            return false;
        }
        Intent intent;
        if (currentObject instanceof FriendsResponse.FriendlistBean) {

        }
        if (currentObject instanceof DeviceResponse.DevlistBean) {
            intent = new Intent(getActivity(), DeviceDetailActivity.class);
            intent.putExtra(IntentExtras.DEVICE.DATA, (DeviceResponse.DevlistBean) currentObject);
            startActivity(intent);
        }
        return false;
    }
}
