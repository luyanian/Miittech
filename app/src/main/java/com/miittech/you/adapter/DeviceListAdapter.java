package com.miittech.you.adapter;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.AnimationDrawable;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.bumptech.glide.Glide;
import com.clj.fastble.BleManager;
import com.miittech.you.R;
import com.miittech.you.common.Common;
import com.miittech.you.glide.GlideApp;
import com.miittech.you.global.IntentExtras;
import com.miittech.you.impl.OnListItemClick;
import com.miittech.you.net.response.DeviceResponse;
import com.ryon.mutils.LogUtils;
import com.ryon.mutils.TimeUtils;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by Administrator on 2017/10/19.
 */

public class DeviceListAdapter extends RecyclerView.Adapter {

    private List<DeviceResponse.DevlistBean> mData = new ArrayList<>();
    private Activity activity;
    private OnListItemClick onDeviceItemClick;
    private CmdResponseReceiver cmdResponseReceiver = new CmdResponseReceiver();
    private Map<String,ViewHolder> holders = new HashMap<>();

    public DeviceListAdapter(Activity activity, OnListItemClick onDeviceItemClick) {
        this.activity = activity;
        this.onDeviceItemClick = onDeviceItemClick;

        IntentFilter filter=new IntentFilter();
        filter.addAction(IntentExtras.ACTION.ACTION_CMD_RESPONSE);
        activity.registerReceiver(cmdResponseReceiver,filter);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, final int i) {
        View view = View.inflate(activity, R.layout.item_device_list, null);
        ViewHolder viewHolder = new ViewHolder(view);
        return viewHolder;
    }



    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, final int i) {
        final DeviceResponse.DevlistBean devlistBean = mData.get(i);
        final ViewHolder holder = (ViewHolder) viewHolder;
        holders.put(Common.formatDevId2Mac(devlistBean.getDevidX()),holder);
        holder.itemTitle.setText(Common.decodeBase64(devlistBean.getDevname()));
        holder.tvIsShared.setVisibility(View.GONE);
        if(TextUtils.isEmpty(devlistBean.getFriendname())){
            holder.itemShared.setVisibility(View.GONE);
        }else{
            if(i==0){
                holder.tvIsShared.setVisibility(View.VISIBLE);
            }else{
                DeviceResponse.DevlistBean lastItem = mData.get(i-1);
                if(TextUtils.isEmpty(lastItem.getFriendname())){
                    holder.tvIsShared.setVisibility(View.VISIBLE);
                }
            }
            holder.itemShared.setVisibility(View.VISIBLE);
            holder.itemShared.setText("分享自"+Common.decodeBase64(devlistBean.getFriendname()));
        }
        if(!BleManager.getInstance().isConnected(Common.formatDevId2Mac(devlistBean.getDevidX()))){
            holder.itemLocation.setText(Common.decodeBase64(devlistBean.getLocinfo().getAddr()));
            setTimeText(holder.itemTime,devlistBean.getLasttime());
        }
        setConnectStatusStyle(Common.formatDevId2Mac(devlistBean.getDevidX()));
        GlideApp.with(activity)
                .load(devlistBean.getDevimg())
                .error(Common.getDefaultDevImgResouceId(Common.decodeBase64(devlistBean.getGroupname())))
                .placeholder(Common.getDefaultDevImgResouceId(Common.decodeBase64(devlistBean.getGroupname())))
                .into(holder.itemIcon);
        holder.rlItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(onDeviceItemClick!=null){
                    ViewHolder viewHolder = holders.get(Common.formatDevId2Mac(devlistBean.getDevidX()));
                    onDeviceItemClick.onItemClick(devlistBean,viewHolder.itemLocation.getText().toString());
                }
            }
        });
    }

    private void setConnectStatusStyle(String mac) {
        ViewHolder holder = holders.get(mac);
        if(holder==null){
            return;
        }
        if(BleManager.getInstance().isConnected(mac)){
            holder.itemIconStatus.setVisibility(View.VISIBLE);
            holder.itemIconStatus.setBackgroundResource(R.drawable.anim_connect_status);
            AnimationDrawable animationDrawable = (AnimationDrawable) holder.itemIconStatus.getBackground();
            if(animationDrawable!=null&&!animationDrawable.isRunning()){
                animationDrawable.start();
            }
        }else{
            holder.itemIconStatus.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    public void updateData(List<DeviceResponse.DevlistBean> devlist) {
        mData.clear();
        ArrayList<String> tempList = new ArrayList<>();
        tempList.clear();
        if(devlist!=null){
            Collections.sort(devlist, new Comparator<DeviceResponse.DevlistBean>() {
                @Override
                public int compare(DeviceResponse.DevlistBean o1, DeviceResponse.DevlistBean o2) {
                    if(TextUtils.isEmpty(o1.getFriendname())){
                        return -1;
                    }
                    return 1;
                }
            });
            mData.addAll(devlist);
            notifyDataSetChanged();
            for(DeviceResponse.DevlistBean devlistBean : devlist){
                if(TextUtils.isEmpty(devlistBean.getFriendname())) {
                    tempList.add(Common.formatDevId2Mac(devlistBean.getDevidX()));
                }
            }
        }
        Intent intent= new Intent(IntentExtras.ACTION.ACTION_BLE_COMMAND);
        intent.putExtra("cmd",IntentExtras.CMD.CMD_DEVICE_LIST_ADD);
        intent.putStringArrayListExtra("macList",tempList);
        activity.sendBroadcast(intent);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.rl_item)
        RelativeLayout rlItem;
        @BindView(R.id.tv_is_shared)
        TextView tvIsShared;
        @BindView(R.id.item_icon_status)
        ImageView itemIconStatus;
        @BindView(R.id.item_icon)
        ImageView itemIcon;
        @BindView(R.id.item_title)
        TextView itemTitle;
        @BindView(R.id.item_location)
        TextView itemLocation;
        @BindView(R.id.item_battery)
        TextView itemBattery;
        @BindView(R.id.item_shared)
        TextView itemShared;
        @BindView(R.id.item_time)
        TextView itemTime;

        ViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }
    }
    private class CmdResponseReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(IntentExtras.ACTION.ACTION_CMD_RESPONSE)){
                int ret = intent.getIntExtra("ret", -1);//获取Extra信息
                String address = intent.getStringExtra("address");
                switch (ret){
                    case IntentExtras.RET.RET_BLE_MODE_WORK_SUCCESS:
                        setConnectStatusStyle(address);
                        break;
                    case IntentExtras.RET.RET_BLE_READ_RSSI:
                        LogUtils.d("RET_DEVICE_READ_RSSI");
                        int rssi = intent.getIntExtra("rssi",0);
                        updateItemRssi(address,rssi);
                        break;
                    case IntentExtras.RET.RET_BLE_READ_BATTERY:
                        LogUtils.d("RET_DEVICE_READ_BATTERY");
                        String battery = intent.getStringExtra("battery");
                        updateItemBattery(address,battery);
                        break;
                    case IntentExtras.RET.RET_BLE_DISCONNECT:
                        setConnectStatusStyle(address);
                        updateItemData(address);
                        break;
                }

            }
        }
    }

    private void updateItemData(String address) {
        ViewHolder viewHolder = holders.get(address);
        if(viewHolder!=null&&viewHolder.itemLocation!=null){
            for(DeviceResponse.DevlistBean devlistBean : mData){
                if(address.equals(Common.formatDevId2Mac(devlistBean.getDevidX()))){
                    viewHolder.itemLocation.setText(Common.decodeBase64(devlistBean.getLocinfo().getAddr()));
                    setTimeText(viewHolder.itemTime,devlistBean.getLasttime());
                }
            }

        }
    }

    private void setTimeText(TextView itemTime, String lasttime) {
        if(itemTime==null){
            return;
        }
        SimpleDateFormat sdf=new SimpleDateFormat("yyyyMMddhhmmss");
        String timeSpan = TimeUtils.getFriendlyTimeSpanByNow(lasttime,sdf);
        itemTime.setText(timeSpan);
    }

    private void updateItemBattery(String address, String battery) {
        ViewHolder viewHolder = holders.get(address);
        if(viewHolder!=null&&!TextUtils.isEmpty(battery)){
            if(viewHolder.itemBattery!=null&&Integer.valueOf(battery)<20) {
                viewHolder.itemBattery.setText("剩余电量  " + battery + "%");
            }
            if(viewHolder.itemTime!=null) {
                viewHolder.itemTime.setText("现在");
            }
        }
    }

    private void updateItemRssi(String mac, int rssi) {
        ViewHolder viewHolder = holders.get(mac);
        if(viewHolder!=null){
            if(viewHolder.itemLocation!=null) {
                if (rssi < -85) {
                    viewHolder.itemLocation.setText("远离");
                }
                if (rssi > -85 && rssi < -70) {
                    viewHolder.itemLocation.setText("较远");
                }
                if (rssi > -70) {
                    viewHolder.itemLocation.setText("很近");
                }
            }
            if(viewHolder.itemTime!=null) {
                viewHolder.itemTime.setText("现在");
            }
        }
    }

    public void unregist() {
        activity.unregisterReceiver(cmdResponseReceiver);
    }
}
