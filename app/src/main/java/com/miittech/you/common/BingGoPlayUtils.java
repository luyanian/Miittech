package com.miittech.you.common;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.SoundPool;
import android.support.v4.app.NotificationCompat;

import com.miittech.you.App;
import com.miittech.you.R;
import com.miittech.you.global.IntentExtras;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static android.content.Context.NOTIFICATION_SERVICE;

public class BingGoPlayUtils {
    // SoundPool对象
    public static SoundPool mSoundPlayer = new SoundPool(10,AudioManager.STREAM_SYSTEM, 5);
    public static BingGoPlayUtils soundPlayUtils;

    // 上下文
    static Context mContext;
    private static List<Integer> soundIds = new ArrayList<>();

    /**
     * 初始化
     * 
     * @param context
     */
    public static BingGoPlayUtils init(Context context) {
        if (soundPlayUtils == null) {
            soundPlayUtils = new BingGoPlayUtils();
        }
        // 初始化声音
        mContext = context;
        mSoundPlayer.load(mContext, R.raw.bingo, 1);// 1
        return soundPlayUtils;
    }

    /**
     * 播放声音
     */
    public static void playBingGo() {
        int playId = mSoundPlayer.play(1, 1, 1, 0, 0, 1);
    }
}