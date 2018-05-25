package com.miittech.you.activity.device;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Gravity;
import android.view.SurfaceView;
import android.view.View;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.gson.Gson;
import com.miittech.you.App;
import com.miittech.you.R;
import com.miittech.you.activity.BaseActivity;
import com.miittech.you.activity.setting.WebViewActivity;
import com.miittech.you.global.HttpUrl;
import com.miittech.you.global.IntentExtras;
import com.miittech.you.global.Params;
import com.miittech.you.global.PubParam;
import com.miittech.you.impl.TitleBarOptions;
import com.miittech.you.net.ApiServiceManager;
import com.miittech.you.net.response.BaseResponse;
import com.miittech.you.net.response.DeviceListResponse;
import com.miittech.you.utils.Common;
import com.miittech.you.weight.CircleProgressBar;
import com.miittech.you.weight.SilkyAnimation;
import com.miittech.you.weight.Titlebar;
import com.ryon.mutils.EncryptUtils;
import com.ryon.mutils.LogUtils;
import com.ryon.mutils.NetworkUtils;
import com.ryon.mutils.ToastUtils;
import com.tbruyelle.rxpermissions2.RxPermissions;

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
 * Created by Administrator on 2017/10/17.
 */

public class DeviceAddStepActivity extends BaseActivity implements Handler.Callback {
    @BindView(R.id.step1)
    RelativeLayout step1;
    @BindView(R.id.step1_surfaceview)
    SurfaceView surfaceview;
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
    @BindView(R.id.btn_authority)
    RelativeLayout btnAuthority;
    @BindView(R.id.btn_next)
    TextView btnNext;
    @BindView(R.id.tv_authority)
    TextView tvAuthority;

