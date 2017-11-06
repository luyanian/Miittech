package com.miittech.you.activity.device;

import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.miittech.you.App;
import com.miittech.you.R;
import com.miittech.you.activity.BaseActivity;
import com.miittech.you.activity.MainActivity;
import com.miittech.you.adapter.SelectAdapter;
import com.miittech.you.adapter.SelectRingAdapter;
import com.miittech.you.common.Common;
import com.miittech.you.global.HttpUrl;
import com.miittech.you.global.IntentExtras;
import com.miittech.you.global.Params;
import com.miittech.you.global.PubParam;
import com.miittech.you.impl.OnListItemClick;
import com.miittech.you.impl.TitleBarOptions;
import com.miittech.you.net.ApiServiceManager;
import com.miittech.you.net.response.DeviceResponse;
import com.miittech.you.net.response.SoundListResponse;
import com.miittech.you.weight.CircleImageView;
import com.miittech.you.weight.Titlebar;
import com.ryon.mutils.ActivityPools;
import com.ryon.mutils.EncryptUtils;
import com.ryon.mutils.LogUtils;
import com.ryon.mutils.ToastUtils;

import java.util.ArrayList;
import java.util.HashMap;
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
 * Created by Administrator on 2017/10/30.
 */

public class DeviceSelectRingActivity extends BaseActivity implements OnListItemClick {
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
    @BindView(R.id.recyclerview)
    RecyclerView recyclerview;

    private SelectRingAdapter selectRingAdapter;
    List<SoundListResponse.SourndlistBean> sourndlist = new ArrayList<>();

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
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(this);
        recyclerview.setLayoutManager(mLayoutManager);
        //如果可以确定每个item的高度是固定的，设置这个选项可以提高性能
        recyclerview.setHasFixedSize(true);
        selectRingAdapter = new SelectRingAdapter(this,sourndlist,this);
        recyclerview.setAdapter(selectRingAdapter);
        getSoundList();
    }

    @Override
    public void onItemClick(Object o) {

    }

    public void getSoundList(){
        PubParam pubParam = new PubParam(App.getUserId());
        String sign_unSha1 = pubParam.toValueString() + App.getTocken();
        LogUtils.d("sign_unsha1", sign_unSha1);
        String sign = EncryptUtils.encryptSHA1ToString(sign_unSha1).toLowerCase();
        LogUtils.d("sign_sha1", sign);
        String path = HttpUrl.Api + "soundlist/" + pubParam.toUrlParam(sign);
        final RequestBody requestBody = RequestBody.create(MediaType.parse(HttpUrl.MediaType_Json), "");
        ApiServiceManager.getInstance().buildApiService(this).postToGetSoundList(path,requestBody)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<SoundListResponse>() {
                    @Override
                    public void accept(SoundListResponse response) throws Exception {
                        if(response.isSuccessful()){
                            initSoundList(response.getSourndlist());
                        }else{
                            response.onError(DeviceSelectRingActivity.this);
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        throwable.printStackTrace();
                    }
                });
    }

    private void initSoundList(List<SoundListResponse.SourndlistBean> sourndlist) {
        this.sourndlist.clear();
        this.sourndlist.addAll(sourndlist);
        selectRingAdapter.notifyDataSetChanged();
    }
}
