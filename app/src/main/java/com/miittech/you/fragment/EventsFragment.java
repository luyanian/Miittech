package com.miittech.you.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
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

    private FragmentTransaction transaction;
    private EventLogFragment eventLogFragment;
    private TraceFragment traceFragment;

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

        typeSelector.setItemText("轨迹追中","事件报告");
        transaction = getChildFragmentManager().beginTransaction();
        typeSelector.setTypeSelectorChangeLisener(new TypeSelectorChangeLisener() {
            @Override
            public void onTabSelectorChanged(int item) {
                switchFragment(item);
            }
        });
        switchFragment(1);
        transaction.commit();
    }

    private void switchFragment(int item) {
        if(traceFragment!=null){
            transaction.hide(traceFragment);
        }
        if(eventLogFragment!=null){
            transaction.hide(eventLogFragment);
        }

        if(item==0){
            if(traceFragment==null){
                traceFragment = new TraceFragment();
                transaction.add(R.id.id_fragment, traceFragment);
            }else{
                transaction.show(traceFragment);;
            }
        }else{
            if(eventLogFragment==null){
                eventLogFragment = new EventLogFragment();
                transaction.add(R.id.id_fragment,eventLogFragment);
            }else{
                transaction.show(eventLogFragment);
            }
        }
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }
}
