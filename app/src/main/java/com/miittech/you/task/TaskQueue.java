package com.miittech.you.task;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class TaskQueue {

    private BlockingQueue<ITask> mTaskQueue;
    private TaskExecutor[] mTaskExecutors;
    private AtomicInteger mAtomicInteger = new AtomicInteger();

    // 在开发者new队列的时候，要指定窗口数量。
    public TaskQueue(int size) {
        mTaskQueue = new PriorityBlockingQueue<>();
        mTaskExecutors = new TaskExecutor[size];
    }

    // 开始上班。
    public void start() {
        stop();
        // 把各个窗口都打开，让窗口开始上班。
        for (int i = 0; i < mTaskExecutors.length; i++) {
            mTaskExecutors[i] = new TaskExecutor(mTaskQueue);
            mTaskExecutors[i].start();
        }
    }



    // 统一各个窗口下班。
    public void stop() {
        if (mTaskExecutors != null)
            for (TaskExecutor taskExecutor : mTaskExecutors) {
                if (taskExecutor != null) taskExecutor.quit();
            }
    }

    // 开一个门，让办事的人能进来。
    public <T extends ITask> int add(T task) {
        if (!mTaskQueue.contains(task)) {
            task.setSequence(mAtomicInteger.incrementAndGet()); // 注意这行
            mTaskQueue.add(task);
        }
        // 返回排的队的人数，公开透明，让外面的人看的有多少人在等着办事。
        return mTaskQueue.size();
    }
}