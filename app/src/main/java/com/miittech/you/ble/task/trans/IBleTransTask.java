package com.miittech.you.ble.task.trans;

import com.miittech.you.ble.task.Priority;

public interface IBleTransTask extends Comparable<IBleTransTask> {
    void run();
    void setPriority(Priority priority);
    Priority getPriority();
    void setSequence(int sequence);
    int getSequence();
    String getMacAddress();
    void setIsUpdate(boolean isUpdate);
    boolean isUpdate();
}