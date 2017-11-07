package com.miittech.you.activity.user;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.gson.Gson;
import com.luck.picture.lib.PictureSelector;
import com.luck.picture.lib.config.PictureConfig;
import com.luck.picture.lib.config.PictureMimeType;
import com.luck.picture.lib.entity.LocalMedia;
import com.miittech.you.App;
import com.miittech.you.R;
import com.miittech.you.activity.BaseActivity;
import com.miittech.you.common.Common;
import com.miittech.you.dialog.DialogUtils;
import com.miittech.you.global.SPConst;
import com.miittech.you.net.ApiServiceManager;
import com.miittech.you.global.HttpUrl;
import com.miittech.you.global.Params;
import com.miittech.you.global.PubParam;
import com.miittech.you.net.response.BaseResponse;
import com.miittech.you.net.response.UserInfoResponse;
import com.miittech.you.weight.Titlebar;
import com.miittech.you.impl.TitleBarOptions;
import com.ryon.mutils.ActivityPools;
import com.ryon.mutils.EncryptUtils;
import com.ryon.mutils.FileUtils;
import com.ryon.mutils.LogUtils;
import com.ryon.mutils.RegexUtils;
import com.ryon.mutils.SPUtils;
import com.ryon.mutils.ToastUtils;

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
 * Created by Administrator on 2017/9/15.
 */

public class UserCenterActivity extends BaseActivity {
    @BindView(R.id.titlebar)
    Titlebar titlebar;
    @BindView(R.id.user_hearder_image)
    ImageView userHearderImage;
    @BindView(R.id.user_name)
    TextView userName;
    @BindView(R.id.user_phone)
    TextView userPhone;
    @BindView(R.id.user_email)
    TextView userEmail;
    @BindView(R.id.user_wechat)
    TextView userWechat;
    @BindView(R.id.user_qq)
    TextView userQq;
    @BindView(R.id.user_location_toogle)
    CheckBox userLocationToogle;

