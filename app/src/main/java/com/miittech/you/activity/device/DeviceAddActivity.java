package com.miittech.you.activity.device;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.miittech.you.R;
import com.miittech.you.activity.BaseActivity;
import com.miittech.you.impl.TitleBarOptions;
import com.miittech.you.weight.Titlebar;
import com.ryon.mutils.ToastUtils;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by Administrator on 2017/10/17.
 */

public class DeviceAddActivity extends BaseActivity {
    @BindView(R.id.titlebar)
    Titlebar titlebar;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_add);
        ButterKnife.bind(this);

        initTitleBar(titlebar, R.string.text_logo);
        titlebar.showBackOption();
        titlebar.setTitleBarOptions(new TitleBarOptions() {
            @Override
            public void onBack() {
                super.onBack();
                finish();
            }
        });
    }

    @OnClick(R.id.rl_device)
    public void onRlDeviceClicked() {
        Intent intent = new Intent(this,DeviceAddStepActivity.class);
        startActivity(intent);
    }

    @OnClick(R.id.btn_buy)
    public void onBtnBuyClicked() {
        ToastUtils.showShort("功能开发测试中，敬请期待！");
    }
}
