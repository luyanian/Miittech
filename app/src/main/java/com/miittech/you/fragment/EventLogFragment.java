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
import com.google.gson.Gson;
import com.miittech.you.R;
import com.miittech.you.activity.event.EventLogDetailActivity;
import com.miittech.you.adapter.EventLogAdapter;
import com.miittech.you.common.Common;
import com.miittech.you.impl.OnListItemClick;
import com.miittech.you.net.ApiServiceManager;
import com.miittech.you.global.HttpUrl;
import com.miittech.you.global.Params;
import com.miittech.you.global.PubParam;
import com.miittech.you.net.response.UserInfoResponse;
import com.ryon.mutils.EncryptUtils;
import com.ryon.mutils.LogUtils;
import com.scwang.smartrefresh.layout.SmartRefreshLayout;
import com.scwang.smartrefresh.layout.api.RefreshLayout;
import com.scwang.smartrefresh.layout.listener.OnLoadmoreListener;
import com.scwang.smartrefresh.layout.listener.OnRefreshListener;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
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

public class EventLogFragment extends Fragment {
    @BindView(R.id.recyclerview)
    RecyclerView recyclerview;
    @BindView(R.id.refreshLayout)
    SmartRefreshLayout refreshLayout;
    Unbinder unbinder;

    private EventLogAdapter eventLogAdapter;
    private String sid="0";
    private String dir="0";
    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        refreshLayout.setOnRefreshListener(new OnRefreshListener() {
            @Override
            public void onRefresh(RefreshLayout refreshlayout) {
                synchronized (this) {
                    refreshlayout.finishRefresh(2000);
                    sid = "0";
                    dir = "1";
                    getEventList();
                }
            }
        });
        refreshLayout.setOnLoadmoreListener(new OnLoadmoreListener() {
            @Override
            public void onLoadmore(RefreshLayout refreshlayout) {
                synchronized (this) {
                    refreshlayout.finishLoadmore(2000);
                    dir = "2";
                    getEventList();
                }
            }
        });
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(getActivity());
        recyclerview.setLayoutManager(mLayoutManager);
        recyclerview.setHasFixedSize(true);
        //创建并设置Adapter
        eventLogAdapter = new EventLogAdapter(getActivity(), new OnListItemClick<UserInfoResponse.EventlistBean>() {
            @Override
            public void onItemClick(UserInfoResponse.EventlistBean eventlistBean) {
                super.onItemClick(eventlistBean);
                Intent intent = new Intent(EventLogFragment.this.getActivity(), EventLogDetailActivity.class);
                intent.putExtra("eventlistBean",eventlistBean);
                startActivity(intent);
            }
        });
        recyclerview.setAdapter(eventLogAdapter);
        getEventList();
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if(isVisibleToUser){
            refreshLayout.autoRefresh();
        }else {

        }
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if(hidden){

        }else {
            refreshLayout.autoRefresh();
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_event_log, null);
        unbinder = ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }
    private synchronized void getEventList() {
        Map param = new HashMap();
        param.put("qrytype", Params.QRY_TYPE.EVENTLOG);
        param.put("dir", dir);
        param.put("len", 10);
        param.put("sid", sid);

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd");
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        param.put("edate", simpleDateFormat.format(calendar.getTime()));
        calendar.add(Calendar.MONTH, -1);
        param.put("sdate", simpleDateFormat.format(calendar.getTime()));

        String json = new Gson().toJson(param);
        PubParam pubParam = new PubParam(Common.getUserId());
        String sign_unSha1 = pubParam.toValueString() + json + Common.getTocken();
        LogUtils.d("sign_unsha1", sign_unSha1);
        String sign = EncryptUtils.encryptSHA1ToString(sign_unSha1).toLowerCase();
        LogUtils.d("sign_sha1", sign);
        String path = HttpUrl.Api + "userinfo/" + pubParam.toUrlParam(sign);
        final RequestBody requestBody = RequestBody.create(MediaType.parse(HttpUrl.MediaType_Json), json);
        ApiServiceManager.getInstance().buildApiService(getActivity()).postToGetUserInfo(path, requestBody)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<UserInfoResponse>() {
                    @Override
                    public void accept(UserInfoResponse response) throws Exception {
                        if (response.isSuccessful()) {
                            if("0".equals(dir)){
                                eventLogAdapter.refreshEventLog(response.getEventlist());
                            }else if("2".equals(dir)){
                                List<UserInfoResponse.EventlistBean> list = response.getEventlist();
                                if(list!=null) {
                                    UserInfoResponse.EventlistBean eventlistBean = list.get(list.size()-1);
                                    if(eventlistBean!=null){
                                        sid=eventlistBean.getEventid();
                                    }
                                    eventLogAdapter.loadMoreEventLog(response.getEventlist());
                                }
                            }
                        }else{
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

}
