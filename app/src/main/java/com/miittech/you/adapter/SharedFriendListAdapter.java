package com.miittech.you.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Base64;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.miittech.you.R;
import com.miittech.you.glide.GlideApp;
import com.miittech.you.net.response.FriendsResponse;
import com.miittech.you.weight.BtnTextView;
import com.miittech.you.weight.CircleImageView;

import java.util.ArrayList;
import java.util.List;
import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by Administrator on 2017/9/27.
 */

public class SharedFriendListAdapter extends RecyclerView.Adapter {

    private Context context;
    private List<FriendsResponse.FriendlistBean> friendlist = new ArrayList<>();

    public SharedFriendListAdapter(Context context) {
        this.context = context;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = View.inflate(context, R.layout.item_friend_list, null);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        ViewHolder viewHolder = (ViewHolder) holder;
        final FriendsResponse.FriendlistBean friend = friendlist.get(position);
        GlideApp.with(context)
                .load(friend.getHeadimg())
                .error(R.drawable.ic_header_img)
                .placeholder(R.drawable.ic_header_img)
                .into(viewHolder.itemImage);
        viewHolder.itemName.setText(new String(Base64.decode(friend.getNickname(), Base64.DEFAULT)));
    }

    @Override
    public int getItemCount() {
        return friendlist.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.item_image)
        CircleImageView itemImage;
        @BindView(R.id.item_name)
        TextView itemName;
        @BindView(R.id.item_flag)
        BtnTextView itemFlag;
        ViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }
    }
    public void updateData(List<FriendsResponse.FriendlistBean> friendlist){
        this.friendlist.clear();
        this.friendlist.addAll(friendlist);
        this.notifyDataSetChanged();
    }
}
