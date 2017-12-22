package com.miittech.you.activity.user;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.google.gson.Gson;
import com.miittech.you.R;
import com.miittech.you.activity.BaseActivity;
import com.miittech.you.activity.MainActivity;
import com.miittech.you.common.Common;
import com.miittech.you.global.SPConst;
import com.miittech.you.impl.TitleBarOptions;
import com.miittech.you.impl.TypeSelectorChangeLisener;
import com.miittech.you.net.ApiServiceManager;
import com.miittech.you.global.HttpUrl;
import com.miittech.you.global.Params;
import com.miittech.you.global.PubParam;
import com.miittech.you.net.response.BaseResponse;
import com.miittech.you.net.response.LoginResponse;
import com.miittech.you.weight.Titlebar;
import com.miittech.you.weight.TypeSelector;
import com.ryon.mutils.ActivityPools;
import com.ryon.mutils.AppUtils;
import com.ryon.mutils.EncryptUtils;
import com.ryon.mutils.LogUtils;
import com.ryon.mutils.NetworkUtils;
import com.ryon.mutils.RegexUtils;
import com.ryon.mutils.SPUtils;
import com.ryon.mutils.ToastUtils;

import java.util.HashMap;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cn.jpush.android.api.JPushInterface;
import cn.sharesdk.framework.Platform;
import cn.sharesdk.framework.PlatformActionListener;
import cn.sharesdk.framework.ShareSDK;
import cn.sharesdk.tencent.qq.QQ;
import cn.sharesdk.wechat.friends.Wechat;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import okhttp3.MediaType;
import okhttp3.RequestBody;

public class LoginActivity extends BaseActivity implements TypeSelectorChangeLisener, PlatformActionListener {

