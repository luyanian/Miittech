package com.miittech.you.adapter;

import android.app.Activity;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.clj.fastble.BleManager;
import com.clj.fastble.conn.BleCharacterCallback;
import com.clj.fastble.conn.BleGattCallback;
import com.clj.fastble.exception.BleException;
import com.miittech.you.App;
import com.miittech.you.R;
import com.miittech.you.activity.BaseActivity;
import com.miittech.you.common.BleCommon;
import com.miittech.you.common.Common;
import com.miittech.you.global.Params;
import com.miittech.you.impl.OnDeviceItemClick;
import com.miittech.you.net.response.DeviceResponse;
import com.ryon.mutils.LogUtils;
import com.ryon.mutils.ToastUtils;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by Administrator on 2017/10/19.
 */

public class DeviceListAdapter extends RecyclerView.Adapter {

    private List<DeviceResponse.DevlistBean> mData;
    private Activity activity;
    private OnDeviceItemClick onDeviceItemClick;
    private BleManager bleManager;

    public DeviceListAdapter(Activity activity, List<DeviceResponse.DevlistBean> mData, OnDeviceItemClick onDeviceItemClick) {
        this.activity = activity;
        this.mData = mData;
        this.onDeviceItemClick = onDeviceItemClick;
        bleManager = new BleManager(activity);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, final int i) {
        View view = View.inflate(activity, R.layout.item_device_list, null);
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
        Glide.with(activity).load(devlistBean.getDevimg()).into(holder.itemIcon);
        autoConnect(devlistBean);
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
    private void autoConnect(DeviceResponse.DevlistBean devlistBean) {
        bleManager.scanMacAndConnect(
                Common.formatDevId2Mac(devlistBean.getDevidX()),
                3000,
                true,
                new BleGattCallback() {
                    @Override
                    public void onConnectError(BleException exception) {

                    }

                    @Override
                    public void onConnectSuccess(BluetoothGatt gatt, int status) {
                        settingWorkMode();
                    }

                    @Override
                    public void onDisConnected(BluetoothGatt gatt, int status, BleException exception) {

                    }
                }
        );
    }

    private void settingWorkMode() {
        byte[] data = Common.formatBleMsg(Params.BLEMODE.MODE_WORK, App.getUserId());
        bleManager.writeDevice(
                BleCommon.serviceUUID,
                BleCommon.characteristicUUID,
                data,
                new BleCharacterCallback() {
                    @Override
                    public void onSuccess(BluetoothGattCharacteristic characteristic) {
                        LogUtils.d(characteristic.toString());
                    }

                    @Override
                    public void onFailure(BleException exception) {
                        LogUtils.e(exception);
                    }

                    @Override
                    public void onInitiatedResult(boolean result) {
                        LogUtils.d(result);
                    }
                });
    }
}
