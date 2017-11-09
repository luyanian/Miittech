package com.miittech.you.activity;

import android.content.pm.ActivityInfo;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.inuker.bluetooth.library.connect.listener.BluetoothStateListener;
import com.miittech.you.ble.ClientManager;
import com.miittech.you.weight.Titlebar;

public class BaseActivity extends AppCompatActivity{

    public static final String DEVICE_NAME = "yoowoo";
    public static final long TIME_OUT = 30000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        ClientManager.getInstance().getClient().registerBluetoothStateListener(new BluetoothStateListener() {
            @Override
            public void onBluetoothStateChanged(boolean openOrClosed) {

            }
        });
    }



    public void initTitleBar(Titlebar titlebar,int strId){
        initTitleBar(titlebar,getResources().getString(strId)) ;
    }

    public void initTitleBar(Titlebar titlebar,String title){
        titlebar.setTitle(title);
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