package com.miittech.you.activity.setting;

import android.os.Bundle;
import android.webkit.WebView;

import com.miittech.you.R;
import com.miittech.you.activity.BaseActivity;
import com.miittech.you.impl.TitleBarOptions;
import com.miittech.you.weight.Titlebar;

import butterknife.BindView;
import butterknife.ButterKnife;

public class WebViewActivity extends BaseActivity {
    @BindView(R.id.titlebar)
    Titlebar titlebar;
    @BindView(R.id.webview)
    WebView webview;

    public static final String authority = "http://www.yoowoo.cn/helper/helper_setting.html";
    public static final String help = "http://www.yoowoo.cn/helper/helper_intro.html";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webview);
        ButterKnife.bind(this);
        initMyTitleBar(titlebar,"使用说明");
        titlebar.showBackOption();
        titlebar.setTitleBarOptions(new TitleBarOptions(){
            @Override
            public void onBack() {
                super.onBack();
                finish();
            }
        });
        String url = getIntent().getStringExtra("url");
        webview.loadUrl("url");
    }
}
