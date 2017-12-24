package com.miittech.you.activity.device;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.widget.TextView;

import com.google.gson.Gson;
import com.miittech.you.R;
import com.miittech.you.activity.BaseActivity;
import com.miittech.you.activity.MainActivity;
import com.miittech.you.adapter.SelectRingAdapter;
import com.miittech.you.common.Common;
import com.miittech.you.common.SoundPlayUtils;
import com.miittech.you.entity.DeviceInfo;
import com.miittech.you.glide.GlideApp;
import com.miittech.you.global.HttpUrl;
import com.miittech.you.global.IntentExtras;
import com.miittech.you.global.PubParam;
import com.miittech.you.impl.OnListItemClick;
import com.miittech.you.impl.OnNetRequestCallBack;
import com.miittech.you.impl.TitleBarOptions;
import com.miittech.you.net.ApiServiceManager;
import com.miittech.you.net.response.DeviceDetailResponse;
import com.miittech.you.net.response.DeviceListResponse;
import com.miittech.you.net.response.SoundListResponse;
import com.miittech.you.weight.CircleImageView;
import com.miittech.you.weight.Titlebar;
import com.ryon.mutils.ActivityPools;
import com.ryon.mutils.EncryptUtils;
import com.ryon.mutils.LogUtils;
import com.ryon.mutils.NetworkUtils;
import com.ryon.mutils.ToastUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import okhttp3.MediaType;
import okhttp3.RequestBody;

/**
 * Created by Administrator on 2017/10/30.
 */

