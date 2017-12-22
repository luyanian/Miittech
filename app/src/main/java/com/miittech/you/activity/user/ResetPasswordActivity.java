package com.miittech.you.activity.user;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;

import com.google.gson.Gson;
import com.miittech.you.R;
import com.miittech.you.activity.BaseActivity;
import com.miittech.you.activity.MainActivity;
import com.miittech.you.global.HttpUrl;
import com.miittech.you.global.Params;
import com.miittech.you.global.PubParam;
import com.miittech.you.global.SPConst;
import com.miittech.you.impl.TitleBarOptions;
import com.miittech.you.net.ApiServiceManager;
import com.miittech.you.net.response.LoginResponse;
import com.miittech.you.weight.Titlebar;
import com.ryon.mutils.ActivityPools;
import com.ryon.mutils.EncryptUtils;
import com.ryon.mutils.LogUtils;
import com.ryon.mutils.NetworkUtils;
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

/**
 * Created by Administrator on 2017/12/19.
 */

public class ResetPasswordActivity extends BaseActivity {
    @BindView(R.id.titlebar)
    Titlebar titlebar;
    @BindView(R.id.et_email_user_password)
    EditText etEmailUserPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_password_reset);
        ButterKnife.bind(this);
        initMyTitleBar(titlebar,"重置密码");
        titlebar.showBackOption();
        titlebar.setTitleBarOptions(new TitleBarOptions(){
            @Override
            public void onBack() {
                super.onBack();
                finish();
            }
        });
    }

    @OnClick(R.id.btn_ok)
    public void onViewClicked() {
        doResetPassword();
    }

    private void doResetPassword() {
        String password = etEmailUserPassword.getText().toString().trim();
        if(TextUtils.isEmpty(password)){
            ToastUtils.showShort("新密码不能为空");
            return;
        }
        if(!NetworkUtils.isConnected()){
            ToastUtils.showShort(R.string.msg_net_error);
            return;
        }
        String phone = getIntent().getStringExtra("phone");
        String cliendid = getIntent().getStringExtra("cliendId");
        String verCode = getIntent().getStringExtra("verCode");
        Map param = new HashMap();
        param.put("phone", phone);
        param.put("clientid", cliendid);
        param.put("newpasswd",EncryptUtils.encryptSHA1ToString(password).toLowerCase());
        String json = new Gson().toJson(param);
        PubParam pubParam = new PubParam(Params.userid_unlogin);
        String sign_unSha1 = pubParam.toValueString() + json + EncryptUtils.encryptSHA1ToString(verCode).toLowerCase();
        LogUtils.d("sign_unsha1", sign_unSha1);
        String sign = EncryptUtils.encryptSHA1ToString(sign_unSha1).toLowerCase();
        LogUtils.d("sign_sha1", sign);
        String path = HttpUrl.Api + "resetpasswd/" + pubParam.toUrlParam(sign);
        RequestBody requestBody = RequestBody.create(MediaType.parse(HttpUrl.MediaType_Json), json);

        ApiServiceManager.getInstance().buildApiService(this).postToLogin(path, requestBody)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<LoginResponse>() {
                    @Override
                    public void accept(LoginResponse response) throws Exception {
                        if (response.isSuccessful()) {
                            SPUtils.getInstance(SPConst.USER.SP_NAME).put(SPConst.USER.KEY_TOCKEN,response.getToken());
                            SPUtils.getInstance(SPConst.USER.SP_NAME).put(SPConst.USER.KEY_USERID,response.getUserid());
                            ToastUtils.showLong(getResources().getString(R.string.msg_regist_successful));
                            Intent intent = new Intent(ResetPasswordActivity.this, MainActivity.class);
                            startActivity(intent);
                            ActivityPools.finishAllExcept(RegisteActivity.class);
                        }else {
                            response.onError(ResetPasswordActivity.this);
                        }
                    }
                });
    }

}
