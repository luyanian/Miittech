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

public class MsgTipDialog extends Dialog {

    @BindView(R.id.msg_title)
    TextView msgTitle;
    @BindView(R.id.msg_info)
    TextView msgInfo;
    @BindView(R.id.btn_button_sure)
    TextView btnButtonSure;
    @BindView(R.id.tv_lines)
    TextView tvLines;
    @BindView(R.id.btn_button_cancle)
    TextView btnButtonCancle;
    private OnMsgTipOptions onMsgTipOptions;

    public MsgTipDialog(@NonNull Context context) {
        super(context, R.style.DialogStyle);
        setContentView(R.layout.dialog_msg_tip);
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

    public MsgTipDialog setTitle(String title) {
        msgTitle.setText(title);
        return this;
    }

    public MsgTipDialog setMsg(String msg) {
        msgInfo.setText(msg);
        return this;
    }
    public MsgTipDialog setLeftBtnText(String leftBtnText){
        btnButtonCancle.setText(leftBtnText);
        return this;
    }
    public MsgTipDialog setRightBtnText(String rightBtnText){
        btnButtonSure.setText(rightBtnText);
        return this;
    }
    public MsgTipDialog setOnMsgTipOptions(OnMsgTipOptions onMsgTipOptions) {
        this.onMsgTipOptions = onMsgTipOptions;
        return this;
    }

    public MsgTipDialog hideLeftBtn(){
        this.btnButtonCancle.setVisibility(View.GONE);
        this.tvLines.setVisibility(View.GONE);
        return this;
    }


    @OnClick(R.id.btn_button_cancle)
    public void onViewClicked() {
        if (this.onMsgTipOptions != null) {
            this.onMsgTipOptions.onCancel();
        }
        this.dismiss();
    }

    @OnClick(R.id.btn_button_sure)
    public void onBtnButtonSureClicked() {
        if(this.onMsgTipOptions!=null){
            this.onMsgTipOptions.onSure();
        }
        this.dismiss();
    }
}
