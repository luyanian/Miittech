package com.miittech.you.adapter;

import android.app.Activity;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.miittech.you.R;
import com.miittech.you.impl.OnListItemClick;
import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by Administrator on 2017/10/19.
 */

public class SelectAdapter extends RecyclerView.Adapter {

    private String[] mData;
    private Context activity;
    private OnListItemClick<String> onListItemClick;

    public SelectAdapter(Context activity, String[] mData, OnListItemClick onDeviceItemClick) {
        this.activity = activity;
        this.mData = mData;
        this.onListItemClick = onDeviceItemClick;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, final int i) {
        View view = View.inflate(activity, R.layout.item_dialog_select, null);
        ViewHolder viewHolder = new ViewHolder(view);
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(onListItemClick!=null){
                    onListItemClick.onItemClick(mData[i]);
                }
            }
        });
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int i) {
        ViewHolder holder = (ViewHolder) viewHolder;
        holder.itemText.setText(mData[i]);
    }

    @Override
    public int getItemCount() {
        return mData.length;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.item_text)
        TextView itemText;

        ViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }
    }
}
