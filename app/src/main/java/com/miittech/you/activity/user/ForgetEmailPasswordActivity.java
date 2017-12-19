package com.miittech.you.activity.user;

import android.os.Bundle;
import android.widget.EditText;

import com.miittech.you.R;
import com.miittech.you.activity.BaseActivity;
import com.miittech.you.impl.TitleBarOptions;
import com.miittech.you.weight.Titlebar;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by Administrator on 2017/12/18.
 */

public class ForgetEmailPasswordActivity extends BaseActivity {
    @BindView(R.id.titlebar)
    Titlebar titlebar;
    @BindView(R.id.et_user_email)
    EditText etUserEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_email_password_forget);
        ButterKnife.bind(this);
        initMyTitleBar(titlebar,"验证邮箱");
        titlebar.showBackOption();
        titlebar.setTitleBarOptions(new TitleBarOptions(){
            @Override
            public void onBack() {
                super.onBack();
                finish();
            }
        });
    }

    @OnClick(R.id.btn_ok)
    public void onViewClicked() {
    }
}
