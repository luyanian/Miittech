package com.miittech.you.activity.user;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.View;

import com.miittech.you.App;
import com.miittech.you.R;
import com.miittech.you.activity.BaseActivity;
import com.miittech.you.activity.MainActivity;
import com.ryon.mutils.ActivityPools;

import butterknife.ButterKnife;
import butterknife.OnClick;

public class LoginRegisteActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_registe);
        ButterKnife.bind(this);
        if(!TextUtils.isEmpty(App.getInstance().getTocken())){
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    Intent intent = new Intent(LoginRegisteActivity.this, MainActivity.class);
                    startActivity(intent);
                }
            },2000);
        }
    }

    @OnClick({R.id.btn_play, R.id.btn_register, R.id.btn_login})
    public void onViewClicked(View view) {
        Intent intent;
        switch (view.getId()) {
            case R.id.btn_play:
                // TODO: 2017/9/13 登陆页播放视频
                break;
            case R.id.btn_register:
                intent = new Intent(this,RegisteActivity.class);
                startActivity(intent);
                ActivityPools.finishAllExcept(LoginRegisteActivity.class);
                break;
            case R.id.btn_login:
                intent = new Intent(this,LoginActivity.class);
                startActivity(intent);
                ActivityPools.finishAllExcept(LoginRegisteActivity.class);
                break;
        }
    }
}
