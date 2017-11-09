package com.miittech.you.dialog;

import android.app.Dialog;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;

import com.miittech.you.R;
import com.miittech.you.adapter.MapDeviceUsersListAdapter;
import com.miittech.you.adapter.SelectTimeAdapter;
import com.miittech.you.impl.OnListItemClick;
import com.miittech.you.net.response.FriendsResponse;
import com.ryon.mutils.ScreenUtils;

import java.util.ArrayList;
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
    private OnListItemClick onListItemClick;
    private List<T> mData = new ArrayList<>();

    public MapDeviceUsersListDialog(@NonNull Context context) {
        super(context, R.style.DialogStyle);
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
        mapAdapter = new MapDeviceUsersListAdapter(this.context,mData,this.onListItemClick);
        recyclerview.setAdapter(mapAdapter);
    }

    public void setOnListItemClick(OnListItemClick onListItemClick){
        this.onListItemClick = onListItemClick;
    }
    public void initData(List<T> list){
        mData.clear();
        mData.addAll(list);
        mapAdapter.notifyDataSetChanged();
    }
}
