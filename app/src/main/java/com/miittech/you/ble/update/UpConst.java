package com.miittech.you.ble.update;

import android.os.Environment;

import java.io.File;

/**
 * Created by Administrator on 2018/2/6.
 */

public class UpConst {
    public static final String file_blefirmware_download_path = Environment.getExternalStorageDirectory()+ File.separator+"yoowoo"+File.separator+"firmware";
    public static final int fileChunkSize = 20;
}
