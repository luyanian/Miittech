package com.miittech.you.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.baidu.mapapi.search.core.PoiInfo;
import com.baidu.mapapi.search.poi.PoiResult;
import com.miittech.you.R;
import com.miittech.you.impl.OnListItemClick;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by Administrator on 2017/11/10.
 */

public class PoiResultAdapter extends RecyclerView.Adapter {
    private Context mContext;
    private OnListItemClick onListItemClick;
    private List<PoiInfo> poiInfos = new ArrayList<>();

    public PoiResultAdapter(Context mContext) {
        this.mContext = mContext;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = View.inflate(mContext, R.layout.item_poiresult, null);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        ViewHolder viewHolder = (ViewHolder) holder;
        final PoiInfo poiInfo = poiInfos.get(position);
        viewHolder.itemText.setText(poiInfo.address);
        viewHolder.llItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(onListItemClick!=null){
                    onListItemClick.onItemClick(poiInfo);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return poiInfos.size();
    }

    public void setOnItemClick(OnListItemClick onListItemClick) {
        this.onListItemClick = onListItemClick;
    }

    public void setPoiResult(PoiResult result) {
        this.poiInfos.clear();
        if(result!=null||result.getAllPoi()==null) {
            this.poiInfos.addAll(result.getAllPoi());
        }
        this.notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.item_text)
        TextView itemText;
        @BindView(R.id.ll_item)
        RelativeLayout llItem;

        ViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }
    }
}
