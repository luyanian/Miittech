package com.miittech.you.activity.event;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.baidu.mapapi.map.MapView;
import com.miittech.you.R;
import com.miittech.you.activity.BaseActivity;
import com.miittech.you.activity.MainActivity;
import com.miittech.you.activity.setting.SettingActivity;
import com.miittech.you.adapter.EventLogAdapter;
import com.miittech.you.adapter.TraceDalySelectAdapter;
import com.miittech.you.fragment.EventLogFragment;
import com.miittech.you.global.IntentExtras;
import com.miittech.you.impl.OnListItemClick;
import com.miittech.you.impl.TitleBarOptions;
import com.miittech.you.net.response.UserInfoResponse;
import com.miittech.you.weight.CircleImageView;
import com.miittech.you.weight.Titlebar;
import com.ryon.mutils.TimeUtils;
import com.ryon.mutils.ToastUtils;

import java.util.Date;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by ryon on 2017/12/2.
 */

public class EventTraceDetailActivity extends BaseActivity {

    @BindView(R.id.titlebar)
    Titlebar titlebar;
    @BindView(R.id.recyclerview)
    RecyclerView recyclerview;
    @BindView(R.id.tv_is_shared)
    TextView tvIsShared;
    @BindView(R.id.item_icon_status)
    ImageView itemIconStatus;
    @BindView(R.id.item_icon)
    CircleImageView itemIcon;
    @BindView(R.id.rl_item_icon)
    RelativeLayout rlItemIcon;
    @BindView(R.id.item_title)
    TextView itemTitle;
    @BindView(R.id.item_location)
    TextView itemLocation;
    @BindView(R.id.item_time)
    TextView itemTime;
    @BindView(R.id.item_battery)
    TextView itemBattery;
    @BindView(R.id.item_shared)
    TextView itemShared;
    @BindView(R.id.rl_item)
    RelativeLayout rlItem;
    @BindView(R.id.map_view)
    MapView mapView;
    private String devId;

    private TraceDalySelectAdapter traceDalySelectAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_trace_detail);
        ButterKnife.bind(this);
        devId = getIntent().getStringExtra(IntentExtras.DEVICE.ID);
        initMyTitleBar(titlebar,"LOGO");
        titlebar.showBackOption();
        titlebar.showSettingOption();
        titlebar.setTitleBarOptions(new TitleBarOptions(){
            @Override
            public void onBack() {
                super.onBack();
                finish();
            }

            @Override
            public void onSetting() {
                super.onSetting();
                Intent intent = new Intent(EventTraceDetailActivity.this, SettingActivity.class);
                startActivity(intent);
            }
        });
        //创建默认的线性LayoutManager
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(this);
        mLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        recyclerview.setLayoutManager(mLayoutManager);
        recyclerview.setHasFixedSize(true);
        //创建并设置Adapter
        traceDalySelectAdapter = new TraceDalySelectAdapter(this, new OnListItemClick<Date>() {
            @Override
            public void onItemClick(Date date) {
                super.onItemClick(date);
                ToastUtils.showShort(TimeUtils.date2String(date));
            }
        });
        recyclerview.setAdapter(traceDalySelectAdapter);

    }
}
