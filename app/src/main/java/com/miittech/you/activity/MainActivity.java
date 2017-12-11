package com.miittech.you.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.widget.TextView;

import com.google.gson.Gson;
import com.miittech.you.App;
import com.miittech.you.R;
import com.miittech.you.activity.device.DeviceAddActivity;
import com.miittech.you.activity.setting.SettingActivity;
import com.miittech.you.common.Common;
import com.miittech.you.fragment.EventsFragment;
import com.miittech.you.fragment.ListFragment;
import com.miittech.you.fragment.MapFragment;
import com.miittech.you.global.HttpUrl;
import com.miittech.you.global.Params;
import com.miittech.you.global.PubParam;
import com.miittech.you.global.SPConst;
import com.miittech.you.impl.TitleBarOptions;
import com.miittech.you.net.ApiServiceManager;
import com.miittech.you.net.response.UserInfoResponse;
import com.miittech.you.weight.Titlebar;
import com.ryon.mutils.ActivityPools;
import com.ryon.mutils.EncryptUtils;
import com.ryon.mutils.LogUtils;
import com.ryon.mutils.SPUtils;
import com.ryon.mutils.ToastUtils;

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
        getIgnoreSetting();
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
    private void getIgnoreSetting() {
        Map param = new LinkedHashMap();
        param.put("qrytype", Params.QRY_TYPE.ALL);
        String json = new Gson().toJson(param);
        PubParam pubParam = new PubParam(Common.getUserId());
        String sign_unSha1 = pubParam.toValueString() + json + Common.getTocken();
        LogUtils.d("sign_unsha1", sign_unSha1);
        String sign = EncryptUtils.encryptSHA1ToString(sign_unSha1).toLowerCase();
        LogUtils.d("sign_sha1", sign);
        String path = HttpUrl.Api + "userinfo/" + pubParam.toUrlParam(sign);
        final RequestBody requestBody = RequestBody.create(MediaType.parse(HttpUrl.MediaType_Json), json);

        ApiServiceManager.getInstance().buildApiService(this).postToGetUserInfo(path, requestBody)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<UserInfoResponse>() {
                    @Override
                    public void accept(UserInfoResponse response) throws Exception {
                        if(response.isSuccessful()) {
                            SPUtils.getInstance().readObject(SPConst.USER_INFO);
                            SPUtils.getInstance().saveObject(SPConst.USER_INFO, response);
                        }else {
                            response.onError(MainActivity.this);
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        throwable.printStackTrace();
                    }
                });
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
            },2000);
        } else {
            ActivityPools.finishAllActivity();
            System.exit(0);
        }
    }
}
