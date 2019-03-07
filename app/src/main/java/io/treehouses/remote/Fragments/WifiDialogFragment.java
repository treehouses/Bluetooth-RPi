package io.treehouses.remote.Fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ToggleButton;

import androidx.core.content.ContextCompat;
import io.treehouses.remote.R;

public class WifiDialogFragment extends androidx.fragment.app.DialogFragment {

    private static final String TAG = "WifiDialogFragment";

    // Layout Views
    protected EditText mSSIDEditText;
    protected EditText mPWDEditText;
    TextBoxValidation textboxValidation = new TextBoxValidation();

    ToggleButton show_wifi;

    public static WifiDialogFragment newInstance(int num){

        WifiDialogFragment dialogFragment = new WifiDialogFragment();
//        Bundle bundle = new Bundle();
//        bundle.putInt("num", num);
//        dialogFragment.setArguments(bundle);

        return dialogFragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Log.d(TAG,"In onCreateDialog()");

        // Build the dialog and set up the button click handlers
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View mView = inflater.inflate(R.layout.dialog_wifi,null);
        initLayoutView(mView);

        final AlertDialog mDialog = getAlertDialog(mView);

        //initially disable button click
        textboxValidation.getListener(mDialog);
        setTextChangeListener(mDialog);

        return mDialog;
    }

    protected AlertDialog getAlertDialog(View mView) {
        return new AlertDialog.Builder(getActivity())
                    .setView(mView)
                    .setTitle(R.string.dialog_message)
                    .setIcon(R.drawable.dialog_icon)
                    .setPositiveButton(R.string.start_configuration,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
    //                                getActivity().getIntent().putExtra("isValidInput", mSSIDEditText.getText().toString().length() > 0? Boolean.TRUE: Boolean.FALSE);
                                        String SSID = mSSIDEditText.getText().toString();
                                        String PWD = mPWDEditText.getText().toString();

                                        Intent intent = new Intent();
                                        intent.putExtra("SSID", SSID);
                                        intent.putExtra("PWD", PWD);
                                        intent.putExtra("type", "wifi");
                                        getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, intent);
                                }
                            }
                    )
                    .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_CANCELED, getActivity().getIntent());
                        }
                    })
                    .create();
    }

    public void setTextChangeListener(final AlertDialog mDialog) {
        textboxValidation.mDialog = mDialog;
        textboxValidation.textWatcher = mSSIDEditText;
        textboxValidation.SSID = mSSIDEditText;
        textboxValidation.PWD = mPWDEditText;
        textboxValidation.textboxValidation(getActivity(), "wifi");

        textboxValidation.mDialog = mDialog;
        textboxValidation.textWatcher = mPWDEditText;
        textboxValidation.SSID = mSSIDEditText;
        textboxValidation.PWD = mPWDEditText;
        textboxValidation.textboxValidation(getActivity(), "wifi");
    }

    protected void initLayoutView(View mView) {
        mSSIDEditText = mView.findViewById(R.id.SSID);
        mPWDEditText = mView.findViewById(R.id.password);
    }
}

