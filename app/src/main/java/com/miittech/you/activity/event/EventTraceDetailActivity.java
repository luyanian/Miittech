package com.miittech.you.activity.event;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.model.LatLngBounds;
import com.bumptech.glide.Glide;
import com.clj.fastble.BleManager;
import com.google.gson.Gson;
import com.miittech.you.App;
import com.miittech.you.R;
import com.miittech.you.activity.BaseActivity;
import com.miittech.you.activity.setting.SettingActivity;
import com.miittech.you.adapter.TraceDalySelectAdapter;
import com.miittech.you.common.Common;
import com.miittech.you.entity.Locinfo;
import com.miittech.you.glide.GlideApp;
import com.miittech.you.global.HttpUrl;
import com.miittech.you.global.IntentExtras;
import com.miittech.you.global.Params;
import com.miittech.you.global.PubParam;
import com.miittech.you.global.SPConst;
import com.miittech.you.impl.OnListItemClick;
import com.miittech.you.impl.TitleBarOptions;
import com.miittech.you.net.ApiServiceManager;
import com.miittech.you.net.response.DeviceInfoResponse;
import com.miittech.you.net.response.DeviceResponse;
import com.miittech.you.weight.CircleImageView;
import com.miittech.you.weight.Titlebar;
import com.ryon.mutils.EncryptUtils;
import com.ryon.mutils.LogUtils;
import com.ryon.mutils.SPUtils;
import com.ryon.mutils.TimeUtils;
import com.ryon.mutils.ToastUtils;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import mapapi.clusterutil.clustering.ClusterItem;
import mapapi.clusterutil.clustering.ClusterManager;
import okhttp3.MediaType;
import okhttp3.RequestBody;

/**
 * Created by ryon on 2017/12/2.
 */

public class EventTraceDetailActivity extends BaseActivity implements BaiduMap.OnMapLoadedCallback {

