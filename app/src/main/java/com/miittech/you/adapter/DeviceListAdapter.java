package com.miittech.you.adapter;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.gson.Gson;
import com.liulishuo.filedownloader.BaseDownloadTask;
import com.liulishuo.filedownloader.FileDownloadListener;
import com.liulishuo.filedownloader.FileDownloader;
import com.miittech.you.App;
import com.miittech.you.R;
import com.miittech.you.activity.device.DeviceDetailSettingActivity;
import com.miittech.you.ble.BleClient;
import com.miittech.you.ble.BleUUIDS;
import com.miittech.you.ble.gatt.BleReadCallback;
import com.miittech.you.ble.update.IOtaUpdateListener;
import com.miittech.you.ble.update.OtaOptions;
import com.miittech.you.ble.update.UpConst;
import com.miittech.you.dialog.DialogUtils;
import com.miittech.you.dialog.MsgTipDialog;
import com.miittech.you.dialog.ProgressDialog;
import com.miittech.you.dialog.UpdateDialog;
import com.miittech.you.entity.Locinfo;
import com.miittech.you.global.HttpUrl;
import com.miittech.you.global.PubParam;
import com.miittech.you.global.SPConst;
import com.miittech.you.impl.OnMsgTipOptions;
import com.miittech.you.net.ApiServiceManager;
import com.miittech.you.net.response.BleVersionResponse;
import com.miittech.you.utils.Common;
import com.miittech.you.entity.DeviceInfo;
import com.miittech.you.glide.GlideApp;
import com.miittech.you.global.IntentExtras;
import com.miittech.you.impl.OnListItemClick;
import com.miittech.you.weight.CircleImageView;
import com.ryon.mutils.EncryptUtils;
import com.ryon.mutils.FileUtils;
import com.ryon.mutils.LogUtils;
import com.ryon.mutils.NetworkUtils;
import com.ryon.mutils.SPUtils;
import com.ryon.mutils.TimeUtils;
import com.ryon.mutils.ToastUtils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import okhttp3.MediaType;
import okhttp3.RequestBody;

/**
 * Created by Administrator on 2017/10/19.
 */

