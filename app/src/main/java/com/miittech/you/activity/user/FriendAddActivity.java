package com.miittech.you.activity.user;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.google.gson.Gson;
import com.miittech.you.App;
import com.miittech.you.R;
import com.miittech.you.activity.BaseActivity;
import com.miittech.you.net.ApiServiceManager;
import com.miittech.you.global.HttpUrl;
import com.miittech.you.global.Params;
import com.miittech.you.global.PubParam;
import com.miittech.you.net.response.BaseResponse;
import com.miittech.you.weight.Titlebar;
import com.miittech.you.weight.TypeSelector;
import com.miittech.you.impl.TitleBarOptions;
import com.miittech.you.impl.TypeSelectorChangeLisener;
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

/**
 * Created by Administrator on 2017/9/25.
 */

public class FriendAddActivity extends BaseActivity implements TypeSelectorChangeLisener {
    @BindView(R.id.titlebar)
    Titlebar titlebar;
    @BindView(R.id.typeSelector)
    TypeSelector typeSelector;
    @BindView(R.id.et_user_phone)
    EditText etUserPhone;
    @BindView(R.id.ll_tab_phone)
    LinearLayout llTabPhone;
    @BindView(R.id.et_user_email)
    EditText etUserEmail;
    @BindView(R.id.ll_tab_email)
    LinearLayout llTabEmail;

    private int curItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friend_add);
        ButterKnife.bind(this);
        initTitleBar(titlebar,R.string.text_setting_myfriends_add);
        titlebar.showBackOption();
        titlebar.setTitleBarOptions(new TitleBarOptions(){
            @Override
            public void onBack() {
                super.onBack();
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

    @OnClick(R.id.btn_ok)
    public void onViewClicked() {
        String phone = etUserPhone.getText().toString().trim();
        String email = etUserEmail.getText().toString().trim();
        if(typeSelector.getSelectItem()==0){
            email="";
            if(!RegexUtils.isMobileSimple(phone)){
                ToastUtils.showShort(R.string.tip_ver_phone_faild);
                return;
            }
        }else if(typeSelector.getSelectItem()==1){
            phone="";
            if(!RegexUtils.isEmail(email)){
                ToastUtils.showShort(R.string.tip_ver_email_faild);
                return;
            }
        }

        Map param = new HashMap();
        param.put("method", Params.METHOD.FRIEND_ADD);
        param.put("phone", phone);
        param.put("email", email);
        String json = new Gson().toJson(param);
        PubParam pubParam = new PubParam(App.getInstance().getUserId());
        String sign_unSha1 = pubParam.toValueString() + json + App.getInstance().getTocken();
        LogUtils.d("sign_unsha1", sign_unSha1);
        String sign = EncryptUtils.encryptSHA1ToString(sign_unSha1).toLowerCase();
        LogUtils.d("sign_sha1", sign);
        String path = HttpUrl.Api + "friend/" + pubParam.toUrlParam(sign);
        RequestBody requestBody = RequestBody.create(MediaType.parse(HttpUrl.MediaType_Json), json);

        ApiServiceManager.getInstance().buildApiService(this).postNetRequest(path, requestBody)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<BaseResponse>() {
                    @Override
                    public void accept(BaseResponse response) throws Exception {
                        if (response.isSuccessful()) {
                            ToastUtils.showLong(getResources().getString(R.string.msg_friend_add_apply));
                            FriendAddActivity.this.finish();
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
