package com.miittech.you.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.miittech.you.R;
import com.miittech.you.impl.OnListItemClick;
import com.miittech.you.net.response.SoundListResponse;

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
    private OnListItemClick onListItemClick;
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
        viewHolders.put(i,viewHolder);
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showCurrentItem(i);
                if (onListItemClick != null) {
                    onListItemClick.onItemClick(mData.get(i));
                }
            }
        });
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int i) {
        ViewHolder holder = (ViewHolder) viewHolder;
        final SoundListResponse.SourndlistBean sourndlistBean = mData.get(i);
        holder.tvItem.setText(sourndlistBean.getName());
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    public void showCurrentItem(int currentItem) {
//        for (ViewHolder viewHolder:viewHolders){
//
//        }
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
