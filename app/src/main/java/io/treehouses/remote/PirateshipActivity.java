package io.treehouses.remote;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Application;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Lalitha S Oruganty on 3/14/2018.
 */

public class PirateshipActivity extends Activity  {

    private static final String TAG ="pirateship" ;
    static String currentStatus = "not connected";

    // Layout Views
    private ListView mConversationView;
    private EditText mOutEditText;
    private Button pibutton;
    private ProgressDialog mProgressDialog;

    /**
     * Name of the connected device
     */
    private String mConnectedDeviceName = null;

    /**
     * Array adapter for the conversation thread
     */
    private ArrayAdapter<String> mConversationArrayAdapter;

    /**
     * String buffer for outgoing messages
     */
    private StringBuffer mOutStringBuffer;

    /**
     * Local Bluetooth adapter
     */
    private BluetoothAdapter mBluetoothAdapter = null;

    /**
     * Member object for the chat services
     */
    private BluetoothChatService mChatService = null;

    private static boolean isRead = false;
    private static boolean isCountdown = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActionBar().setDisplayShowHomeEnabled(true);
        getActionBar().setLogo(R.mipmap.ic_launcher);
        getActionBar().setDisplayUseLogoEnabled(true);
        setContentView(R.layout.pirateship_layout);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Bundle mBundle = getIntent().getExtras();
        mChatService = (BluetoothChatService) mBundle.getSerializable("mChatService");
        Log.d(TAG, "mChatService's state in ChatFragment: " + mChatService.getState());
        mChatService.setHandler(mHandler);

