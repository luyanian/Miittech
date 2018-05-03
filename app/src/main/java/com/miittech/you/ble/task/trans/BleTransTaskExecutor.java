package com.miittech.you.ble.task.trans;

import com.miittech.you.ble.BleClient;
import com.miittech.you.ble.task.connect.BleConnectTask;
import com.ryon.mutils.LogUtils;

import java.util.concurrent.BlockingQueue;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

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
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }else{
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}