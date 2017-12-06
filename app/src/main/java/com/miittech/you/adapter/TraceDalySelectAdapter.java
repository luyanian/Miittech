package com.miittech.you.adapter;

import android.app.Activity;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.miittech.you.R;
import com.miittech.you.impl.OnListItemClick;
import com.ryon.constant.TimeConstants;
import com.ryon.mutils.TimeUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by Administrator on 2017/12/6.
 */

public class TraceDalySelectAdapter extends RecyclerView.Adapter {
    private Context context;
    private OnListItemClick onListItemClick;
    private RecyclerView recyclerview;
    List<Date> mlist = new ArrayList<>();
    List<TextView> textViews = new ArrayList<>();
    public TraceDalySelectAdapter(Context context, OnListItemClick onListItemClick) {
        this.context = context;
        this.onListItemClick = onListItemClick;
        textViews.clear();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.add(Calendar.MONTH, -1);
        while (calendar.getTime().getTime()<=Calendar.getInstance().getTime().getTime()){
            mlist.add(calendar.getTime());
            calendar.add(Calendar.DAY_OF_MONTH,+1);
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = View.inflate(context, R.layout.item_trace_detail_daly, null);
        ViewHolder viewHolder = new ViewHolder(view);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
        final ViewHolder viewHolder = (ViewHolder) holder;
        final Date date = mlist.get(position);
        textViews.add(viewHolder.tvDaly);
        viewHolder.tvWeek.setText(TimeUtils.getChineseWeek(date).replace("周",""));
        viewHolder.tvDaly.setText(TimeUtils.date2String(date,new SimpleDateFormat("dd")));
        viewHolder.tvDaly.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                unSelectAll();
                if(recyclerview!=null){
                    recyclerview.scrollToPosition(position);
                }
                viewHolder.tvDaly.setSelected(true);
                if(onListItemClick!=null){
                    onListItemClick.onItemClick(date);
                }
            }
        });
        if(position==mlist.size()-1){
            viewHolder.tvDaly.setSelected(true);
        }
    }

    @Override
    public int getItemCount() {
        return mlist.size();
    }

    public void setView(RecyclerView recyclerview) {
        this.recyclerview = recyclerview;
    }

    public void scrollToEnd() {
        if(recyclerview!=null){
            recyclerview.scrollToPosition(mlist.size()-1);
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.tv_week)
        TextView tvWeek;
        @BindView(R.id.tv_daly)
        TextView tvDaly;

        ViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }
    }
    private void unSelectAll() {
        for(TextView textView : textViews){
            if(textView!=null){
                textView.setSelected(false);
            }
        }
    }
}
