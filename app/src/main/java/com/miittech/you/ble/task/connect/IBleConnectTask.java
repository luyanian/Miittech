package com.miittech.you.ble.task.connect;

import com.miittech.you.ble.task.Priority;

public interface IBleConnectTask extends Comparable<IBleConnectTask> {
    void run();
    void setPriority(Priority priority);
    Priority getPriority();
    void setSequence(int sequence);
    int getSequence();

    String getMac();
}