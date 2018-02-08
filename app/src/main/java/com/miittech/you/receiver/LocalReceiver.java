package com.miittech.you.receiver;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.miittech.you.App;
import com.miittech.you.entity.MsgData;
import com.miittech.you.utils.MsgDataUtils;
import com.miittech.you.utils.SoundPlayUtils;
import com.miittech.you.dialog.DialogUtils;
import com.miittech.you.global.IntentExtras;
import com.miittech.you.ble.BleService;
import com.ryon.mutils.ActivityPools;
import com.ryon.mutils.ServiceUtils;

import static android.content.Context.NOTIFICATION_SERVICE;

/**
 * Created by Administrator on 2017/11/28.
 */

public class LocalReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        int soundId = intent.getIntExtra("soundId",0);
        String devName = intent.getStringExtra("devName");
        if(intent.getAction()== IntentExtras.ACTION.ACTION_SOUND_PLAY_ONCLICK){
            SoundPlayUtils.stopAll();
            NotificationManager notificationManager = (NotificationManager) App.getInstance().getSystemService(NOTIFICATION_SERVICE);
            notificationManager.cancel(1);
        }else if(intent.getAction()==IntentExtras.ACTION.ACTION_SOUND_PLAY_DIALOG){
            Activity activity = ActivityPools.getTopActivity();
            if (activity != null) {
                DialogUtils.getInstance().showSoundCloseDialog(activity).setSoundId(soundId).setDevTitle(devName);
            }else{
                MsgData msgData = new MsgData();
                msgData.setSoundId(soundId);
                msgData.setDevName(devName);
                MsgDataUtils.getInstance().addMsg(msgData);
            }
        }else if(intent.getAction()==IntentExtras.ACTION.ACTION_TASK_SEND){
            if(!ServiceUtils.isServiceRunning("com.miittech.you.ble.BleService")){
                ServiceUtils.startService(BleService.class);
            }
            Intent task= new Intent(IntentExtras.ACTION.ACTION_BLE_COMMAND);
            task.putExtra("cmd",IntentExtras.CMD.CMD_TASK_EXCE);
            context.sendBroadcast(task);
        }
    }

}
