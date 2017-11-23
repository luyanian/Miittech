package com.miittech.you.activity.setting;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;

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
import com.ryon.mutils.ToastUtils;

import org.w3c.dom.Text;

import java.util.HashMap;
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
 * Created by Administrator on 2017/11/23.
 */

public class IgnoreNameEditActivity extends BaseActivity {
    @BindView(R.id.titlebar)
    Titlebar titlebar;
    @BindView(R.id.et_name)
    EditText etName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ignore_name_edit);
        ButterKnife.bind(this);
        initMyTitleBar(titlebar,"设置位置勿扰区域");
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
                updateIgnoreSetting();
            }
        });
    }
    public void updateIgnoreSetting(){
        String name = etName.getText().toString().trim();
        if(TextUtils.isEmpty(name)){
            ToastUtils.showShort("请填写名称");
            return;
        }
        double lat = getIntent().getDoubleExtra("lat",0);
        double lng = getIntent().getDoubleExtra("lng",0);
        int progress = getIntent().getIntExtra("progress",0);
        Map area = new HashMap();
        area.put("lat",lat);
        area.put("lng",lng);
        area.put("R",progress);
        Map areadef = new HashMap();
        areadef.put("id",0);
        areadef.put("title", Common.encodeBase64(name));
        areadef.put("inout",1);
        areadef.put("areadef",area);
        Map donotdisturb = new HashMap();
        donotdisturb.put("areadef",areadef);
        Map config = new HashMap();
        config.put("donotdisturb",donotdisturb);
        Map param = new LinkedHashMap();
        param.put("method", Params.METHOD.IGNORE_ADD);
        param.put("config_type", "AREA");
        param.put("config", config);
        String json = new Gson().toJson(param);
        PubParam pubParam = new PubParam(App.getInstance().getUserId());
        String sign_unSha1 = pubParam.toValueString() + json + App.getInstance().getTocken();
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
                            response.onError(IgnoreNameEditActivity.this);
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
