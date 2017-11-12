package com.miittech.you.activity.device;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.RelativeLayout;

import com.google.gson.Gson;
import com.miittech.you.App;
import com.miittech.you.R;
import com.miittech.you.activity.BaseActivity;
import com.miittech.you.adapter.FriendListAdapter;
import com.miittech.you.adapter.SharedFriendListAdapter;
import com.miittech.you.global.HttpUrl;
import com.miittech.you.global.IntentExtras;
import com.miittech.you.global.Params;
import com.miittech.you.global.PubParam;
import com.miittech.you.impl.OnListItemClick;
import com.miittech.you.impl.TitleBarOptions;
import com.miittech.you.net.ApiServiceManager;
import com.miittech.you.net.response.DeviceInfoResponse;
import com.miittech.you.net.response.DeviceResponse;
import com.miittech.you.net.response.FriendsResponse;
import com.miittech.you.weight.Titlebar;
import com.ryon.mutils.EncryptUtils;
import com.ryon.mutils.LogUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
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
 * Created by ryon on 2017/11/12.
 */

public class DeviceSharedListActivity extends BaseActivity {
    @BindView(R.id.titlebar)
    Titlebar titlebar;
    @BindView(R.id.recyclerview)
    RecyclerView recyclerview;
    @BindView(R.id.rl_tip)
    RelativeLayout rlTip;

    private SharedFriendListAdapter mAdapter;
    private String deviceId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_shared_list);
        ButterKnife.bind(this);
        initMyTitleBar(titlebar,"分享");
        deviceId = getIntent().getStringExtra(IntentExtras.DEVICE.ID);
        titlebar.showBackOption();
        titlebar.showSettingOption();
        titlebar.setSettingIcon(R.drawable.ic_device_shared);
        titlebar.setTitleBarOptions(new TitleBarOptions(){
            @Override
            public void onBack() {
                super.onBack();
                finish();
            }

            @Override
            public void onSetting() {
                super.onSetting();
                doShare();
            }
        });
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerview.setLayoutManager(layoutManager);
        recyclerview.setHasFixedSize(true);
        mAdapter = new SharedFriendListAdapter(this);
        recyclerview.setAdapter(mAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        getDeviceSharedList(deviceId);
    }

    @OnClick(R.id.btn_shared)
    public void onViewClicked() {
        doShare();
    }

    private void doShare() {
        Intent intent = new Intent(this,DeviceSharedAddActivity.class);
        intent.putExtras(getIntent());
        startActivity(intent);
    }

    private void getDeviceSharedList(String devId) {
        Map param = new LinkedHashMap();
        param.put("devid",devId);
        String json = new Gson().toJson(param);
        PubParam pubParam = new PubParam(App.getInstance().getUserId());
        String sign_unSha1 = pubParam.toValueString() + json + App.getInstance().getTocken();
        LogUtils.d("sign_unsha1", sign_unSha1);
        String sign = EncryptUtils.encryptSHA1ToString(sign_unSha1).toLowerCase();
        LogUtils.d("sign_sha1", sign);
        String path = HttpUrl.Api + "devsharelist/" + pubParam.toUrlParam(sign);
        final RequestBody requestBody = RequestBody.create(MediaType.parse(HttpUrl.MediaType_Json), json);

        ApiServiceManager.getInstance().buildApiService(this).postToGetFriendList(path, requestBody)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<FriendsResponse>() {
                    @Override
                    public void accept(FriendsResponse response) throws Exception {
                        initFriendList(response.getFriendlist());
                        if(!response.isSuccessful()){
                            response.onError(DeviceSharedListActivity.this);
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        throwable.printStackTrace();
                    }
                });
    }

    private void initFriendList(List<FriendsResponse.FriendlistBean> devlist) {
        if(devlist!=null&&devlist.size()>0){
            rlTip.setVisibility(View.GONE);
            recyclerview.setVisibility(View.VISIBLE);
            mAdapter.updateData(devlist);
        }else{
            recyclerview.setVisibility(View.GONE);
            rlTip.setVisibility(View.VISIBLE);
        }

    }
}
