package com.miittech.you.weight;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.miittech.you.R;
import com.miittech.you.impl.TitleBarOptions;

import butterknife.BindView;

/**
 * Created by Administrator on 2017/9/13.
 */

public class Titlebar extends RelativeLayout implements View.OnClickListener {
    private ImageView mBackBtn;
    private ImageView mAddBtn;
    private ImageView mSettingBtn;
    private TextView mCancel;
    private TextView mComplete;
    private TextView mTitle;
    private RelativeLayout rlBar;
    private TitleBarOptions titleBarOptions;

    public Titlebar(Context context) {
        super(context);
        init(context);
    }

    public Titlebar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public Titlebar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public void setTitleBarOptions(TitleBarOptions titleBarOptions) {
        this.titleBarOptions = titleBarOptions;
    }

    private void init(Context context) {
        View view = LayoutInflater.from(context).inflate(R.layout.layout_title_bar, this);
        rlBar = findViewById(R.id.rlBar);
        mBackBtn = (ImageView) view.findViewById(R.id.btn_back);
        mAddBtn = (ImageView) view.findViewById(R.id.btn_add);
        mSettingBtn = (ImageView) view.findViewById(R.id.btn_setting);
        mCancel = (TextView) view.findViewById(R.id.btn_cancel);
        mComplete = (TextView) view.findViewById(R.id.btn_complete);
        mTitle = (TextView) view.findViewById(R.id.title);
        mBackBtn.setOnClickListener(this);
        mAddBtn.setOnClickListener(this);
        mSettingBtn.setOnClickListener(this);
        mCancel.setOnClickListener(this);
        mComplete.setOnClickListener(this);
    }

    public void setTitle(String title) {
        if (mTitle != null) {
            mTitle.setText(title);
        }
    }

    public void setTopPadding(int padding) {
        rlBar.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, padding));
    }

    public Titlebar showBackOption() {
        mBackBtn.setVisibility(VISIBLE);
        return this;
    }

    public Titlebar showAddOption() {
        mAddBtn.setVisibility(VISIBLE);
        return this;
    }

    public Titlebar showSettingOption() {
        mSettingBtn.setVisibility(VISIBLE);
        return this;
    }

    public Titlebar showCancleOption() {
        mCancel.setVisibility(VISIBLE);
        return this;
    }

    public Titlebar showCompleteOption() {
        mComplete.setVisibility(VISIBLE);
        return this;
    }

    public Titlebar showCompleteOption(String text) {
        mComplete.setText(text);
        return showCompleteOption();
    }

    public Titlebar setAddIcon(int addIcon) {
        if (mAddBtn != null) {
            mAddBtn.setImageResource(addIcon);
        }
        return this;
    }

    public Titlebar setSettingIcon(int ic_friends_add) {
        if (mSettingBtn != null) {
            mSettingBtn.setImageResource(ic_friends_add);
        }
        return this;
    }


    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_back:
                if (titleBarOptions != null) {
                    titleBarOptions.onBack();
                }
                break;
            case R.id.btn_add:
                if (titleBarOptions != null) {
                    titleBarOptions.onAdd();
                }
                break;
            case R.id.btn_setting:
                if (titleBarOptions != null) {
                    titleBarOptions.onSetting();
                }
                break;
            case R.id.btn_cancel:
                if (titleBarOptions != null) {
                    titleBarOptions.onCancel();
                }
                break;
            case R.id.btn_complete:
                if (titleBarOptions != null) {
                    titleBarOptions.onComplete();
                }
                break;
        }
    }


}
