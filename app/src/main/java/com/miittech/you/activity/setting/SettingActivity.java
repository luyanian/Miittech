package com.miittech.you.activity.setting;

import android.content.Intent;
import android.content.pm.ShortcutManager;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.Base64;
import android.view.View;
import android.widget.TextView;

import com.miittech.you.App;
import com.miittech.you.R;
import com.miittech.you.activity.BaseActivity;
import com.miittech.you.activity.user.MyFriendsActivity;
import com.miittech.you.activity.user.UserCenterActivity;
import com.miittech.you.dialog.DialogUtils;
import com.miittech.you.dialog.MsgTipDialog;
import com.miittech.you.glide.GlideApp;
import com.miittech.you.global.SPConst;
import com.miittech.you.impl.OnMsgTipOptions;
import com.miittech.you.impl.TitleBarOptions;
import com.miittech.you.net.response.UserInfoResponse;
import com.miittech.you.utils.Common;
import com.miittech.you.weight.CircleImageView;
import com.miittech.you.weight.Titlebar;
import com.ryon.mutils.AppUtils;
import com.ryon.mutils.ImageUtils;
import com.ryon.mutils.LogUtils;
import com.ryon.mutils.SPUtils;

import java.util.HashMap;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cn.sharesdk.framework.Platform;
import cn.sharesdk.framework.PlatformActionListener;
import cn.sharesdk.framework.PlatformDb;
import cn.sharesdk.framework.ShareSDK;
import cn.sharesdk.onekeyshare.OnekeyShare;
import cn.sharesdk.onekeyshare.ShareContentCustomizeCallback;
import cn.sharesdk.system.email.Email;
import cn.sharesdk.system.text.ShortMessage;
import cn.sharesdk.tencent.qq.QQ;
import cn.sharesdk.wechat.friends.Wechat;
import cn.sharesdk.wechat.moments.WechatMoments;

/**
 * Created by Administrator on 2017/9/14.
 */

public class SettingActivity extends BaseActivity {
    @BindView(R.id.titlebar)
    Titlebar titlebar;
    @BindView(R.id.user_header_image)
    CircleImageView userHeaderImage;
    @BindView(R.id.user_name)
    TextView userName;
    @BindView(R.id.tv_cur_version)
    TextView tvCurVersion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        ButterKnife.bind(this);
        initMyTitleBar(titlebar, getResources().getString(R.string.text_setting));
        titlebar.showBackOption();
        titlebar.setTitleBarOptions(new TitleBarOptions() {
            @Override
            public void onBack() {
                super.onBack();
                finish();
            }
        });
        tvCurVersion.setText("当前版本v"+AppUtils.getAppVersionName());
    }

    @Override
    protected void onResume() {
        super.onResume();
        UserInfoResponse response = (UserInfoResponse) SPUtils.getInstance().readObject(SPConst.DATA.USERINFO);
        if (response != null) {
            initData(response.getUserinfo());
        }
    }

    @OnClick({R.id.user_header_image, R.id.user_name, R.id.btn_setting_myfriends, R.id.btn_setting_ignore, R.id.btn_setting_service, R.id.btn_setting_star, R.id.btn_setting_shop, R.id.btn_setting_readme, R.id.btn_setting_feedbak, R.id.btn_setting_share, R.id.btn_setting_about_me,R.id.btn_setting_check_version})
    public void onViewClicked(View view) {
        Intent intent;
        switch (view.getId()) {
            case R.id.user_header_image:
            case R.id.user_name:
                intent = new Intent(SettingActivity.this, UserCenterActivity.class);
                startActivity(intent);
                break;
            case R.id.btn_setting_myfriends:
                intent = new Intent(SettingActivity.this, MyFriendsActivity.class);
                startActivity(intent);
                break;
            case R.id.btn_setting_ignore:
                intent = new Intent(SettingActivity.this, IgnoreSettingActivity.class);
                startActivity(intent);
                break;
            case R.id.btn_setting_service:
                intent = new Intent(SettingActivity.this, ServiceSettingActivity.class);
                startActivity(intent);
                break;
            case R.id.btn_setting_star:
                break;
            case R.id.btn_setting_shop:
                MsgTipDialog msgTipDialog = DialogUtils.getInstance().createMsgTipDialog(this);
                msgTipDialog.setTitle("提示");
                msgTipDialog.setMsg("商城即将开通，敬请期待");
                msgTipDialog.hideLeftBtn();
                msgTipDialog.setRightBtnText("知道了");
                msgTipDialog.setOnMsgTipOptions(new OnMsgTipOptions() {
                    @Override
                    public void onSure() {
                        super.onSure();
                    }
                });
                msgTipDialog.show();
                break;
            case R.id.btn_setting_readme:
                Intent urlIntent = new Intent(this,WebViewActivity.class);
                urlIntent.putExtra("url",WebViewActivity.help);
                urlIntent.putExtra("title","使用帮助");
                startActivity(urlIntent);
                break;
            case R.id.btn_setting_feedbak:
                intent = new Intent(SettingActivity.this, FeedBackActivity.class);
                startActivity(intent);
                break;
            case R.id.btn_setting_share:
                OnekeyShare oks = new OnekeyShare();
                oks.setShareContentCustomizeCallback(new ShareContentCustomizeCallback() {
                    @Override
                    public void onShare(Platform platform, Platform.ShareParams paramsToShare) {
                        String url = "https://www.yoowoo.cn/apps/yoowoo/download.html";
                        String nikeName = Common.decodeBase64(Common.getNikeName());
                        String title = "有物分享";
                        String text = nikeName+"邀请你一起使用有物，有物是一款功能强大的智能防丢防忘应用。点击下载 "+url;
                        if (Wechat.NAME.equals(platform.getName())|| WechatMoments.NAME.equals(platform.getName())) {
                            paramsToShare.setShareType(Platform.SHARE_TEXT);
                            paramsToShare.setTitle(title);
                            paramsToShare.setText(text);
                            platform.share(paramsToShare);
                        }else if(QQ.NAME.equals(platform.getName())){
                            paramsToShare.setTitle(title);
                            paramsToShare.setText(text);
                            paramsToShare.setTitleUrl(null);
                            paramsToShare.setImageData(null);
                            platform.share(paramsToShare);
                        }else if(Email.NAME.equals(platform.getName())|| ShortMessage.NAME.equals(platform.getName())){
                            paramsToShare.setTitle(title);
                            paramsToShare.setText(text);
                            platform.share(paramsToShare);
                        }
                    }
                });
                oks.show(this);
                break;
            case R.id.btn_setting_about_me:
                intent = new Intent(SettingActivity.this, AboutMeActivity.class);
                startActivity(intent);
                break;
            case R.id.btn_setting_check_version:
                Common.getAppVersion(this,true);
                break;
        }
    }

    private void initData(UserInfoResponse.UserinfoBean userinfo) {
        if (!TextUtils.isEmpty(userinfo.getHeadimg())) {
            GlideApp.with(this)
                    .load(userinfo.getHeadimg())
                    .error(R.drawable.ic_header_img)
                    .placeholder(R.drawable.ic_header_img)
                    .into(userHeaderImage);
        }

        if (!TextUtils.isEmpty(userinfo.getNickname())) {
            userName.setText(new String(Base64.decode(userinfo.getNickname(), Base64.DEFAULT)));
        }
    }
}
