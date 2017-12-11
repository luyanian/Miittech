package com.miittech.you.dialog;

import android.content.Context;

/**
 * Created by Administrator on 2017/9/21.
 */

public class DialogUtils {
    private static DialogUtils dialogUtils;
    public static DialogUtils getInstance(){
        if(dialogUtils==null){
            synchronized (DialogUtils.class) {
                dialogUtils = new DialogUtils();
            }
        }
        return dialogUtils;
    }
    static LogoutDialog logoutDialog;
    public static LogoutDialog showLogoutDialog(Context context){
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
    static MsgTipDialog msgTipDialog;
    public static MsgTipDialog createMsgTipDialog(Context context){
        if(msgTipDialog==null){
            msgTipDialog = new MsgTipDialog(context);
        }else{
            if(msgTipDialog.isShowing()){
                msgTipDialog.dismiss();
            }
        }
        return msgTipDialog;
    }

    static IgnoreAddDialog ignoreAddDialog;
    public static IgnoreAddDialog showIgnoreAddDialog(Context context){
        if(ignoreAddDialog==null){
            ignoreAddDialog = new IgnoreAddDialog(context);
            ignoreAddDialog.show();
        }else{
            if(ignoreAddDialog.isShowing()){
                ignoreAddDialog.dismiss();
            }
        }
        return ignoreAddDialog;
    }
    static SelectTimeDialog selectDialog;
    public static SelectTimeDialog createSelectDialog(Context context){
        if(selectDialog==null){
            selectDialog = new SelectTimeDialog(context);
        }else{
            if(selectDialog.isShowing()){
                selectDialog.dismiss();
            }
        }
        return selectDialog;
    }

    static MapDeviceUsersListDialog mapDialog;
    public static MapDeviceUsersListDialog createDevicesUsersDialog(Context context){
        if(mapDialog==null){
            mapDialog = new MapDeviceUsersListDialog(context);
        }else{
            if(mapDialog.isShowing()){
                mapDialog.dismiss();
            }
        }
        return mapDialog;
    }
    static SoundCloseDialog soundCloseDialog;
    public static SoundCloseDialog showSoundCloseDialog(Context context) {
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
