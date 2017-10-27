package com.miittech.you.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.miittech.you.R;
import com.miittech.you.impl.OnDeviceItemClick;
import com.miittech.you.net.response.DeviceResponse;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by Administrator on 2017/10/19.
 */

public class DeviceListAdapter extends RecyclerView.Adapter {

    private List<DeviceResponse.DevlistBean> mData;
    private Context context;
    private OnDeviceItemClick onDeviceItemClick;

    public DeviceListAdapter(Context context, List<DeviceResponse.DevlistBean> mData,OnDeviceItemClick onDeviceItemClick) {
        this.context = context;
        this.mData = mData;
        this.onDeviceItemClick = onDeviceItemClick;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, final int i) {
        View view = View.inflate(context, R.layout.item_device_list, null);
        ViewHolder viewHolder = new ViewHolder(view);
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(onDeviceItemClick!=null){
                    onDeviceItemClick.onItemClick(mData.get(i));
                }
            }
        });
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int i) {
        DeviceResponse.DevlistBean devlistBean = mData.get(i);
        ViewHolder holder = (ViewHolder) viewHolder;
        holder.itemTitle.setText(devlistBean.getDevname());
        holder.itemLocation.setText(devlistBean.getLocinfo().getAddr());
        holder.itemLocation.setText(devlistBean.getLasttime());
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.item_icon)
        ImageView itemIcon;
        @BindView(R.id.item_title)
        TextView itemTitle;
        @BindView(R.id.item_location)
        TextView itemLocation;
        @BindView(R.id.item_time)
        TextView itemTime;

        ViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }
    }
}
