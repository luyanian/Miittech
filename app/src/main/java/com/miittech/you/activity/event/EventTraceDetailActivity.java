package com.miittech.you.activity.event;

import android.content.Intent;
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
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.model.LatLng;
import com.bumptech.glide.Glide;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.inuker.bluetooth.library.Constants;
import com.miittech.you.App;
import com.miittech.you.R;
import com.miittech.you.activity.BaseActivity;
import com.miittech.you.activity.MainActivity;
import com.miittech.you.activity.device.DeviceDetailActivity;
import com.miittech.you.activity.setting.SettingActivity;
import com.miittech.you.adapter.EventLogAdapter;
import com.miittech.you.adapter.TraceDalySelectAdapter;
import com.miittech.you.common.Common;
import com.miittech.you.entity.Locinfo;
import com.miittech.you.fragment.EventLogFragment;
import com.miittech.you.global.HttpUrl;
import com.miittech.you.global.IntentExtras;
import com.miittech.you.global.Params;
import com.miittech.you.global.PubParam;
import com.miittech.you.global.SPConst;
import com.miittech.you.impl.OnListItemClick;
import com.miittech.you.impl.TitleBarOptions;
import com.miittech.you.manager.BLEClientManager;
import com.miittech.you.net.ApiServiceManager;
import com.miittech.you.net.response.DeviceInfoResponse;
import com.miittech.you.net.response.DeviceResponse;
import com.miittech.you.net.response.UserInfoResponse;
import com.miittech.you.weight.CircleImageView;
import com.miittech.you.weight.Titlebar;
import com.ryon.mutils.EncryptUtils;
import com.ryon.mutils.LogUtils;
import com.ryon.mutils.NetworkUtils;
import com.ryon.mutils.SPUtils;
import com.ryon.mutils.TimeUtils;
import com.ryon.mutils.ToastUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
    @BindView(R.id.item_icon_status)
    ImageView itemIconStatus;
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
    private ClusterManager mClusterManager;
    private MapStatus ms;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_trace_detail);
        ButterKnife.bind(this);
        devlistBean = (DeviceResponse.DevlistBean) getIntent().getSerializableExtra(IntentExtras.DEVICE.DATA);
        initMyTitleBar(titlebar,"LOGO");
        titlebar.showBackOption();
        titlebar.showSettingOption();
        titlebar.setTitleBarOptions(new TitleBarOptions(){
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
        if(locinfo!=null) {
            ms = new MapStatus.Builder().target(new LatLng(locinfo.getLat(), locinfo.getLng())).zoom(16).build();
            mBaiduMap.animateMapStatus(MapStatusUpdateFactory.newMapStatus(ms));
        }
        // 定义点聚合管理类ClusterManager
        mClusterManager = new ClusterManager<MyItem>(this, mBaiduMap);
        // 设置地图监听，当地图状态发生改变时，进行点聚合运算
        mBaiduMap.setOnMapStatusChangeListener(mClusterManager);
        // 设置maker点击时的响应
        mBaiduMap.setOnMarkerClickListener(mClusterManager);
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
                getPoints(date);
            }
        });
        recyclerview.setAdapter(traceDalySelectAdapter);
        traceDalySelectAdapter.scrollToEnd(recyclerview);
        getPoints(new Date());
        initDeviceInfo();
    }

    private void initDeviceInfo() {
        itemTitle.setText(Common.decodeBase64(devlistBean.getDevname()));
        tvIsShared.setVisibility(View.GONE);
        if(TextUtils.isEmpty(devlistBean.getFriendname())){
            itemShared.setVisibility(View.GONE);
        }else {
            itemShared.setVisibility(View.VISIBLE);
            itemShared.setText("分享自" + Common.decodeBase64(devlistBean.getFriendname()));
        }

        if(BLEClientManager.getClient().getConnectStatus(Common.formatDevId2Mac(devlistBean.getDevidX()))!= Constants.STATUS_DEVICE_CONNECTED){
            itemLocation.setText(Common.decodeBase64(devlistBean.getLocinfo().getAddr()));
            setTimeText(itemTime,devlistBean.getLasttime());
        }
        Glide.with(this).load(devlistBean.getDevimg()).into(itemIcon);
    }

    private void getPoints(Date date) {
        String daly = TimeUtils.date2String(date,new SimpleDateFormat("yyyyMMdd"));
        Map param = new HashMap();
        param.put("devid", devlistBean.getDevidX());
        param.put("qrytype", Params.QRY_TYPE.TRACE);
        param.put("sdate", daly);
        param.put("edate", daly);
        String json = new Gson().toJson(param);
        PubParam pubParam = new PubParam(App.getInstance().getUserId());
        String sign_unSha1 = pubParam.toValueString() + json + App.getInstance().getTocken();
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
        if(tracelist==null||tracelist.size()<=0){
            ToastUtils.showShort("没有发现轨迹信息");
            return;
        }
        List<OverlayOptions> options = new ArrayList<OverlayOptions>();
        DeviceInfoResponse.TracelistBean.LocinfoBean locinfoBean = tracelist.get(0).getLocinfo();
        LatLng latLng = new LatLng(locinfoBean.getLat(),locinfoBean.getLng());
        ms = new MapStatus.Builder().target(latLng).zoom(16).build();
        mBaiduMap.animateMapStatus(MapStatusUpdateFactory.newMapStatus(ms));
        List<MyItem> items = new ArrayList<>();
        for (DeviceInfoResponse.TracelistBean tracelistBean : tracelist){
            LatLng point1 = new LatLng(tracelistBean.getLocinfo().getLat(), tracelistBean.getLocinfo().getLng());
            BitmapDescriptor descriptor = BitmapDescriptorFactory.fromResource(R.drawable.ic_map_point);
            OverlayOptions option1 =  new MarkerOptions()
                    .position(point1)
                    .icon(descriptor);
            options.add(option1);

            items.add(new MyItem(point1));
        }
        mBaiduMap.addOverlays(options);
//        mClusterManager.addItems(items);
    }

    private void setTimeText(TextView itemTime, String lasttime) {
        if(itemTime==null){
            return;
        }
        SimpleDateFormat sdf=new SimpleDateFormat("yyyyMMddhhmmss");
        String timeSpan = TimeUtils.getFriendlyTimeSpanByNow(lasttime,sdf);
        itemTime.setText(timeSpan);
    }

    @Override
    public void onMapLoaded() {
        ms = new MapStatus.Builder().zoom(9).build();
        mBaiduMap.animateMapStatus(MapStatusUpdateFactory.newMapStatus(ms));
    }

    /**
     * 每个Marker点，包含Marker点坐标以及图标
     */
    public class MyItem implements ClusterItem {
        private final LatLng mPosition;

        public MyItem(LatLng latLng) {
            mPosition = latLng;
        }

        @Override
        public LatLng getPosition() {
            return mPosition;
        }

        @Override
        public BitmapDescriptor getBitmapDescriptor() {
            return BitmapDescriptorFactory
                    .fromResource(R.drawable.ic_map_point);
        }
    }
}
