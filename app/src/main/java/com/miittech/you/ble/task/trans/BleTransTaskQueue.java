package com.miittech.you.ble.task.trans;

import android.support.v4.util.SimpleArrayMap;

import com.miittech.you.ble.task.Priority;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class BleTransTaskQueue {

    private SimpleArrayMap<String,BlockingQueue<IBleTransTask>> mapTaskQueue = new SimpleArrayMap<>();
    private SimpleArrayMap<String,BleTransTaskExecutor> mapTaskExecutor = new SimpleArrayMap<>();
    private AtomicInteger mAtomicInteger = new AtomicInteger();



    // 统一各个窗口下班。
    public void stop() {
        for (int i=0;i<mapTaskExecutor.size();i++){
            BleTransTaskExecutor mTaskExecutor = mapTaskExecutor.get(i);
            if(mTaskExecutor!=null) {
                mTaskExecutor.quit();
            }
        }
    }

    // 开一个门，让办事的人能进来。
    public <T extends IBleTransTask> int add(T task) {
        BlockingQueue mTaskQueue;
        BleTransTaskExecutor bleTransTaskExecutor;
        if(mapTaskQueue.containsKey(task.getMacAddress())&&mapTaskQueue.get(task.getMacAddress())!=null){
            mTaskQueue = mapTaskQueue.get(task.getMacAddress());

        }else{
            mTaskQueue = new PriorityBlockingQueue<>();
            mapTaskQueue.put(task.getMacAddress(),mTaskQueue);
        }
        if(!mapTaskExecutor.containsKey(task.getMacAddress())||mapTaskExecutor.get(task.getMacAddress())==null){
            bleTransTaskExecutor = new BleTransTaskExecutor(mTaskQueue);
            mapTaskExecutor.put(task.getMacAddress(),bleTransTaskExecutor);
            bleTransTaskExecutor.start();
        }

        if(!mTaskQueue.contains(task)) {
            task.setSequence(mAtomicInteger.incrementAndGet()); // 注意这行
            if (!task.isUpdate()) {
                task.setPriority(Priority.LOW);
            }
            mTaskQueue.add(task);
        }

        // 返回排的队的人数，公开透明，让外面的人看的有多少人在等着办事。
        return mTaskQueue.size();
    }
}