        pibutton = (Button)findViewById(R.id.dpi);
        mConversationView = (ListView)findViewById(R.id.pview);
        pibutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String command = "pirateship detectrpi";
                sendMessage(command);
            }
        });

        mOutEditText = (EditText) findViewById(R.id.edit_text_out);

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            this.finish();
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, Constants.REQUEST_ENABLE_BT);
            // Otherwise, setup the chat session
        } else {setupChat();}
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(isCountdown){
            mHandler.removeCallbacks(watchDogTimeOut);
        }
        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mChatService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mChatService.getState() == BluetoothChatService.STATE_NONE) {
                // Start the Bluetooth chat services
                mChatService.start();
            }
        }
    }

    /**
     * Set up the UI and background operations for chat.
     */
    private void setupChat() {
        Log.d(TAG, "setupChat()");

        // Initialize the array adapter for the conversation thread
        mConversationArrayAdapter = new ArrayAdapter<String>(this,R.layout.message){
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView tView = (TextView) view.findViewById(R.id.listItem);
                if(isRead){
                    tView.setTextColor(Color.BLUE);
                }else{
                    tView.setTextColor(Color.RED);
                }
                return view;
            }
        };

        mConversationView.setAdapter(mConversationArrayAdapter);

        // Initialize the compose field with a listener for the return key
        mOutEditText.setOnEditorActionListener(mWriteListener);

        // Initialize the buffer for outgoing messages
        mOutStringBuffer = new StringBuffer("");

        //get spinner
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setTitle(R.string.progress_dialog_title);
        mProgressDialog.setMessage(getString(R.string.progress_dialog_message));
        mProgressDialog.setCancelable(false); // disable dismiss by tapping outside of the dialog

    }

    private void ensureDiscoverable() {
        if (mBluetoothAdapter.getScanMode() !=
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }

    /**
     * Sends a message.
     *
     * @param message A string of text to send.
     */
    private void sendMessage(String message) {
        // Check that we're actually connected before trying anything
        if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
            byte[] send = message.getBytes();
            mChatService.write(send);

            // Reset out string buffer to zero and clear the edit text field
            mOutStringBuffer.setLength(0);
            mOutEditText.setText(mOutStringBuffer);
        }

    }

    /*
    private void sendMessage(String SSID, String PWD) {
        // Check that we're actually connected before trying anything
        if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }

        // Check that there's actually something to send
        if (SSID.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
            JSONObject mJson = new JSONObject();
            try {
                mJson.put("SSID",SSID);
                mJson.put("PWD",PWD);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            byte[] send = mJson.toString().getBytes();
            mChatService.write(send);

            // Reset out string buffer to zero and clear the edit text field
            mOutStringBuffer.setLength(0);
            mOutEditText.setText(mOutStringBuffer);
        }
    }
    */

    /**
     * The action listener for the EditText widget, to listen for the return key
     */
    private TextView.OnEditorActionListener mWriteListener
            = new TextView.OnEditorActionListener() {
        public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
            // If the action is a key-up event on the return key, send the message
            if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_UP) {
                String message = view.getText().toString();
                sendMessage(message);
            }
            return true;
        }
    };

    /**
     * Updates the status on the action bar.
     *
     * @param resId a string resource ID
     */
    private void setStatus(int resId) {
        if (null == this) {
            return;
        }
        final ActionBar actionBar = this.getActionBar();
        if (null == actionBar) {
            return;
        }
        Log.d(TAG, "actionBar.setSubtitle(resId) = " + resId );
        currentStatus = getString(resId);
        actionBar.setSubtitle(resId);

    }

    /**
     * Updates the status on the action bar.
     *
     * @param subTitle status
     */
    private void setStatus(CharSequence subTitle) {
        if (null == this) {
            return;
        }
        final ActionBar actionBar = this.getActionBar();
        if (null == actionBar) {
            return;
        }
        Log.d(TAG, "actionBar.setSubtitle(subTitle) = " + subTitle );
        currentStatus = subTitle.toString();
        actionBar.setSubtitle(subTitle);
    }

    private final Runnable watchDogTimeOut = new Runnable() {
        @Override
        public void run() {
            isCountdown = false;
            //time out
            if(mProgressDialog.isShowing()){
                mProgressDialog.dismiss();
                Toast.makeText(getApplicationContext(),"No response from RPi",Toast.LENGTH_LONG).show();
            }
        }
    };

    /**
     * The Handler that gets information back from the BluetoothChatService
     */
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Application activity = getApplication();
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothChatService.STATE_CONNECTED:
                            setStatus(getString(R.string.title_connected_to, mConnectedDeviceName));
                            mConversationArrayAdapter.clear();
                            break;
                        case BluetoothChatService.STATE_CONNECTING:
                            setStatus(R.string.title_connecting);
                            break;
                        case BluetoothChatService.STATE_LISTEN:
                        case BluetoothChatService.STATE_NONE:
                            setStatus(R.string.title_not_connected);
                            break;
                    }
                    break;
                case Constants.MESSAGE_WRITE:
                    isRead = false;
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                    Log.d(TAG, "writeMessage = " + writeMessage);
                    mConversationArrayAdapter.add("Command:  " + writeMessage);
                    break;
                case Constants.MESSAGE_READ:
                    isRead = true;
