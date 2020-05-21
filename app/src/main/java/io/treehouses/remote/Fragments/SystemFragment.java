package io.treehouses.remote.Fragments;

import android.annotation.SuppressLint;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.format.Formatter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.Collections;

import io.treehouses.remote.Constants;
import io.treehouses.remote.Network.BluetoothChatService;
import io.treehouses.remote.adapter.NetworkListAdapter;
import io.treehouses.remote.adapter.ViewHolderTether;
import io.treehouses.remote.adapter.ViewHolderVnc;
import io.treehouses.remote.bases.BaseFragment;
import io.treehouses.remote.databinding.ActivitySystemFragmentBinding;
import io.treehouses.remote.pojo.NetworkListItem;

import static android.content.Context.WIFI_SERVICE;

public class SystemFragment extends BaseFragment {

    private Boolean network = true;
    private Boolean hostname = false;
    private Boolean tether = false;

    private boolean retry= false;

    ActivitySystemFragmentBinding bind;

    public SystemFragment() {
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        bind = ActivitySystemFragmentBinding.inflate(inflater, container, false);
        BluetoothChatService mChatService = listener.getChatService();
        mChatService.updateHandler(mHandler);

        NetworkListAdapter adapter = new NetworkListAdapter(getContext(), NetworkListItem.getSystemList(), mChatService);
        adapter.setListener(listener);

        bind.listView.setOnGroupExpandListener(groupPosition -> {
            if (groupPosition == 1) {
                listener.sendMessage("treehouses networkmode info\n");
                tether = true;
                return;
            }
            else if(groupPosition == 2){
                Log.d("3", "onCreateView: ");
            }
            listener.sendMessage("treehouses networkmode info\n");
        });


        bind.listView.setAdapter(adapter);
        return bind.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        listener.sendMessage("hostname -I\n");
        hostname = true;
    }

    private void checkAndPrefilIp(String readMessage, ArrayList<Long> diff) {
        if (readMessage.contains(".") && hostname && !readMessage.contains("essid")) {
            checkSubnet(readMessage, diff);
        }

        ipPrefil(readMessage);
    }

    private void ipPrefil(String readMessage) {
        if (network) {
            checkIfHotspot(readMessage);
        }
        else if (!retry){
            Toast.makeText(getContext(), "Warning: Your RPI may be in the wrong subnet", Toast.LENGTH_LONG).show();
            prefillIp(readMessage);
        }

    }

    private void checkIfHotspot(String readMessage) {
        if (readMessage.contains("ip") && !readMessage.contains("ap0")) {
            prefillIp(readMessage);
        } else if (readMessage.contains("ap0")) {
            prefillHotspot(readMessage);
        }
    }

    private void checkSubnet(String readMessage, ArrayList<Long> diff) {
        hostname = false;

        WifiManager wm = (WifiManager) getContext().getApplicationContext().getSystemService(WIFI_SERVICE);
        String deviceIp = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());

        long deviceIpAddress = ipToLong(deviceIp);

        if (!convertIp(readMessage, deviceIpAddress, diff)) {
            listener.sendMessage("treehouses networkmode info\n");
            retry = true;
        }
        else{
            retry = false;
            network = isInNetwork(diff);
        }
    }

    private boolean checkElement(String s) {
        return s.length() <= 15 && !s.contains(":");
    }

    private boolean convertIp(String readMessage, long deviceIpAddress, ArrayList<Long> diff) {
        String[] array = readMessage.split(" ");

        for (String element : array) {
            //TODO: Need to convert IPv6 addresses to long; currently it is being skipped
            long ip = -1;
            if (checkElement(element)) {
                ip = ipToLong(element);
                diff.add(deviceIpAddress - ip);
            }
            if (ip == -1) {
                return false;
            }
        }
        return true;
    }

    private long ipToLong(String ipAddress) {
        String[] ipAddressInArray = ipAddress.split("[.]");
        long result = 0;

        for (int i = 0; i < ipAddressInArray.length; i++) {
            int power = 3 - i;

            try {
                int ip = Integer.parseInt(ipAddressInArray[i]);
                result += ip * Math.pow(256, power);

            } catch (NumberFormatException e) {
                return -1;
            }
        }
        return result;
    }

    private boolean isInNetwork(ArrayList<Long> diff) {
        Collections.sort(diff);
        return diff.get(0) <= 256;
    }

    private void prefillIp(String readMessage) {
        String[] array = readMessage.split(",");

        for (String element : array) {
            elementConditions(element);
        }
    }

    private void prefillHotspot(String readMessage) {
        String[] array = readMessage.split(",");

        for (String element : array) {
            elementConditions(element);
            if (element.contains("essid") && tether) {
                tether = false;
                ViewHolderTether.getEditTextSSID().setText(element.substring(12).trim());
            }
        }
    }

    private void elementConditions(String element) {
        if (element.contains("ip")) {
            try {
                ViewHolderVnc.getEditTextIp().setText(element.substring(4).trim());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void vncToast(String readMessage) {
        if (readMessage.contains("Success: the vnc service has been started")) {
            Toast.makeText(getContext(), "VNC enabled", Toast.LENGTH_LONG).show();
        } else if (readMessage.contains("Success: the vnc service has been stopped")) {
            Toast.makeText(getContext(), "VNC disabled", Toast.LENGTH_LONG).show();
        }
    }

    @SuppressLint("HandlerLeak")
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == Constants.MESSAGE_READ) {
                String readMessage = msg.obj.toString().trim();
                ArrayList<Long> diff = new ArrayList<>();

                readMessageConditions(readMessage);

                Log.d("TAG", "readMessage = " + readMessage);

                vncToast(readMessage);
                checkAndPrefilIp(readMessage, diff);
            }
        }
    };

    private void readMessageConditions(String readMessage) {
        if (readMessage.contains("true") || readMessage.contains("false")) {
            return;
        }

        if (readMessage.equals("password network") || readMessage.equals("open wifi network")) {
            Toast.makeText(getContext(), "Connected", Toast.LENGTH_LONG).show();
        }
    }
}
