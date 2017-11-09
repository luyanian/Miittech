package com.miittech.you.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.miittech.you.R;
import com.miittech.you.impl.OnListItemClick;
import com.miittech.you.net.response.FriendsResponse;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by Administrator on 2017/11/8.
 */

public class MapDeviceUsersListAdapter<T> extends RecyclerView.Adapter {
    private Context context;
    private List<T> mData;
    private OnListItemClick onListItemClick;

    public MapDeviceUsersListAdapter(Context context, List<T> mData, OnListItemClick onListItemClick) {
        this.context = context;
        this.mData = mData;
        this.onListItemClick = onListItemClick;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = View.inflate(context, R.layout.item_dialog_device_users, null);
        ViewHolder viewHolder = new ViewHolder(view);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        ViewHolder viewHolder = (ViewHolder) holder;
        final T t = mData.get(position);
        if(t instanceof FriendsResponse.FriendlistBean) {
            FriendsResponse.FriendlistBean friend = (FriendsResponse.FriendlistBean) t;
            Glide.with(context).load(friend.getHeadimg()).into(viewHolder.itemIcon);
        }
        viewHolder.itemIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(onListItemClick!=null){
                    onListItemClick.onItemClick(t);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder{
        @BindView(R.id.item_icon)
        ImageView itemIcon;

        ViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }
    }
}
