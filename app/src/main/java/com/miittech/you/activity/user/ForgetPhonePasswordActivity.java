package com.miittech.you.activity.user;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.miittech.you.R;
import com.miittech.you.activity.BaseActivity;
import com.miittech.you.utils.Common;
import com.miittech.you.impl.OnGetVerCodeComplete;
import com.miittech.you.impl.TitleBarOptions;
import com.miittech.you.utils.CountDownTimerUtils;
import com.miittech.you.weight.Titlebar;
import com.ryon.mutils.RegexUtils;
import com.ryon.mutils.ToastUtils;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by Administrator on 2017/12/18.
 */

public class ForgetPhonePasswordActivity extends BaseActivity {
    @BindView(R.id.titlebar)
    Titlebar titlebar;
    @BindView(R.id.et_user_phone)
    EditText etUserPhone;
    @BindView(R.id.et_user_vercode)
    EditText etUserVercode;
    @BindView(R.id.btn_get_code)
    TextView btnGetCode;
    private String cliendid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone_password_forget);
        ButterKnife.bind(this);
        initMyTitleBar(titlebar, "验证手机");
        titlebar.showBackOption();
        titlebar.setTitleBarOptions(new TitleBarOptions() {
            @Override
            public void onBack() {
                super.onBack();
                finish();
            }
        });
    }

    @OnClick({R.id.btn_get_code, R.id.btn_ok,R.id.tv_email_forget})
    public void onViewClicked(View view) {
        String phone = etUserPhone.getText().toString().trim();
        switch (view.getId()) {
            case R.id.btn_get_code:
                Common.getMsgCode(this, phone, new OnGetVerCodeComplete() {
                    @Override
                    public void onRequestStart() {
                        CountDownTimerUtils mCountDownTimerUtils = new CountDownTimerUtils(btnGetCode, 60000, 1000);
                        mCountDownTimerUtils.start();
                    }

                    @Override
                    public void onSuccessful(String cliendid) {
                        ForgetPhonePasswordActivity.this.cliendid = cliendid;
                    }
                });
                break;
            case R.id.btn_ok:
                String verCode = etUserVercode.getText().toString().trim();
                if (!RegexUtils.isMobileSimple(phone)) {
                    ToastUtils.showShort(R.string.tip_ver_phone_faild);
                    return;
                }
                if (TextUtils.isEmpty(verCode)) {
                    ToastUtils.showShort("请输入收到的短信验证码");
                    return;
                }
                Intent intent = new Intent(this, ResetPasswordActivity.class);
                intent.putExtra("phone", phone);
                intent.putExtra("verCode", verCode);
                intent.putExtra("cliendId", cliendid);
                startActivity(intent);
                break;
            case R.id.tv_email_forget:
                Intent intent1 = new Intent(this,ForgetEmailPasswordActivity.class);
                startActivity(intent1);
                finish();
                break;
        }
    }
}
