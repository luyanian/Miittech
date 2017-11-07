package com.miittech.you.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.miittech.you.R;
import com.miittech.you.impl.OnListItemClick;
import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by Administrator on 2017/10/19.
 */

public class SelectTimeAdapter extends RecyclerView.Adapter {

    private String[] mData;
    private Context activity;
    private OnListItemClick<String> onListItemClick;

    public SelectTimeAdapter(Context activity, String[] mData, OnListItemClick onDeviceItemClick) {
        this.activity = activity;
        this.mData = mData;
        this.onListItemClick = onDeviceItemClick;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, final int i) {
        View view = View.inflate(activity, R.layout.item_dialog_select_time, null);
        ViewHolder viewHolder = new ViewHolder(view);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, final int i) {
        ViewHolder holder = (ViewHolder) viewHolder;
        final String time = mData[i];
        holder.itemText.setText(time);
        holder.llItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(onListItemClick!=null){
                    onListItemClick.onItemClick(time);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return mData.length;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.ll_item)
        RelativeLayout llItem;
        @BindView(R.id.item_text)
        TextView itemText;

        ViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }
    }
}
