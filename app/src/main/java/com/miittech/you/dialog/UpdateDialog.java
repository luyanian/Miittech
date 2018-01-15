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

public class UpdateDialog extends Dialog {

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

    public UpdateDialog(@NonNull Context context,boolean canCancle) {
        super(context, R.style.DialogStyle);
        setContentView(R.layout.dialog_msg_tip);
        ButterKnife.bind(this);
        Window window = this.getWindow();
        window.getDecorView().setPadding(80, 0, 80, 0);
        WindowManager.LayoutParams attr = window.getAttributes();
        this.setCancelable(canCancle);
        this.setCanceledOnTouchOutside(canCancle);
        if (attr != null) {
            attr.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            attr.width = ViewGroup.LayoutParams.MATCH_PARENT;
            attr.gravity = Gravity.CENTER;
            window.setAttributes(attr);
        }
    }

    public UpdateDialog setTitle(String title) {
        msgTitle.setText(title);
        return this;
    }

    public UpdateDialog setMsg(String msg) {
        msgInfo.setText(msg);
        return this;
    }
    public UpdateDialog setLeftBtnText(String leftBtnText){
        btnButtonCancle.setText(leftBtnText);
        return this;
    }
    public UpdateDialog setRightBtnText(String rightBtnText){
        btnButtonSure.setText(rightBtnText);
        return this;
    }
    public UpdateDialog setOnMsgTipOptions(OnMsgTipOptions onMsgTipOptions) {
        this.onMsgTipOptions = onMsgTipOptions;
        return this;
    }

    public UpdateDialog hideLeftBtn(){
        this.btnButtonCancle.setVisibility(View.GONE);
        this.tvLines.setVisibility(View.GONE);
        return this;
    }


    @OnClick(R.id.btn_button_cancle)
    public void onViewClicked() {
        if (this.onMsgTipOptions != null) {
            this.onMsgTipOptions.onCancel();
        }
    }

    @OnClick(R.id.btn_button_sure)
    public void onBtnButtonSureClicked() {
        if(this.onMsgTipOptions!=null){
            this.onMsgTipOptions.onSure();
        }
    }
}
