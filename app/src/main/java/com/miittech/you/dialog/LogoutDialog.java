package com.miittech.you.dialog;

import android.app.Dialog;
import android.content.Context;
import android.support.annotation.NonNull;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import com.miittech.you.R;
import com.miittech.you.impl.OnMsgTipOptions;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by Administrator on 2017/9/21.
 */

public class LogoutDialog extends Dialog {
    @BindView(R.id.btn_button_sure)
    TextView btnButtonSure;

    public LogoutDialog(@NonNull Context context) {
        super(context,R.style.DialogStyle);
        setContentView(R.layout.dialog_logout);
        ButterKnife.bind(this);
        Window window = this.getWindow();
        window.getDecorView().setPadding(20, 0, 20, 20);
        WindowManager.LayoutParams attr = window.getAttributes();
        if (attr != null) {
            attr.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            attr.width = ViewGroup.LayoutParams.MATCH_PARENT;
            attr.gravity = Gravity.BOTTOM;
            window.setAttributes(attr);
        }
    }

    public void onClickSure(final OnMsgTipOptions onMsgTipOptions) {
        btnButtonSure.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(onMsgTipOptions!=null){
                    onMsgTipOptions.onSure();
                }
                dismiss();
            }
        });
    }

    @OnClick(R.id.btn_button_cancle)
    public void onViewClicked() {
        this.dismiss();
    }
}
