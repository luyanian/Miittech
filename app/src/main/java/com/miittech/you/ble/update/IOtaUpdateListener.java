package com.miittech.you.ble.update;

/**
 * Created by Administrator on 2018/2/2.
 */

public interface IOtaUpdateListener {
    public void updateTitle(String title);
    public void onProgress(int progress);
    public void onError(OtaOptions options,String msg);
    public void onUpdateComplete(OtaOptions options,final String msg);
}
