package io.treehouses.remote;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;


/**
 * Created by Terrence on 3/12/2018.
 */

public class HotspotDialogFragment extends DialogFragment {

    private static final String TAG = "HotspotDialogFragment";

    protected EditText hotspotSSIDEditText;
    protected EditText hotspotPWDEditText;
    protected EditText confirmPWDEditText;
    TextBoxValidation textBoxValidation = new TextBoxValidation();


    public static HotspotDialogFragment newInstance(int num) {
        HotspotDialogFragment hDialogFragment = new HotspotDialogFragment();
//        Bundle bundle = new Bundle();
//        bundle.putInt("num", num);
//        dialogFragment.setArguments(bundle);

        return hDialogFragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Log.d(TAG,"In onCreateDialog()");

        // Build the dialog and set up the button click handlers
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View mView = inflater.inflate(R.layout.hotspot_dialog,null);
        initLayoutView(mView);

        final AlertDialog mDialog = getAlertDialog(mView);
        mDialog.setTitle(R.string.dialog_message_hotspot);

        //initially disable button click
        textBoxValidation.getListener(mDialog);
        setTextChangeListener(mDialog);

        return mDialog;
    }

    protected AlertDialog getAlertDialog(View mView) {
        return new AlertDialog.Builder(getActivity())
                .setView(mView)
                .setTitle(R.string.dialog_message)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(R.string.start_configuration,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                //                                getActivity().getIntent().putExtra("isValidInput", mSSIDEditText.getText().toString().length() > 0? Boolean.TRUE: Boolean.FALSE);
                                String SSID = hotspotSSIDEditText.getText().toString();
                                String PWD = hotspotPWDEditText.getText().toString();
                                String CPWD = confirmPWDEditText.getText().toString();

                                Intent intent = new Intent();
                                intent.putExtra("SSID", SSID);
                                intent.putExtra("PWD", PWD);
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
        textBoxValidation.mDialog = mDialog;
        textBoxValidation.textWatcher = hotspotSSIDEditText;
        textBoxValidation.SSID = hotspotSSIDEditText;
        textBoxValidation.PWD = hotspotPWDEditText;
        textBoxValidation.hotspotTextboxValidation(confirmPWDEditText, getContext());

        textBoxValidation.mDialog = mDialog;
        textBoxValidation.textWatcher = hotspotPWDEditText;
        textBoxValidation.SSID = hotspotSSIDEditText;
        textBoxValidation.PWD = hotspotPWDEditText;
        textBoxValidation.hotspotTextboxValidation(confirmPWDEditText, getContext());

        textBoxValidation.mDialog = mDialog;
        textBoxValidation.textWatcher = confirmPWDEditText;
        textBoxValidation.SSID = hotspotSSIDEditText;
        textBoxValidation.PWD = hotspotPWDEditText;
        textBoxValidation.hotspotTextboxValidation(confirmPWDEditText, getContext());
    }

    protected void initLayoutView(View mView) {
        hotspotSSIDEditText = (EditText)mView.findViewById(R.id.hotspotSSID);
        hotspotPWDEditText = (EditText)mView.findViewById(R.id.hotspotPassword);
        confirmPWDEditText = (EditText)mView.findViewById(R.id.confirmPassword);

    }

}


