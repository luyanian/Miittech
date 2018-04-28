package com.miittech.you.ble.task.connect;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class BleConnectTaskQueue {

    private BlockingQueue<IBleConnectTask> mTaskQueue;
    private BleConnectTaskExecutor mTaskExecutor;
    private AtomicInteger mAtomicInteger = new AtomicInteger();

    // 在开发者new队列的时候，要指定窗口数量。
    public BleConnectTaskQueue() {
        mTaskQueue = new PriorityBlockingQueue<>();
    }

    // 开始上班。
    public void start() {
        stop();
        // 把各个窗口都打开，让窗口开始上班。
        mTaskExecutor = new BleConnectTaskExecutor(mTaskQueue);
        mTaskExecutor.start();
    }



    // 统一各个窗口下班。
    public void stop() {
        if(mTaskExecutor!=null) {
            mTaskExecutor.quit();
        }
    }

    // 开一个门，让办事的人能进来。
    public <T extends IBleConnectTask> int add(T task) {
        synchronized (BleConnectTaskQueue.this) {
            for (IBleConnectTask x : mTaskQueue) {
                if (x.getMac().equals(task.getMac())) {
                    return x.getSequence();
                }
            }

            if (!mTaskQueue.contains(task)) {
                task.setSequence(mAtomicInteger.incrementAndGet()); // 注意这行
                mTaskQueue.add(task);
            }
        // 返回排的队的人数，公开透明，让外面的人看的有多少人在等着办事。
            return mTaskQueue.size();
        }
    }
}