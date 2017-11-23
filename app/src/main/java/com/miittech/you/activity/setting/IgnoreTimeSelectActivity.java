package com.miittech.you.activity.setting;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import com.bigkoo.pickerview.TimePickerView;
import com.miittech.you.R;
import com.miittech.you.activity.BaseActivity;
import com.miittech.you.impl.TitleBarOptions;
import com.miittech.you.weight.Titlebar;
import com.ryon.mutils.TimeUtils;
import com.ryon.mutils.ToastUtils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by Administrator on 2017/11/21.
 */

public class IgnoreTimeSelectActivity extends BaseActivity {
    @BindView(R.id.titlebar)
    Titlebar titlebar;
    @BindView(R.id.tv_start_time)
    TextView tvStartTime;
    @BindView(R.id.tv_end_time)
    TextView tvEndTime;

    private String startTime = "";
    private String endTime = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ignore_time_select);
        ButterKnife.bind(this);
        initMyTitleBar(titlebar,"时间设置");
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
                doComplete();
            }
        });
    }

    private void doComplete() {
        if(TextUtils.isEmpty(startTime)){
            ToastUtils.showShort("请设置开始时间！");
            return;
        }
        if(TextUtils.isEmpty(endTime)){
            ToastUtils.showShort("请设置结束时间！");
            return;
        }
//        if(TimeUtils.string2Millis(endTime)<=TimeUtils.string2Millis(startTime)){
//            ToastUtils.showShort("结束时间不能小于开始时间！");
//            return;
//        }
        Intent data = new Intent();
        data.putExtra("startTime",startTime);
        data.putExtra("endTime",endTime);
        setResult(RESULT_OK,data);
        finish();
    }

    @OnClick({R.id.rl_start_time, R.id.rl_end_time})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.rl_start_time:
                //时间选择器
                TimePickerView pvStartTime = new TimePickerView.Builder(this, new TimePickerView.OnTimeSelectListener() {
                    @Override
                    public void onTimeSelect(Date date, View v) {//选中事件回调
                        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("hh:mm:ss");
                        startTime = TimeUtils.date2String(date,simpleDateFormat);
                        tvStartTime.setText(startTime);
                    }
                }).setType(new boolean[]{false, false, false, true, true, true})// 默认全部显示
                        .setCancelText("取消")//取消按钮文字
                        .setSubmitText("确定")//确认按钮文字
                        .setContentSize(15)//滚轮文字大小
                        .setTitleSize(16)//标题文字大小
                        .setTitleText("时间")//标题文字
                        .setOutSideCancelable(false)//点击屏幕，点在控件外部范围时，是否取消显示
                        .isCyclic(true)//是否循环滚动
                        .setLabel("年","月","日","时","分","秒")//默认设置为年月日时分秒
                        .isCenterLabel(false) //是否只显示中间选中项的label文字，false则每项item全部都带有label。
                        .isDialog(false)//是否显示为对话框样式
                        .build();
                pvStartTime.setDate(Calendar.getInstance());//注：根据需求来决定是否使用该方法（一般是精确到秒的情况），此项可以在弹出选择器的时候重新设置当前时间，避免在初始化之后由于时间已经设定，导致选中时间与当前时间不匹配的问题。
                pvStartTime.show();
                break;
            case R.id.rl_end_time:
                TimePickerView pvEndTime = new TimePickerView.Builder(this, new TimePickerView.OnTimeSelectListener() {
                    @Override
                    public void onTimeSelect(Date date, View v) {//选中事件回调
                        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("hh:mm:ss");
                        endTime = TimeUtils.date2String(date,simpleDateFormat);
                        tvEndTime.setText(endTime);
                    }
                }).setType(new boolean[]{false, false, false, true, true, true})// 默认全部显示
                .setCancelText("取消")//取消按钮文字
                .setSubmitText("确定")//确认按钮文字
                .setContentSize(15)//滚轮文字大小
                .setTitleSize(16)//标题文字大小
                .setTitleText("时间")//标题文字
                .setOutSideCancelable(false)//点击屏幕，点在控件外部范围时，是否取消显示
                .isCyclic(true)//是否循环滚动
                .setLabel("年","月","日","时","分","秒")//默认设置为年月日时分秒
                .isCenterLabel(false) //是否只显示中间选中项的label文字，false则每项item全部都带有label。
                .isDialog(false)//是否显示为对话框样式
                .build();
                pvEndTime.setDate(Calendar.getInstance());//注：根据需求来决定是否使用该方法（一般是精确到秒的情况），此项可以在弹出选择器的时候重新设置当前时间，避免在初始化之后由于时间已经设定，导致选中时间与当前时间不匹配的问题。
                pvEndTime.show();
                break;
        }
    }
}
