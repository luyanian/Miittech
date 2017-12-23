package com.miittech.you.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.miittech.you.R;
import com.miittech.you.activity.device.DeviceAddActivity;
import com.miittech.you.activity.setting.SettingActivity;
import com.miittech.you.common.Common;
import com.miittech.you.fragment.EventsFragment;
import com.miittech.you.fragment.ListFragment;
import com.miittech.you.fragment.MapFragment;
import com.miittech.you.impl.TitleBarOptions;
import com.miittech.you.weight.Titlebar;
import com.ryon.mutils.ActivityPools;
import com.ryon.mutils.ToastUtils;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends BaseActivity {

    @BindView(R.id.titlebar)
    Titlebar titlebar;
    @BindView(R.id.tab_list)
    LinearLayout tabList;
    @BindView(R.id.tab_map)
    LinearLayout tabMap;
    @BindView(R.id.tab_events)
    LinearLayout tabEvents;
    @BindView(R.id.img_list)
    ImageView imgList;
    @BindView(R.id.tv_list)
    TextView tvList;
    @BindView(R.id.img_map)
    ImageView imgMap;
    @BindView(R.id.tv_map)
    TextView tvMap;
    @BindView(R.id.img_events)
    ImageView imgEvents;
    @BindView(R.id.tv_events)
    TextView tvEvents;

    // 布局管理器
    private FragmentManager fragmentManager;
    private ListFragment fragmentList;
    private MapFragment fragmentMap;
    private EventsFragment fragmentEvents;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        fragmentManager = getSupportFragmentManager();
        ButterKnife.bind(this);
        initMyTitleBar(titlebar);
        titlebar.showAddOption();
        titlebar.showSettingOption();
        titlebar.setTitleBarOptions(new TitleBarOptions() {
            @Override
            public void onAdd() {
                Intent intent = new Intent(MainActivity.this, DeviceAddActivity.class);
                startActivity(intent);
            }

            @Override
            public void onSetting() {
                Intent intent = new Intent(MainActivity.this, SettingActivity.class);
                startActivity(intent);
            }
        });
        tvMap.setSelected(true);
        imgMap.setSelected(true);
        onViewClicked(tabList);
        Common.updateIngnoreSettingValid();
        Common.getUserInfo(this, null);
        Common.initDeviceList(this, null);
    }

    @OnClick({R.id.tab_list, R.id.tab_map, R.id.tab_events})
    public void onViewClicked(View view) {
        FragmentTransaction trans = fragmentManager.beginTransaction();
        int vID = view.getId();
        setMenuStyle(vID);
        hideFrament(trans);
        setFragment(vID, trans);
        trans.commit();
    }

    /**
     * 隐藏所有的fragment(编程初始化状态)
     *
     * @param trans
     */
    private void hideFrament(FragmentTransaction trans) {
        if (fragmentList != null) {
            trans.hide(fragmentList);
        }
        if (fragmentMap != null) {
            trans.hide(fragmentMap);
        }
        if (fragmentEvents != null) {
            trans.hide(fragmentEvents);
        }
    }

    /**
     * 设置menu样式
     *
     * @param vID
     */
    private void setMenuStyle(int vID) {
        //列表
        if (vID == R.id.tab_list) {
            tvMap.setSelected(false);
            imgMap.setSelected(false);
            tvEvents.setSelected(false);
            imgEvents.setSelected(false);
            tvList.setSelected(true);
            imgList.setSelected(true);
        }
        // 地图
        if (vID == R.id.tab_map) {
            tvList.setSelected(false);
            imgList.setSelected(false);
            tvEvents.setSelected(false);
            imgEvents.setSelected(false);
            tvMap.setSelected(true);
            imgMap.setSelected(true);
        }
        // 事件
        if (vID == R.id.tab_events) {
            tvList.setSelected(false);
            imgList.setSelected(false);
            tvMap.setSelected(false);
            imgMap.setSelected(false);
            tvEvents.setSelected(true);
            imgEvents.setSelected(true);
        }
    }

    /**
     * 设置Fragment
     *
     * @param vID
     * @param trans
     */
    private void setFragment(int vID, FragmentTransaction trans) {
        switch (vID) {
            case R.id.tab_list:
                if (fragmentList == null) {
                    fragmentList = new ListFragment();
                    trans.add(R.id.content, fragmentList);
                } else {
                    trans.show(fragmentList);
                }
                break;
            case R.id.tab_map:
                if (fragmentMap == null) {
                    fragmentMap = new MapFragment();
                    trans.add(R.id.content, fragmentMap);
                } else {
                    trans.show(fragmentMap);
                }
                break;
            case R.id.tab_events:
                if (fragmentEvents == null) {
                    fragmentEvents = new EventsFragment();
                    trans.add(R.id.content, fragmentEvents);
                } else {
                    trans.show(fragmentEvents);
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void onBackPressed() {
        exit();
    }

    // 定义一个变量，来标识是否退出
    private static boolean isExit = false;

    private void exit() {
        if (!isExit) {
            isExit = true;
            ToastUtils.showShort("再按一次退出程序");
            // 利用handler延迟发送更改状态信息
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    isExit = false;
                }
            }, 2000);
        } else {
            ActivityPools.finishAllActivity();
            System.exit(0);
        }
    }
}
