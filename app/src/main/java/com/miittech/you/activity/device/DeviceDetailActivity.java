package com.miittech.you.activity.device;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.gson.Gson;
import com.miittech.you.App;
import com.miittech.you.R;
import com.miittech.you.activity.BaseActivity;
import com.miittech.you.ble.BleClient;
import com.miittech.you.dialog.DialogUtils;
import com.miittech.you.dialog.MsgTipDialog;
import com.miittech.you.entity.DeviceInfo;
import com.miittech.you.glide.GlideApp;
import com.miittech.you.global.HttpUrl;
import com.miittech.you.global.IntentExtras;
import com.miittech.you.global.Params;
import com.miittech.you.global.PubParam;
import com.miittech.you.global.SPConst;
import com.miittech.you.impl.OnMsgTipOptions;
import com.miittech.you.impl.OnNetRequestCallBack;
import com.miittech.you.impl.TitleBarOptions;
import com.miittech.you.impl.TypeSelectorChangeLisener;
import com.miittech.you.net.ApiServiceManager;
import com.miittech.you.net.response.DeviceListResponse;
import com.miittech.you.utils.Common;
import com.miittech.you.utils.GlideImageLoader;
import com.miittech.you.weight.CircleImageView;
import com.miittech.you.weight.Titlebar;
import com.miittech.you.weight.TypeSelector;
import com.ryon.mutils.EncryptUtils;
import com.ryon.mutils.LogUtils;
import com.ryon.mutils.NetworkUtils;
import com.ryon.mutils.SPUtils;
import com.ryon.mutils.TimeUtils;
import com.ryon.mutils.ToastUtils;
import com.youth.banner.Banner;
import com.youth.banner.BannerConfig;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import okhttp3.MediaType;
import okhttp3.RequestBody;

/**
 * Created by Administrator on 2017/10/25.
 */