public class DeviceListAdapter extends RecyclerView.Adapter {
    private List<DeviceInfo> mData = new ArrayList<>();
    private Activity activity;
    private OnListItemClick onDeviceItemClick;
    private CmdResponseReceiver cmdResponseReceiver = new CmdResponseReceiver();
    private Map<String,ViewHolder> holders = new HashMap<>();
    private LinkedBlockingQueue<String> devicesCanUpdate = new LinkedBlockingQueue<>();
    private boolean isUpdating = false;
    ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    public DeviceListAdapter(Activity activity, OnListItemClick onDeviceItemClick) {
        this.activity = activity;
        this.onDeviceItemClick = onDeviceItemClick;
        IntentFilter filter=new IntentFilter();
        filter.addAction(IntentExtras.ACTION.ACTION_CMD_RESPONSE);
        App.getInstance().getLocalBroadCastManager().registerReceiver(cmdResponseReceiver,filter);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, final int i) {
        View view = View.inflate(activity, R.layout.item_device_list, null);
        ViewHolder viewHolder = new ViewHolder(view);
        return viewHolder;
    }



    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, final int i) {
        final DeviceInfo deviceInfo = mData.get(i);
        final ViewHolder holder = (ViewHolder) viewHolder;
        holders.put(Common.formatDevId2Mac(deviceInfo.getDevidX()),holder);
        holder.itemTitle.setText(Common.decodeBase64(deviceInfo.getDevname()));
        holder.tvIsShared.setVisibility(View.GONE);
        if(TextUtils.isEmpty(deviceInfo.getFriendname())){
            holder.itemShared.setVisibility(View.GONE);
        }else{
            if(i==0){
                holder.tvIsShared.setVisibility(View.VISIBLE);
            }else{
                DeviceInfo lastItem = mData.get(i-1);
                if(TextUtils.isEmpty(lastItem.getFriendname())){
                    holder.tvIsShared.setVisibility(View.VISIBLE);
                }else{
                    holder.tvIsShared.setVisibility(View.GONE);
                }
            }
            holder.itemShared.setVisibility(View.VISIBLE);
            holder.itemShared.setText("分享自"+Common.decodeBase64(deviceInfo.getFriendname()));
        }
        if(!BleClient.getInstance().isConnected(Common.formatDevId2Mac(deviceInfo.getDevidX()))){
            holder.itemLocation.setText(Common.decodeBase64(deviceInfo.getLocinfo().getAddr()));
            setTimeText(holder.itemTime,deviceInfo.getLasttime());
        }else{
            holder.itemTime.setText("现在");
        }
        setConnectStatusStyle(Common.formatDevId2Mac(deviceInfo.getDevidX()));
        GlideApp.with(activity)
                .load(deviceInfo.getDevimg())
                .error(Common.getDefaultDevImgResouceId(Common.decodeBase64(deviceInfo.getGroupname())))
                .placeholder(Common.getDefaultDevImgResouceId(Common.decodeBase64(deviceInfo.getGroupname())))
                .into(holder.itemIcon);
        holder.rlItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(onDeviceItemClick!=null){
                    ViewHolder viewHolder = holders.get(Common.formatDevId2Mac(deviceInfo.getDevidX()));
                    onDeviceItemClick.onDeviceItemClick(deviceInfo,viewHolder.itemLocation.getText().toString(),viewHolder.itemTime.getText().toString());
                }
            }
        });
        BleClient.getInstance().readRemoteRssi(Common.formatDevId2Mac(deviceInfo.getDevidX()));
    }

    private void setConnectStatusStyle(String mac){
        setConnectStatusStyle(mac,-50);
    }

    private void setConnectStatusStyle(String mac,int rssi) {
        ViewHolder holder = holders.get(mac);
        if(holder==null){
            return;
        }
        if(BleClient.getInstance().isConnected(mac)){
            if(rssi>-50) {
                holder.itemIcon.setBorderColor(activity.getResources().getColor(R.color.ic_connect1));
            }else if(rssi<=-50&&rssi>-65){
                holder.itemIcon.setBorderColor(activity.getResources().getColor(R.color.ic_connect2));
            }else if(rssi<=-65&&rssi>-85){
                holder.itemIcon.setBorderColor(activity.getResources().getColor(R.color.ic_connect3));
            }else if(rssi<=-85&&rssi>-100){
                holder.itemIcon.setBorderColor(activity.getResources().getColor(R.color.ic_connect4));
            }else if(rssi<=-100){
                holder.itemIcon.setBorderColor(activity.getResources().getColor(R.color.ic_connect5));
            }
            updateItemRssi(mac,rssi);
        }else{
            holder.itemIcon.setBorderColor(activity.getResources().getColor(R.color.windowBg));
        }
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    public void updateData(List<DeviceInfo> devlist) {
        mData.clear();
        ArrayList<String> tempList = new ArrayList<>();
        tempList.clear();
        if(devlist!=null){
            Collections.sort(devlist, new Comparator<DeviceInfo>() {
                @Override
                public int compare(DeviceInfo o1, DeviceInfo o2) {
                    if(TextUtils.isEmpty(o1.getFriendname())){
                        return -1;
                    }
                    return 1;
                }
            });
            mData.addAll(devlist);
            notifyDataSetChanged();
            for(DeviceInfo devlistBean : devlist){
                if(TextUtils.isEmpty(devlistBean.getFriendname())) {
                    tempList.add(Common.formatDevId2Mac(devlistBean.getDevidX()));
                    SPUtils.getInstance().remove(Common.formatDevId2Mac(devlistBean.getDevidX()));
                    SPUtils.getInstance().saveObject(Common.formatDevId2Mac(devlistBean.getDevidX()),devlistBean);
                }
            }
        }
        Intent intent= new Intent(IntentExtras.ACTION.ACTION_BLE_COMMAND);
        intent.putExtra("cmd",IntentExtras.CMD.CMD_DEVICE_LIST_ADD);
        intent.putStringArrayListExtra("macList",tempList);
        App.getInstance().getLocalBroadCastManager().sendBroadcast(intent);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.rl_item)
        RelativeLayout rlItem;
        @BindView(R.id.tv_is_shared)
        TextView tvIsShared;
        @BindView(R.id.item_icon)
        CircleImageView itemIcon;
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
                final String address = intent.getStringExtra("address");
                switch (ret){
                    case IntentExtras.RET.RET_BLE_MODE_WORK_SUCCESS:
                        setConnectStatusStyle(address);
//                        devicesCanUpdate.add(address);
//                        recycleUpdate();
                        break;
                    case IntentExtras.RET.RET_BLE_READ_RSSI:
                        LogUtils.d("RET_DEVICE_READ_RSSI");
                        int rssi = intent.getIntExtra("rssi",0);
                        setConnectStatusStyle(address,rssi);
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
                    case IntentExtras.RET.RET_BLE_STATE_OFF:
                        notifyDataSetChanged();
                        break;
                }

            }
        }
    }

    private void updateItemData(String address) {
        ViewHolder viewHolder = holders.get(address);
        if(viewHolder!=null&&viewHolder.itemLocation!=null){
            Locinfo locinfo = (Locinfo) SPUtils.getInstance().readObject(SPConst.LOC_INFO);
            if(locinfo!=null){
                viewHolder.itemLocation.setText(locinfo.getAddr());
            }else{
                for(DeviceInfo devlistBean : mData){
                    if(address.equals(Common.formatDevId2Mac(devlistBean.getDevidX()))){
                        viewHolder.itemLocation.setText(Common.decodeBase64(devlistBean.getLocinfo().getAddr()));
                    }
                }
            }
            SimpleDateFormat sdf=new SimpleDateFormat("yyyyMMddHHmmss");
            setTimeText(viewHolder.itemTime,TimeUtils.getNowString(sdf));
        }
    }

    private void setTimeText(TextView itemTime, String lasttime) {
        if(itemTime==null){
            return;
        }
        SimpleDateFormat sdf=new SimpleDateFormat("yyyyMMddHHmmss");
        String timeSpan = TimeUtils.getFriendlyTimeSpanByNow(lasttime,sdf);
        itemTime.setText(timeSpan);
    }

    private void updateItemBattery(String address, String battery) {
        ViewHolder viewHolder = holders.get(address);
        if(viewHolder!=null&&!TextUtils.isEmpty(battery)){
//            if(viewHolder.itemBattery!=null&&Integer.valueOf(battery)<20) {
//                viewHolder.itemBattery.setText("剩余电量  " + battery + "%");
//            }
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
        App.getInstance().getLocalBroadCastManager().unregisterReceiver(cmdResponseReceiver);
    }

    private void recycleUpdate() {
        if(isUpdating){
            return;
        }
        if(devicesCanUpdate.size()>0) {
            isUpdating = true;
            executorService.schedule(new Runnable() {
                @Override
                public void run() {
                    String address = devicesCanUpdate.remove();
                    if (BleClient.getInstance().isConnected(address)) {
                        getBleVersion(address);
                    } else {
                        isUpdating = false;
                    }
                }
            }, 2, TimeUnit.SECONDS);
        }
    }

    public synchronized void getBleVersion(final String mac){
        BleClient.getInstance().read(
                mac,
                BleUUIDS.versionServiceUUID,
                BleUUIDS.firmwareVertionCharacteristicUUID,
                new BleReadCallback(){
                    @Override
                    public synchronized void onReadResponse(BluetoothDevice device, BluetoothGattCharacteristic characteristic, final byte[] data) {
                        super.onReadResponse(device,characteristic,data);
                        String firmware = new String(data);
                        LogUtils.d("bleservice_update","("+mac+") current device firmware version is:"+firmware);
                        getNetBleVersion(mac,firmware);
                    }
                });
    }
    private void getNetBleVersion(final String mac, final String firmware){
        if(!NetworkUtils.isConnected()){
            ToastUtils.showShort("网络链接断开，请检查网络");
            isUpdating = false;
            return;
        }
        Map param = new HashMap();
        param.put("devtype", "1");
        param.put("debug", "1");
        String json = new Gson().toJson(param);
        PubParam pubParam = new PubParam(Common.getUserId());
        String sign_unSha1 = pubParam.toValueString() + json + Common.getTocken();
        LogUtils.d("sign_unsha1", sign_unSha1);
        String sign = EncryptUtils.encryptSHA1ToString(sign_unSha1).toLowerCase();
        LogUtils.d("sign_sha1", sign);
        String path = HttpUrl.Api + "devicefirmware/" + pubParam.toUrlParam(sign);
        final RequestBody requestBody = RequestBody.create(MediaType.parse(HttpUrl.MediaType_Json), json);

        ApiServiceManager.getInstance().buildApiService(App.getInstance()).postGetBleVersion(path, requestBody)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<BleVersionResponse>() {
                    @Override
                    public void accept(final BleVersionResponse response) throws Exception {
                        if (response.isSuccessful()) {
                            final BleVersionResponse.FirmwareBean firmwareBean = response.getFirmware();
                            if(firmwareBean!=null&&(firmware.compareTo(firmwareBean.getFirmware())<0)){
                                LogUtils.d("bleservice_update","("+mac+") find new firmware version:"+firmwareBean.getFirmware());
                                startDownloadFirmware(mac,firmwareBean.getDl_url());
                                return;
                            }else{
                                isUpdating = false;
                            }
                        } else {
                            response.onError(activity);
                            isUpdating = false;
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        throwable.printStackTrace();
                        isUpdating = false;
                    }
                });
    }
    private void startDownloadFirmware(final String mac, String downloadUrl) {
        FileDownloader.setup(activity);
        LogUtils.d("bleservice_update","("+mac+") start download the last firmware");
        FileDownloader.getImpl().create(downloadUrl)
                .setPath(UpConst.file_blefirmware_download_path+ File.separator+"firmware.img")
                .setListener(new FileDownloadListener() {
                    @Override
                    protected void pending(BaseDownloadTask task, int soFarBytes, int totalBytes) {
                    }

                    @Override
                    protected void connected(BaseDownloadTask task, String etag, boolean isContinue, int soFarBytes, int totalBytes) {

                    }

                    @Override
                    protected void progress(BaseDownloadTask task, int soFarBytes, int totalBytes) {
                        LogUtils.d("bleservice_update","("+mac+") download "+soFarBytes+"/"+totalBytes);
                    }

                    @Override
                    protected void blockComplete(BaseDownloadTask task) {
                    }

                    @Override
                    protected void retry(final BaseDownloadTask task, final Throwable ex, final int retryingTimes, final int soFarBytes) {
                    }

                    @Override
                    protected void completed(BaseDownloadTask task) {
                        LogUtils.d("bleservice_update","("+mac+") download completed ,and start update");
                        updateDevice(mac,task);
                    }

                    @Override
                    protected void paused(BaseDownloadTask task, int soFarBytes, int totalBytes) {
                    }

                    @Override
                    protected void error(BaseDownloadTask task, Throwable e) {
                        LogUtils.d("bleservice_update","("+mac+") download error");
                        isUpdating = false;
                    }

                    @Override
                    protected void warn(BaseDownloadTask task) {
                    }
                }).start();
    }
    private void updateDevice(final String mac, final BaseDownloadTask task) {
        String filePath = UpConst.file_blefirmware_download_path+ File.separator+task.getFilename();
        final OtaOptions otaOptions = new OtaOptions(activity);
        try {
            otaOptions.init(filePath,mac);
            otaOptions.startUpdate(new IOtaUpdateListener() {
                @Override
                public void updateTitle(final String title) {
                }

                @Override
                public void onProgress(final int progress) {
                    LogUtils.d("bleservice_update","("+mac+") update "+progress);
                }

                @Override
                public void onError(OtaOptions options,String msg) {

                }

                @Override
                public void onUpdateComplete() {
                    try {
                        FileUtils.deleteFile(task.getPath() + File.separator + task.getFilename());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    LogUtils.d("bleservice_update","("+mac+") update complete and sendRebootSignal");
                    isUpdating = false;
                    otaOptions.sendRebootSignal();
                    recycleUpdate();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            LogUtils.d("bleservice_update","("+mac+") exception "+e.getMessage());
        }
    }
}
