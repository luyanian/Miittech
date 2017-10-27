package com.miittech.you.adapter;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.util.Base64;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.miittech.you.R;
import com.miittech.you.net.response.FriendsResponse;
import com.miittech.you.weight.BtnTextView;
import com.miittech.you.weight.CircleImageView;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by Administrator on 2017/9/27.
 */

public class FriendListAdapter extends BaseAdapter {

    private Context context;
    private List<FriendsResponse.FriendlistBean> friendlist;

    public FriendListAdapter(Context context, List<FriendsResponse.FriendlistBean> friendlist) {
        this.context = context;
        this.friendlist = friendlist;
    }

    @Override
    public int getCount() {
        return friendlist.size();
    }

    @Override
    public Object getItem(int i) {
        return friendlist.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        ViewHolder holder;
        if(view==null||view.getTag()==null){
            view = View.inflate(context, R.layout.item_friend_list, null);
            holder = new ViewHolder(view);
            view.setTag(holder);
        }else{
            holder = (ViewHolder) view.getTag();
        }
        FriendsResponse.FriendlistBean friend = friendlist.get(i);
        Glide.with(context).load(friend.getHeadimg()).into(holder.itemImage);
        holder.itemName.setText(new String(Base64.decode(friend.getNickname(), Base64.DEFAULT)));
        //1：已添加 2：申请中 4：被邀请 8：已被对方删除 16：已拒绝
        switch (friend.getState()){
            case 1:
                holder.itemFlag.setText(context.getResources().getString(R.string.text_friend_aready_add));
                holder.itemFlag.setTextColor(ContextCompat.getColor(context,R.color.text_friend_state1));
                break;
            case 2:
                holder.itemFlag.setText(context.getResources().getString(R.string.text_friend_applying));
                holder.itemFlag.setTextColor(ContextCompat.getColor(context,R.color.text_friend_state2));
                break;
            case 4:
                holder.itemFlag.setText(context.getResources().getString(R.string.text_friend_be_invitation));
                holder.itemFlag.setTextColor(ContextCompat.getColor(context,R.color.text_friend_state4));
                break;
            case 8:
                break;
            case 16:
                holder.itemFlag.setText(context.getResources().getString(R.string.text_friend_refuse));
                holder.itemFlag.setTextColor(ContextCompat.getColor(context,R.color.text_friend_state16));
                break;
        }

        return view;
    }

    static class ViewHolder {
        @BindView(R.id.item_image)
        CircleImageView itemImage;
        @BindView(R.id.item_name)
        TextView itemName;
        @BindView(R.id.item_flag)
        BtnTextView itemFlag;

        ViewHolder(View view) {
            ButterKnife.bind(this, view);
        }
    }
}
