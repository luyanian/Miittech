package com.miittech.you.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.miittech.you.App;
import com.miittech.you.R;
import com.miittech.you.impl.TypeSelectorChangeLisener;
import com.miittech.you.net.ApiServiceManager;
import com.miittech.you.net.global.HttpUrl;
import com.miittech.you.net.global.Params;
import com.miittech.you.net.global.PubParam;
import com.miittech.you.net.response.UserInfoResponse;
import com.miittech.you.weight.TypeSelector;
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

public class EventsFragment extends Fragment {
    @BindView(R.id.typeSelector)
    TypeSelector typeSelector;
    @BindView(R.id.recyclerview)
    RecyclerView recyclerview;
    @BindView(R.id.refreshLayout)
    SmartRefreshLayout refreshLayout;
    Unbinder unbinder;

    private String sid;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_events, null);
        unbinder = ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        typeSelector.setItemText("轨迹追中","事件报告");
        typeSelector.setTypeSelectorChangeLisener(new TypeSelectorChangeLisener() {
            @Override
            public void onTabSelectorChanged(int item) {
                refreshLayout.autoRefresh();
            }
        });
        refreshLayout.setOnRefreshListener(new OnRefreshListener() {
            @Override
            public void onRefresh(RefreshLayout refreshlayout) {
                refreshlayout.finishRefresh(2000);
                sid="0";
                getEventList();
            }
        });
        refreshLayout.setOnLoadmoreListener(new OnLoadmoreListener() {
            @Override
            public void onLoadmore(RefreshLayout refreshlayout) {
                refreshlayout.finishLoadmore(2000);
            }
        });
        typeSelector.setSelectItem(0);
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            //相当于Fragment的onResume
        } else {
            //相当于Fragment的onPause
        }
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (hidden) {

        } else {

        }
    }

    private void getEventList() {
        Map param = new HashMap();
        int item = typeSelector.getSelectItem();
        if(item==0){
            param.put("qrytype", Params.QRY_TYPE.TRACE);
        }else{
            param.put("qrytype", Params.QRY_TYPE.EVENTLOG);
        }
        param.put("dir", 0);
        param.put("len", 10);
        param.put("sid", sid);

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd");
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        param.put("sdate", simpleDateFormat.format(calendar.getTime()));
        calendar.add(Calendar.MONTH, -1);
        param.put("edate", simpleDateFormat.format(calendar.getTime()));

        String json = new Gson().toJson(param);
        PubParam pubParam = new PubParam(App.getUserId());
        String sign_unSha1 = pubParam.toValueString() + json + App.getTocken();
        LogUtils.d("sign_unsha1", sign_unSha1);
        String sign = EncryptUtils.encryptSHA1ToString(sign_unSha1).toLowerCase();
        LogUtils.d("sign_sha1", sign);
        String path = HttpUrl.Api + "userinfo/" + pubParam.toUrlParam(sign);
        RequestBody requestBody = RequestBody.create(MediaType.parse(HttpUrl.MediaType_Json), json);
        ApiServiceManager.getInstance().buildApiService(getActivity()).postNetRequestObject(path, requestBody)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<JsonObject>() {
                    @Override
                    public void accept(JsonObject jsonObject) throws Exception {
                        String sfds = jsonObject.getAsString().toString();
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        throwable.printStackTrace();
                    }
                });
//        ApiServiceManager.getInstance().buildApiService(getActivity()).postToGetUserInfo(path, requestBody)
//                .subscribeOn(Schedulers.io())
//                .observeOn(AndroidSchedulers.mainThread())
//                .subscribe(new Consumer<UserInfoResponse>() {
//                    @Override
//                    public void accept(UserInfoResponse response) throws Exception {
//                        if (response.isSuccessful()) {
//
//                        } else {
//                            response.getUserinfo();
//                        }
//                    }
//                }, new Consumer<Throwable>() {
//                    @Override
//                    public void accept(Throwable throwable) throws Exception {
//                        throwable.printStackTrace();
//                    }
//                });

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }
}
