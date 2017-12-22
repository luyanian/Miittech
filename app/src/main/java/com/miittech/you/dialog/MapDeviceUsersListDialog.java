package com.miittech.you.dialog;
import android.app.Dialog;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import com.miittech.you.R;
import com.miittech.you.adapter.MapDeviceUsersListAdapter;
import com.miittech.you.common.Common;
import com.miittech.you.entity.DeviceInfo;
import com.miittech.you.impl.OnListItemClick;
import com.miittech.you.net.response.DeviceListResponse;
import com.miittech.you.net.response.FriendsResponse;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by Administrator on 2017/11/8.
 */

public class MapDeviceUsersListDialog<T> extends Dialog {

    @BindView(R.id.recyclerview)
    RecyclerView recyclerview;
    private Context context;
    private MapDeviceUsersListAdapter mapAdapter;

    public MapDeviceUsersListDialog(@NonNull Context context) {
        super(context, R.style.DialogTransStyle);
        setContentView(R.layout.dialog_map_device_users_list);
        ButterKnife.bind(this);
        this.context = context;
        this.setCancelable(true);
        this.setCanceledOnTouchOutside(true);
        Window window = this.getWindow();
        window.getDecorView().setPadding(20, 20, 20, 20);
        WindowManager.LayoutParams attr = window.getAttributes();
        if (attr != null) {
            attr.height = WindowManager.LayoutParams.WRAP_CONTENT;
            attr.width = WindowManager.LayoutParams.WRAP_CONTENT;
            attr.gravity = Gravity.BOTTOM|Gravity.LEFT;
            attr.y=400;
            window.setAttributes(attr);
        }
        init(context);
    }

    private void init(Context context) {
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(context);
        mLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        recyclerview.setLayoutManager(mLayoutManager);
        //如果可以确定每个item的高度是固定的，设置这个选项可以提高性能
        recyclerview.setHasFixedSize(true);
        mapAdapter = new MapDeviceUsersListAdapter(this.context);
        recyclerview.setAdapter(mapAdapter);
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
