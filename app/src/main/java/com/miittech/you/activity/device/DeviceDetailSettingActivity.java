package com.miittech.you.activity.device;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.google.gson.Gson;
import com.luck.picture.lib.PictureSelector;
import com.luck.picture.lib.config.PictureConfig;
import com.luck.picture.lib.config.PictureMimeType;
import com.luck.picture.lib.entity.LocalMedia;
import com.miittech.you.R;
import com.miittech.you.activity.BaseActivity;
import com.miittech.you.common.Common;
import com.miittech.you.entity.DeviceInfo;
import com.miittech.you.glide.GlideApp;
import com.miittech.you.global.HttpUrl;
import com.miittech.you.global.IntentExtras;
import com.miittech.you.global.Params;
import com.miittech.you.global.PubParam;
import com.miittech.you.impl.TitleBarOptions;
import com.miittech.you.net.ApiServiceManager;
import com.miittech.you.net.response.BaseResponse;
import com.miittech.you.net.response.DeviceDetailResponse;
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
        DeviceDetailResponse response = (DeviceDetailResponse) SPUtils.getInstance().readObject(Common.formatDevId2Mac(deviceInfo.getDevidX()));
        if(response!=null&&response.getUserinfo()!=null&&response.getUserinfo().getDevinfo()!=null){
            deviceInfo = response.getUserinfo().getDevinfo();
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
        tvDeviceVertion.setText("v1.0");
        GlideApp.with(this)
            .load(deviceDetailInfo.getDevimg())
            .error(Common.getDefaultDevImgResouceId(Common.decodeBase64(deviceDetailInfo.getGroupname())))
            .placeholder(Common.getDefaultDevImgResouceId(Common.decodeBase64(deviceDetailInfo.getGroupname())))
            .into(imgDeviceIcon);
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

}
