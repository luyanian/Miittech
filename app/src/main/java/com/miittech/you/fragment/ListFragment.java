package com.miittech.you.fragment;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.PermissionChecker;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.google.gson.Gson;
import com.inuker.bluetooth.library.connect.listener.BluetoothStateListener;
import com.luck.picture.lib.permissions.RxPermissions;
import com.miittech.you.App;
import com.miittech.you.R;
import com.miittech.you.activity.device.DeviceDetailActivity;
import com.miittech.you.adapter.DeviceListAdapter;
import com.miittech.you.common.Common;
import com.miittech.you.global.HttpUrl;
import com.miittech.you.global.IntentExtras;
import com.miittech.you.global.Params;
import com.miittech.you.global.PubParam;
import com.miittech.you.impl.OnListItemClick;
import com.miittech.you.manager.BLEClientManager;
import com.miittech.you.net.ApiServiceManager;
import com.miittech.you.net.response.DeviceResponse;
import com.ryon.mutils.EncryptUtils;
import com.ryon.mutils.LogUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import cn.jpush.android.api.JPushInterface;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import okhttp3.MediaType;
import okhttp3.RequestBody;

/**
 * Created by Administrator on 2017/9/14.
 */

public class ListFragment extends Fragment {
    @BindView(R.id.rl_tip)
    RelativeLayout rlTip;
    @BindView(R.id.recyclerView)
    RecyclerView recyclerView;
    Unbinder unbinder;
    @BindView(R.id.ll_bluetooth_disabled)
    LinearLayout llBluetoothDisabled;
    @BindView(R.id.ll_notify_service_disabled)
    LinearLayout llNotifyServiceDisabled;
    @BindView(R.id.ll_location_service_disabled)
    LinearLayout llLocationServiceDisabled;
    @BindView(R.id.ll_background_service_disabled)
    LinearLayout llBackgroundServiceDisabled;

    private LinearLayoutManager mLayoutManager;
    private DeviceListAdapter mDeviceListAdapter;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_list, null);
        unbinder = ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        //创建默认的线性LayoutManager
        mLayoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setHasFixedSize(true);
        //创建并设置Adapter
        mDeviceListAdapter = new DeviceListAdapter(getActivity(), new OnListItemClick<DeviceResponse.DevlistBean>() {
            @Override
            public void onItemClick(DeviceResponse.DevlistBean devlistBean) {
                super.onItemClick(devlistBean);
                Intent intent = new Intent(getActivity(), DeviceDetailActivity.class);
                intent.putExtra(IntentExtras.DEVICE.DATA, devlistBean);
                startActivity(intent);
            }
        });
        recyclerView.setAdapter(mDeviceListAdapter);
        getDeviceList();
        initServiceState();
    }

    @Override
    public void onStart() {
        super.onStart();
        if(!BLEClientManager.getClient().isBluetoothOpened()){
            llBluetoothDisabled.setVisibility(View.VISIBLE);
        }else{
            llBluetoothDisabled.setVisibility(View.GONE);
        }
        RxPermissions rxPermissions = new RxPermissions(getActivity());
        if(rxPermissions.isGranted(Manifest.permission.ACCESS_FINE_LOCATION)||rxPermissions.isGranted(Manifest.permission.ACCESS_COARSE_LOCATION)){
            llLocationServiceDisabled.setVisibility(View.GONE);
        }else{
            llLocationServiceDisabled.setVisibility(View.VISIBLE);
        }
        NotificationManagerCompat manager = NotificationManagerCompat.from(App.getInstance().getApplicationContext());
        if(manager.areNotificationsEnabled()){
            llNotifyServiceDisabled.setVisibility(View.GONE);
        }else{
            llNotifyServiceDisabled.setVisibility(View.VISIBLE);
        }
//        if(Common.isAccessibilitySettingsOn(getActivity())){
//            llBackgroundServiceDisabled.setVisibility(View.GONE);
//        }else{
//            llBackgroundServiceDisabled.setVisibility(View.VISIBLE);
//        }
//
//        llBackgroundServiceDisabled.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                if (!Common.isAccessibilitySettingsOn(getActivity())) {
//                    Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
//                    startActivity(intent);
//                }
//            }
//        });
    }

    private void initServiceState() {
        BLEClientManager.getClient().registerBluetoothStateListener(new BluetoothStateListener() {
            @Override
            public void onBluetoothStateChanged(boolean openOrClosed) {
                if(openOrClosed){
                    llBluetoothDisabled.setVisibility(View.GONE);
                }else{
                    llBluetoothDisabled.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            //相当于Fragment的onResume
            getDeviceList();
        } else {
            //相当于Fragment的onPause
        }
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (hidden) {

        } else {
            getDeviceList();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        getDeviceList();
    }

    private void getDeviceList() {
        Map param = new LinkedHashMap();
        param.put("qrytype", Params.QRY_TYPE.USED);
        String json = new Gson().toJson(param);
        PubParam pubParam = new PubParam(App.getInstance().getUserId());
        String sign_unSha1 = pubParam.toValueString() + json + App.getInstance().getTocken();
        LogUtils.d("sign_unsha1", sign_unSha1);
        String sign = EncryptUtils.encryptSHA1ToString(sign_unSha1).toLowerCase();
        LogUtils.d("sign_sha1", sign);
        String path = HttpUrl.Api + "userdevicelist/" + pubParam.toUrlParam(sign);
        final RequestBody requestBody = RequestBody.create(MediaType.parse(HttpUrl.MediaType_Json), json);

        ApiServiceManager.getInstance().buildApiService(getActivity()).postDeviceOption(path, requestBody)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<DeviceResponse>() {
                    @Override
                    public void accept(DeviceResponse response) throws Exception {
                        initDeviceList(response.getDevlist());
                        if (!response.isSuccessful()) {
                            response.onError(getActivity());
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        throwable.printStackTrace();
                    }
                });
    }

    private void initDeviceList(List<DeviceResponse.DevlistBean> devlist) {
        if (devlist == null || devlist.size() == 0) {
            rlTip.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            return;
        }
        rlTip.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
        mDeviceListAdapter.updateData(devlist);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }
}
