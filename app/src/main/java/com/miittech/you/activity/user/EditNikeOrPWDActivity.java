package com.miittech.you.activity.user;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.google.gson.Gson;
import com.miittech.you.App;
import com.miittech.you.R;
import com.miittech.you.activity.BaseActivity;
import com.miittech.you.common.Common;
import com.miittech.you.impl.TitleBarOptions;
import com.miittech.you.net.ApiServiceManager;
import com.miittech.you.global.HttpUrl;
import com.miittech.you.global.PubParam;
import com.miittech.you.net.response.BaseResponse;
import com.miittech.you.weight.Titlebar;
import com.ryon.mutils.EncryptUtils;
import com.ryon.mutils.LogUtils;
import com.ryon.mutils.ToastUtils;

import java.util.HashMap;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import okhttp3.MediaType;
import okhttp3.RequestBody;

/**
 * Created by Administrator on 2017/9/19.
 */

public class EditNikeOrPWDActivity extends BaseActivity {
    @BindView(R.id.titlebar)
    Titlebar titlebar;
    String edit = "";
    @BindView(R.id.et_user_nike)
    EditText etUserNike;
    @BindView(R.id.ll_edit_uname)
    LinearLayout llEditUname;
    @BindView(R.id.et_user_password_old)
    EditText etUserPasswordOld;
    @BindView(R.id.et_user_password_new)
    EditText etUserPasswordNew;
    @BindView(R.id.ll_edit_upassword)
    LinearLayout llEditUpassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_nike_or_pwd);
        ButterKnife.bind(this);
        initMyTitleBar(titlebar, getResources().getString(R.string.text_setting));
        titlebar.showCancleOption();
        titlebar.showCompleteOption();
        edit = getIntent().getStringExtra("edit");
        if ("nike".equals(edit)) {
            llEditUpassword.setVisibility(View.GONE);
            llEditUname.setVisibility(View.VISIBLE);
            String nikeName = getIntent().getStringExtra("nike");
            if(!TextUtils.isEmpty(nikeName)){
                etUserNike.setText(new String(android.util.Base64.decode(nikeName, android.util.Base64.DEFAULT)));
            }
        } else {
            llEditUname.setVisibility(View.GONE);
            llEditUpassword.setVisibility(View.VISIBLE);
        }
        titlebar.setTitleBarOptions(new TitleBarOptions() {
            @Override
            public void onCancel() {
                super.onCancel();
                finish();
            }

            @Override
            public void onComplete() {
                super.onComplete();
                doEditInfo();
            }
        });
    }

    private void doEditInfo() {
        if("nike".equals(edit)){
            editUserNike();
        }
        if("password".equals(edit)){
            editUserPassword();
        }
    }

    private void editUserPassword() {
        String oldpwd = etUserPasswordOld.getText().toString().trim();
        String newpwd = etUserPasswordNew.getText().toString().trim();

        if(TextUtils.isEmpty(newpwd)){
            ToastUtils.showShort(getResources().getString(R.string.tip_ver_password_empty));
            return;
        }

        Map param = new HashMap();
        param.put("oldpasswd", oldpwd);
        param.put("newpasswd", newpwd);

        String json = new Gson().toJson(param);
        PubParam pubParam = new PubParam(Common.getUserId());
        String sign_unSha1 = pubParam.toValueString() + json + Common.getTocken();
        LogUtils.d("sign_unsha1", sign_unSha1);
        String sign = EncryptUtils.encryptSHA1ToString(sign_unSha1).toLowerCase();
        LogUtils.d("sign_sha1", sign);
        String path = HttpUrl.Api + "userpasswd/" + pubParam.toUrlParam(sign);
        RequestBody requestBody = RequestBody.create(MediaType.parse(HttpUrl.MediaType_Json), json);

        ApiServiceManager.getInstance().buildApiService(this).postNetRequest(path, requestBody)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<BaseResponse>() {
                    @Override
                    public void accept(BaseResponse response) throws Exception {
                        if (response.isSuccessful()) {
                            ToastUtils.showShort(getResources().getString(R.string.msg_edit_successful));
                            EditNikeOrPWDActivity.this.finish();
                        }else{
                            ToastUtils.showShort(response.getErrmsg());
                        }
                    }
                });
    }

    private void editUserNike() {
        String nikeName = etUserNike.getText().toString().trim();
        if(TextUtils.isEmpty(nikeName)){
            ToastUtils.showShort(getResources().getString(R.string.tip_ver_user_nike_empty));
            return;
        }
        Map userattr = new HashMap();
        userattr.put("nickname", Common.encodeBase64(nikeName));
        Map param = new HashMap();
        param.put("method", "B");
        param.put("userattr", userattr);

        String json = new Gson().toJson(param);
        PubParam pubParam = new PubParam(Common.getUserId());
        String sign_unSha1 = pubParam.toValueString() + json + Common.getTocken();
        LogUtils.d("sign_unsha1", sign_unSha1);
        String sign = EncryptUtils.encryptSHA1ToString(sign_unSha1).toLowerCase();
        LogUtils.d("sign_sha1", sign);
        String path = HttpUrl.Api + "userattr/" + pubParam.toUrlParam(sign);
        RequestBody requestBody = RequestBody.create(MediaType.parse(HttpUrl.MediaType_Json), json);

        ApiServiceManager.getInstance().buildApiService(this).postNetRequest(path, requestBody)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<BaseResponse>() {
                    @Override
                    public void accept(BaseResponse response) throws Exception {
                        if (response.isSuccessful()) {
                            ToastUtils.showShort(getResources().getString(R.string.msg_edit_successful));
                            EditNikeOrPWDActivity.this.finish();
                        }else{
                            ToastUtils.showShort(response.getErrmsg());
                        }
                    }
                });
    }
}