    @BindView(R.id.titlebar)
    Titlebar titlebar;
    @BindView(R.id.recyclerview)
    RecyclerView recyclerview;
    @BindView(R.id.tv_is_shared)
    TextView tvIsShared;
    @BindView(R.id.item_icon)
    CircleImageView itemIcon;
    @BindView(R.id.rl_item_icon)
    RelativeLayout rlItemIcon;
    @BindView(R.id.item_title)
    TextView itemTitle;
    @BindView(R.id.item_location)
    TextView itemLocation;
    @BindView(R.id.item_time)
    TextView itemTime;
    @BindView(R.id.item_battery)
    TextView itemBattery;
    @BindView(R.id.item_shared)
    TextView itemShared;
    @BindView(R.id.rl_item)
    RelativeLayout rlItem;
    @BindView(R.id.map_view)
    MapView mapView;
    private BaiduMap mBaiduMap;
    private DeviceResponse.DevlistBean devlistBean;
    private TraceDalySelectAdapter traceDalySelectAdapter;
    private MapStatus ms;
    private CmdResponseReceiver cmdResponseReceiver = new CmdResponseReceiver();
    private List<LatLng> latLngs = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_trace_detail);
        ButterKnife.bind(this);
        devlistBean = (DeviceResponse.DevlistBean) getIntent().getSerializableExtra(IntentExtras.DEVICE.DATA);
        initMyTitleBar(titlebar);
        titlebar.showBackOption();
        titlebar.showSettingOption();

        titlebar.setTitleBarOptions(new TitleBarOptions() {
            @Override
            public void onBack() {
                super.onBack();
                finish();
            }

            @Override
            public void onSetting() {
                super.onSetting();
                Intent intent = new Intent(EventTraceDetailActivity.this, SettingActivity.class);
                startActivity(intent);
            }
        });
        mBaiduMap = mapView.getMap();
        //普通地图
        mBaiduMap.setMapType(BaiduMap.MAP_TYPE_NORMAL);
        mBaiduMap.setOnMapLoadedCallback(this);
        Locinfo locinfo = (Locinfo) SPUtils.getInstance().readObject(SPConst.LOC_INFO);
        if (locinfo != null) {
            ms = new MapStatus.Builder().target(new LatLng(locinfo.getLat(), locinfo.getLng())).zoom(16).build();
            mBaiduMap.animateMapStatus(MapStatusUpdateFactory.newMapStatus(ms));
        }
        //创建默认的线性LayoutManager
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(this);
        mLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        recyclerview.setLayoutManager(mLayoutManager);
        recyclerview.setHasFixedSize(true);
        recyclerview.setItemViewCacheSize(31);
        //创建并设置Adapter
        traceDalySelectAdapter = new TraceDalySelectAdapter(this, new OnListItemClick<Date>() {
            @Override
            public void onItemClick(Date date) {
                super.onItemClick(date);
                mBaiduMap.clear();
                getPoints(date);
            }
        });
        recyclerview.setAdapter(traceDalySelectAdapter);
        traceDalySelectAdapter.scrollToEnd(recyclerview);
        getPoints(new Date());
        initDeviceInfo();
        IntentFilter filter = new IntentFilter();
        filter.addAction(IntentExtras.ACTION.ACTION_CMD_RESPONSE);
        this.registerReceiver(cmdResponseReceiver, filter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.unregisterReceiver(cmdResponseReceiver);
    }

    private void initDeviceInfo() {
        itemTitle.setText(Common.decodeBase64(devlistBean.getDevname()));
        tvIsShared.setVisibility(View.GONE);
        if (TextUtils.isEmpty(devlistBean.getFriendname())) {
            itemShared.setVisibility(View.GONE);
        } else {
            itemShared.setVisibility(View.VISIBLE);
            itemShared.setText("分享自" + Common.decodeBase64(devlistBean.getFriendname()));
        }

        if (!BleManager.getInstance().isConnected(Common.formatDevId2Mac(devlistBean.getDevidX()))) {
            itemLocation.setText(Common.decodeBase64(devlistBean.getLocinfo().getAddr()));
            setTimeText(itemTime, devlistBean.getLasttime());
        }else{
            itemTime.setText("现在");
            if(getIntent().hasExtra("location")){
                itemLocation.setText(getIntent().getStringExtra("location"));
            }
        }
        GlideApp.with(this)
                .load(devlistBean.getDevimg())
                .error(Common.getDefaultDevImgResouceId(Common.decodeBase64(devlistBean.getGroupname())))
                .placeholder(Common.getDefaultDevImgResouceId(Common.decodeBase64(devlistBean.getGroupname())))
                .into(itemIcon);
    }

    private void getPoints(Date date) {
        String daly = TimeUtils.date2String(date, new SimpleDateFormat("yyyyMMdd"));
        Map param = new HashMap();
        param.put("devid", devlistBean.getDevidX());
        param.put("qrytype", Params.QRY_TYPE.TRACE);
        param.put("sdate", daly);
        param.put("edate", daly);
        String json = new Gson().toJson(param);
        PubParam pubParam = new PubParam(Common.getUserId());
        String sign_unSha1 = pubParam.toValueString() + json + Common.getTocken();
        LogUtils.d("sign_unsha1", sign_unSha1);
        String sign = EncryptUtils.encryptSHA1ToString(sign_unSha1).toLowerCase();
        LogUtils.d("sign_sha1", sign);
        String path = HttpUrl.Api + "deviceinfo/" + pubParam.toUrlParam(sign);
        final RequestBody requestBody = RequestBody.create(MediaType.parse(HttpUrl.MediaType_Json), json);

        ApiServiceManager.getInstance().buildApiService(this).postDeviceInfoOption(path, requestBody)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<DeviceInfoResponse>() {
                    @Override
                    public void accept(DeviceInfoResponse response) throws Exception {
                        if (response.isSuccessful()) {
                            initMapPoints(response.getTracelist());
                        } else {
                            response.onError(EventTraceDetailActivity.this);
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        throwable.printStackTrace();
                    }
                });
    }

    private void initMapPoints(List<DeviceInfoResponse.TracelistBean> tracelist) {
        if (tracelist == null || tracelist.size() <= 0) {
            ToastUtils.showShort("没有发现轨迹信息");
            return;
        }
        List<OverlayOptions> options = new ArrayList<OverlayOptions>();
        DeviceInfoResponse.TracelistBean.LocinfoBean locinfoBean = tracelist.get(0).getLocinfo();
        LatLng latLng = new LatLng(locinfoBean.getLat(), locinfoBean.getLng());
        ms = new MapStatus.Builder().target(latLng).zoom(16).build();
        mBaiduMap.animateMapStatus(MapStatusUpdateFactory.newMapStatus(ms));
        latLngs.clear();
        for (DeviceInfoResponse.TracelistBean tracelistBean : tracelist) {
            LatLng point1 = new LatLng(tracelistBean.getLocinfo().getLat(), tracelistBean.getLocinfo().getLng());
            latLngs.add(point1);
            BitmapDescriptor descriptor = BitmapDescriptorFactory.fromResource(R.drawable.ic_map_point);
            OverlayOptions option1 = new MarkerOptions()
                    .position(point1)
                    .icon(descriptor);
            options.add(option1);
        }
        mBaiduMap.addOverlays(options);

        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (LatLng p : latLngs) {
            builder = builder.include(p);
        }
        LatLngBounds latlngBounds = builder.build();
        MapStatusUpdate mapStatusUpdate = MapStatusUpdateFactory.newLatLngBounds(latlngBounds,mapView.getWidth(),mapView.getHeight());
        mBaiduMap.animateMapStatus(mapStatusUpdate);
    }

    private void setTimeText(TextView itemTime, String lasttime) {
        if (itemTime == null) {
            return;
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        String timeSpan = TimeUtils.getFriendlyTimeSpanByNow(lasttime, sdf);
        itemTime.setText(timeSpan);
    }

    @Override
    public void onMapLoaded() {
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (LatLng p : latLngs) {
            builder = builder.include(p);
        }
        LatLngBounds latlngBounds = builder.build();
        MapStatusUpdate mapStatusUpdate = MapStatusUpdateFactory.newLatLngBounds(latlngBounds,mapView.getWidth(),mapView.getHeight());
        mBaiduMap.animateMapStatus(mapStatusUpdate);
//        ms = new MapStatus.Builder().zoom(9).build();
//        mBaiduMap.animateMapStatus(MapStatusUpdateFactory.newMapStatus(ms));
    }

    private class CmdResponseReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(IntentExtras.ACTION.ACTION_CMD_RESPONSE)) {
                String address = intent.getStringExtra("address");
                int ret = intent.getIntExtra("ret", -1);//获取Extra信息
                switch (ret) {
                    case IntentExtras.RET.RET_BLE_MODE_WORK_SUCCESS:
                        if(address.equals(Common.formatDevId2Mac(devlistBean.getDevidX()))) {
                            LogUtils.d("RET_DEVICE_CONNECT_SUCCESS");
                            if (itemTime != null) {
                                itemTime.setText("现在");
                            }
                        }
                        break;
                    case IntentExtras.RET.RET_BLE_DISCONNECT:
                        if(address.equals(Common.formatDevId2Mac(devlistBean.getDevidX()))) {
                            LogUtils.d("RET_DEVICE_CONNECT_FAILED");
                            if (itemLocation != null) {
                                itemLocation.setText(Common.decodeBase64(devlistBean.getLocinfo().getAddr()));
                            }
                            if (itemTime != null) {
                                setTimeText(itemTime, devlistBean.getLasttime());
                            }
                        }
                        break;
                    case IntentExtras.RET.RET_BLE_READ_RSSI:
                        if(address.equals(Common.formatDevId2Mac(devlistBean.getDevidX()))) {
                            LogUtils.d("RET_DEVICE_READ_RSSI");
                            int rssi = intent.getIntExtra("rssi", 0);
                            if (address.equals(Common.formatDevId2Mac(devlistBean.getDevidX()))) {
                                updateItemRssi(rssi);
                            }
                        }
                        break;
                    case IntentExtras.RET.RET_BLE_READ_BATTERY:
                        if(address.equals(Common.formatDevId2Mac(devlistBean.getDevidX()))) {
                            LogUtils.d("RET_DEVICE_READ_BATTERY");
                            String battery = intent.getStringExtra("battery");
                            if (address.equals(Common.formatDevId2Mac(devlistBean.getDevidX()))) {
                                updateItemBattery(battery);
                            }
                        }
                        break;
                }

            }
        }
    }

    private void updateItemRssi(int rssi) {
        if (itemLocation != null) {
            if (rssi < -85) {
                itemLocation.setText("远离");
            }
            if (rssi > -85 && rssi < -70) {
                itemLocation.setText("较远");
            }
            if (rssi > -70) {
                itemLocation.setText("很近");
            }
        }
        if (itemTime != null) {
            itemTime.setText("现在");
        }
    }

    private void updateItemBattery(String battery) {
        if (itemBattery != null && Integer.valueOf(battery) < 20) {
            itemBattery.setVisibility(View.VISIBLE);
            itemBattery.setText("剩余电量  " + battery + "%");
        }
        if (itemTime != null) {
            itemTime.setText("现在");
        }
    }

}
