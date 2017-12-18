package com.miittech.you.activity.user;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.gson.Gson;
import com.miittech.you.R;
import com.miittech.you.activity.BaseActivity;
import com.miittech.you.activity.MainActivity;
import com.miittech.you.activity.setting.PrivacyProtocolsActivity;
import com.miittech.you.activity.setting.UseTermsActivity;
import com.miittech.you.common.Common;
import com.miittech.you.common.OnGetVerCodeComplete;
import com.miittech.you.global.SPConst;
import com.miittech.you.impl.TitleBarOptions;
import com.miittech.you.impl.TypeSelectorChangeLisener;
import com.miittech.you.net.ApiServiceManager;
import com.miittech.you.global.HttpUrl;
import com.miittech.you.global.Params;
import com.miittech.you.global.PubParam;
import com.miittech.you.net.response.BaseResponse;
import com.miittech.you.net.response.LoginResponse;
import com.miittech.you.utils.CountDownTimerUtils;
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

public class RegisteActivity extends BaseActivity implements TypeSelectorChangeLisener {

    @BindView(R.id.relativeLayout3)
    RelativeLayout relativeLayout3;
    @BindView(R.id.titlebar)
    Titlebar titlebar;
    @BindView(R.id.typeSelector)
    TypeSelector typeSelector;
    @BindView(R.id.et_user_phone)
    EditText etUserPhone;
    @BindView(R.id.et_user_vercode)
    EditText etUserVercode;
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
    @BindView(R.id.btn_get_code)
    TextView btnGetCode;

    private String cliendid = "";
    private boolean isSSORegist = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registe);
        ButterKnife.bind(this);
        initMyTitleBar(titlebar,getResources().getString(R.string.text_register));
        typeSelector.setTypeSelectorChangeLisener(this);
        typeSelector.setSelectItem(0);
        titlebar.showBackOption();
        titlebar.setTitleBarOptions(new TitleBarOptions() {
            @Override
            public void onBack() {
                finish();
            }
        });
        isSSORegist = getIntent().hasExtra("method");
        if(isSSORegist){
            typeSelector.disableSelectEmail();
        }
    }

    @OnClick({R.id.btn_get_code, R.id.btn_ok,R.id.tv_use_terms,R.id.tv_privace_protocols})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.btn_get_code:
                String phone = etUserPhone.getText().toString().trim();
                Common.getMsgCode(this,phone,new OnGetVerCodeComplete(){
                    @Override
                    public void onRequestStart() {
                        CountDownTimerUtils mCountDownTimerUtils = new CountDownTimerUtils(btnGetCode, 60000, 1000);
                        mCountDownTimerUtils.start();
                    }

                    @Override
                    public void onSuccessful(String cliendid) {
                        RegisteActivity.this.cliendid = cliendid;
                    }
                });
                break;
            case R.id.btn_ok:
                doRegiste();
                break;
            case R.id.tv_use_terms:
                Intent terms = new Intent(this, UseTermsActivity.class);
                startActivity(terms);
                break;
            case R.id.tv_privace_protocols:
                Intent privace = new Intent(this, PrivacyProtocolsActivity.class);
                startActivity(privace);
                break;
        }
    }



    public void doRegiste(){
        String method="";
        String phone = etUserPhone.getText().toString().trim();
        String verCode = etUserVercode.getText().toString().trim();
        String email = etUserEmail.getText().toString().trim();
        String password = "";
        if(typeSelector.getSelectItem()==0){
            method=Params.METHOD.PHONE;
            password = etPhoneUserPassword.getText().toString().trim();
            if(!RegexUtils.isMobileSimple(phone)){
                ToastUtils.showShort(R.string.tip_ver_phone_faild);
                return;
            }
            if(TextUtils.isEmpty(verCode)){
                ToastUtils.showShort(R.string.tip_ver_code_empty);
                return;
            }
        }else if(typeSelector.getSelectItem()==1){
            method=Params.METHOD.EMAIL;
            password = etEmailUserPassword.getText().toString().trim();
            if(!RegexUtils.isEmail(email)){
                ToastUtils.showShort(R.string.tip_ver_email_faild);
                return;
            }
        }
        if(TextUtils.isEmpty(password)){
            ToastUtils.showShort(R.string.tip_ver_password_empty);
            return;
        }
        Map param = new HashMap();
        if(isSSORegist) {
            String openid = getIntent().getStringExtra("openid");
            method = getIntent().getStringExtra("method");
            param.put("openid", openid);
        }
        param.put("method", method);
        param.put("phone", phone);
        param.put("clientid", cliendid);
        param.put("username", email);
        param.put("password",EncryptUtils.encryptSHA1ToString(password).toLowerCase());
        String json = new Gson().toJson(param);
        PubParam pubParam = new PubParam(Params.userid_unlogin);
        String sign_unSha1 = pubParam.toValueString() + json + (typeSelector.getSelectItem()==0?EncryptUtils.encryptSHA1ToString(verCode).toLowerCase():Params.signkey_unlogin);
        LogUtils.d("sign_unsha1", sign_unSha1);
        String sign = EncryptUtils.encryptSHA1ToString(sign_unSha1).toLowerCase();
        LogUtils.d("sign_sha1", sign);
        String path = HttpUrl.Api + "user/" + pubParam.toUrlParam(sign);
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
                            Intent intent = new Intent(RegisteActivity.this, MainActivity.class);
                            startActivity(intent);
                            ActivityPools.finishAllExcept(RegisteActivity.class);
                        }else {
                            response.onError(RegisteActivity.this);
                        }
                    }
                });

    }

    @Override
    public void onTabSelectorChanged(int item) {
        if (item == 0) {
            llTabEmail.setVisibility(View.GONE);
            llTabPhone.setVisibility(View.VISIBLE);
        } else if (item == 1) {
            llTabPhone.setVisibility(View.GONE);
            llTabEmail.setVisibility(View.VISIBLE);
        }
    }
}
