package io.treehouses.remote.adapter;

import android.content.Context;
import android.graphics.Color;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import io.treehouses.remote.R;
import io.treehouses.remote.callback.HomeInteractListener;
import io.treehouses.remote.utils.ButtonConfiguration;
import io.treehouses.remote.utils.TextWatcherUtils;

class ViewHolderHotspot extends ButtonConfiguration{
    private EditText etPassword;
    private Spinner spn;

    public ViewHolderHotspot(View v, final HomeInteractListener listener, final Context context) {
        etSsid = v.findViewById(R.id.et_hotspot_ssid);
        spn = v.findViewById(R.id.spn_hotspot_type);
        etPassword = v.findViewById(R.id.et_hotspot_password);
        btnStartConfiguration = v.findViewById(R.id.btn_start_config);
        etSsid.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);

        buttonProperties(false, Color.LTGRAY, btnStartConfiguration);

        etSsid.addTextChangedListener(new TextWatcherUtils(etSsid));

        btnStartConfiguration.setOnClickListener(view -> {
            if (etPassword.getText().toString().isEmpty()) {
                listener.sendMessage("treehouses ap \"" + spn.getSelectedItem().toString() + "\" \"" + etSsid.getText().toString() + "\"");
                Toast.makeText(context, "Connecting...", Toast.LENGTH_LONG).show();
            } else {
                listener.sendMessage("treehouses ap \"" + spn.getSelectedItem().toString() + "\" \"" + etSsid.getText().toString() + "\" \"" + etPassword.getText().toString() + "\"");
                Toast.makeText(context, "Connecting...", Toast.LENGTH_LONG).show();
            }

            buttonProperties(false, Color.LTGRAY, btnStartConfiguration);
        });
    }
}
