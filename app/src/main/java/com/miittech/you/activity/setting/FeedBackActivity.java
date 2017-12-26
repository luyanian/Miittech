package com.miittech.you.activity.setting;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;

import com.google.gson.Gson;
import com.miittech.you.R;
import com.miittech.you.activity.BaseActivity;
import com.miittech.you.utils.Common;
import com.miittech.you.global.HttpUrl;
import com.miittech.you.global.PubParam;
import com.miittech.you.impl.TitleBarOptions;
import com.miittech.you.net.ApiServiceManager;
import com.miittech.you.net.response.BaseResponse;
import com.miittech.you.weight.Titlebar;
import com.ryon.mutils.EncryptUtils;
import com.ryon.mutils.LogUtils;
import com.ryon.mutils.NetworkUtils;
import com.ryon.mutils.RegexUtils;
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
 * Created by ryon on 2017/11/26.
 */

public class FeedBackActivity extends BaseActivity {
    @BindView(R.id.titlebar)
    Titlebar titlebar;
    @BindView(R.id.et_email)
    EditText etEmail;
    @BindView(R.id.et_content)
    EditText etContent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feedback);
        ButterKnife.bind(this);
        initMyTitleBar(titlebar,"意见反馈");
        titlebar.showBackOption();
        titlebar.setTitleBarOptions(new TitleBarOptions(){
            @Override
            public void onBack() {
                super.onBack();
                finish();
            }
        });
    }

    @OnClick(R.id.btn_submit)
    public void onViewClicked() {
        String email = etEmail.getText().toString().trim();
        String content = etContent.getText().toString().trim();
        if(TextUtils.isEmpty(email)){
            ToastUtils.showShort("邮箱不能为空");
            return;
        }
        if(!RegexUtils.isEmail(email)){
            ToastUtils.showShort("邮箱格式不正确");
            return;
        }
        if(TextUtils.isEmpty(content)){
            ToastUtils.showShort("反馈内容不能为空");
            return;
        }
        if(!NetworkUtils.isConnected()){
            ToastUtils.showShort(R.string.msg_net_error);
            return;
        }
        Map param = new LinkedHashMap();
        param.put("email", email);
        param.put("content", content);
        String json = new Gson().toJson(param);
        PubParam pubParam = new PubParam(Common.getUserId());
        String sign_unSha1 = pubParam.toValueString() + json + Common.getTocken();
        LogUtils.d("sign_unsha1", sign_unSha1);
        String sign = EncryptUtils.encryptSHA1ToString(sign_unSha1).toLowerCase();
        LogUtils.d("sign_sha1", sign);
        String path = HttpUrl.Api + "feedback/" + pubParam.toUrlParam(sign);
        final RequestBody requestBody = RequestBody.create(MediaType.parse(HttpUrl.MediaType_Json), json);

        ApiServiceManager.getInstance().buildApiService(this).postNetRequest(path, requestBody)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<BaseResponse>() {
                    @Override
                    public void accept(BaseResponse response) throws Exception {
                        if(response.isSuccessful()){
                            ToastUtils.showShort("提交成功");
                            finish();
                        }else{
                            response.onError(FeedBackActivity.this);
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
