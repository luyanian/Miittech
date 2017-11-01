package com.miittech.you.activity.user;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.google.gson.Gson;
import com.miittech.you.App;
import com.miittech.you.R;
import com.miittech.you.activity.BaseActivity;
import com.miittech.you.activity.MainActivity;
import com.miittech.you.impl.TitleBarOptions;
import com.miittech.you.impl.TypeSelectorChangeLisener;
import com.miittech.you.net.ApiServiceManager;
import com.miittech.you.global.HttpUrl;
import com.miittech.you.global.Params;
import com.miittech.you.global.PubParam;
import com.miittech.you.net.response.UserInfoResponse;
import com.miittech.you.weight.Titlebar;
import com.miittech.you.weight.TypeSelector;
import com.ryon.mutils.ActivityPools;
import com.ryon.mutils.EncryptUtils;
import com.ryon.mutils.LogUtils;
import com.ryon.mutils.RegexUtils;
import com.ryon.mutils.SPUtils;
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

public class LoginActivity extends BaseActivity implements TypeSelectorChangeLisener {

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
        initTitleBar(titlebar, getResources().getString(R.string.text_login));
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
        ApiServiceManager.getInstance().buildApiService(this).postToGetUserInfo(path, requestBody)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<UserInfoResponse>() {
                    @Override
                    public void accept(UserInfoResponse response) throws Exception {
                        if (response.isSuccessful()) {
                            SPUtils.getInstance(App.SESSION).put(App.USERID,response.getUserid());
                            SPUtils.getInstance(App.SESSION).put(App.TOCKEN,response.getToken());
                            SPUtils.getInstance(App.SESSION).put(App.UNAME,response.getUsername());

                            ToastUtils.showShort(getResources().getString(R.string.msg_user_login_successful));
                            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                            startActivity(intent);
                            ActivityPools.finishAllExcept(MainActivity.class);
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
                break;
            case R.id.btn_login_with_wechat:
                break;
            case R.id.btn_login_with_qq:
                break;
        }
    }
}
