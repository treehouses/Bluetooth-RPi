package io.treehouses.remote.Fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Toast;

import androidx.viewpager.widget.ViewPager;

import java.util.ArrayList;
import java.util.Collections;

import io.treehouses.remote.Constants;
import io.treehouses.remote.R;
import io.treehouses.remote.adapter.ServiceCardAdapter;
import io.treehouses.remote.adapter.ServicesListAdapter;
import io.treehouses.remote.bases.BaseServicesFragment;
import io.treehouses.remote.callback.ServiceAction;
import io.treehouses.remote.databinding.ActivityServicesDetailsBinding;
import io.treehouses.remote.databinding.DialogChooseUrlBinding;
import io.treehouses.remote.pojo.ServiceInfo;

public class ServicesDetailsFragment extends BaseServicesFragment implements AdapterView.OnItemSelectedListener, ViewPager.OnPageChangeListener, ServiceAction {

    private boolean received = false;
    private boolean wait = false;

    private ServicesListAdapter spinnerAdapter;
    private ArrayList<ServiceInfo> services;

    private ServiceInfo selected;

    private ServiceCardAdapter serviceCardAdapter;

    private boolean scrolled = false;

    private ActivityServicesDetailsBinding binding;

    ServicesDetailsFragment(ArrayList<ServiceInfo> serviceInfos){ this.services = serviceInfos; }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        mChatService = listener.getChatService();
        binding = ActivityServicesDetailsBinding.inflate(inflater, container, false);

        spinnerAdapter = new ServicesListAdapter(getContext(), services, getResources().getColor(R.color.md_grey_600));
        binding.pickService.setAdapter(spinnerAdapter);
        binding.pickService.setSelection(1);
        binding.pickService.setOnItemSelectedListener(this);

        serviceCardAdapter = new ServiceCardAdapter(getChildFragmentManager(), services);
        binding.servicesCards.setAdapter(serviceCardAdapter);

        binding.servicesCards.addOnPageChangeListener(this);

