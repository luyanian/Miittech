package com.miittech.you.activity.setting;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.TextView;

import com.google.gson.Gson;
import com.miittech.you.App;
import com.miittech.you.R;
import com.miittech.you.activity.BaseActivity;
import com.miittech.you.common.Common;
import com.miittech.you.global.HttpUrl;
import com.miittech.you.global.Params;
import com.miittech.you.global.PubParam;
import com.miittech.you.impl.TitleBarOptions;
import com.miittech.you.net.ApiServiceManager;
import com.miittech.you.net.response.BaseResponse;
import com.miittech.you.weight.Titlebar;
import com.ryon.mutils.EncryptUtils;
import com.ryon.mutils.LogUtils;
import com.ryon.mutils.NetworkUtils;
import com.ryon.mutils.ToastUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
 * Created by Administrator on 2017/9/30.
 */

public class IgnoreAddWifiActivity extends BaseActivity {
    @BindView(R.id.titlebar)
    Titlebar titlebar;
    @BindView(R.id.et_name)
    EditText etName;
    @BindView(R.id.tv_ssid)
    TextView tvSsid;

    private String ssid="";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ignore_wifi_add);
        ButterKnife.bind(this);
        initMyTitleBar(titlebar,"WIFI勿扰区域");
        titlebar.showBackOption();
        titlebar.showCompleteOption();
        titlebar.setTitleBarOptions(new TitleBarOptions(){
            @Override
            public void onBack() {
                super.onBack();
                finish();
            }

            @Override
            public void onComplete() {
                super.onComplete();
                doComplete();
            }
        });
        updateSSID();
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        registerReceiver(new WifiStateReciver(),filter);
    }

    private class WifiStateReciver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            int wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, -1);
            if (wifiState == WifiManager.WIFI_STATE_DISABLING) {
                //正在关闭
            } else if (wifiState == WifiManager.WIFI_STATE_ENABLING) {
                //正在打开
            } else if (wifiState == WifiManager.WIFI_STATE_DISABLED) {
                //已经关闭
            } else if (wifiState == WifiManager.WIFI_STATE_ENABLED) {
                //已经打开
                updateSSID();
            } else {
                //未知状态
            }
        }
    }

    private void updateSSID() {
        ssid = NetworkUtils.getSsidOfConnectWifi().replace("\"","");
        tvSsid.setText("当前连接WIFI : "+ssid);
    }

    private void doComplete() {
        String name = etName.getText().toString().trim();
        if(TextUtils.isEmpty(name)){
            ToastUtils.showShort("请输入名称");
            return;
        }
        if(TextUtils.isEmpty(ssid)){
            ToastUtils.showShort("未获取到您的ssid,请wifi网络是否正常");
            return;
        }
        Map areadef = new HashMap();
        areadef.put("id",0);
        areadef.put("title", Common.encodeBase64(name));
        areadef.put("inout",1);
        areadef.put("ssid",ssid);
        Map donotdisturb = new HashMap();
        donotdisturb.put("areadef",areadef);
        Map config = new HashMap();
        config.put("donotdisturb",donotdisturb);
        Map param = new LinkedHashMap();
        param.put("method", Params.METHOD.IGNORE_ADD);
        param.put("config_type", "AREA");
        param.put("config", config);
        String json = new Gson().toJson(param);
        PubParam pubParam = new PubParam(Common.getUserId());
        String sign_unSha1 = pubParam.toValueString() + json + Common.getTocken();
        LogUtils.d("sign_unsha1", sign_unSha1);
        String sign = EncryptUtils.encryptSHA1ToString(sign_unSha1).toLowerCase();
        LogUtils.d("sign_sha1", sign);
        String path = HttpUrl.Api + "userconf/" + pubParam.toUrlParam(sign);
        final RequestBody requestBody = RequestBody.create(MediaType.parse(HttpUrl.MediaType_Json), json);
        ApiServiceManager.getInstance().buildApiService(this).postNetRequest(path, requestBody)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<BaseResponse>() {
                    @Override
                    public void accept(BaseResponse response) throws Exception {
                        if(response.isSuccessful()){
                            ToastUtils.showShort("添加勿扰设置成功");
                            finish();
                        }else{
                            response.onError(IgnoreAddWifiActivity.this);
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
