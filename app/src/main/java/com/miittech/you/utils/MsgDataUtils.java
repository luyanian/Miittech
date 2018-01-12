package com.miittech.you.utils;

import com.miittech.you.dialog.MsgTipDialog;
import com.miittech.you.entity.MsgData;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrator on 2018/1/12.
 */

public class MsgDataUtils {

    List<MsgData> mlist = new ArrayList<>();
    static MsgDataUtils msgDataUtils;
    public static synchronized MsgDataUtils getInstance(){
        if(msgDataUtils==null){
            synchronized (MsgTipDialog.class){
                msgDataUtils = new MsgDataUtils();
            }
        }
        return msgDataUtils;
    }

    public synchronized void addMsg(MsgData msgData){
        synchronized (MsgTipDialog.class) {
            if (msgData != null) {
                mlist.add(msgData);
            }
        }
    }
    public synchronized boolean hasMsg(){
        synchronized (MsgDataUtils.class) {
            if (mlist.size() > 0) {
                return true;
            } else {
                return false;
            }
        }
    }

    public synchronized List<MsgData> getAllMsg(){
        synchronized (MsgTipDialog.class) {
            return mlist;
        }
    }
    public synchronized void clear(){
        synchronized (MsgTipDialog.class) {
            mlist.clear();
        }
    }
}
