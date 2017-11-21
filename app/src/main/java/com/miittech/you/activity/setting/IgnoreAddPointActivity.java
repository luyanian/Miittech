package com.miittech.you.activity.setting;

import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.search.core.PoiInfo;
import com.baidu.mapapi.search.poi.OnGetPoiSearchResultListener;
import com.baidu.mapapi.search.poi.PoiCitySearchOption;
import com.baidu.mapapi.search.poi.PoiDetailResult;
import com.baidu.mapapi.search.poi.PoiIndoorResult;
import com.baidu.mapapi.search.poi.PoiResult;
import com.baidu.mapapi.search.poi.PoiSearch;
import com.google.gson.Gson;
import com.miittech.you.App;
import com.miittech.you.R;
import com.miittech.you.activity.BaseActivity;
import com.miittech.you.adapter.PoiResultAdapter;
import com.miittech.you.common.Common;
import com.miittech.you.global.HttpUrl;
import com.miittech.you.global.Params;
import com.miittech.you.global.PubParam;
import com.miittech.you.impl.OnListItemClick;
import com.miittech.you.impl.TitleBarOptions;
import com.miittech.you.location.LocationClient;
import com.miittech.you.net.ApiServiceManager;
import com.miittech.you.net.response.BaseResponse;
import com.miittech.you.net.response.DeviceResponse;
import com.miittech.you.weight.Titlebar;
import com.ryon.mutils.EncryptUtils;
import com.ryon.mutils.LogUtils;
import com.ryon.mutils.StringUtils;
import com.ryon.mutils.ToastUtils;

import java.util.HashMap;
import java.util.LinkedHashMap;
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
 * Created by Administrator on 2017/9/30.
 */

public class IgnoreAddPointActivity extends BaseActivity {

    @BindView(R.id.et_ignore_serch_text)
    EditText etSerchText;
    @BindView(R.id.map_view)
    MapView mMapView;
    @BindView(R.id.titlebar)
    Titlebar titlebar;
    @BindView(R.id.recyclerview)
    RecyclerView recyclerView;
    @BindView(R.id.seekbar)
    SeekBar seekbar;
    @BindView(R.id.tv_radius)
    TextView tvRadius;

    private BaiduMap mBaiduMap;
    private PoiSearch mPoiSearch;
    private PoiResultAdapter poiResultAdapter;
    private int progress=200;
    private PoiInfo poiInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ignore_point_add);
        ButterKnife.bind(this);
        initMyTitleBar(titlebar,"设置位置勿扰区域");
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
                updateIgnoreSetting();
            }
        });
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(mLayoutManager);
        //如果可以确定每个item的高度是固定的，设置这个选项可以提高性能
        recyclerView.setHasFixedSize(true);

        mPoiSearch = PoiSearch.newInstance();
        mBaiduMap = mMapView.getMap();
        mBaiduMap.setMyLocationEnabled(true);
        MapView.setMapCustomEnable(true);
        poiResultAdapter = new PoiResultAdapter(this);
        recyclerView.setAdapter(poiResultAdapter);
        //普通地图
        mBaiduMap.setMapType(BaiduMap.MAP_TYPE_NORMAL);
        mPoiSearch.setOnGetPoiSearchResultListener(poiListener);
        poiResultAdapter.setOnItemClick(new OnListItemClick(){

            @Override
            public void onItemClick(Object o) {
                super.onItemClick(o);
                poiResultAdapter.clearData();
                PoiInfo poiInfo = (PoiInfo) o;
                IgnoreAddPointActivity.this.poiInfo = poiInfo;
                updateMapLocalView(poiInfo.location);
            }
        });
        seekbar.setMax(800);
        seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                IgnoreAddPointActivity.this.progress = progress+200;
                tvRadius.setText("当前半径"+IgnoreAddPointActivity.this.progress+"米");
                MyLocationData locationData = mBaiduMap.getLocationData();
                if(locationData!=null) {
                    LatLng latLng = new LatLng(locationData.latitude,locationData.longitude);
                    updateMapLocalView(latLng);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        seekbar.setProgress(0);
        LocationClientOption option = new LocationClientOption();
        option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);
        option.setCoorType("bd09ll");
        option.setScanSpan(0);//只定位一次
        option.setOpenGps(true);
        option.setIsNeedAddress(true);
        option.setLocationNotify(true);
        option.setIgnoreKillProcess(false);
        option.SetIgnoreCacheException(false);
        option.setWifiCacheTimeOut(5*60*1000);
        option.setEnableSimulateGps(false);
        LocationClient.getInstance().initLocation().startLocation(option, new BDAbstractLocationListener() {
            @Override
            public void onReceiveLocation(final BDLocation bdLocation) {
                updateMapLocalView(new LatLng(bdLocation.getLatitude(),bdLocation.getLongitude()));

                etSerchText.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {

                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                        if(bdLocation.getCity()==null||s==null||StringUtils.isEmpty(s.toString())){
                            poiResultAdapter.setPoiResult(null);
                            return;
                        }
                        mPoiSearch.searchInCity((new PoiCitySearchOption())
                                .city(bdLocation.getCity())
                                .keyword(s.toString())
                                .pageNum(6));
                    }
                });
            }
        });
    }

    @OnClick({R.id.tv_cancle, R.id.img_ignore_serch, R.id.img_ignore_serch_delete})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.tv_cancle:
                etSerchText.setText("");
                break;
            case R.id.img_ignore_serch:
                break;
            case R.id.img_ignore_serch_delete:
                etSerchText.setText("");
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mPoiSearch.destroy();
    }

    OnGetPoiSearchResultListener poiListener = new OnGetPoiSearchResultListener(){

        public void onGetPoiResult(PoiResult result){
            //获取POI检索结果
            poiResultAdapter.setPoiResult(result);
        }

        public void onGetPoiDetailResult(PoiDetailResult result){
            PoiDetailResult resu=result;
        }

        @Override
        public void onGetPoiIndoorResult(PoiIndoorResult poiIndoorResult) {
            PoiIndoorResult poiIn = poiIndoorResult;
        }
    };

    public void updateMapLocalView(LatLng latLng){
        MyLocationData locData = new MyLocationData.Builder()
                .accuracy(IgnoreAddPointActivity.this.progress)
                .latitude(latLng.latitude)
                .longitude(latLng.longitude)
                .build();
        mBaiduMap.setMyLocationData(locData);

        MapStatus.Builder builder = new MapStatus.Builder();
        builder.target(latLng).zoom(16.0f);
        mBaiduMap.animateMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()));
    }

    public void updateIgnoreSetting(){
        if(this.poiInfo==null){
            return;
        }
        Map area = new HashMap();
        area.put("lat",this.poiInfo.location.latitude);
        area.put("lng",this.poiInfo.location.longitude);
        area.put("R",this.progress);
        Map areadef = new HashMap();
        areadef.put("id",0);
        areadef.put("title", Common.encodeBase64(this.poiInfo.name));
        areadef.put("inout",1);
        areadef.put("areadef",area);
        Map donotdisturb = new HashMap();
        donotdisturb.put("donotdisturb",areadef);
        Map param = new LinkedHashMap();
        param.put("method", Params.METHOD.IGNORE_ADD);
        param.put("config_type", "AREA");
        param.put("config", donotdisturb);
        String json = new Gson().toJson(param);
        PubParam pubParam = new PubParam(App.getInstance().getUserId());
        String sign_unSha1 = pubParam.toValueString() + json + App.getInstance().getTocken();
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
                        }else{
                            response.onError(IgnoreAddPointActivity.this);
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
