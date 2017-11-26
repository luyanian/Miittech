package com.miittech.you.activity.setting;

import android.os.Bundle;

import com.miittech.you.R;
import com.miittech.you.activity.BaseActivity;
import com.miittech.you.impl.TitleBarOptions;
import com.miittech.you.weight.Titlebar;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by ryon on 2017/11/26.
 */

public class PrivacyProtocolsActivity extends BaseActivity {
    @BindView(R.id.titlebar)
    Titlebar titlebar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_privacy_protocols);
        ButterKnife.bind(this);
        initMyTitleBar(titlebar,"隐私协议");
        titlebar.showBackOption();
        titlebar.setTitleBarOptions(new TitleBarOptions(){
            @Override
            public void onBack() {
                super.onBack();
                finish();
            }
        });
    }
}
