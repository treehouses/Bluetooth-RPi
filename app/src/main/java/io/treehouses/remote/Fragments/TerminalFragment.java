package io.treehouses.remote.Fragments;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
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
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import io.treehouses.remote.MainApplication;
import io.treehouses.remote.Constants;
import io.treehouses.remote.Network.BluetoothChatService;
import io.treehouses.remote.R;
import io.treehouses.remote.adapter.NetworkListAdapter;
import io.treehouses.remote.bases.BaseTerminalFragment;
import io.treehouses.remote.pojo.NetworkListItem;

public class TerminalFragment extends BaseTerminalFragment {

    private static final String TAG = "BluetoothChatFragment";
    private ListView mConversationView;
    private EditText mOutEditText;
    private Button mSendButton;
    private Button pingStatusButton;
    private Button mCheckButton;
    private TextView mPingStatus;
    private ExpandableListView expandableListView;
    private NetworkListAdapter adapter;
    private static TerminalFragment instance = null;
    View view;

    public TerminalFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.activity_terminal_fragment, container, false);
        mChatService = listener.getChatService();
        mChatService.updateHandler(mHandler);
        instance = this;
        adapter = new NetworkListAdapter(getContext(), NetworkListItem.getTerminalList(), mChatService);
        adapter.setListener(listener);
        Log.e("TERMINAL mChatService", "" + mChatService.getState());
        expandableListView = view.findViewById(R.id.terminalList);
        onGroupExpand();
        expandableListView.setAdapter(adapter);
        setHasOptionsMenu(true);

        return view;
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

    private void onGroupExpand() {
        expandableListView.setOnGroupClickListener((parent, v, groupPosition, id) -> {
            if (!expandableListView.isGroupExpanded(groupPosition)) {
                expandableListView.setBackgroundColor(Color.WHITE);
            } else {
                expandableListView.setBackgroundColor(0);
            }
            return false;
        });
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
                idle(mPingStatus, pingStatusButton);
            }
        }
    }

    @Override
    public void onResume(){
        Log.e("tag", "LOG check onResume method ");
        super.onResume();
        setupChat();
    }

    public static TerminalFragment getInstance() {
        return instance;
    }

    public ArrayAdapter<String> getmConversationArrayAdapter() {
        return mConversationArrayAdapter;
    }

    /**
     * Set up the UI and background operations for chat.
     */
    public void setupChat() {
        Log.d(TAG, "setupChat()");
      
        copyToList(mConversationView, getContext());

        mConversationArrayAdapter = new ArrayAdapter<String>(getActivity(),R.layout.message, MainApplication.getTerminalList()){
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                return getViews(view, isRead);
            }
        };
        mConversationView.setAdapter(mConversationArrayAdapter);

        // Initialize the compose field with a listener for the return key
        mOutEditText.setOnEditorActionListener(mWriteListener);

        btnSendClickListener();

        buttonOnClick(mCheckButton, mChatService, mPingStatus, pingStatusButton);

        // Initialize the BluetoothChatService to perform bluetooth connections
        if (mChatService.getState() == Constants.STATE_NONE) {
            mChatService = new BluetoothChatService(mHandler);
        }
    }

    private void btnSendClickListener() {
        // Initialize the send button with a listener that for click events
        mSendButton.setOnClickListener(v -> {
            // Send a message using content of the edit text widget
            View view = getView();
            if (null != view) {
                TextView consoleInput = view.findViewById(R.id.edit_text_out);
                String message = consoleInput.getText().toString();
                listener.sendMessage(message);
                consoleInput.setText("");
            }
        });
    }

    /**
     * The action listener for the EditText widget, to listen for the return key
     */
    private TextView.OnEditorActionListener mWriteListener = new TextView.OnEditorActionListener() {
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

    /**
     * The Handler that gets information back from the BluetoothChatService
     */
    @SuppressLint("HandlerLeak")
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    if (msg.arg1 == Constants.STATE_LISTEN || msg.arg1 == Constants.STATE_NONE) { idle(mPingStatus, pingStatusButton); }
                    break;
                case Constants.MESSAGE_WRITE:
                    isRead = false;
                    handlerCaseWrite(TAG, mConversationArrayAdapter, msg);
                    break;
                case Constants.MESSAGE_READ:
                    String readMessage = (String)msg.obj;
                    isRead = true;
                    handlerCaseRead(readMessage, mPingStatus, pingStatusButton);
                    filterMessages(readMessage, mConversationArrayAdapter, MainApplication.getTerminalList());
                    break;
                case Constants.MESSAGE_DEVICE_NAME:
                    Activity activity = getActivity();
                    handlerCaseName(msg, activity);
                    break;
                case Constants.MESSAGE_TOAST:
                    handlerCaseToast(msg);
                    break;
            }
        }
    };
}