    private Handler handler = new Handler(this);
    private CmdResponseReceiver cmdResponseReceiver = new CmdResponseReceiver();
    private boolean bindSuccessful = false;
    private String bindSuccessfulMac = "";
    private SilkyAnimation silkyAnimation;

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
        silkyAnimation = new SilkyAnimation.Builder(surfaceview)
                //设置常驻内存的缓存数量, 默认5.
                .setCacheCount(8)
                //设置帧间隔, 默认100
                .setFrameInterval(25)
                //设置缩放类型, 默认fit center，与ImageView的缩放模式通用
                .setScaleType(SilkyAnimation.SCALE_TYPE_CENTER_CROP)
                //设置动画开始结束状态监听
                .setAnimationListener(new SilkyAnimation.AnimationStateListener() {
                    @Override
                    public void onStart() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                surfaceview.setVisibility(View.VISIBLE);
                            }
                        });
                    }

                    @Override
                    public void onFinish() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                surfaceview.setVisibility(View.INVISIBLE);
                            }
                        });
                    }
                })
                //设置是否支持bitmap复用，默认为true
                .setSupportInBitmap(true)
                //设置循环模式, 默认不循环
                .setRepeatMode(SilkyAnimation.MODE_INFINITE)
                .build();
        startScan();
    }

    private void startScan() {
        if(!silkyAnimation.isDrawing()) {
            silkyAnimation.start("anim/guide");
        }
//        surfaceview.setZOrderMediaOverlay(false);
//        surfaceview.getHolder().setFormat();
        step2.setVisibility(View.GONE);
        step3.setVisibility(View.GONE);
        step1.setVisibility(View.VISIBLE);

        IntentFilter filter = new IntentFilter();
        filter.addAction(IntentExtras.ACTION.ACTION_CMD_RESPONSE);
        App.getInstance().getLocalBroadCastManager().registerReceiver(cmdResponseReceiver, filter);
        sacnDevice();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        App.getInstance().getLocalBroadCastManager().unregisterReceiver(cmdResponseReceiver);
    }

    private void sacnDevice() {
        RxPermissions rxPermissions = new RxPermissions(this);
        rxPermissions.request(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Boolean>() {
                    @Override
                    public void accept(Boolean aBoolean) throws Exception {
                        if (aBoolean) {
                            Intent intent = new Intent(IntentExtras.ACTION.ACTION_BLE_COMMAND);
                            intent.putExtra("cmd", IntentExtras.CMD.CMD_DEVICE_BIND_SCAN);
                            App.getInstance().getLocalBroadCastManager().sendBroadcast(intent);
                        }
                    }
                });
    }

    private void vertifyDevice(final String mac) {
        if (!NetworkUtils.isConnected()) {
            ToastUtils.showShort(R.string.msg_net_error);
            outBindError();
            Intent intent = new Intent(IntentExtras.ACTION.ACTION_BLE_COMMAND);
            intent.putExtra("cmd", IntentExtras.CMD.CMD_DEVICE_UNBIND_ERROR);
            intent.putExtra("address", mac);
            return;
        }
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
                .subscribe(new Consumer<DeviceListResponse>() {
                    @Override
                    public void accept(DeviceListResponse response) throws Exception {
                        progressbar.setProgress(24);
                        if (response.isVerSuccessful()) {
                            progressbar.setProgress(27);
                            Intent intent = new Intent(IntentExtras.ACTION.ACTION_BLE_COMMAND);
                            intent.putExtra("cmd", IntentExtras.CMD.CMD_DEVICE_CONNECT_BIND);
                            intent.putExtra("address", mac);
                            App.getInstance().getLocalBroadCastManager().sendBroadcast(intent);
                        } else {
                            response.onVerError();
                            Intent intent = new Intent(IntentExtras.ACTION.ACTION_BLE_COMMAND);
                            intent.putExtra("cmd", IntentExtras.CMD.CMD_DEVICE_UNBIND_ERROR);
                            intent.putExtra("address", mac);
                            App.getInstance().getLocalBroadCastManager().sendBroadcast(intent);
                            outBindError();
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        outBindError();
                        Intent intent = new Intent(IntentExtras.ACTION.ACTION_BLE_COMMAND);
                        intent.putExtra("cmd", IntentExtras.CMD.CMD_DEVICE_UNBIND_ERROR);
                        intent.putExtra("address", mac);
                        App.getInstance().getLocalBroadCastManager().sendBroadcast(intent);
                    }
                });
    }

    private void outBindError() {
        step1.setVisibility(View.GONE);
        step2.setVisibility(View.GONE);
        step3.setVisibility(View.VISIBLE);
        btnAuthority.setVisibility(View.VISIBLE);
        tvAuthority.setText("重新绑定");
        btnNext.setVisibility(View.GONE);

        imgConnectStatus.setImageResource(R.drawable.ic_device_connect_faild);
        tvConnectStatus.setText("绑定失败");
        tvConnectMsg.setVisibility(View.VISIBLE);
        tvConnectMsg.setGravity(Gravity.CENTER);
        tvConnectMsg.setText("您可以:\n关闭再打开手机蓝牙\n将手机贴近防丢器（10cm以内距离）");
    }

    private void onBindSuccessful(String mac) {
        bindSuccessfulMac = mac;
        bindSuccessful = true;
        btnAuthority.setVisibility(View.VISIBLE);
        tvAuthority.setText("防丢贴片权限设置");
        btnNext.setVisibility(View.VISIBLE);
    }

    private void bindDevice(final String mac) {
        if (!NetworkUtils.isConnected()) {
            ToastUtils.showShort(R.string.msg_net_error);
            outBindError();
            Intent intent = new Intent(IntentExtras.ACTION.ACTION_BLE_COMMAND);
            intent.putExtra("cmd", IntentExtras.CMD.CMD_DEVICE_UNBIND_ERROR);
            intent.putExtra("address", mac);
            return;
        }
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
        ApiServiceManager.getInstance().buildApiService(this).postNetRequest(path, requestBody)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<BaseResponse>() {
                    @Override
                    public void accept(BaseResponse response) throws Exception {
                        progressbar.setProgress(85);
                        if (response.isBindSuccessful()) {
                            progressbar.setProgress(100);
                            step1.setVisibility(View.GONE);
                            step2.setVisibility(View.GONE);
                            step3.setVisibility(View.VISIBLE);
                            imgConnectStatus.setImageResource(R.drawable.ic_device_connect_success);
                            tvConnectStatus.setText("绑定成功");
                            tvConnectMsg.setVisibility(View.GONE);
                            Common.doCommitEvents(DeviceAddStepActivity.this, response.getDevid(), Params.EVENT_TYPE.DEVICE_ADD);
                            Common.initDeviceList(DeviceAddStepActivity.this, null);
                            onBindSuccessful(mac);
                        } else {
                            Intent intent = new Intent(IntentExtras.ACTION.ACTION_BLE_COMMAND);
                            intent.putExtra("cmd", IntentExtras.CMD.CMD_DEVICE_UNBIND_ERROR);
                            intent.putExtra("address", mac);
                            App.getInstance().getLocalBroadCastManager().sendBroadcast(intent);
                            outBindError();
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

    @OnClick({R.id.btn_authority, R.id.btn_next})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.btn_authority:
                if(bindSuccessful) {
                    Intent urlIntent = new Intent(DeviceAddStepActivity.this, WebViewActivity.class);
                    urlIntent.putExtra("url", WebViewActivity.authority);
                    urlIntent.putExtra("title", "权限设置");
                    startActivity(urlIntent);
                }else{
                    startScan();
                }
                break;
            case R.id.btn_next:
                Intent intent = new Intent(DeviceAddStepActivity.this, DeviceSetClassifyActivity.class);
                intent.putExtra(IntentExtras.DEVICE.ID, Common.formatMac2DevId(bindSuccessfulMac));
                startActivity(intent);
                break;
        }
    }

    private class CmdResponseReceiver extends BroadcastReceiver {
        private String bindMac;

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(IntentExtras.ACTION.ACTION_CMD_RESPONSE)) {
                int ret = intent.getIntExtra("ret", -1);
                String address = intent.getStringExtra("address");
                switch (ret) {
                    case IntentExtras.RET.RET_BLE_FIND_BIND_DEVICE:
                        LogUtils.d("RET_BLE_FIND_BIND_DEVICE");
                        bindMac = address;
                        silkyAnimation.stop();
                        step1.setVisibility(View.GONE);
                        step3.setVisibility(View.GONE);
                        step2.setVisibility(View.VISIBLE);
                        progressbar.setProgress(0);
                        tvProgress.setText("正在激活");
                        vertifyDevice(address);
                        break;
                    case IntentExtras.RET.RET_BLE_CONNECT_START:
                        if (address.equals(bindMac)) {
                            LogUtils.d("RET_BLE_CONNECT_START");
                            progressbar.setProgress(31);
                        }
                        break;
                    case IntentExtras.RET.RET_BLE_CONNECT_FAILED:
                        if (address.equals(bindMac)) {
                            LogUtils.d("RET_BLE_CONNECT_FAILED");
                            handler.sendEmptyMessageDelayed(0, 3000);
                        }
                        break;
                    case IntentExtras.RET.RET_BLE_CONNECT_SUCCESS:
                        if (address.equals(bindMac)) {
                            LogUtils.d("RET_BLE_CONNECT_SUCCESS");
                            progressbar.setProgress(46);
                        }
                        break;
                    case IntentExtras.RET.RET_BLE_MODE_BIND_SUCCESS:
                        if (address.equals(bindMac)) {
                            LogUtils.d("RET_BLE_MODE_BIND_SUCCESS");
                            handler.removeCallbacksAndMessages(null);
                            progressbar.setProgress(59);
                        }
                    case IntentExtras.RET.RET_BLE_MODE_BIND_NOTIFY_SUCCESS:
                        if (address.equals(bindMac)) {
                            LogUtils.d("RET_BLE_MODE_BIND_SUCCESS");
                            handler.removeCallbacksAndMessages(null);
                            progressbar.setProgress(61);
                            bindDevice(address);
                        }
                        break;
                    case IntentExtras.RET.RET_BLE_MODE_BIND_FAIL:
                        if (address.equals(bindMac)) {
                            LogUtils.d("RET_BLE_MODE_BIND_FAIL");
                            handler.sendEmptyMessageDelayed(0, 3000);
                        }
                        break;
                }
            }
        }
    }
}
