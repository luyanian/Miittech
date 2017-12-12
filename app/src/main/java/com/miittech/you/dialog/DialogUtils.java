package com.miittech.you.dialog;

import android.content.Context;

/**
 * Created by Administrator on 2017/9/21.
 */

public class DialogUtils {
    public static DialogUtils dialogUtils;
    public synchronized static DialogUtils getInstance(){
        if(dialogUtils==null){
            synchronized (DialogUtils.class){
                dialogUtils=new DialogUtils();
            }
        }
        return dialogUtils;
    }
    LogoutDialog logoutDialog;
    public LogoutDialog showLogoutDialog(Context context){
        if(logoutDialog!=null){
            if(logoutDialog.isShowing()){
                logoutDialog.dismiss();
            }
            logoutDialog=null;
        }
        logoutDialog = new LogoutDialog(context);
        logoutDialog.show();
        return logoutDialog;
    }
    MsgTipDialog msgTipDialog;
    public MsgTipDialog createMsgTipDialog(Context context){
        if(msgTipDialog!=null){
            if(msgTipDialog.isShowing()){
                msgTipDialog.dismiss();
            }
            msgTipDialog=null;
        }
        msgTipDialog = new MsgTipDialog(context);
        return msgTipDialog;
    }

    IgnoreAddDialog ignoreAddDialog;
    public IgnoreAddDialog showIgnoreAddDialog(Context context){
        if(ignoreAddDialog!=null){
            if(ignoreAddDialog.isShowing()){
                ignoreAddDialog.dismiss();
            }
            ignoreAddDialog=null;
        }
        ignoreAddDialog = new IgnoreAddDialog(context);
        ignoreAddDialog.show();
        return ignoreAddDialog;
    }
    SelectTimeDialog selectDialog;
    public SelectTimeDialog createSelectDialog(Context context){
        if(selectDialog!=null){
            if(selectDialog.isShowing()){
                selectDialog.dismiss();
            }
        }
        selectDialog = new SelectTimeDialog(context);
        return selectDialog;
    }

    MapDeviceUsersListDialog mapDialog;
    public MapDeviceUsersListDialog createDevicesUsersDialog(Context context){
        if(mapDialog!=null){
            if(mapDialog.isShowing()){
                mapDialog.dismiss();
            }
        }
        mapDialog = new MapDeviceUsersListDialog(context);
        return mapDialog;
    }
    SoundCloseDialog soundCloseDialog;
    public synchronized SoundCloseDialog showSoundCloseDialog(Context context) {
        if(soundCloseDialog!=null){
            if(!soundCloseDialog.isShowing()){
                soundCloseDialog.dismiss();
            }
            soundCloseDialog=null;
        }
        soundCloseDialog = new SoundCloseDialog(context);
        soundCloseDialog.show();
        return soundCloseDialog;
    }

}
