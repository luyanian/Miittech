package com.miittech.you.activity.setting;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.gson.Gson;
import com.miittech.you.App;
import com.miittech.you.R;
import com.miittech.you.activity.BaseActivity;
import com.miittech.you.activity.user.MyFriendsActivity;
import com.miittech.you.activity.user.UserCenterActivity;
import com.miittech.you.common.Common;
import com.miittech.you.glide.GlideApp;
import com.miittech.you.global.SPConst;
import com.miittech.you.impl.TitleBarOptions;
import com.miittech.you.net.ApiServiceManager;
import com.miittech.you.global.HttpUrl;
import com.miittech.you.global.Params;
import com.miittech.you.global.PubParam;
import com.miittech.you.net.response.UserInfoResponse;
import com.miittech.you.weight.CircleImageView;
import com.miittech.you.weight.Titlebar;
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
 * Created by Administrator on 2017/9/14.
 */

public class SettingActivity extends BaseActivity {
    @BindView(R.id.titlebar)
    Titlebar titlebar;
    @BindView(R.id.user_header_image)
    CircleImageView userHeaderImage;
    @BindView(R.id.user_name)
    TextView userName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        ButterKnife.bind(this);
        initMyTitleBar(titlebar,getResources().getString(R.string.text_setting));
        titlebar.showBackOption();
        titlebar.setTitleBarOptions(new TitleBarOptions() {
            @Override
            public void onBack() {
                super.onBack();
                finish();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        UserInfoResponse response = (UserInfoResponse) SPUtils.getInstance().readObject(SPConst.DATA.USERINFO);
        if(response!=null){
            initData(response.getUserinfo());
        }
    }

    @OnClick({R.id.user_header_image, R.id.user_name, R.id.btn_setting_myfriends, R.id.btn_setting_ignore, R.id.btn_setting_service, R.id.btn_setting_star, R.id.btn_setting_shop, R.id.btn_setting_readme, R.id.btn_setting_feedbak,R.id.btn_setting_about_me})
    public void onViewClicked(View view) {
        Intent intent;
        switch (view.getId()) {
            case R.id.user_header_image:
            case R.id.user_name:
                intent = new Intent(SettingActivity.this, UserCenterActivity.class);
                startActivity(intent);
                break;
            case R.id.btn_setting_myfriends:
                intent = new Intent(SettingActivity.this, MyFriendsActivity.class);
                startActivity(intent);
                break;
            case R.id.btn_setting_ignore:
                intent = new Intent(SettingActivity.this,IgnoreSettingActivity.class);
                startActivity(intent);
                break;
            case R.id.btn_setting_service:
                intent = new Intent(SettingActivity.this,ServiceSettingActivity.class);
                startActivity(intent);
                break;
            case R.id.btn_setting_star:
                break;
            case R.id.btn_setting_shop:
                break;
            case R.id.btn_setting_readme:
                break;
            case R.id.btn_setting_feedbak:
                intent = new Intent(SettingActivity.this,FeedBackActivity.class);
                startActivity(intent);
                break;
            case R.id.btn_setting_about_me:
                intent = new Intent(SettingActivity.this,AboutMeActivity.class);
                startActivity(intent);
                break;
        }
    }
    private void initData(UserInfoResponse.UserinfoBean userinfo) {
        if(!TextUtils.isEmpty(userinfo.getHeadimg())){
            GlideApp.with(this)
                    .load(userinfo.getHeadimg())
                    .error(R.drawable.ic_header_img)
                    .placeholder(R.drawable.ic_header_img)
                    .into(userHeaderImage);
        }

        if(!TextUtils.isEmpty(userinfo.getNickname())){
            userName.setText(new String(android.util.Base64.decode(userinfo.getNickname(), android.util.Base64.DEFAULT)));
        }
    }
}
