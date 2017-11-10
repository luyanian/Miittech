package com.miittech.you.activity.setting;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.miittech.you.R;
import com.miittech.you.activity.BaseActivity;
import com.miittech.you.dialog.DialogUtils;
import com.miittech.you.impl.OnIgnoreAddOptions;
import com.miittech.you.impl.TitleBarOptions;
import com.miittech.you.impl.TypeSelectorChangeLisener;
import com.miittech.you.weight.Titlebar;
import com.miittech.you.weight.TypeSelector;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

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
}
