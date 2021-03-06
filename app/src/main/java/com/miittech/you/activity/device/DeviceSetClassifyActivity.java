package com.miittech.you.activity.device;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.google.gson.Gson;
import com.miittech.you.R;
import com.miittech.you.activity.BaseActivity;
import com.miittech.you.utils.Common;
import com.miittech.you.global.HttpUrl;
import com.miittech.you.global.IntentExtras;
import com.miittech.you.global.PubParam;
import com.miittech.you.impl.OnNetRequestCallBack;
import com.miittech.you.impl.TitleBarOptions;
import com.miittech.you.net.ApiServiceManager;
import com.miittech.you.net.response.DeviceListResponse;
import com.miittech.you.weight.Titlebar;
import com.ryon.mutils.EncryptUtils;
import com.ryon.mutils.LogUtils;
import com.ryon.mutils.NetworkUtils;
import com.ryon.mutils.ToastUtils;

import java.util.LinkedHashMap;
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
 * Created by Administrator on 2017/10/19.
 */

public class DeviceSetClassifyActivity extends BaseActivity {
    @BindView(R.id.titlebar)
    Titlebar titlebar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_set_classify);
        ButterKnife.bind(this);
        initMyTitleBar(titlebar,"设置分类");
        titlebar.showBackOption();
        titlebar.setTitleBarOptions(new TitleBarOptions(){
            @Override
            public void onBack() {
                super.onBack();
                finish();
            }
        });
    }

    @OnClick({R.id.ll_yaoshi, R.id.ll_qianbao, R.id.ll_shoutibao, R.id.ll_diannao, R.id.ll_zixingche, R.id.ll_qiche, R.id.ll_xiangji, R.id.ll_yusan, R.id.ll_yifu, R.id.ll_shenfezheng, R.id.ll_huzhao, R.id.ll_xinglixiang, R.id.ll_beibao, R.id.ll_shoutixiang, R.id.ll_qita})
    public void onViewClicked(View view) {
        Intent intent = new Intent(this,DeviceSetAttrActivity.class);
        intent.putExtras(getIntent());
        switch (view.getId()) {
            case R.id.ll_yaoshi:
                intent.putExtra(IntentExtras.DEVICE.CLASSIFY,"钥匙");
                break;
            case R.id.ll_qianbao:
                intent.putExtra(IntentExtras.DEVICE.CLASSIFY,"钱包");
                break;
            case R.id.ll_shoutibao:
                intent.putExtra(IntentExtras.DEVICE.CLASSIFY,"手提包");
                break;
            case R.id.ll_diannao:
                intent.putExtra(IntentExtras.DEVICE.CLASSIFY,"电脑");
                break;
            case R.id.ll_zixingche:
                intent.putExtra(IntentExtras.DEVICE.CLASSIFY,"自行车");
                break;
            case R.id.ll_qiche:
                intent.putExtra(IntentExtras.DEVICE.CLASSIFY,"汽车");
                break;
            case R.id.ll_xiangji:
                intent.putExtra(IntentExtras.DEVICE.CLASSIFY,"相机");
                break;
            case R.id.ll_yusan:
                intent.putExtra(IntentExtras.DEVICE.CLASSIFY,"雨伞");
                break;
            case R.id.ll_yifu:
                intent.putExtra(IntentExtras.DEVICE.CLASSIFY,"衣服");
                break;
            case R.id.ll_shenfezheng:
                intent.putExtra(IntentExtras.DEVICE.CLASSIFY,"身份证");
                break;
            case R.id.ll_huzhao:
                intent.putExtra(IntentExtras.DEVICE.CLASSIFY,"护照");
                break;
            case R.id.ll_xinglixiang:
                intent.putExtra(IntentExtras.DEVICE.CLASSIFY,"行李箱");
                break;
            case R.id.ll_beibao:
                intent.putExtra(IntentExtras.DEVICE.CLASSIFY,"背包");
                break;
            case R.id.ll_shoutixiang:
                intent.putExtra(IntentExtras.DEVICE.CLASSIFY,"手提箱");
                break;
            case R.id.ll_qita:
                intent.putExtra(IntentExtras.DEVICE.CLASSIFY,"其他");
                break;
        }
        setDeviceClassfy(intent);
    }

    private void setDeviceClassfy(final Intent intent) {
        if(!NetworkUtils.isConnected()){
            ToastUtils.showShort(R.string.msg_net_error);
            return;
        }
        final String devId = intent.getStringExtra(IntentExtras.DEVICE.ID);
        final String classfy = intent.getStringExtra(IntentExtras.DEVICE.CLASSIFY);
        Map devattrMap = new LinkedHashMap();
        Map param = new LinkedHashMap();
        if(getIntent().hasExtra(IntentExtras.FROM)&&"DEVICESETTING".equals(getIntent().getStringExtra(IntentExtras.FROM))){
            devattrMap.put("groupid", "1");
            devattrMap.put("groupname", Common.encodeBase64(classfy));
            param.put("devid", devId);
            param.put("method", "D");
            param.put("devattr", devattrMap);
        }else {
            devattrMap.put("groupid", "1");
            devattrMap.put("groupname", Common.encodeBase64(classfy));
            devattrMap.put("devname", Common.encodeBase64(classfy));
            param.put("devid", devId);
            param.put("method", "AD");
            param.put("devattr", devattrMap);
        }
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
                            if(getIntent().hasExtra(IntentExtras.FROM)&&"DEVICESETTING".equals(getIntent().getStringExtra(IntentExtras.FROM))){
                                Intent data = new Intent();
                                data.putExtra(IntentExtras.DEVICE.CLASSIFY,classfy);
                                setResult(RESULT_OK,data);
                                Common.initDeviceList(DeviceSetClassifyActivity.this,null);
                                Common.getDeviceDetailInfo(DeviceSetClassifyActivity.this, devId, new OnNetRequestCallBack() {
                                    @Override
                                    public void OnRequestComplete() {
                                        DeviceSetClassifyActivity.this.finish();
                                    }
                                });
                            }else {
                                startActivity(intent);
                            }
                        }else {
                            response.onError(DeviceSetClassifyActivity.this);
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
