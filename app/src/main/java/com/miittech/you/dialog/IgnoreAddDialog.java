package com.miittech.you.dialog;

import android.app.Dialog;
import android.content.Context;
import android.support.annotation.NonNull;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import com.miittech.you.R;
import com.miittech.you.impl.OnIgnoreAddOptions;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by Administrator on 2017/9/21.
 */

public class IgnoreAddDialog extends Dialog {
    OnIgnoreAddOptions ignoreAddOptions;

    public IgnoreAddDialog(@NonNull Context context) {
        super(context, R.style.DialogStyle);
        setContentView(R.layout.dialog_ignore_add);
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

    public void setIgnoreAddOptions(OnIgnoreAddOptions ignoreAddOptions) {
        this.ignoreAddOptions = ignoreAddOptions;
    }



    @OnClick(R.id.btn_button_cancle)
    public void onBtnButtonCancleClicked() {
        if (this.ignoreAddOptions != null) {
            this.ignoreAddOptions.cancle();
        }
        dismiss();
    }

    @OnClick(R.id.btn_ignore_point_add)
    public void onBtnIgnorePointAddClicked() {
        if (this.ignoreAddOptions != null) {
            this.ignoreAddOptions.addPointIgnore();
        }
    }

    @OnClick(R.id.btn_ignore_wifi_add)
    public void onBtnIgnoreWifiAddClicked() {
        if (this.ignoreAddOptions != null) {
            this.ignoreAddOptions.addWifiIgnore();
        }
    }
}
