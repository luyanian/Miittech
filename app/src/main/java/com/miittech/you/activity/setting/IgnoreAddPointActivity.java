package com.miittech.you.activity.setting;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;

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
import com.miittech.you.R;
import com.miittech.you.activity.BaseActivity;
import com.miittech.you.adapter.PoiResultAdapter;
import com.miittech.you.utils.Common;
import com.miittech.you.entity.Locinfo;
import com.miittech.you.global.IntentExtras;
import com.miittech.you.global.SPConst;
import com.miittech.you.impl.OnListItemClick;
import com.miittech.you.impl.TitleBarOptions;
import com.miittech.you.net.response.UserInfoResponse;
import com.miittech.you.weight.Titlebar;
import com.ryon.mutils.SPUtils;
import com.ryon.mutils.ToastUtils;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

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
    private Locinfo locinfo;
    private boolean isLocUpdate = false;
    private UserInfoResponse.ConfigBean.DonotdisturbBean.ArealistBean arealistBean;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ignore_point_add);
        ButterKnife.bind(this);
        initMyTitleBar(titlebar,"设置位置勿扰区域");
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE|WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
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
                Intent intent = new Intent(IgnoreAddPointActivity.this,IgnoreNameEditActivity.class);
                if(getIntent().hasExtra(IntentExtras.IGNORE.DATA)&&arealistBean!=null) {
                    if(isLocUpdate&&locinfo!=null){
                        intent.putExtra("id",arealistBean.getId());
                        intent.putExtra("name",Common.decodeBase64(arealistBean.getTitle()));
                        intent.putExtra("lat",locinfo.getLat());
                        intent.putExtra("lng",locinfo.getLng());
                        intent.putExtra("addr",locinfo.getAddr());
                        intent.putExtra("progress",progress);
                        startActivity(intent);
                    }else{
                        intent.putExtra("id",arealistBean.getId());
                        intent.putExtra("name",Common.decodeBase64(arealistBean.getTitle()));
                        intent.putExtra("lat",arealistBean.getArea().getLat());
                        intent.putExtra("lng",arealistBean.getArea().getLng());
                        intent.putExtra("addr",Common.decodeBase64(arealistBean.getArea().getAddr()));
                        intent.putExtra("progress",progress);
                        startActivity(intent);
                    }
                    return;
                }
                if(IgnoreAddPointActivity.this.locinfo==null){
                    ToastUtils.showShort("没有获取到合法的位置");
                    return;

                }else {
                    intent.putExtra("lat",locinfo.getLat());
                    intent.putExtra("lng",locinfo.getLng());
                    intent.putExtra("addr",locinfo.getAddr());
                    intent.putExtra("progress", progress);
                    startActivity(intent);
                }
            }
        });
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(mLayoutManager);
        //如果可以确定每个item的高度是固定的，设置这个选项可以提高性能
        recyclerView.setHasFixedSize(true);
        poiResultAdapter = new PoiResultAdapter(this);
        recyclerView.setAdapter(poiResultAdapter);

        mBaiduMap = mMapView.getMap();
        mBaiduMap.setMapType(BaiduMap.MAP_TYPE_NORMAL);
        mBaiduMap.setMyLocationEnabled(true);
        MapView.setMapCustomEnable(true);
        mPoiSearch = PoiSearch.newInstance();
        mPoiSearch.setOnGetPoiSearchResultListener(poiListener);
        poiResultAdapter.setOnItemClick(new OnListItemClick(){

            @Override
            public void onItemClick(Object o) {
                super.onItemClick(o);
                isLocUpdate = true;
                poiResultAdapter.clearData();
                PoiInfo poiInfo = (PoiInfo) o;
                if(locinfo==null){
                    locinfo = new Locinfo();
                }
                locinfo.setLat(poiInfo.location.latitude);
                locinfo.setLng(poiInfo.location.longitude);
                locinfo.setAddr(poiInfo.address);
                locinfo.setCity(poiInfo.city);
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
        boolean islocation = true;
        if(getIntent().hasExtra(IntentExtras.IGNORE.DATA)){
            arealistBean = (UserInfoResponse.ConfigBean.DonotdisturbBean.ArealistBean) getIntent().getSerializableExtra(IntentExtras.IGNORE.DATA);
            if(arealistBean!=null) {
                this.progress = arealistBean.getArea().getR();
                if(this.progress>=200&&this.progress<=1000)
                seekbar.setProgress(this.progress-200);
                updateMapLocalView(new LatLng(arealistBean.getArea().getLat(), arealistBean.getArea().getLng()));
                islocation = false;
            }
        }

        locinfo = (Locinfo) SPUtils.getInstance().readObject(SPConst.LOC_INFO);
        if(locinfo!=null) {
            if(islocation) {
                updateMapLocalView(new LatLng(locinfo.getLat(), locinfo.getLng()));
            }
            etSerchText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable s) {
                    if(TextUtils.isEmpty(s.toString())||TextUtils.isEmpty(locinfo.getCity())){
                        return;
                    }
                    mPoiSearch.searchInCity((new PoiCitySearchOption())
                            .city(locinfo.getCity())
                            .isReturnAddr(true)
                            .keyword(s.toString())
                            .pageCapacity(12)
                            .pageNum(1));
                }
            });
            String temp = etSerchText.getText().toString().trim();
            if(!TextUtils.isEmpty(temp)) {
                mPoiSearch.searchInCity((new PoiCitySearchOption())
                        .city(locinfo.getCity())
                        .keyword(temp)
                        .pageCapacity(12)
                        .pageNum(1));
            }
        }
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
}
