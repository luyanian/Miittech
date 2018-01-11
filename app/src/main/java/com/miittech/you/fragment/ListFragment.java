package com.miittech.you.fragment;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import com.google.gson.Gson;
import com.luck.picture.lib.permissions.RxPermissions;
import com.miittech.you.App;
import com.miittech.you.R;
import com.miittech.you.activity.device.DeviceDetailActivity;
import com.miittech.you.adapter.DeviceListAdapter;
import com.miittech.you.ble.BleClient;
import com.miittech.you.utils.Common;
import com.miittech.you.entity.DeviceInfo;
import com.miittech.you.global.HttpUrl;
import com.miittech.you.global.IntentExtras;
import com.miittech.you.global.Params;
import com.miittech.you.global.PubParam;
import com.miittech.you.global.SPConst;
import com.miittech.you.impl.OnListItemClick;
import com.miittech.you.net.ApiServiceManager;
import com.miittech.you.net.response.DeviceListResponse;
import com.ryon.mutils.EncryptUtils;
import com.ryon.mutils.LogUtils;
import com.ryon.mutils.NetworkUtils;
import com.ryon.mutils.SPUtils;
import com.ryon.mutils.ToastUtils;
import com.scwang.smartrefresh.layout.SmartRefreshLayout;
import com.scwang.smartrefresh.layout.api.RefreshLayout;
import com.scwang.smartrefresh.layout.listener.OnRefreshListener;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
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
    @BindView(R.id.refreshLayout)
    SmartRefreshLayout refreshLayout;

    private CmdResponseReceiver cmdResponseReceiver = new CmdResponseReceiver();
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
        recyclerView.setItemViewCacheSize(10);

        refreshLayout.setOnRefreshListener(new OnRefreshListener() {
            @Override
            public void onRefresh(RefreshLayout refreshlayout) {
                refreshlayout.finishRefresh(2000);
                getDeviceList(true);
            }
        });
        //创建并设置Adapter
        mDeviceListAdapter = new DeviceListAdapter(getActivity(), new OnListItemClick<DeviceInfo>() {
            @Override
            public void onItemClick(DeviceInfo devlistBean, String flag) {
                super.onItemClick(devlistBean, flag);
                Intent intent = new Intent(getActivity(), DeviceDetailActivity.class);
                intent.putExtra(IntentExtras.DEVICE.DATA, devlistBean);
                intent.putExtra("location", flag);
                startActivity(intent);
            }
        });
        recyclerView.setAdapter(mDeviceListAdapter);
        initState();
        IntentFilter filter=new IntentFilter();
        filter.addAction(IntentExtras.ACTION.ACTION_CMD_RESPONSE);
        getActivity().registerReceiver(cmdResponseReceiver,filter);
    }

    public void initState() {
        if (BleClient.getInstance().isEnabled()) {
            if(llBluetoothDisabled!=null) {
                llBluetoothDisabled.setVisibility(View.GONE);
            }
        } else {
            if(llBluetoothDisabled!=null) {
                llBluetoothDisabled.setVisibility(View.VISIBLE);
            }
        }
        RxPermissions rxPermissions = new RxPermissions(getActivity());
        if (rxPermissions.isGranted(Manifest.permission.ACCESS_FINE_LOCATION) || rxPermissions.isGranted(Manifest.permission.ACCESS_COARSE_LOCATION)) {
            if(llLocationServiceDisabled!=null) {
                llLocationServiceDisabled.setVisibility(View.GONE);
            }
        } else {
            if(llLocationServiceDisabled!=null) {
                llLocationServiceDisabled.setVisibility(View.VISIBLE);
            }
        }
        NotificationManagerCompat manager = NotificationManagerCompat.from(App.getInstance().getApplicationContext());
        if (manager.areNotificationsEnabled()) {
            if(llNotifyServiceDisabled!=null) {
                llNotifyServiceDisabled.setVisibility(View.GONE);
            }
        } else {
            if(llNotifyServiceDisabled!=null) {
                llNotifyServiceDisabled.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            getDeviceList(false);
            initState();
        } else {
        }
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (hidden) {

        } else {
            getDeviceList(false);
            initState();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        initState();
        getDeviceList(false);
    }

    private void getDeviceList(boolean isFromNet) {
        if(!isFromNet){
            DeviceListResponse response = (DeviceListResponse) SPUtils.getInstance().readObject(SPConst.DATA.DEVICELIST);
            if (response != null) {
                initDeviceList(response.getDevlist());
            }
            return;
        }
        if(!NetworkUtils.isConnected()){
            ToastUtils.showShort(R.string.msg_net_error);
            return;
        }
        Map param = new LinkedHashMap();
        param.put("qrytype", Params.QRY_TYPE.ALL);
        String json = new Gson().toJson(param);
        PubParam pubParam = new PubParam(Common.getUserId());
        String sign_unSha1 = pubParam.toValueString() + json + Common.getTocken();
        LogUtils.d("sign_unsha1", sign_unSha1);
        String sign = EncryptUtils.encryptSHA1ToString(sign_unSha1).toLowerCase();
        LogUtils.d("sign_sha1", sign);
        String path = HttpUrl.Api + "userdevicelist/" + pubParam.toUrlParam(sign);
        final RequestBody requestBody = RequestBody.create(MediaType.parse(HttpUrl.MediaType_Json), json);

        ApiServiceManager.getInstance().buildApiService(getActivity()).postDeviceOption(path, requestBody)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<DeviceListResponse>() {
                    @Override
                    public void accept(DeviceListResponse response) throws Exception {
                        SPUtils.getInstance().remove(SPConst.DATA.DEVICELIST);
                        SPUtils.getInstance().saveObject(SPConst.DATA.DEVICELIST, response);
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

    private void initDeviceList(List<DeviceInfo> devlist) {
        if (devlist == null || devlist.size() == 0) {
            rlTip.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            rlTip.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
        mDeviceListAdapter.updateData(devlist);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mDeviceListAdapter != null) {
            mDeviceListAdapter.unregist();
        }
        getActivity().unregisterReceiver(cmdResponseReceiver);
        unbinder.unbind();
    }

    private class CmdResponseReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(IntentExtras.ACTION.ACTION_CMD_RESPONSE)){
                int ret = intent.getIntExtra("ret", -1);//获取Extra信息
                String address = intent.getStringExtra("address");
                switch (ret){
                    case IntentExtras.RET.RET_BLE_STATE_ON:
                        llBluetoothDisabled.setVisibility(View.GONE);
                        break;
                    case IntentExtras.RET.RET_BLE_STATE_OFF:
                        llBluetoothDisabled.setVisibility(View.VISIBLE);
                        break;
                }

            }
        }
    }
}
