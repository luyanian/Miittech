package com.miittech.you.ble.task.trans;

import java.util.concurrent.BlockingQueue;

public class BleTransTaskExecutor extends Thread {

    private BlockingQueue<IBleTransTask> taskQueue;
    private boolean isRunning = true;

    public BleTransTaskExecutor(BlockingQueue<IBleTransTask> taskQueue) {
        this.taskQueue = taskQueue;
    }

    public void quit() {
        isRunning = false;
        interrupt();
    }

    @Override
    public void run() {
        while (isRunning) {
            IBleTransTask iTask;
            try {
                iTask = taskQueue.take();
            } catch (InterruptedException e) {
                if (!isRunning) {
                    interrupt();
                    break;
                }
                continue;
            }
            iTask.run();
            if(iTask.isUpdate()) {
                try {
                    Thread.sleep(30);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }else {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}