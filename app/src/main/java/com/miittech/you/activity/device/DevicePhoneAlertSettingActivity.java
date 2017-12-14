package com.miittech.you.activity.device;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.google.gson.Gson;
import com.miittech.you.R;
import com.miittech.you.activity.BaseActivity;
import com.miittech.you.common.Common;
import com.miittech.you.dialog.DialogUtils;
import com.miittech.you.dialog.SelectTimeDialog;
import com.miittech.you.global.HttpUrl;
import com.miittech.you.global.IntentExtras;
import com.miittech.you.global.PubParam;
import com.miittech.you.impl.OnListItemClick;
import com.miittech.you.impl.TitleBarOptions;
import com.miittech.you.net.ApiServiceManager;
import com.miittech.you.net.response.DeviceInfoResponse;
import com.miittech.you.net.response.DeviceResponse;
import com.miittech.you.weight.Titlebar;
import com.ryon.mutils.EncryptUtils;
import com.ryon.mutils.LogUtils;

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

public class DevicePhoneAlertSettingActivity extends BaseActivity implements CompoundButton.OnCheckedChangeListener {
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
    @BindView(R.id.tv_info1)
    TextView tvInfo1;
    @BindView(R.id.tv_info2)
    TextView tvInfo2;
    @BindView(R.id.tv_info3)
    TextView tvInfo3;


    private DeviceInfoResponse.UserinfoBean.DevinfoBean deviceInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone_alert_setting);
        ButterKnife.bind(this);
        initMyTitleBar(titlebar,"手机提醒设置");
        titlebar.showBackOption();
        titlebar.setTitleBarOptions(new TitleBarOptions(){
            @Override
            public void onBack() {
                super.onBack();
                finish();
            }
        });
        deviceInfo = (DeviceInfoResponse.UserinfoBean.DevinfoBean) getIntent().getSerializableExtra(IntentExtras.DEVICE.DATA);
        initViews(deviceInfo);
        checkDisconnectRepeatedRemind.setOnCheckedChangeListener(this);
        checkRepeatedRemind.setOnCheckedChangeListener(this);
        checkVibrate.setOnCheckedChangeListener(this);
    }

    private void initViews(DeviceInfoResponse.UserinfoBean.DevinfoBean deviceInfo) {
        tvBell.setText(this.deviceInfo.getAlertinfo().getName());
        checkVibrate.setChecked((this.deviceInfo.getAlertinfo().getIsShake()==1)?true:false);
        tvDisconnectReminderTime.setText(this.deviceInfo.getAlertinfo().getDuration()+"s");
        checkDisconnectRepeatedRemind.setChecked((this.deviceInfo.getAlertinfo().getIsReconnect()==1)?true:false);
        checkRepeatedRemind.setChecked((this.deviceInfo.getAlertinfo().getIsRepeat()==1)?true:false);
        String info1 ="手机与“"+Common.decodeBase64(this.deviceInfo.getDevname())+"”断开连接时响铃时长";
        String info2 = "锁屏时手机和“"+Common.decodeBase64(this.deviceInfo.getDevname())+"”断开连接后多次提醒，解锁手机或打开APP即可停止提醒";
        String info3 = "手机与“"+Common.decodeBase64(this.deviceInfo.getDevname())+"”重新连接时手机提醒";
        tvInfo1.setText(info1);
        tvInfo2.setText(info2);
        tvInfo3.setText(info3);
    }

    @OnClick({R.id.rl_bell, R.id.rl_disconnect_reminder_time})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.rl_bell:
                Intent intent = new Intent(DevicePhoneAlertSettingActivity.this,DeviceSelectRingActivity.class);
                intent.putExtra(IntentExtras.DEVICE.DATA,this.deviceInfo);
                startActivityForResult(intent,0);
                break;
            case R.id.rl_disconnect_reminder_time:
                final SelectTimeDialog selectDialog = DialogUtils.getInstance().createSelectDialog(this);
                selectDialog.setTitle("请选择时间");
                selectDialog.setOnListItemClick(new OnListItemClick<String>() {
                    @Override
                    public void onItemClick(String s) {
                        tvDisconnectReminderTime.setText(s);
                        setDeviceAlertinfo();
                        if(selectDialog!=null&&selectDialog.isShowing()) {
                            selectDialog.dismiss();
                        }
                    }
                });
                selectDialog.initData();
                selectDialog.show();
                break;
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()){
            case R.id.check_disconnect_repeated_remind:
                setDeviceAlertinfo();
                break;
            case R.id.check_repeated_remind:
                setDeviceAlertinfo();
                break;
            case R.id.check_vibrate:
                setDeviceAlertinfo();
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data!=null){
            this.deviceInfo.getAlertinfo().setId(data.getIntExtra(IntentExtras.SOURND.ID,1));
            this.deviceInfo.getAlertinfo().setName(data.getStringExtra(IntentExtras.SOURND.NAME));
            tvBell.setText(data.getStringExtra(IntentExtras.SOURND.NAME));
        }
    }

    private void setDeviceAlertinfo() {
        Map alertinfo = new HashMap();
        alertinfo.put("vol",31);//音量
        this.deviceInfo.getAlertinfo().setVol(31);
        alertinfo.put("isShake",checkVibrate.isChecked()?1:0);//是否振东
        this.deviceInfo.getAlertinfo().setIsShake(checkVibrate.isChecked()?1:0);
        alertinfo.put("isRepeat",checkRepeatedRemind.isChecked()?1:0);//是否重复提醒，选填
        this.deviceInfo.getAlertinfo().setIsRepeat(checkRepeatedRemind.isChecked()?1:0);
        alertinfo.put("isReconnect",checkDisconnectRepeatedRemind.isChecked()?1:0);//是否重连提醒，选填
        this.deviceInfo.getAlertinfo().setIsReconnect(checkDisconnectRepeatedRemind.isChecked()?1:0);
        int duration = Integer.valueOf(tvDisconnectReminderTime.getText().toString().replaceAll("s",""));
        alertinfo.put("duration",duration);//响铃时长
        this.deviceInfo.getAlertinfo().setDuration(duration);
        alertinfo.put("id",deviceInfo.getAlertinfo().getId());//铃声ID,缺省1，铃音编号
        alertinfo.put("name",deviceInfo.getAlertinfo().getName());//铃声名称
        Map devattr = new HashMap();
        devattr.put("alertinfo",alertinfo);
        Map param = new HashMap();
        param.put("devid", deviceInfo.getDevid());
        param.put("method", "G");
        param.put("devattr",devattr);
        String json = new Gson().toJson(param);
        PubParam pubParam = new PubParam(Common.getUserId());
        String sign_unSha1 = pubParam.toValueString() + json + Common.getTocken();
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
                        if (!response.isSuccessful()) {
                            response.onError(DevicePhoneAlertSettingActivity.this);
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
