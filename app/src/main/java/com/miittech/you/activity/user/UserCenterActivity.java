package com.miittech.you.activity.user;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.gson.Gson;
import com.luck.picture.lib.PictureSelector;
import com.luck.picture.lib.config.PictureConfig;
import com.luck.picture.lib.config.PictureMimeType;
import com.luck.picture.lib.entity.LocalMedia;
import com.miittech.you.R;
import com.miittech.you.activity.BaseActivity;
import com.miittech.you.utils.Common;
import com.miittech.you.dialog.DialogUtils;
import com.miittech.you.dialog.MsgTipDialog;
import com.miittech.you.glide.GlideApp;
import com.miittech.you.global.IntentExtras;
import com.miittech.you.global.SPConst;
import com.miittech.you.impl.OnMsgTipOptions;
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
import com.ryon.mutils.NetworkUtils;
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
import cn.sharesdk.framework.Platform;
import cn.sharesdk.framework.PlatformActionListener;
import cn.sharesdk.framework.ShareSDK;
import cn.sharesdk.tencent.qq.QQ;
import cn.sharesdk.wechat.friends.Wechat;
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
        initMyTitleBar(titlebar, getResources().getString(R.string.text_setting));
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
        UserInfoResponse response = (UserInfoResponse) SPUtils.getInstance().readObject(SPConst.DATA.USERINFO);
        if(response!=null){
            initData(response.getUserinfo());
        }
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
        if(!NetworkUtils.isConnected()){
            ToastUtils.showShort(R.string.msg_net_error);
            return;
        }
        Map userattr = new HashMap();
        userattr.put("headimg", imageUrl);
        Map param = new HashMap();
        param.put("method", "C");
        param.put("userattr", userattr);

        String json = new Gson().toJson(param);
        PubParam pubParam = new PubParam(Common.getUserId());
        String sign_unSha1 = pubParam.toValueString() + json + Common.getTocken();
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
                            getUserInfo();
                            ToastUtils.showShort(R.string.msg_upload_file_success);
                        } else {
                            ToastUtils.showShort(response.getErrmsg());
                        }
                    }
                });
    }

    private void getUserInfo() {
        if(!NetworkUtils.isConnected()){
            ToastUtils.showShort(R.string.msg_net_error);
            return;
        }
        Map param = new HashMap();
        param.put("qrytype", Params.QRY_TYPE.ALL);
        String json = new Gson().toJson(param);
        PubParam pubParam = new PubParam(Common.getUserId());
        String sign_unSha1 = pubParam.toValueString() + json + Common.getTocken();
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
                            SPUtils.getInstance().remove(SPConst.DATA.USERINFO);
                            SPUtils.getInstance().saveObject(SPConst.DATA.USERINFO,response);
                            initData(response.getUserinfo());
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
            userWechat.setText("已绑定");
        }
        if (userinfo.getIsBindQQ() == 0) {
            userQq.setText(getResources().getString(R.string.text_unbind));
        } else {
            userQq.setText("已绑定");
        }
        if(userinfo.getIsShareLocation()==1){
            userLocationToogle.setChecked(true);
        }else{
            userLocationToogle.setChecked(false);
        }
    }

    public void setUserHeadImg(String imgUrl) {
        GlideApp.with(this)
                .load(imgUrl)
                .error(R.drawable.ic_header_img)
                .placeholder(R.drawable.ic_header_img)
                .into(userHearderImage);
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
                if(userinfoBean.getIsBindWx()==0){
                    Platform wechat = ShareSDK.getPlatform(Wechat.NAME);
                    wechat.SSOSetting(false);  //设置false表示使用SSO授权方式
                    wechat.setPlatformActionListener(platformActionListener); // 设置分享事件回调
                    wechat.authorize();//单独授权
                    wechat.showUser(null);//授权并获取用户信息
                }
                break;
            case R.id.btn_user_qq:
                if (userinfoBean.getIsBindQQ() == 0) {
                    Platform qq = ShareSDK.getPlatform(QQ.NAME);
                    qq.SSOSetting(false);  //设置false表示使用SSO授权方式
                    qq.setPlatformActionListener(platformActionListener); // 设置分享事件回调
                    qq.authorize();//单独授权
                    qq.showUser(null);//授权并获取用户信息
                }
                break;
            case R.id.btn_user_location:
                if(userLocationToogle.isChecked()){
                    MsgTipDialog msgTipDialog = DialogUtils.getInstance().createMsgTipDialog(this)
                        .setTitle("操作确认")
                        .setMsg("关闭后，好友将无法查看你的位置，确认关闭吗？")
                        .setOnMsgTipOptions(new OnMsgTipOptions(){
                            @Override
                            public void onSure() {
                                super.onSure();
                                updateIsShareLocation(false);
                            }
                       });
                    msgTipDialog.show();
                }else{
                    updateIsShareLocation(true);
                }
                break;
            case R.id.btn_logout:
                Common.updateIngnoreSettingValid();
                DialogUtils.getInstance().showLogoutDialog(this)
                    .onClickSure(new OnMsgTipOptions() {
                        @Override
                        public void onSure() {
                            super.onSure();
                            doLogout();
                        }
                    });
                break;
        }
    }

    private void updateIsShareLocation(boolean b) {
        if(!NetworkUtils.isConnected()){
            return;
        }
        Map userattr = new HashMap();
        userattr.put("isShareLocation",b?1:0);
        Map param = new HashMap();
        param.put("method", "D");
        param.put("userattr", userattr);

        String json = new Gson().toJson(param);
        PubParam pubParam = new PubParam(Common.getUserId());
        String sign_unSha1 = pubParam.toValueString() + json + Common.getTocken();
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
                        if (!response.isSuccessful()) {
                            ToastUtils.showShort(response.getErrmsg());
                        }else{
                            getUserInfo();
                        }
                    }
                });
    }

    private void doLogout() {
        if(!NetworkUtils.isConnected()){
            return;
        }
        PubParam pubParam = new PubParam(Common.getUserId());
        String sign_unSha1 = pubParam.toValueString() + Common.getTocken();
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
                            Intent cmd= new Intent(IntentExtras.ACTION.ACTION_BLE_COMMAND);
                            cmd.putExtra("cmd",IntentExtras.CMD.CMD_DEVICE_LIST_CLEAR);
                            sendBroadcast(cmd);
                            SPUtils.getInstance(SPConst.USER.SP_NAME).clear();
                            SPUtils.getInstance().clear();
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
    private PlatformActionListener platformActionListener = new PlatformActionListener() {
        @Override
        public void onComplete(Platform platform, int i, HashMap<String, Object> hashMap) {
            doBindSSO(platform);
        }

        @Override
        public void onError(Platform platform, int i, Throwable throwable) {

        }

        @Override
        public void onCancel(Platform platform, int i) {

        }
    };

    private void doBindSSO(Platform platform) {
        if(!NetworkUtils.isConnected()){
            ToastUtils.showShort(R.string.msg_net_error);
            return;
        }
        String access_token = platform.getDb().getToken(); // 获取授权token
        String openid = platform.getDb().getUserId(); // 获取用户在此平台的ID
        long expires_in = platform.getDb().getExpiresIn();
        String unionid = platform.getDb().get("unionid");
        Map param = new HashMap();
        if(Wechat.NAME.equals(platform.getName())){
            param.put("method", Params.METHOD.WECHART);
        }else if(QQ.NAME.equals(platform.getName())){
            param.put("method", Params.METHOD.QQSSO);
        }
        param.put("openid", openid);
        param.put("access_token", access_token);
        param.put("expires_in", expires_in);
        param.put("unionid", unionid);

        String json = new Gson().toJson(param);
        PubParam pubParam = new PubParam(Common.getUserId());
        String sign_unSha1 = pubParam.toValueString() + json + Common.getTocken();
        LogUtils.d("sign_unsha1", sign_unSha1);
        String sign = EncryptUtils.encryptSHA1ToString(sign_unSha1).toLowerCase();
        LogUtils.d("sign_sha1", sign);
        String path = HttpUrl.Api + "userbind/" + pubParam.toUrlParam(sign);
        RequestBody requestBody = RequestBody.create(MediaType.parse(HttpUrl.MediaType_Json), json);
        ApiServiceManager.getInstance().buildApiService(this).postNetRequest(path, requestBody)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<BaseResponse>() {
                    @Override
                    public void accept(BaseResponse response) throws Exception {
                        if (response.isSuccessful()) {
                            getUserInfo();
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
}
