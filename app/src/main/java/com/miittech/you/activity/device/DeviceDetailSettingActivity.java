package com.miittech.you.activity.device;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.google.gson.Gson;
import com.liulishuo.filedownloader.BaseDownloadTask;
import com.liulishuo.filedownloader.FileDownloadListener;
import com.liulishuo.filedownloader.FileDownloader;
import com.luck.picture.lib.PictureSelector;
import com.luck.picture.lib.config.PictureConfig;
import com.luck.picture.lib.config.PictureMimeType;
import com.luck.picture.lib.entity.LocalMedia;
import com.miittech.you.App;
import com.miittech.you.R;
import com.miittech.you.activity.BaseActivity;
import com.miittech.you.ble.BleClient;
import com.miittech.you.ble.gatt.BleReadCallback;
import com.miittech.you.ble.BleUUIDS;
import com.miittech.you.ble.task.trans.BleTransTaskQueue;
import com.miittech.you.ble.update.IOtaUpdateListener;
import com.miittech.you.ble.update.OtaOptions;
import com.miittech.you.ble.update.UpConst;
import com.miittech.you.dialog.DialogUtils;
import com.miittech.you.dialog.MsgTipDialog;
import com.miittech.you.dialog.ProgressDialog;
import com.miittech.you.dialog.UpdateDialog;
import com.miittech.you.impl.OnMsgTipOptions;
import com.miittech.you.net.response.BleVersionResponse;
import com.miittech.you.utils.Common;
import com.miittech.you.entity.DeviceInfo;
import com.miittech.you.glide.GlideApp;
import com.miittech.you.global.HttpUrl;
import com.miittech.you.global.IntentExtras;
import com.miittech.you.global.PubParam;
import com.miittech.you.impl.TitleBarOptions;
import com.miittech.you.net.ApiServiceManager;
import com.miittech.you.net.response.BaseResponse;
import com.miittech.you.net.response.DeviceListResponse;
import com.miittech.you.weight.CircleImageView;
import com.miittech.you.weight.Titlebar;
import com.ryon.mutils.EncryptUtils;
import com.ryon.mutils.FileUtils;
import com.ryon.mutils.LogUtils;
import com.ryon.mutils.NetworkUtils;
import com.ryon.mutils.SPUtils;
import com.ryon.mutils.TimeUtils;
import com.ryon.mutils.ToastUtils;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

/**
 * Created by Administrator on 2017/12/13.
 */

public class DeviceDetailSettingActivity extends BaseActivity {
    @BindView(R.id.titlebar)
    Titlebar titlebar;
    @BindView(R.id.tv_device_name)
    TextView tvDeviceName;
    @BindView(R.id.img_device_icon)
    CircleImageView imgDeviceIcon;
    @BindView(R.id.tv_device_classify)
    TextView tvDeviceClassify;
    @BindView(R.id.tv_device_time_active)
    TextView tvDeviceTimeActive;
    @BindView(R.id.tv_device_id)
    TextView tvDeviceId;
    @BindView(R.id.tv_device_vertion)
    TextView tvDeviceVertion;

