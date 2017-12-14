package com.miittech.you.activity.device;

import android.Manifest;
import android.bluetooth.BluetoothGatt;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.clj.fastble.BleManager;
import com.clj.fastble.callback.BleScanAndConnectCallback;
import com.clj.fastble.data.BleDevice;
import com.clj.fastble.exception.BleException;
import com.clj.fastble.scan.BleScanRuleConfig;
import com.google.gson.Gson;
import com.luck.picture.lib.permissions.RxPermissions;
import com.miittech.you.App;
import com.miittech.you.R;
import com.miittech.you.activity.BaseActivity;
import com.miittech.you.common.Common;
import com.miittech.you.global.IntentExtras;
import com.miittech.you.impl.TitleBarOptions;
import com.miittech.you.net.ApiServiceManager;
import com.miittech.you.global.HttpUrl;
import com.miittech.you.global.Params;
import com.miittech.you.global.PubParam;
import com.miittech.you.net.response.DeviceResponse;
import com.miittech.you.service.BleService;
import com.miittech.you.weight.CircleProgressBar;
import com.miittech.you.weight.Titlebar;
import com.ryon.mutils.EncryptUtils;
import com.ryon.mutils.LogUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import okhttp3.MediaType;
import okhttp3.RequestBody;

/**
 * Created by Administrator on 2017/10/17.
 */

