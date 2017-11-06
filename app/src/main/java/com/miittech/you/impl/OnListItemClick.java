package com.miittech.you.impl;

import com.miittech.you.net.response.DeviceResponse;

/**
 * Created by Administrator on 2017/10/26.
 */

public interface OnListItemClick<T>  {
    void onItemClick(T t);
}
