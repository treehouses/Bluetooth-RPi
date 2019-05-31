package io.treehouses.remote.Fragments;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import io.treehouses.remote.InitialActivity;
import io.treehouses.remote.Constants;
import io.treehouses.remote.Network.BluetoothChatService;
import io.treehouses.remote.R;
import io.treehouses.remote.bases.BaseFragment;
import io.treehouses.remote.callback.SetDisconnect;

import static io.treehouses.remote.Constants.REQUEST_ENABLE_BT;

public class HomeFragment extends BaseFragment implements SetDisconnect {
    private BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private BluetoothChatService mChatService = null;
    private Button connectRpi, getStarted;
    private Boolean connectionState = false;
    View view;

    public HomeFragment(){}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.activity_home_fragment, container, false);
        mChatService = listener.getChatService();
        connectRpi = view.findViewById(R.id.btn_connect);
        getStarted = view.findViewById(R.id.btn_getStarted);

        checkConnectionState();

        connectRpiListener();
        getStartedListener();

        return view;
    }

    private void getStartedListener() {
        getStarted.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                InitialActivity.getInstance().openCallFragment(new AboutFragment());
            }
        });
    }

    public void connectRpiListener() {
        connectRpi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (connectionState) {
                    RPIDialogFragment.getInstance().bluetoothCheck("unregister");
                    mChatService.stop();
                    connectionState = false;
                    checkConnectionState();
                    return;
                }

                if (mBluetoothAdapter.getState() == BluetoothAdapter.STATE_OFF) {
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                    Toast.makeText(getContext(), "Bluetooth is disabled", Toast.LENGTH_LONG).show();
                    return;
                } else if (mBluetoothAdapter.getState() == BluetoothAdapter.STATE_ON) {
                    showRPIDialog();
                }
            }
        });
    }

    public void checkConnectionState() {
        mChatService = listener.getChatService();
        if (mChatService.getState() == Constants.STATE_CONNECTED) {
            connectRpi.setText("Disconnect");
            connectionState = true;
        } else {
            connectRpi.setText("Connect to RPI");
            connectionState = false;
        }
    }

    private void showRPIDialog(){
        androidx.fragment.app.DialogFragment dialogFrag =  RPIDialogFragment.newInstance(123);
        ((RPIDialogFragment) dialogFrag).setCheckConnectionState(this);
        dialogFrag.setTargetFragment(this, Constants.REQUEST_DIALOG_FRAGMENT_HOTSPOT);
        dialogFrag.show(getFragmentManager().beginTransaction(),"rpiDialog");
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(resultCode == Activity.RESULT_OK){
            //Bundle bundle = data.getExtras();
            //String type = bundle.getString("type");
            //Log.e("ON ACTIVITY RESULT","Request Code: "+requestCode+" ;; Result Code: "+resultCode+" ;; Intent: "+bundle+" ;; Type: "+bundle.getString("type"));


        }
    }
}
