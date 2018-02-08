package com.miittech.you.dialog;

import android.app.Dialog;
import android.content.Context;
import android.support.annotation.NonNull;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.miittech.you.R;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by Administrator on 2017/9/21.
 */

public class ProgressDialog extends Dialog {

    @BindView(R.id.progress_title)
    TextView progressTitle;
    @BindView(R.id.progress_bar)
    ProgressBar progressBar;

    public ProgressDialog(@NonNull Context context) {
        super(context, R.style.DialogStyle);
        setContentView(R.layout.dialog_progress);
        ButterKnife.bind(this);
        Window window = this.getWindow();
        window.getDecorView().setPadding(80, 0, 80, 0);
        WindowManager.LayoutParams attr = window.getAttributes();
        this.setCancelable(false);
        this.setCanceledOnTouchOutside(false);
        if (attr != null) {
            attr.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            attr.width = ViewGroup.LayoutParams.MATCH_PARENT;
            attr.gravity = Gravity.CENTER;
            window.setAttributes(attr);
        }
    }

    public ProgressDialog setTitle(String title) {
        progressTitle.setText(title);
        return this;
    }

    public ProgressBar getProgressBar() {
        return this.progressBar;
    }
}
