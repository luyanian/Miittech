package com.miittech.you.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.widget.TextView;

import com.miittech.you.R;
import com.miittech.you.activity.device.DeviceAddActivity;
import com.miittech.you.activity.setting.SettingActivity;
import com.miittech.you.fragment.EventsFragment;
import com.miittech.you.fragment.ListFragment;
import com.miittech.you.fragment.MapFragment;
import com.miittech.you.impl.TitleBarOptions;
import com.miittech.you.weight.Titlebar;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends BaseActivity {

    @BindView(R.id.titlebar)
    Titlebar titlebar;
    @BindView(R.id.tab_list)
    TextView tabList;
    @BindView(R.id.tab_map)
    TextView tabMap;
    @BindView(R.id.tab_events)
    TextView tabEvents;

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
        initMyTitleBar(titlebar,"LOGO");
        titlebar.showAddOption();
        titlebar.showSettingOption();
        titlebar.setTitleBarOptions(new TitleBarOptions(){
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

        tabMap.setSelected(true);
        onViewClicked(tabList);
    }

    @OnClick({R.id.tab_list, R.id.tab_map, R.id.tab_events})
    public void onViewClicked(View view) {
        FragmentTransaction trans = fragmentManager.beginTransaction();
        int vID = view.getId();
        setMenuStyle(vID);
        hideFrament(trans);
        setFragment(vID,trans);
        trans.commit();
    }

    /**
     * 隐藏所有的fragment(编程初始化状态)
     * @param trans
     */
    private void hideFrament(FragmentTransaction trans) {
        if(fragmentList!=null){
            trans.hide(fragmentList);
        }
        if(fragmentMap!=null){
            trans.hide(fragmentMap);
        }
        if(fragmentEvents!=null){
            trans.hide(fragmentEvents);
        }
    }

    /**
     * 设置menu样式
     * @param vID
     */
    private void setMenuStyle(int vID) {
        //列表
        if(vID==R.id.tab_list){
            tabMap.setSelected(false);
            tabEvents.setSelected(false);
            tabList.setSelected(true);
        }
        // 地图
        if(vID==R.id.tab_map){
            tabList.setSelected(false);
            tabEvents.setSelected(false);
            tabMap.setSelected(true);
        }
        // 事件
        if(vID==R.id.tab_events){
            tabList.setSelected(false);
            tabMap.setSelected(false);
            tabEvents.setSelected(true);
        }
    }

    /**
     * 设置Fragment
     * @param vID
     * @param trans
     */
    private void setFragment(int vID,FragmentTransaction trans) {
        switch (vID) {
            case R.id.tab_list:
                if(fragmentList==null){
                    fragmentList = new ListFragment();
                    trans.add(R.id.content, fragmentList);
                }else{
                    trans.show(fragmentList);
                }
                break;
            case R.id.tab_map:
                if(fragmentMap==null){
                    fragmentMap = new MapFragment();
                    trans.add(R.id.content, fragmentMap);
                }else{
                    trans.show(fragmentMap);
                }
                break;
            case R.id.tab_events:
                if(fragmentEvents==null){
                    fragmentEvents = new EventsFragment();
                    trans.add(R.id.content, fragmentEvents);
                }else{
                    trans.show(fragmentEvents);
                }
                break;
            default:
                break;
        }
    }
}
