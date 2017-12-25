package com.miittech.you.activity;

import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;

import com.miittech.you.R;
import com.miittech.you.activity.user.LoginRegisteActivity;
import com.miittech.you.common.Common;

public class StartActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent();
                if(!TextUtils.isEmpty(Common.getTocken())){
                    intent.setClass(StartActivity.this, MainActivity.class);
                }else{
                    intent.setClass(StartActivity.this, LoginRegisteActivity.class);
                }
                startActivity(intent);
                finish();
            }
        },3000);
    }
}
