package com.miittech.you.activity.device;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;

import com.google.gson.Gson;
import com.miittech.you.R;
import com.miittech.you.activity.BaseActivity;
import com.miittech.you.utils.Common;
import com.miittech.you.global.IntentExtras;
import com.miittech.you.impl.OnNetRequestCallBack;
import com.miittech.you.impl.TitleBarOptions;
import com.miittech.you.net.ApiServiceManager;
import com.miittech.you.global.HttpUrl;
import com.miittech.you.global.PubParam;
import com.miittech.you.net.response.DeviceListResponse;
import com.miittech.you.weight.Titlebar;
import com.ryon.mutils.EncryptUtils;
import com.ryon.mutils.LogUtils;
import com.ryon.mutils.NetworkUtils;
import com.ryon.mutils.StringUtils;
import com.ryon.mutils.ToastUtils;

import java.util.LinkedHashMap;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import okhttp3.MediaType;
import okhttp3.RequestBody;

/**
 * Created by Administrator on 2017/10/20.
 */

public class DeviceEditNameActivity extends BaseActivity {

    @BindView(R.id.titlebar)
    Titlebar titlebar;
    @BindView(R.id.et_name)
    EditText etName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_edit_name);
        ButterKnife.bind(this);
        initMyTitleBar(titlebar,"设置分类");
        titlebar.showBackOption();
        titlebar.showCompleteOption("保存");
        titlebar.setTitleBarOptions(new TitleBarOptions(){
            @Override
            public void onBack() {
                super.onBack();
                finish();
            }

            @Override
            public void onComplete() {
                super.onComplete();
                doDeviceEditAttr();
            }
        });

    }

    private void doDeviceEditAttr() {
        final String devId = getIntent().getStringExtra(IntentExtras.DEVICE.ID);
        final String devName = etName.getText().toString().trim();
        if(StringUtils.isEmpty(devName)){
            ToastUtils.showShort("设备名称不能为空！");
            return;
        }
        if(!NetworkUtils.isConnected()){
            ToastUtils.showShort(R.string.msg_net_error);
            return;
        }
        Map devattrMap = new LinkedHashMap();
        devattrMap.put("devname", Common.encodeBase64(devName));
        Map param = new LinkedHashMap();
        param.put("devid", devId);
        param.put("method", "A");
        param.put("devattr", devattrMap);
        String json = new Gson().toJson(param);
        PubParam pubParam = new PubParam(Common.getUserId());
        String sign_unSha1 = pubParam.toValueString() + json + Common.getTocken();
        LogUtils.d("sign_unsha1", sign_unSha1);
        String sign = EncryptUtils.encryptSHA1ToString(sign_unSha1).toLowerCase();
        LogUtils.d("sign_sha1", sign);
        String path = HttpUrl.Api + "deviceattr/" + pubParam.toUrlParam(sign);
        final RequestBody requestBody = RequestBody.create(MediaType.parse(HttpUrl.MediaType_Json), json);

        ApiServiceManager.getInstance().buildApiService(this).postDeviceOption(path, requestBody)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<DeviceListResponse>() {
                    @Override
                    public void accept(DeviceListResponse response) throws Exception {
                        if (response.isSuccessful()) {
                            Common.initDeviceList(DeviceEditNameActivity.this,null);
                            Common.getDeviceDetailInfo(DeviceEditNameActivity.this, devId, new OnNetRequestCallBack() {
                                @Override
                                public void OnRequestComplete() {
                                    Intent data = new Intent();
                                    data.putExtra(IntentExtras.DEVICE.NAME,devName);
                                    setResult(RESULT_OK,data);
                                    finish();
                                }
                            });
                        }else {
                            response.onError(DeviceEditNameActivity.this);
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
