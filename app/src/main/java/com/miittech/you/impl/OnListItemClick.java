package com.miittech.you.impl;

import android.view.View;

import com.baidu.mapapi.map.BitmapDescriptor;

/**
 * Created by Administrator on 2017/10/26.
 */

public class OnListItemClick<T>  {
    public void onItemClick(T t){};
    public void onItemRemoved(T t){};
    public void onItemFlagClick(T t){};
    public void onItemClick(T t,String flag){};
    public void onItemClick(BitmapDescriptor bitmapDescriptor, T t){};
}
