package com.miittech.you.activity.device;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.model.LatLng;
import com.clj.fastble.BleManager;
import com.miittech.you.R;
import com.miittech.you.activity.BaseActivity;
import com.miittech.you.common.Common;
import com.miittech.you.entity.Locinfo;
import com.miittech.you.global.IntentExtras;
import com.miittech.you.global.SPConst;
import com.miittech.you.impl.TitleBarOptions;
import com.miittech.you.net.response.DeviceInfoResponse;
import com.miittech.you.weight.Titlebar;
import com.ryon.mutils.AppUtils;
import com.ryon.mutils.LogUtils;
import com.ryon.mutils.SPUtils;
import com.ryon.mutils.TimeUtils;
import com.ryon.mutils.ToastUtils;

import java.text.SimpleDateFormat;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by ryon on 2017/12/2.
 */

public class DeviceMapDetailActivity extends BaseActivity {
    @BindView(R.id.titlebar)
    Titlebar titlebar;
    @BindView(R.id.tv_title)
    TextView tvTitle;
    @BindView(R.id.tv_time)
    TextView tvTime;
    @BindView(R.id.map_view)
    MapView mapView;
    @BindView(R.id.rl_bell_status)
    RelativeLayout rlBellStatus;
    @BindView(R.id.img_find_butten)
    ImageView imgFindButten;
    private CmdResponseReceiver cmdResponseReceiver = new CmdResponseReceiver();
    DeviceInfoResponse.UserinfoBean.DevinfoBean deviceDetailInfo;
    private BaiduMap mBaiduMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_log_detail);
        ButterKnife.bind(this);
        deviceDetailInfo = (DeviceInfoResponse.UserinfoBean.DevinfoBean) getIntent().getSerializableExtra(IntentExtras.DEVICE.DATA);
        initView(deviceDetailInfo);
        switchFindBtnStyle();
        IntentFilter filter=new IntentFilter();
        filter.addAction(IntentExtras.ACTION.ACTION_CMD_RESPONSE);
        this.registerReceiver(cmdResponseReceiver,filter);
    }

    private void initView(DeviceInfoResponse.UserinfoBean.DevinfoBean devlistBean) {
        initMyTitleBar(titlebar, "");
        titlebar.showBackOption();
        titlebar.setTitleBarOptions(new TitleBarOptions() {
            @Override
            public void onBack() {
                super.onBack();
                finish();
            }
        });
        if(BleManager.getInstance().isConnected(Common.formatDevId2Mac(deviceDetailInfo.getDevid()))) {
            updateMapLocalView((Locinfo) SPUtils.getInstance().readObject(SPConst.LOC_INFO));
        }else{
            updateMapLocalView(null);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.unregisterReceiver(cmdResponseReceiver);
    }

    @OnClick({R.id.img_navagation, R.id.rl_bell_status})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.img_navagation:
                if (AppUtils.isInstallApp("com.baidu.BaiduMap")) {
                    Intent i1 = new Intent();
                    i1.setData(Uri.parse("baidumap://map/geocoder?location=" + deviceDetailInfo.getLocinfo().getLat() + "," + deviceDetailInfo.getLocinfo().getLng()));
                    startActivity(i1);
                } else {
                    Intent l2 = new Intent(Intent.ACTION_VIEW);
                    l2.setData(Uri.parse("http://api.map.baidu.com/geocoder?location=" + deviceDetailInfo.getLocinfo().getLat() + "," + deviceDetailInfo.getLocinfo().getLng()
                            + "&coord_type=bd09ll&output=html&src=智云有物"));
                    startActivity(l2);
                }
                break;
            case R.id.rl_bell_status:
                if(TextUtils.isEmpty(deviceDetailInfo.getOwneruser())||"0".equals(deviceDetailInfo.getOwneruser())) {
                    doFindOrBell();
                }
                break;
        }
    }

    private void doFindOrBell() {
        Intent intent = new Intent(IntentExtras.ACTION.ACTION_BLE_COMMAND);
        if (SPUtils.getInstance(SPConst.ALET_STATUE.SP_NAME).getInt(deviceDetailInfo.getDevid(), SPConst.ALET_STATUE.STATUS_UNBELL) == SPConst.ALET_STATUE.STATUS_UNBELL) {
            intent.putExtra("cmd", IntentExtras.CMD.CMD_DEVICE_ALERT_START);
        } else {
            intent.putExtra("cmd", IntentExtras.CMD.CMD_DEVICE_ALERT_STOP);
        }
        intent.putExtra("address", Common.formatDevId2Mac(deviceDetailInfo.getDevid()));
        sendBroadcast(intent);
    }

    private void switchFindBtnStyle() {
        String mac = Common.formatDevId2Mac(deviceDetailInfo.getDevid());
        if(BleManager.getInstance().isConnected(mac)) {
            if (SPUtils.getInstance(SPConst.ALET_STATUE.SP_NAME).getInt(deviceDetailInfo.getDevid(), SPConst.ALET_STATUE.STATUS_UNBELL) == SPConst.ALET_STATUE.STATUS_UNBELL) {
                rlBellStatus.setBackgroundResource(R.drawable.shape_corner_device_find);
                imgFindButten.setImageResource(R.drawable.ic_device_find);
            } else {
                rlBellStatus.setBackgroundResource(R.drawable.shape_corner_device_bell);
                imgFindButten.setImageResource(R.drawable.ic_device_bell);
            }
        }else{
            rlBellStatus.setBackgroundResource(R.drawable.shape_corner_device_diconnect);
            imgFindButten.setImageResource(R.drawable.ic_device_find);
        }
    }
    public void updateMapLocalView(Locinfo locinfo){

        mBaiduMap = mapView.getMap();
        mBaiduMap.setMyLocationEnabled(true);
        MapView.setMapCustomEnable(true);
        mBaiduMap.setMapType(BaiduMap.MAP_TYPE_NORMAL);
        LatLng latLng;
        if(locinfo==null) {
            latLng = new LatLng(deviceDetailInfo.getLocinfo().getLat(), deviceDetailInfo.getLocinfo().getLng());
        }else{
            latLng = new LatLng(locinfo.getLat(),locinfo.getLng());
            tvTime.setText("现在");
            tvTitle.setText(locinfo.getAddr());
        }
        MyLocationData locData = new MyLocationData.Builder()
                .accuracy(200)
                .latitude(latLng.latitude)
                .longitude(latLng.longitude)
                .build();
        mBaiduMap.setMyLocationData(locData);

        MapStatus.Builder builder = new MapStatus.Builder();
        builder.target(latLng).zoom(16.0f);
        mBaiduMap.animateMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()));
    }
    private class CmdResponseReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(IntentExtras.ACTION.ACTION_CMD_RESPONSE)){
                String address = intent.getStringExtra("address");
                int ret = intent.getIntExtra("ret", -1);
                switch (ret){
                    case IntentExtras.RET.RET_BLE_MODE_WORK_SUCCESS:
                        if(address.equals(Common.formatDevId2Mac(deviceDetailInfo.getDevid()))) {
                            LogUtils.d("RET_DEVICE_CONNECT_SUCCESS");
                            switchFindBtnStyle();
                        }
                        break;
                    case IntentExtras.RET.RET_BLE_MODE_WORK_FAIL:
                        if(address.equals(Common.formatDevId2Mac(deviceDetailInfo.getDevid()))) {
                            LogUtils.d("RET_DEVICE_CONNECT_FAILED");
                            switchFindBtnStyle();
                        }
                        break;
                    case IntentExtras.RET.RET_BLE_ALERT_STARTED:
                        if(address.equals(Common.formatDevId2Mac(deviceDetailInfo.getDevid()))) {
                            SPUtils.getInstance(SPConst.ALET_STATUE.SP_NAME).put(deviceDetailInfo.getDevid(), SPConst.ALET_STATUE.STATUS_BELLING);
                            switchFindBtnStyle();
                        }
                        break;
                    case IntentExtras.RET.RET_BLE_ALERT_STOPED:
                        if(address.equals(Common.formatDevId2Mac(deviceDetailInfo.getDevid()))) {
                            SPUtils.getInstance(SPConst.ALET_STATUE.SP_NAME).put(deviceDetailInfo.getDevid(), SPConst.ALET_STATUE.STATUS_UNBELL);
                            switchFindBtnStyle();
                        }
                        break;
                    case IntentExtras.RET.LOCATION:
                        Locinfo locinfo = (Locinfo) intent.getSerializableExtra("data");
                        if(BleManager.getInstance().isConnected(Common.formatDevId2Mac(deviceDetailInfo.getDevid()))) {
                            updateMapLocalView(locinfo);
                        }
                        break;
                }
            }
        }
    }
}
