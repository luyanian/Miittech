package com.miittech.you.activity.setting;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import com.miittech.you.R;
import com.miittech.you.activity.BaseActivity;
import com.miittech.you.manager.BLEManager;
import com.miittech.you.dialog.DialogUtils;
import com.miittech.you.dialog.MsgTipDialog;
import com.miittech.you.impl.OnMsgTipOptions;
import com.miittech.you.weight.Titlebar;
import com.miittech.you.impl.TitleBarOptions;
import com.ryon.mutils.AppUtils;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by Administrator on 2017/9/28.
 */

public class ServiceSettingActivity extends BaseActivity implements CompoundButton.OnCheckedChangeListener {
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

        initMyTitleBar(titlebar,R.string.text_setting_service);
        titlebar.showBackOption();
        titlebar.setTitleBarOptions(new TitleBarOptions(){
            @Override
            public void onBack() {
                super.onBack();
                finish();
            }
        });
        checkBluetooth.setOnCheckedChangeListener(this);
        checkLcoation.setOnCheckedChangeListener(this);
        checkNotification.setOnCheckedChangeListener(this);
        checkBackground.setOnCheckedChangeListener(this);
    }

    @OnClick(R.id.btn_ok)
    public void onViewClicked() {
    }


    @Override
    public void onCheckedChanged(final CompoundButton compoundButton, boolean b) {
        Intent intent;
        MsgTipDialog msgTipDialog = DialogUtils.getInstance().createMsgTipDialog(this);
        switch (compoundButton.getId()){
            case R.id.check_bluetooth:
                if(b){
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    enableBtIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    ServiceSettingActivity.this.startActivity(enableBtIntent);
                }else{
                    msgTipDialog.setTitle("蓝牙服务已关闭").setMsg("关闭蓝牙服务后，贴片将不能与您的手机连接，是否确定关闭");
                    msgTipDialog.setOnMsgTipOptions(new OnMsgTipOptions() {
                        @Override
                        public void onSure() {
                            super.onSure();
                            BLEManager.getInstance().closeBluetooth();
                        }

                        @Override
                        public void onCancel() {
                            super.onCancel();
                            compoundButton.setChecked(true);
                        }
                    });
                    msgTipDialog.show();
                }

                break;
            case R.id.check_lcoation:
                if(b){
                    intent =  new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(intent);
                }else{
                    msgTipDialog.setTitle("定位服务已关闭").setMsg("关闭定位服务后，将不能获取您的设备位置，是否确定关闭");
                    msgTipDialog.setOnMsgTipOptions(new OnMsgTipOptions() {
                        @Override
                        public void onSure() {
                            super.onSure();
                            Intent intent1 =  new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                            startActivity(intent1);
                        }
                    });
                    msgTipDialog.show();
                }

                break;
            case R.id.check_notification:
                if(b){
                    Uri packageURI = Uri.parse("package:" + AppUtils.getAppPackageName());
                    intent =  new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,packageURI);
                    startActivity(intent);
                }else{
                    msgTipDialog.setTitle("通知服务已关闭").setMsg("关闭通知服务后，贴片将无法给您发送消息，是否确定关闭");
                    msgTipDialog.setOnMsgTipOptions(new OnMsgTipOptions() {
                        @Override
                        public void onSure() {
                            super.onSure();
                            Uri packageURI = Uri.parse("package:" + AppUtils.getAppPackageName());
                            Intent intent1 =  new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,packageURI);
                            startActivity(intent1);
                        }
                    });
                    msgTipDialog.show();
                }

                break;
            case R.id.check_background:
                break;
        }
    }
}
