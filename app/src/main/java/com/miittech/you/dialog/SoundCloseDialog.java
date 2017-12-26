package com.miittech.you.dialog;

import android.app.Dialog;
import android.app.NotificationManager;
import android.content.Context;
import android.support.annotation.NonNull;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import com.miittech.you.R;
import com.miittech.you.utils.SoundPlayUtils;

import butterknife.ButterKnife;
import butterknife.OnClick;

import static android.content.Context.NOTIFICATION_SERVICE;

/**
 * Created by Administrator on 2017/9/21.
 */

public class SoundCloseDialog extends Dialog {

    private int setSoundId=-100;
    private Context context;

    public SoundCloseDialog(@NonNull Context context) {
        super(context, R.style.DialogStyle);
        setContentView(R.layout.dialog_sound_close);
        this.context = context;
        ButterKnife.bind(this);
        Window window = this.getWindow();
        window.getDecorView().setPadding(80, 0, 80, 0);
        WindowManager.LayoutParams attr = window.getAttributes();
        if (attr != null) {
            attr.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            attr.width = ViewGroup.LayoutParams.MATCH_PARENT;
            attr.gravity = Gravity.CENTER;
            window.setAttributes(attr);
        }
    }

    public SoundCloseDialog setSoundId(int setSoundId) {
        this.setSoundId = setSoundId;
        return this;
    }

    @OnClick(R.id.ll_btn_sure)
    public void onViewClicked() {
        if(setSoundId!=-100){
            SoundPlayUtils.stopAll();
            NotificationManager notificationManager = (NotificationManager)context.getSystemService(NOTIFICATION_SERVICE);
            notificationManager.cancel(1);
            if(this.isShowing()){
                this.dismiss();
            }
        }
    }
}
