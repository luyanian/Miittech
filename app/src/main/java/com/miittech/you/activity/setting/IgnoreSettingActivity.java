package com.miittech.you.activity.setting;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.gson.Gson;
import com.miittech.you.R;
import com.miittech.you.activity.BaseActivity;
import com.miittech.you.utils.Common;
import com.miittech.you.dialog.DialogUtils;
import com.miittech.you.global.HttpUrl;
import com.miittech.you.global.IntentExtras;
import com.miittech.you.global.Params;
import com.miittech.you.global.PubParam;
import com.miittech.you.global.SPConst;
import com.miittech.you.impl.OnIgnoreAddOptions;
import com.miittech.you.impl.OnMsgTipOptions;
import com.miittech.you.impl.TitleBarOptions;
import com.miittech.you.impl.TypeSelectorChangeLisener;
import com.miittech.you.net.ApiServiceManager;
import com.miittech.you.net.response.BaseResponse;
import com.miittech.you.net.response.UserInfoResponse;
import com.miittech.you.weight.Titlebar;
import com.miittech.you.weight.TypeSelector;
import com.ryon.mutils.EncryptUtils;
import com.ryon.mutils.LogUtils;
import com.ryon.mutils.NetworkUtils;
import com.ryon.mutils.SPUtils;
import com.ryon.mutils.TimeUtils;
import com.ryon.mutils.ToastUtils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
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
 * Created by Administrator on 2017/9/29.
 */

public class IgnoreSettingActivity extends BaseActivity implements TypeSelectorChangeLisener {
    @BindView(R.id.titlebar)
    Titlebar titlebar;
    @BindView(R.id.typeSelector)
    TypeSelector typeSelector;
    @BindView(R.id.tv_setting_title)
    TextView tvSettingTitle;
    @BindView(R.id.check_setting_ignore)
    CheckBox checkSettingIgnore;
    @BindView(R.id.tv_setting_ignore_desc)
    TextView tvSettingIgnoreDesc;
    @BindView(R.id.tv_setting_ignore_area)
    TextView tvSettingIgnoreArea;
    @BindView(R.id.ll_ignore_areas)
    LinearLayout llIgnoreAreas;
    @BindView(R.id.tv_setting_ignore_ssid)
    TextView tvSettingIgnoreSsid;
    @BindView(R.id.ll_ignore_ssids)
    LinearLayout llIgnoreSsids;
    @BindView(R.id.ll_ignore_points)
    LinearLayout llIgnorePoints;
    @BindView(R.id.ll_ignore_times)
    LinearLayout llIgnoreTimes;
    @BindView(R.id.btn_add_text)
    TextView btnAddText;

