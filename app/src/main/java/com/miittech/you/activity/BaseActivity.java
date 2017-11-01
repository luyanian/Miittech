package com.miittech.you.activity;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.clj.fastble.BleManager;
import com.luck.picture.lib.permissions.RxPermissions;
import com.miittech.you.receiver.BluetoothReceiver;
import com.miittech.you.weight.Titlebar;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

public class BaseActivity extends AppCompatActivity{

    public static final String DEVICE_NAME = "yoowoo";
    public static final long TIME_OUT = 30000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        initBle();
    }

    private void initBle() {
        RxPermissions rxPermissions = new RxPermissions(this);
        rxPermissions.request(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Boolean>() {
                    @Override
                    public void accept(Boolean aBoolean) throws Exception {
                        if (aBoolean) {

                        }
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