package com.miittech.you.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.miittech.you.R;
import com.miittech.you.impl.TypeSelectorChangeLisener;
import com.miittech.you.weight.TypeSelector;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

/**
 * Created by Administrator on 2017/9/14.
 */

public class EventsFragment extends Fragment {
    @BindView(R.id.typeSelector)
    TypeSelector typeSelector;
    Unbinder unbinder;

    // 布局管理器
    private FragmentManager fragmentManager;
    private EventLogFragment eventLogFragment;
    private ListFragment traceFragment;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_events, null);
        unbinder = ButterKnife.bind(this, view);
        return view;
    }


    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        typeSelector.setItemText("轨迹追踪","事件报告");
        fragmentManager = getChildFragmentManager();
        typeSelector.setTypeSelectorChangeLisener(new TypeSelectorChangeLisener() {
            @Override
            public void onTabSelectorChanged(int item) {
                FragmentTransaction trans = fragmentManager.beginTransaction();
                hideFrament(trans);
                setFragment(item,trans);
                trans.commitAllowingStateLoss();
            }
        });
        typeSelector.setSelectItem(0);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }
    /**
     * 设置Fragment
     * @param item
     * @param trans
     */
    private void setFragment(int item,FragmentTransaction trans) {
        switch (item) {
            case 0:
                if(traceFragment==null){
                    traceFragment = new ListFragment();
                    trans.add(R.id.id_fragment, traceFragment);
                }else{
                    trans.show(traceFragment);
                }
                break;
            case 1:
                if(eventLogFragment==null){
                    eventLogFragment = new EventLogFragment();
                    trans.add(R.id.id_fragment, eventLogFragment);
                }else{
                    trans.show(eventLogFragment);
                }
                break;
        }
    }
    /**
     * 隐藏所有的fragment(编程初始化状态)
     * @param trans
     */
    private void hideFrament(FragmentTransaction trans) {
        if(traceFragment!=null){
            trans.hide(traceFragment);
        }
        if(eventLogFragment!=null){
            trans.hide(eventLogFragment);
        }
    }
}
