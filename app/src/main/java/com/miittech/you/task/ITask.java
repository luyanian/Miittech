package com.miittech.you.task;

public interface ITask extends Comparable<ITask> {
    void run();
    void setPriority(Priority priority);
    Priority getPriority();
    void setSequence(int sequence);
    int getSequence();
}