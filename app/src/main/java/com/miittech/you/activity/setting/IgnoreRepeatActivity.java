package com.miittech.you.activity.setting;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;

import com.miittech.you.R;
import com.miittech.you.activity.BaseActivity;
import com.miittech.you.impl.TitleBarOptions;
import com.miittech.you.weight.Titlebar;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by Administrator on 2017/11/23.
 */

public class IgnoreRepeatActivity extends BaseActivity {
    @BindView(R.id.titlebar)
    Titlebar titlebar;
    @BindView(R.id.img_week_1)
    ImageView imgWeek1;
    @BindView(R.id.img_week_2)
    ImageView imgWeek2;
    @BindView(R.id.img_week_3)
    ImageView imgWeek3;
    @BindView(R.id.img_week_4)
    ImageView imgWeek4;
    @BindView(R.id.img_week_5)
    ImageView imgWeek5;
    @BindView(R.id.img_week_6)
    ImageView imgWeek6;
    @BindView(R.id.img_week_7)
    ImageView imgWeek7;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ignore_repeat);
        ButterKnife.bind(this);
        initMyTitleBar(titlebar,"防丢勿扰设置");
        titlebar.showBackOption();
        titlebar.showCompleteOption();
        titlebar.setTitleBarOptions(new TitleBarOptions(){
            @Override
            public void onBack() {
                super.onBack();
                finish();
            }

            @Override
            public void onComplete() {
                super.onComplete();
                StringBuilder key = new StringBuilder();
                StringBuilder value = new StringBuilder();
                if(imgWeek1.getVisibility()==View.VISIBLE){
                    key.append(1);
                    value.append(",星期一");
                }
                if(imgWeek2.getVisibility()==View.VISIBLE){
                    key.append(2);
                    value.append(",星期二");
                }
                if(imgWeek3.getVisibility()==View.VISIBLE){
                    key.append(3);
                    value.append(",星期三");
                }
                if(imgWeek4.getVisibility()==View.VISIBLE){
                    key.append(4);
                    value.append(",星期四");
                }
                if(imgWeek5.getVisibility()==View.VISIBLE){
                    key.append(5);
                    value.append(",星期五");
                }
                if(imgWeek6.getVisibility()==View.VISIBLE){
                    key.append(6);
                    value.append(",星期六");
                }
                if(imgWeek7.getVisibility()==View.VISIBLE){
                    key.append(7);
                    value.append(",星期日");
                }
                Intent data = new Intent();
                data.putExtra("key",key.toString());
                String keyStr = key.toString();
                if("1234567".equals(keyStr)){
                    data.putExtra("value","每天");
                }else if("".equals(keyStr)){
                    data.putExtra("value","");
                }else{
                    data.putExtra("value",value.toString().substring(1));
                }
                setResult(RESULT_OK,data);
                finish();
            }
        });
        initWeek(getIntent().getStringExtra("week"));
    }

    private void initWeek(String week) {
        if(TextUtils.isEmpty(week)){
            return;
        }
        if(week.contains("1")){
            imgWeek1.setVisibility(View.VISIBLE);
        }else{
            imgWeek1.setVisibility(View.GONE);
        }
        if(week.contains("2")){
            imgWeek2.setVisibility(View.VISIBLE);
        }else{
            imgWeek2.setVisibility(View.GONE);
        }
        if(week.contains("3")){
            imgWeek3.setVisibility(View.VISIBLE);
        }else{
            imgWeek3.setVisibility(View.GONE);
        }
        if(week.contains("4")){
            imgWeek4.setVisibility(View.VISIBLE);
        }else{
            imgWeek4.setVisibility(View.GONE);
        }
        if(week.contains("5")){
            imgWeek5.setVisibility(View.VISIBLE);
        }else{
            imgWeek5.setVisibility(View.GONE);
        }
        if(week.contains("6")){
            imgWeek6.setVisibility(View.VISIBLE);
        }else{
            imgWeek6.setVisibility(View.GONE);
        }
        if(week.contains("7")){
            imgWeek7.setVisibility(View.VISIBLE);
        }else{
            imgWeek7.setVisibility(View.GONE);
        }
    }

    @OnClick({R.id.rl_week_1, R.id.rl_week_2, R.id.rl_week_3, R.id.rl_week_4, R.id.rl_week_5, R.id.rl_week_6, R.id.rl_week_7})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.rl_week_1:
                if(imgWeek1.getVisibility()==View.VISIBLE){
                    imgWeek1.setVisibility(View.GONE);
                }else {
                    imgWeek1.setVisibility(View.VISIBLE);
                }
                break;
            case R.id.rl_week_2:
                if(imgWeek2.getVisibility()==View.VISIBLE){
                    imgWeek2.setVisibility(View.GONE);
                }else {
                    imgWeek2.setVisibility(View.VISIBLE);
                }
                break;
            case R.id.rl_week_3:
                if(imgWeek3.getVisibility()==View.VISIBLE){
                    imgWeek3.setVisibility(View.GONE);
                }else {
                    imgWeek3.setVisibility(View.VISIBLE);
                }
                break;
            case R.id.rl_week_4:
                if(imgWeek4.getVisibility()==View.VISIBLE){
                    imgWeek4.setVisibility(View.GONE);
                }else {
                    imgWeek4.setVisibility(View.VISIBLE);
                }
                break;
            case R.id.rl_week_5:
                if(imgWeek5.getVisibility()==View.VISIBLE){
                    imgWeek5.setVisibility(View.GONE);
                }else {
                    imgWeek5.setVisibility(View.VISIBLE);
                }
                break;
            case R.id.rl_week_6:
                if(imgWeek6.getVisibility()==View.VISIBLE){
                    imgWeek6.setVisibility(View.GONE);
                }else {
                    imgWeek6.setVisibility(View.VISIBLE);
                }
                break;
            case R.id.rl_week_7:
                if(imgWeek7.getVisibility()==View.VISIBLE){
                    imgWeek7.setVisibility(View.GONE);
                }else {
                    imgWeek7.setVisibility(View.VISIBLE);
                }
                break;
        }
    }
}
