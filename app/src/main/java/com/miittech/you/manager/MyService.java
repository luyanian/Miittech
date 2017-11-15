package com.miittech.you.manager;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;

import com.inuker.bluetooth.library.Constants;
import com.inuker.bluetooth.library.connect.response.BleConnectResponse;
import com.inuker.bluetooth.library.connect.response.BleWriteResponse;
import com.inuker.bluetooth.library.model.BleGattProfile;
import com.miittech.you.App;
import com.miittech.you.common.BleCommon;
import com.miittech.you.common.Common;
import com.miittech.you.global.IntentExtras;
import com.miittech.you.global.Params;
import com.ryon.mutils.LogUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.inuker.bluetooth.library.Constants.REQUEST_SUCCESS;
import static com.miittech.you.common.BleCommon.userCharacteristicLogUUID;
import static com.miittech.you.common.BleCommon.userServiceUUID;

public class MyService extends Service {
  
    public boolean threadFlag = true;  
    MyThread myThread;  
    CommandReceiver cmdReceiver;//继承自BroadcastReceiver对象，用于得到Activity发送过来的命令
    private List<String> mMacList = new ArrayList<>();
  
    /**************service 命令*********/   
    static final int CMD_STOP_SERVICE = 0x01;  
    static final int CMD_DEVICE_BIND = 0x02;
    static final int CMD_DEVICE_CONNECT =0x03;
    static final int CMD_DEVICE_READRSSI =0x04;
     public  boolean bluetoothFlag  = true;

      
    @Override  
    public IBinder onBind(Intent intent) {
        return null;  
    }  
  
    @Override  
    public void onCreate() {
        super.onCreate();
    }
      
      
    //前台Activity调用startService时，该方法自动执行   
    @Override  
    public int onStartCommand(Intent intent, int flags, int startId) {
        cmdReceiver = new CommandReceiver();  
        IntentFilter filter = new IntentFilter();//创建IntentFilter对象
        //注册一个广播，用于接收Activity传送过来的命令，控制Service的行为，如：发送数据，停止服务等   
        filter.addAction(IntentExtras.ACTION.ACTION_BLE_COMMAND);
        //注册Broadcast Receiver   
        registerReceiver(cmdReceiver, filter);
        doJob();//调用方法启动线程   
        return super.onStartCommand(intent, flags, startId);  
  
    }  
      
      
  
    @Override  
    public void onDestroy() {  
        // TODO Auto-generated method stub   
        super.onDestroy();  
        this.unregisterReceiver(cmdReceiver);//取消注册的CommandReceiver   
        threadFlag = false;  
        boolean retry = true;  
        while(retry){  
            try{   
                 myThread.join();  
                 retry = false;  
            }catch(Exception e){  
                e.printStackTrace();  
            }  
              
        }  
    }  
      
    public class MyThread extends Thread{          
        @Override  
        public void run() {
            super.run();  
            connectDevice();//连接蓝牙设备
        }
    }  
      
    public void doJob(){
        if (!BLEClientManager.getClient().isBluetoothOpened()) {
            LogUtils.d("蓝牙设备不可用，请打开蓝牙！");
            bluetoothFlag  = false;  
            return;  
        }
        threadFlag = true;    
        myThread = new MyThread();  
        myThread.start();
    }

