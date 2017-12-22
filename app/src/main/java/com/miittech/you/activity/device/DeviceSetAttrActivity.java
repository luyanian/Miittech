package com.miittech.you.activity.device;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import com.google.gson.Gson;
import com.luck.picture.lib.PictureSelector;
import com.luck.picture.lib.config.PictureConfig;
import com.luck.picture.lib.config.PictureMimeType;
import com.luck.picture.lib.entity.LocalMedia;
import com.miittech.you.R;
import com.miittech.you.activity.BaseActivity;
import com.miittech.you.common.Common;
import com.miittech.you.glide.GlideApp;
import com.miittech.you.global.IntentExtras;
import com.miittech.you.impl.TitleBarOptions;
import com.miittech.you.net.ApiServiceManager;
import com.miittech.you.global.HttpUrl;
import com.miittech.you.global.PubParam;
import com.miittech.you.net.response.BaseResponse;
import com.miittech.you.net.response.DeviceListResponse;
import com.miittech.you.weight.CircleImageView;
import com.miittech.you.weight.Titlebar;
import com.ryon.mutils.EncryptUtils;
import com.ryon.mutils.FileUtils;
import com.ryon.mutils.LogUtils;

import java.io.File;
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
 * Created by Administrator on 2017/10/20.
 */

public class DeviceSetAttrActivity extends BaseActivity {
    @BindView(R.id.titlebar)
    Titlebar titlebar;
    @BindView(R.id.img_device_icon)
    CircleImageView imgDeviceIcon;
    @BindView(R.id.tv_device_name)
    TextView tvDeviceName;
    @BindView(R.id.tv_device_classify)
    TextView tvDeviceClassify;

    private String classify="";
    private String address="";
    private String iconUrl;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_set_attr);
        ButterKnife.bind(this);
        initMyTitleBar(titlebar,"设置分类");
        titlebar.showBackOption();
        titlebar.showCompleteOption("下一步");
        titlebar.setTitleBarOptions(new TitleBarOptions(){
            @Override
            public void onBack() {
                super.onBack();
                finish();
            }

            @Override
            public void onComplete() {
                super.onComplete();
                Intent intent = new Intent(DeviceSetAttrActivity.this,DeviceSelectRingActivity.class);
                intent.putExtra(IntentExtras.DEVICE.ID,address);
                intent.putExtra(IntentExtras.DEVICE.NAME,tvDeviceName.getText().toString());
                intent.putExtra(IntentExtras.DEVICE.CLASSIFY,tvDeviceClassify.getText().toString());
                intent.putExtra(IntentExtras.DEVICE.IMAGE,iconUrl);
                startActivity(intent);
            }
        });
        Intent data = getIntent();
        address = data.getStringExtra(IntentExtras.DEVICE.ID);
        classify = data.getStringExtra(IntentExtras.DEVICE.CLASSIFY);
        if(data.hasExtra(IntentExtras.DEVICE.IMAGE)){
            iconUrl = data.getStringExtra(IntentExtras.DEVICE.IMAGE);
        }
        tvDeviceClassify.setText(classify);
        tvDeviceName.setText(classify);
        GlideApp.with(this)
                    .load(iconUrl)
                    .error(Common.getDefaultDevImgResouceId(classify))
                    .placeholder(Common.getDefaultDevImgResouceId(classify))
                    .into(imgDeviceIcon);


    }

    @OnClick(R.id.rl_device_img)
    public void onRlDeviceImgClicked() {
        PictureSelector.create(this)
            .openGallery(PictureMimeType.ofImage())
            .selectionMode(PictureConfig.SINGLE)
            .isCamera(true)
            .forResult(PictureConfig.CHOOSE_REQUEST);
    }

    @OnClick(R.id.rl_device_name)
    public void onRlDeviceNameClicked() {
        Intent intent = new Intent(this,DeviceEditNameActivity.class);
        intent.putExtras(getIntent());
        startActivityForResult(intent,0);
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
                case 0:
                    tvDeviceName.setText(data.getStringExtra(IntentExtras.DEVICE.NAME));
                    break;
            }
        }
    }

    private void uploadImage(String path) {
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
                            response.onError(DeviceSetAttrActivity.this);
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
        this.iconUrl = iconUrl;
        Map devattrMap = new HashMap();
        devattrMap.put("devimg",iconUrl);
        Map param = new HashMap();
        param.put("devid", address);
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
                            GlideApp.with(DeviceSetAttrActivity.this)
                                    .load(iconUrl)
                                    .error(Common.getDefaultDevImgResouceId(classify))
                                    .placeholder(Common.getDefaultDevImgResouceId(classify))
                                    .into(imgDeviceIcon);
                        }else{
                            response.onError(DeviceSetAttrActivity.this);
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
