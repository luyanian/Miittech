package com.miittech.you.activity.device;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.google.gson.Gson;
import com.luck.picture.lib.permissions.RxPermissions;
import com.miittech.you.App;
import com.miittech.you.R;
import com.miittech.you.activity.BaseActivity;
import com.miittech.you.common.BleCommon;
import com.miittech.you.common.Common;
import com.miittech.you.global.IntentExtras;
import com.miittech.you.impl.TitleBarOptions;
import com.miittech.you.net.ApiServiceManager;
import com.miittech.you.global.HttpUrl;
import com.miittech.you.global.Params;
import com.miittech.you.global.PubParam;
import com.miittech.you.net.response.DeviceResponse;
import com.miittech.you.weight.CircleProgressBar;
import com.miittech.you.weight.Titlebar;
import com.ryon.mutils.EncryptUtils;
import com.ryon.mutils.LogUtils;
import com.vise.baseble.ViseBle;
import com.vise.baseble.callback.IBleCallback;
import com.vise.baseble.callback.IConnectCallback;
import com.vise.baseble.callback.scan.IScanCallback;
import com.vise.baseble.callback.scan.SingleFilterScanCallback;
import com.vise.baseble.common.PropertyType;
import com.vise.baseble.core.BluetoothGattChannel;
import com.vise.baseble.core.DeviceMirror;
import com.vise.baseble.exception.BleException;
import com.vise.baseble.model.BluetoothLeDevice;
import com.vise.baseble.model.BluetoothLeDeviceStore;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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
                        if (aBoolean) {
                            //该方式是扫到指定设备就停止扫描
                            ViseBle.getInstance().startScan(new SingleFilterScanCallback(new IScanCallback() {
                                @Override
                                public void onDeviceFound(BluetoothLeDeviceStore bluetoothLeDeviceStore) {
                                    step1.setVisibility(View.GONE);
                                    step3.setVisibility(View.GONE);
                                    step2.setVisibility(View.VISIBLE);
                                    progressbar.setProgress(0);
                                    tvProgress.setText("正在激活");
                                }

                                @Override
                                public void onScanFinish(BluetoothLeDeviceStore bluetoothLeDeviceStore) {
                                    ViseBle.getInstance().connect(bluetoothLeDeviceStore.getDeviceList().get(0), new IConnectCallback() {
                                        @Override
                                        public void onConnectSuccess(DeviceMirror deviceMirror) {
                                            BluetoothGattChannel bluetoothGattChannel = new BluetoothGattChannel.Builder()
                                                    .setBluetoothGatt(deviceMirror.getBluetoothGatt())
                                                    .setPropertyType(PropertyType.PROPERTY_WRITE)
                                                    .setServiceUUID(UUID.fromString(BleCommon.userServiceUUID))
                                                    .setCharacteristicUUID(UUID.fromString(BleCommon.userCharacteristicLogUUID))
                                                    .builder();
                                            deviceMirror.bindChannel(new IBleCallback() {
                                                @Override
                                                public void onSuccess(byte[] data, BluetoothGattChannel bluetoothGattChannel, BluetoothLeDevice bluetoothLeDevice) {
                                                    vertifyDevice(bluetoothLeDevice.getDevice());
                                                }

                                                @Override
                                                public void onFailure(BleException exception) {
                                                    LogUtils.e(exception);
                                                }
                                            }, bluetoothGattChannel);
                                            byte[] data = Common.formatBleMsg(Params.BLEMODE.MODE_BIND,App.getUserId());
                                            deviceMirror.writeData(data);

                                        }

                                        @Override
                                        public void onConnectFailure(BleException exception) {
                                            step1.setVisibility(View.GONE);
                                            step2.setVisibility(View.GONE);
                                            step3.setVisibility(View.VISIBLE);
                                            imgConnectStatus.setImageResource(R.drawable.ic_device_connect_faild);
                                            tvConnectStatus.setText("绑定失败");
                                            tvConnectMsg.setVisibility(View.VISIBLE);
                                            tvConnectMsg.setText("没有找到设备，请重新绑定");
                                        }

                                        @Override
                                        public void onDisconnect(boolean isActive) {
                                            LogUtils.d(isActive);
                                        }
                                    });
                                }

                                @Override
                                public void onScanTimeout() {

                                }
                            }).setDeviceName(DEVICE_NAME));


                        }
                    }
                });
    }

    private void vertifyDevice(final BluetoothDevice device) {
        progressbar.setProgress(20);
        Map param = new HashMap();
        param.put("devid", Common.formatMac2DevId(device.getAddress()));
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
                            bindDevice(device);
                        }else{
                            response.onError(DeviceAddStepActivity.this);
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
//                        bleManager.cancelScan();
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

    private void bindDevice(final BluetoothDevice device) {
        progressbar.setProgress(60);
        Map param = new HashMap();
        param.put("devid", Common.formatMac2DevId(device.getAddress()));
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
                                    intent.putExtra(IntentExtras.DEVICE.ID,Common.formatMac2DevId(device.getAddress()));

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
//    private void setLocationService() {
//        Intent locationIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
//        this.startActivityForResult(locationIntent, REQUEST_CODE_LOCATION_SETTINGS);
//    }
//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        if (requestCode == REQUEST_CODE_LOCATION_SETTINGS) {
//            if (isLocationEnable(this)) {
//                //定位已打开的处理
//            } else {
//                //定位依然没有打开的处理
//            }
//        } else super.onActivityResult(requestCode, resultCode, data);
//    }
}
