package com.miittech.you.activity.user;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.daimajia.swipe.util.Attributes;
import com.google.gson.Gson;
import com.miittech.you.R;
import com.miittech.you.activity.BaseActivity;
import com.miittech.you.adapter.MyFriendsAdapter;
import com.miittech.you.fragment.ListFragment;
import com.miittech.you.global.IntentExtras;
import com.miittech.you.utils.Common;
import com.miittech.you.global.HttpUrl;
import com.miittech.you.global.Params;
import com.miittech.you.global.PubParam;
import com.miittech.you.impl.OnListItemClick;
import com.miittech.you.impl.TitleBarOptions;
import com.miittech.you.net.ApiServiceManager;
import com.miittech.you.net.response.FriendsResponse;
import com.miittech.you.weight.Titlebar;
import com.ryon.mutils.EncryptUtils;
import com.ryon.mutils.LogUtils;
import com.ryon.mutils.NetworkUtils;
import com.scwang.smartrefresh.layout.SmartRefreshLayout;
import com.scwang.smartrefresh.layout.api.RefreshLayout;
import com.scwang.smartrefresh.layout.listener.OnRefreshListener;

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
import jp.wasabeef.recyclerview.animators.FadeInLeftAnimator;
import okhttp3.MediaType;
import okhttp3.RequestBody;

/**
 * Created by Administrator on 2017/9/25.
 */

public class MyFriendsActivity extends BaseActivity {
    @BindView(R.id.titlebar)
    Titlebar titlebar;
    @BindView(R.id.recyclerview)
    RecyclerView recyclerView;
    @BindView(R.id.tv_point)
    TextView tvPoint;
    @BindView(R.id.refreshLayout)
    SmartRefreshLayout refreshLayout;
    private MyFriendsAdapter mAdapter;
    private FriendsResponse response;
    private CmdResponseReceiver cmdResponseReceiver = new CmdResponseReceiver();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_friends);
        ButterKnife.bind(this);
        initMyTitleBar(titlebar, getResources().getString(R.string.text_setting_myfriends));
        titlebar.showBackOption()
                .showSettingOption()
                .setSettingIcon(R.drawable.ic_menu_friends_add)
                .setTitleBarOptions(new TitleBarOptions() {
                    @Override
                    public void onBack() {
                        super.onBack();
                        finish();
                    }

                    @Override
                    public void onSetting() {
                        super.onSetting();
                        Intent intent = new Intent(MyFriendsActivity.this, FriendAddActivity.class);
                        startActivity(intent);
                    }
                });
        refreshLayout.setOnRefreshListener(new OnRefreshListener() {
            @Override
            public void onRefresh(RefreshLayout refreshlayout) {
                synchronized (this) {
                    refreshlayout.finishRefresh(2000);
                    getFrinds();
                }
            }
        });

        // Layout Managers:
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(linearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(linearLayoutManager);
        // Item Decorator:
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(this,DividerItemDecoration.HORIZONTAL);
        dividerItemDecoration.setDrawable(getResources().getDrawable(R.drawable.shape_divider));
        recyclerView.addItemDecoration(dividerItemDecoration);
        recyclerView.setItemAnimator(new FadeInLeftAnimator());
        mAdapter = new MyFriendsAdapter(this,new OnListItemClick() {
            @Override
            public void onItemFlagClick(Object o) {
                super.onItemFlagClick(o);
            }

            @Override
            public void onItemRemoved(Object o) {
                super.onItemRemoved(o);


            }
        });
        mAdapter.setMode(Attributes.Mode.Single);
        recyclerView.setAdapter(mAdapter);

        IntentFilter filter=new IntentFilter();
        filter.addAction(IntentExtras.ACTION.ACTION_RECEIVE_MESSAGE);
        registerReceiver(cmdResponseReceiver,filter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        getFrinds();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(cmdResponseReceiver);
    }

    private void getFrinds() {
        if(!NetworkUtils.isConnected()){
            return;
        }
        Map param = new HashMap();
        param.put("state", Params.FRIEND_STATUS.FRIEND_BE_INVITED+Params.FRIEND_STATUS.FRIEND_AREADY_ADD);
        String json = new Gson().toJson(param);
        PubParam pubParam = new PubParam(Common.getUserId());
        String sign_unSha1 = pubParam.toValueString() + json + Common.getTocken();
        LogUtils.d("sign_unsha1", sign_unSha1);
        String sign = EncryptUtils.encryptSHA1ToString(sign_unSha1).toLowerCase();
        LogUtils.d("sign_sha1", sign);
        String path = HttpUrl.Api + "friendslist/" + pubParam.toUrlParam(sign);
        RequestBody requestBody = RequestBody.create(MediaType.parse(HttpUrl.MediaType_Json), json);

        ApiServiceManager.getInstance().buildApiService(this).postToGetFriendList(path, requestBody)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<FriendsResponse>() {
                    @Override
                    public void accept(FriendsResponse response) throws Exception {
                        if (response.isSuccessful()) {
                            initFriendList(response.getFriendlist());
                        } else {
                            response.onError(MyFriendsActivity.this);
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        throwable.printStackTrace();
                    }
                });

    }

    private void initFriendList(List<FriendsResponse.FriendlistBean> friendlist) {
        if(friendlist==null){
            return;
        }
        List<FriendsResponse.FriendlistBean> tempData = new ArrayList<>();
        int count = 0;
        for (FriendsResponse.FriendlistBean friendlistBean : friendlist) {
            if (friendlistBean.getState() == Params.FRIEND_STATUS.FRIEND_BE_INVITED) {
                count++;
            }
            if(friendlistBean.getState() == Params.FRIEND_STATUS.FRIEND_AREADY_ADD){
                tempData.add(friendlistBean);
            }
        }
        mAdapter.notifyData(tempData);
        if(count>0) {
            tvPoint.setVisibility(View.VISIBLE);
            tvPoint.setText(count+"");
        }else{
            tvPoint.setVisibility(View.GONE);
        }
    }
    @OnClick(R.id.rl_new_friends)
    public void onViewClicked() {
        Intent intent = new Intent(this,FriendBeInvitedActivity.class);
        startActivity(intent);
    }
    private class CmdResponseReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(IntentExtras.ACTION.ACTION_RECEIVE_MESSAGE)){
                int cmd = intent.getIntExtra("cmd", -1);//获取Extra信息
                switch (cmd){
                    case IntentExtras.HANDLER.MSG_HANDLER_DEVECE_LIST:
                        refreshLayout.autoRefresh();
                        break;
                }
            }
        }
    }
}