    private static final int REQUEST_DEVICE_NAME=0x01;
    private static final int REQUEST_DEVICE_CLASSFY=0x02;
    DeviceInfo deviceInfo;
    private String firmware;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_detail_setting);
        ButterKnife.bind(this);
        initMyTitleBar(titlebar,"设置");
        titlebar.showBackOption();
        titlebar.setTitleBarOptions(new TitleBarOptions(){
            @Override
            public void onBack() {
                super.onBack();
                finish();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        deviceInfo = (DeviceInfo) getIntent().getSerializableExtra(IntentExtras.DEVICE.DATA);
        DeviceInfo response = (DeviceInfo) SPUtils.getInstance().readObject(Common.formatDevId2Mac(deviceInfo.getDevidX()));
        if(response!=null){
            deviceInfo = response;
        }
        initData(deviceInfo);
    }

    private void initData(DeviceInfo deviceDetailInfo) {
        tvDeviceName.setText(Common.decodeBase64(deviceDetailInfo.getDevname()));
        tvDeviceClassify.setText(Common.decodeBase64(deviceDetailInfo.getGroupname()));
        tvDeviceId.setText(deviceInfo.getDevidX());
        DateFormat dateFormat = new SimpleDateFormat("yyyy年MM月dd日 a HH:mm");
        Date date = TimeUtils.string2Date(deviceDetailInfo.getBindtime(),new SimpleDateFormat("yyyyMMddHHmmss"));
        tvDeviceTimeActive.setText(TimeUtils.date2String(date,dateFormat));
        GlideApp.with(this)
            .load(deviceDetailInfo.getDevimg())
            .error(Common.getDefaultDevImgResouceId(Common.decodeBase64(deviceDetailInfo.getGroupname())))
            .placeholder(Common.getDefaultDevImgResouceId(Common.decodeBase64(deviceDetailInfo.getGroupname())))
            .into(imgDeviceIcon);
        BleClient.getInstance().read(
                Common.formatDevId2Mac(deviceDetailInfo.getDevidX()),
                BleUUIDS.versionServiceUUID,
                BleUUIDS.firmwareVertionCharacteristicUUID,
                new BleReadCallback(){
            @Override
            public synchronized void onReadResponse(BluetoothDevice device, BluetoothGattCharacteristic characteristic, final byte[] data) {
                super.onReadResponse(device,characteristic,data);
                DeviceDetailSettingActivity.this.firmware = new String(data);
                LogUtils.d("value:"+DeviceDetailSettingActivity.this.firmware);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                    tvDeviceVertion.setText(DeviceDetailSettingActivity.this.firmware);
                    }
                });
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case PictureConfig.CHOOSE_REQUEST:
                    // 图片选择结果回调
                    List<LocalMedia> selectList = PictureSelector.obtainMultipleResult(data);
                    LocalMedia localMedia = selectList.remove(0);
                    // 例如 LocalMedia 里面返回三种path
                    // 1.media.getPath(); 为原图path
                    // 2.media.getCutPath();为裁剪后path，需判断media.isCut();是否为true
                    // 3.media.getCompressPath();为压缩后path，需判断media.isCompressed();是否为true
                    // 如果裁剪并压缩了，以取压缩路径为准，因为是先裁剪后压缩的
                    String path = localMedia.getPath();
                    uploadImage(path);
                    break;
                case REQUEST_DEVICE_NAME:
                    tvDeviceName.setText(data.getStringExtra(IntentExtras.DEVICE.NAME));
                    break;
                case REQUEST_DEVICE_CLASSFY:
                    tvDeviceClassify.setText(data.getStringExtra(IntentExtras.DEVICE.CLASSIFY));
                    break;

            }
        }
    }

    @OnClick({R.id.rl_device_name, R.id.rl_device_img, R.id.rl_device_classify, R.id.rl_device_alert, R.id.rl_device_update})
    public void onViewClicked(View view) {
        Intent intent;
        switch (view.getId()) {
            case R.id.rl_device_name:
                intent = new Intent(this,DeviceEditNameActivity.class);
                intent.putExtra(IntentExtras.DEVICE.ID,deviceInfo.getDevidX());
                startActivityForResult(intent,REQUEST_DEVICE_NAME);
                break;
            case R.id.rl_device_img:
                PictureSelector.create(this)
                        .openGallery(PictureMimeType.ofImage())
                        .selectionMode(PictureConfig.SINGLE)
                        .isCamera(true)
                        .forResult(PictureConfig.CHOOSE_REQUEST);
                break;
            case R.id.rl_device_classify:
                intent = new Intent(this,DeviceSetClassifyActivity.class);
                intent.putExtra(IntentExtras.DEVICE.ID,deviceInfo.getDevidX());
                intent.putExtra(IntentExtras.FROM,"DEVICESETTING");
                startActivityForResult(intent,REQUEST_DEVICE_CLASSFY);
                break;
            case R.id.rl_device_alert:
                intent = new Intent(this,DevicePhoneAlertSettingActivity.class);
                intent.putExtra(IntentExtras.DEVICE.DATA,deviceInfo);
                startActivity(intent);
                break;
            case R.id.rl_device_update:
                if(!TextUtils.isEmpty(firmware)){
                    checkBleVersion(deviceInfo.getDevidX(),firmware);
                }
                break;
        }
    }
    private void uploadImage(String path) {
        if(!NetworkUtils.isConnected()){
            ToastUtils.showShort(R.string.msg_net_error);
            return;
        }
        File file = new File(path);
        Map param = new HashMap();
        String fileName = file.getName();
        long size = FileUtils.getFileLength(file);
        param.put("path", file.getName());
        param.put("size", size);
        String sha = "";
        try {
            sha = FileUtils.getSha1(file).toLowerCase();
            param.put("sha", sha);
        } catch (Exception e) {
            e.printStackTrace();
        }
        String json = new Gson().toJson(param);
        LogUtils.d("imgupload", json);
        PubParam pubParam = new PubParam(Common.getUserId());
        String sign = EncryptUtils.encryptSHA1ToString(pubParam.toValueString() + fileName + size + sha + Common.getTocken()).toLowerCase();
        LogUtils.d("sign", sign);
        String urlPath = HttpUrl.Api + "imgupload/" + pubParam.toUrlParam(sign) + "&path=" + fileName + "&size=" + size + "&sha=" + sha;

        RequestBody requestFile = RequestBody.create(MediaType.parse("multipart/form-data"), file);
        MultipartBody.Part body = MultipartBody.Part.createFormData("image", file.getName(), requestFile);

        ApiServiceManager.getInstance().buildApiService(this).uploadImage(urlPath, body)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<BaseResponse>() {
                    @Override
                    public void accept(BaseResponse response) throws Exception {
                        if (response.isSuccessful()) {
                            doDeviceIconEditAttr(response.getUrl());
                        } else {
                            response.onError(DeviceDetailSettingActivity.this);
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        throwable.printStackTrace();
                    }
                });
    }
    private void doDeviceIconEditAttr(final String iconUrl) {
        if(!NetworkUtils.isConnected()){
            ToastUtils.showShort(R.string.msg_net_error);
            return;
        }
        final Map devattrMap = new HashMap();
        devattrMap.put("devimg",iconUrl);
        Map param = new HashMap();
        param.put("devid", deviceInfo.getDevidX());
        param.put("method", "C");
        param.put("devattr", devattrMap);
        String json = new Gson().toJson(param);
        PubParam pubParam = new PubParam(Common.getUserId());
        String sign_unSha1 = pubParam.toValueString() + json + Common.getTocken();
        LogUtils.d("sign_unsha1", sign_unSha1);
        String sign = EncryptUtils.encryptSHA1ToString(sign_unSha1).toLowerCase();
        LogUtils.d("sign_sha1", sign);
        String path = HttpUrl.Api + "deviceattr/" + pubParam.toUrlParam(sign);
        final RequestBody requestBody = RequestBody.create(MediaType.parse(HttpUrl.MediaType_Json), json);

        ApiServiceManager.getInstance().buildApiService(this).postDeviceOption(path, requestBody)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<DeviceListResponse>() {
                    @Override
                    public void accept(DeviceListResponse response) throws Exception {
                        if(response.isSuccessful()){
                            GlideApp.with(DeviceDetailSettingActivity.this)
                                    .load(iconUrl)
                                    .error(Common.getDefaultDevImgResouceId(Common.decodeBase64(deviceInfo.getGroupname())))
                                    .placeholder(Common.getDefaultDevImgResouceId(Common.decodeBase64(deviceInfo.getGroupname())))
                                    .into(imgDeviceIcon);
                            Common.getDeviceDetailInfo(DeviceDetailSettingActivity.this,deviceInfo.getDevidX(),null);
                            Common.initDeviceList(DeviceDetailSettingActivity.this,null);
                        }else{
                            response.onError(DeviceDetailSettingActivity.this);
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        throwable.printStackTrace();
                    }
                });

    }
    public synchronized void checkBleVersion(final String devidX, final String firmwareVertion){
        if(TextUtils.isEmpty(devidX)){
            return;
        }
        if(!NetworkUtils.isConnected()){
            ToastUtils.showShort("网络链接断开，请检查网络");
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
                            if(firmwareBean!=null&&(firmwareVertion.compareTo(firmwareBean.getFirmware())<0)){
                                final UpdateDialog updateDialog = DialogUtils.getInstance().showUpdateDialog(DeviceDetailSettingActivity.this,true);
                                updateDialog.setTitle("固件更新");
                                updateDialog.setMsg("检查到新的固件 "+firmwareBean.getFirmware()+",请及时更新");
                                updateDialog.setLeftBtnText("取消");
                                updateDialog.setRightBtnText("更新");
                                updateDialog.setOnMsgTipOptions(new OnMsgTipOptions(){
                                    @Override
                                    public void onSure() {
                                        super.onSure();
                                        if(updateDialog!=null&&updateDialog.isShowing()){
                                            updateDialog.dismiss();
                                        }
                                        startDownloadFirmware(devidX,firmwareBean.getDl_url(),firmwareBean.getFirmware());
                                    }

                                    @Override
                                    public void onCancel() {
                                        super.onCancel();
                                        if(updateDialog!=null&&updateDialog.isShowing()){
                                            updateDialog.dismiss();
                                        }
                                    }
                                });
                                updateDialog.show();
                                return;
                            }else{
                                ToastUtils.showShort("当前已是最新版本");
                            }
                        } else {
                            response.onError(DeviceDetailSettingActivity.this);
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        throwable.printStackTrace();
                    }
                });
    }

    private void startDownloadFirmware(final String devidX, String downloadUrl,String firmware) {
        final ProgressDialog progressDialog = DialogUtils.getInstance().showProgressDialog(this);
        progressDialog.setTitle("正在准备更新...");
        final ProgressBar progressBar = progressDialog.getProgressBar();
        FileDownloader.setup(this);
        FileDownloader.getImpl().create(downloadUrl)
                .setPath(UpConst.file_blefirmware_download_path+File.separator+"firmware"+firmware+".img")
                .setListener(new FileDownloadListener() {
                    @Override
                    protected void pending(BaseDownloadTask task, int soFarBytes, int totalBytes) {
                    }

                    @Override
                    protected void connected(BaseDownloadTask task, String etag, boolean isContinue, int soFarBytes, int totalBytes) {
                        progressBar.setMax(totalBytes);
                    }

                    @Override
                    protected void progress(BaseDownloadTask task, int soFarBytes, int totalBytes) {
                        progressBar.setProgress(soFarBytes);
                    }

                    @Override
                    protected void blockComplete(BaseDownloadTask task) {
                    }

                    @Override
                    protected void retry(final BaseDownloadTask task, final Throwable ex, final int retryingTimes, final int soFarBytes) {
                    }

                    @Override
                    protected void completed(BaseDownloadTask task) {
                        updateDevice(Common.formatDevId2Mac(devidX),progressDialog,task);
                    }

                    @Override
                    protected void paused(BaseDownloadTask task, int soFarBytes, int totalBytes) {
                    }

                    @Override
                    protected void error(BaseDownloadTask task, Throwable e) {
                        MsgTipDialog msgTipDialog = DialogUtils.getInstance().createMsgTipDialog(DeviceDetailSettingActivity.this);
                        msgTipDialog.setTitle("提示");
                        msgTipDialog.setMsg("下载出错了，请重试！");                        msgTipDialog.setRightBtnText("关闭");
                        msgTipDialog.hideLeftBtn();
                        msgTipDialog.setOnMsgTipOptions(new OnMsgTipOptions(){
                            @Override
                            public void onSure() {
                                super.onSure();
                                if(progressDialog!=null&&progressDialog.isShowing()){
                                    progressDialog.dismiss();
                                }
                            }
                        });
                        msgTipDialog.show();
                    }

                    @Override
                    protected void warn(BaseDownloadTask task) {
                    }
                }).start();
        progressDialog.show();
    }
    private void updateDevice(String mac, final ProgressDialog progressDialog, final BaseDownloadTask task) {
        if(progressDialog==null){
            return;
        }
        final ProgressBar progressBar = progressDialog.getProgressBar();
        String filePath = UpConst.file_blefirmware_download_path+ File.separator+task.getFilename();
        final OtaOptions otaOptions = new OtaOptions(this);
        try {
            otaOptions.init(filePath,mac);
            otaOptions.startUpdate(new IOtaUpdateListener() {
                @Override
                public void updateTitle(final String title) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressDialog.setTitle(title);
                        }
                    });
                }

                @Override
                public void onProgress(final int progress) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if(progressBar!=null){
                                progressBar.setProgress(progress);
                            }
                        }
                    });
                }

                @Override
                public void onError(OtaOptions options,final String msg) {
                    LogUtils.e("有物更新失败："+msg);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ToastUtils.showShort("有物更新失败："+msg);
                            if(progressDialog!=null&&progressDialog.isShowing()){
                                progressDialog.dismiss();
                            }
                        }
                    });
                    options.distroy();

                }

                @Override
                public void onUpdateComplete() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if(progressDialog!=null&&progressDialog.isShowing()){
                                progressDialog.dismiss();
                            }
                            try {
                                FileUtils.deleteFile(task.getPath()+File.separator+task.getFilename());
                            }catch (Exception e){
                                e.printStackTrace();
                            }
                            DialogUtils.getInstance().createMsgTipDialog(DeviceDetailSettingActivity.this)
                                    .setTitle("更新")
                                    .setMsg("已完成固件更新，是否重启蓝牙设备？")
                                    .setLeftBtnText("取消")
                                    .setRightBtnText("确定")
                                    .setOnMsgTipOptions(new OnMsgTipOptions(){
                                        @Override
                                        public void onSure() {
                                            super.onSure();
                                            otaOptions.sendRebootSignal();
                                            new Handler().postDelayed(new Runnable() {
                                                @Override
                                                public void run() {
                                                    BleClient.getInstance().read(
                                                            Common.formatDevId2Mac(DeviceDetailSettingActivity.this.deviceInfo.getDevidX()),
                                                            BleUUIDS.versionServiceUUID,
                                                            BleUUIDS.firmwareVertionCharacteristicUUID,
                                                            true,
                                                            new BleReadCallback(){
                                                                @Override
                                                                public synchronized void onReadResponse(BluetoothDevice device, BluetoothGattCharacteristic characteristic,final byte[] data) {
                                                                    super.onReadResponse(device,characteristic,data);
                                                                    runOnUiThread(new Runnable() {
                                                                        @Override
                                                                        public void run() {
                                                                            DeviceDetailSettingActivity.this.firmware = new String(data);
                                                                            LogUtils.d("value:"+DeviceDetailSettingActivity.this.firmware);
                                                                            runOnUiThread(new Runnable() {
                                                                                @Override
                                                                                public void run() {
                                                                                    tvDeviceVertion.setText(DeviceDetailSettingActivity.this.firmware);
                                                                                }
                                                                            });
                                                                        }
                                                                    });
                                                                }
                                                            });
                                                }
                                            },2000);

                                        }

                                        @Override
                                        public void onCancel() {
                                            super.onCancel();
                                        }
                                    }).show();
                        }
                    });
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
