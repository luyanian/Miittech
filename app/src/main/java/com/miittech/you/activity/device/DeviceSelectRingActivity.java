package com.miittech.you.activity.device;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.miittech.you.R;
import com.miittech.you.activity.BaseActivity;
import com.miittech.you.activity.MainActivity;
import com.miittech.you.global.IntentExtras;
import com.miittech.you.impl.TitleBarOptions;
import com.miittech.you.weight.CircleImageView;
import com.miittech.you.weight.Titlebar;
import com.ryon.mutils.ActivityPools;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by Administrator on 2017/10/30.
 */

public class DeviceSelectRingActivity extends BaseActivity {
    @BindView(R.id.titlebar)
    Titlebar titlebar;
    @BindView(R.id.img_device_icon)
    CircleImageView imgDeviceIcon;
    @BindView(R.id.tv_device_name)
    TextView tvDeviceName;
    @BindView(R.id.tv_device_location)
    TextView tvDeviceLocation;
    @BindView(R.id.tv_device_time)
    TextView tvDeviceTime;
    @BindView(R.id.tv_item1)
    TextView tvItem1;
    @BindView(R.id.tv_item2)
    TextView tvItem2;
    @BindView(R.id.tv_item3)
    TextView tvItem3;
    @BindView(R.id.tv_item4)
    TextView tvItem4;
    @BindView(R.id.img_item1)
    ImageView imgItem1;
    @BindView(R.id.img_item2)
    ImageView imgItem2;
    @BindView(R.id.img_item3)
    ImageView imgItem3;
    @BindView(R.id.img_item4)
    ImageView imgItem4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_select_ring);
        ButterKnife.bind(this);

        initTitleBar(titlebar, "设置分类");
        titlebar.showBackOption();
        titlebar.showCompleteOption("完成");
        titlebar.setTitleBarOptions(new TitleBarOptions() {
            @Override
            public void onBack() {
                super.onBack();
                finish();
            }

            @Override
            public void onComplete() {
                super.onComplete();
                ActivityPools.finishAllExcept(MainActivity.class);
            }
        });

        tvDeviceLocation.setText(getIntent().getStringExtra(IntentExtras.DEVICE.NAME));
    }

    private void showCurrentImg(View view) {
        if (imgItem1 == view) {
            imgItem1.setVisibility(View.VISIBLE);
            imgItem2.setVisibility(View.GONE);
            imgItem3.setVisibility(View.GONE);
            imgItem4.setVisibility(View.GONE);
            tvDeviceTime.setText(tvItem1.getText().toString());
        }
        if (imgItem2 == view) {
            imgItem1.setVisibility(View.GONE);
            imgItem2.setVisibility(View.VISIBLE);
            imgItem3.setVisibility(View.GONE);
            imgItem4.setVisibility(View.GONE);
            tvDeviceTime.setText(tvItem2.getText().toString());
        }
        if (imgItem3 == view) {
            imgItem1.setVisibility(View.GONE);
            imgItem2.setVisibility(View.GONE);
            imgItem3.setVisibility(View.VISIBLE);
            imgItem4.setVisibility(View.GONE);
            tvDeviceTime.setText(tvItem3.getText().toString());
        }
        if (imgItem4 == view) {
            imgItem1.setVisibility(View.GONE);
            imgItem2.setVisibility(View.GONE);
            imgItem3.setVisibility(View.GONE);
            imgItem4.setVisibility(View.VISIBLE);
            tvDeviceTime.setText(tvItem4.getText().toString());
        }
    }

    @OnClick({R.id.rl_item1, R.id.rl_item2, R.id.rl_item3, R.id.rl_item4})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.rl_item1:
                showCurrentImg(imgItem1);
                break;
            case R.id.rl_item2:
                showCurrentImg(imgItem2);
                break;
            case R.id.rl_item3:
                showCurrentImg(imgItem3);
                break;
            case R.id.rl_item4:
                showCurrentImg(imgItem4);
                break;
        }
    }
}
