package com.miittech.you.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.miittech.you.R;
import com.miittech.you.common.SoundPlayUtils;
import com.miittech.you.dialog.DialogUtils;
import com.miittech.you.impl.OnListItemClick;
import com.miittech.you.net.response.DeviceInfoResponse;
import com.miittech.you.net.response.SoundListResponse;
import com.ryon.mutils.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by Administrator on 2017/10/19.
 */

public class SelectRingAdapter extends RecyclerView.Adapter {

    List<SoundListResponse.SourndlistBean> mData;
    private Context activity;
    private OnListItemClick<SoundListResponse.SourndlistBean> onListItemClick;
    private Map<Integer,ViewHolder> viewHolders = new HashMap<>();

    public SelectRingAdapter(Context activity, List<SoundListResponse.SourndlistBean> mData, OnListItemClick onDeviceItemClick) {
        this.activity = activity;
        this.mData = mData;
        this.onListItemClick = onDeviceItemClick;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, final int i) {
        View view = View.inflate(activity, R.layout.item_device_select_rings, null);
        ViewHolder viewHolder = new ViewHolder(view);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, final int i) {
        ViewHolder holder = (ViewHolder) viewHolder;
        viewHolders.put(i,holder);
        final SoundListResponse.SourndlistBean sourndlistBean = mData.get(i);
        holder.tvItem.setText(sourndlistBean.getName());
        holder.imgItem.setVisibility(View.GONE);
        holder.rlItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showCurrentItem(i);
                if (onListItemClick != null) {
                    onListItemClick.onItemClick(sourndlistBean);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    public void showCurrentItem(int currentItem) {
        for (Map.Entry<Integer, ViewHolder> entry: viewHolders.entrySet()) {
            ViewHolder value = entry.getValue();
            if(entry.getKey()==currentItem){
                value.imgItem.setVisibility(View.VISIBLE);
            }else{
                value.imgItem.setVisibility(View.GONE);
            }
        }
        int index = (currentItem+1+2)%4;
        SoundPlayUtils.playSound(index,false,10000);
    }

    public void initSelectAlerName(DeviceInfoResponse.UserinfoBean.DevinfoBean deviceInfo) {
        if(deviceInfo!=null&&deviceInfo.getAlertinfo()!=null){
            for(int i=0;i<mData.size();i++){
                if(mData.get(i).getId()==deviceInfo.getAlertinfo().getId()){
                    showCurrentItem(i);
                    if (onListItemClick != null) {
                        onListItemClick.onItemClick(mData.get(i));
                    }
                    return;
                }
            }
        }
        showCurrentItem(0);
        if (onListItemClick != null) {
            onListItemClick.onItemClick(mData.get(0));
        }
    }


    static class ViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.tv_item)
        TextView tvItem;
        @BindView(R.id.img_item)
        ImageView imgItem;
        @BindView(R.id.rl_item)
        RelativeLayout rlItem;

        ViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }
    }
}
