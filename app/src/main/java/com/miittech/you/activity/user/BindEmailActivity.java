package com.miittech.you.activity.user;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.google.gson.Gson;
import com.miittech.you.App;
import com.miittech.you.R;
import com.miittech.you.activity.BaseActivity;
import com.miittech.you.net.ApiServiceManager;
import com.miittech.you.net.global.HttpUrl;
import com.miittech.you.net.global.Params;
import com.miittech.you.net.global.PubParam;
import com.miittech.you.net.response.UserInfoResponse;
import com.miittech.you.weight.Titlebar;
import com.miittech.you.impl.TitleBarOptions;
import com.ryon.mutils.EncryptUtils;
import com.ryon.mutils.LogUtils;
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

public class BindEmailActivity extends BaseActivity {

    @BindView(R.id.et_user_email)
    EditText etUserEmail;
    @BindView(R.id.tv_msg)
    TextView tvMsg;
    @BindView(R.id.btn_ok_text)
    TextView btnOkText;
    @BindView(R.id.titlebar)
    Titlebar titlebar;

    String email = "";
    int isBindEmail = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bind_email);
        ButterKnife.bind(this);
        initTitleBar(titlebar, getResources().getString(R.string.text_bind_email));
        titlebar.showBackOption();
        titlebar.setTitleBarOptions(new TitleBarOptions() {
            @Override
            public void onBack() {
                super.onBack();
                finish();
            }
        });
        email = getIntent().getStringExtra("email");
        isBindEmail = getIntent().getIntExtra("isBindEmail", 0);

        if (TextUtils.isEmpty(email)) {
            btnOkText.setText(getResources().getString(R.string.text_submit));
            tvMsg.setVisibility(View.GONE);
        } else {
            etUserEmail.setText(email);
            btnOkText.setText(getResources().getString(R.string.text_email_resend_to_verify));
            if(isBindEmail!=1){
                tvMsg.setVisibility(View.VISIBLE);
            }
        }
    }

    @OnClick(R.id.btn_ok)
    public void onViewClicked() {
        String email = etUserEmail.getText().toString().trim();
        if (!RegexUtils.isEmail(email)) {
            ToastUtils.showShort(getResources().getString(R.string.tip_ver_email_faild));
            return;
        }

        Map param = new HashMap();
        param.put("method", Params.METHOD.EMAIL);
        param.put("email", email);

        String json = new Gson().toJson(param);
        PubParam pubParam = new PubParam(App.getUserId());
        String sign_unSha1 = pubParam.toValueString() + json + App.getTocken();
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
                            ToastUtils.showShort(getResources().getString(R.string.msg_device_bind_success));
                            finish();
                        } else {
                            response.onError(BindEmailActivity.this);
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
