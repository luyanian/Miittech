package com.miittech.you.ble.update;

/**
 * Created by Administrator on 2018/2/2.
 */

public interface IOtaUpdateListener {
    public void updateTitle(String title);
    public void onProgress(int progress);
    public void onError(String msg);
    public void onUpdateComplete();
}
