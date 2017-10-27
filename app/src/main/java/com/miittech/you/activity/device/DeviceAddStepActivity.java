package com.miittech.you.activity.device;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.clj.fastble.BleManager;
import com.clj.fastble.conn.BleGattCallback;
import com.clj.fastble.data.ScanResult;
import com.clj.fastble.exception.BleException;
import com.google.gson.Gson;
import com.luck.picture.lib.permissions.RxPermissions;
import com.miittech.you.App;
import com.miittech.you.R;
import com.miittech.you.activity.BaseActivity;
import com.miittech.you.common.Common;
import com.miittech.you.global.IntentExtras;
import com.miittech.you.impl.TitleBarOptions;
import com.miittech.you.net.ApiServiceManager;
import com.miittech.you.net.global.HttpUrl;
import com.miittech.you.net.global.Params;
import com.miittech.you.net.global.PubParam;
import com.miittech.you.net.response.DeviceResponse;
import com.miittech.you.service.BluetoothService;
import com.miittech.you.weight.CircleProgressBar;
import com.miittech.you.weight.Titlebar;
import com.ryon.mutils.EncryptUtils;
import com.ryon.mutils.LogUtils;

import java.util.HashMap;
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

public class DeviceAddStepActivity extends BaseActivity{
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

    private static final String DEVICE_NAME = "yoowoo";
    private static final long TIME_OUT = 30000;

    private BleManager bleManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_add_step);
        ButterKnife.bind(this);
        initTitleBar(titlebar, R.string.text_logo);
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
        bleManager = new BleManager(this);
        sacnDevice();
    }

    private void sacnDevice() {
        RxPermissions rxPermissions = new RxPermissions(this);
        rxPermissions.request(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new Consumer<Boolean>() {
                @Override
                public void accept(Boolean aBoolean) throws Exception {
                if(aBoolean) {
                    bleManager.scanNameAndConnect(DEVICE_NAME, TIME_OUT, false, new BleGattCallback() {

                        @Override
                        public void onFoundDevice(final ScanResult scanResult) {
                            step1.setVisibility(View.GONE);
                            step3.setVisibility(View.GONE);
                            step2.setVisibility(View.VISIBLE);
                            progressbar.setProgress(0);
                            tvProgress.setText("正在激活");
                            vertifyDevice(scanResult);
                        }

                        @Override
                        public void onConnectError(BleException exception) {

                        }

                        @Override
                        public void onConnectSuccess(BluetoothGatt gatt, int status) {

                        }

                        @Override
                        public void onDisConnected(BluetoothGatt gatt, int status, BleException exception) {

                        }
                    });
                }
                }
            });
    }

    private void vertifyDevice(final ScanResult scanResult) {
        progressbar.setProgress(20);
        Map param = new HashMap();
        param.put("devid", Common.getMac(scanResult.getDevice().getAddress()));
        String json = new Gson().toJson(param);
        PubParam pubParam = new PubParam(App.getUserId());
        String sign_unSha1 = pubParam.toValueString() + json + App.getTocken();
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
                        progressbar.setProgress(40);
                        if(response.isVerSuccessful()){
                            connectDevice(scanResult);
                        }else{
                            bleManager.cancelScan();
                            response.onError(DeviceAddStepActivity.this);
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        bleManager.cancelScan();
                        step1.setVisibility(View.GONE);
                        step2.setVisibility(View.GONE);
                        step3.setVisibility(View.VISIBLE);
                        imgConnectStatus.setImageResource(R.drawable.ic_device_connect_faild);
                        tvConnectStatus.setText("绑定失败");
                        tvConnectMsg.setVisibility(View.VISIBLE);
                        tvConnectMsg.setText("请重新绑定");
                    }
                });
    }

    private void connectDevice(ScanResult scanResult) {
        bleManager.connectDevice(scanResult, true, new BleGattCallback() {
            @Override
            public void onConnectError(BleException exception) {
                step1.setVisibility(View.GONE);
                step2.setVisibility(View.GONE);
                step3.setVisibility(View.VISIBLE);
                imgConnectStatus.setImageResource(R.drawable.ic_device_connect_faild);
                tvConnectStatus.setText("绑定失败");
                tvConnectMsg.setVisibility(View.VISIBLE);
                tvConnectMsg.setText("没有找到设备，请重新绑定");
            }

            @Override
            public void onConnectSuccess(BluetoothGatt gatt, int status) {
                bindDevice(gatt);
            }

            @Override
            public void onDisConnected(BluetoothGatt gatt, int status, BleException exception) {

            }
        });
    }

    private void bindDevice(final BluetoothGatt bluetoothGatt) {
        progressbar.setProgress(60);
        Map param = new HashMap();
        param.put("devid", Common.getMac(bluetoothGatt.getDevice().getAddress()));
        param.put("method", Params.METHOD.BINGD);
        String json = new Gson().toJson(param);
        PubParam pubParam = new PubParam(App.getUserId());
        String sign_unSha1 = pubParam.toValueString() + json + App.getTocken();
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
                        progressbar.setProgress(96);
                        if(response.isBindSuccessful()){
                            progressbar.setProgress(100);
                            step1.setVisibility(View.GONE);
                            step2.setVisibility(View.GONE);
                            step3.setVisibility(View.VISIBLE);
                            imgConnectStatus.setImageResource(R.drawable.ic_device_connect_success);
                            tvConnectStatus.setText("绑定成功");
                            tvConnectMsg.setVisibility(View.GONE);
                            Common.doCommitEvents(DeviceAddStepActivity.this,response.getDevid(),Common.getCurrentTime(),Params.EVENT_TYPE.DEVICE_ADD,null,null);
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    Intent intent = new Intent(DeviceAddStepActivity.this,DeviceSetClassifyActivity.class);
                                    intent.putExtra(IntentExtras.DEVICE.ID,Common.getMac(bluetoothGatt.getDevice().getAddress()));
                                    startActivity(intent);
                                }
                            },2000);
                        }else{
                            step1.setVisibility(View.GONE);
                            step2.setVisibility(View.GONE);
                            step3.setVisibility(View.VISIBLE);
                            imgConnectStatus.setImageResource(R.drawable.ic_device_connect_faild);
                            tvConnectStatus.setText("绑定失败");
                            tvConnectMsg.setVisibility(View.VISIBLE);
                            tvConnectMsg.setText("请重新绑定");
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        step1.setVisibility(View.GONE);
                        step2.setVisibility(View.GONE);
                        step3.setVisibility(View.VISIBLE);
                        imgConnectStatus.setImageResource(R.drawable.ic_device_connect_faild);
                        tvConnectStatus.setText("绑定失败");
                        tvConnectMsg.setVisibility(View.VISIBLE);
                        tvConnectMsg.setText("请重新绑定");
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
