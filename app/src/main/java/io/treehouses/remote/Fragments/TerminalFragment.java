package io.treehouses.remote.Fragments;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Message;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.chrono.ThaiBuddhistEra;

import io.treehouses.remote.MainApplication;
import io.treehouses.remote.Constants;
import io.treehouses.remote.Network.BluetoothChatService;
import io.treehouses.remote.R;
import io.treehouses.remote.Terminal;
import io.treehouses.remote.bases.BaseFragment;
import io.treehouses.remote.utils.Utils;

public class TerminalFragment extends BaseFragment {

    private static final String TAG = "BluetoothChatFragment";
    private ListView mConversationView;
    private EditText mOutEditText;
    private Button mSendButton;
    private Button pingStatusButton;
    private Button mCheckButton;
    private TextView mPingStatus;
    private ListView listView;
    View view;

    public TerminalFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.activity_terminal_fragment, container, false);
        mChatService = listener.getChatService();
        mChatService.updateHandler(mHandler);
        Log.e("TERMINAL mChatService", "" + mChatService.getState());
        listView = view.findViewById(R.id.listView);
        listView.setDivider(null);
        listView.setDividerHeight(0);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), R.layout.terminal_options_list, R.id.terminalTexxtview, Constants.terminalList);
        listView.setAdapter(adapter);
        setHasOptionsMenu(true);
        return view;
    }

    /**
     * Array adapter for the conversation thread
     */
    private ArrayAdapter<String> mConversationArrayAdapter;

    /**
     * Member object for the chat services
     */
    private BluetoothChatService mChatService = null;

    private static boolean isRead = false;

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mChatService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mChatService.getState() == Constants.STATE_NONE) {
                // Start the Bluetooth chat services
                mChatService.start();
                mIdle();
            }
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        mConversationView = view.findViewById(R.id.in);
        mOutEditText = view.findViewById(R.id.edit_text_out);
        mCheckButton = view.findViewById(R.id.check);
        mSendButton = view.findViewById(R.id.button_send);
        mPingStatus = view.findViewById(R.id.pingStatus);
        pingStatusButton = view.findViewById(R.id.PING);
    }

    @Override
    public void onResume(){
        Log.e("tag", "LOG check onResume method ");
        super.onResume();
        setupChat();
    }

    /**
     * Set up the UI and background operations for chat.
     */
    public void setupChat() {
        Log.d(TAG, "setupChat()");
      
        Terminal.copyToList(mConversationView, getContext());

        mConversationArrayAdapter = new ArrayAdapter<String>(getActivity(),R.layout.message, MainApplication.getTerminalList()){
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                Terminal.getView(view, isRead);
                return view;
            }
        };
        mConversationView.setAdapter(mConversationArrayAdapter);

        buttonFunctionality();

        // Initialize the compose field with a listener for the return key
        mOutEditText.setOnEditorActionListener(mWriteListener);

        // Initialize the send button with a listener that for click events
        mSendButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Send a message using content of the edit text widget
                View view = getView();
                if (null != view) {
                    TextView consoleInput = view.findViewById(R.id.edit_text_out);
                    String message = consoleInput.getText().toString();
                    listener.sendMessage(message);
                    consoleInput.setText("");
                }
            }
        });

        mCheckButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e("CHECK STATUS", "" + mChatService.getState());
                checkStatusNow();
            }
        });

        // Initialize the BluetoothChatService to perform bluetooth connections
        if (mChatService.getState() == Constants.STATE_NONE) {
            mChatService = new BluetoothChatService(mHandler);
        }
    }

    private void buttonFunctionality() {
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (position == 1) {
                    listener.sendMessage("treehouses");
                } else if (position == 3) {
                    listener.sendMessage("docker ps");
                } else if (position == 2) {
                    listener.sendMessage("treehouses detectrpi");
                } else if (position == 0) {
                    showChPasswordDialog();
                } else if (position == 5) {
                    listener.sendMessage("treehouses expandfs");
                }
            }
        });
    }

    public void checkStatusNow() {
        Terminal.checkStatus(mChatService);
    }

    /**
     * The action listener for the EditText widget, to listen for the return key
     */
    private TextView.OnEditorActionListener mWriteListener
            = new TextView.OnEditorActionListener() {
        public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
            // If the action is a key-up event on the return key, send the message
            if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_UP) {
                String message = view.getText().toString();
                listener.sendMessage(message);
                mOutEditText.setText("");
            }
            return true;
        }
    };

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case Constants.REQUEST_ENABLE_BT:
                onResultCaseEnable(resultCode);
                break;
            case Constants.REQUEST_DIALOG_FRAGMENT_CHPASS:
                onResultCaseDialogChpass(resultCode, data);
                break;
        }
    }

    private void onResultCaseDialogChpass(int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            //get password change request
            String chPWD = data.getStringExtra("password") == null ? "" : data.getStringExtra("password");

            //store password and command
            String password = "treehouses password " + chPWD;
            Log.d(TAG, "back from change password");

            //send password to command line interface
            listener.sendMessage(password);
        } else {
            Log.d(TAG, "back from change password, fail");
        }
    }

    private void onResultCaseEnable(int resultCode) {
        // When the request to enable Bluetooth returns
        if (resultCode == Activity.RESULT_OK) {
            // Bluetooth is now enabled, so set up a chat session
            setupChat();
        } else {
            // User did not enable Bluetooth or an error occurred
            Log.d(TAG, "BT not enabled");
            Toast.makeText(getActivity(), R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
            getActivity().finish();
        }
    }

    public void showChPasswordDialog() {
        // Create an instance of the dialog fragment and show it
        androidx.fragment.app.DialogFragment dialogFrag = ChPasswordDialogFragment.newInstance(123);
        dialogFrag.setTargetFragment(this, Constants.REQUEST_DIALOG_FRAGMENT_CHPASS);
        dialogFrag.show(getFragmentManager().beginTransaction(), "ChangePassDialog");
    }

    private boolean isJson(String str) {
        try {
            new JSONObject(str);
        } catch (JSONException ex) {
            return false;
        }
        return true;
    }

    private void mOffline() {
        Terminal.offline(mPingStatus, pingStatusButton);
    }

    private void mIdle() {
        Terminal.idle(mPingStatus, pingStatusButton);
    }

    private void mConnect() {
       Terminal.connect(mPingStatus, pingStatusButton);
    }

    /**
     * The Handler that gets information back from the BluetoothChatService
     */
    @SuppressLint("HandlerLeak")
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case Constants.STATE_LISTEN:
                        case Constants.STATE_NONE:
                            mIdle();
                            break;
                    }
                    break;
                case Constants.MESSAGE_WRITE:
                    Terminal.handlerCaseWrite(isRead, TAG, mConversationArrayAdapter, msg);
                    break;
                case Constants.MESSAGE_READ:
                    isRead = true;
                    String readMessage = (String)msg.obj;
                    Log.d("tag", "readMessage = " + readMessage);

                    //TODO: if message is json -> callback from RPi
                    if (isJson(readMessage)) {
                    } else {

                        readMessage = readMessage.trim();

                        //check if ping was successful
                        if (readMessage.contains("1 packets")) {
                            mConnect();
                        }
                        if (readMessage.contains("Unreachable") || readMessage.contains("failure")) {
                            mOffline();
                        }
                        //make it so text doesn't show on chat (need a better way to check multiple strings since mConversationArrayAdapter only takes messages line by line)
                        if (!readMessage.contains("1 packets") && !readMessage.contains("64 bytes") && !readMessage.contains("google.com") &&
                                !readMessage.contains("rtt") && !readMessage.trim().isEmpty()) {
                            MainApplication.getTerminalList().add(readMessage);
                            mConversationArrayAdapter.notifyDataSetChanged();
                        }
                    }
                    break;
                case Constants.MESSAGE_DEVICE_NAME:
                    Activity activity = getActivity();
                    Terminal.handlerCaseName(msg, activity);
                    break;
                case Constants.MESSAGE_TOAST:
                    if (null != getActivity()) {
                        Toast.makeText(getActivity(), msg.getData().getString(Constants.TOAST),
                                Toast.LENGTH_SHORT).show();
                    }
                    break;
            }
        }
    };
}
