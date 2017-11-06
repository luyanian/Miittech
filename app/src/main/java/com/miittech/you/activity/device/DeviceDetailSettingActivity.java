package com.miittech.you.activity.device;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.google.gson.Gson;
import com.miittech.you.App;
import com.miittech.you.R;
import com.miittech.you.activity.BaseActivity;
import com.miittech.you.common.Common;
import com.miittech.you.dialog.DialogUtils;
import com.miittech.you.dialog.SelectDialog;
import com.miittech.you.global.HttpUrl;
import com.miittech.you.global.IntentExtras;
import com.miittech.you.global.Params;
import com.miittech.you.global.PubParam;
import com.miittech.you.impl.OnListItemClick;
import com.miittech.you.impl.TitleBarOptions;
import com.miittech.you.net.ApiServiceManager;
import com.miittech.you.net.response.DeviceResponse;
import com.miittech.you.weight.Titlebar;
import com.ryon.mutils.EncryptUtils;
import com.ryon.mutils.LogUtils;
import com.ryon.mutils.ToastUtils;

import java.util.HashMap;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import okhttp3.MediaType;
import okhttp3.RequestBody;

/**
 * Created by Administrator on 2017/11/6.
 */

public class DeviceDetailSettingActivity extends BaseActivity implements CompoundButton.OnCheckedChangeListener {
    @BindView(R.id.titlebar)
    Titlebar titlebar;
    @BindView(R.id.tv_bell)
    TextView tvBell;
    @BindView(R.id.check_vibrate)
    CheckBox checkVibrate;
    @BindView(R.id.tv_disconnect_reminder_time)
    TextView tvDisconnectReminderTime;
    @BindView(R.id.check_disconnect_repeated_remind)
    CheckBox checkDisconnectRepeatedRemind;
    @BindView(R.id.check_repeated_remind)
    CheckBox checkRepeatedRemind;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_detail_setting);
        ButterKnife.bind(this);
        initTitleBar(titlebar,"手机提醒设置");
        titlebar.showBackOption();
        titlebar.setTitleBarOptions(new TitleBarOptions(){
            @Override
            public void onBack() {
                super.onBack();
                finish();
            }
        });
        checkDisconnectRepeatedRemind.setOnCheckedChangeListener(this);
        checkRepeatedRemind.setOnCheckedChangeListener(this);
        checkVibrate.setOnCheckedChangeListener(this);
    }

    @OnClick({R.id.rl_bell, R.id.rl_disconnect_reminder_time})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.rl_bell:
                Intent intent = new Intent(DeviceDetailSettingActivity.this,DeviceSelectRingActivity.class);
                startActivity(intent);
                break;
            case R.id.rl_disconnect_reminder_time:
                SelectDialog selectDialog = DialogUtils.createSelectDialog(this);
                selectDialog.setTitle("请选择时间");
                selectDialog.setOnListItemClick(new OnListItemClick<String>() {
                    @Override
                    public void onItemClick(String s) {
                        tvDisconnectReminderTime.setText(s);
                        editAttr();
                    }
                });
                selectDialog.init();
                selectDialog.show();
                break;
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()){
            case R.id.check_disconnect_repeated_remind:
                break;
            case R.id.check_repeated_remind:
                break;
            case R.id.check_vibrate:
                break;
        }
    }

    private void editAttr() {
        Map alertinfo = new HashMap();
        alertinfo.put("","");
        Map param = new HashMap();
//        param.put("devid", device.getDevidX());
        param.put("method", Params.METHOD.UNBIND);
        String json = new Gson().toJson(param);
        PubParam pubParam = new PubParam(App.getUserId());
        String sign_unSha1 = pubParam.toValueString() + json + App.getTocken();
        LogUtils.d("sign_unsha1", sign_unSha1);
        String sign = EncryptUtils.encryptSHA1ToString(sign_unSha1).toLowerCase();
        LogUtils.d("sign_sha1", sign);
        String path = HttpUrl.Api + "deviceattr/" + pubParam.toUrlParam(sign);
        final RequestBody requestBody = RequestBody.create(MediaType.parse(HttpUrl.MediaType_Json), json);
        ApiServiceManager.getInstance().buildApiService(this).postDeviceOption(path, requestBody)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<DeviceResponse>() {
                    @Override
                    public void accept(DeviceResponse response) throws Exception {
                        if (response.isSuccessful()) {
                            ToastUtils.showShort("设置成功");
                        } else {
                            response.onError(DeviceDetailSettingActivity.this);
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        throwable.printStackTrace();
                    }
                });
    }
}
