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
import android.widget.ExpandableListView;
import android.widget.Toast;
import androidx.annotation.RequiresApi;
import java.util.ArrayList;
import java.util.Collections;
import io.treehouses.remote.Constants;
import io.treehouses.remote.Network.BluetoothChatService;
import io.treehouses.remote.R;
import io.treehouses.remote.adapter.NetworkListAdapter;
import io.treehouses.remote.adapter.ViewHolderVnc;
import io.treehouses.remote.bases.BaseFragment;
import io.treehouses.remote.pojo.NetworkListItem;

import static android.content.Context.WIFI_SERVICE;

public class SystemFragment extends BaseFragment {

    private Boolean network = true;
    private Boolean hostname = false;

    View view;

    public SystemFragment() { }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.activity_system_fragment, container, false);
        BluetoothChatService mChatService = listener.getChatService();
        mChatService.updateHandler(mHandler);

        ExpandableListView listView = view.findViewById(R.id.listView);
        NetworkListAdapter adapter = new NetworkListAdapter(getContext(), NetworkListItem.getSystemList(), mChatService);
        adapter.setListener(listener);

        listView.setOnGroupExpandListener(groupPosition -> {
            if (groupPosition == 0) {
                listener.sendMessage("treehouses networkmode info");
            }
        });

        listView.setAdapter(adapter);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        listener.sendMessage("hostname -I");
        hostname = true;
    }

    private void checkAndPrefilIp(String readMessage, ArrayList<Long> diff) {
        if (readMessage.contains(".") && hostname) {
            checkSubnet(readMessage, diff);
        }

        ipPrefil(readMessage, diff);
    }

    private void ipPrefil(String readMessage, ArrayList<Long> diff) {
        if (readMessage.contains("ip") && !readMessage.contains("ap0")) {
            if (network) {
                prefillIp(readMessage);
            } else {
                Toast.makeText(getContext(), "Warning: Your RPI may be in the wrong subnet", Toast.LENGTH_LONG).show();
                prefillIp(readMessage);
            }
        }
    }

    private void checkSubnet(String readMessage, ArrayList<Long> diff) {
        hostname = false;

        WifiManager wm = (WifiManager) getContext().getApplicationContext().getSystemService(WIFI_SERVICE);
        String deviceIp = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());

        long deviceIpAddress = ipToLong(deviceIp);

        convertIp(readMessage, deviceIpAddress, diff);
        network = isInNetwork(diff);
    }

    private void convertIp(String readMessage, long deviceIpAddress, ArrayList<Long> diff) {
        String[] array = readMessage.split(" ");

        for (String element : array) {
            //TODO: Need to convert IPv6 addresses to long; currently it is being skipped
            if (element.length() <= 15) {
                long ip = ipToLong(element);
                diff.add(deviceIpAddress - ip);
            }
        }
    }

    private long ipToLong(String ipAddress) {
        String[] ipAddressInArray = ipAddress.split("[.]");
        long result = 0;

        for (int i = 0; i < ipAddressInArray.length; i++) {
            int power = 3 - i;
            int ip = Integer.parseInt(ipAddressInArray[i]);
            result += ip * Math.pow(256, power);
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

    private void elementConditions(String element) {
        if (element.contains("ip")) {
            try {
                ViewHolderVnc.getEditTextIp().setText(element.trim().substring(4));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @SuppressLint("HandlerLeak")
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == Constants.MESSAGE_READ) {
                String readMessage = (String) msg.obj;
                ArrayList<Long> diff = new ArrayList<>();

                Log.d("TAG", "readMessage = " + readMessage);

                if (readMessage.trim().contains("true") || readMessage.trim().contains("false")) {
                    return;
                }

                checkAndPrefilIp(readMessage, diff);
            }
        }
    };
}
