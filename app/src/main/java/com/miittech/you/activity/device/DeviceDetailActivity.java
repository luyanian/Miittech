package com.miittech.you.activity.device;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.clj.fastble.BleManager;
import com.google.gson.Gson;
import com.miittech.you.R;
import com.miittech.you.activity.BaseActivity;
import com.miittech.you.adapter.DeviceListAdapter;
import com.miittech.you.common.Common;
import com.miittech.you.dialog.DialogUtils;
import com.miittech.you.dialog.MsgTipDialog;
import com.miittech.you.glide.GlideApp;
import com.miittech.you.global.IntentExtras;
import com.miittech.you.global.SPConst;
import com.miittech.you.impl.OnMsgTipOptions;
import com.miittech.you.impl.TitleBarOptions;
import com.miittech.you.impl.TypeSelectorChangeLisener;
import com.miittech.you.net.ApiServiceManager;
import com.miittech.you.global.HttpUrl;
import com.miittech.you.global.Params;
import com.miittech.you.global.PubParam;
import com.miittech.you.net.response.DeviceInfoResponse;
import com.miittech.you.net.response.DeviceResponse;
import com.miittech.you.weight.CircleImageView;
import com.miittech.you.weight.Titlebar;
import com.miittech.you.weight.TypeSelector;
import com.ryon.mutils.EncryptUtils;
import com.ryon.mutils.LogUtils;
import com.ryon.mutils.NetworkUtils;
import com.ryon.mutils.SPUtils;
import com.ryon.mutils.StringUtils;
import com.ryon.mutils.TimeUtils;
import com.ryon.mutils.ToastUtils;
import java.text.SimpleDateFormat;
import java.util.HashMap;
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
    private DeviceResponse.DevlistBean device;
    private DeviceInfoResponse.UserinfoBean.DevinfoBean deviceDetailInfo;
    private CmdResponseReceiver cmdResponseReceiver = new CmdResponseReceiver();

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

        IntentFilter filter=new IntentFilter();
        filter.addAction(IntentExtras.ACTION.ACTION_CMD_RESPONSE);
        this.registerReceiver(cmdResponseReceiver,filter);

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
        device = (DeviceResponse.DevlistBean) getIntent().getSerializableExtra(IntentExtras.DEVICE.DATA);
        if(getIntent().hasExtra("location")){
            tvDeviceLocation.setText(getIntent().getStringExtra("location"));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.unregisterReceiver(cmdResponseReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        getDeviceInfo(device);
    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
    }

    private void initViewData(DeviceInfoResponse.UserinfoBean.DevinfoBean device) {
        this.deviceDetailInfo = device;
        tvDeviceName.setText(Common.decodeBase64(device.getDevname()));
        tvBattarry.setVisibility(View.GONE);

        if(BleManager.getInstance().isConnected(Common.formatDevId2Mac(device.getDevid()))){
            tvDeviceTime.setText("现在");
        }else {
            setTimeText(tvDeviceTime,device.getLasttime());
            tvDeviceLocation.setText(Common.decodeBase64(device.getLocinfo().getAddr()));
        }
        GlideApp.with(this)
                .load(device.getDevimg())
                .error(Common.getDefaultDevImgResouceId(Common.decodeBase64(device.getGroupname())))
                .placeholder(Common.getDefaultDevImgResouceId(Common.decodeBase64(device.getGroupname())))
                .into(imgDeviceIcon);
        switchFindBtnStyle();
        setConnectStatusStyle(Common.formatDevId2Mac(device.getDevimg()));

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            if (data.hasExtra(IntentExtras.DEVICE.NAME)) {
                String name = data.getStringExtra(IntentExtras.DEVICE.NAME);
                if (!StringUtils.isEmpty(name)) {
                    tvDeviceName.setText(name);
                }
            }
        }
    }

    @OnClick({R.id.rl_bell_status, R.id.btn_option_map, R.id.btn_option_share, R.id.btn_option_setting, R.id.btn_option_delete})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.rl_bell_status:
                if(deviceDetailInfo==null){
                    return;
                }
                if(TextUtils.isEmpty(deviceDetailInfo.getOwneruser())||"0".equals(deviceDetailInfo.getOwneruser())) {
                    doFindOrBell();
                }else{
                    ToastUtils.showShort("您不是贴片拥有者，无操作权限");
                }
                break;
            case R.id.btn_option_map:
                if(deviceDetailInfo==null) {
                    return;
                }
                Intent map = new Intent(this, DeviceMapDetailActivity.class);
                map.putExtra(IntentExtras.DEVICE.DATA,deviceDetailInfo);
                startActivity(map);
                break;
            case R.id.btn_option_share:
                if(deviceDetailInfo==null) {
                    return;
                }
                if(TextUtils.isEmpty(deviceDetailInfo.getOwneruser())||"0".equals(deviceDetailInfo.getOwneruser())) {
                    Intent toshareIntent = new Intent(this, DeviceSharedListActivity.class);
                    toshareIntent.putExtra(IntentExtras.DEVICE.DATA, deviceDetailInfo);
                    startActivity(toshareIntent);
                }else{
                    ToastUtils.showShort("您不是贴片拥有者，无操作权限");
                }
                break;
            case R.id.btn_option_setting:
                if(deviceDetailInfo==null) {
                    return;
                }
                if(TextUtils.isEmpty(deviceDetailInfo.getOwneruser())||"0".equals(deviceDetailInfo.getOwneruser())) {
                    Intent intent = new Intent(this, DeviceDetailSettingActivity.class);
                    intent.putExtra(IntentExtras.DEVICE.DATA, this.deviceDetailInfo);
                    startActivity(intent);
                }else{
                    ToastUtils.showShort("您不是贴片拥有者，无操作权限");
                }
                break;
            case R.id.btn_option_delete:
                if(deviceDetailInfo==null) {
                    return;
                }
                if(!TextUtils.isEmpty(deviceDetailInfo.getOwneruser())&&!"0".equals(deviceDetailInfo.getOwneruser())){
                    ToastUtils.showShort("您不是贴片拥有者，无操作权限");
                    return;
                }
                MsgTipDialog msgTipDialog = DialogUtils.getInstance().createMsgTipDialog(this);
                msgTipDialog.setTitle("删除设备").setMsg("删除操作将清空所有的设备信息，是否确定删除");
                msgTipDialog.setOnMsgTipOptions(new OnMsgTipOptions() {
                    @Override
                    public void onSure() {
                        super.onSure();
                        Intent unbind= new Intent(IntentExtras.ACTION.ACTION_BLE_COMMAND);
                        unbind.putExtra("cmd",IntentExtras.CMD.CMD_DEVICE_UNBIND);
                        unbind.putExtra("address",Common.formatDevId2Mac(device.getDevidX()));
                        sendBroadcast(unbind);
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
        if(Common.isIgnoreBell()){
            ToastUtils.showShort("贴片在勿扰范围内");
            return;
        }
        Intent intent= new Intent(IntentExtras.ACTION.ACTION_BLE_COMMAND);
        if(SPUtils.getInstance(SPConst.ALET_STATUE.SP_NAME).getInt(device.getDevidX(),SPConst.ALET_STATUE.STATUS_UNBELL)== SPConst.ALET_STATUE.STATUS_UNBELL){
            intent.putExtra("cmd",IntentExtras.CMD.CMD_DEVICE_ALERT_START);
        }else{
            intent.putExtra("cmd",IntentExtras.CMD.CMD_DEVICE_ALERT_STOP);
        }
        intent.putExtra("address",Common.formatDevId2Mac(device.getDevidX()));
        sendBroadcast(intent);
    }
    private void switchFindBtnStyle() {
        String mac = Common.formatDevId2Mac(device.getDevidX());
        if(BleManager.getInstance().isConnected(mac)) {
            if (SPUtils.getInstance(SPConst.ALET_STATUE.SP_NAME).getInt(device.getDevidX(), SPConst.ALET_STATUE.STATUS_UNBELL) == SPConst.ALET_STATUE.STATUS_UNBELL) {
                rlBellStatus.setBackgroundResource(R.drawable.shape_corner_device_find);
                imgFindBtn.setImageResource(R.drawable.ic_device_find);
            } else {
                rlBellStatus.setBackgroundResource(R.drawable.shape_corner_device_bell);
                imgFindBtn.setImageResource(R.drawable.ic_device_bell);
            }
        }else{
            rlBellStatus.setBackgroundResource(R.drawable.shape_corner_device_diconnect);
            imgFindBtn.setImageResource(R.drawable.ic_device_find);
        }
    }
    private void setTimeText(TextView itemTime, String lasttime) {
        if(itemTime==null){
            return;
        }
        SimpleDateFormat sdf=new SimpleDateFormat("yyyyMMddHHmmss");
        String timeSpan = TimeUtils.getFriendlyTimeSpanByNow(lasttime,sdf);
        itemTime.setText(timeSpan);
    }

    private void getDeviceInfo(final DeviceResponse.DevlistBean device) {
        if(!NetworkUtils.isConnected()){
            DeviceInfoResponse response = (DeviceInfoResponse) SPUtils.getInstance().readObject(Common.formatDevId2Mac(device.getDevidX()));
            if(response!=null){
                initViewData(response.getUserinfo().getDevinfo());
            }
            return;
        }
        Map param = new HashMap();
        param.put("devid", device.getDevidX());
        param.put("qrytype", Params.QRY_TYPE.ALL);
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
                            SPUtils.getInstance().remove(Common.formatDevId2Mac(device.getDevidX()));
                            SPUtils.getInstance().saveObject(Common.formatDevId2Mac(device.getDevidX()),response);
                            initViewData(response.getUserinfo().getDevinfo());
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
    private void unbindDevice() {
        Map param = new HashMap();
        param.put("devid", device.getDevidX());
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
                .subscribe(new Consumer<DeviceResponse>() {
                    @Override
                    public void accept(DeviceResponse response) throws Exception {
                        if (response.isSuccessful()) {
                            ToastUtils.showShort("删除成功");
                            finish();
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
            if(intent.getAction().equals(IntentExtras.ACTION.ACTION_CMD_RESPONSE)){
                if(deviceDetailInfo==null){
                    return;
                }
                String address = intent.getStringExtra("address");
                int ret = intent.getIntExtra("ret", -1);//获取Extra信息
                switch (ret){
                    case IntentExtras.RET.RET_BLE_MODE_WORK_SUCCESS:
                        if(address.equals(Common.formatDevId2Mac(deviceDetailInfo.getDevid()))) {
                            LogUtils.d("RET_DEVICE_CONNECT_SUCCESS");
                            setConnectStatusStyle(address);
                            switchFindBtnStyle();
                        }
                        break;
                    case IntentExtras.RET.RET_BLE_MODE_WORK_FAIL:
                        if(address.equals(Common.formatDevId2Mac(deviceDetailInfo.getDevid()))) {
                            LogUtils.d("RET_DEVICE_CONNECT_FAILED");
                            setConnectStatusStyle(address);
                            switchFindBtnStyle();
                        }
                        break;
                    case IntentExtras.RET.RET_BLE_ALERT_STARTED:
                        if(address.equals(Common.formatDevId2Mac(deviceDetailInfo.getDevid()))) {
                            SPUtils.getInstance(SPConst.ALET_STATUE.SP_NAME).put(device.getDevidX(), SPConst.ALET_STATUE.STATUS_BELLING);
                            switchFindBtnStyle();
                        }
                        break;
                    case IntentExtras.RET.RET_BLE_ALERT_STOPED:
                        if(address.equals(Common.formatDevId2Mac(deviceDetailInfo.getDevid()))) {
                            SPUtils.getInstance(SPConst.ALET_STATUE.SP_NAME).put(device.getDevidX(), SPConst.ALET_STATUE.STATUS_UNBELL);
                            switchFindBtnStyle();
                        }
                        break;
                    case IntentExtras.RET.RET_BLE_READ_RSSI:
                        if(address.equals(Common.formatDevId2Mac(deviceDetailInfo.getDevid()))) {
                            LogUtils.d("RET_DEVICE_READ_RSSI");
                            int rssi = intent.getIntExtra("rssi",0);
                            setConnectStatusStyle(address,rssi);
                            updateItemRssi(rssi);
                        }
                        break;
                    case IntentExtras.RET.RET_BLE_READ_BATTERY:
                        if(address.equals(Common.formatDevId2Mac(deviceDetailInfo.getDevid()))) {
                            LogUtils.d("RET_DEVICE_READ_BATTERY");
                            String battery = intent.getStringExtra("battery");
                            updateItemBattery(battery);
                        }
                        break;
                    case IntentExtras.RET.RET_BLE_UNBIND_COMPLETE:
                        unbindDevice();
                        break;
                }

            }
        }
    }
    private void setConnectStatusStyle(String mac){
        setConnectStatusStyle(mac,-50);
    }

    private void setConnectStatusStyle(String mac,int rssi) {
        if(imgDeviceIcon==null){
            return;
        }
//        if(BleManager.getInstance().isConnected(mac)){
//            if(rssi>-50) {
//                imgDeviceIcon.setBorderColor(getResources().getColor(R.color.ic_connect1));
//            }else if(rssi<=-50&&rssi>-65){
//                imgDeviceIcon.setBorderColor(getResources().getColor(R.color.ic_connect2));
//            }else if(rssi<=-65&&rssi>-85){
//                imgDeviceIcon.setBorderColor(getResources().getColor(R.color.ic_connect3));
//            }else if(rssi<=-85&&rssi>-100){
//                imgDeviceIcon.setBorderColor(getResources().getColor(R.color.ic_connect4));
//            }else if(rssi<=-100){
//                imgDeviceIcon.setBorderColor(getResources().getColor(R.color.ic_connect5));
//            }
//        }else{
//            imgDeviceIcon.setBorderColor(getResources().getColor(R.color.windowBg));
//        }
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
        if(tvDeviceTime!=null){
            tvDeviceTime.setText("现在");
        }
    }

    private void updateItemBattery(String battery) {
        if(tvBattarry!=null&&Integer.valueOf(battery)<20) {
            tvBattarry.setVisibility(View.VISIBLE);
            tvBattarry.setText("剩余电量  " + battery + "%");
        }
        if(tvDeviceTime!=null){
            tvDeviceTime.setText("现在");
        }
    }
}
