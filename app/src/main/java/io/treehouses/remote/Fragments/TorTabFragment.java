package io.treehouses.remote.Fragments;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import io.treehouses.remote.Constants;
import io.treehouses.remote.Fragments.DialogFragments.BottomSheetDialogs.EthernetBottomSheet;
import io.treehouses.remote.Fragments.DialogFragments.BottomSheetDialogs.TorBottomSheet;
import io.treehouses.remote.Network.BluetoothChatService;
import io.treehouses.remote.R;
import io.treehouses.remote.bases.BaseFragment;


public class TorTabFragment extends BaseFragment {
    private BluetoothChatService mChatService;
    private Button startButton, moreButton;
    private TextView textStatus;
    View view;
    private ImageView background, logo, internetstatus;
    private ClipboardManager myClipboard;
    private ClipData myClip;
    private ProgressDialog nDialog;
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {


        mChatService = listener.getChatService();
        mChatService.updateHandler(mHandler);

        listener.sendMessage("treehouses tor status");



        view = inflater.inflate(R.layout.activity_tor_fragment, container, false);
        logo = view.findViewById(R.id.treehouse_logo);
        startButton = view.findViewById(R.id.btn_tor_start);
        startButton.setEnabled(false);
        startButton.setText("Getting Tor Status from raspberry pi");
        textStatus = view.findViewById(R.id.tor_status_text);
        ColorMatrix matrix = new ColorMatrix();
        matrix.setSaturation(0);
        textStatus.setText("-");
        ColorMatrixColorFilter filter = new ColorMatrixColorFilter(matrix);
            logo.setColorFilter(filter);
             /* start/stop tor button click */
            startButton.setOnClickListener(v -> {
                if(startButton.getText().toString() == "Stop Tor"){
                   startButton.setText("Stopping Tor");
                    startButton.setEnabled(false);
                    listener.sendMessage("treehouses tor stop");
                }

                else{

                    listener.sendMessage("treehouses tor start");
                    startButton.setEnabled(false);
                    startButton.setText("Starting tor......");

                }


            });

        textStatus.setOnClickListener(v -> {


                myClipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                String text;
                text = textStatus.getText().toString();

                myClip = ClipData.newPlainText("text", textStatus.getText());
                myClipboard.setPrimaryClip(myClip);

                Toast.makeText(requireContext(), textStatus.getText() + " copied!",Toast.LENGTH_SHORT).show();

        });
            /* more button click */
//            moreButton.setOnClickListener(v ->{
//                showBottomSheet(new TorBottomSheet(), "ethernet");
//            });


        return view;
    }



    private void showBottomSheet(BottomSheetDialogFragment fragment, String tag) {
        fragment.setTargetFragment(TorTabFragment.this, Constants.NETWORK_BOTTOM_SHEET);
        fragment.show(getFragmentManager(), tag);
    }
    @SuppressLint("HandlerLeak")
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            if (msg.what == Constants.MESSAGE_READ) {
                String readMessage = (String) msg.obj;
                Log.d("Tor reply", "" + readMessage);
                if(readMessage.contains("inactive")){


                    ColorMatrix matrix = new ColorMatrix();
                    matrix.setSaturation(0);
                    textStatus.setText("-");
                    ColorMatrixColorFilter filter = new ColorMatrixColorFilter(matrix);
                    logo.setColorFilter(filter);
                    startButton.setText("Start Tor");
                    textStatus.setText("-");
                    startButton.setEnabled(true);
                }
                else if(readMessage.contains("the tor service has been stopped") || readMessage.contains("the tor service has been started")){

                    listener.sendMessage("treehouses tor status");
                }
                else if(readMessage.contains(".onion")){
                    textStatus.setText(readMessage);

                }
                else if(readMessage.contains("Error")){
                    textStatus.setText("Check if tor is setup");
                }
                else if(readMessage.contains("active")){
                    ColorMatrix matrix = new ColorMatrix();
                    ColorMatrixColorFilter filter = new ColorMatrixColorFilter(matrix);
                    logo.setColorFilter(filter);
                    startButton.setText("Stop Tor");
                    listener.sendMessage("treehouses tor");
                    startButton.setEnabled(true);
                }

                }



        }
    };
}