//                    byte[] readBuf = (byte[]) msg.obj;
//                     construct a string from the valid bytes in the buffer
//                    String readMessage = new String(readBuf, 0, msg.arg1);
//                    String readMessage = new String(readBuf);
                    String readMessage = (String)msg.obj;
                    Log.d(TAG, "readMessage = " + readMessage);
                    //TODO: if message is json -> callback from RPi
                    if(isJson(readMessage)){
                        handleCallback(readMessage);
                    }else{
                        if(isCountdown){
                            mHandler.removeCallbacks(watchDogTimeOut);
                            isCountdown = false;
                        }
                        if(mProgressDialog.isShowing()){
                            mProgressDialog.dismiss();
                            Toast.makeText(activity, R.string.config_alreadyConfig, Toast.LENGTH_SHORT).show();
                        }
                        //remove the space at the very end of the readMessage -> eliminate space between items
                        readMessage = readMessage.substring(0,readMessage.length()-1);
                        //mConversationArrayAdapter.add(mConnectedDeviceName + ":  " + readMessage);
                        mConversationArrayAdapter.add(readMessage);
                    }
                    break;
                case Constants.MESSAGE_TOAST:
                    if (null != activity) {
                        Toast.makeText(activity, msg.getData().getString(Constants.TOAST),
                                Toast.LENGTH_SHORT).show();
                    }
                    break;
            }
        }
    };

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case Constants.REQUEST_CONNECT_DEVICE_SECURE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data, true);
                }
                break;
            case Constants.REQUEST_CONNECT_DEVICE_INSECURE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data, false);
                }
                break;
            case Constants.REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled, so set up a chat session
                    setupChat();
                } else {
                    // User did not enable Bluetooth or an error occurred
                    Log.d(TAG, "BT not enabled");
                    Toast.makeText(this, R.string.bt_not_enabled_leaving,
                            Toast.LENGTH_SHORT).show();
                    //getApplication().finish();
                }
            case Constants.REQUEST_DIALOG_FRAGMENT:
                if(resultCode == Activity.RESULT_OK){

                    //check status
                    if(mChatService.getState() != BluetoothChatService.STATE_CONNECTED){
                        Toast.makeText(this, R.string.not_connected,
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    //show the progress bar, disable user interaction
                    mProgressDialog.show();
                    //TODO: start watchdog
                    isCountdown = true;
                    mHandler.postDelayed(watchDogTimeOut,30000);
                    Log.d(TAG, "watchDog start");

                    //get SSID & PWD from user input
                    String SSID = data.getStringExtra("SSID") == null? "":data.getStringExtra("SSID");
                    String PWD = data.getStringExtra("PWD") == null? "":data.getStringExtra("PWD");

                    Log.d(TAG, "back from dialog: ok, SSID = " + SSID + ", PWD = " + PWD);

                    //TODO: 1. check Valid input  2. get the SSID and password from data object and send it to RPi through sendMessage() method
//                    Toast.makeText(getActivity(), R.string.config_success,
//                            Toast.LENGTH_SHORT).show();

                    //sendMessage(SSID,PWD);
                    //TODO:1. lock the app when configuring. 2. listen to configuration result and do the logic

                }else{
                    Log.d(TAG, "back from dialog, fail");
                }
        }
    }

    private void connectDevice(Intent data, boolean secure) {
        // Get the device MAC address
        String address = data.getExtras()
                .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        // Get the BluetoothDevice object
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        // Attempt to connect to the device
        mChatService.connect(device, secure);
    }

    public boolean isJson(String str) {
        try {
            new JSONObject(str);
        } catch (JSONException ex) {
            return false;
        }
        return true;
    }

    public void handleCallback(String str){
        String result;
        String ip;
        if(isCountdown){
            mHandler.removeCallbacks(watchDogTimeOut);
            isCountdown = false;
        }
        //enable user interaction
        mProgressDialog.dismiss();
        try{
            JSONObject mJSON = new JSONObject(str);
            result = mJSON.getString("result") == null? "" : mJSON.getString("result");
            ip = mJSON.getString("IP") == null? "" : mJSON.getString("IP");
            //Toast.makeText(getActivity(), "result: "+result+", IP: "+ip, Toast.LENGTH_LONG).show();

            if(!result.equals("SUCCESS")){
                Toast.makeText(getApplication(), R.string.config_fail,
                        Toast.LENGTH_LONG).show();
            }else{
//                Toast.makeText(getActivity(), R.string.config_success,
//                            Toast.LENGTH_SHORT).show();
                Toast.makeText(this,getString(R.string.config_success) + ip,Toast.LENGTH_LONG).show();
            }

        }catch (JSONException e){
            // error handling
            Toast.makeText(this, "SOMETHING WENT WRONG", Toast.LENGTH_LONG).show();
        }

    }
}