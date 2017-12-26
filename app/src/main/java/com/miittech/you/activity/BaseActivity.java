package com.miittech.you.activity;

import android.content.pm.ActivityInfo;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;

import com.miittech.you.R;
import com.miittech.you.utils.Common;
import com.miittech.you.weight.Titlebar;

public class BaseActivity extends AppCompatActivity{



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }


    public void initMyTitleBar(Titlebar titlebar){
        initMyTitleBar(titlebar,null) ;
    }
    public void initMyTitleBar(Titlebar titlebar, int strId){
        initMyTitleBar(titlebar,getResources().getString(strId)) ;
    }

    public void initMyTitleBar(Titlebar titlebar, String title){
        if(!TextUtils.isEmpty(title)) {
            titlebar.setTitle(title);
        }else{
            titlebar.setLogo(R.drawable.logo);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
             int statusBarHeight1 = -1;
            //获取status_bar_height资源的ID
            int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
            if (resourceId > 0) {
                //根据资源ID获取响应的尺寸值
                statusBarHeight1 = getResources().getDimensionPixelSize(resourceId);
            }
            titlebar.setTopPadding(statusBarHeight1);
        }
    }
}