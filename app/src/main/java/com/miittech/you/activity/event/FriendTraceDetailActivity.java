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
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.model.LatLng;
import com.clj.fastble.BleManager;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
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
import com.miittech.you.net.response.FriendTraceResponse;
import com.miittech.you.net.response.FriendsResponse;
import com.miittech.you.weight.CircleImageView;
import com.miittech.you.weight.Titlebar;
import com.ryon.mutils.EncryptUtils;
import com.ryon.mutils.LogUtils;
import com.ryon.mutils.SPUtils;
import com.ryon.mutils.TimeUtils;
import com.ryon.mutils.ToastUtils;

import java.text.DateFormat;
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

public class FriendTraceDetailActivity extends BaseActivity {

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
    FriendsResponse.FriendlistBean friend;
    private TraceDalySelectAdapter traceDalySelectAdapter;
    private MapStatus ms;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_trace_detail);
        ButterKnife.bind(this);
        friend = (FriendsResponse.FriendlistBean)getIntent().getSerializableExtra(IntentExtras.DEVICE.DATA);
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
                Intent intent = new Intent(FriendTraceDetailActivity.this, SettingActivity.class);
                startActivity(intent);
            }
        });
        mBaiduMap = mapView.getMap();
        //普通地图
        mBaiduMap.setMapType(BaiduMap.MAP_TYPE_NORMAL);
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
        initFriendInfo();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void initFriendInfo() {
        itemTitle.setText(Common.decodeBase64(friend.getNickname()));
        tvIsShared.setVisibility(View.GONE);
        itemShared.setVisibility(View.GONE);
        GlideApp.with(this)
                .load(friend.getHeadimg())
                .error(R.drawable.ic_header_img)
                .placeholder(R.drawable.ic_header_img)
                .into(itemIcon);
    }

    private void getPoints(Date date) {
        String daly = TimeUtils.date2String(date, new SimpleDateFormat("yyyyMMdd"));
        Map param = new HashMap();
        param.put("friendid", friend.getFriendid());
        param.put("qrytype", Params.QRY_TYPE.TRACE);
        param.put("sdate", daly);
        param.put("edate", daly);
        String json = new Gson().toJson(param);
        PubParam pubParam = new PubParam(Common.getUserId());
        String sign_unSha1 = pubParam.toValueString() + json + Common.getTocken();
        LogUtils.d("sign_unsha1", sign_unSha1);
        String sign = EncryptUtils.encryptSHA1ToString(sign_unSha1).toLowerCase();
        LogUtils.d("sign_sha1", sign);
        String path = HttpUrl.Api + "friendtrace/" + pubParam.toUrlParam(sign);
        final RequestBody requestBody = RequestBody.create(MediaType.parse(HttpUrl.MediaType_Json), json);

        ApiServiceManager.getInstance().buildApiService(this).postGetFriendTraceList(path, requestBody)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new Consumer<FriendTraceResponse>() {
                @Override
                public void accept(FriendTraceResponse response) throws Exception {
                    if (response.isSuccessful()) {
                        initMapPoints(response.getTracelist());
                    } else {
                        response.onError(FriendTraceDetailActivity.this);
                    }
                }
            }, new Consumer<Throwable>() {
                @Override
                public void accept(Throwable throwable) throws Exception {
                    throwable.printStackTrace();
                }
            });
    }

    private void initMapPoints(List<FriendTraceResponse.TracelistBean> tracelist) {
        if (tracelist == null || tracelist.size() <= 0) {
            ToastUtils.showShort("没有发现轨迹信息");
            return;
        }
        List<OverlayOptions> options = new ArrayList<OverlayOptions>();
        FriendTraceResponse.TracelistBean tracelistBean = tracelist.get(0);
        LatLng latLng = new LatLng(tracelistBean.getUser_loc().getLat(), tracelistBean.getUser_loc().getLng());
        itemLocation.setText(Common.decodeBase64(tracelistBean.getUser_loc().getAddr()));
        setTimeText(tracelistBean.getReptime());
        ms = new MapStatus.Builder().target(latLng).zoom(16).build();
        mBaiduMap.animateMapStatus(MapStatusUpdateFactory.newMapStatus(ms));
        for (FriendTraceResponse.TracelistBean traceItem : tracelist) {
            LatLng point1 = new LatLng(traceItem.getUser_loc().getLat(), traceItem.getUser_loc().getLng());
            BitmapDescriptor descriptor = BitmapDescriptorFactory.fromResource(R.drawable.ic_map_point);
            OverlayOptions option1 = new MarkerOptions()
                    .position(point1)
                    .icon(descriptor);
            options.add(option1);
        }
        mBaiduMap.addOverlays(options);
    }

    private void setTimeText(String lasttime) {
        if (itemTime == null) {
            return;
        }
        DateFormat dateFormat = new SimpleDateFormat("yyyy年MM月dd日 a hh:mm");
        Date date = TimeUtils.string2Date(lasttime,new SimpleDateFormat("yyyyMMddHHmmss"));
        itemTime.setText(TimeUtils.date2String(date,dateFormat));
    }
}
