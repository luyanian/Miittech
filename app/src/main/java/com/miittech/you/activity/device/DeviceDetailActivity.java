package com.miittech.you.activity.device;

import android.content.Intent;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.gson.Gson;
import com.miittech.you.App;
import com.miittech.you.R;
import com.miittech.you.activity.BaseActivity;
import com.miittech.you.common.Common;
import com.miittech.you.global.IntentExtras;
import com.miittech.you.impl.TitleBarOptions;
import com.miittech.you.impl.TypeSelectorChangeLisener;
import com.miittech.you.net.ApiServiceManager;
import com.miittech.you.global.HttpUrl;
import com.miittech.you.global.Params;
import com.miittech.you.global.PubParam;
import com.miittech.you.net.response.DeviceResponse;
import com.miittech.you.weight.CircleImageView;
import com.miittech.you.weight.Titlebar;
import com.miittech.you.weight.TypeSelector;
import com.ryon.mutils.EncryptUtils;
import com.ryon.mutils.LogUtils;
import com.ryon.mutils.StringUtils;
import com.ryon.mutils.ToastUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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
    @BindView(R.id.img_find_or_bell)
    ImageView imgFindOrBell;
    @BindView(R.id.typeSelector)
    TypeSelector typeSelector;
    @BindView(R.id.ll_tips)
    LinearLayout llTips;
    @BindView(R.id.ll_options)
    LinearLayout llOptions;
    @BindView(R.id.tv_device_location)
    TextView tvDeviceLocation;
    @BindView(R.id.tv_device_time)
    TextView tvDeviceTime;
    private DeviceResponse.DevlistBean device;
//    private BleManager bleManager;

    public final static UUID serviceUUID= UUID.fromString("00001802-0000-1000-8000-00805f9b34fb");
    public final static UUID characteristicUUID= UUID.fromString("00002a06-0000-1000-80000-00805f9b34fb");
    public final static UUID descriptorUUID = UUID.fromString("00002a06-0000-1000-80000-00805f9b34fb");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_detail);
        ButterKnife.bind(this);
//        bleManager = new BleManager(this);
        initTitleBar(titlebar, "设备");
        titlebar.showBackOption();
        titlebar.setTitleBarOptions(new TitleBarOptions() {
            @Override
            public void onBack() {
                super.onBack();
                finish();
            }
        });

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
        initViewData(device);
    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
    }

    private void initViewData(DeviceResponse.DevlistBean device) {
        tvDeviceName.setText(device.getDevname());
        tvDeviceLocation.setText(device.getLocinfo().getAddr());
        tvDeviceTime.setText(device.getLasttime());

        Glide.with(this)
                .load(device.getDevimg())
                .into(imgDeviceIcon);
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

    @OnClick({R.id.rl_find_or_bell, R.id.btn_option_map, R.id.btn_option_share, R.id.btn_option_setting, R.id.btn_option_delete})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.rl_find_or_bell:
                doFindOrBell();
                break;
            case R.id.btn_option_map:
                break;
            case R.id.btn_option_share:
                break;
            case R.id.btn_option_setting:
                break;
            case R.id.btn_option_delete:
                unbindDevice();
                break;
        }
    }

    private void doFindOrBell() {
//        if(bleManager==null||!bleManager.isBlueEnable()||!bleManager.isConnected()){
//            return;
//        }
//        try {
//            bleManager.writeDevice(
//                    "1802",
//                    "2A06",
//                    HexUtil.hexStringToBytes("0x01"),
//                    new BleCharacterCallback() {
//                        @Override
//                        public void onSuccess(BluetoothGattCharacteristic characteristic) {
//                            ToastUtils.showShort(characteristic.getDescriptors().toString());
//                        }
//
//                        @Override
//                        public void onFailure(BleException exception) {
//                            ToastUtils.showShort(exception.getDescription());
//                        }
//
//                        @Override
//                        public void onInitiatedResult(boolean result) {
//                            ToastUtils.showShort(String.valueOf(result));
//                        }
//                    });
//        }catch (Exception e){
//            e.printStackTrace();
//        }


    }

    private void unbindDevice() {
        Map param = new HashMap();
        param.put("devid", device.getDevidX());
        param.put("method", Params.METHOD.UNBIND);
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
                        if (response.isSuccessful()) {
                            ToastUtils.showShort("删除成功！");
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
}
