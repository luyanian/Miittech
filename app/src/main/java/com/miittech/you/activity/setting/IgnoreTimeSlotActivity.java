package com.miittech.you.activity.setting;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.google.gson.Gson;
import com.miittech.you.R;
import com.miittech.you.activity.BaseActivity;
import com.miittech.you.utils.Common;
import com.miittech.you.global.HttpUrl;
import com.miittech.you.global.IntentExtras;
import com.miittech.you.global.Params;
import com.miittech.you.global.PubParam;
import com.miittech.you.impl.TitleBarOptions;
import com.miittech.you.net.ApiServiceManager;
import com.miittech.you.net.response.BaseResponse;
import com.miittech.you.net.response.UserInfoResponse;
import com.miittech.you.weight.Titlebar;
import com.ryon.mutils.ActivityPools;
import com.ryon.mutils.EncryptUtils;
import com.ryon.mutils.LogUtils;
import com.ryon.mutils.NetworkUtils;
import com.ryon.mutils.TimeUtils;
import com.ryon.mutils.ToastUtils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import okhttp3.MediaType;
import okhttp3.RequestBody;

/**
 * Created by Administrator on 2017/11/22.
 */

public class IgnoreTimeSlotActivity extends BaseActivity {
    @BindView(R.id.titlebar)
    Titlebar titlebar;
    @BindView(R.id.et_name)
    EditText etName;
    @BindView(R.id.tv_time)
    TextView tvTime;
    @BindView(R.id.tv_is_repeat)
    TextView tvIsRepeat;

    private String week="";
    private String startTime="";
    private String endTime ="";
    private String ignoreTimeId = "0";

    private final static int REQUEST_CODE_TIME=0x01;
    private final static int REQUEST_CODE_REPEAT=0x02;
    private boolean isSubmitting = false;
    private UserInfoResponse.ConfigBean.DonotdisturbBean.TimelistBean timelistBean;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ignore_time_slot);
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
                doComplete();
            }
        });
        if(getIntent().hasExtra(IntentExtras.IGNORE.DATA)){
            timelistBean = (UserInfoResponse.ConfigBean.DonotdisturbBean.TimelistBean) getIntent().getSerializableExtra(IntentExtras.IGNORE.DATA);
            ignoreTimeId = timelistBean.getId();
            etName.setText(Common.decodeBase64(timelistBean.getTitle()));
            startTime = timelistBean.getStime();
            endTime = timelistBean.getEtime();

            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd");
            Date date = new Date();
            String ymd = simpleDateFormat.format(date);

            String sstime = Common.repairStrLen(timelistBean.getStime());
            String eetime = Common.repairStrLen(timelistBean.getEtime());

            DateFormat dateFormat1 = new SimpleDateFormat("yyyyMMddHHmmss");
            long millisStart = TimeUtils.string2Millis(ymd+sstime,dateFormat1);
            long millisEnd = TimeUtils.string2Millis(ymd+eetime,dateFormat1);

            DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
            String sTime = TimeUtils.millis2String(millisStart,dateFormat);
            String eTime = TimeUtils.millis2String(millisEnd,dateFormat);

            if(millisEnd<millisStart) {
                tvTime.setText(sTime + " - 次日" + eTime);
            }else{
                tvTime.setText(sTime + " - " + eTime);
            }
            week = timelistBean.getDayofweek();
            tvIsRepeat.setText(Common.formatWeekRepeat(timelistBean.getDayofweek()));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode){
            case REQUEST_CODE_TIME:
                if(resultCode==RESULT_OK){
                    startTime = data.getStringExtra("startTime");
                    endTime = data.getStringExtra("endTime");
                    DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
                    if(TimeUtils.string2Millis(endTime,dateFormat)<TimeUtils.string2Millis(startTime,dateFormat)){
                        tvTime.setText(startTime + " - 次日" + endTime);
                    }else {
                        tvTime.setText(startTime + " - " + endTime);
                    }
                }
                break;
            case REQUEST_CODE_REPEAT:
                if(resultCode==RESULT_OK){
                    week = data.getStringExtra("key");
                    tvIsRepeat.setText(data.getStringExtra("value"));
                }
                break;
        }
    }

    @OnClick({R.id.rl_name, R.id.rl_time, R.id.rl_is_repeat})
    public void onViewClicked(View view) {
        Intent intent = new Intent();
        switch (view.getId()) {
            case R.id.rl_name:
                break;
            case R.id.rl_time:
                intent.setClass(this,IgnoreTimeSelectActivity.class);
                intent.putExtra("tvTime",tvTime.getText().toString().trim());
                startActivityForResult(intent,REQUEST_CODE_TIME);
                break;
            case R.id.rl_is_repeat:
                intent.setClass(this,IgnoreRepeatActivity.class);
                intent.putExtra("week",week);
                startActivityForResult(intent,REQUEST_CODE_REPEAT);
                break;
        }
    }

    private void doComplete() {
        if(isSubmitting){
            return;
        }
        String title = etName.getText().toString().trim();
        if(TextUtils.isEmpty(title)){
            ToastUtils.showShort("请填写名称！");
            return;
        }
        if(TextUtils.isEmpty(week)){
            ToastUtils.showShort("请选择重复周期！");
            return;
        }
        if(TextUtils.isEmpty(startTime)||TextUtils.isEmpty(endTime)){
            ToastUtils.showShort("请选择时间！");
            return;
        }
        if(!NetworkUtils.isConnected()){
            ToastUtils.showShort(R.string.msg_net_error);
            return;
        }
        isSubmitting=true;
        Map timedef = new HashMap();
        timedef.put("id",ignoreTimeId);
        timedef.put("title", Common.encodeBase64(title));
        timedef.put("dayofweek",week);
        timedef.put("stime",startTime.replace(":",""));
        timedef.put("etime",endTime.replace(":",""));
        Map donotdisturb = new HashMap();
        donotdisturb.put("timedef",timedef);
        Map config = new HashMap();
        config.put("donotdisturb",donotdisturb);
        Map param = new HashMap();
        if(getIntent().hasExtra(IntentExtras.IGNORE.DATA)){
            param.put("method", Params.METHOD.IGNORE_UPD);
        }else {
            param.put("method", Params.METHOD.IGNORE_ADD);
        }
        param.put("config_type", "TIME");
        param.put("config", config);
        String json = new Gson().toJson(param);
        PubParam pubParam = new PubParam(Common.getUserId());
        String sign_unSha1 = pubParam.toValueString() + json + Common.getTocken();
        LogUtils.d("sign_unsha1", sign_unSha1);
        String sign = EncryptUtils.encryptSHA1ToString(sign_unSha1).toLowerCase();
        LogUtils.d("sign_sha1", sign);
        String path = HttpUrl.Api + "userconf/" + pubParam.toUrlParam(sign);
        final RequestBody requestBody = RequestBody.create(MediaType.parse(HttpUrl.MediaType_Json), json);
        ApiServiceManager.getInstance().buildApiService(this).postNetRequest(path, requestBody)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<BaseResponse>() {
                    @Override
                    public void accept(BaseResponse response) throws Exception {
                        isSubmitting=false;
                        if(response.isSuccessful()){
                            ActivityPools.finishActivity(IgnoreTimeSlotActivity.class);
                        }else{
                            response.onError(IgnoreTimeSlotActivity.this);
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        isSubmitting=false;
                        throwable.printStackTrace();
                    }
                });
    }
}
