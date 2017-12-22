package com.miittech.you.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.miittech.you.R;
import com.miittech.you.common.Common;
import com.miittech.you.entity.DeviceInfo;
import com.miittech.you.glide.GlideApp;
import com.miittech.you.impl.OnListItemClick;
import com.miittech.you.net.response.DeviceListResponse;
import com.miittech.you.net.response.FriendsResponse;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by Administrator on 2017/11/8.
 */

public class MapDeviceUsersListAdapter extends RecyclerView.Adapter {
    private Context context;
    private List<Object> mData = new ArrayList<>();
    private OnListItemClick onListItemClick;

    public MapDeviceUsersListAdapter(Context context) {
        this.context = context;
    }

    public void setOnListItemClick(OnListItemClick onListItemClick){
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
        final Object object = mData.get(position);
        if(object instanceof FriendsResponse.FriendlistBean) {
            FriendsResponse.FriendlistBean friend = (FriendsResponse.FriendlistBean) object;
            GlideApp.with(context)
                    .load(friend.getHeadimg())
                    .error(R.drawable.ic_header_img)
                    .placeholder(R.drawable.ic_header_img)
                    .into(viewHolder.itemIcon);
        }
        if(object instanceof DeviceInfo){
            DeviceInfo device = (DeviceInfo) object;
            GlideApp.with(context)
                    .load(device.getDevimg())
                    .error(Common.getDefaultDevImgResouceId(Common.decodeBase64(device.getGroupname())))
                    .placeholder(Common.getDefaultDevImgResouceId(Common.decodeBase64(device.getGroupname())))
                    .into(viewHolder.itemIcon);
        }
        viewHolder.itemIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(onListItemClick!=null){
                    onListItemClick.onItemClick(object);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    public void setData(List<Object> list) {
        mData.clear();
        mData.addAll(list);
        this.notifyDataSetChanged();
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