        return binding.getRoot();
    }

    final Handler handlerDetails = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.MESSAGE_READ:
                    String output = (String) msg.obj;
                    moreActions(output);
                    break;
            }
        }
    };

    private void matchOutput(String s) {
        selected =((ServiceInfo) binding.pickService.getSelectedItem());
        Log.d("Entered", "matchOutput: "+s);
        if (s.contains("started")) {
            selected.serviceStatus = ServiceInfo.SERVICE_RUNNING;
        }
        else if (s.contains("stopped and removed")) {
            selected.serviceStatus = ServiceInfo.SERVICE_AVAILABLE;
            Log.d("STOP", "matchOutput: ");
        }
        else if (s.contains("stopped") || s.contains("installed")) {
            selected.serviceStatus = ServiceInfo.SERVICE_INSTALLED;
        }
        else {
            return;
        }
        Collections.sort(services);
        serviceCardAdapter.notifyDataSetChanged();
        spinnerAdapter.notifyDataSetChanged();
        setScreenState(true);
        wait = false;
        goToSelected();
    }

    private void moreActions(String output) {
        if (wait) {
            matchOutput(output.trim());
        }
        else if (isLocalUrl(output, received)) {
            received = true;
            openLocalURL(output.trim());
            binding.progressBar.setVisibility(View.GONE);
        }
        else if (isTorURL(output, received)) {
            received = true;
            openTorURL(output.trim());
            binding.progressBar.setVisibility(View.GONE);
        }
        else if (output.contains("service autorun set")) {
            setScreenState(true);
            Toast.makeText(getContext(), "Switched autorun", Toast.LENGTH_SHORT).show();
        }
        else if (output.toLowerCase().contains("error")) {
            setScreenState(true);
            Toast.makeText(getContext(), "An Error occurred", Toast.LENGTH_SHORT).show();
        }

    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (!scrolled) {
            int statusCode = services.get(position).serviceStatus;
            if (statusCode == ServiceInfo.SERVICE_HEADER_AVAILABLE || statusCode == ServiceInfo.SERVICE_HEADER_INSTALLED) return;

            int count = countHeadersBefore(position);
            binding.servicesCards.setCurrentItem(position-count);
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) { }


    void setSelected(ServiceInfo s) {
        Log.d("SELECTED", "setSelected: " + s.name);
        selected = s;
    }

    private void goToSelected() {
        if (selected != null && binding.pickService != null) {
            int pos = inServiceList(selected.name, services);
            int count = countHeadersBefore(pos);
            binding.servicesCards.setCurrentItem(pos-count);
            binding.pickService.setSelection(pos);
        }
    }
    @Override
    public void onResume() {
        super.onResume();
        goToSelected();
    }

    @Override
    public void onPause() {
        super.onPause();
        selected = (ServiceInfo) binding.pickService.getSelectedItem();
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) { }

    @Override
    public void onPageSelected(int position) {
        Log.d("SELECTED", "onPageSelected: ");
        scrolled = true;
        int pos = position+countHeadersBefore(position+1);
        binding.pickService.setSelection(pos);
        scrolled = false;
    }

    @Override
    public void onPageScrollStateChanged(int state) { }

    private int countHeadersBefore(int position) {
        int count = 0;
        for (int i = 0; i <= position; i++) { if (services.get(i).isHeader()) count++; }
        return count;
    }
    private void showDeleteDialog(ServiceInfo selected) {
        new AlertDialog.Builder(getContext())
                .setTitle("Delete " + selected.name + "?")
                .setMessage("Are you sure you would like to delete this service? All of its data will be lost and the service must be reinstalled.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    performService("Uninstalling", "treehouses services " + selected.name + " cleanup\n", selected.name);
                    wait = true;
                    setScreenState(false);
                }).setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss()).create().show();
    }

    private void onInstall(ServiceInfo selected) {
        if (selected.serviceStatus == ServiceInfo.SERVICE_AVAILABLE) {
            performService("Installing", "treehouses services " + selected.name + " install\n", selected.name);
            wait = true;
            setScreenState(false);
        }
        else if (installedOrRunning(selected)) { showDeleteDialog(selected); }
    }

    private void onStart(ServiceInfo selected) {
        if (selected.serviceStatus == ServiceInfo.SERVICE_INSTALLED) {
            performService("Starting", "treehouses services " + selected.name + " up\n", selected.name);
        } else if (selected.serviceStatus == ServiceInfo.SERVICE_RUNNING) {
            performService("Stopping", "treehouses services " + selected.name + " stop\n", selected.name);
        }
    }

    private void setOnClick(View v, String command, AlertDialog alertDialog) {
        v.setOnClickListener(v1 -> {
            writeToRPI(command);
            alertDialog.dismiss();
            binding.progressBar.setVisibility(View.VISIBLE);
        });
    }

    private void onLink(ServiceInfo selected) {
        //reqUrls();
        DialogChooseUrlBinding chooseBind = DialogChooseUrlBinding.inflate(getLayoutInflater());
        AlertDialog alertDialog = new AlertDialog.Builder(getContext()).setView(chooseBind.getRoot()).setTitle("Select URL type").create();

        setOnClick(chooseBind.localButton, "treehouses services " + selected.name + " url local \n", alertDialog);
        setOnClick(chooseBind.torButton, "treehouses services " + selected.name + " url tor \n", alertDialog);

        alertDialog.show();
    }

    private void setScreenState(boolean state) {
        binding.servicesCards.setPagingEnabled(state);
        binding.pickService.setEnabled(state);
        if (state) binding.progressBar.setVisibility(View.GONE);
        else binding.progressBar.setVisibility(View.VISIBLE);
    }

    @Override
    public void onClickInstall(ServiceInfo s) { onInstall(s); }

    @Override
    public void onClickStart(ServiceInfo s) {
        onStart(s);
        wait = true;
        setScreenState(false);
    }

    @Override
    public void onClickLink(ServiceInfo s) {
        onLink(s);
        received = false;
    }

    @Override
    public void onClickAutorun(ServiceInfo s, boolean newAutoRun) {
        setScreenState(false);
        if (newAutoRun) listener.sendMessage("treehouses services "+s.name + " autorun true\n");
        else listener.sendMessage("treehouses services "+s.name + " autorun false\n");

        Toast.makeText(getContext(), "Switching autorun status to "+ newAutoRun, Toast.LENGTH_SHORT).show();
    }
}

