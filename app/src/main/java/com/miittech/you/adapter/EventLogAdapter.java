package com.miittech.you.adapter;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BaiduMapOptions;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.LogoPosition;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.map.SupportMapFragment;
import com.baidu.mapapi.map.TextureMapView;
import com.baidu.mapapi.model.LatLng;
import com.miittech.you.R;
import com.miittech.you.common.Common;
import com.miittech.you.global.Params;
import com.miittech.you.impl.OnListItemClick;
import com.miittech.you.net.response.UserInfoResponse;
import com.ryon.mutils.TimeUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by Administrator on 2017/10/19.
 */

public class EventLogAdapter extends RecyclerView.Adapter {

    private List<UserInfoResponse.EventlistBean> eventlist = new ArrayList<>();
    private FragmentActivity activity;
    private OnListItemClick onDeviceItemClick;
    FragmentManager manager;

    public EventLogAdapter(FragmentActivity context, OnListItemClick onDeviceItemClick) {
        this.activity = context;
        this.onDeviceItemClick = onDeviceItemClick;
        manager = activity.getSupportFragmentManager();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, final int i) {
        View view = View.inflate(activity, R.layout.item_event_log, null);
        ViewHolder viewHolder = new ViewHolder(view);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int i) {
        final UserInfoResponse.EventlistBean eventlistBean = eventlist.get(i);
        final ViewHolder holder = (ViewHolder) viewHolder;
        String eventStr = "";
        switch (eventlistBean.getEtype()) {
            case Params.EVENT_TYPE.DEVICE_ADD:
                eventStr = "绑定成功";
                break;
            case Params.EVENT_TYPE.DEVICE_CONNECT:
                eventStr = "连接成功";
                break;
            case Params.EVENT_TYPE.DEVICE_LOSE:
                eventStr = "丢失";
                break;
            case Params.EVENT_TYPE.DEVICE_REDISCOVER:
                eventStr = "找回成功";
                break;
        }
        holder.itemTitle.setText("您的" + Common.decodeBase64(eventlistBean.getDevname()) + eventStr);
        holder.itemTime.setText(TimeUtils.getFriendlyTimeSpanByNow(eventlistBean.getEtime(), new SimpleDateFormat("yyyyMMddhhmmss")));
        if (eventlistBean.getLocinfo() != null) {
            LatLng latLng = new LatLng(eventlistBean.getLocinfo().getLat(), eventlistBean.getLocinfo().getLng());
            MapStatus.Builder builder = new MapStatus.Builder();
            builder.target(latLng);
            BaiduMapOptions bo = new BaiduMapOptions().mapStatus(builder.build()).compassEnabled(false).zoomControlsEnabled(false);
            SupportMapFragment supportMapFragment = SupportMapFragment.newInstance(bo);
            manager.beginTransaction().add(R.id.id_fragment_eventlog, supportMapFragment, "map_fragment").commit();

            final BaiduMap mBaiduMap = supportMapFragment.getBaiduMap();
            final MapView mapView = supportMapFragment.getMapView();
            mBaiduMap.setOnMapLoadedCallback(new BaiduMap.OnMapLoadedCallback() {
                @Override
                public void onMapLoaded() {
                    mBaiduMap.snapshot(new BaiduMap.SnapshotReadyCallback() {
                        @Override
                        public void onSnapshotReady(Bitmap bitmap) {
                            if(bitmap!=null){
                                holder.itemImgMap.setImageBitmap(bitmap);
                            }
                        }
                    });
                }
            });

//            MapView mapView = holder.supportMapFragment.getMapView();
//            BaiduMap baiduMap = holder.supportMapFragment.getBaiduMap();
//            baiduMap.getUiSettings().setAllGesturesEnabled(false);
//            mapView.showZoomControls(false);
//            mapView.setMapCustomEnable(false);
//            mapView.setLogoPosition(LogoPosition.logoPostionRightBottom);
//
//            LatLng latLng = new LatLng(eventlistBean.getLocinfo().getLat(), eventlistBean.getLocinfo().getLng());
//            MapStatusUpdate u1 = MapStatusUpdateFactory.newLatLng(latLng);
//            baiduMap.setMapStatus(u1);

//            BitmapDescriptor mCurrentMarker = BitmapDescriptorFactory.fromResource(R.drawable.ic_map);
//            MyLocationConfiguration config = new MyLocationConfiguration(MyLocationConfiguration.LocationMode.NORMAL, true, mCurrentMarker, 0xAAFFFF88, 0xAA00FF00);
//            mBaiduMap.setMyLocationConfiguration(config);
//            MyLocationData locData = new MyLocationData.Builder().latitude(eventlistBean.getLocinfo().getLat()).longitude(eventlistBean.getLocinfo().getLng()).build();
//            mBaiduMap.setMyLocationData(locData);

        }
        holder.llItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onDeviceItemClick != null) {
                    onDeviceItemClick.onItemClick(eventlistBean);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return eventlist.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.item_title)
        TextView itemTitle;
        @BindView(R.id.item_time)
        TextView itemTime;
        @BindView(R.id.item_img_map)
        ImageView itemImgMap;
        @BindView(R.id.ll_item)
        LinearLayout llItem;

        ViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }
    }

    public void refreshEventLog(List<UserInfoResponse.EventlistBean> eventlist) {
        this.eventlist.clear();
        this.eventlist.addAll(eventlist);
        notifyDataSetChanged();
    }

    public void loadMoreEventLog(List<UserInfoResponse.EventlistBean> eventlist) {
        this.eventlist.addAll(eventlist);
        notifyDataSetChanged();
    }
}