    private UserInfoResponse.UserinfoBean userinfoBean;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_center);
        ButterKnife.bind(this);
        initTitleBar(titlebar, getResources().getString(R.string.text_setting));
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
        getUserInfo();
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
        PubParam pubParam = new PubParam(App.getInstance().getUserId());
        String sign = EncryptUtils.encryptSHA1ToString(pubParam.toValueString() + fileName + size + sha + App.getInstance().getTocken()).toLowerCase();
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
                            aysnUserInfo(response.getUrl());
                            setUserHeadImg(response.getUrl());
                        } else {
                            response.onError(UserCenterActivity.this);
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        throwable.printStackTrace();
                    }
                });
    }

    public void aysnUserInfo(String imageUrl) {

        Map userattr = new HashMap();
        userattr.put("headimg", imageUrl);
        Map param = new HashMap();
        param.put("method", "C");
        param.put("userattr", userattr);

        String json = new Gson().toJson(param);
        PubParam pubParam = new PubParam(App.getInstance().getUserId());
        String sign_unSha1 = pubParam.toValueString() + json + App.getInstance().getTocken();
        LogUtils.d("sign_unsha1", sign_unSha1);
        String sign = EncryptUtils.encryptSHA1ToString(sign_unSha1).toLowerCase();
        LogUtils.d("sign_sha1", sign);
        String path = HttpUrl.Api + "userattr/" + pubParam.toUrlParam(sign);
        RequestBody requestBody = RequestBody.create(MediaType.parse(HttpUrl.MediaType_Json), json);

        ApiServiceManager.getInstance().buildApiService(this).postNetRequest(path, requestBody)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<BaseResponse>() {
                    @Override
                    public void accept(BaseResponse response) throws Exception {
                        if (response.isSuccessful()) {
                            ToastUtils.showShort(R.string.msg_upload_file_success);
                        } else {
                            ToastUtils.showShort(response.getErrmsg());
                        }
                    }
                });
    }

    private void getUserInfo() {
        Map param = new HashMap();
        param.put("qrytype", Params.QRY_TYPE.BASE);
//        param.put("sdate", "20170101");
//        param.put("edate", "20170920");
        String json = new Gson().toJson(param);
        PubParam pubParam = new PubParam(App.getInstance().getUserId());
        String sign_unSha1 = pubParam.toValueString() + json + App.getInstance().getTocken();
        LogUtils.d("sign_unsha1", sign_unSha1);
        String sign = EncryptUtils.encryptSHA1ToString(sign_unSha1).toLowerCase();
        LogUtils.d("sign_sha1", sign);
        String path = HttpUrl.Api + "userinfo/" + pubParam.toUrlParam(sign);
        RequestBody requestBody = RequestBody.create(MediaType.parse(HttpUrl.MediaType_Json), json);
        ApiServiceManager.getInstance().buildApiService(this).postToGetUserInfo(path, requestBody)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<UserInfoResponse>() {
                    @Override
                    public void accept(UserInfoResponse response) throws Exception {
                        if (response.isSuccessful()) {
                            initData(response.getUserinfo());
                        } else {
                            response.getUserinfo();
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        throwable.printStackTrace();
                    }
                });
    }

    private void initData(UserInfoResponse.UserinfoBean userinfo) {
        this.userinfoBean = userinfo;
        if (!TextUtils.isEmpty(userinfo.getHeadimg())) {
            setUserHeadImg(userinfo.getHeadimg());
        }

        if (!TextUtils.isEmpty(userinfo.getNickname())) {
            userName.setText(Common.decodeBase64(userinfo.getNickname()));
        }
        if (!TextUtils.isEmpty(userinfo.getPhone())) {
            userPhone.setText(RegexUtils.getSecurityPhoneNum(userinfo.getPhone()));
        }
        if (!TextUtils.isEmpty(userinfo.getEmail())) {
            userEmail.setText(userinfo.getEmail());
        }
        if (userinfo.getIsBindWx() == 0) {
            userWechat.setText(getResources().getString(R.string.text_unbind));
        } else {
            userWechat.setText(userinfo.getIsBindWx());
        }
        if (userinfo.getIsBindQQ() == 0) {
            userQq.setText(getResources().getString(R.string.text_unbind));
        } else {
            userQq.setText(userinfo.getIsBindQQ());
        }
    }

    public void setUserHeadImg(String imgUrl) {
        Glide.with(this).load(imgUrl).into(userHearderImage);
    }

    @OnClick({R.id.user_hearder_image, R.id.btn_user_nike, R.id.btn_user_password, R.id.btn_user_phone,R.id.btn_user_email, R.id.btn_user_wechat, R.id.btn_user_qq, R.id.btn_user_location,R.id.btn_logout})
    public void onViewClicked(View view) {
        Intent intent;
        switch (view.getId()) {
            case R.id.user_hearder_image:
                PictureSelector.create(this)
                        .openGallery(PictureMimeType.ofImage())
                        .selectionMode(PictureConfig.SINGLE)
                        .isCamera(true)
                        .forResult(PictureConfig.CHOOSE_REQUEST);
                break;
            case R.id.btn_user_nike:
                intent = new Intent(UserCenterActivity.this, EditNikeOrPWDActivity.class);
                intent.putExtra("nike", this.userinfoBean.getNickname());
                intent.putExtra("edit", "nike");
                startActivity(intent);
                break;
            case R.id.btn_user_password:
                intent = new Intent(UserCenterActivity.this, EditNikeOrPWDActivity.class);
                intent.putExtra("edit", "password");
                startActivity(intent);
                break;
            case R.id.btn_user_phone:
                intent = new Intent(UserCenterActivity.this,BindPhoneActivity.class);
                intent.putExtra("phone",this.userinfoBean.getPhone());
                startActivity(intent);
                break;
            case R.id.btn_user_email:
                intent = new Intent(UserCenterActivity.this,BindEmailActivity.class);
                intent.putExtra("email",this.userinfoBean.getEmail());
                intent.putExtra("isBindEmail",this.userinfoBean.getIsBindEmail());
                startActivity(intent);
                break;
            case R.id.btn_user_wechat:
                break;
            case R.id.btn_user_qq:
                break;
            case R.id.btn_user_location:
                break;
            case R.id.btn_logout:
                DialogUtils.getInstance().showLogoutDialog(this).onClickSure(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        doLogout();
                    }
                });
                break;
        }
    }

    private void doLogout() {

        PubParam pubParam = new PubParam(App.getInstance().getUserId());
        String sign_unSha1 = pubParam.toValueString() + App.getInstance().getTocken();
        LogUtils.d("sign_unsha1", sign_unSha1);
        String sign = EncryptUtils.encryptSHA1ToString(sign_unSha1).toLowerCase();
        LogUtils.d("sign_sha1", sign);
        String path = HttpUrl.Api + "logout/" + pubParam.toUrlParam(sign);
        RequestBody requestBody = RequestBody.create(MediaType.parse(HttpUrl.MediaType_Json),"");
        ApiServiceManager.getInstance().buildApiService(this).postToGetUserInfo(path, requestBody)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<UserInfoResponse>() {
                    @Override
                    public void accept(UserInfoResponse response) throws Exception {
                        if (!response.isSuccessful()) {
                            ToastUtils.showShort(response.getErrmsg());
                        }else{
                            SPUtils.getInstance(SPConst.USER.SP_NAME).clear();
                            Intent intent = new Intent(UserCenterActivity.this,LoginRegisteActivity.class);
                            startActivity(intent);
                            ActivityPools.finishAllExcept(LoginRegisteActivity.class);
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
