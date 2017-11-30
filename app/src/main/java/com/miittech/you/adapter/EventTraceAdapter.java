package com.miittech.you.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.miittech.you.R;
import com.miittech.you.common.Common;
import com.miittech.you.impl.OnListItemClick;
import com.miittech.you.net.response.DeviceResponse;
import com.miittech.you.net.response.UserInfoResponse;
import com.ryon.mutils.TimeUtils;

import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by Administrator on 2017/10/19.
 */

public class EventTraceAdapter extends RecyclerView.Adapter {

    private List<UserInfoResponse.TracelistBean> traceList = new ArrayList<>();
    private Context context;
    private OnListItemClick onDeviceItemClick;

    public EventTraceAdapter(Context context,OnListItemClick onDeviceItemClick) {
        this.context = context;
        this.onDeviceItemClick = onDeviceItemClick;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, final int i) {
        View view = View.inflate(context, R.layout.item_event_trace, null);
        ViewHolder viewHolder = new ViewHolder(view);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int i) {
        final UserInfoResponse.TracelistBean tracelistBean = traceList.get(i);
        ViewHolder holder = (ViewHolder) viewHolder;
        holder.itemTitle.setText(Common.decodeBase64(tracelistBean.getUser_loc().getAddr()));
        holder.itemLocation.setText(Common.decodeBase64(tracelistBean.getUser_loc().getAddr()));
        holder.itemTime.setText(TimeUtils.getFriendlyTimeSpanByNow(tracelistBean.getReptime(),new SimpleDateFormat("yyyyMMddhhmmss")));
//        Glide.with(context).load(tracelistBean.getDevimg()).into(holder.itemIcon);
        holder.rlItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(onDeviceItemClick!=null){
                    onDeviceItemClick.onItemClick(tracelistBean);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return traceList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.rl_item)
        RelativeLayout rlItem;
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

    public void refreshData(List<UserInfoResponse.TracelistBean> traceList){
        this.traceList.clear();
        this.traceList.addAll(traceList);
        notifyDataSetChanged();
    }
}
