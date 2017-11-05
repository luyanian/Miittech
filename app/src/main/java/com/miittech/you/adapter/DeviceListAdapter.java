package com.miittech.you.adapter;

import android.app.Activity;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.bumptech.glide.Glide;
import com.miittech.you.App;
import com.miittech.you.R;
import com.miittech.you.common.Common;
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
    private Activity activity;
    private OnDeviceItemClick onDeviceItemClick;

    public DeviceListAdapter(Activity activity, List<DeviceResponse.DevlistBean> mData, OnDeviceItemClick onDeviceItemClick) {
        this.activity = activity;
        this.mData = mData;
        this.onDeviceItemClick = onDeviceItemClick;
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
        String macAddress = Common.formatDevId2Mac(devlistBean.getDevidX());
        App.getInstance().addMac(macAddress);
//        BleConnectOptions options = new BleConnectOptions.Builder()
//                .setConnectRetry(3)   // 连接如果失败重试3次
//                .setConnectTimeout(30000)   // 连接超时30s
//                .setServiceDiscoverRetry(3)  // 发现服务如果失败重试3次
//                .setServiceDiscoverTimeout(20000)  // 发现服务超时20s
//                .build();
//        byte[] dataWork = Common.formatBleMsg(Params.BLEMODE.MODE_WORK, App.getUserId());
//        ClientManager.getClient().write(macAddress, UUID.fromString(userServiceUUID), UUID.fromString(userCharacteristicLogUUID), dataWork, new BleWriteResponse() {
//            @Override
//            public void onResponse(int code) {
//                if (code == REQUEST_SUCCESS) {
//
//                }
//            }
//        });

//        ClientManager.getClient().connect(macAddress, options, new BleConnectResponse() {
//            @Override
//            public void onResponse(int code, BleGattProfile data) {
//                if(code==Constants.CODE_CONNECT){
//                    byte[] dataWork = Common.formatBleMsg(Params.BLEMODE.MODE_WORK, App.getUserId());
//                    ClientManager.getClient().write(macAddress, UUID.fromString(userServiceUUID), UUID.fromString(userCharacteristicLogUUID), dataWork, new BleWriteResponse() {
//                        @Override
//                        public void onResponse(int code) {
//                            if (code == REQUEST_SUCCESS) {
//
//                            }
//                        }
//                    });
//                }
//            }
//        });
//
//        ClientManager.getClient().registerConnectStatusListener(macAddress, new BleConnectStatusListener() {
//            @Override
//            public void onConnectStatusChanged(String mac, int status) {
//                if (status == STATUS_CONNECTED) {
//
//                } else if (status == STATUS_DISCONNECTED) {
//
//                }
//            }
//        });


//        if(ViseBle.getInstance().getConnectState(macAddress).getCode()!= ConnectState.CONNECT_DISCONNECT.getCode()){
//           return;
//        }
//
//        ViseBle.getInstance().connectByMac(macAddress, new IConnectCallback() {
//            @Override
//            public void onConnectSuccess(DeviceMirror deviceMirror) {
//                settingWorkMode(deviceMirror);
//            }
//
//            @Override
//            public void onConnectFailure(BleException exception) {
//                LogUtils.e(exception.getDescription());
//            }
//
//            @Override
//            public void onDisconnect(boolean isActive) {
//                LogUtils.d(isActive);
//            }
//        });
//    }
//
//    private void settingWorkMode(DeviceMirror deviceMirror) {
//        byte[] data = Common.formatBleMsg(Params.BLEMODE.MODE_WORK, App.getUserId());
//
//        BluetoothGattChannel bluetoothGattChannel = new BluetoothGattChannel.Builder()
//                .setBluetoothGatt(deviceMirror.getBluetoothGatt())
//                .setPropertyType(PropertyType.PROPERTY_WRITE)
//                .setServiceUUID(UUID.fromString(BleCommon.userServiceUUID))
//                .setCharacteristicUUID(UUID.fromString(BleCommon.userCharacteristicLogUUID))
//                .builder();
//        deviceMirror.bindChannel(new IBleCallback() {
//            @Override
//            public void onSuccess(byte[] data, BluetoothGattChannel bluetoothGattChannel, BluetoothLeDevice bluetoothLeDevice) {
//                LogUtils.d(data);
//            }
//
//            @Override
//            public void onFailure(com.vise.baseble.exception.BleException exception) {
//                LogUtils.e(exception);
//            }
//        }, bluetoothGattChannel);
//        byte[] dataWork = Common.formatBleMsg(Params.BLEMODE.MODE_WORK,App.getUserId());
//        deviceMirror.writeData(dataWork);
    }
}
