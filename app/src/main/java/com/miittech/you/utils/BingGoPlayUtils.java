package com.miittech.you.utils;

import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;

import com.miittech.you.R;

import java.util.ArrayList;
import java.util.List;

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