public class DeviceDetailActivity extends BaseActivity {
    @BindView(R.id.titlebar)
    Titlebar titlebar;
    @BindView(R.id.img_device_icon)
    CircleImageView imgDeviceIcon;
    @BindView(R.id.tv_device_name)
    TextView tvDeviceName;
    @BindView(R.id.rl_bell_status)
    RelativeLayout rlBellStatus;
    @BindView(R.id.img_find_butten)
    ImageView imgFindBtn;
    @BindView(R.id.typeSelector)
    TypeSelector typeSelector;
    @BindView(R.id.ll_tips)
    LinearLayout llTips;
    @BindView(R.id.ll_options)
    LinearLayout llOptions;
    @BindView(R.id.tv_device_location)
    TextView tvDeviceLocation;
    @BindView(R.id.tv_battarry)
    TextView tvBattarry;
    @BindView(R.id.tv_device_time)
    TextView tvDeviceTime;
    @BindView(R.id.banner)
    Banner banner;
    @BindView(R.id.tv_tips)
    TextView tvTips;
    private DeviceInfo deviceInfo;
    private CmdResponseReceiver cmdResponseReceiver = new CmdResponseReceiver();
    private int deviceState_connect = 0x01;
    private int deviceState_disconnect = 0x02;
    private int deviceState_lowbattery = 0x03;
    private int deviceState = deviceState_disconnect;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_detail);
        ButterKnife.bind(this);
        initMyTitleBar(titlebar, "设备");
        titlebar.showBackOption();
        titlebar.setTitleBarOptions(new TitleBarOptions() {
            @Override
            public void onBack() {
                super.onBack();
                finish();
            }
        });

        IntentFilter filter = new IntentFilter();
        filter.addAction(IntentExtras.ACTION.ACTION_CMD_RESPONSE);
        App.getInstance().getLocalBroadCastManager().registerReceiver(cmdResponseReceiver, filter);

        //banner设置方法全部调用完毕时最后调用
        banner.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                if(deviceState == deviceState_connect){
                    if(position==0){
                        tvTips.setText("您的物品就在附近\n试试点击“响铃“按钮来找");
                    }else if(position==1){
                        tvTips.setText("当您找不到手机的时候\n试试双击有物背面中央处的按钮，让手机响起来");
                    }
                }else if(deviceState ==deviceState_disconnect){
                    tvTips.setText("在附近走走，发现图标变成连接状态时\n点击“响铃”按钮，来寻找有物");
                }else if(deviceState == deviceState_lowbattery){
                    tvTips.setText("贴片电池电量低时\n打开背面盖板，更换贴片电池");
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
        banner.setBannerStyle(BannerConfig.NOT_INDICATOR);

        typeSelector.setItemText("小贴士", "功能");
        typeSelector.setTypeSelectorChangeLisener(new TypeSelectorChangeLisener() {
            @Override
            public void onTabSelectorChanged(int item) {
                if (item == 0) {
                    llTips.setVisibility(View.VISIBLE);
                    llOptions.setVisibility(View.GONE);
                }
                if (item == 1) {
                    llTips.setVisibility(View.GONE);
                    llOptions.setVisibility(View.VISIBLE);
                }
            }
        });
        typeSelector.setSelectItem(0);
    }

    @Override
    protected void onResume() {
        super.onResume();
        initDeviceInfoFromList();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        App.getInstance().getLocalBroadCastManager().unregisterReceiver(cmdResponseReceiver);
    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
    }

    private void initDeviceInfoFromList() {
        DeviceInfo devlistBean = (DeviceInfo) getIntent().getSerializableExtra(IntentExtras.DEVICE.DATA);
        DeviceInfo deviceInfo = (DeviceInfo) SPUtils.getInstance().readObject(Common.formatDevId2Mac(devlistBean.getDevidX()));
        if (deviceInfo != null) {
            initDeviceInfo(deviceInfo);
        } else {
            initDeviceInfo(devlistBean);
        }
        if (getIntent().hasExtra("location")) {
            tvDeviceLocation.setText(getIntent().getStringExtra("location"));
        }
        if (getIntent().hasExtra("time")) {
            tvDeviceTime.setText(getIntent().getStringExtra("time"));
        }
    }

    private void initDeviceInfo(DeviceInfo deviceInfo) {
        this.deviceInfo = deviceInfo;
        tvDeviceName.setText(Common.decodeBase64(deviceInfo.getDevname()));
        tvBattarry.setVisibility(View.GONE);

        if (BleClient.getInstance().isConnected(Common.formatDevId2Mac(deviceInfo.getDevidX()))) {
            tvDeviceTime.setText("现在");
        } else {
            setTimeText(tvDeviceTime, deviceInfo.getLasttime());
            tvDeviceLocation.setText(Common.decodeBase64(deviceInfo.getLocinfo().getAddr()));
        }
        GlideApp.with(this)
                .load(deviceInfo.getDevimg())
                .error(Common.getDefaultDevImgResouceId(Common.decodeBase64(deviceInfo.getGroupname())))
                .placeholder(Common.getDefaultDevImgResouceId(Common.decodeBase64(deviceInfo.getGroupname())))
                .into(imgDeviceIcon);
        switchFindBtnStyle();
        setConnectStatusStyle(Common.formatDevId2Mac(deviceInfo.getDevidX()));

    }

    @OnClick({R.id.rl_bell_status, R.id.btn_option_map, R.id.btn_option_share, R.id.btn_option_setting, R.id.btn_option_delete})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.rl_bell_status:
                if (deviceInfo == null) {
                    return;
                }
                if (TextUtils.isEmpty(deviceInfo.getOwneruser()) || "0".equals(deviceInfo.getOwneruser())) {
                    doFindOrBell();
                } else {
                    ToastUtils.showShort("您不是有物拥有者，无操作权限");
                }
                break;
            case R.id.btn_option_map:
                if (deviceInfo == null) {
                    return;
                }
                Intent map = new Intent(this, DeviceMapDetailActivity.class);
                map.putExtra(IntentExtras.DEVICE.DATA, deviceInfo);
                map.putExtra("location", tvDeviceLocation.getText().toString());
                map.putExtra("time", tvDeviceTime.getText().toString());
                startActivity(map);
                break;
            case R.id.btn_option_share:
                if (deviceInfo == null) {
                    return;
                }
                if (TextUtils.isEmpty(deviceInfo.getOwneruser()) || "0".equals(deviceInfo.getOwneruser())) {
                    Intent toshareIntent = new Intent(this, DeviceSharedListActivity.class);
                    toshareIntent.putExtra(IntentExtras.DEVICE.DATA, deviceInfo);
                    startActivity(toshareIntent);
                } else {
                    ToastUtils.showShort("您不是有物拥有者，无操作权限");
                }
                break;
            case R.id.btn_option_setting:
                if (deviceInfo == null) {
                    return;
                }
                if (TextUtils.isEmpty(deviceInfo.getOwneruser()) || "0".equals(deviceInfo.getOwneruser())) {
                    Intent intent = new Intent(this, DeviceDetailSettingActivity.class);
                    intent.putExtra(IntentExtras.DEVICE.DATA, this.deviceInfo);
                    startActivityForResult(intent, 0);
                } else {
                    ToastUtils.showShort("您不是有物拥有者，无操作权限");
                }
                break;
            case R.id.btn_option_delete:
                if (deviceInfo == null) {
                    return;
                }
                if (!TextUtils.isEmpty(deviceInfo.getOwneruser()) && !"0".equals(deviceInfo.getOwneruser())) {
                    ToastUtils.showShort("您不是有物拥有者，无操作权限");
                    return;
                }
                MsgTipDialog msgTipDialog = DialogUtils.getInstance().createMsgTipDialog(this);
                msgTipDialog.setTitle("删除设备").setMsg("删除操作将清空所有的设备信息，是否确定删除");
                msgTipDialog.setOnMsgTipOptions(new OnMsgTipOptions() {
                    @Override
                    public void onSure() {
                        super.onSure();
                        Intent unbind = new Intent(IntentExtras.ACTION.ACTION_BLE_COMMAND);
                        unbind.putExtra("cmd", IntentExtras.CMD.CMD_DEVICE_UNBIND);
                        unbind.putExtra("address", Common.formatDevId2Mac(deviceInfo.getDevidX()));
                        App.getInstance().getLocalBroadCastManager().sendBroadcast(unbind);
                    }

                    @Override
                    public void onCancel() {
                        super.onCancel();

                    }
                });
                msgTipDialog.show();
                break;
        }
    }

    private void doFindOrBell() {
        Intent intent = new Intent(IntentExtras.ACTION.ACTION_BLE_COMMAND);
        if (SPUtils.getInstance(SPConst.ALET_STATUE.SP_NAME).getInt(deviceInfo.getDevidX(), SPConst.ALET_STATUE.STATUS_UNBELL) == SPConst.ALET_STATUE.STATUS_UNBELL) {
            intent.putExtra("cmd", IntentExtras.CMD.CMD_DEVICE_ALERT_START);
        } else {
            intent.putExtra("cmd", IntentExtras.CMD.CMD_DEVICE_ALERT_STOP);
        }
        intent.putExtra("address", Common.formatDevId2Mac(deviceInfo.getDevidX()));
        App.getInstance().getLocalBroadCastManager().sendBroadcast(intent);
    }

    private void switchFindBtnStyle() {
        String mac = Common.formatDevId2Mac(deviceInfo.getDevidX());
        if (BleClient.getInstance().isConnected(mac)) {
            onDeviceStateChange(deviceState_connect);
            if (SPUtils.getInstance(SPConst.ALET_STATUE.SP_NAME).getInt(deviceInfo.getDevidX(), SPConst.ALET_STATUE.STATUS_UNBELL) == SPConst.ALET_STATUE.STATUS_UNBELL) {
                rlBellStatus.setBackgroundResource(R.drawable.shape_corner_device_find);
                imgFindBtn.setImageResource(R.drawable.ic_device_find);
            } else {
                rlBellStatus.setBackgroundResource(R.drawable.shape_corner_device_bell);
                imgFindBtn.setImageResource(R.drawable.ic_device_bell);
            }
        } else {
            onDeviceStateChange(deviceState_disconnect);
            rlBellStatus.setBackgroundResource(R.drawable.shape_corner_device_diconnect);
            imgFindBtn.setImageResource(R.drawable.ic_device_find);
        }
    }

    private void setTimeText(TextView itemTime, String lasttime) {
        if (itemTime == null) {
            return;
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        String timeSpan = TimeUtils.getFriendlyTimeSpanByNow(lasttime, sdf);
        itemTime.setText(timeSpan);
    }

    private void unbindDevice() {
        if (!NetworkUtils.isConnected()) {
            ToastUtils.showShort(R.string.msg_net_error);
            return;
        }
        Map param = new HashMap();
        param.put("devid", deviceInfo.getDevidX());
        param.put("method", Params.METHOD.UNBIND);
        String json = new Gson().toJson(param);
        PubParam pubParam = new PubParam(Common.getUserId());
        String sign_unSha1 = pubParam.toValueString() + json + Common.getTocken();
        LogUtils.d("sign_unsha1", sign_unSha1);
        String sign = EncryptUtils.encryptSHA1ToString(sign_unSha1).toLowerCase();
        LogUtils.d("sign_sha1", sign);
        String path = HttpUrl.Api + "devbind/" + pubParam.toUrlParam(sign);
        final RequestBody requestBody = RequestBody.create(MediaType.parse(HttpUrl.MediaType_Json), json);
        ApiServiceManager.getInstance().buildApiService(this).postDeviceOption(path, requestBody)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<DeviceListResponse>() {
                    @Override
                    public void accept(DeviceListResponse response) throws Exception {
                        if (response.isSuccessful()) {
                            ToastUtils.showShort("删除成功");
                            Common.initDeviceList(App.getInstance(), new OnNetRequestCallBack() {
                                @Override
                                public void OnRequestComplete() {
                                    DeviceDetailActivity.this.finish();
                                }
                            });
                        } else {
                            response.onError(DeviceDetailActivity.this);
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        throwable.printStackTrace();
                    }
                });
    }

    private class CmdResponseReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(IntentExtras.ACTION.ACTION_CMD_RESPONSE)) {
                if (deviceInfo == null) {
                    return;
                }
                String address = intent.getStringExtra("address");
                int ret = intent.getIntExtra("ret", -1);//获取Extra信息
                switch (ret) {
                    case IntentExtras.RET.RET_BLE_CONNECT_SUCCESS:
                        if (address.equals(Common.formatDevId2Mac(deviceInfo.getDevidX()))) {
                            LogUtils.d("RET_BLE_CONNECT_SUCCESS");
                            setConnectStatusStyle(address);
                            switchFindBtnStyle();
                        }
                        break;
                    case IntentExtras.RET.RET_BLE_MODE_WORK_SUCCESS:
                        if (address.equals(Common.formatDevId2Mac(deviceInfo.getDevidX()))) {
                            LogUtils.d("RET_DEVICE_CONNECT_SUCCESS");
                            setConnectStatusStyle(address);
                            switchFindBtnStyle();
                        }
                        break;
                    case IntentExtras.RET.RET_BLE_MODE_WORK_FAIL:
                        if (address.equals(Common.formatDevId2Mac(deviceInfo.getDevidX()))) {
                            LogUtils.d("RET_DEVICE_CONNECT_FAILED");
                            setConnectStatusStyle(address);
                            switchFindBtnStyle();
                        }
                        break;
                    case IntentExtras.RET.RET_BLE_ALERT_STARTED:
                        if (address.equals(Common.formatDevId2Mac(deviceInfo.getDevidX()))) {
                            SPUtils.getInstance(SPConst.ALET_STATUE.SP_NAME).put(deviceInfo.getDevidX(), SPConst.ALET_STATUE.STATUS_BELLING);
                            switchFindBtnStyle();
                        }
                        break;
                    case IntentExtras.RET.RET_BLE_ALERT_STOPED:
                        if (address.equals(Common.formatDevId2Mac(deviceInfo.getDevidX()))) {
                            SPUtils.getInstance(SPConst.ALET_STATUE.SP_NAME).put(deviceInfo.getDevidX(), SPConst.ALET_STATUE.STATUS_UNBELL);
                            switchFindBtnStyle();
                        }
                        break;
                    case IntentExtras.RET.RET_BLE_DISCONNECT:
                        if (address.equals(Common.formatDevId2Mac(deviceInfo.getDevidX()))) {
                            switchFindBtnStyle();
                            setConnectStatusStyle(address);
                        }
                        break;
                    case IntentExtras.RET.RET_BLE_READ_RSSI:
                        if (address.equals(Common.formatDevId2Mac(deviceInfo.getDevidX()))) {
                            LogUtils.d("RET_DEVICE_READ_RSSI");
                            int rssi = intent.getIntExtra("rssi", 0);
                            setConnectStatusStyle(address, rssi);
                            updateItemRssi(rssi);
                        }
                        break;
                    case IntentExtras.RET.RET_BLE_READ_BATTERY:
                        if (address.equals(Common.formatDevId2Mac(deviceInfo.getDevidX()))) {
                            LogUtils.d("RET_DEVICE_READ_BATTERY");
                            String battery = intent.getStringExtra("battery");
                            updateItemBattery(battery);
                        }
                        break;
                    case IntentExtras.RET.RET_BLE_UNBIND_COMPLETE:
                        unbindDevice();
                        break;
                    case IntentExtras.RET.RET_BLE_UNBIND_FAILD:
                        ToastUtils.showShort("解绑失败，请重试");
                        break;
                }

            }
        }
    }

    private void setConnectStatusStyle(String mac) {
        setConnectStatusStyle(mac, -50);
    }

    private void setConnectStatusStyle(String mac, int rssi) {
        if (imgDeviceIcon == null) {
            return;
        }
        if (BleClient.getInstance().isConnected(mac)) {
            if (rssi > -50) {
                imgDeviceIcon.setBorderColor(getResources().getColor(R.color.ic_connect1));
            } else if (rssi <= -50 && rssi > -65) {
                imgDeviceIcon.setBorderColor(getResources().getColor(R.color.ic_connect2));
            } else if (rssi <= -65 && rssi > -85) {
                imgDeviceIcon.setBorderColor(getResources().getColor(R.color.ic_connect3));
            } else if (rssi <= -85 && rssi > -100) {
                imgDeviceIcon.setBorderColor(getResources().getColor(R.color.ic_connect4));
            } else if (rssi <= -100) {
                imgDeviceIcon.setBorderColor(getResources().getColor(R.color.ic_connect5));
            }
            onDeviceStateChange(deviceState_connect);
        } else {
            imgDeviceIcon.setBorderColor(getResources().getColor(R.color.windowBg));
            onDeviceStateChange(deviceState_disconnect);
        }
    }

    private void updateItemRssi(int rssi) {
        if (tvDeviceLocation != null) {
            if (rssi < -85) {
                tvDeviceLocation.setText("远离");
            }
            if (rssi > -85 && rssi < -70) {
                tvDeviceLocation.setText("较远");
            }
            if (rssi > -70) {
                tvDeviceLocation.setText("很近");
            }
        }
        if (tvDeviceTime != null) {
            tvDeviceTime.setText("现在");
        }
    }

    private void updateItemBattery(String battery) {
        if (tvBattarry != null && Integer.valueOf(battery) < 20) {
            tvBattarry.setVisibility(View.VISIBLE);
            tvBattarry.setText("剩余电量  " + battery + "%");
            onDeviceStateChange(deviceState_lowbattery);
        }
        if (tvDeviceTime != null) {
            tvDeviceTime.setText("现在");
        }
    }

    public void onDeviceStateChange(int state){
        deviceState = state;
        //设置图片加载器
        banner.setImageLoader(new GlideImageLoader());
        List<Integer> list = new ArrayList<>();
        if(state == deviceState_connect){
            list.add(R.drawable.ic_tips_1);
            list.add(R.drawable.ic_tips_2);
        }else if(state ==deviceState_disconnect){
            list.add(R.drawable.ic_tips_3);
        }else if(state == deviceState_lowbattery){
            list.add(R.drawable.ic_tips_4);
        }
        if(banner.isActivated()){
            banner.update(list);
        }else{
            //设置图片集合
            banner.setImages(list);
            banner.start();
        }

    }
}
