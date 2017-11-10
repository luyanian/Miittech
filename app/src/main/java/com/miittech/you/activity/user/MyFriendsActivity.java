package com.miittech.you.activity.user;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ListView;

import com.google.gson.Gson;
import com.miittech.you.App;
import com.miittech.you.R;
import com.miittech.you.activity.BaseActivity;
import com.miittech.you.adapter.FriendListAdapter;
import com.miittech.you.impl.OnListItemClick;
import com.miittech.you.net.ApiServiceManager;
import com.miittech.you.global.HttpUrl;
import com.miittech.you.global.Params;
import com.miittech.you.global.PubParam;
import com.miittech.you.net.response.FriendsResponse;
import com.miittech.you.weight.Titlebar;
import com.miittech.you.impl.TitleBarOptions;
import com.ryon.mutils.EncryptUtils;
import com.ryon.mutils.LogUtils;

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
 * Created by Administrator on 2017/9/25.
 */

public class MyFriendsActivity extends BaseActivity {
    @BindView(R.id.titlebar)
    Titlebar titlebar;
    @BindView(R.id.listview)
    ListView listview;
    private FriendListAdapter mAdapter;
    List<FriendsResponse.FriendlistBean> friendlist = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_friends);
        ButterKnife.bind(this);
        initMyTitleBar(titlebar,getResources().getString(R.string.text_setting_myfriends));
        titlebar.showBackOption()
                .showSettingOption()
                .setSettingIcon(R.drawable.ic_friends_add)
                .setTitleBarOptions(new TitleBarOptions(){
                    @Override
                    public void onBack() {
                        super.onBack();
                        finish();
                    }

                    @Override
                    public void onSetting() {
                        super.onSetting();
                        Intent intent=new Intent(MyFriendsActivity.this,FriendAddActivity.class);
                        startActivity(intent);
                    }
                });
        mAdapter = new FriendListAdapter(this,friendlist,new OnListItemClick(){
            @Override
            public void onItemFlagClick(Object o) {
                super.onItemFlagClick(o);
                FriendsResponse.FriendlistBean friend = (FriendsResponse.FriendlistBean)o;
                doPassApply(friend.getFriendid());
            }
        });
        listview.setAdapter(mAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        getFrinds();
    }

    private void getFrinds() {
        Map param = new HashMap();
        param.put("state", Params.FRIEND_STATUS.FRIEND_APPLYING
                +Params.FRIEND_STATUS.FRIEND_REFUSED
                +Params.FRIEND_STATUS.FRIEND_AREADY_ADD
                +Params.FRIEND_STATUS.FRIEND_BE_INVITED);

        String json = new Gson().toJson(param);
        PubParam pubParam = new PubParam(App.getInstance().getUserId());
        String sign_unSha1 = pubParam.toValueString() + json + App.getInstance().getTocken();
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
        this.friendlist.clear();
        this.friendlist.addAll(friendlist);
        mAdapter.notifyDataSetChanged();
    }

    private void doPassApply(String friendId) {
        Map param = new HashMap();
        param.put("method", Params.METHOD.FRIEND_CONFIRM);
        param.put("friended", friendId);
        String json = new Gson().toJson(param);
        PubParam pubParam = new PubParam(App.getInstance().getUserId());
        String sign_unSha1 = pubParam.toValueString() + json + App.getInstance().getTocken();
        LogUtils.d("sign_unsha1", sign_unSha1);
        String sign = EncryptUtils.encryptSHA1ToString(sign_unSha1).toLowerCase();
        LogUtils.d("sign_sha1", sign);
        String path = HttpUrl.Api + "friend/" + pubParam.toUrlParam(sign);
        final RequestBody requestBody = RequestBody.create(MediaType.parse(HttpUrl.MediaType_Json), json);

        ApiServiceManager.getInstance().buildApiService(this).postToGetFriendList(path, requestBody)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<FriendsResponse>() {
                    @Override
                    public void accept(FriendsResponse response) throws Exception {
                        if(response.isSuccessful()){
                            getFrinds();
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
