package com.miittech.you.utils;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;

import com.miittech.you.R;
import com.miittech.you.adapter.MapDeviceUsersListAdapter;
import com.miittech.you.entity.DeviceInfo;
import com.miittech.you.impl.OnListItemClick;
import com.miittech.you.net.response.FriendsResponse;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by Administrator on 2017/11/8.
 */

public class MapDeviceUsersPopWindowOptions<T>{
    static MapDeviceUsersPopWindowOptions mapDeviceUsersListOptions;
    PopupWindow window;
    RecyclerView recyclerview;
    private MapDeviceUsersListAdapter mapAdapter;

    public static MapDeviceUsersPopWindowOptions getInstance(){
        if(mapDeviceUsersListOptions==null){
            synchronized (MapDeviceUsersPopWindowOptions.class){
                mapDeviceUsersListOptions = new MapDeviceUsersPopWindowOptions();
            }
        }
        return mapDeviceUsersListOptions;
    }

    public void initWindow(@NonNull Context context) {
        View contentView= View.inflate(context,R.layout.dialog_map_device_users_list, null);
        recyclerview = contentView.findViewById(R.id.recyclerview);
        window=new PopupWindow(contentView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        window.setTouchable(true); // 设置popupwindow可点击
        window.setOutsideTouchable(false); // 设置popupwindow外部可点击
        window.setFocusable(false); // 获取焦点
        init(context);
    }

    private void init(Context context) {
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(context);
        mLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        recyclerview.setLayoutManager(mLayoutManager);
        //如果可以确定每个item的高度是固定的，设置这个选项可以提高性能
        recyclerview.setHasFixedSize(true);
        recyclerview.setItemViewCacheSize(20);
        mapAdapter = new MapDeviceUsersListAdapter(context);
        recyclerview.setAdapter(mapAdapter);
    }
    public PopupWindow getPopupWindow(){
        return window;
    }

    public void initData(String type,List<Object> list,OnListItemClick onListItemClick){
        if(list!=null) {
            if("device".equals(type)&&list.size()>0){
                Collections.sort(list, new Comparator<Object>() {
                    @Override
                    public int compare(Object o1, Object o2) {
                        if (o1 instanceof DeviceInfo) {
                            if (TextUtils.isEmpty(((DeviceInfo) o1).getFriendname())) {
                                return -1;
                            }
                            return 1;
                        } else {
                            return 0;
                        }
                    }
                });
            }else if("friend".equals(type)){
                FriendsResponse.FriendlistBean friendlistBean = new FriendsResponse.FriendlistBean();
                friendlistBean.setFriendid(Common.getUserId());
                friendlistBean.setNickname(Common.getNikeName());
                friendlistBean.setHeadimg(Common.getUserHeadImage());
                list.add(0,friendlistBean);
            }
            mapAdapter.setOnListItemClick(onListItemClick);
            mapAdapter.setData(list);
        }
    }
}
