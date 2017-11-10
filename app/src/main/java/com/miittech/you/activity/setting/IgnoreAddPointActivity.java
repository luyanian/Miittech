package com.miittech.you.activity.setting;

import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.CircleOptions;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.map.Stroke;
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
import com.miittech.you.impl.OnListItemClick;
import com.miittech.you.impl.TitleBarOptions;
import com.miittech.you.location.LocationClient;
import com.miittech.you.weight.Titlebar;
import com.ryon.mutils.KeyboardUtils;
import com.ryon.mutils.StringUtils;

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

    private BaiduMap mBaiduMap;
    private PoiSearch mPoiSearch;
    private PoiResultAdapter poiResultAdapter;

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
                PoiInfo poiInfo = (PoiInfo) o;
                OverlayOptions ooCircle = new CircleOptions().fillColor(0x000000FF)
                        .center(poiInfo.location).stroke(new Stroke(5, 0xAA000000))
                        .radius(1400);
                mBaiduMap.addOverlay(ooCircle);
            }
        });
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
                MyLocationData locData = new MyLocationData.Builder()
                        .accuracy(bdLocation.getRadius())
                        // 此处设置开发者获取到的方向信息，顺时针0-360
                        .direction(0).latitude(bdLocation.getLatitude())
                        .longitude(bdLocation.getLongitude()).build();
                // 设置定位数据
                mBaiduMap.setMyLocationData(locData);

                LatLng ll = new LatLng(bdLocation.getLatitude(),bdLocation.getLongitude());
                MapStatus.Builder builder = new MapStatus.Builder();
                builder.target(ll).zoom(18.0f);
                mBaiduMap.animateMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()));

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
}
