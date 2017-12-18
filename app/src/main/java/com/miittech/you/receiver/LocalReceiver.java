package com.miittech.you.receiver;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

import com.miittech.you.App;
import com.miittech.you.R;
import com.miittech.you.activity.BaseActivity;
import com.miittech.you.common.Common;
import com.miittech.you.common.SoundPlayUtils;
import com.miittech.you.dialog.DialogUtils;
import com.miittech.you.global.IntentExtras;
import com.miittech.you.service.BleService;
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
        if(intent.getAction()== IntentExtras.ACTION.ACTION_SOUND_PLAY_ONCLICK){
            SoundPlayUtils.stopAll();
            NotificationManager notificationManager = (NotificationManager) App.getInstance().getSystemService(NOTIFICATION_SERVICE);
            notificationManager.cancel(1);
        }else if(intent.getAction()==IntentExtras.ACTION.ACTION_SOUND_PLAY_DIALOG){
            Activity activity = ActivityPools.getTopActivity();
            if(activity!=null){
                DialogUtils.getInstance().showSoundCloseDialog(activity).setSoundId(soundId);
            }
        }else if(intent.getAction()==IntentExtras.ACTION.ACTION_TASK_SEND){
            if(!ServiceUtils.isServiceRunning("BleService")){
                ServiceUtils.startService(BleService.class);
            }
            Intent unbind= new Intent(IntentExtras.ACTION.ACTION_BLE_COMMAND);
            unbind.putExtra("cmd",IntentExtras.CMD.CMD_TASK_EXCE);
            context.sendBroadcast(unbind);
        }
    }

}
