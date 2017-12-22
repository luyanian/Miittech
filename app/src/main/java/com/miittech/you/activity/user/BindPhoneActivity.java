package com.miittech.you.activity.user;

import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.google.gson.Gson;
import com.miittech.you.App;
import com.miittech.you.R;
import com.miittech.you.activity.BaseActivity;
import com.miittech.you.common.Common;
import com.miittech.you.common.OnGetVerCodeComplete;
import com.miittech.you.impl.OnNetRequestCallBack;
import com.miittech.you.net.ApiServiceManager;
import com.miittech.you.global.HttpUrl;
import com.miittech.you.global.Params;
import com.miittech.you.global.PubParam;
import com.miittech.you.net.response.UserInfoResponse;
import com.miittech.you.utils.CountDownTimerUtils;
import com.miittech.you.weight.Titlebar;
import com.miittech.you.impl.TitleBarOptions;
import com.ryon.mutils.EncryptUtils;
import com.ryon.mutils.LogUtils;
import com.ryon.mutils.NetworkUtils;
import com.ryon.mutils.RegexUtils;
import com.ryon.mutils.ToastUtils;

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

public class BindPhoneActivity extends BaseActivity {

    @BindView(R.id.et_user_phone)
    EditText etUserPhone;
    @BindView(R.id.et_user_vercode)
    EditText etUserVercode;
    @BindView(R.id.btn_get_code)
    TextView btnGetCode;
    @BindView(R.id.titlebar)
    Titlebar titlebar;
    private String cliendid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bind_phone);
        ButterKnife.bind(this);
        initMyTitleBar(titlebar,getResources().getString(R.string.text_bind_phone));
        titlebar.showBackOption();
        titlebar.setTitleBarOptions(new TitleBarOptions(){
            @Override
            public void onBack() {
                super.onBack();
                finish();
            }
        });
    }

    @OnClick({R.id.btn_get_code, R.id.btn_ok})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.btn_get_code:
                String phone = etUserPhone.getText().toString().trim();
                Common.getMsgCode(this, phone, new OnGetVerCodeComplete() {
                    @Override
                    public void onRequestStart() {
                        CountDownTimerUtils mCountDownTimerUtils = new CountDownTimerUtils(btnGetCode, 60000, 1000);
                        mCountDownTimerUtils.start();
                    }
                    @Override
                    public void onSuccessful(String cliendid) {
                        BindPhoneActivity.this.cliendid = cliendid;
                    }
                });
                break;
            case R.id.btn_ok:
                doBindPhone();
                break;
        }
    }

    private void doBindPhone() {
        String phone = etUserPhone.getText().toString().trim();
        String vercode = etUserVercode.getText().toString().trim();
        if (!RegexUtils.isMobileSimple(phone)) {
            ToastUtils.showShort(getResources().getString(R.string.tip_ver_phone_faild));
            return;
        }
        if (TextUtils.isEmpty(vercode)) {
            ToastUtils.showShort(getResources().getString(R.string.tip_ver_code_empty));
            return;
        }
        if(!NetworkUtils.isConnected()){
            ToastUtils.showShort(R.string.msg_net_error);
            return;
        }
        Map param = new HashMap();
        param.put("method", Params.METHOD.PHONE);
        param.put("phone", phone);
        param.put("clientid", cliendid);
        param.put("vcode", vercode);

        String json = new Gson().toJson(param);
        PubParam pubParam = new PubParam(Common.getUserId());
        String sign_unSha1 = pubParam.toValueString() + json + Common.getTocken();
        LogUtils.d("sign_unsha1", sign_unSha1);
        String sign = EncryptUtils.encryptSHA1ToString(sign_unSha1).toLowerCase();
        LogUtils.d("sign_sha1", sign);
        String path = HttpUrl.Api + "userbind/" + pubParam.toUrlParam(sign);
        RequestBody requestBody = RequestBody.create(MediaType.parse(HttpUrl.MediaType_Json), json);
        ApiServiceManager.getInstance().buildApiService(this).postToGetUserInfo(path, requestBody)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<UserInfoResponse>() {
                    @Override
                    public void accept(UserInfoResponse response) throws Exception {
                        if (response.isSuccessful()) {
                            Common.getUserInfo(App.getInstance(), new OnNetRequestCallBack() {
                                @Override
                                public void OnRequestComplete() {
                                    BindPhoneActivity.this.finish();
                                }
                            });
                            ToastUtils.showShort(getResources().getString(R.string.msg_device_bind_success));
                        } else {
                            response.onError(BindPhoneActivity.this);
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