    public  void connectDevice(){   
        LogUtils.d("正在尝试连接蓝牙设备，请稍后····");
        for(final String mac:mMacList){
            int status = BLEClientManager.getClient().getConnectStatus(mac);
            if(status== Constants.STATUS_DEVICE_CONNECTED||status==Constants.STATUS_DEVICE_CONNECTING){
                return;
            }else{
                BLEClientManager.getClient().connect(mac, new BleConnectResponse() {
                    @Override
                    public void onResponse(int code, BleGattProfile data) {
                        setWorkMode(mac);
                    }
                });
            }
        }
    }
    public  void connectDevice(final String mac){
        int status = BLEClientManager.getClient().getConnectStatus(mac);
        if(status== Constants.STATUS_DEVICE_CONNECTED||status==Constants.STATUS_DEVICE_CONNECTING){
            return;
        }else{
            BLEClientManager.getClient().connect(mac, new BleConnectResponse() {
                @Override
                public void onResponse(int code, BleGattProfile data) {
                    Intent intent = new Intent();
                    intent.putExtra("action",IntentExtras.ACTION.ACTION_CMD_RESPONSE);
                    if(code==Constants.REQUEST_SUCCESS){
                        intent.putExtra("ret",IntentExtras.RET.RET_DEVICE_CONNECT_SUCCESS);
                        sendBroadcast(intent);
                        setWorkMode(mac);
                    }else{
                        intent.putExtra("ret",IntentExtras.RET.RET_DEVICE_CONNECT_FAILED);
                        sendBroadcast(intent);
                    }
                }
            });
        }

    }
    public  void bindDevice(final String mac){
        int status = BLEClientManager.getClient().getConnectStatus(mac);
        if(status== Constants.STATUS_DEVICE_CONNECTED||status==Constants.STATUS_DEVICE_CONNECTING){
            return;
        }else{
            BLEClientManager.getClient().connect(mac, new BleConnectResponse() {
                @Override
                public void onResponse(int code, BleGattProfile data) {
                    Intent intent = new Intent();
                    intent.putExtra("action",IntentExtras.ACTION.ACTION_CMD_RESPONSE);
                    if(code==Constants.REQUEST_SUCCESS) {
                        intent.putExtra("ret", IntentExtras.RET.RET_DEVICE_CONNECT_SUCCESS);
                        sendBroadcast(intent);
                        setBindMode(mac);
                    }else{
                        intent.putExtra("ret", IntentExtras.RET.RET_DEVICE_CONNECT_FAILED);
                        sendBroadcast(intent);
                    }
                }
            });
        }

    }

    public void stopService(){//停止服务       
        threadFlag = false;//停止线程   
        stopSelf();//停止服务   
    }
      
     //接收Activity传送过来的命令   
    private class CommandReceiver extends BroadcastReceiver {
        @Override  
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(IntentExtras.ACTION.ACTION_BLE_COMMAND)){
                int cmd = intent.getIntExtra("cmd", -1);//获取Extra信息
                switch (cmd) {
                    case IntentExtras.CMD.CMD_DEVICE_CONNECT_BIND:
                        bindDevice(intent.getStringExtra("data"));
                        break;
                    case IntentExtras.CMD.CMD_DEVICE_CONNECT_WORK:
                        connectDevice(intent.getStringExtra("data"));
                        break;
                }
                          
            }     
        }                          
    }

    public void setWorkMode(String mac){
        byte[] dataWork = Common.formatBleMsg(Params.BLEMODE.MODE_WORK, App.getInstance().getUserId());
        BLEClientManager.getClient().write(mac, userServiceUUID, userCharacteristicLogUUID, dataWork, new BleWriteResponse() {
            @Override
            public void onResponse(int code) {
                Intent intent = new Intent();
                intent.putExtra("action",IntentExtras.ACTION.ACTION_CMD_RESPONSE);
                if(code==Constants.REQUEST_SUCCESS) {
                    intent.putExtra("ret", IntentExtras.RET.RET_DEVICE_CONNECT_WORK_SUCCESS);
                    sendBroadcast(intent);
                }else{
                    intent.putExtra("ret", IntentExtras.RET.RET_DEVICE_CONNECT_WORK_FAILED);
                    sendBroadcast(intent);
                }
            }
        });
    }
    public void setBindMode(String mac){
        byte[] bind = Common.formatBleMsg(Params.BLEMODE.MODE_BIND,App.getInstance().getUserId());
        BLEClientManager.getClient().write(mac, BleCommon.userServiceUUID, BleCommon.userCharacteristicLogUUID, bind, new BleWriteResponse() {
            @Override
            public void onResponse(int code) {
                Intent intent = new Intent();
                intent.putExtra("action",IntentExtras.ACTION.ACTION_CMD_RESPONSE);
                if(code==Constants.REQUEST_SUCCESS) {
                    intent.putExtra("ret", IntentExtras.RET.RET_DEVICE_CONNECT_BIND_SUCCESS);
                    sendBroadcast(intent);
                }else{
                    intent.putExtra("ret", IntentExtras.RET.RET_DEVICE_CONNECT_BIND_FAIL);
                    sendBroadcast(intent);
                }
            }
        });
    }
}