public class DeviceSelectRingActivity extends BaseActivity{
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
    private DeviceInfo deviceInfo;
    private SoundListResponse.SourndlistBean sourndlistBean;
    private String devId ;
    private Intent intent;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_select_ring);
        ButterKnife.bind(this);
        initMyTitleBar(titlebar, "设置分类");
        titlebar.showBackOption();
        titlebar.showCompleteOption("完成");
        titlebar.setTitleBarOptions(new TitleBarOptions() {
            @Override
            public void onBack() {
                super.onBack();
                SoundPlayUtils.stopAll();
                finish();
            }

            @Override
            public void onComplete() {
                super.onComplete();
                if(sourndlistBean==null){
                    return;
                }
                setDeviceAlertinfo();
            }
        });
        initViews();
    }


    private void initViews() {
        intent = getIntent();
        if(intent.hasExtra(IntentExtras.DEVICE.DATA)){
            deviceInfo = (DeviceInfo) intent.getSerializableExtra(IntentExtras.DEVICE.DATA);
            this.devId = deviceInfo.getDevidX();
            tvDeviceName.setText(Common.decodeBase64(deviceInfo.getDevname()));
            tvDeviceLocation.setText(Common.decodeBase64(deviceInfo.getGroupname()));
            GlideApp.with(this)
                    .load(deviceInfo.getDevimg())
                    .error(Common.getDefaultDevImgResouceId(Common.decodeBase64(deviceInfo.getGroupname())))
                    .placeholder(Common.getDefaultDevImgResouceId(Common.decodeBase64(deviceInfo.getGroupname())))
                    .into(imgDeviceIcon);
        }else{
            devId = intent.getStringExtra(IntentExtras.DEVICE.ID);
            String devName = intent.getStringExtra(IntentExtras.DEVICE.NAME);
            String iconUrl = intent.getStringExtra(IntentExtras.DEVICE.IMAGE);
            String devClassfy = intent.getStringExtra(IntentExtras.DEVICE.CLASSIFY);
            tvDeviceName.setText(devName);
            tvDeviceLocation.setText(devClassfy);
            GlideApp.with(this)
                    .load(iconUrl)
                    .error(Common.getDefaultDevImgResouceId(devClassfy))
                    .placeholder(Common.getDefaultDevImgResouceId(devClassfy))
                    .into(imgDeviceIcon);
        }
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(this);
        recyclerview.setLayoutManager(mLayoutManager);
        //如果可以确定每个item的高度是固定的，设置这个选项可以提高性能
        recyclerview.setHasFixedSize(true);
        selectRingAdapter = new SelectRingAdapter(this,sourndlist,new OnListItemClick(){
            @Override
            public void onItemClick(Object o) {
                super.onItemClick(o);
                sourndlistBean = (SoundListResponse.SourndlistBean) o;
            }
        });
        recyclerview.setAdapter(selectRingAdapter);
        getSoundList();
    }

    private void initSoundList(List<SoundListResponse.SourndlistBean> sourndlist) {
        this.sourndlist.clear();
        this.sourndlist.addAll(sourndlist);
        selectRingAdapter.notifyDataSetChanged();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                selectRingAdapter.initSelectAlerName(deviceInfo);
            }
        },1000);
    }


    public void getSoundList(){
        if(!NetworkUtils.isConnected()){
            ToastUtils.showShort(R.string.msg_net_error);
            return;
        }
        PubParam pubParam = new PubParam(Common.getUserId());
        String sign_unSha1 = pubParam.toValueString() + Common.getTocken();
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

    private void setDeviceAlertinfo() {
        if(!NetworkUtils.isConnected()){
            ToastUtils.showShort(R.string.msg_net_error);
            return;
        }
        Map alertinfo = new HashMap();
        if(this.deviceInfo==null){
            alertinfo.put("vol",31);//音量
            alertinfo.put("isShake",1);//是否振东
            alertinfo.put("duration",30);//响铃时长
            alertinfo.put("isRepeat",1);//是否重复提醒，选填
            alertinfo.put("isReconnect",1);//是否重连提醒，选填
            alertinfo.put("id",this.sourndlistBean.getId());//铃声ID,缺省1，铃音编号
            alertinfo.put("name",this.sourndlistBean.getName());//铃声名称
        }else{
            alertinfo.put("vol",this.deviceInfo.getAlertinfo().getVol());//音量
            alertinfo.put("isShake",this.deviceInfo.getAlertinfo().getIsShake());//是否振东
            alertinfo.put("isRepeat",this.deviceInfo.getAlertinfo().getIsRepeat());//是否重复提醒，选填
            alertinfo.put("isReconnect",this.deviceInfo.getAlertinfo().getIsReconnect());//是否重连提醒，选填
            alertinfo.put("duration",this.deviceInfo.getAlertinfo().getDuration());//响铃时长
            alertinfo.put("id",this.sourndlistBean.getId());//铃声ID,缺省1，铃音编号
            alertinfo.put("name",this.sourndlistBean.getName());//铃声名称
        }
        
        Map devattr = new HashMap();
        devattr.put("alertinfo",alertinfo);
        Map param = new HashMap();
        param.put("devid", devId);
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
                .subscribe(new Consumer<DeviceListResponse>() {
                    @Override
                    public void accept(DeviceListResponse response) throws Exception {
                    if (response.isSuccessful()) {
                        SoundPlayUtils.stopAll();
                        Intent data = new Intent();
                        data.putExtra(IntentExtras.SOURND.ID,sourndlistBean.getId());
                        data.putExtra(IntentExtras.SOURND.NAME,sourndlistBean.getName());
                        setResult(RESULT_OK,data);
                        Common.initDeviceList(DeviceSelectRingActivity.this,null);
                        Common.getDeviceDetailInfo(DeviceSelectRingActivity.this, devId, new OnNetRequestCallBack() {
                            @Override
                            public void OnRequestComplete() {
                                if(intent.hasExtra(IntentExtras.DEVICE.DATA)){
                                    ActivityPools.finishActivity(DeviceSetClassifyActivity.class);
                                    ActivityPools.finishActivity(DeviceSetAttrActivity.class);
                                    ActivityPools.finishActivity(DeviceSelectRingActivity.class);
                                }else{
                                    ActivityPools.finishAllExcept(MainActivity.class);
                                }
                            }
                        });
                    } else {
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
}
