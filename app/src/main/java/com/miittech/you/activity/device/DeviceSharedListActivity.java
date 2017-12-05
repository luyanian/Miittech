package com.miittech.you.activity.device;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.RelativeLayout;

import com.google.gson.Gson;
import com.inuker.bluetooth.library.Constants;
import com.miittech.you.App;
import com.miittech.you.R;
import com.miittech.you.activity.BaseActivity;
import com.miittech.you.adapter.FriendListAdapter;
import com.miittech.you.adapter.SharedFriendListAdapter;
import com.miittech.you.common.Common;
import com.miittech.you.entity.Locinfo;
import com.miittech.you.global.HttpUrl;
import com.miittech.you.global.IntentExtras;
import com.miittech.you.global.Params;
import com.miittech.you.global.PubParam;
import com.miittech.you.global.SPConst;
import com.miittech.you.impl.OnListItemClick;
import com.miittech.you.impl.TitleBarOptions;
import com.miittech.you.manager.BLEClientManager;
import com.miittech.you.net.ApiServiceManager;
import com.miittech.you.net.response.DeviceInfoResponse;
import com.miittech.you.net.response.DeviceResponse;
import com.miittech.you.net.response.FriendsResponse;
import com.miittech.you.weight.Titlebar;
import com.ryon.mutils.EncryptUtils;
import com.ryon.mutils.LogUtils;
import com.ryon.mutils.SPUtils;
import com.ryon.mutils.ToastUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cn.sharesdk.framework.Platform;
import cn.sharesdk.framework.PlatformActionListener;
import cn.sharesdk.onekeyshare.OnekeyShare;
import cn.sharesdk.onekeyshare.ShareContentCustomizeCallback;
import cn.sharesdk.tencent.qq.QQ;
import cn.sharesdk.wechat.friends.Wechat;
import cn.sharesdk.wechat.moments.WechatMoments;
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
    private DeviceInfoResponse.UserinfoBean.DevinfoBean devinfoBean;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_shared_list);
        ButterKnife.bind(this);
        initMyTitleBar(titlebar,"分享");
        devinfoBean = (DeviceInfoResponse.UserinfoBean.DevinfoBean) getIntent().getSerializableExtra(IntentExtras.DEVICE.DATA);
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
                shareToSocial();
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
        getDeviceSharedList(devinfoBean.getDevid());
    }

    @OnClick(R.id.btn_shared)
    public void onViewClicked() {
        shareToFriend();
    }

    private void shareToFriend() {
        Intent intent = new Intent(this,DeviceSharedAddActivity.class);
        intent.putExtras(getIntent());
        startActivity(intent);
    }
    private void shareToSocial(){
        OnekeyShare oks = new OnekeyShare();
        oks.setShareContentCustomizeCallback(new ShareContentCustomizeCallback() {
            @Override
            public void onShare(Platform platform, Platform.ShareParams paramsToShare) {
                double lat = 0;
                double lng = 0;
                if(BLEClientManager.getClient().getConnectStatus(Common.formatDevId2Mac(devinfoBean.getDevid()))== Constants.STATUS_DEVICE_CONNECTED){
                    Locinfo locinfo = (Locinfo) SPUtils.getInstance().readObject(SPConst.LOC_INFO);
                    if(locinfo!=null){
                        lat = locinfo.getLat();
                        lng = locinfo.getLng();
                    }else{
                        lat = devinfoBean.getLocinfo().getLat();
                        lng = devinfoBean.getLocinfo().getLng();
                    }
                }else{
                    lat = devinfoBean.getLocinfo().getLat();
                    lng = devinfoBean.getLocinfo().getLng();
                }
                String imgUrl = "http://api.map.baidu.com/staticimage/v2?ak=eA32DSeForR8YOTkvDM6LhUcFaHwrwVR" +
                        "&mcode=4B:DB:0E:E2:CA:AA:EF:77:C7:37:FA:46:B9:6D:C6:CB:CD:02:10:47;com.miittech.you" +
                        "&center="+lng+","+lat+"&width=560&height=280&zoom=16" +
                        "&markers="+lng+","+lat+"&markerStyles=l,A|m,B|l,C|l,D|m,E|,|l,G|m,H";
                if(WechatMoments.NAME.equals(platform.getName())||Wechat.NAME.equals(platform.getName())){
                    paramsToShare.setShareType(Platform.SHARE_IMAGE);
                    paramsToShare.setTitle("智云有物");
                    paramsToShare.setImageUrl(imgUrl);
                }else if(QQ.NAME.equals(platform.getName())){
                    paramsToShare.setShareType(Platform.SHARE_IMAGE);
                    paramsToShare.setImageUrl(imgUrl);
                }
            }
        });
        oks.setCallback(new PlatformActionListener() {
            @Override
            public void onComplete(Platform platform, int i, HashMap<String, Object> hashMap) {
                ToastUtils.showShort("分享成功");
            }

            @Override
            public void onError(Platform platform, int i, Throwable throwable) {
                ToastUtils.showShort("分享失败，请重试");
            }

            @Override
            public void onCancel(Platform platform, int i) {
                ToastUtils.showShort("取消分享");
            }
        });
        oks.show(this);
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
