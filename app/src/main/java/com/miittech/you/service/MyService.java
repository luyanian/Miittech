package com.miittech.you.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.PowerManager;

import com.miittech.you.location.LocationClient;

public class MyService extends Service {
  
    private LocationClient locationClient;
  
    private PowerManager pm;
    private PowerManager.WakeLock wakeLock;
  
    @Override  
    public IBinder onBind(Intent intent) {
        // TODO Auto-generated method stub  
        return null;  
    }  
  
    @Override  
    public void onCreate() {  
        super.onCreate();  
  
        //创建LocationManger对象(LocationMangager，位置管理器。要想操作定位相关设备，必须先定义个LocationManager)  
        locationClient = LocationClient.getInstance();

    }  
  
    @Override  
    public void onStart(Intent intent, int startId) {  
        // TODO Auto-generated method stub  
        super.onStart(intent, startId);  
        //创建PowerManager对象  
        pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        //保持cpu一直运行，不管屏幕是否黑屏  
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "CPUKeepRunning");  
        wakeLock.acquire();  
    }  

  
    @Override  
    public void onDestroy() {
        wakeLock.release();  
        super.onDestroy();  
    }  
  
}  