public class DeviceAddStepActivity extends BaseActivity implements Handler.Callback{
    @BindView(R.id.step1)
    TextView step1;
    @BindView(R.id.progressbar)
    CircleProgressBar progressbar;
    @BindView(R.id.tv_progress)
    TextView tvProgress;
    @BindView(R.id.step2)
    RelativeLayout step2;
    @BindView(R.id.titlebar)
    Titlebar titlebar;
    @BindView(R.id.img_connect_status)
    ImageView imgConnectStatus;
    @BindView(R.id.tv_connect_status)
    TextView tvConnectStatus;
    @BindView(R.id.tv_connect_msg)
    TextView tvConnectMsg;
    @BindView(R.id.step3)
    RelativeLayout step3;
    private Handler handler = new Handler(this);
    private CmdResponseReceiver cmdResponseReceiver = new CmdResponseReceiver();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_add_step);
        ButterKnife.bind(this);
        initMyTitleBar(titlebar);
        titlebar.showBackOption();
        titlebar.setTitleBarOptions(new TitleBarOptions() {
            @Override
            public void onBack() {
                super.onBack();
                finish();
            }
        });
        step2.setVisibility(View.GONE);
        step3.setVisibility(View.GONE);
        step1.setVisibility(View.VISIBLE);

        IntentFilter filter=new IntentFilter();
        filter.addAction(IntentExtras.ACTION.ACTION_CMD_RESPONSE);
        this.registerReceiver(cmdResponseReceiver,filter);
        sacnDevice();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(cmdResponseReceiver);
    }

    private void sacnDevice() {
        RxPermissions rxPermissions = new RxPermissions(this);
        rxPermissions.request(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Boolean>() {
                    @Override
                    public void accept(Boolean aBoolean) throws Exception {
                        if (aBoolean) {
                            Intent intent= new Intent(IntentExtras.ACTION.ACTION_BLE_COMMAND);
                            intent.putExtra("cmd",IntentExtras.CMD.CMD_DEVICE_BIND_SCAN);
                            sendBroadcast(intent);
                        }
                    }
                });
    }

    private void vertifyDevice(final String mac) {
        progressbar.setProgress(12);
        Map param = new HashMap();
        param.put("devid", Common.formatMac2DevId(mac));
        String json = new Gson().toJson(param);
        PubParam pubParam = new PubParam(Common.getUserId());
        String sign_unSha1 = pubParam.toValueString() + json + Common.getTocken();
        LogUtils.d("sign_unsha1", sign_unSha1);
        String sign = EncryptUtils.encryptSHA1ToString(sign_unSha1).toLowerCase();
        LogUtils.d("sign_sha1", sign);
        String path = HttpUrl.Api + "devicevalid/" + pubParam.toUrlParam(sign);
        final RequestBody requestBody = RequestBody.create(MediaType.parse(HttpUrl.MediaType_Json), json);
        ApiServiceManager.getInstance().buildApiService(this).postDeviceOption(path, requestBody)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<DeviceResponse>() {
                    @Override
                    public void accept(DeviceResponse response) throws Exception {
                        progressbar.setProgress(24);
                        if(response.isVerSuccessful()){
                            progressbar.setProgress(27);
                            Intent intent= new Intent(IntentExtras.ACTION.ACTION_BLE_COMMAND);
                            intent.putExtra("cmd",IntentExtras.CMD.CMD_DEVICE_CONNECT_BIND);
                            intent.putExtra("address",mac);
                            sendBroadcast(intent);
                        }else{
                            response.onVerError();
                            Intent intent= new Intent(IntentExtras.ACTION.ACTION_BLE_COMMAND);
                            intent.putExtra("cmd",IntentExtras.CMD.CMD_DEVICE_UNBIND_ERROR);
                            intent.putExtra("address",mac);
                            sendBroadcast(intent);
                            outBindError();
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        outBindError();
                        Intent intent= new Intent(IntentExtras.ACTION.ACTION_BLE_COMMAND);
                        intent.putExtra("cmd",IntentExtras.CMD.CMD_DEVICE_UNBIND);
                        intent.putExtra("address",mac);
                        sendBroadcast(intent);
                    }
                });
    }

    private void outBindError() {
        step1.setVisibility(View.GONE);
        step2.setVisibility(View.GONE);
        step3.setVisibility(View.VISIBLE);
        imgConnectStatus.setImageResource(R.drawable.ic_device_connect_faild);
        tvConnectStatus.setText("绑定失败");
        tvConnectMsg.setVisibility(View.VISIBLE);
        tvConnectMsg.setText("请重新绑定");
    }

    private void bindDevice(final String mac) {
        progressbar.setProgress(71);
        Map param = new HashMap();
        param.put("devid", Common.formatMac2DevId(mac));
        param.put("method", Params.METHOD.BINGD);
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
                        progressbar.setProgress(85);
                        if(response.isBindSuccessful()){
                            progressbar.setProgress(100);
                            step1.setVisibility(View.GONE);
                            step2.setVisibility(View.GONE);
                            step3.setVisibility(View.VISIBLE);
                            imgConnectStatus.setImageResource(R.drawable.ic_device_connect_success);
                            tvConnectStatus.setText("绑定成功");
                            tvConnectMsg.setVisibility(View.GONE);
                            Common.doCommitEvents(DeviceAddStepActivity.this,response.getDevid(),Params.EVENT_TYPE.DEVICE_ADD,null);
                            Intent intent = new Intent(DeviceAddStepActivity.this,DeviceSetClassifyActivity.class);
                            intent.putExtra(IntentExtras.DEVICE.ID,Common.formatMac2DevId(mac));
                            startActivity(intent);
                        }else{
                            outBindError();
                            Intent intent= new Intent(IntentExtras.ACTION.ACTION_BLE_COMMAND);
                            intent.putExtra("cmd",IntentExtras.CMD.CMD_DEVICE_UNBIND_ERROR);
                            intent.putExtra("address",mac);
                            sendBroadcast(intent);
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        outBindError();
                    }
                });
    }

    /**
     * Location service if enable
     *
     * @param context
     * @return location is enable if return true, otherwise disable.
     */
    public static final boolean isLocationEnable(Context context) {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        boolean networkProvider = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        boolean gpsProvider = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if (networkProvider || gpsProvider) return true;
        return false;
    }

    @Override
    public boolean handleMessage(Message msg) {
        outBindError();
        return false;
    }

    private class CmdResponseReceiver extends BroadcastReceiver {
        private String bindMac;
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(IntentExtras.ACTION.ACTION_CMD_RESPONSE)){
                int ret = intent.getIntExtra("ret", -1);
                String address = intent.getStringExtra("address");
                switch (ret){
                    case IntentExtras.RET.RET_BLE_FIND_BIND_DEVICE:
                        LogUtils.d("RET_BLE_FIND_BIND_DEVICE");
                        bindMac = address;
                        step1.setVisibility(View.GONE);
                        step3.setVisibility(View.GONE);
                        step2.setVisibility(View.VISIBLE);
                        progressbar.setProgress(0);
                        tvProgress.setText("正在激活");
                        vertifyDevice(address);
                        break;
                    case IntentExtras.RET.RET_BLE_CONNECT_START:
                        if(address.equals(bindMac)) {
                            LogUtils.d("RET_BLE_CONNECT_START");
                            progressbar.setProgress(31);
                        }
                        break;
                    case IntentExtras.RET.RET_BLE_CONNECT_FAILED:
                        if(address.equals(bindMac)) {
                            LogUtils.d("RET_BLE_CONNECT_FAILED");
                            handler.sendEmptyMessageDelayed(0,3000);
                        }
                        break;
                    case IntentExtras.RET.RET_BLE_CONNECT_SUCCESS:
                        if(address.equals(bindMac)) {
                            LogUtils.d("RET_BLE_CONNECT_SUCCESS");
                            progressbar.setProgress(46);
                        }
                        break;
                    case IntentExtras.RET.RET_BLE_MODE_BIND_SUCCESS:
                        if(address.equals(bindMac)) {
                            LogUtils.d("RET_BLE_MODE_BIND_SUCCESS");
                            handler.removeCallbacksAndMessages(null);
                            progressbar.setProgress(59);
                            bindDevice(address);
                        }
                        break;
                    case IntentExtras.RET.RET_BLE_MODE_BIND_FAIL:
                        if(address.equals(bindMac)) {
                            LogUtils.d("RET_BLE_MODE_BIND_FAIL");
                            handler.sendEmptyMessageDelayed(0,3000);
                        }
                        break;
                }

            }
        }
    }
}