    @BindView(R.id.titlebar)
    Titlebar titlebar;
    @BindView(R.id.typeSelector)
    TypeSelector typeSelector;
    @BindView(R.id.et_user_phone)
    EditText etUserPhone;
    @BindView(R.id.et_phone_user_password)
    EditText etPhoneUserPassword;
    @BindView(R.id.ll_tab_phone)
    LinearLayout llTabPhone;
    @BindView(R.id.et_user_email)
    EditText etUserEmail;
    @BindView(R.id.et_email_user_password)
    EditText etEmailUserPassword;
    @BindView(R.id.ll_tab_email)
    LinearLayout llTabEmail;
    private int curItem = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        ButterKnife.bind(this);
        initMyTitleBar(titlebar, getResources().getString(R.string.text_login));
        titlebar.setTitleBarOptions(new TitleBarOptions() {
            @Override
            public void onBack() {
                finish();
            }
        });
        typeSelector.setTypeSelectorChangeLisener(this);
        typeSelector.setSelectItem(0);

    }

    @Override
    public void onTabSelectorChanged(int item) {
        curItem = item;
        if (item == 0) {
            llTabEmail.setVisibility(View.GONE);
            llTabPhone.setVisibility(View.VISIBLE);
        } else if (item == 1) {
            llTabPhone.setVisibility(View.GONE);
            llTabEmail.setVisibility(View.VISIBLE);
        }
    }

    public void doLogin() {
        String uname = "";
        String password = "";
        if (curItem == 0) {
            uname = etUserPhone.getText().toString().trim();
            password = etPhoneUserPassword.getText().toString().trim();
            if (!RegexUtils.isMobileSimple(uname)) {
                ToastUtils.showShort(getResources().getString(R.string.tip_ver_phone_faild));
                return;
            }
        } else if (curItem == 1) {
            uname = etUserEmail.getText().toString().trim();
            password = etEmailUserPassword.getText().toString().trim();
            if (!RegexUtils.isEmail(uname)) {
                ToastUtils.showShort(getResources().getString(R.string.tip_ver_email_faild));
                return;
            }
        }
        if (TextUtils.isEmpty(password)) {
            ToastUtils.showShort(getResources().getString(R.string.tip_ver_password_empty));
            return;
        }
        if(!NetworkUtils.isConnected()){
            ToastUtils.showShort(R.string.msg_net_error);
            return;
        }
        Map param = new HashMap();
        param.put("method", Params.METHOD.UNAME);
        param.put("username", uname);

        String json = new Gson().toJson(param);
        PubParam pubParam = new PubParam(Params.userid_unlogin);
        String sign_unSha1 = pubParam.toValueString() + json + EncryptUtils.encryptSHA1ToString(password).toLowerCase();
        LogUtils.d("sign_unsha1", sign_unSha1);
        String sign = EncryptUtils.encryptSHA1ToString(sign_unSha1).toLowerCase();
        LogUtils.d("sign_sha1", sign);
        String path = HttpUrl.Api + "login/" + pubParam.toUrlParam(sign);
        RequestBody requestBody = RequestBody.create(MediaType.parse(HttpUrl.MediaType_Json), json);
        ApiServiceManager.getInstance().buildApiService(this).postToLogin(path, requestBody)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<LoginResponse>() {
                    @Override
                    public void accept(LoginResponse response) throws Exception {
                        if (response.isSuccessful()) {
                            SPUtils.getInstance(SPConst.USER.SP_NAME).put(SPConst.USER.KEY_USERID,response.getUserid());
                            SPUtils.getInstance(SPConst.USER.SP_NAME).put(SPConst.USER.KEY_TOCKEN,response.getToken());
                            JPushInterface.setAlias(LoginActivity.this,0,response.getUserid());

                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    sendRegistationIdToServer(LoginActivity.this,JPushInterface.getRegistrationID(LoginActivity.this));
                                }
                            },500);

                            ToastUtils.showShort(getResources().getString(R.string.msg_user_login_successful));
                            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                            startActivity(intent);

                            ActivityPools.finishActivity(LoginRegisteActivity.class);
                            ActivityPools.finishActivity(LoginActivity.class);
                            ActivityPools.finishActivity(RegisteActivity.class);
                        } else {
                            response.onError(LoginActivity.this);
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        throwable.printStackTrace();
                    }
                });
    }

    @OnClick({R.id.btn_ok, R.id.btn_forget_password, R.id.btn_login_with_wechat, R.id.btn_login_with_qq})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.btn_ok:
                doLogin();
                break;
            case R.id.btn_forget_password:
                Intent intent = new Intent(this,ForgetPhonePasswordActivity.class);
                startActivity(intent);
                break;
            case R.id.btn_login_with_wechat:
                Platform wechat = ShareSDK.getPlatform(Wechat.NAME);
                wechat.SSOSetting(false);  //设置false表示使用SSO授权方式
                wechat.setPlatformActionListener(this); // 设置分享事件回调
                wechat.authorize();//单独授权
                wechat.showUser(null);//授权并获取用户信息
                break;
            case R.id.btn_login_with_qq:
                Platform qq = ShareSDK.getPlatform(QQ.NAME);
                qq.SSOSetting(false);  //设置false表示使用SSO授权方式
                qq.setPlatformActionListener(this); // 设置分享事件回调
                qq.authorize();//单独授权
                qq.showUser(null);//授权并获取用户信息
                break;
        }
    }

    @Override
    public void onComplete(Platform platform, int i, HashMap<String, Object> hashMap) {
        doShareSdkLogin(platform);
    }

    @Override
    public void onError(Platform platform, int i, Throwable throwable) {

    }

    @Override
    public void onCancel(Platform platform, int i) {

    }

    private synchronized void doShareSdkLogin(final Platform platform) {
        if(!NetworkUtils.isConnected()){
            ToastUtils.showShort(R.string.msg_net_error);
            return;
        }
        Map param = new HashMap();
        String access_token = platform.getDb().getToken(); // 获取授权token
        String openid = platform.getDb().getUserId(); // 获取用户在此平台的ID
        long expires_in = platform.getDb().getExpiresIn();
        String unionid = platform.getDb().get("unionid");
        if (Wechat.NAME.equals(platform.getName())) {
            param.put("method", Params.METHOD.WECHART);
        } else if (QQ.NAME.equals(platform.getName())) {
            param.put("method", Params.METHOD.QQSSO);
        }
        param.put("access_token", access_token);
        param.put("openid", openid);
        param.put("expires_in", expires_in);
        param.put("unionid", unionid);

        String json = new Gson().toJson(param);
        PubParam pubParam = new PubParam(Params.userid_unlogin);
        String sign_unSha1 = pubParam.toValueString() + json + Params.signkey_unlogin;
        LogUtils.d("sign_unsha1", sign_unSha1);
        String sign = EncryptUtils.encryptSHA1ToString(sign_unSha1).toLowerCase();
        LogUtils.d("sign_sha1", sign);
        String path = HttpUrl.Api + "login/" + pubParam.toUrlParam(sign);
        RequestBody requestBody = RequestBody.create(MediaType.parse(HttpUrl.MediaType_Json), json);
        ApiServiceManager.getInstance().buildApiService(this).postToLogin(path, requestBody)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<LoginResponse>() {
                    @Override
                    public void accept(LoginResponse response) throws Exception {
                        if (response.isSuccessful()) {
                            SPUtils.getInstance(SPConst.USER.SP_NAME).put(SPConst.USER.KEY_USERID,response.getUserid());
                            SPUtils.getInstance(SPConst.USER.SP_NAME).put(SPConst.USER.KEY_TOCKEN,response.getToken());
                            JPushInterface.setAlias(LoginActivity.this,0,response.getUserid());

                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    sendRegistationIdToServer(LoginActivity.this,JPushInterface.getRegistrationID(LoginActivity.this));
                                }
                            },1000);

                            ToastUtils.showShort(getResources().getString(R.string.msg_user_login_successful));
                            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                            startActivity(intent);

                            ActivityPools.finishActivity(LoginRegisteActivity.class);
                            ActivityPools.finishActivity(LoginActivity.class);
                            ActivityPools.finishActivity(RegisteActivity.class);
                        }else if(response.unExsitUser()){
                            Intent intent = new Intent(LoginActivity.this,RegisteActivity.class);
                            if (Wechat.NAME.equals(platform.getName())) {
                                intent.putExtra("method",Params.METHOD.PHONEWITHWX);
                                intent.putExtra("openid",response.getOpenid());
                                startActivity(intent);
                            }
                            if (QQ.NAME.equals(platform.getName())) {
                                intent.putExtra("method",Params.METHOD.PHONEWITHQQ);
                                intent.putExtra("openid",response.getOpenid());
                                startActivity(intent);
                            }
                        } else {
                            response.onError(LoginActivity.this);
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        throwable.printStackTrace();
                    }
                });
    }

    private void sendRegistationIdToServer(final Context context, String regId) {
        if(!NetworkUtils.isConnected()){
            return;
        }
        Map param = new HashMap();
        param.put("regid", regId);
        param.put("ostype", "android");
        param.put("ver", AppUtils.getAppVersionName());
        String json = new Gson().toJson(param);
        PubParam pubParam = new PubParam(Common.getUserId());
        String sign_unSha1 = pubParam.toValueString() + json + Common.getTocken();
        LogUtils.d("sign_unsha1", sign_unSha1);
        String sign = EncryptUtils.encryptSHA1ToString(sign_unSha1).toLowerCase();
        LogUtils.d("sign_sha1", sign);
        String path = HttpUrl.Api + "jpushid/" + pubParam.toUrlParam(sign);
        final RequestBody requestBody = RequestBody.create(MediaType.parse(HttpUrl.MediaType_Json), json);
        ApiServiceManager.getInstance().buildApiService(context).postNetRequest(path, requestBody)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<BaseResponse>() {
                    @Override
                    public void accept(BaseResponse response) throws Exception {
                        if (response.isSuccessful()) {

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