    private boolean isEdit = false;
    private List<CheckBox> ignoreCheckList = new ArrayList<>();
    private static Object delObject;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting_ignore);
        ButterKnife.bind(this);
        initMyTitleBar(titlebar, R.string.text_setting_ignore);
        titlebar.showBackOption();
        titlebar.showSettingOption(R.drawable.ic_ignore_delete);
        titlebar.setTitleBarOptions(new TitleBarOptions() {
            @Override
            public void onBack() {
                super.onBack();
                finish();
            }

            @Override
            public void onSetting() {
                super.onSetting();
                if(isEdit){
                    isEdit = false;
                    toggleAllCheck(false);
                    if(typeSelector.getSelectItem()==0){
                        btnAddText.setText(getResources().getString(R.string.text_setting_ignore_area_add));
                    }else{
                        btnAddText.setText(getResources().getString(R.string.text_setting_ignore_time_add));
                    }
                }else{
                    toggleAllCheck(true);
                    isEdit = true;
                    btnAddText.setText("删除");
                }
            }
        });
        typeSelector.setItemText(R.string.text_setting_ignore, R.string.text_setting_ignore_time);
        typeSelector.setTypeSelectorChangeLisener(this);
        typeSelector.setSelectItem(0);
    }
    private void updateIngnoreSettingValid(int valid) {
        Map userattr = new HashMap();
        Map param = new HashMap();
        if(typeSelector.getSelectItem()==0) {
            SPUtils.getInstance().remove(SPConst.DISTURB.ISAREADISTURB);
            SPUtils.getInstance().put(SPConst.DISTURB.ISAREADISTURB,valid);
            userattr.put("isAreaDisturb", valid);
            param.put("method", "E");
        }else if(typeSelector.getSelectItem()==1){
            SPUtils.getInstance().remove(SPConst.DISTURB.ISTIMEDISTURB);
            SPUtils.getInstance().put(SPConst.DISTURB.ISTIMEDISTURB,valid);
            userattr.put("isTimeDisturb", valid);
            param.put("method", "F");
        }
        param.put("userattr", userattr);

        if(!NetworkUtils.isConnected()){
            return;
        }
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

                        } else {
                            ToastUtils.showShort(response.getErrmsg());
                        }
                    }
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        getIgnoreSetting();
    }

    @Override
    public void onTabSelectorChanged(int item) {
        if (item == 0) {
            tvSettingTitle.setText(getResources().getString(R.string.text_setting_ignore_area));
            tvSettingIgnoreDesc.setText(getResources().getString(R.string.tip_msg_ignore_area_desc));
            llIgnorePoints.setVisibility(View.VISIBLE);
            llIgnoreTimes.setVisibility(View.GONE);
            if(isEdit){
                btnAddText.setText("删除");
            }else {
                btnAddText.setText(getResources().getString(R.string.text_setting_ignore_area_add));
            }
            if(SPUtils.getInstance().getInt(SPConst.DISTURB.ISAREADISTURB,1)==1){
                checkSettingIgnore.setChecked(true);
            }else{
                checkSettingIgnore.setChecked(false);
            }
        } else if (item == 1) {
            tvSettingTitle.setText(getResources().getString(R.string.text_setting_ignore_time));
            tvSettingIgnoreDesc.setText(getResources().getString(R.string.tip_msg_ignore_time_desc));
            llIgnorePoints.setVisibility(View.GONE);
            llIgnoreTimes.setVisibility(View.VISIBLE);
            if(isEdit){
                btnAddText.setText("删除");
            }else {
                btnAddText.setText(getResources().getString(R.string.text_setting_ignore_time_add));
            }
            if(SPUtils.getInstance().getInt(SPConst.DISTURB.ISTIMEDISTURB,1)==1){
                checkSettingIgnore.setChecked(true);
            }else{
                checkSettingIgnore.setChecked(false);
            }
        }
    }

    @OnClick({R.id.btn_add_setting,R.id.check_setting_ignore})
    public void onViewClicked(View view) {
        switch (view.getId()){
            case R.id.btn_add_setting:
                if(isEdit){
                    //删除操作
                    doDelIgnoreSetting();
                    return;
                }
                if(typeSelector.getSelectItem()==0){
                    DialogUtils.getInstance().showIgnoreAddDialog(this).setIgnoreAddOptions(new OnIgnoreAddOptions() {
                        @Override
                        public void addPointIgnore() {
                            Intent intent = new Intent(IgnoreSettingActivity.this,IgnoreAddPointActivity.class);
                            startActivity(intent);
                        }

                        @Override
                        public void addWifiIgnore() {
                            Intent intent = new Intent(IgnoreSettingActivity.this,IgnoreAddWifiActivity.class);
                            startActivity(intent);
                        }
                    });
                }
                if(typeSelector.getSelectItem()==1){
                    Intent intent = new Intent(IgnoreSettingActivity.this,IgnoreTimeSlotActivity.class);
                    startActivity(intent);
                }
                break;
            case R.id.check_setting_ignore:
                if(checkSettingIgnore.isChecked()){
                    updateIngnoreSettingValid(1);
                }else{
                    DialogUtils.getInstance().createMsgTipDialog(IgnoreSettingActivity.this)
                            .setTitle("关闭勿扰设置")
                            .setMsg("关闭后，所有的勿扰设置将失效")
                            .setLeftBtnText("取消")
                            .setRightBtnText("确定")
                            .setOnMsgTipOptions(new OnMsgTipOptions(){
                                @Override
                                public void onCancel() {
                                    super.onCancel();
                                    checkSettingIgnore.setChecked(true);
                                }

                                @Override
                                public void onSure() {
                                    super.onSure();
                                    updateIngnoreSettingValid(0);
                                }
                            }).show();
                }
                break;
        }
    }

    private void getIgnoreSetting() {
        if(!NetworkUtils.isConnected()){
            UserInfoResponse userInfoResponse = (UserInfoResponse) SPUtils.getInstance().readObject(SPConst.DATA.USERINFO);
            if(userInfoResponse!=null){
                initIgnoreConfig(userInfoResponse);
            }
            return;
        }
        Map param = new LinkedHashMap();
        param.put("qrytype", Params.QRY_TYPE.ALL);
        String json = new Gson().toJson(param);
        PubParam pubParam = new PubParam(Common.getUserId());
        String sign_unSha1 = pubParam.toValueString() + json + Common.getTocken();
        LogUtils.d("sign_unsha1", sign_unSha1);
        String sign = EncryptUtils.encryptSHA1ToString(sign_unSha1).toLowerCase();
        LogUtils.d("sign_sha1", sign);
        String path = HttpUrl.Api + "userinfo/" + pubParam.toUrlParam(sign);
        final RequestBody requestBody = RequestBody.create(MediaType.parse(HttpUrl.MediaType_Json), json);

        ApiServiceManager.getInstance().buildApiService(this).postToGetUserInfo(path, requestBody)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<UserInfoResponse>() {
                    @Override
                    public void accept(UserInfoResponse response) throws Exception {
                        if(response.isSuccessful()) {
                            SPUtils.getInstance().readObject(SPConst.DATA.USERINFO);
                            SPUtils.getInstance().saveObject(SPConst.DATA.USERINFO, response);
                            initIgnoreConfig(response);
                        }else {
                            response.onError(IgnoreSettingActivity.this);
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        throwable.printStackTrace();
                    }
                });
    }

    private void initIgnoreConfig(UserInfoResponse response) {
        llIgnoreAreas.removeAllViews();
        llIgnoreSsids.removeAllViews();
        llIgnoreTimes.removeAllViews();
        tvSettingIgnoreArea.setVisibility(View.GONE);
        tvSettingIgnoreSsid.setVisibility(View.GONE);
        if (response.getConfig() == null) {
            return;
        }else if(response.getConfig().getDonotdisturb() == null){
            return;
        }
        List<UserInfoResponse.ConfigBean.DonotdisturbBean.ArealistBean> arealist = response.getConfig().getDonotdisturb().getArealist();
        if (arealist != null) {
            updateAreaList(arealist);
        }
        List<UserInfoResponse.ConfigBean.DonotdisturbBean.TimelistBean> timelist = response.getConfig().getDonotdisturb().getTimelist();
        if (timelist != null) {
            updateTimeList(timelist);
        }
    }

    private void updateAreaList(List<UserInfoResponse.ConfigBean.DonotdisturbBean.ArealistBean> arealist) {
        for (final UserInfoResponse.ConfigBean.DonotdisturbBean.ArealistBean arealistBean:arealist){
            View view = View.inflate(this,R.layout.item_ignore_point,null);
            RelativeLayout rlItem = view.findViewById(R.id.rl_item);
            final CheckBox itemCheck = view.findViewById(R.id.item_check);
            TextView itemName = view.findViewById(R.id.item_name);
            TextView itemFlag = view.findViewById(R.id.item_flag);
            ignoreCheckList.add(itemCheck);
            itemName.setText(Common.decodeBase64(arealistBean.getTitle()));
            rlItem.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(isEdit){
                        if(itemCheck.isChecked()){
                            itemCheck.setChecked(false);
                        }else{
                            unSelectAllExcept();
                            itemCheck.setChecked(true);
                            delObject = arealistBean;
                        }
                    }else{
                        if(TextUtils.isEmpty(arealistBean.getSsid())) {
                            Intent intent = new Intent(IgnoreSettingActivity.this, IgnoreAddPointActivity.class);
                            intent.putExtra(IntentExtras.IGNORE.DATA,arealistBean);
                            startActivity(intent);
                        }else{
                            Intent intent = new Intent(IgnoreSettingActivity.this, IgnoreAddWifiActivity.class);
                            intent.putExtra(IntentExtras.IGNORE.DATA,arealistBean);
                            startActivity(intent);
                        }
                    }
                }
            });
            if(TextUtils.isEmpty(arealistBean.getSsid())) {
                tvSettingIgnoreArea.setVisibility(View.VISIBLE);
                itemFlag.setText(Common.decodeBase64(arealistBean.getArea().getAddr()));
                llIgnoreAreas.addView(view);
            }else{
                tvSettingIgnoreSsid.setVisibility(View.VISIBLE);
                itemFlag.setText(Common.decodeBase64(arealistBean.getSsid()));
                llIgnoreSsids.addView(view);
            }
        }
    }

    private void updateTimeList(List<UserInfoResponse.ConfigBean.DonotdisturbBean.TimelistBean> timelist) {
        for (final UserInfoResponse.ConfigBean.DonotdisturbBean.TimelistBean timelistBean:timelist){
            View view = View.inflate(this,R.layout.item_ignore_time,null);
            RelativeLayout rlItem = view.findViewById(R.id.rl_item);
            final CheckBox itemCheck = view.findViewById(R.id.item_check);
            TextView itemName = view.findViewById(R.id.item_name);
            TextView itemTime = view.findViewById(R.id.item_time);
            TextView itemRepeat = view.findViewById(R.id.item_repet);
            ignoreCheckList.add(itemCheck);
            itemName.setText(Common.decodeBase64(timelistBean.getTitle()));
            try {
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
                    itemTime.setText( sTime + " - 次日" + eTime);
                }else{
                    itemTime.setText( sTime + " - " + eTime);
                }
            }catch (Exception e){
                String sTime = timelistBean.getStime();
                String eTime = timelistBean.getEtime();
                itemTime.setText( sTime + " - " + eTime);
            }
            itemRepeat.setText(Common.formatWeekRepeat(timelistBean.getDayofweek()));
            rlItem.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(isEdit){
                        if(itemCheck.isChecked()){
                            itemCheck.setChecked(false);
                        }else{
                            unSelectAllExcept();
                            itemCheck.setChecked(true);
                            delObject = timelistBean;
                        }
                    }else{
                        Intent intent = new Intent(IgnoreSettingActivity.this, IgnoreTimeSlotActivity.class);
                        intent.putExtra(IntentExtras.IGNORE.DATA,timelistBean);
                        startActivity(intent);
                    }
                }
            });
            llIgnoreTimes.addView(view);
        }
    }

    private void unSelectAllExcept() {
        for(CheckBox item : ignoreCheckList){
            if(item!=null) {
                item.setChecked(false);
            }
        }
    }
    private void toggleAllCheck(boolean isShow){
        for(CheckBox item : ignoreCheckList){
            if(item!=null) {
                item.setChecked(false);
                item.setVisibility(isShow?View.VISIBLE:View.GONE);
            }
        }
    }
    private void doDelIgnoreSetting() {
        if(delObject==null){
            ToastUtils.showShort("请选择删除信息");
            return;
        }
        if(!NetworkUtils.isConnected()){
            ToastUtils.showShort(R.string.msg_net_error);
            return;
        }
        Map param = new HashMap();
        if(delObject instanceof UserInfoResponse.ConfigBean.DonotdisturbBean.TimelistBean){
            UserInfoResponse.ConfigBean.DonotdisturbBean.TimelistBean timelistBean = (UserInfoResponse.ConfigBean.DonotdisturbBean.TimelistBean) delObject;
            Map timedef = new HashMap();
            timedef.put("id",timelistBean.getId());
            timedef.put("title", timelistBean.getTitle());
            timedef.put("dayofweek",timelistBean.getDayofweek());
            timedef.put("stime",timelistBean.getStime());
            timedef.put("etime",timelistBean.getEtime());
            Map donotdisturb = new HashMap();
            donotdisturb.put("timedef",timedef);
            Map config = new HashMap();
            config.put("donotdisturb",donotdisturb);

            param.put("method", Params.METHOD.IGNORE_DEL);
            param.put("config_type", "TIME");
            param.put("config", config);
        }else{
            UserInfoResponse.ConfigBean.DonotdisturbBean.ArealistBean arealistBean = (UserInfoResponse.ConfigBean.DonotdisturbBean.ArealistBean) delObject;
            Map area = new HashMap();
            if(arealistBean.getArea()!=null) {
                area.put("lat", arealistBean.getArea().getLat());
                area.put("lng", arealistBean.getArea().getLng());
                area.put("R", arealistBean.getArea().getR());
            }
            Map areadef = new HashMap();
            areadef.put("id",arealistBean.getId());
            areadef.put("title", arealistBean.getTitle());
            areadef.put("inout",1);
            areadef.put("areadef",area);
            areadef.put("ssid",arealistBean.getSsid());
            Map donotdisturb = new HashMap();
            donotdisturb.put("areadef",areadef);
            Map config = new HashMap();
            config.put("donotdisturb",donotdisturb);

            param.put("method", Params.METHOD.IGNORE_DEL);
            param.put("config_type", "AREA");
            param.put("config", config);
        }

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
                        if(response.isSuccessful()){
                            delObject=null;
                            isEdit = false;
                            getIgnoreSetting();
                            toggleAllCheck(false);
                            if(typeSelector.getSelectItem()==0){
                                btnAddText.setText(getResources().getString(R.string.text_setting_ignore_area_add));
                            }else{
                                btnAddText.setText(getResources().getString(R.string.text_setting_ignore_time_add));
                            }
                        }else{
                            response.onError(IgnoreSettingActivity.this);
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
