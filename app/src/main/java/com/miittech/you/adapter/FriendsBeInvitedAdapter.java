package com.miittech.you.adapter;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Base64;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.daimajia.swipe.SimpleSwipeListener;
import com.daimajia.swipe.SwipeLayout;
import com.daimajia.swipe.adapters.RecyclerSwipeAdapter;
import com.miittech.you.R;
import com.miittech.you.glide.GlideApp;
import com.miittech.you.global.Params;
import com.miittech.you.impl.OnListItemClick;
import com.miittech.you.net.response.FriendsResponse;
import com.miittech.you.weight.BtnTextView;
import com.miittech.you.weight.CircleImageView;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class FriendsBeInvitedAdapter extends RecyclerView.Adapter {

    private Context mContext;
    private List<FriendsResponse.FriendlistBean> friendlist = new ArrayList<>();
    private OnListItemClick onListItemClick;

    public FriendsBeInvitedAdapter(Context context, OnListItemClick onListItemClick) {
        this.mContext = context;
        this.onListItemClick = onListItemClick;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = View.inflate(mContext,R.layout.item_friend_be_invated_list,null);
        ViewHolder viewHolder = new ViewHolder(view);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
        final FriendsResponse.FriendlistBean friend = friendlist.get(position);
        ViewHolder viewHolder = (ViewHolder)holder;
        GlideApp.with(mContext)
                .load(friend.getHeadimg())
                .error(R.drawable.ic_header_img)
                .placeholder(R.drawable.ic_header_img)
                .into(viewHolder.itemImage);
        viewHolder.itemName.setText(new String(Base64.decode(friend.getNickname(), Base64.DEFAULT)));
        //1：已添加 2：申请中 4：被邀请 8：已被对方删除 16：已拒绝
        switch (friend.getState()) {
            case Params.FRIEND_STATUS.FRIEND_AREADY_ADD:
                viewHolder.itemFlag.setText(mContext.getResources().getString(R.string.text_friend_aready_add));
                viewHolder.itemFlag.setTextColor(ContextCompat.getColor(mContext, R.color.text_friend_state1));
                break;
            case Params.FRIEND_STATUS.FRIEND_APPLYING:
                viewHolder.itemFlag.setText(mContext.getResources().getString(R.string.text_friend_applying));
                viewHolder.itemFlag.setTextColor(ContextCompat.getColor(mContext, R.color.text_friend_state2));
                break;
            case Params.FRIEND_STATUS.FRIEND_BE_INVITED:
                viewHolder.itemFlag.setText(mContext.getResources().getString(R.string.text_friend_be_invitation));
                viewHolder.itemFlag.setTextColor(ContextCompat.getColor(mContext, R.color.text_friend_state4));
                break;
            case Params.FRIEND_STATUS.FRIEND_BE_DELETE:
                break;
            case Params.FRIEND_STATUS.FRIEND_REFUSED:
                viewHolder.itemFlag.setText(mContext.getResources().getString(R.string.text_friend_refuse));
                viewHolder.itemFlag.setTextColor(ContextCompat.getColor(mContext, R.color.text_friend_state16));
                break;
        }

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
    }

    @Override
    public int getItemCount() {
        return friendlist.size();
    }

    public void notifyData(List<FriendsResponse.FriendlistBean> friendlist) {
        this.friendlist.clear();
        this.friendlist.addAll(friendlist);
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.item_image)
        CircleImageView itemImage;
        @BindView(R.id.item_name)
        TextView itemName;
        @BindView(R.id.item_flag)
        BtnTextView itemFlag;
        public ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

}