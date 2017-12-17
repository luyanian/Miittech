package com.miittech.you.activity.user;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.daimajia.swipe.util.Attributes;
import com.google.gson.Gson;
import com.miittech.you.R;
import com.miittech.you.activity.BaseActivity;
import com.miittech.you.adapter.FriendsBeInvitedAdapter;
import com.miittech.you.adapter.MyFriendsAdapter;
import com.miittech.you.common.Common;
import com.miittech.you.dialog.DialogUtils;
import com.miittech.you.global.HttpUrl;
import com.miittech.you.global.Params;
import com.miittech.you.global.PubParam;
import com.miittech.you.impl.OnListItemClick;
import com.miittech.you.impl.OnMsgTipOptions;
import com.miittech.you.impl.TitleBarOptions;
import com.miittech.you.net.ApiServiceManager;
import com.miittech.you.net.response.FriendsResponse;
import com.miittech.you.weight.Titlebar;
import com.ryon.mutils.EncryptUtils;
import com.ryon.mutils.LogUtils;
import com.scwang.smartrefresh.layout.SmartRefreshLayout;

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
 * Created by ryon on 2017/12/15.
 */

public class FriendBeInvitedActivity extends BaseActivity{
    @BindView(R.id.titlebar)
    Titlebar titlebar;
    @BindView(R.id.recyclerview)
    RecyclerView recyclerView;
    @BindView(R.id.refreshLayout)
    SmartRefreshLayout refreshLayout;
    @BindView(R.id.rl_new_friends)
    RelativeLayout rlNewFriend;
    private FriendsBeInvitedAdapter mAdapter;
    private FriendsResponse response;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_friends);
        ButterKnife.bind(this);
        rlNewFriend.setVisibility(View.GONE);
        initMyTitleBar(titlebar, getResources().getString(R.string.text_setting_myfriends));
        titlebar.showBackOption()
                .showSettingOption()
                .setSettingIcon(R.drawable.ic_friends_add)
                .setTitleBarOptions(new TitleBarOptions() {
                    @Override
                    public void onBack() {
                        super.onBack();
                        finish();
                    }

                    @Override
                    public void onSetting() {
                        super.onSetting();
                        Intent intent = new Intent(FriendBeInvitedActivity.this, FriendAddActivity.class);
                        startActivity(intent);
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
        mAdapter = new FriendsBeInvitedAdapter(this,new OnListItemClick() {
            @Override
            public void onItemFlagClick(Object o) {
                super.onItemFlagClick(o);
                final FriendsResponse.FriendlistBean friend = (FriendsResponse.FriendlistBean) o;
                DialogUtils.getInstance().createMsgTipDialog(FriendBeInvitedActivity.this)
                        .setLeftBtnText("拒绝")
                        .setRightBtnText("同意")
                        .setTitle("好友添加申请")
                        .setMsg("是否接受好友添加请求？")
                        .setOnMsgTipOptions(new OnMsgTipOptions(){
                            @Override
                            public void onSure() {
                                super.onSure();
                                doFriendOption(friend.getFriendid(),Params.METHOD.FRIEND_CONFIRM);
                            }

                            @Override
                            public void onCancel() {
                                super.onCancel();
                                doFriendOption(friend.getFriendid(),Params.METHOD.FRIEND_REFUSE);
                            }
                        });

            }
        });
        recyclerView.setAdapter(mAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        getFrinds();
    }

    private void getFrinds() {
        Map param = new HashMap();
        param.put("state",Params.FRIEND_STATUS.FRIEND_BE_INVITED);

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
                            response.onError(FriendBeInvitedActivity.this);
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
        mAdapter.notifyData(friendlist);

    }

    private void doFriendOption(String friendId,String method) {
        Map param = new HashMap();
        param.put("method",method);
        param.put("friended", friendId);
        String json = new Gson().toJson(param);
        PubParam pubParam = new PubParam(Common.getUserId());
        String sign_unSha1 = pubParam.toValueString() + json + Common.getTocken();
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
                        if (response.isSuccessful()) {
                            getFrinds();
                        } else {
                            response.onError(FriendBeInvitedActivity.this);
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
