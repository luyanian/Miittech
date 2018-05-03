package com.miittech.you.ble.task.connect;

import com.miittech.you.ble.BleClient;

import java.util.concurrent.BlockingQueue;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

public class BleConnectTaskExecutor extends Thread {

    private BlockingQueue<IBleConnectTask> taskQueue;
    private boolean isRunning = true;

    public BleConnectTaskExecutor(BlockingQueue<IBleConnectTask> taskQueue) {
        this.taskQueue = taskQueue;
    }

    public void quit() {
        isRunning = false;
        interrupt();
    }

    @Override
    public void run() {
        while (isRunning) {
            try {
                sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            IBleConnectTask iTask;
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

        }
    }
}