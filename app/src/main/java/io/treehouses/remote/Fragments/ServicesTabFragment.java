package io.treehouses.remote.Fragments;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;

import io.treehouses.remote.Constants;
import io.treehouses.remote.R;
import io.treehouses.remote.adapter.ServicesListAdapter;
import io.treehouses.remote.bases.BaseServicesFragment;
import io.treehouses.remote.callback.ServicesListener;
import io.treehouses.remote.pojo.ServiceInfo;

public class ServicesTabFragment extends BaseServicesFragment implements AdapterView.OnItemClickListener {

    private View view;
    public ArrayList<ServiceInfo> services;
    private ServicesListAdapter adapter;
    private ListView listView;
    private ServicesListener servicesListener;


    public ServicesTabFragment(ArrayList<ServiceInfo> serviceInfos) {
        this.services = serviceInfos;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mChatService = listener.getChatService();

        view = inflater.inflate(R.layout.activity_services_tab_fragment, container, false);

        listView = view.findViewById(R.id.listView);
        adapter = new ServicesListAdapter(getActivity(), services, getResources().getColor(R.color.bg_white));
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(this);

        return view;
    }


//    public final Handler handlerOverview = new Handler() {
//        @Override
//        public void handleMessage(Message msg) {
//
//            switch (msg.what) {
//                case Constants.MESSAGE_READ:
//                    String output = (String) msg.obj;
//                    performAction(output, progressBar, services, versionIntNumber, adapter);
//                    break;
//                case Constants.MESSAGE_WRITE:
//                    String write_msg = new String((byte[]) msg.obj);
//                    Log.d("WRITE", write_msg);
//                    break;
//
//            }
//        }
//    };


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            servicesListener = (ServicesListener) getParentFragment();
        }
        catch (ClassCastException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        ServiceInfo selected = services.get(position);
        if (servicesListener != null) servicesListener.onClick(selected);
    }


}