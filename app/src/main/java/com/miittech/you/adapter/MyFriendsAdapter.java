package com.miittech.you.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Base64;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.daimajia.swipe.SimpleSwipeListener;
import com.daimajia.swipe.SwipeLayout;
import com.daimajia.swipe.adapters.RecyclerSwipeAdapter;
import com.google.gson.Gson;
import com.miittech.you.R;
import com.miittech.you.activity.user.MyFriendsActivity;
import com.miittech.you.common.Common;
import com.miittech.you.dialog.DialogUtils;
import com.miittech.you.glide.GlideApp;
import com.miittech.you.global.HttpUrl;
import com.miittech.you.global.Params;
import com.miittech.you.global.PubParam;
import com.miittech.you.impl.OnListItemClick;
import com.miittech.you.impl.OnMsgTipOptions;
import com.miittech.you.net.ApiServiceManager;
import com.miittech.you.net.response.FriendsResponse;
import com.miittech.you.weight.BtnTextView;
import com.miittech.you.weight.CircleImageView;
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

public class MyFriendsAdapter extends RecyclerSwipeAdapter<MyFriendsAdapter.SimpleViewHolder> {

    private Context mContext;
    private List<FriendsResponse.FriendlistBean> friendlist = new ArrayList<>();
    private OnListItemClick onListItemClick;

    public MyFriendsAdapter(Context context, OnListItemClick onListItemClick) {
        this.mContext = context;
        this.onListItemClick = onListItemClick;
    }

    @Override
    public SimpleViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = View.inflate(mContext,R.layout.item_friend_list,null);
        SimpleViewHolder viewHolder = new SimpleViewHolder(view);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(final SimpleViewHolder viewHolder, final int position) {
        final FriendsResponse.FriendlistBean friend = friendlist.get(position);
        GlideApp.with(mContext)
                .load(friend.getHeadimg())
                .error(R.drawable.ic_header_img)
                .placeholder(R.drawable.ic_header_img)
                .into(viewHolder.itemImage);
        viewHolder.itemName.setText(new String(Base64.decode(friend.getNickname(), Base64.DEFAULT)));
        //1：已添加 2：申请中 4：被邀请 8：已被对方删除 16：已拒绝
        viewHolder.itemFlag.setVisibility(View.GONE);
        viewHolder.swipeLayout.setShowMode(SwipeLayout.ShowMode.LayDown);
        viewHolder.swipeLayout.addSwipeListener(new SimpleSwipeListener() {
            @Override
            public void onOpen(SwipeLayout layout) {
//                YoYo.with(Techniques.Tada).duration(500).delay(100).playOn(layout.findViewById(R.id.trash));
            }
        });
        viewHolder.itemDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DialogUtils.getInstance().createMsgTipDialog(mContext)
                        .setTitle("删除好友")
                        .setMsg("是否删除该好友？")
                        .setLeftBtnText("取消")
                        .setRightBtnText("删除")
                        .setOnMsgTipOptions(new OnMsgTipOptions(){
                            @Override
                            public void onSure() {
                                super.onSure();
                                doFriendDelete(position,friend,viewHolder);
                            }

                            @Override
                            public void onCancel() {
                                super.onCancel();
                                mItemManger.closeAllItems();
                            }
                        }).show();

            }
        });
        viewHolder.itemFlag.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (friend.getState() == Params.FRIEND_STATUS.FRIEND_BE_INVITED) {
                    if (onListItemClick != null) {
                        onListItemClick.onItemFlagClick(friend);
                    }
                }
            }
        });
        mItemManger.bindView(viewHolder.itemView, position);
    }

    @Override
    public int getItemCount() {
        return friendlist.size();
    }

    @Override
    public int getSwipeLayoutResourceId(int position) {
        return R.id.swipe;
    }

    public void notifyData(List<FriendsResponse.FriendlistBean> friendlist) {
        this.friendlist.clear();
        this.friendlist.addAll(friendlist);
        notifyDataSetChanged();
    }

    public static class SimpleViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.item_delete)
        ImageView itemDelete;
        @BindView(R.id.item_image)
        CircleImageView itemImage;
        @BindView(R.id.item_name)
        TextView itemName;
        @BindView(R.id.item_flag)
        BtnTextView itemFlag;
        @BindView(R.id.swipe)
        SwipeLayout swipeLayout;
        public SimpleViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

    private void doFriendDelete(final int position , final FriendsResponse.FriendlistBean friend, final SimpleViewHolder viewHolder) {
        if(!NetworkUtils.isConnected()){
            ToastUtils.showShort(R.string.msg_net_error);
            return;
        }
        Map param = new HashMap();
        param.put("method", Params.METHOD.FRIEND_DELETE);
        param.put("friended", friend.getFriendid());
        String json = new Gson().toJson(param);
        PubParam pubParam = new PubParam(Common.getUserId());
        String sign_unSha1 = pubParam.toValueString() + json + Common.getTocken();
        LogUtils.d("sign_unsha1", sign_unSha1);
        String sign = EncryptUtils.encryptSHA1ToString(sign_unSha1).toLowerCase();
        LogUtils.d("sign_sha1", sign);
        String path = HttpUrl.Api + "friend/" + pubParam.toUrlParam(sign);
        final RequestBody requestBody = RequestBody.create(MediaType.parse(HttpUrl.MediaType_Json), json);

        ApiServiceManager.getInstance().buildApiService(mContext).postToGetFriendList(path, requestBody)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<FriendsResponse>() {
                    @Override
                    public void accept(FriendsResponse response) throws Exception {
                        if (response.isSuccessful()) {
                            mItemManger.removeShownLayouts(viewHolder.swipeLayout);
                            friendlist.remove(position);
                            notifyDataSetChanged();
                            mItemManger.closeAllItems();
                            if (onListItemClick != null) {
                                onListItemClick.onItemRemoved(friend);
                            }
                        } else {
                            response.onError(mContext);
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