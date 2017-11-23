package com.miittech.you.activity.setting;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.gson.Gson;
import com.miittech.you.App;
import com.miittech.you.R;
import com.miittech.you.activity.BaseActivity;
import com.miittech.you.dialog.DialogUtils;
import com.miittech.you.global.HttpUrl;
import com.miittech.you.global.Params;
import com.miittech.you.global.PubParam;
import com.miittech.you.impl.OnIgnoreAddOptions;
import com.miittech.you.impl.TitleBarOptions;
import com.miittech.you.impl.TypeSelectorChangeLisener;
import com.miittech.you.net.ApiServiceManager;
import com.miittech.you.net.response.DeviceResponse;
import com.miittech.you.weight.Titlebar;
import com.miittech.you.weight.TypeSelector;
import com.ryon.mutils.EncryptUtils;
import com.ryon.mutils.LogUtils;

import java.util.LinkedHashMap;
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
 * Created by Administrator on 2017/9/29.
 */

public class IgnoreSettingActivity extends BaseActivity implements TypeSelectorChangeLisener {
    @BindView(R.id.titlebar)
    Titlebar titlebar;
    @BindView(R.id.typeSelector)
    TypeSelector typeSelector;
    @BindView(R.id.tv_setting_title)
    TextView tvSettingTitle;
    @BindView(R.id.check_setting_ignore)
    CheckBox checkSettingIgnore;
    @BindView(R.id.tv_setting_ignore_desc)
    TextView tvSettingIgnoreDesc;
    @BindView(R.id.tv_setting_ignore_area)
    TextView tvSettingIgnoreArea;
    @BindView(R.id.ll_ignore_areas)
    LinearLayout llIgnoreAreas;
    @BindView(R.id.tv_setting_ignore_ssid)
    TextView tvSettingIgnoreSsid;
    @BindView(R.id.ll_ignore_ssids)
    LinearLayout llIgnoreSsids;
    @BindView(R.id.ll_ignore_times)
    LinearLayout llIgnoreTimes;
    @BindView(R.id.btn_add_text)
    TextView btnAddText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting_ignore);
        ButterKnife.bind(this);
        initMyTitleBar(titlebar, R.string.text_setting_ignore);
        titlebar.showBackOption();
        titlebar.setTitleBarOptions(new TitleBarOptions() {
            @Override
            public void onBack() {
                super.onBack();
                finish();
            }
        });
        typeSelector.setItemText(R.string.text_setting_ignore, R.string.text_setting_ignore_time);
        typeSelector.setTypeSelectorChangeLisener(this);
        typeSelector.setSelectItem(0);
        getIgnoreSetting();
    }

    @Override
    public void onTabSelectorChanged(int item) {
        if (item == 0) {
            tvSettingTitle.setText(getResources().getString(R.string.text_setting_ignore_area));
            tvSettingIgnoreDesc.setText(getResources().getString(R.string.tip_msg_ignore_area_desc));
            tvSettingIgnoreArea.setVisibility(View.VISIBLE);
            llIgnoreAreas.setVisibility(View.VISIBLE);
            tvSettingIgnoreSsid.setVisibility(View.VISIBLE);
            llIgnoreSsids.setVisibility(View.VISIBLE);
            llIgnoreTimes.setVisibility(View.GONE);
            btnAddText.setText(getResources().getString(R.string.text_setting_ignore_area_add));
        } else if (item == 1) {
            tvSettingTitle.setText(getResources().getString(R.string.text_setting_ignore_time));
            tvSettingIgnoreDesc.setText(getResources().getString(R.string.tip_msg_ignore_time_desc));
            tvSettingIgnoreArea.setVisibility(View.GONE);
            llIgnoreAreas.setVisibility(View.GONE);
            tvSettingIgnoreSsid.setVisibility(View.GONE);
            llIgnoreSsids.setVisibility(View.GONE);
            llIgnoreTimes.setVisibility(View.VISIBLE);
            btnAddText.setText(getResources().getString(R.string.text_setting_ignore_time_add));
        }
    }

    @OnClick(R.id.btn_add_setting)
    public void onViewClicked() {
        if(typeSelector.getSelectItem()==0){
            DialogUtils.showIgnoreAddDialog(this).setIgnoreAddOptions(new OnIgnoreAddOptions() {
                @Override
                public void addPointIgnore() {
                    Intent intent = new Intent(IgnoreSettingActivity.this,IgnoreAddPointActivity.class);
                    startActivity(intent);
                }

                @Override
                public void addWifiIgnore() {
                    Intent intent = new Intent(IgnoreSettingActivity.this,IgnoreAddWifiActivity.class);
                    startActivity(intent);
                }
            });
        }
        if(typeSelector.getSelectItem()==1){
            Intent intent = new Intent(IgnoreSettingActivity.this,IgnoreTimeSlotActivity.class);
            startActivity(intent);
        }
    }

    private void getIgnoreSetting() {
        Map param = new LinkedHashMap();
        param.put("qrytype", Params.QRY_TYPE.USED);
        String json = new Gson().toJson(param);
        PubParam pubParam = new PubParam(App.getInstance().getUserId());
        String sign_unSha1 = pubParam.toValueString() + json + App.getInstance().getTocken();
        LogUtils.d("sign_unsha1", sign_unSha1);
        String sign = EncryptUtils.encryptSHA1ToString(sign_unSha1).toLowerCase();
        LogUtils.d("sign_sha1", sign);
        String path = HttpUrl.Api + "userdevicelist/" + pubParam.toUrlParam(sign);
        final RequestBody requestBody = RequestBody.create(MediaType.parse(HttpUrl.MediaType_Json), json);

        ApiServiceManager.getInstance().buildApiService(this).postDeviceOption(path, requestBody)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<DeviceResponse>() {
                    @Override
                    public void accept(DeviceResponse response) throws Exception {

                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        throwable.printStackTrace();
                    }
                });
    }
}
