package com.miittech.you.activity.setting;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.CheckBox;
import com.miittech.you.R;
import com.miittech.you.activity.BaseActivity;
import com.miittech.you.ble.BleClient;
import com.miittech.you.dialog.DialogUtils;
import com.miittech.you.dialog.MsgTipDialog;
import com.miittech.you.impl.OnMsgTipOptions;
import com.miittech.you.impl.TitleBarOptions;
import com.miittech.you.utils.Common;
import com.miittech.you.weight.Titlebar;
import com.ryon.mutils.AppUtils;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by Administrator on 2017/9/28.
 */

public class ServiceSettingActivity extends BaseActivity{
    @BindView(R.id.titlebar)
    Titlebar titlebar;
    @BindView(R.id.check_bluetooth)
    CheckBox checkBluetooth;
    @BindView(R.id.check_lcoation)
    CheckBox checkLcoation;
    @BindView(R.id.check_notification)
    CheckBox checkNotification;
    @BindView(R.id.check_background)
    CheckBox checkBackground;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting_service);
        ButterKnife.bind(this);

        initMyTitleBar(titlebar, R.string.text_setting_service);
        titlebar.showBackOption();
        titlebar.setTitleBarOptions(new TitleBarOptions() {
            @Override
            public void onBack() {
                super.onBack();
                finish();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (BleClient.getInstance().isEnabled()) {
            checkBluetooth.setChecked(true);
        } else {
            checkBluetooth.setChecked(false);
        }
        if (Common.isLocationEnabled()) {
            checkLcoation.setChecked(true);
        } else {
            checkLcoation.setChecked(false);
        }
        if (Common.isNotificationEnabled()) {
            checkNotification.setChecked(true);
        }else{
            checkNotification.setChecked(false);
        }
    }


    @OnClick({R.id.check_bluetooth, R.id.check_lcoation, R.id.check_notification})
    public void onViewClicked(View view) {
        Intent intent;
        MsgTipDialog msgTipDialog = DialogUtils.getInstance().createMsgTipDialog(this);
        switch (view.getId()) {
            case R.id.check_bluetooth:
                if (!checkBluetooth.isChecked()) {
                    msgTipDialog.setTitle("蓝牙服务已关闭").setMsg("关闭蓝牙服务后，贴片将不能与您的手机连接，是否确定关闭");
                    msgTipDialog.setOnMsgTipOptions(new OnMsgTipOptions() {
                        @Override
                        public void onSure() {
                            super.onSure();
                            checkBluetooth.setChecked(false);
                            BleClient.getInstance().disableBluetooth();
                        }

                        @Override
                        public void onCancel() {
                            super.onCancel();
                            checkBluetooth.setChecked(true);
                        }
                    });
                    msgTipDialog.show();
                } else {
                    checkBluetooth.setChecked(true);
                    BleClient.getInstance().enaableBluetooth();
                }
                break;
            case R.id.check_lcoation:
                if (!checkLcoation.isChecked()) {
                    msgTipDialog.setTitle("定位服务已关闭").setMsg("关闭定位服务后，将不能获取您的设备位置，是否确定关闭");
                    msgTipDialog.setOnMsgTipOptions(new OnMsgTipOptions() {
                        @Override
                        public void onSure() {
                            super.onSure();
                            Intent intent1 = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                            startActivity(intent1);
                        }

                        @Override
                        public void onCancel() {
                            super.onCancel();
                            checkLcoation.setChecked(true);
                        }
                    });
                    msgTipDialog.show();
                } else {
                    intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(intent);
                }
                break;
            case R.id.check_notification:
                if (!checkNotification.isChecked()) {
                    msgTipDialog.setTitle("通知服务已关闭").setMsg("关闭通知服务后，贴片将无法给您发送消息，是否确定关闭");
                    msgTipDialog.setOnMsgTipOptions(new OnMsgTipOptions() {
                        @Override
                        public void onSure() {
                            super.onSure();
                            Uri packageURI = Uri.parse("package:" + AppUtils.getAppPackageName());
                            Intent intent1 = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageURI);
                            startActivity(intent1);
                        }

                        @Override
                        public void onCancel() {
                            super.onCancel();
                            checkNotification.setChecked(true);
                        }
                    });
                    msgTipDialog.show();
                } else {
                    Uri packageURI = Uri.parse("package:" + AppUtils.getAppPackageName());
                    intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageURI);
                    startActivity(intent);
                }
                break;
        }
    }
}
