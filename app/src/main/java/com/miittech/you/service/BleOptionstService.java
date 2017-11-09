package com.miittech.you.service;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

public class BleOptionstService extends Service {
    private static final String TAG = "BleOptionstService";
    MediaPlayer player;  
  
    @Override  
    public IBinder onBind(Intent intent) {  
        return null;  
    }  
  
    @Override  
    public void onCreate() {  
        Toast.makeText(this, "BleOptionstService Service created", Toast.LENGTH_LONG).show();
        Log.i(TAG, "onCreate");  
  
//        player = MediaPlayer.create(this, R.raw.braincandy);
//        player.setLooping(false);
    }  
  
    @Override  
    public void onDestroy() {  
        Toast.makeText(this, "BleOptionstService Service Stoped", Toast.LENGTH_LONG).show();
        Log.i(TAG, "onDestroy");  
        player.stop();  
    }  
  
    @Override  
    public void onStart(Intent intent, int startid) {  
        Toast.makeText(this, "BleOptionstService Service Start", Toast.LENGTH_LONG).show();
        Log.i(TAG, "onStart");  
        player.start();  
    }  
}  