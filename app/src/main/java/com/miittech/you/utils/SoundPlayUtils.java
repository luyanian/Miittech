package com.miittech.you.utils;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Vibrator;
import android.support.v4.app.NotificationCompat;

import com.miittech.you.App;
import com.miittech.you.R;
import com.miittech.you.global.IntentExtras;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static android.content.Context.NOTIFICATION_SERVICE;

public class SoundPlayUtils {
    // SoundPool对象
    public static SoundPool mSoundPlayer = new SoundPool(10,AudioManager.STREAM_SYSTEM, 5);
    private static Vibrator vibrator = (Vibrator)App.getInstance().getSystemService(App.getInstance().VIBRATOR_SERVICE);
    public static SoundPlayUtils soundPlayUtils;
    static Timer timer = new Timer();
    // 上下文
    static Context mContext;
    private static List<Integer> soundIds = new ArrayList<>();

    /**
     * 初始化
     * 
     * @param context
     */
    public synchronized static SoundPlayUtils init(Context context) {
        if (soundPlayUtils == null) {
            soundPlayUtils = new SoundPlayUtils();
        }
        // 初始化声音
        mContext = context;
        mSoundPlayer.load(mContext, R.raw.donotcry, 1);// 1
        mSoundPlayer.load(mContext, R.raw.theclassiccall, 1);// 2
        mSoundPlayer.load(mContext, R.raw.bluesforslim, 1);// 3
        mSoundPlayer.load(mContext, R.raw.countryfair, 1);// 4
        return soundPlayUtils;
    }

    /**
     * 播放声音
     * 
     * @param soundID
     */
    public synchronized static void playSound(int soundID) {
        stopAll();
        int playId = mSoundPlayer.play(soundID, 1, 1, 0, 0, 1);
        soundIds.add(playId);
    }
    public synchronized static void play(int soundID,int duration,boolean isShake,String devName) {
        stopAll();
        int playId = mSoundPlayer.play(soundID, 1, 1, 0, 0, 1);
        soundIds.add(playId);
        if(isShake) {
            long[] patter = {800, 800};
            vibrator.vibrate(patter, 0);
        }
        localNotify(App.getInstance().getApplicationContext(), playId,isShake,devName);
        timer.schedule(new MyTimerTask(), duration);
    }

    public synchronized static void stopAll(){
        for (int soundId : soundIds){
            mSoundPlayer.stop(soundId);
        }
        if(vibrator!=null){
            vibrator.cancel();
        }
        soundIds.clear();
    }
    public synchronized static void stop(int soundId) {
        mSoundPlayer.stop(soundId);
        if(soundIds.contains(soundId)) {
            soundIds.remove(soundId);
        }
        if(vibrator!=null){
            vibrator.cancel();
        }
    }

    static class MyTimerTask extends TimerTask {
        @Override
        public void run() {
            stopAll();
        }
    };

    private synchronized static void localNotify(Context context, int playId, boolean isShake,String devName) {
        NotificationManager manager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context);
        mBuilder.setContentTitle("有物查找手机报警中")//设置通知栏标题
                .setContentText("点击关闭报警响铃")
                .setTicker("手机报警中") //通知首次出现在通知栏，带上升动画效果的
                .setWhen(System.currentTimeMillis())//通知产生的时间，会在通知信息里显示，一般是系统获取到的时间
                .setPriority(Notification.PRIORITY_DEFAULT) //设置该通知优先级
                .setOngoing(false)//ture，设置他为一个正在进行的通知。他们通常是用来表示一个后台任务,用户积极参与(如播放音乐)或以某种方式正在等待,因此占用设备(如一个文件下载,同步操作,主动网络连接
                .setSmallIcon(R.mipmap.ic_launcher);//设置通知小ICON
        if (isShake) {
            mBuilder.setVibrate(new long[]{});
        } else{
            mBuilder.setDefaults(Notification.DEFAULT_VIBRATE);
        }

        Intent intent = new Intent(IntentExtras.ACTION.ACTION_SOUND_PLAY_ONCLICK);
        intent.putExtra("soundId", playId);
        intent.putExtra("devName",devName);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context,0,intent,0);
        mBuilder.setContentIntent(pendingIntent);
        Notification notification = mBuilder.build();
        manager.notify(1,notification);

        Intent intent1 = new Intent(IntentExtras.ACTION.ACTION_SOUND_PLAY_DIALOG);
        intent1.putExtra("soundId", 1);
        intent1.putExtra("devName",devName);

        App.getInstance().sendBroadcast(intent1);
    }

}