package com.miittech.you.activity.setting;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.miittech.you.R;
import com.miittech.you.activity.BaseActivity;
import com.miittech.you.activity.user.UserCenterActivity;
import com.miittech.you.impl.TitleBarOptions;
import com.miittech.you.weight.Titlebar;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by ryon on 2017/11/26.
 */

public class AboutMeActivity extends BaseActivity {
    @BindView(R.id.titlebar)
    Titlebar titlebar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ablout_me);
        ButterKnife.bind(this);
        initMyTitleBar(titlebar,"关于我们");
        titlebar.showBackOption();
        titlebar.setTitleBarOptions(new TitleBarOptions(){
            @Override
            public void onBack() {
                super.onBack();
                finish();
            }
        });
    }

    @OnClick({R.id.btn_privacy_protocols, R.id.btn_use_terms})
    public void onViewClicked(View view) {
        Intent intent = new Intent();
        switch (view.getId()) {
            case R.id.btn_privacy_protocols:
                intent.setClass(this,PrivacyProtocolsActivity.class);
                startActivity(intent);
                break;
            case R.id.btn_use_terms:
                intent.setClass(this,UseTermsActivity.class);
                startActivity(intent);
                break;
        }
    }
}
