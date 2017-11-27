package com.miittech.you.adapter;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.bumptech.glide.Glide;
import com.miittech.you.R;
import com.miittech.you.activity.device.DeviceDetailActivity;
import com.miittech.you.common.Common;
import com.miittech.you.global.IntentExtras;
import com.miittech.you.global.SPConst;
import com.miittech.you.impl.OnListItemClick;
import com.miittech.you.net.response.DeviceResponse;
import com.ryon.mutils.LogUtils;
import com.ryon.mutils.SPUtils;

import org.w3c.dom.Text;

import java.util.ArrayList;
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
        ViewHolder holder = (ViewHolder) viewHolder;
        holders.put(Common.formatDevId2Mac(devlistBean.getDevidX()),holder);
        holder.itemTitle.setText(Common.decodeBase64(devlistBean.getDevname())+"--->"+devlistBean.getDevidX());
        holder.itemLocation.setText(devlistBean.getLocinfo().getAddr());
        holder.itemLocation.setText(devlistBean.getLasttime());
        Glide.with(activity).load(devlistBean.getDevimg()).into(holder.itemIcon);
        holder.rlItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(onDeviceItemClick!=null){
                    onDeviceItemClick.onItemClick(devlistBean);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    public void updateData(List<DeviceResponse.DevlistBean> devlist) {
        mData.clear();
        mData.addAll(devlist);
        notifyDataSetChanged();
        ArrayList<String> devList = new ArrayList<>();
        devlist.clear();
        for(DeviceResponse.DevlistBean devlistBean : devlist){
            devList.add(Common.formatDevId2Mac(devlistBean.getDevidX()));
        }
        Intent intent= new Intent(IntentExtras.ACTION.ACTION_BLE_COMMAND);
        intent.putExtra("cmd",IntentExtras.CMD.CMD_DEVICE_LIST_ADD);
        intent.putStringArrayListExtra("macList",devList);
        activity.sendBroadcast(intent);
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
        @BindView(R.id.item_battery)
        TextView itemBattery;
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
                switch (ret){
                    case IntentExtras.RET.RET_DEVICE_READ_RSSI:
                        LogUtils.d("RET_DEVICE_READ_RSSI");
                        String mac = intent.getStringExtra("address");
                        int rssi = intent.getIntExtra("rssi",0);
                        updateItemRssi(mac,rssi);
                        break;
                    case IntentExtras.RET.RET_DEVICE_READ_BATTERY:
                        LogUtils.d("RET_DEVICE_READ_BATTERY");
                        String address = intent.getStringExtra("address");
                        String battery = intent.getStringExtra("battery");
                        updateItemBattery(address,battery);
                        break;
                }

            }
        }
    }

    private void updateItemBattery(String address, String battery) {
        ViewHolder viewHolder = holders.get(address);
        if(viewHolder!=null&&viewHolder.itemLocation!=null){
            viewHolder.itemBattery.setText("剩余电量  "+battery+"%");
        }
    }

    private void updateItemRssi(String mac, int rssi) {
        ViewHolder viewHolder = holders.get(mac);
        if(viewHolder!=null&&viewHolder.itemLocation!=null){
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
    }

    public void unregist() {
        activity.unregisterReceiver(cmdResponseReceiver);
    }
}
