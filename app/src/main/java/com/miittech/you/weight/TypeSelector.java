package com.miittech.you.weight;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.miittech.you.R;
import com.miittech.you.impl.TypeSelectorChangeLisener;

/**
 * Created by Administrator on 2017/9/13.
 */

public class TypeSelector extends LinearLayout implements View.OnClickListener{
    private Context mContext;
    private TextView tvItem1;
    private TextView tvItem2;
    private Drawable drawableSelect;
    private Drawable drawableUnSelect;
    private TypeSelectorChangeLisener typeSelectorChangeLisener;
    private int selectItem = 0;
    private boolean conSelectEmail = true;
    public TypeSelector(Context context) {
        super(context);
        init(context);
    }

    public TypeSelector(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public TypeSelector(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }
    public void init(Context context){
        this.mContext = context;
        LayoutInflater.from(mContext).inflate(R.layout.selector_type,this);
        tvItem1 = (TextView)findViewById(R.id.tv_item1);
        tvItem2 = (TextView)findViewById(R.id.tv_item2);
        tvItem1.setOnClickListener(this);
        tvItem2.setOnClickListener(this);

        drawableSelect = getResources().getDrawable(R.drawable.ic_selecter_hr);
        drawableSelect.setBounds(0, 0, drawableSelect.getMinimumWidth(), drawableSelect.getMinimumHeight());

        drawableUnSelect = getResources().getDrawable(R.drawable.shap_trans);
        drawableUnSelect.setBounds(0, 0, drawableUnSelect.getMinimumWidth(), drawableUnSelect.getMinimumHeight());

    }
    public void setItemText(int text1Id,int text2Id){
        setItemText(getResources().getString(text1Id),getResources().getString(text2Id));
    }
    public void setItemText(String text1,String text2){
        if(tvItem1!=null){
            tvItem1.setText(text1);
        }
        if(tvItem2!=null){
            tvItem2.setText(text2);
        }
    }
    public void setTypeSelectorChangeLisener(TypeSelectorChangeLisener typeSelectorChangeLisener){
        this.typeSelectorChangeLisener = typeSelectorChangeLisener;
    }

    public void setSelectItem(int item){
        switch (item){
            case 0:
                onClick(tvItem1);
                break;
            case 1:
                onClick(tvItem2);
                break;
        }
    }
    public int getSelectItem(){
        return selectItem;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.tv_item1:
                this.selectItem = 0;
                tvItem2.setSelected(false);
                tvItem2.setCompoundDrawables(null, null, null, drawableUnSelect);
                tvItem1.setSelected(true);
                tvItem1.setCompoundDrawables(null, null, null, drawableSelect);
                break;
            case R.id.tv_item2:
                if(!conSelectEmail){
                    return;
                }
                this.selectItem = 1;
                tvItem1.setSelected(false);
                tvItem1.setCompoundDrawables(null, null, null, drawableUnSelect);
                tvItem2.setSelected(true);
                tvItem2.setCompoundDrawables(null, null, null, drawableSelect);
                break;
        }
        if(typeSelectorChangeLisener!=null){
            typeSelectorChangeLisener.onTabSelectorChanged(selectItem);
        }
    }

    public void disableSelectEmail() {
        this.conSelectEmail=false;
    }
}
