package com.miittech.you.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.miittech.you.App;
import com.miittech.you.R;
import com.miittech.you.activity.event.EventTraceDetailActivity;
import com.miittech.you.adapter.DeviceListAdapter;
import com.miittech.you.global.IntentExtras;
import com.miittech.you.global.SPConst;
import com.miittech.you.impl.OnListItemClick;
import com.miittech.you.net.ApiServiceManager;
import com.miittech.you.global.HttpUrl;
import com.miittech.you.global.Params;
import com.miittech.you.global.PubParam;
import com.miittech.you.net.response.DeviceResponse;
import com.miittech.you.net.response.UserInfoResponse;
import com.ryon.mutils.EncryptUtils;
import com.ryon.mutils.LogUtils;
import com.ryon.mutils.NetworkUtils;
import com.ryon.mutils.SPUtils;
import com.scwang.smartrefresh.layout.SmartRefreshLayout;
import com.scwang.smartrefresh.layout.api.RefreshLayout;
import com.scwang.smartrefresh.layout.listener.OnLoadmoreListener;
import com.scwang.smartrefresh.layout.listener.OnRefreshListener;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
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
 * Created by ryon on 2017/10/28.
 */

public class TraceFragment extends Fragment {
    @BindView(R.id.recyclerview)
    RecyclerView recyclerview;
    @BindView(R.id.rl_tip)
    RelativeLayout rlTip;
    @BindView(R.id.refreshLayout)
    SmartRefreshLayout refreshLayout;
    Unbinder unbinder;
    private DeviceListAdapter mDeviceListAdapter;

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        refreshLayout.setOnRefreshListener(new OnRefreshListener() {
            @Override
            public void onRefresh(RefreshLayout refreshlayout) {
                refreshlayout.finishRefresh(2000);
                getDeviceList();
            }
        });
        refreshLayout.setOnLoadmoreListener(new OnLoadmoreListener() {
            @Override
            public void onLoadmore(RefreshLayout refreshlayout) {
                refreshlayout.finishLoadmore(2000);
            }
        });
        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        recyclerview.setLayoutManager(layoutManager);
        recyclerview.setHasFixedSize(true);
        mDeviceListAdapter = new DeviceListAdapter(getActivity(),new OnListItemClick(){
            @Override
            public void onItemClick(Object o) {
                super.onItemClick(o);
                DeviceResponse.DevlistBean devlistBean = (DeviceResponse.DevlistBean) o;
                Intent intent = new Intent(getActivity(), EventTraceDetailActivity.class);
                intent.putExtra(IntentExtras.DEVICE.ID,devlistBean.getDevidX());
                startActivity(intent);
            }
        });
        recyclerview.setAdapter(mDeviceListAdapter);
        refreshLayout.autoRefresh();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_trace, null);
        unbinder = ButterKnife.bind(this, view);
        return view;
    }

    private void getDeviceList() {
        if(!NetworkUtils.isConnected()){
            DeviceResponse response = (DeviceResponse) SPUtils.getInstance().readObject(SPConst.DATA.DEVICELIST);
            if(response!=null){
                initDeviceList(response.getDevlist());
            }
            return;
        }
        Map param = new LinkedHashMap();
        param.put("qrytype", Params.QRY_TYPE.ALL);
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
                        SPUtils.getInstance().remove(SPConst.DATA.DEVICELIST);
                        SPUtils.getInstance().saveObject(SPConst.DATA.DEVICELIST,response);
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
            recyclerview.setVisibility(View.GONE);
        }else{
            rlTip.setVisibility(View.GONE);
            recyclerview.setVisibility(View.VISIBLE);
        }
        mDeviceListAdapter.updateData(devlist);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if(mDeviceListAdapter!=null){
            mDeviceListAdapter.unregist();
        }
        unbinder.unbind();
    }
}
