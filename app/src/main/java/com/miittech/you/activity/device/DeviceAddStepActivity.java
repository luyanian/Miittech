package com.miittech.you.activity.device;

import android.Manifest;
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
import com.inuker.bluetooth.library.Code;
import com.inuker.bluetooth.library.Constants;
import com.inuker.bluetooth.library.connect.response.BleConnectResponse;
import com.inuker.bluetooth.library.connect.response.BleWriteResponse;
import com.inuker.bluetooth.library.model.BleGattProfile;
import com.inuker.bluetooth.library.search.SearchRequest;
import com.inuker.bluetooth.library.search.SearchResult;
import com.inuker.bluetooth.library.search.response.SearchResponse;
import com.luck.picture.lib.permissions.RxPermissions;
import com.miittech.you.App;
import com.miittech.you.R;
import com.miittech.you.activity.BaseActivity;
import com.miittech.you.ble.BLEManager;
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

    private List<String> mScanList = new ArrayList<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_add_step);
        ButterKnife.bind(this);
        initMyTitleBar(titlebar, R.string.text_logo);
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
        mScanList.clear();
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
                            SearchRequest request = new SearchRequest.Builder()
                                    .searchBluetoothLeDevice(10000, 3)   // 先扫BLE设备3次，每次3s
                                    .searchBluetoothLeDevice(5000)      // 再扫BLE设备2s
                                    .build();

                            BLEManager.getInstance().search(request, new SearchResponse() {
                                @Override
                                public void onSearchStarted() {
                                    LogUtils.d("onSearchStarted");
                                }

                                @Override
                                public void onDeviceFounded(final SearchResult device) {
                                    if(!device.getName().contains(DEVICE_NAME)){
                                        return;
                                    }

                                    if(mScanList.contains(device.getAddress())){
                                        return;
                                    }

                                    mScanList.add(device.getAddress());
                                    step1.setVisibility(View.GONE);
                                    step3.setVisibility(View.GONE);
                                    step2.setVisibility(View.VISIBLE);
                                    progressbar.setProgress(0);
                                    tvProgress.setText("正在激活");

                                    BLEManager.getInstance().connectDevice(device.getAddress(),new BleConnectResponse(){
                                        @Override
                                        public void onResponse(int code, BleGattProfile data) {
                                            progressbar.setProgress(13);
                                            if(code== Constants.REQUEST_SUCCESS){
                                                BLEManager.getInstance().setBindMode(device.getAddress(),new BleWriteResponse() {
                                                    @Override
                                                    public void onResponse(int code) {
                                                        if (code == Code.REQUEST_SUCCESS) {
                                                            progressbar.setProgress(27);
                                                            BLEManager.getInstance().setWorkMode(device.getAddress());
                                                            vertifyDevice(device.getAddress());
                                                        }
                                                    }
                                                });
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
                                    });
                                    LogUtils.v(String.format("device for %s\n%s", device.getAddress(), device.toString()));
                                }

                                @Override
                                public void onSearchStopped() {
                                    LogUtils.d("onSearchStopped");
                                }

                                @Override
                                public void onSearchCanceled() {
                                    LogUtils.d("onSearchCanceled");
                                }
                            });
                        }
                    }
                });
    }

    private void vertifyDevice(final String mac) {
        progressbar.setProgress(38);
        Map param = new HashMap();
        param.put("devid", Common.formatMac2DevId(mac));
        String json = new Gson().toJson(param);
        PubParam pubParam = new PubParam(App.getInstance().getUserId());
        String sign_unSha1 = pubParam.toValueString() + json + App.getInstance().getTocken();
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
                            progressbar.setProgress(69);
                            bindDevice(mac);
                        }else{
                            response.onError(DeviceAddStepActivity.this);
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

    private void bindDevice(final String mac) {
        progressbar.setProgress(81);
        Map param = new HashMap();
        param.put("devid", Common.formatMac2DevId(mac));
        param.put("method", Params.METHOD.BINGD);
        String json = new Gson().toJson(param);
        PubParam pubParam = new PubParam(App.getInstance().getUserId());
        String sign_unSha1 = pubParam.toValueString() + json + App.getInstance().getTocken();
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
                                    intent.putExtra(IntentExtras.DEVICE.ID,Common.formatMac2DevId(mac));
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
