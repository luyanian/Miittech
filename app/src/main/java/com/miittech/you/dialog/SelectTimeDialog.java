package com.miittech.you.dialog;

import android.app.Dialog;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.miittech.you.R;
import com.miittech.you.adapter.SelectTimeAdapter;
import com.miittech.you.impl.OnListItemClick;
import com.ryon.mutils.ScreenUtils;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by Administrator on 2017/11/6.
 */

public class SelectTimeDialog extends Dialog{
    @BindView(R.id.title)
    TextView tvTitle;
    @BindView(R.id.recyclerview)
    RecyclerView recyclerview;

    private SelectTimeAdapter selectAdapter;
    private OnListItemClick<String> onListItemClick;
    private Context context;

    public SelectTimeDialog(@NonNull Context context) {
        super(context, R.style.DialogStyle);
        setContentView(R.layout.dialog_select_time);
        ButterKnife.bind(this);
        this.context = context;
        Window window = this.getWindow();
        window.getDecorView().setPadding(20, 0, 20, 20);
        WindowManager.LayoutParams attr = window.getAttributes();
        if (attr != null) {
            attr.height = ScreenUtils.getScreenHeight()*2/3;
            attr.width = ScreenUtils.getScreenWidth()*6/7;
            attr.gravity = Gravity.CENTER;
            window.setAttributes(attr);
        }
        init(context);
    }

    private void init(Context context) {
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(context);
        recyclerview.setLayoutManager(mLayoutManager);
        //如果可以确定每个item的高度是固定的，设置这个选项可以提高性能
        recyclerview.setHasFixedSize(true);
    }

    public void setTitle(String title){
        tvTitle.setText(title);
    }
    public void setOnListItemClick(OnListItemClick<String> onListItemClick){
        this.onListItemClick = onListItemClick;
    }

    public void initData(){
        String[] mData = context.getResources().getStringArray(R.array.repeat_time);
        //创建并设置Adapter
        selectAdapter = new SelectTimeAdapter(this.context,mData,this.onListItemClick);
        recyclerview.setAdapter(selectAdapter);
    }

}
